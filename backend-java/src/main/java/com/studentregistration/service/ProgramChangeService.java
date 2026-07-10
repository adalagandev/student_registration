package com.studentregistration.service;

import java.util.List;
import java.util.Locale;

import com.studentregistration.dto.StudentDto;
import com.studentregistration.entity.Document;
import com.studentregistration.entity.Student;
import com.studentregistration.repository.DocumentRepository;
import com.studentregistration.repository.StudentRepository;
import com.studentregistration.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * ProgramChangeService — the gated program-change flow from app.py's
 * {@code change_program}. Changing a student's program is only allowed together
 * with 1–2 supporting PDF uploads, so the validation here is stricter than a plain
 * update and the ORDER of the checks matters (see below).
 */
@Service
public class ProgramChangeService {

    /** 25 MB per individual PDF (26,214,400 bytes) — checked in application code. */
    static final long MAX_FILE_BYTES = 25L * 1024 * 1024;
    static final int MIN_FILES = 1;
    static final int MAX_FILES = 2;

    private final StudentRepository students;
    private final DocumentRepository documents;
    private final FileStorageService storage;

    public ProgramChangeService(StudentRepository students,
                                DocumentRepository documents,
                                FileStorageService storage) {
        this.students = students;
        this.documents = documents;
        this.storage = storage;
    }

    /**
     * Apply a program change. {@code @Transactional} wraps the document inserts and
     * the program UPDATE in one unit of work: if anything throws, none of the DB
     * writes commit (the Python code committed once at the end for a similar effect).
     *
     * <p><b>Validation order is deliberate and must not change</b> — it matches Flask
     * exactly, where program/file validation runs BEFORE the student-existence check.
     * So a bad file for a non-existent student returns 400 (bad input), not 404.
     */
    @Transactional
    public StudentDto changeProgram(long studentId, String programRaw, List<MultipartFile> forms) {
        String newProgram = programRaw == null ? "" : programRaw.strip();

        // Drop empty file entries some browsers include (Flask: [f for f in files if f and f.filename]).
        List<MultipartFile> files = (forms == null ? List.<MultipartFile>of() : forms).stream()
                .filter(f -> f != null && f.getOriginalFilename() != null && !f.getOriginalFilename().isEmpty())
                .toList();

        // (a) program name required.
        if (newProgram.isEmpty()) {
            throw ApiException.badRequest("New program name is required.");
        }

        // (b) file count must be 1..2.
        if (files.size() < MIN_FILES || files.size() > MAX_FILES) {
            throw ApiException.badRequest(
                    "Please upload " + MIN_FILES + " to " + MAX_FILES + " PDF file(s).");
        }

        // (c) validate EVERY file up front — before saving ANY — so a bad second file
        //     doesn't leave the first one written to disk.
        for (MultipartFile f : files) {
            String name = f.getOriginalFilename();
            boolean isPdfName = name != null && name.toLowerCase(Locale.ROOT).endsWith(".pdf");
            boolean isPdfType = "application/pdf".equals(f.getContentType());
            if (!(isPdfName && isPdfType)) {
                throw ApiException.badRequest("'" + name + "' is not a PDF.");
            }
            if (f.getSize() > MAX_FILE_BYTES) {
                throw ApiException.badRequest("'" + name + "' exceeds the 25 MB limit.");
            }
        }

        // (d) NOW check the student exists (after input validation, per Flask order).
        Student student = students.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("Student not found."));

        // (e) Save each file and record a documents row; all files share one timestamp.
        String now = Timestamps.nowUtcSeconds();
        for (MultipartFile f : files) {
            FileStorageService.StoredFile stored = storage.store(studentId, f, now);
            documents.save(new Document(studentId, stored.storedName(), stored.originalName(), now));
        }

        // Apply the program change now that the forms are accepted and stored.
        student.setProgram(newProgram);
        return StudentDto.from(students.save(student));
    }
}
