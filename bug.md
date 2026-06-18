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

### How to verify your fixes

Run the app (`python app.py` in `backend/`, `npm run dev` in `frontend/`) and
walk through each ticket's **Steps to reproduce** — every bug is observable in
the running UI. There is no automated test suite in this project.
