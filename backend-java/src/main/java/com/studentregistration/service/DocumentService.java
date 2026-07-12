package com.studentregistration.service;

import java.util.List;

import com.studentregistration.dto.DocumentDto;
import com.studentregistration.entity.Document;
import com.studentregistration.repository.DocumentRepository;
import com.studentregistration.web.ApiException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DocumentService — listing, streaming and deleting a student's uploaded PDFs
 * (app.py's {@code list_documents}, {@code get_document}, {@code delete_document}).
 */
@Service
public class DocumentService {

    private final DocumentRepository documents;
    private final FileStorageService storage;

    public DocumentService(DocumentRepository documents, FileStorageService storage) {
        this.documents = documents;
        this.storage = storage;
    }

    /**
     * GET /api/students/{id}/documents — the student's documents, newest first.
     * Note: an unknown student simply yields an EMPTY list (never a 404), matching
     * the Flask behaviour where the query just returns no rows.
     */
    public List<DocumentDto> listDocuments(long studentId) {
        return documents.findByStudentIdOrderByIdDesc(studentId)
                .stream()
                .map(DocumentDto::from)
                .toList();
    }

    /**
     * Resolve the actual PDF bytes for streaming. We look the row up by BOTH ids so
     * a document can't be fetched under the wrong student, then read the on-disk
     * filename from the DB (never from the URL) — the path-traversal-safe approach
     * app.py used. Missing row → 404.
     */
    public Resource loadDocumentFile(long studentId, long docId) {
        Document doc = documents.findByIdAndStudentId(docId, studentId)
                .orElseThrow(() -> ApiException.notFound("Document not found."));
        return storage.loadAsResource(studentId, doc.getFilename());
    }

    /**
     * DELETE /api/students/{id}/documents/{docId} — remove the file from disk AND the
     * row from the database. Matched on both ids; missing row → 404. Returns the
     * deleted id so the controller can echo {@code {"deleted": <id>}}.
     */
    @Transactional
    public long deleteDocument(long studentId, long docId) {
        Document doc = documents.findByIdAndStudentId(docId, studentId)
                .orElseThrow(() -> ApiException.notFound("Document not found."));

        // Delete the physical file first (ignoring an already-missing file), then the row.
        storage.deleteQuietly(studentId, doc.getFilename());
        documents.delete(doc);
        return docId;
    }
}
