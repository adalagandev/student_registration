package com.studentregistration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import com.studentregistration.entity.Document;
import com.studentregistration.entity.Student;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * DocumentApiTest — the document sub-routes: list, stream and delete. Parity points
 * verified here: an unknown student LISTS as {@code []} (never 404), the display name
 * falls back to the stored filename, and a document can't be reached under the wrong
 * student id.
 */
class DocumentApiTest extends ApiIntegrationTestBase {

    // ---- GET .../documents ---------------------------------------------------

    @Test
    void listReturnsDocumentsWithIdNameAndUploadedAt() throws Exception {
        Student student = persistDefaultStudent();
        uploadPdf(student.getId(), "transfer.pdf");

        mockMvc.perform(get("/api/students/{id}/documents", student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].name").value("transfer.pdf"))
                .andExpect(jsonPath("$[0].uploadedAt").exists());
    }

    @Test
    void listNameFallsBackToStoredFilenameWhenOriginalNameIsNull() throws Exception {
        // Legacy rows (pre original_name column) have a null display name; the API must
        // fall back to the on-disk filename ("original_name or filename" in Flask).
        Student student = persistDefaultStudent();
        documentRepository.save(new Document(student.getId(),
                "2026-01-01T00-00-00_stored.pdf", null, "2026-01-01T00:00:00"));

        mockMvc.perform(get("/api/students/{id}/documents", student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("2026-01-01T00-00-00_stored.pdf"));
    }

    @Test
    void listUnknownStudentReturnsEmptyArrayNot404() throws Exception {
        mockMvc.perform(get("/api/students/{id}/documents", 999999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    // ---- GET .../documents/{docId} ------------------------------------------

    @Test
    void getDocumentStreamsPdfContentType() throws Exception {
        Student student = persistDefaultStudent();
        Document doc = uploadPdf(student.getId(), "transfer.pdf");

        mockMvc.perform(get("/api/students/{sid}/documents/{did}",
                        student.getId(), doc.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void getDocumentUnderWrongStudentReturns404() throws Exception {
        // The document belongs to studentA; requesting it under studentB must 404 (the
        // row is matched on BOTH ids), not silently serve another student's file.
        Student owner = persistStudent("Ann", "Lee", "ann@x.com", "CS", "1 St", "");
        Student other = persistStudent("Bob", "Ng", "bob@x.com", "Maths", "2 Ave", "");
        Document doc = uploadPdf(owner.getId(), "transfer.pdf");

        mockMvc.perform(get("/api/students/{sid}/documents/{did}",
                        other.getId(), doc.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Document not found."));
    }

    // ---- DELETE .../documents/{docId} ---------------------------------------

    @Test
    void deleteRemovesRowAndFileAndReturnsDeletedId() throws Exception {
        Student student = persistDefaultStudent();
        Document doc = uploadPdf(student.getId(), "transfer.pdf");
        Path onDisk = UPLOADS_ROOT.resolve(String.valueOf(student.getId()))
                .resolve(doc.getFilename());
        // Guard: the arrangement actually wrote the file we're about to delete.
        Assertions.assertThat(Files.exists(onDisk)).isTrue();

        mockMvc.perform(delete("/api/students/{sid}/documents/{did}",
                        student.getId(), doc.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(doc.getId().intValue()));

        // The physical file is gone AND the row is gone.
        Assertions.assertThat(Files.exists(onDisk)).isFalse();
        Assertions.assertThat(documentRepository.findById(doc.getId())).isEmpty();
    }

    @Test
    void deleteUnknownDocumentReturns404() throws Exception {
        Student student = persistDefaultStudent();

        mockMvc.perform(delete("/api/students/{sid}/documents/{did}",
                        student.getId(), 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Document not found."));
    }

    // --- Arrangement helper ---------------------------------------------------

    /**
     * Upload one real PDF through the program-change endpoint (so a file lands on disk
     * and a row is created), then return the created Document entity for assertions.
     */
    private Document uploadPdf(long studentId, String filename) throws Exception {
        mockMvc.perform(multipart("/api/students/{id}/program-change", studentId)
                        .file(pdf(filename))
                        .param("program", "Data Science"))
                .andExpect(status().isOk());
        // Newest-first, so the just-uploaded document is element 0.
        return documentRepository.findByStudentIdOrderByIdDesc(studentId).get(0);
    }
}
