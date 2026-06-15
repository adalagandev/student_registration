"""
app.py — The Python backend for the Student Registration app.

WHAT THIS MODULE DOES
---------------------
This is a small REST API built with Flask. A "REST API" is just a set of URLs
(called "endpoints") that the React frontend can call over HTTP to read and
write data. This backend is responsible for:

  1. Creating the database table the first time it runs.
  2. Saving a new student registration          -> POST   /api/students
  3. Returning the full list of students        -> GET    /api/students
  4. Updating ONLY the address of a student     -> PUT    /api/students/<id>/address

We use SQLite for storage. SQLite is a tiny database that lives in a single
file (students.db) right next to this script, so there is nothing extra to
install or run — Python's standard library already knows how to talk to it.
"""

# ---------------------------------------------------------------------------
# Imports
# ---------------------------------------------------------------------------
import sqlite3                       # Python's built-in driver for the SQLite database.
import os                            # Used to build a file path to the database.
from flask import Flask, request, jsonify  # Flask = the web framework. request/jsonify are helpers.
from flask_cors import CORS         # CORS lets the browser (running on a different port) call this API.

# ---------------------------------------------------------------------------
# App setup
# ---------------------------------------------------------------------------

# Create the Flask application object. `__name__` tells Flask where the app lives
# on disk so it can find related files. This `app` object is the thing that
# actually handles incoming web requests.
app = Flask(__name__)

# Enable CORS (Cross-Origin Resource Sharing). The React dev server runs on
# http://localhost:5173 while this API runs on http://localhost:5000. Browsers
# block requests between different origins by default for security; CORS tells
# the browser "it's okay, this API trusts that frontend."
CORS(app)

# Build an absolute path to the database file so it always lands in the backend
# folder, no matter which directory we launch the app from.
DB_PATH = os.path.join(os.path.dirname(__file__), "students.db")


# ---------------------------------------------------------------------------
# Database helpers
# ---------------------------------------------------------------------------

def get_connection():
    """
    Open a new connection to the SQLite database file and return it.

    Why open a fresh connection per request? SQLite connections are cheap, and
    creating one per request keeps things simple and avoids threading issues in
    a small app like this.
    """
    conn = sqlite3.connect(DB_PATH)
    # By default sqlite3 returns each row as a plain tuple like (1, "Jane", ...).
    # Setting row_factory to sqlite3.Row lets us access columns by NAME
    # (row["first_name"]), which makes it easy to convert rows into dictionaries
    # that turn cleanly into JSON.
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    """
    Create the `students` table if it does not already exist.

    This runs once when the server starts. "IF NOT EXISTS" means it is safe to
    run every time — it will not wipe or duplicate an existing table.
    """
    conn = get_connection()
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS students (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,  -- unique id, auto-numbered
            first_name  TEXT    NOT NULL,                   -- required text field
            last_name   TEXT    NOT NULL,
            email       TEXT    NOT NULL,
            program     TEXT    NOT NULL,                   -- e.g. "Computer Science"
            address     TEXT    NOT NULL                    -- the only field we allow editing later
        )
        """
    )
    conn.commit()  # Save (commit) the change to the file.
    conn.close()   # Always close connections when done with them.


def row_to_dict(row):
    """
    Convert one sqlite3.Row into a normal Python dictionary.

    We do this because `jsonify` knows how to turn dictionaries and lists into
    JSON (the text format the frontend understands), but it does not know how to
    serialize a raw sqlite3.Row object.
    """
    return {
        "id": row["id"],
        "firstName": row["first_name"],   # Note: we expose camelCase keys to the
        "lastName": row["last_name"],     # frontend (JS convention) even though the
        "email": row["email"],            # database columns use snake_case (SQL convention).
        "program": row["program"],
        "address": row["address"],
    }


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

    # Convert each database row into a dictionary, then jsonify the whole list.
    students = [row_to_dict(row) for row in rows]
    return jsonify(students)


@app.route("/api/students", methods=["POST"])
def create_student():
    """
    POST /api/students
    Create a new student. The frontend sends a JSON body like:
        { "firstName": "Jane", "lastName": "Doe", "email": "...", ... }
    """
    # request.get_json() reads the JSON the frontend sent and turns it into a
    # Python dictionary. `or {}` guards against the body being empty/None.
    data = request.get_json() or {}

    # Pull each expected field out of the incoming data. .strip() removes
    # accidental leading/trailing spaces.
    first_name = (data.get("firstName") or "").strip()
    last_name = (data.get("lastName") or "").strip()
    email = (data.get("email") or "").strip()
    program = (data.get("program") or "").strip()
    address = (data.get("address") or "").strip()

    # Basic server-side validation. Never trust that the frontend validated for
    # you — always re-check on the backend. If anything is missing we return a
    # 400 ("Bad Request") status code so the frontend knows it failed.
    if not all([first_name, last_name, email, program, address]):
        return jsonify({"error": "All fields are required."}), 400

    conn = get_connection()
    # The "?" placeholders are parameterized queries. They safely insert the
    # values and protect against SQL injection — never build SQL with string
    # concatenation of user input.
    cursor = conn.execute(
        """
        INSERT INTO students (first_name, last_name, email, program, address)
        VALUES (?, ?, ?, ?, ?)
        """,
        (first_name, last_name, email, program, address),
    )
    conn.commit()
    new_id = cursor.lastrowid  # The id SQLite auto-assigned to the new row.

    # Read the row we just inserted so we can return the complete record.
    row = conn.execute("SELECT * FROM students WHERE id = ?", (new_id,)).fetchone()
    conn.close()

    # 201 means "Created" — the standard status code for a successful POST.
    return jsonify(row_to_dict(row)), 201


@app.route("/api/students/<int:student_id>/address", methods=["PUT"])
def update_address(student_id):
    """
    PUT /api/students/<id>/address
    Update ONLY the address of an existing student. This is the single field the
    app allows editing. `<int:student_id>` captures the number from the URL and
    passes it into this function as `student_id`.
    """
    data = request.get_json() or {}
    new_address = (data.get("address") or "").strip()

    if not new_address:
        return jsonify({"error": "Address cannot be empty."}), 400

    conn = get_connection()

    # Make sure the student actually exists before trying to update.
    existing = conn.execute(
        "SELECT * FROM students WHERE id = ?", (student_id,)
    ).fetchone()
    if existing is None:
        conn.close()
        return jsonify({"error": "Student not found."}), 404  # 404 = Not Found.

    # Update just the address column for this one student.
    conn.execute(
        "UPDATE students SET address = ? WHERE id = ?",
        (new_address, student_id),
    )
    conn.commit()

    # Fetch and return the updated record.
    row = conn.execute(
        "SELECT * FROM students WHERE id = ?", (student_id,)
    ).fetchone()
    conn.close()

    return jsonify(row_to_dict(row))


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------
# This block only runs when you execute `python app.py` directly (not when the
# file is imported by another module).
if __name__ == "__main__":
    init_db()  # Make sure the table exists before we start serving requests.
    # debug=True auto-reloads the server when you edit this file and shows
    # detailed error pages — convenient while learning. port=5000 is the URL
    # the React app will call.
    app.run(debug=True, port=5000)
