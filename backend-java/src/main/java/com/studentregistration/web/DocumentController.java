package com.studentregistration.web;

import java.util.List;
import java.util.Map;

import com.studentregistration.dto.DocumentDto;
import com.studentregistration.service.DocumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DocumentController — the document sub-routes under a student:
 * {@code /api/students/{studentId}/documents...}. Mirrors app.py's
 * {@code list_documents}, {@code get_document} and {@code delete_document}.
 */
@RestController
@RequestMapping("/api/students/{studentId}/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /** GET .../documents → 200 JSON array (empty for an unknown student, never 404). */
    @GetMapping
    public List<DocumentDto> list(@PathVariable long studentId) {
        return documentService.listDocuments(studentId);
    }

    /**
     * GET .../documents/{docId} → stream the actual PDF so the browser can display it
     * INLINE (opened in a new tab, not downloaded). This is the only non-JSON response
     * in the API. We set {@code Content-Type: application/pdf} and
     * {@code Content-Disposition: inline}, matching {@code send_from_directory(...,
     * as_attachment=False)}.
     */
    @GetMapping("/{docId}")
    public ResponseEntity<Resource> view(@PathVariable long studentId, @PathVariable long docId) {
        Resource pdf = documentService.loadDocumentFile(studentId, docId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(pdf);
    }

    /**
     * DELETE .../documents/{docId} → 200 with {@code {"deleted": <docId>}}. Returning
     * JSON (rather than a bodiless 204) keeps the frontend's response-parsing uniform,
     * exactly as app.py chose.
     */
    @DeleteMapping("/{docId}")
    public Map<String, Long> delete(@PathVariable long studentId, @PathVariable long docId) {
        long deletedId = documentService.deleteDocument(studentId, docId);
        return Map.of("deleted", deletedId);
    }
}
