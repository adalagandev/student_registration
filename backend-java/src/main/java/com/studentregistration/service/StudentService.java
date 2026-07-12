package com.studentregistration.service;

import java.util.List;

import com.studentregistration.dto.CreateStudentRequest;
import com.studentregistration.dto.StudentDto;
import com.studentregistration.dto.UpdateStudentRequest;
import com.studentregistration.entity.Student;
import com.studentregistration.repository.StudentRepository;
import com.studentregistration.web.ApiException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * StudentService — the business logic for creating, listing and updating students.
 *
 * <p>This is where the validation and "server is the source of truth" rules from
 * app.py's {@code list_students}, {@code create_student} and {@code update_student}
 * routes live. Controllers stay thin: they just hand the request here and serialize
 * whatever comes back. Marked {@code @Service} so Spring creates one shared instance
 * and injects the repository through the constructor.
 */
@Service
public class StudentService {

    private final StudentRepository students;

    public StudentService(StudentRepository students) {
        this.students = students;
    }

    /**
     * GET /api/students — every student, newest first.
     * {@code Sort.by(DESC, "id")} sorts on the entity's {@code id} field, producing
     * {@code ORDER BY id DESC} just like the Flask query.
     */
    public List<StudentDto> listStudents() {
        return students.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(StudentDto::from)
                .toList();
    }

    /**
     * POST /api/students — create a student.
     * Mirrors app.py: trim every field (treating null/blank as missing), require the
     * five mandatory fields, and set {@code registeredAt} on the SERVER so the client
     * can neither forge nor omit it.
     */
    public StudentDto createStudent(CreateStudentRequest req) {
        String firstName = trim(req.firstName());
        String lastName = trim(req.lastName());
        String email = trim(req.email());
        String program = trim(req.program());
        String address = trim(req.address());
        String phone = trim(req.phone()); // optional

        // Server-side validation — never trust the frontend to have validated.
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()
                || program.isEmpty() || address.isEmpty()) {
            throw ApiException.badRequest(
                    "First name, last name, email, program and address are required.");
        }

        Student student = new Student();
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setProgram(program);
        student.setAddress(address);
        student.setPhone(phone);
        // The registration date is decided by the server, not sent by the client.
        student.setRegisteredAt(Timestamps.nowUtcSeconds());

        // save() INSERTs and returns the managed entity with its generated id filled in.
        return StudentDto.from(students.save(student));
    }

    /**
     * PUT /api/students/{id} — update editable contact fields only.
     * Email and address are required (non-empty); phone is optional. Name and
     * program are intentionally left untouched.
     *
     * <p>{@code @Transactional} wraps the find-then-save as one unit of work, keeping
     * it consistent with the other multi-step service methods.
     */
    @Transactional
    public StudentDto updateStudent(long studentId, UpdateStudentRequest req) {
        String email = trim(req.email());
        String address = trim(req.address());
        String phone = trim(req.phone()); // optional

        if (email.isEmpty() || address.isEmpty()) {
            throw ApiException.badRequest("Email and address cannot be empty.");
        }

        // Existence check → 404 if the student doesn't exist.
        Student student = students.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("Student not found."));

        student.setEmail(email);
        student.setAddress(address);
        student.setPhone(phone);

        return StudentDto.from(students.save(student));
    }

    /**
     * Null-coalesce then trim, matching Python's {@code (value or "").strip()}.
     * A missing JSON key deserializes to {@code null}; here that becomes "".
     */
    private static String trim(String value) {
        return value == null ? "" : value.strip();
    }
}
