package com.studentregistration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import com.studentregistration.entity.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;

/**
 * ProgramChangeApiTest — the gated {@code POST /api/students/{id}/program-change}
 * multipart flow. Program changes require 1–2 PDFs (≤25 MB each); the ORDER of the
 * validation checks is parity-critical and is asserted explicitly below.
 */
class ProgramChangeApiTest extends ApiIntegrationTestBase {

    // ---- Happy paths ---------------------------------------------------------

    @Test
    void changeProgramWithOnePdfUpdatesProgramAndRecordsOneDocument() throws Exception {
        Student student = persistDefaultStudent();

        mockMvc.perform(multipart("/api/students/{id}/program-change", student.getId())
                        .file(pdf("transfer.pdf"))
                        .param("program", "Data Science"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.program").value("Data Science"));

        // One documents row was created for this student.
        org.assertj.core.api.Assertions
                .assertThat(documentRepository.findByStudentIdOrderByIdDesc(student.getId()))
                .hasSize(1);
    }

    @Test
    void changeProgramWithTwoPdfsRecordsTwoDocuments() throws Exception {
        Student student = persistDefaultStudent();

        mockMvc.perform(multipart("/api/students/{id}/program-change", student.getId())
                        .file(pdf("form-a.pdf"))
                        .file(pdf("form-b.pdf"))
                        .param("program", "Physics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.program").value("Physics"));

        org.assertj.core.api.Assertions
                .assertThat(documentRepository.findByStudentIdOrderByIdDesc(student.getId()))
                .hasSize(2);
    }

    // ---- Field / count validation -------------------------------------------

    @Test
    void changeProgramWithoutProgramNameReturns400() throws Exception {
        Student student = persistDefaultStudent();

        // A file is present but the program text field is missing entirely.
        mockMvc.perform(multipart("/api/students/{id}/program-change", student.getId())
                        .file(pdf("transfer.pdf")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("New program name is required."));
    }

    @Test
    void changeProgramWithZeroFilesReturns400() throws Exception {
        Student student = persistDefaultStudent();

        mockMvc.perform(multipart("/api/students/{id}/program-change", student.getId())
                        .param("program", "Data Science"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Please upload 1 to 2 PDF file(s)."));
    }

    @Test
    void changeProgramWithThreeFilesReturns400() throws Exception {
        Student student = persistDefaultStudent();

        mockMvc.perform(multipart("/api/students/{id}/program-change", student.getId())
                        .file(pdf("a.pdf"))
                        .file(pdf("b.pdf"))
                        .file(pdf("c.pdf"))
                        .param("program", "Data Science"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Please upload 1 to 2 PDF file(s)."));
    }

    // ---- Per-file validation -------------------------------------------------

    static Stream<Arguments> nonPdfFiles() {
        // Same "is not a PDF" behaviour reached two ways: wrong extension, wrong type.
        return Stream.of(
                Arguments.of("wrong extension",
                        new MockMultipartFile("forms", "notes.txt", "application/pdf",
                                "not a pdf".getBytes()),
                        "notes.txt"),
                Arguments.of("wrong content-type",
                        new MockMultipartFile("forms", "resume.pdf", "text/plain",
                                "not a pdf".getBytes()),
                        "resume.pdf"));
    }

    @ParameterizedTest(name = "{0} -> 400 not a PDF")
    @MethodSource("nonPdfFiles")
    void changeProgramRejectsNonPdfFile(String label, MockMultipartFile file,
                                        String nameInMessage) throws Exception {
        Student student = persistDefaultStudent();

        mockMvc.perform(multipart("/api/students/{id}/program-change", student.getId())
                        .file(file)
                        .param("program", "Data Science"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("'" + nameInMessage + "' is not a PDF."));
    }

    @Test
    void changeProgramRejectsPdfOverTwentyFiveMegabytes() throws Exception {
        Student student = persistDefaultStudent();

        // 25 MB + 1 byte: a valid PDF name/type so it reaches the SIZE check (which is
        // evaluated after the type check). Still well under the 55 MB servlet cap, so
        // this surfaces as our 400 message, not a servlet-level 413.
        byte[] tooBig = new byte[25 * 1024 * 1024 + 1];
        MockMultipartFile big = new MockMultipartFile("forms", "big.pdf",
                "application/pdf", tooBig);

        mockMvc.perform(multipart("/api/students/{id}/program-change", student.getId())
                        .file(big)
                        .param("program", "Data Science"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("'big.pdf' exceeds the 25 MB limit."));
    }

    // ---- Parity-critical ordering -------------------------------------------

    @Test
    void changeProgramValidatesFilesBeforeCheckingStudentExists() throws Exception {
        // THE parity guard: a bad file for a NON-EXISTENT student must return 400
        // (input validation), NOT 404 (existence), because Flask ran file validation
        // first. If the order ever flips this test goes red.
        long nonExistentStudentId = 999999;
        MockMultipartFile notPdf = new MockMultipartFile("forms", "notes.txt",
                "text/plain", "nope".getBytes());

        mockMvc.perform(multipart("/api/students/{id}/program-change", nonExistentStudentId)
                        .file(notPdf)
                        .param("program", "Data Science"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("'notes.txt' is not a PDF."));
    }

    @Test
    void changeProgramWithValidFilesButUnknownStudentReturns404() throws Exception {
        // The mirror case: once input validation PASSES, an unknown student is a 404.
        long nonExistentStudentId = 999999;

        mockMvc.perform(multipart("/api/students/{id}/program-change", nonExistentStudentId)
                        .file(pdf("transfer.pdf"))
                        .param("program", "Data Science"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Student not found."));
    }
}
