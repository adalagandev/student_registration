"""
app.py — The Python backend for the Student Registration app.

WHAT THIS MODULE DOES
---------------------
This is a small REST API built with Flask. A "REST API" is just a set of URLs
(called "endpoints") that the React frontend can call over HTTP to read and
write data. This backend is responsible for:

  1. Creating the database tables the first time it runs.
  2. Saving a new student registration          -> POST   /api/students
  3. Returning the full list of students        -> GET    /api/students
  4. Updating email / address / phone           -> PUT    /api/students/<id>
  5. Changing a student's program, which is GATED behind uploading 1-2 PDF
     forms (<=25 MB each)                        -> POST   /api/students/<id>/program-change

We use SQLite for storage. SQLite is a tiny database that lives in a single
file (students.db) right next to this script, so there is nothing extra to
install or run — Python's standard library already knows how to talk to it.

Uploaded PDF forms are saved to disk under backend/uploads/<student_id>/, and a
row describing each file is recorded in the `documents` table.
"""

# ---------------------------------------------------------------------------
# Imports
# ---------------------------------------------------------------------------
import os                            # Filesystem paths + creating the uploads folder.
import sqlite3                       # Python's built-in driver for the SQLite database.
from datetime import datetime       # Timestamp for uploaded documents.
from flask import Flask, request, jsonify, send_from_directory  # Flask helpers; send_from_directory serves files safely.
from flask_cors import CORS                         # CORS lets the browser (different port) call this API.
from werkzeug.utils import secure_filename          # Sanitizes uploaded filenames (strips paths/odd chars).

# ---------------------------------------------------------------------------
# Configuration constants
# ---------------------------------------------------------------------------
MAX_FILE_BYTES = 25 * 1024 * 1024     # 25 MB per individual PDF.
MAX_FILES = 2                          # At most 2 PDFs per program change.
MIN_FILES = 1                          # At least 1 PDF required.
# A hard limit on the WHOLE request body. We allow a little headroom over
# (2 files * 25 MB) for multipart overhead. If exceeded, Flask raises a 413.
MAX_REQUEST_BYTES = 55 * 1024 * 1024

# ---------------------------------------------------------------------------
# App setup
# ---------------------------------------------------------------------------

# Create the Flask application object. `__name__` tells Flask where the app lives
# on disk so it can find related files. This `app` object actually handles
# incoming web requests.
app = Flask(__name__)

# Reject any request whose body is larger than this. Protects us from someone
# uploading a giant file; Flask turns the overflow into a 413 error.
app.config["MAX_CONTENT_LENGTH"] = MAX_REQUEST_BYTES

# Enable CORS (Cross-Origin Resource Sharing). The React dev server runs on
# http://localhost:5173 while this API runs on http://localhost:5000. Browsers
# block requests between different origins by default; CORS tells the browser
# "it's okay, this API trusts that frontend."
CORS(app)

# Build absolute paths so files always land next to this script, regardless of
# the directory we launch from.
BASE_DIR = os.path.dirname(__file__)
DB_PATH = os.path.join(BASE_DIR, "students.db")
UPLOAD_DIR = os.path.join(BASE_DIR, "uploads")


# ---------------------------------------------------------------------------
# Database helpers
# ---------------------------------------------------------------------------

def get_connection():
    """
    Open a new connection to the SQLite database file and return it.

    Opening a fresh connection per request keeps things simple and avoids
    threading issues in a small app like this.
    """
    conn = sqlite3.connect(DB_PATH)
    # row_factory = sqlite3.Row lets us access columns by NAME (row["email"])
    # instead of by position, which makes building dictionaries easy.
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    """
    Create the database tables if they do not exist, and run a tiny migration to
    add the `phone` column to databases that were created before phone existed.

    Runs once on startup. "IF NOT EXISTS" makes it safe to run every time.
    """
    conn = get_connection()

    # The main students table. Note `phone` is included for fresh databases.
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS students (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,  -- unique id, auto-numbered
            first_name  TEXT    NOT NULL,
            last_name   TEXT    NOT NULL,
            email       TEXT    NOT NULL,
            program     TEXT    NOT NULL,                   -- changed only via PDF upload
            address     TEXT    NOT NULL,
            phone       TEXT    NOT NULL DEFAULT ''         -- editable contact number
        )
        """
    )

    # The documents table records every uploaded program-change form.
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS documents (
            id            INTEGER PRIMARY KEY AUTOINCREMENT,
            student_id    INTEGER NOT NULL,
            filename      TEXT    NOT NULL,   -- the name as stored on disk (timestamp-prefixed)
            original_name TEXT,               -- the name the user originally uploaded (for display)
            uploaded_at   TEXT    NOT NULL,
            FOREIGN KEY (student_id) REFERENCES students (id)
        )
        """
    )

    # ---- Tiny migration -------------------------------------------------
    # An existing students.db (created by the earlier version of this app) will
    # NOT have the `phone` column, and CREATE TABLE IF NOT EXISTS won't add it.
    # So we inspect the existing columns and ALTER the table if phone is absent.
    existing_columns = [row["name"] for row in conn.execute("PRAGMA table_info(students)")]
    if "phone" not in existing_columns:
        conn.execute("ALTER TABLE students ADD COLUMN phone TEXT NOT NULL DEFAULT ''")

    # Same idea for the documents table gaining an original_name column.
    doc_columns = [row["name"] for row in conn.execute("PRAGMA table_info(documents)")]
    if "original_name" not in doc_columns:
        conn.execute("ALTER TABLE documents ADD COLUMN original_name TEXT")

    conn.commit()
    conn.close()


def row_to_dict(row):
    """
    Convert one sqlite3.Row into a plain dictionary that jsonify can serialize.

    We expose camelCase keys to the frontend (JS convention) even though the
    database columns use snake_case (SQL convention).
    """
    return {
        "id": row["id"],
        "firstName": row["first_name"],
        "lastName": row["last_name"],
        "email": row["email"],
        "program": row["program"],
        "address": row["address"],
        "phone": row["phone"],
    }


def fetch_student(conn, student_id):
    """Small helper: return one student row (or None) by id."""
    return conn.execute("SELECT * FROM students WHERE id = ?", (student_id,)).fetchone()


# ---------------------------------------------------------------------------
# Routes (the API endpoints)
# ---------------------------------------------------------------------------
# A "route" maps a URL + HTTP method to a Python function. The @app.route(...)
# line above each function is a "decorator" that registers it with Flask.

@app.route("/api/students", methods=["GET"])
def list_students():
    """
    GET /api/students
    Return every student as a JSON array, newest first.
    """
    conn = get_connection()
    rows = conn.execute("SELECT * FROM students ORDER BY id DESC").fetchall()
    conn.close()
    return jsonify([row_to_dict(row) for row in rows])


@app.route("/api/students", methods=["POST"])
def create_student():
    """
    POST /api/students
    Create a new student from a JSON body like:
        { "firstName": "Jane", "lastName": "Doe", "email": "...", "program": "...",
          "address": "...", "phone": "..." }
    Phone is optional; everything else is required.
    """
    data = request.get_json() or {}

    first_name = (data.get("firstName") or "").strip()
    last_name = (data.get("lastName") or "").strip()
    email = (data.get("email") or "").strip()
    program = (data.get("program") or "").strip()
    address = (data.get("address") or "").strip()
    phone = (data.get("phone") or "").strip()  # optional

    # Server-side validation. Never trust the frontend validated for you.
    if not all([first_name, last_name, email, program, address]):
        return jsonify({"error": "First name, last name, email, program and address are required."}), 400

    conn = get_connection()
    # "?" placeholders are parameterized queries: they safely insert values and
    # protect against SQL injection. Never build SQL by string-concatenating input.
    cursor = conn.execute(
        """
        INSERT INTO students (first_name, last_name, email, program, address, phone)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        (first_name, last_name, email, program, address, phone),
    )
    conn.commit()
    new_id = cursor.lastrowid
    row = fetch_student(conn, new_id)
    conn.close()

    return jsonify(row_to_dict(row)), 201  # 201 = "Created".


@app.route("/api/students/<int:student_id>", methods=["PUT"])
def update_student(student_id):
    """
    PUT /api/students/<id>
    Update a student's editable contact fields: email, address, phone.
    Deliberately does NOT touch name or program — those are not editable here
    (program has its own gated endpoint below).
    """
    data = request.get_json() or {}
    email = (data.get("email") or "").strip()
    address = (data.get("address") or "").strip()
    phone = (data.get("phone") or "").strip()  # optional

    if not email or not address:
        return jsonify({"error": "Email and address cannot be empty."}), 400

    conn = get_connection()
    if fetch_student(conn, student_id) is None:
        conn.close()
        return jsonify({"error": "Student not found."}), 404  # 404 = Not Found.

    conn.execute(
        "UPDATE students SET email = ?, address = ?, phone = ? WHERE id = ?",
        (email, address, phone, student_id),
    )
    conn.commit()
    row = fetch_student(conn, student_id)
    conn.close()

    return jsonify(row_to_dict(row))


@app.route("/api/students/<int:student_id>/program-change", methods=["POST"])
def change_program(student_id):
    """
    POST /api/students/<id>/program-change
    Change a student's program. This is gated: the request must be
    multipart/form-data containing:
      • program : the new program name (text field)
      • forms   : 1 or 2 PDF files, each <= 25 MB

    On success we save the PDFs to disk, record them in `documents`, update the
    program, and return the updated student.
    """
    # For multipart uploads, text fields live in request.form and files live in
    # request.files (NOT request.get_json()).
    new_program = (request.form.get("program") or "").strip()
    # getlist returns ALL files sent under the "forms" field name as a list.
    files = request.files.getlist("forms")
    # Some browsers include an empty file entry; filter those out.
    files = [f for f in files if f and f.filename]

    if not new_program:
        return jsonify({"error": "New program name is required."}), 400

    # ---- Validate the uploaded files -----------------------------------
    if len(files) < MIN_FILES or len(files) > MAX_FILES:
        return jsonify({"error": f"Please upload {MIN_FILES} to {MAX_FILES} PDF file(s)."}), 400

    for f in files:
        # Check extension AND mimetype to be reasonably sure it's a PDF.
        is_pdf_name = f.filename.lower().endswith(".pdf")
        is_pdf_type = (f.mimetype == "application/pdf")
        if not (is_pdf_name and is_pdf_type):
            return jsonify({"error": f"'{f.filename}' is not a PDF."}), 400

        # Measure size by seeking to the end of the stream, reading the position,
        # then rewinding to the start so we can still save the file afterwards.
        f.stream.seek(0, os.SEEK_END)
        size = f.stream.tell()
        f.stream.seek(0)
        if size > MAX_FILE_BYTES:
            return jsonify({"error": f"'{f.filename}' exceeds the 25 MB limit."}), 400

    conn = get_connection()
    if fetch_student(conn, student_id) is None:
        conn.close()
        return jsonify({"error": "Student not found."}), 404

    # Save each file under uploads/<student_id>/ and record it in the DB.
    student_folder = os.path.join(UPLOAD_DIR, str(student_id))
    os.makedirs(student_folder, exist_ok=True)
    now = datetime.utcnow().isoformat(timespec="seconds")

    for f in files:
        safe_name = secure_filename(f.filename)  # e.g. "../evil.pdf" -> "evil.pdf"
        # Prefix with a timestamp to avoid overwriting files of the same name.
        stored_name = f"{now.replace(':', '-')}_{safe_name}"
        f.save(os.path.join(student_folder, stored_name))
        conn.execute(
            "INSERT INTO documents (student_id, filename, original_name, uploaded_at) VALUES (?, ?, ?, ?)",
            (student_id, stored_name, safe_name, now),
        )

    # Apply the program change now that the forms are accepted and stored.
    conn.execute(
        "UPDATE students SET program = ? WHERE id = ?",
        (new_program, student_id),
    )
    conn.commit()
    row = fetch_student(conn, student_id)
    conn.close()

    return jsonify(row_to_dict(row))


@app.route("/api/students/<int:student_id>/documents", methods=["GET"])
def list_documents(student_id):
    """
    GET /api/students/<id>/documents
    Return the list of supporting PDF forms uploaded for this student. We expose
    a display name (the original upload name, falling back to the stored name)
    plus the upload timestamp. The actual file is fetched via the route below.
    """
    conn = get_connection()
    rows = conn.execute(
        "SELECT id, filename, original_name, uploaded_at FROM documents "
        "WHERE student_id = ? ORDER BY id DESC",
        (student_id,),
    ).fetchall()
    conn.close()

    return jsonify([
        {
            "id": row["id"],
            # `or` picks original_name when present, else the stored filename.
            "name": row["original_name"] or row["filename"],
            "uploadedAt": row["uploaded_at"],
        }
        for row in rows
    ])


@app.route("/api/students/<int:student_id>/documents/<int:doc_id>", methods=["GET"])
def get_document(student_id, doc_id):
    """
    GET /api/students/<id>/documents/<doc_id>
    Stream the actual PDF file back so the browser can display it. We look up the
    stored filename in the DB (never trusting a filename from the URL), then use
    send_from_directory, which guards against path-traversal tricks.
    """
    conn = get_connection()
    row = conn.execute(
        "SELECT filename FROM documents WHERE id = ? AND student_id = ?",
        (doc_id, student_id),
    ).fetchone()
    conn.close()

    if row is None:
        return jsonify({"error": "Document not found."}), 404

    student_folder = os.path.join(UPLOAD_DIR, str(student_id))
    # mimetype application/pdf + as_attachment=False => the browser opens the PDF
    # inline (in a new tab) instead of forcing a download.
    return send_from_directory(
        student_folder, row["filename"], mimetype="application/pdf", as_attachment=False
    )


@app.route("/api/students/<int:student_id>/documents/<int:doc_id>", methods=["DELETE"])
def delete_document(student_id, doc_id):
    """
    DELETE /api/students/<id>/documents/<doc_id>
    Permanently delete one supporting document: remove the file from disk AND the
    row from the database. This cannot be undone.
    """
    conn = get_connection()
    row = conn.execute(
        "SELECT filename FROM documents WHERE id = ? AND student_id = ?",
        (doc_id, student_id),
    ).fetchone()

    if row is None:
        conn.close()
        return jsonify({"error": "Document not found."}), 404

    # Delete the physical file. We guard with exists() so a missing file (e.g.
    # already deleted) doesn't crash the request — we still want to clear the row.
    file_path = os.path.join(UPLOAD_DIR, str(student_id), row["filename"])
    if os.path.exists(file_path):
        os.remove(file_path)

    # Remove the database record.
    conn.execute("DELETE FROM documents WHERE id = ?", (doc_id,))
    conn.commit()
    conn.close()

    # 200 with a small confirmation body. (204 "No Content" is also common, but
    # returning JSON keeps the frontend's response-parsing consistent.)
    return jsonify({"deleted": doc_id})


# ---------------------------------------------------------------------------
# Error handler for oversized uploads
# ---------------------------------------------------------------------------
# When the whole request body exceeds MAX_CONTENT_LENGTH, Flask raises a 413
# ("Request Entity Too Large"). By default that returns an HTML page; this
# handler turns it into clean JSON the frontend can read.
@app.errorhandler(413)
def too_large(_error):
    return jsonify({"error": "Upload too large. Each PDF must be 25 MB or less."}), 413


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------
# This block only runs when you execute `python app.py` directly.
if __name__ == "__main__":
    init_db()                              # Ensure tables exist / migrate.
    os.makedirs(UPLOAD_DIR, exist_ok=True)  # Ensure the uploads folder exists.
    # debug=True auto-reloads on file changes and shows detailed errors.
    app.run(debug=True, port=5000)
