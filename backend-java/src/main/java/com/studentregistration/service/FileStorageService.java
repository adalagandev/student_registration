package com.studentregistration.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Set;

import com.studentregistration.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * FileStorageService — everything about putting program-change PDFs on disk and
 * reading them back. This is the Java home of the file bits of app.py:
 * {@code secure_filename}, the timestamp-prefixed {@code stored_name}, the
 * {@code uploads/<student_id>/} layout, and {@code send_from_directory}.
 *
 * <p>The upload root ({@code app.upload-dir}) points at the SAME {@code uploads/}
 * folder the Python backend used, so previously uploaded files remain reachable.
 */
@Service
public class FileStorageService {

    /** Werkzeug only keeps these characters; everything else is stripped. */
    private static final String ALLOWED = "[^A-Za-z0-9_.-]";

    /** Windows reserved device names werkzeug guards against by prefixing "_". */
    private static final Set<String> WINDOWS_DEVICE_FILES = Set.of(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir);
    }

    /**
     * The result of storing one upload: what we wrote to disk, and the sanitized
     * display name to record in the {@code original_name} column. (The Python
     * backend stored the SANITIZED name as the "original", not the raw browser
     * name — we do the same.)
     */
    public record StoredFile(String storedName, String originalName) {
    }

    /**
     * Save one uploaded PDF under {@code uploads/<studentId>/} and return the names
     * to persist. {@code timestamp} is shared across all files in one request (like
     * app.py's single {@code now}); the on-disk name replaces the ISO {@code :} with
     * {@code -} but keeps the {@code T}, e.g. {@code 2026-06-15T13-19-00_resume.pdf}.
     */
    public StoredFile store(long studentId, MultipartFile file, String timestamp) {
        String safeName = secureFilename(file.getOriginalFilename());
        // Prefix with the timestamp so same-named uploads don't collide across requests.
        String storedName = timestamp.replace(":", "-") + "_" + safeName;

        try {
            Path folder = uploadRoot.resolve(String.valueOf(studentId));
            Files.createDirectories(folder);
            Path target = folder.resolve(storedName);
            // Copy the bytes; REPLACE_EXISTING mirrors Flask's f.save() overwrite.
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Wrap as unchecked so callers aren't forced to declare it; the global
            // handler will turn any leak into a 500.
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }

        return new StoredFile(storedName, safeName);
    }

    /**
     * Return a readable Resource for a stored file so the controller can stream it
     * inline. If the row exists but the file is gone from disk, we surface a 404 —
     * the same effect {@code send_from_directory} had.
     */
    public Resource loadAsResource(long studentId, String filename) {
        Path path = uploadRoot.resolve(String.valueOf(studentId)).resolve(filename);
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw ApiException.notFound("Document not found.");
        }
        return new PathResource(path);
    }

    /**
     * Delete a stored file, ignoring the case where it is already gone — exactly
     * like app.py's {@code if os.path.exists(...): os.remove(...)} guard, so a
     * missing file never blocks removing the database row.
     */
    public void deleteQuietly(long studentId, String filename) {
        try {
            Path path = uploadRoot.resolve(String.valueOf(studentId)).resolve(filename);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete stored file", e);
        }
    }

    /**
     * A faithful port of werkzeug's {@code secure_filename}. Given an arbitrary
     * (possibly malicious) upload name, produce a safe filename containing only
     * {@code [A-Za-z0-9_.-]} with no directory components. Steps mirror werkzeug:
     * <ol>
     *   <li>Unicode-normalize (NFKD) and drop non-ASCII characters.</li>
     *   <li>Turn path separators ({@code /} and {@code \}) into spaces.</li>
     *   <li>Collapse runs of whitespace into single underscores.</li>
     *   <li>Strip every character outside the allowed set.</li>
     *   <li>Trim leading/trailing dots and underscores.</li>
     *   <li>Prefix {@code _} if the base name is a Windows reserved device name.</li>
     * </ol>
     */
    static String secureFilename(String filename) {
        if (filename == null) {
            return "";
        }
        // 1. Normalize to ASCII, dropping accents/non-ASCII (Python's
        //    encode("ascii", "ignore")).
        String ascii = Normalizer.normalize(filename, Normalizer.Form.NFKD)
                .replaceAll("[^\\x00-\\x7F]", "");

        // 2. Path separators become spaces so directory parts can't survive.
        ascii = ascii.replace('/', ' ').replace('\\', ' ');

        // 3. Collapse whitespace to single underscores ("_".join(text.split())).
        String joined = String.join("_", ascii.trim().split("\\s+"));

        // 4. Keep only allowed characters.
        joined = joined.replaceAll(ALLOWED, "");

        // 5. Strip leading/trailing dots and underscores.
        joined = joined.replaceAll("^[._]+", "").replaceAll("[._]+$", "");

        // 6. Guard Windows device names (we run on Windows).
        if (!joined.isEmpty()) {
            String base = joined.split("\\.")[0].toUpperCase();
            if (WINDOWS_DEVICE_FILES.contains(base)) {
                joined = "_" + joined;
            }
        }

        return joined;
    }
}
