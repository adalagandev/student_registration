package com.studentregistration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentregistration.entity.Student;
import com.studentregistration.repository.DocumentRepository;
import com.studentregistration.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * ApiIntegrationTestBase — shared setup for the MockMvc parity suite.
 *
 * <p><b>Why a full {@code @SpringBootTest}:</b> these tests exist to lock in the exact
 * HTTP contract the React frontend depends on (status codes, camelCase JSON, error
 * shapes). Parity bugs hide in the wiring between the real controllers,
 * {@code GlobalExceptionHandler}, the services and the JPA layer, so we exercise the
 * whole stack end-to-end rather than mocking it out. (Normally the unit suite avoids
 * booting the container — see rule 8 — but here the container wiring IS the behaviour
 * under test.)
 *
 * <p><b>CRITICAL isolation:</b> {@code application.properties} points Hibernate at the
 * REAL shared dev database ({@code ../backend/students.db}) and upload dir
 * ({@code ../backend/uploads}). A test must never touch those. We override both via
 * {@link DynamicPropertySource} <em>before</em> the Spring context is created:
 * <ul>
 *   <li>{@code spring.datasource.url} → a throwaway SQLite file in an OS temp dir.</li>
 *   <li>{@code app.upload-dir} → a throwaway uploads folder in that same temp dir.</li>
 *   <li>{@code ddl-auto=update} → Hibernate creates the schema fresh in the temp DB.</li>
 * </ul>
 * The temp directory is created in a static initializer so it is guaranteed to exist
 * by the time {@code @DynamicPropertySource} runs. No test path can reach the real
 * files: the real URL from {@code application.properties} is replaced outright.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class ApiIntegrationTestBase {

    /**
     * A unique temp root per JVM run, created BEFORE the Spring context reads the
     * dynamic properties below. Everything (DB file + uploads) lives under here, well
     * away from {@code ../backend/}.
     */
    protected static final Path TEMP_ROOT = createTempRoot();

    private static Path createTempRoot() {
        try {
            return Files.createTempDirectory("student-reg-test-");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create test temp dir", e);
        }
    }

    /** The uploads folder the FileStorageService will write to during tests. */
    protected static final Path UPLOADS_ROOT = TEMP_ROOT.resolve("uploads");

    @DynamicPropertySource
    static void overrideProductionPathsWithTempOnes(DynamicPropertyRegistry registry) {
        // Forward slashes keep the JDBC URL valid on Windows (C:\... would confuse it).
        Path db = TEMP_ROOT.resolve("test-students.db");
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:" + db.toString().replace('\\', '/'));
        registry.add("app.upload-dir", () -> UPLOADS_ROOT.toString());
        // Create the schema fresh in the throwaway DB.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected StudentRepository studentRepository;

    @Autowired
    protected DocumentRepository documentRepository;

    /**
     * Reset all shared state before EACH test so tests are order-independent and can
     * each pass alone (rule 4). We clear both tables and wipe the uploads folder.
     */
    @BeforeEach
    void resetState() {
        documentRepository.deleteAll();
        studentRepository.deleteAll();
        wipeUploads();
    }

    // --- Arrangement helpers --------------------------------------------------

    /** Persist a ready-made student directly (arrangement, not the behaviour tested). */
    protected Student persistStudent(String firstName, String lastName, String email,
                                     String program, String address, String phone) {
        Student s = new Student();
        s.setFirstName(firstName);
        s.setLastName(lastName);
        s.setEmail(email);
        s.setProgram(program);
        s.setAddress(address);
        s.setPhone(phone);
        s.setRegisteredAt("2026-01-01T00:00:00");
        return studentRepository.save(s);
    }

    /** A persisted student with sensible defaults; override nothing you don't care about. */
    protected Student persistDefaultStudent() {
        return persistStudent("Ann", "Lee", "ann@example.com",
                "Computer Science", "1 Old Road", "555-0000");
    }

    /** A small, valid-looking PDF upload part under the {@code forms} field. */
    protected MockMultipartFile pdf(String filename) {
        return new MockMultipartFile("forms", filename, "application/pdf",
                "%PDF-1.4 minimal test bytes".getBytes());
    }

    // --- Internal -------------------------------------------------------------

    private void wipeUploads() {
        if (!Files.exists(UPLOADS_ROOT)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(UPLOADS_ROOT)) {
            paths.sorted(Comparator.reverseOrder())
                    .filter(p -> !p.equals(UPLOADS_ROOT))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            throw new UncheckedIOException("Could not clean uploads", e);
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException("Could not walk uploads dir", e);
        }
    }
}
