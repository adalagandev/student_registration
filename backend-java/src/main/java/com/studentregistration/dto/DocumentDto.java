package com.studentregistration.dto;

import com.studentregistration.entity.Document;

/**
 * DocumentDto — the JSON shape returned by {@code GET /students/{id}/documents}:
 * {@code { id, name, uploadedAt }}. This mirrors the inline dictionary the Python
 * backend built in {@code list_documents()} (it did NOT reuse row_to_dict there).
 *
 * <p>Note {@code name} is a DISPLAY name: the original upload name when present,
 * otherwise the on-disk filename — the {@code original_name or filename} fallback
 * from Flask.
 */
public record DocumentDto(Long id, String name, String uploadedAt) {

    public static DocumentDto from(Document d) {
        // `name` prefers the original display name, falling back to the stored
        // filename when original_name is null or empty (legacy rows).
        String original = d.getOriginalName();
        String name = (original != null && !original.isEmpty()) ? original : d.getFilename();
        return new DocumentDto(d.getId(), name, d.getUploadedAt());
    }
}
