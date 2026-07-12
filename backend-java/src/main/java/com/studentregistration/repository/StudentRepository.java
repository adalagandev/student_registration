package com.studentregistration.repository;

import com.studentregistration.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * StudentRepository — a Spring Data "repository". We only declare an interface;
 * Spring generates the implementation at runtime. Extending {@link JpaRepository}
 * grants ready-made CRUD methods that replace the hand-written SQL in app.py:
 *
 * <ul>
 *   <li>{@code findAll(Sort)} → {@code SELECT * FROM students ORDER BY ...}</li>
 *   <li>{@code findById(id)} → the {@code fetch_student()} helper (returns Optional)</li>
 *   <li>{@code save(entity)} → INSERT (new) or UPDATE (existing)</li>
 * </ul>
 *
 * <p>The two type parameters are the entity class and its primary-key type
 * ({@code Student} keyed by {@code Long}).
 */
public interface StudentRepository extends JpaRepository<Student, Long> {
    // No custom queries needed — the built-in methods cover every student operation.
}
