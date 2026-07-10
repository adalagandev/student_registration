package com.studentregistration.dto;

/**
 * CreateStudentRequest — the JSON body Jackson deserializes for
 * {@code POST /api/students}. Field names match the camelCase keys the frontend
 * sends: {@code firstName, lastName, email, program, address, phone}.
 *
 * <p>Every field is a nullable {@code String} on purpose. The Python backend used
 * {@code (data.get("x") or "").strip()} — tolerating missing keys and trimming
 * whitespace — and did its own presence validation. We replicate that null-tolerant
 * behaviour in {@link com.studentregistration.service.StudentService} rather than
 * with bean-validation annotations, so a missing field yields our exact error
 * message instead of a framework-generated one.
 */
public record CreateStudentRequest(
        String firstName,
        String lastName,
        String email,
        String program,
        String address,
        String phone) {
}
