package com.studentregistration.repository;

import java.util.List;
import java.util.Optional;

import com.studentregistration.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * DocumentRepository — CRUD for the {@code documents} table, plus two "derived
 * query" methods. Spring Data parses the METHOD NAME and writes the SQL for us,
 * so the names must read like the query they represent:
 *
 * <ul>
 *   <li>{@code findByStudentIdOrderByIdDesc} →
 *       {@code SELECT * FROM documents WHERE student_id = ? ORDER BY id DESC}
 *       (the query behind {@code GET /documents}, newest first).</li>
 *   <li>{@code findByIdAndStudentId} →
 *       {@code SELECT * FROM documents WHERE id = ? AND student_id = ?}
 *       — matches on BOTH ids so a document can't be reached under the wrong
 *       student, exactly like the Flask GET/DELETE single-document routes.</li>
 * </ul>
 */
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByStudentIdOrderByIdDesc(Long studentId);

    Optional<Document> findByIdAndStudentId(Long id, Long studentId);
}
