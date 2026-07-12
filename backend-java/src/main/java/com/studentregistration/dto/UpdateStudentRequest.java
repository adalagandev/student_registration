package com.studentregistration.dto;

/**
 * UpdateStudentRequest — the JSON body for {@code PUT /api/students/{id}}.
 *
 * <p>Only the editable contact fields are accepted: {@code email}, {@code address}
 * and {@code phone}. Name and program are deliberately NOT part of this DTO — the
 * update endpoint must never change them (program has its own gated endpoint), so
 * omitting them here makes that contract structural rather than something we have
 * to remember to skip.
 */
public record UpdateStudentRequest(String email, String address, String phone) {
}
