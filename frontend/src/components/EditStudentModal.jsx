// EditStudentModal.jsx — The contents of the "Edit student" dialog.
//
// It wraps the reusable <Modal> and provides two independent actions:
//   1. Edit contact details (email, address, phone)        -> onSave
//   2. Change the program, gated behind uploading 1-2 PDFs  -> onProgramChange
//
// First/last name and the current program are shown READ-ONLY, matching the
// rule that those can't be freely edited here.

import { useState, useEffect } from "react";
import Modal from "./Modal.jsx";
import { getStudentDocuments, documentUrl, deleteDocument } from "../api.js";

// Client-side limits that MIRROR the backend's validation. We validate here for
// instant feedback, but the server re-checks because client checks can be
// bypassed and must never be trusted alone.
const MAX_FILE_BYTES = 25 * 1024 * 1024; // 25 MB
const MIN_FILES = 1;
const MAX_FILES = 2;

// Props:
//   • student          : the student object being edited
//   • onSave           : (id, { email, address, phone }) => Promise
//   • onProgramChange  : (id, program, files) => Promise
//   • onClose          : () => void  (close the modal)
export default function EditStudentModal({ student, onSave, onProgramChange, onClose }) {
  // ---- Contact-details form state (pre-filled from the student) ----------
  const [email, setEmail] = useState(student.email);
  const [address, setAddress] = useState(student.address);
  const [phone, setPhone] = useState(student.phone || "");

  // ---- Program-change form state ----------------------------------------
  const [newProgram, setNewProgram] = useState("");
  const [files, setFiles] = useState([]);     // array of selected File objects
  const [fileError, setFileError] = useState("");

  // ---- Existing supporting documents ------------------------------------
  // The PDFs already uploaded for this student. Loaded from the backend when
  // the modal opens.
  const [documents, setDocuments] = useState([]);

  // useEffect runs after the first render. The dependency [student.id] means it
  // re-runs if a different student is edited. We fetch this student's documents
  // and store them in state.
  useEffect(() => {
    let cancelled = false; // guard against setting state after unmount
    getStudentDocuments(student.id)
      .then((docs) => {
        if (!cancelled) setDocuments(docs);
      })
      .catch(() => {
        // Non-critical: if the list fails to load, just leave it empty.
      });
    // Cleanup: if the modal closes before the request finishes, ignore the result.
    return () => {
      cancelled = true;
    };
  }, [student.id]);

  // Permanently delete one document. Because this is irreversible, we ask the
  // user to confirm first. window.confirm() shows a native OK/Cancel dialog and
  // returns true only if they click OK.
  async function handleDeleteDocument(doc) {
    const ok = window.confirm(`Permanently delete "${doc.name}"? This cannot be undone.`);
    if (!ok) return;
    try {
      await deleteDocument(student.id, doc.id);
      // Remove it from local state so the list updates instantly without a refetch.
      // .filter() returns a NEW array excluding the deleted document.
      setDocuments((previous) => previous.filter((d) => d.id !== doc.id));
    } catch (error) {
      // Surface the failure in the same inline error area used elsewhere.
      setFileError(error.message);
    }
  }

  // Submit handler for the contact-details section.
  function handleSaveDetails(event) {
    event.preventDefault();
    onSave(student.id, { email, address, phone });
  }

  // Runs whenever the user picks files. event.target.files is a FileList (an
  // array-like object); we convert it to a real array with Array.from so we can
  // use .length, .map, etc. Then we validate immediately.
  function handleFileChange(event) {
    const selected = Array.from(event.target.files);
    setFiles(selected);
    setFileError(validateFiles(selected));
  }

  // Returns an error string, or "" if the selection is valid.
  function validateFiles(selected) {
    if (selected.length < MIN_FILES || selected.length > MAX_FILES) {
      return `Please choose ${MIN_FILES} to ${MAX_FILES} PDF file(s).`;
    }
    for (const file of selected) {
      if (file.type !== "application/pdf") {
        return `"${file.name}" is not a PDF.`;
      }
      if (file.size > MAX_FILE_BYTES) {
        return `"${file.name}" is larger than 25 MB.`;
      }
    }
    return "";
  }

  // Submit handler for the program-change section.
  function handleSubmitProgram(event) {
    event.preventDefault();
    const error = validateFiles(files);
    if (!newProgram.trim()) {
      setFileError("Enter the new program name.");
      return;
    }
    if (error) {
      setFileError(error);
      return;
    }
    onProgramChange(student.id, newProgram.trim(), files);
  }

  return (
    <Modal title={`Edit ${student.firstName} ${student.lastName}`} onClose={onClose}>
      {/* ---- Read-only identity ---- */}
      <div className="readonly-grid">
        <div>
          <span className="readonly-grid__label">Name</span>
          <span className="readonly-grid__value">
            {student.firstName} {student.lastName}
          </span>
        </div>
        <div>
          <span className="readonly-grid__label">Current program</span>
          <span className="pill">{student.program}</span>
        </div>
      </div>

      {/* ---- Section 1: editable contact details ---- */}
      <form className="form" onSubmit={handleSaveDetails}>
        <h3 className="section-title">Contact details</h3>

        <div className="form__row">
          <label htmlFor="edit-email">Email</label>
          <input
            id="edit-email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
        </div>

        <div className="form__row">
          <label htmlFor="edit-address">Address</label>
          <input
            id="edit-address"
            value={address}
            onChange={(event) => setAddress(event.target.value)}
            required
          />
        </div>

        <div className="form__row">
          <label htmlFor="edit-phone">Phone</label>
          <input
            id="edit-phone"
            value={phone}
            onChange={(event) => setPhone(event.target.value)}
            placeholder="(optional)"
          />
        </div>

        <button type="submit" className="btn btn--primary">
          Save changes
        </button>
      </form>

      <hr className="divider" />

      {/* ---- Section 2: view supporting documents already on file ---- */}
      <div className="form">
        <h3 className="section-title">Supporting documents</h3>
        {documents.length === 0 ? (
          <p className="hint">No documents uploaded yet.</p>
        ) : (
          <ul className="doc-list">
            {documents.map((doc) => (
              <li key={doc.id}>
                {/*
                  A normal link to the backend's file endpoint. target="_blank"
                  opens the PDF in a new browser tab; rel="noreferrer" is a small
                  security/privacy best-practice for external/new-tab links.
                */}
                <a href={documentUrl(student.id, doc.id)} target="_blank" rel="noreferrer">
                  {doc.name}
                </a>
                <span className="doc-list__meta">
                  <span className="doc-list__date">{doc.uploadedAt}</span>
                  {/* Destructive action: a delete button styled as danger. */}
                  <button
                    type="button"
                    className="btn btn--small btn--danger"
                    onClick={() => handleDeleteDocument(doc)}
                  >
                    Delete
                  </button>
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>

      <hr className="divider" />

      {/* ---- Section 3: program change (gated behind PDF upload) ---- */}
      <form className="form" onSubmit={handleSubmitProgram}>
        <h3 className="section-title">Change program</h3>
        <p className="hint">
          Changing a program requires {MIN_FILES}–{MAX_FILES} supporting PDF
          form(s), each 25 MB or less.
        </p>

        <div className="form__row">
          <label htmlFor="new-program">New program</label>
          <input
            id="new-program"
            value={newProgram}
            onChange={(event) => setNewProgram(event.target.value)}
            placeholder="e.g. Data Science"
          />
        </div>

        <div className="form__row">
          <label htmlFor="program-forms">Supporting PDF form(s)</label>
          {/* accept=".pdf" hints the file picker; multiple allows 1-2 files. */}
          <input
            id="program-forms"
            type="file"
            accept="application/pdf"
            multiple
            onChange={handleFileChange}
          />
        </div>

        {/* Show the chosen file names so the user can confirm their selection. */}
        {files.length > 0 && (
          <ul className="file-list">
            {files.map((file) => (
              <li key={file.name}>
                {file.name} — {(file.size / (1024 * 1024)).toFixed(1)} MB
              </li>
            ))}
          </ul>
        )}

        {/* Inline validation error (only shows when fileError is non-empty). */}
        {fileError && <p className="error">{fileError}</p>}

        <button type="submit" className="btn btn--primary">
          Submit program change
        </button>
      </form>
    </Modal>
  );
}
