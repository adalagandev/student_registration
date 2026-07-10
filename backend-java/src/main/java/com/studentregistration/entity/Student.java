package com.studentregistration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Student — a JPA "entity". An entity is a plain Java class that Hibernate maps
 * to a database table: each instance is one row, each mapped field one column.
 * This replaces the ad-hoc dictionaries app.py built from {@code sqlite3.Row}.
 *
 * <p>The table already exists (created by the Python backend), so the important
 * job here is to match its column names EXACTLY. The database uses snake_case
 * (SQL convention) while Java uses camelCase, so every field carries an explicit
 * {@code @Column(name = "...")} bridging the two — the same snake_case↔camelCase
 * boundary that {@code row_to_dict()} handled in Flask.
 */
@Entity                       // "this class is mapped to a database table"
@Table(name = "students")     // ...specifically the `students` table
public class Student {

    /**
     * Primary key. {@code GenerationType.IDENTITY} tells Hibernate the database
     * assigns the id itself (SQLite's {@code INTEGER PRIMARY KEY AUTOINCREMENT}),
     * so it reads the generated value back after INSERT — the equivalent of
     * app.py reading {@code cursor.lastrowid}.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false)
    private String email;

    /** Changed only via the gated program-change endpoint, never a plain update. */
    @Column(name = "program", nullable = false)
    private String program;

    @Column(name = "address", nullable = false)
    private String address;

    /**
     * Optional contact number. The DB column is {@code NOT NULL DEFAULT ''}, and
     * the app treats it as an always-present (possibly empty) string, so we
     * default it to "" rather than allowing null.
     */
    @Column(name = "phone", nullable = false)
    private String phone = "";

    /**
     * When the student registered — an ISO-8601 string set by the SERVER
     * (see StudentService), never supplied by the client. Stored as TEXT to match
     * the Python backend, which never used a real date/time column type.
     */
    @Column(name = "registered_at", nullable = false)
    private String registeredAt = "";

    /**
     * No-argument constructor. JPA requires one to instantiate rows it reads back;
     * it is also {@code public} so our service layer can build a fresh student and
     * populate it via the setters below.
     */
    public Student() {
    }

    // --- Getters and setters --------------------------------------------------
    // Hibernate uses these to read/populate the object; our services use them to
    // build and mutate students.

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(String registeredAt) {
        this.registeredAt = registeredAt;
    }
}
