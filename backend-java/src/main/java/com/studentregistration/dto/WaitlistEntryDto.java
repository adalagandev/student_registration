package com.studentregistration.dto;

/**
 * WaitlistEntryDto — the exact JSON shape the API exposes for one waitlist entry.
 * "DTO" means "Data Transfer Object": a plain carrier of data across the wire.
 *
 * <p>Unlike {@link StudentDto}, there is no persisted entity behind this: the
 * waitlist is read-only mock data bundled as a classpath text file and parsed by
 * {@code WaitlistService}. This record simply declares the four camelCase fields
 * that Jackson serializes verbatim, giving the frontend exactly:
 * {@code {name, email, program, dateAdded}}.
 *
 * <p>{@code dateAdded} is intentionally a {@code String} in {@code YYYY-MM-DD}
 * form — it is a display value copied straight from the source file, not a value
 * we ever do date arithmetic on, so keeping it as text avoids parsing/formatting
 * round-trips and matches how the file stores it.
 *
 * <p>A Java {@code record} is a compact, immutable data class: declaring the
 * components below auto-generates the constructor and accessor methods.
 */
// @agent: service-architect
public record WaitlistEntryDto(
        String name,
        String email,
        String program,
        String dateAdded) {
}
