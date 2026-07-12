# 🐞 Bug Backlog — Student Registration

A set of intentionally-introduced bugs for practice. Each ticket is written
JIRA-style and points you to the exact element using the `id` attributes that
exist in the JSX. Find the element by its id, read the surrounding code, and fix
the behavior so it matches the **Expected** result.

> These bugs are currently live in the working tree. Don't peek at git history
> for the answer — hunt them down by the id. 🙂

| Key | Summary | Component | Difficulty | Status |
|------|---------|-----------|------------|--------|
| SR-101 | "Register student" tab opens the wrong view | App | Easy | ✅ Fixed |
| SR-102 | Phone column never shows a phone number | StudentList | Easy | ✅ Fixed |
| SR-103 | "Register student" button does nothing on click | StudentForm | Medium | 🔲 Open |
| SR-104 | Editing a student's email never saves | EditStudentModal | Medium | 🔲 Open |
| SR-105 | Cannot attach 2 PDFs to a program change | EditStudentModal | Medium | 🔲 Open |
| SR-106 | Migrate the backend to Java 17 (Spring Boot) | Backend | Large | 🔲 Open |
| SR-107 | Agent-driven development: route domain work to specialist agents | Tooling | Medium | 🔲 Open |
| SR-108 | Add ticket-warden agent + commit-msg hook to enforce ticket workflow | Tooling | Medium | 🔲 Open |
| SR-109 | Read-only waitlist tab (mock data from a text file) | Full-stack (frontend + backend-java) | Medium | ✅ Done |
| SR-110 | Maintain API curl-command reference (test-guardian) + generate initial doc | Repo tooling | Low | 🔲 Open |

---

## SR-101 — "Register student" tab opens the student list instead of the form

- **Type:** Bug
- **Priority:** High
- **Difficulty:** Easy
- **Component:** Frontend / Tab navigation
- **Element id:** `tab-register`
- **File:** `frontend/src/App.jsx`
- **Status:** ✅ Fixed
- **Fixed on branch:** `SR-101-Register_student_tab_opens_the_wrong_view`
- **Fixed at:** 2026-06-18 23:13:55 +0200

**Description**
The top tab bar has two tabs. Clicking the **Register student** tab is supposed
to show the registration form, but instead it shows the *Registered students*
table — the same as clicking the other tab. The form becomes unreachable by
clicking, and the tab never highlights as active.

**Steps to reproduce**
1. Open the app (it loads on the Register tab by default).
2. Switch to the **Registered students** tab.
3. Click the **Register student** tab (`#tab-register`).

**Expected:** The registration form is shown and `#tab-register` gets the
active underline.
**Actual:** The student list stays visible and `#tab-register` never looks
active.

**Hint:** Look at what value this button's `onClick` puts into `activeTab`.

---

## SR-102 — Phone column shows a dash for students who *have* a phone

- **Type:** Bug
- **Priority:** Medium
- **Difficulty:** Easy
- **Component:** Frontend / Student table
- **Element id:** `student-phone-<studentId>` (e.g. `student-phone-3`)
- **File:** `frontend/src/components/StudentList.jsx`
- **Status:** ✅ Fixed
- **Fixed on branch:** `SR-102_Phone_column_shows_a_dash_for_students_who`
- **Fixed at:** 2026-06-18 23:30:17 +0200

**Description**
In the students table, the **Phone** cell is backwards. Students who entered a
phone number render an em dash `—`, and students with **no** phone render
nothing at all. The real phone number is never displayed.

**Steps to reproduce**
1. Register a student *with* a phone number.
2. Open the **Registered students** tab and look at that row's phone cell
   (`#student-phone-<id>`).

**Expected:** The phone number shows for students who have one; a `—`
placeholder shows for those who don't.
**Actual:** A `—` shows for students who have a phone; the cell is blank for
those who don't.

**Hint:** This is a one-character logic bug — check the operator used to choose
between the phone value and the `—` fallback.

---

## SR-103 — "Register student" submit button doesn't submit the form

- **Type:** Bug
- **Priority:** High
- **Difficulty:** Medium
- **Component:** Frontend / Registration form
- **Element id:** `register-submit-btn` (inside form `register-form`)
- **File:** `frontend/src/components/StudentForm.jsx`

**Description**
Filling out the registration form and clicking the **Register student** button
does nothing — no student is added and no status message appears. (Curiously,
pressing **Enter** inside a text field still works, which is a strong clue to
the root cause.)

**Steps to reproduce**
1. Go to the Register tab and fill in every required field.
2. Click the **Register student** button (`#register-submit-btn`).

**Expected:** The student is created, the form resets, and the app jumps to the
student list with a confirmation message.
**Actual:** Clicking the button has no effect; the form's `onSubmit` handler
never runs.

**Hint:** A `<button>` inside a `<form>` only triggers the form's submit event
for certain button *types*. Inspect this button's `type` attribute.

---

## SR-104 — Editing a student's email silently fails to save

- **Type:** Bug
- **Priority:** High
- **Difficulty:** Medium
- **Component:** Frontend / Edit student modal
- **Element ids:** `edit-email` (input), `edit-save-btn` (button)
- **File:** `frontend/src/components/EditStudentModal.jsx`

**Description**
In the edit modal you can type a new email into the **Email** field
(`#edit-email`) and the input updates fine. But after clicking **Save changes**
(`#edit-save-btn`), the success message appears yet the student's email is
unchanged. Address and phone edits in the same form *do* save correctly — only
email is dropped.

**Steps to reproduce**
1. Open a student's **Edit** modal.
2. Change the value in `#edit-email`.
3. Click `#edit-save-btn`.
4. Reopen the same student — the email reverted.

**Expected:** The edited email is persisted along with address and phone.
**Actual:** Address and phone persist, but the email stays at its original
value.

**Hint:** Look at the object passed to `onSave` in the save handler. What value
is being sent for `email`?

---

## SR-105 — Program change rejects a valid 2-PDF upload

- **Type:** Bug
- **Priority:** Medium
- **Difficulty:** Medium
- **Component:** Frontend / Program-change file validation
- **Element ids:** `program-forms` (file input), `edit-file-error` (error text),
  `edit-program-submit-btn` (button)
- **File:** `frontend/src/components/EditStudentModal.jsx`

**Description**
The UI says a program change requires **1–2** supporting PDFs, and the backend
accepts up to 2. But the client-side validation rejects a 2-file selection: the
error *"Please choose 1 to 2 PDF file(s)."* appears at `#edit-file-error` and
the submit is blocked. Only a single file is ever accepted, contradicting the
hint text right above the field.

**Steps to reproduce**
1. Open a student's **Edit** modal and scroll to **Change program**.
2. Enter a new program name.
3. Select **2** valid PDF files via `#program-forms`.
4. Observe `#edit-file-error`, or click `#edit-program-submit-btn`.

**Expected:** Selecting 1 *or* 2 valid PDFs passes validation and the program
change can be submitted.
**Actual:** Selecting 2 files fails validation; only 1 file is allowed.

**Hint:** An off-by-one in the file-count check (`validateFiles`). Compare the
comparison operator against `MAX_FILES` with the intended inclusive limit.

---

## SR-106 — Migrate the backend to Java 17 (Spring Boot)

- **Type:** Task / Migration
- **Priority:** Medium
- **Difficulty:** Large
- **Component:** Backend
- **Files:** new `backend-java/` project (Python `backend/` kept as reference)
- **Status:** 🔲 Open

**Description**
Re-implement the existing Python/Flask REST API in Java 17+ using Spring Boot +
Maven + Spring Data JPA, serving on the same `http://localhost:5000/api` so the
React frontend keeps working with **no changes**. This is a faithful port: every
route, status code, JSON field name, validation rule, timestamp format, and
file-storage detail must match `backend/app.py`. The new project lives in
`backend-java/` and reuses the existing `backend/students.db` and
`backend/uploads/`. The Python backend is retained as a reference and retired in a
later ticket.

**Acceptance**
All seven endpoints behave identically to Flask (see the migration plan), every
error path returns `{"error": "..."}` JSON, and the frontend works end-to-end
against the Java backend.

---

## SR-107 — Agent-driven development: route domain work to specialist agents

- **Type:** Task / Tooling
- **Priority:** Medium
- **Component:** Repo tooling (`.claude/agents/`, `CLAUDE.md`)
- **Status:** 🔲 Open

**Description**
Make the specialist subagents the authors of their domains: `service-architect`
(service/business logic), `exception-warden` (error handling), `test-guardian`
(tests). Added a delegation policy + `// @agent: <name>` authorship-stamp
convention to `CLAUDE.md`, reworded each agent's description to lead with
authoring, and documented it in `docs/agent-driven-development.md`.

---

## SR-108 — Add ticket-warden agent + commit-msg hook to enforce ticket workflow

- **Type:** Task / Tooling
- **Priority:** Medium
- **Component:** Repo tooling (`.claude/agents/`, `.githooks/`, `CLAUDE.md`)
- **Status:** 🔲 Open

**Description**
Introduce a `ticket-warden` agent (an "agentic product owner") that runs at the
start of any code-change request to ensure the change is tied to an `SR-<n>`
ticket and a matching branch, and that commits are ticket-prefixed. Move the
detailed branch/commit workflow out of `CLAUDE.md` (leaving a one-line pointer)
into the agent. Add a `.githooks/commit-msg` hook as a hard backstop that rejects
any commit whose message is not `SR-<n>`-prefixed, or that targets `main`/`master`.

**Acceptance**
A commit with a non-`SR-<n>` message (or on `main`) is rejected by the hook; a
`SR-<n> ...` message on a ticket branch passes.

---

## SR-109 — Read-only waitlist tab (mock data from a text file)

- **Type:** Feature
- **Priority:** Medium
- **Component:** Full-stack (frontend + backend-java)
- **Status:** ✅ Done
- **Fixed on branch:** `SR-109-waitlist-tab`
- **Fixed at:** 2026-07-12 23:05:26 +0200

**Description**
Add a read-only **Waitlist** tab showing ~7 mock waitlisted students (fields:
name, email, program, date added). The Java backend reads the mock data from a
bundled text file at the service layer and serves it at `GET /api/waitlist`. The
React frontend adds a third, read-only tab that mirrors the existing
`StudentList` table (no add/edit/delete controls).

**Acceptance**
`GET /api/waitlist` returns 7 entries with `{name, email, program, dateAdded}`;
the UI shows a read-only Waitlist tab (no add/edit/delete); the existing Register
and Registered-students tabs are unaffected.

---

## SR-110 — Maintain API curl-command reference (test-guardian) + generate initial doc

- **Type:** Task / Tooling
- **Priority:** Low
- **Component:** Repo tooling (`.claude/agents/`, `docs/`)
- **Status:** 🔲 Open

**Description**
The `test-guardian` agent now maintains a runnable curl-command reference
covering EVERY API endpoint (old and new) in `docs/api-curl-commands.md`.
Generate the initial version covering the current 8 endpoints (students CRUD +
program-change, documents list/view/delete, waitlist).

**Acceptance**
`docs/api-curl-commands.md` exists with a curl command per current endpoint; the
test-guardian agent instructs maintaining it going forward.

---

### How to verify your fixes

Run the app (`python app.py` in `backend/`, `npm run dev` in `frontend/`) and
walk through each ticket's **Steps to reproduce** — every bug is observable in
the running UI. There is no automated test suite in this project.
