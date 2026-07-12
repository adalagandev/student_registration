package com.studentregistration.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.studentregistration.dto.WaitlistEntryDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * WaitlistService — the business logic behind the read-only "waitlist" tab.
 *
 * <p>There is no database table for the waitlist: it is mock data shipped inside
 * the application as a plain text file ({@code src/main/resources/waitlist.txt}).
 * This service reads that file, parses each line into a {@link WaitlistEntryDto},
 * and hands the list back. Controllers stay thin — they just serialize whatever
 * comes back. Marked {@code @Service} so Spring creates one shared instance;
 * because it holds no mutable fields it is safe to share across concurrent
 * requests.
 *
 * <p>The file lives on the CLASSPATH rather than at a filesystem path on purpose.
 * When the app is packaged as a jar, {@code src/main/resources} contents end up
 * INSIDE the jar, so a raw {@code new File("waitlist.txt")} would not find it.
 * Spring's {@link ClassPathResource} knows how to open a resource whether we are
 * running from an unpacked {@code target/classes} folder in the IDE or from a
 * packaged jar in production — {@code getInputStream()} works in both cases.
 */
// @agent: service-architect
@Service
public class WaitlistService {

    /** Name of the bundled resource, resolved relative to the classpath root. */
    private static final String WAITLIST_RESOURCE = "waitlist.txt";

    /** Each data line is {@code name | email | program | dateAdded}. */
    private static final String FIELD_DELIMITER = "\\|";

    /** A valid entry has exactly this many pipe-delimited fields. */
    private static final int EXPECTED_FIELDS = 4;

    /**
     * GET /api/waitlist — every waitlist entry, in file order.
     *
     * <p>We re-read the file on every call. The file is tiny and read-only, so
     * caching would add complexity (and staleness) for no real benefit; reading
     * fresh keeps the service stateless and always reflects the bundled data.
     */
    public List<WaitlistEntryDto> listWaitlist() {
        // ClassPathResource is a lazy handle — it does not touch I/O until we
        // actually open a stream below.
        ClassPathResource resource = new ClassPathResource(WAITLIST_RESOURCE);

        List<WaitlistEntryDto> entries = new ArrayList<>();

        // try-with-resources guarantees the underlying stream/reader are closed
        // even if a read throws partway through. We wrap the raw bytes in an
        // InputStreamReader with an EXPLICIT UTF-8 charset so names with accented
        // characters decode the same way on every platform (never rely on the
        // JVM's default charset).
        try (InputStream in = resource.getInputStream();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(in, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                WaitlistEntryDto entry = parseLine(line);
                // parseLine returns null for comment/blank/malformed lines; we
                // skip those defensively so one bad row never breaks the list.
                if (entry != null) {
                    entries.add(entry);
                }
            }
        } catch (IOException e) {
            // A genuine failure to read the bundled resource is not something a
            // caller can recover from, so we wrap it as an unchecked exception
            // (same pattern as FileStorageService). The web/GlobalExceptionHandler
            // catch-all then turns this into a 500 {"error": ...} response.
            throw new UncheckedIOException("Failed to read waitlist resource", e);
        }

        return entries;
    }

    /**
     * Parse a single raw line into a {@link WaitlistEntryDto}, or return
     * {@code null} when the line carries no usable entry.
     *
     * <p>Rules (all applied to the TRIMMED line):
     * <ul>
     *   <li>Blank lines and lines starting with {@code #} are comments → skipped.</li>
     *   <li>A data line must split into exactly {@value #EXPECTED_FIELDS}
     *       pipe-delimited fields; anything else is malformed → skipped, rather
     *       than crashing the whole request over one typo in the data file.</li>
     * </ul>
     */
    private static WaitlistEntryDto parseLine(String rawLine) {
        String trimmed = rawLine.trim();

        // Guard clauses first: bail out on the "not a data row" cases so the
        // happy path below stays unindented.
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null;
        }

        // Split on the literal pipe. We pass a negative limit (-1) so that
        // trailing empty fields are KEPT — that way a line ending in "|" yields
        // the wrong field count and is correctly rejected as malformed rather
        // than silently trimmed down to a "valid"-looking 4 fields.
        String[] parts = trimmed.split(FIELD_DELIMITER, -1);
        if (parts.length != EXPECTED_FIELDS) {
            return null;
        }

        // Trim each field: the source file pads columns with spaces for
        // readability, and none of those spaces belong in the wire value.
        String name = parts[0].trim();
        String email = parts[1].trim();
        String program = parts[2].trim();
        String dateAdded = parts[3].trim();

        return new WaitlistEntryDto(name, email, program, dateAdded);
    }
}
