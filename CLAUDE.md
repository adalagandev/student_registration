# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

A small full-stack student-registration app whose real purpose is to **refresh React skills**. The code is intentionally written as study material — see `frontend/src/App.jsx`, which opens with a React keyword/syntax glossary. This shapes how you should edit it (see Conventions).

- **Frontend:** React 18 + Vite, plain JavaScript (no TypeScript).
- **Backend:** Python + Flask + SQLite (single-file DB), no ORM.

## Commands

Two processes, run in separate terminals from their own directories.

**Backend** (from `backend/`):
```powershell
python -m venv venv ; venv\Scripts\Activate.ps1   # optional, first time
pip install -r requirements.txt
python app.py        # serves http://localhost:5000, debug auto-reload on
```

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

**Frontend (`frontend/src/`):**
- `api.js` is the **only** place that talks to the backend — all `fetch` calls live there. Components never call `fetch` directly; add a new function here when adding an endpoint.
- `App.jsx` owns all state (student list, status message, active tab, which student is being edited) and passes data + callbacks down as props. Children are presentational: `StudentForm`, `StudentList`, `EditStudentModal`, and the reusable `Modal`.
- State updates are **immutable** (spread/`.map`/`.filter`, never mutation) so React re-renders — follow this pattern.
- `Modal.jsx` renders via `createPortal` into `document.body`. The edit flow has two independent actions: contact-detail edits (`PUT`) and a gated program change (`POST` multipart). Program-change uses `FormData` and deliberately sets **no `Content-Type` header** (the browser must add the multipart boundary).

## Conventions specific to this repo

- **Match the heavy teaching-comment style.** Existing files explain syntax and "why" at a tutorial level; new code should be commented to the same density, and JS/React keywords are often documented inline. Don't strip these comments when editing.
- **Stable, descriptive `id` attributes** are present on essentially every rendered element (e.g. `tab-register`, `student-row-{id}`, `edit-program-submit-btn`), used as handles to target elements in later requests. When adding markup, give it an id following the existing `block-element` / interpolated-`{id}` naming so the set stays complete and unique.
- Theme is driven by CSS custom properties in `frontend/src/styles.css` (`:root` tokens + layered `--surface`/`--surface-2`/`--surface-3`). Re-theme by changing tokens rather than scattering colors.
   
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
