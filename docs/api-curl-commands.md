# API curl commands

> **Generated and maintained by the `test-guardian` agent.** This file is the
> canonical curl reference for the project's HTTP API and covers **every**
> current endpoint (8 total). Keep it in sync whenever a route is added,
> removed, or its request/response contract changes.

- **Base URL:** `http://localhost:5000/api`
- **Backend:** these commands target the **Java (Spring Boot) backend** in
  `backend-java/`, which must be **running** on port 5000 before you run them.
- **Error shape:** every failure returns a single top-level field —
  `{"error": "<message>"}` — produced centrally by `GlobalExceptionHandler`.
- The examples use `-i` on selected calls so you can see the status line; drop
  it for clean output. Replace `{id}`, `{docId}`, and file paths with real
  values.

Endpoint index:

| # | Method | Path | Success |
|---|--------|------|---------|
| 1 | GET    | `/api/students` | 200 |
| 2 | POST   | `/api/students` | 201 |
| 3 | PUT    | `/api/students/{id}` | 200 |
| 4 | POST   | `/api/students/{id}/program-change` | 200 |
| 5 | GET    | `/api/students/{id}/documents` | 200 |
| 6 | GET    | `/api/students/{id}/documents/{docId}` | 200 (PDF) |
| 7 | DELETE | `/api/students/{id}/documents/{docId}` | 200 |
| 8 | GET    | `/api/waitlist` | 200 |

---

## Students

### 1. List all students

```bash
# List every student (newest first) -> 200, JSON array of student objects
curl -i http://localhost:5000/api/students
```

Each element has the shape
`{id, firstName, lastName, email, program, address, phone, registeredAt}`.

### 2. Create a student

```bash
# Create a student -> 201, the created student object (server sets id + registeredAt)
curl -i -X POST http://localhost:5000/api/students \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","program":"Computer Science","address":"12 Analytical Way","phone":"555-0100"}'
```

`phone` is optional; the other five fields are required.

```bash
# Missing a required field (here: address) -> 400
curl -i -X POST http://localhost:5000/api/students \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","program":"Computer Science"}'
# Body: {"error":"First name, last name, email, program and address are required."}
```

### 3. Update a student's contact fields

Only `email`, `address`, and optional `phone` are editable here (name and
program are not changed by this route).

```bash
# Update contact details for student {id} -> 200, the updated student object
curl -i -X PUT http://localhost:5000/api/students/1 \
  -H "Content-Type: application/json" \
  -d '{"email":"ada.new@example.com","address":"99 Difference Engine Rd","phone":"555-0199"}'
```

```bash
# Empty email or address -> 400
curl -i -X PUT http://localhost:5000/api/students/1 \
  -H "Content-Type: application/json" \
  -d '{"email":"","address":"99 Difference Engine Rd"}'
# Body: {"error":"Email and address cannot be empty."}
```

```bash
# Unknown student id -> 404
curl -i -X PUT http://localhost:5000/api/students/999999 \
  -H "Content-Type: application/json" \
  -d '{"email":"ghost@example.com","address":"Nowhere"}'
# Body: {"error":"Student not found."}
```

### 4. Program change (multipart upload)

This is the only **`multipart/form-data`** endpoint. Send a text field
`program` plus **1 or 2** PDF file parts, each named `forms`. Do **not** set
`Content-Type` yourself — let curl add the multipart boundary via `-F`.

```bash
# Change program for student {id}, uploading one PDF -> 200, the updated student
curl -i -X POST http://localhost:5000/api/students/1/program-change \
  -F "program=Data Science" \
  -F "forms=@/path/to/change-form.pdf;type=application/pdf"
```

```bash
# Two PDFs are also allowed (max 2) -> 200
curl -i -X POST http://localhost:5000/api/students/1/program-change \
  -F "program=Data Science" \
  -F "forms=@/path/to/form-a.pdf;type=application/pdf" \
  -F "forms=@/path/to/form-b.pdf;type=application/pdf"
```

```bash
# Missing the program text field -> 400
curl -i -X POST http://localhost:5000/api/students/1/program-change \
  -F "forms=@/path/to/change-form.pdf;type=application/pdf"
# Body: {"error":"New program name is required."}
```

```bash
# Wrong file count (zero files, or more than two) -> 400
curl -i -X POST http://localhost:5000/api/students/1/program-change \
  -F "program=Data Science"
# Body: {"error":"Please upload 1 to 2 PDF file(s)."}
```

```bash
# A non-PDF file part -> 400 (message includes the offending file name)
curl -i -X POST http://localhost:5000/api/students/1/program-change \
  -F "program=Data Science" \
  -F "forms=@/path/to/notes.txt;type=text/plain"
# Body: {"error":"'notes.txt' is not a PDF."}
```

```bash
# A PDF larger than 25 MB -> 400 (per-file limit, message includes the file name)
curl -i -X POST http://localhost:5000/api/students/1/program-change \
  -F "program=Data Science" \
  -F "forms=@/path/to/huge.pdf;type=application/pdf"
# Body: {"error":"'huge.pdf' exceeds the 25 MB limit."}
```

```bash
# Unknown student id -> 404
curl -i -X POST http://localhost:5000/api/students/999999/program-change \
  -F "program=Data Science" \
  -F "forms=@/path/to/change-form.pdf;type=application/pdf"
# Body: {"error":"Student not found."}
```

---

## Documents

Documents live under a student: `/api/students/{id}/documents`.

### 5. List a student's documents

```bash
# List uploaded PDFs for student {id} -> 200, JSON array of {id, name, uploadedAt}
curl -i http://localhost:5000/api/students/1/documents
```

Note: an **unknown student returns `200` with an empty array `[]`**, not a 404.

### 6. View (stream) a document PDF

Returns the raw PDF bytes with `Content-Type: application/pdf` and
`Content-Disposition: inline` — this is the only non-JSON success response.

```bash
# Download document {docId} of student {id} to a file -> 200, application/pdf
curl -o form.pdf http://localhost:5000/api/students/1/documents/5

# Or let curl pick a name from the response headers
curl -OJ http://localhost:5000/api/students/1/documents/5
```

```bash
# Unknown document (or wrong student) -> 404
curl -i http://localhost:5000/api/students/1/documents/999999
# Body: {"error":"Document not found."}
```

### 7. Delete a document

```bash
# Delete document {docId} of student {id} (removes file + row) -> 200
curl -i -X DELETE http://localhost:5000/api/students/1/documents/5
# Body: {"deleted":5}
```

```bash
# Unknown document -> 404
curl -i -X DELETE http://localhost:5000/api/students/1/documents/999999
# Body: {"error":"Document not found."}
```

---

## Waitlist

### 8. List the waitlist

Read-only; there are no create/update/delete verbs on this resource.

```bash
# List waitlist entries -> 200, JSON array of {name, email, program, dateAdded}
curl -i http://localhost:5000/api/waitlist
```

---

## Cross-cutting error responses

These are not endpoints of their own — they are the global error contract
`GlobalExceptionHandler` guarantees for **any** request, verified by
`ErrorShapeApiTest` / `InternalErrorMaskingApiTest` (SR-111).

```bash
# Any URL no controller matches -> 404 in our error shape (not Spring's default page)
curl -i http://localhost:5000/api/bogus
# Body: {"error":"Not found."}
```

```bash
# An unforeseen server-side failure -> 500 with a FIXED, generic body; the raw
# exception message is never echoed (no SQL / paths / class names leak to clients).
# (Not triggerable on purpose via the API — shown here for the expected shape.)
# Body: {"error":"Internal server error."}
```
