package com.studentregistration.dto;

import com.studentregistration.entity.Student;

/**
 * StudentDto — the exact JSON shape the API exposes for a student. "DTO" means
 * "Data Transfer Object": a plain carrier of data across the wire.
 *
 * <p>This is the Java home of {@code row_to_dict()} from app.py. We deliberately
 * return a DTO rather than the {@link Student} entity so the JSON contract is
 * explicit and cannot drift when the entity changes — Jackson serializes these
 * field names verbatim, giving the frontend exactly:
 * {@code {id, firstName, lastName, email, program, address, phone, registeredAt}}.
 *
 * <p>A Java {@code record} is a compact, immutable data class: declaring the
 * components below auto-generates the constructor and accessor methods.
 */
public record StudentDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        String program,
        String address,
        String phone,
        String registeredAt) {

    /** Build the wire DTO from a persisted entity — the camelCase mapping point. */
    public static StudentDto from(Student s) {
        return new StudentDto(
                s.getId(),
                s.getFirstName(),
                s.getLastName(),
                s.getEmail(),
                s.getProgram(),
                s.getAddress(),
                s.getPhone(),
                s.getRegisteredAt());
    }
}
