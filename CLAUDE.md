# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

A small full-stack student-registration app whose real purpose is to **refresh React skills**. The code is intentionally written as study material — see `frontend/src/App.jsx`, which opens with a React keyword/syntax glossary. This shapes how you should edit it (see Conventions).

- **Frontend:** React 18 + Vite, plain JavaScript (no TypeScript).
- **Backend:** Python + Flask + SQLite (single-file DB), no ORM. **Being migrated to Java** (SR-106): a Spring Boot + JPA port lives in `backend-java/` and serves the identical API on port 5000. The Python `backend/` is retained as reference until Java parity is fully signed off; both read the same `backend/students.db` and `backend/uploads/`, so run only ONE backend at a time.

## Commands

Two processes, run in separate terminals from their own directories.

**Backend — Python** (from `backend/`):
```powershell
python -m venv venv ; venv\Scripts\Activate.ps1   # optional, first time
pip install -r requirements.txt
python app.py        # serves http://localhost:5000, debug auto-reload on
```

**Backend — Java** (from `backend-java/`, needs JDK 17+ and Maven):
```powershell
mvn spring-boot:run              # serves http://localhost:5000 (working dir is backend-java/)
mvn -q -DskipTests package       # build a runnable fat jar into target/
```
Run either the Python OR the Java backend, not both — they share port 5000 and the same `backend/students.db` + `backend/uploads/`. Reusing the DB is why `mvn spring-boot:run` (which uses `backend-java/` as its working directory) resolves the `../backend/students.db` path in `application.properties`.

**Frontend** (from `frontend/`):
```powershell
npm install          # first time
npm run dev          # serves http://localhost:5173 (Vite)
npm run build        # production build into frontend/dist/
npm run preview      # serve the built dist/ locally
```

The frontend expects the backend running on port 5000 (hardcoded in `frontend/src/api.js`). There is **no test runner and no linter** configured — do not invent `npm test`/`npm run lint`.

## Architecture

**Backend (`backend/app.py`) — a single file holds everything:**
- One Flask module with all routes under `/api/...`. SQLite lives in `backend/students.db`, created on startup. A fresh `sqlite3` connection is opened per request (`get_connection`).
- `init_db()` runs on startup and is also the **schema migration path**: it `CREATE TABLE IF NOT EXISTS` and then inspects `PRAGMA table_info` to `ALTER TABLE ... ADD COLUMN` for columns added later (`phone`, `registered_at`, `original_name`). Add new columns this same way, not with a migration tool.
- **Field-name boundary:** the DB uses snake_case; the API exposes camelCase. `row_to_dict()` is the single conversion point — any new student field must be added there and to the SQL.
- Two tables: `students` and `documents` (uploaded program-change PDFs, FK to student). Uploaded files are stored on disk at `backend/uploads/<student_id>/`, timestamp-prefixed via `secure_filename`; the `documents` row records both the stored name and the original display name.
- **Server is the source of truth for validation and for `registered_at`** — never trust client-supplied values. Program changes are gated: `/api/students/<id>/program-change` requires `multipart/form-data` with 1–2 PDFs (≤25 MB each); these limits are duplicated in the frontend for instant feedback but re-checked here.

**Backend (`backend-java/`) — Spring Boot port (SR-106), a faithful re-implementation of the Flask API:**
- Layered, not single-file: `web/` controllers (thin) → `service/` (all business logic + validation) → `repository/` (Spring Data JPA) → `entity/` (`@Entity` mapped to the existing snake_case columns via `@Column(name=...)`). `dto/` holds the wire shapes; `StudentDto.from()` is the camelCase boundary (the `row_to_dict()` equivalent).
- **Schema migration** is Hibernate `ddl-auto=update` (in `application.properties`), replacing the Flask `PRAGMA table_info` + `ALTER TABLE` path. It creates/adds missing tables/columns and leaves existing data untouched.
- **All errors go through `web/GlobalExceptionHandler`**, which emits exactly `{"error": "<msg>"}` — the frontend calls `response.json()` unconditionally and reads `error`, so no error path may return non-JSON. Services throw `web/ApiException(status, msg)` carrying the exact Flask status + message.
- Fidelity details that are easy to break live in `service/`: `Timestamps.nowUtcSeconds()` (UTC, seconds, no `Z`), `FileStorageService.secureFilename()` (werkzeug port) + timestamp-prefixed stored name, and `ProgramChangeService`'s validation ORDER (program/file checks return 400 **before** the student-existence 404). The per-file 25 MB check is done in app code (400), so multipart `max-file-size` is deliberately left at 55 MB — see the note in `application.properties`.

**Frontend (`frontend/src/`):**
- `api.js` is the **only** place that talks to the backend — all `fetch` calls live there. Components never call `fetch` directly; add a new function here when adding an endpoint.
- `App.jsx` owns all state (student list, status message, active tab, which student is being edited) and passes data + callbacks down as props. Children are presentational: `StudentForm`, `StudentList`, `EditStudentModal`, and the reusable `Modal`.
- State updates are **immutable** (spread/`.map`/`.filter`, never mutation) so React re-renders — follow this pattern.
- `Modal.jsx` renders via `createPortal` into `document.body`. The edit flow has two independent actions: contact-detail edits (`PUT`) and a gated program change (`POST` multipart). Program-change uses `FormData` and deliberately sets **no `Content-Type` header** (the browser must add the multipart boundary).

## Conventions specific to this repo

- **Match the heavy teaching-comment style.** Existing files explain syntax and "why" at a tutorial level; new code should be commented to the same density, and JS/React keywords are often documented inline. Don't strip these comments when editing.
- **Stable, descriptive `id` attributes** are present on essentially every rendered element (e.g. `tab-register`, `student-row-{id}`, `edit-program-submit-btn`), used as handles to target elements in later requests. When adding markup, give it an id following the existing `block-element` / interpolated-`{id}` naming so the set stays complete and unique.
- Theme is driven by CSS custom properties in `frontend/src/styles.css` (`:root` tokens + layered `--surface`/`--surface-2`/`--surface-3`). Re-theme by changing tokens rather than scattering colors.
   
## Agent-driven development

Feature and change work is delegated to the specialist subagent that owns the domain — that agent **writes** the code; the main agent coordinates and handles code outside these domains:

- Service / business-logic classes (`service/`, use-cases, DI, transactions, DTO/domain mapping) → **service-architect**
- API error/exception handling, status-code mapping, validation errors → **exception-warden**
- Unit and integration tests → **test-guardian**

A feature that spans domains is split so each agent authors its own layer. Invoke these agents proactively when a prompt lands in their domain — not only for after-the-fact review.

### Authorship stamp

Every class or function an agent authors or substantially rewrites carries an authorship tag on the line directly above its declaration, using the file's line-comment syntax:

```
// @agent: service-architect      (Java / JS)
# @agent: exception-warden        (Python)
```

Keep the tag on edits; change it only when a different agent takes the code over. See `docs/agent-driven-development.md` for rationale and setup details.

## Branch & commit workflow

All work is ticket-driven. Follow this flow for every change:

1. **Every code change must be associated with a ticket.** No commits without a ticket number. If a change doesn't have a ticket yet, create one (e.g. in `bug.md` or your tracker) before writing code. Tickets use the `SR-<number>` key (see `bug.md`).
2. **Branch per ticket.** Create a branch off `main` named with the ticket number prefix followed by a short, kebab-case description of the ticket:
   ```
   git checkout -b SR-104-fix-email-save
   ```
   Format: `SR-<number>-<short-description>`.
3. **Commit messages are ticket-prefixed.** Every commit message starts with the ticket number, followed by an appropriate message:
   ```
   git commit -m "SR-104 persist edited email in save handler"
   ```
   Format: `SR-<number> <what the commit does>`.
