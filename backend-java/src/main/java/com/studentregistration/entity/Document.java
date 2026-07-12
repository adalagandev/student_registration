package com.studentregistration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Document — one uploaded program-change PDF, mapped to the {@code documents}
 * table. Each row records where the file lives on disk plus a friendly display
 * name and an upload timestamp.
 *
 * <p><b>Design choice:</b> the link back to the owning student is modelled as a
 * plain {@code Long studentId} column, NOT a JPA {@code @ManyToOne} association.
 * That mirrors the Python backend exactly: SQLite there never enabled foreign-key
 * enforcement ({@code PRAGMA foreign_keys} was left off), so the relationship is
 * "declarative only" and deletes don't cascade. Keeping it a scalar avoids
 * introducing cascade/lazy-loading behaviour the original app never had.
 */
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The owning student's id (loose reference — see class comment). */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** The name as stored on disk: timestamp-prefixed and sanitized. */
    @Column(name = "filename", nullable = false)
    private String filename;

    /**
     * The display name shown to the user. In the Python backend this held the
     * werkzeug-sanitized upload name (not the raw browser name), and it is
     * nullable for rows created before this column existed.
     */
    @Column(name = "original_name")
    private String originalName;

    /** ISO-8601 upload timestamp (server-generated, matches registered_at format). */
    @Column(name = "uploaded_at", nullable = false)
    private String uploadedAt;

    protected Document() {
    }

    /** Convenience constructor used by the service when recording a new upload. */
    public Document(Long studentId, String filename, String originalName, String uploadedAt) {
        this.studentId = studentId;
        this.filename = filename;
        this.originalName = originalName;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getFilename() {
        return filename;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getUploadedAt() {
        return uploadedAt;
    }
}
