# Student Registration App

A small full-stack app built to **refresh React skills** (and now Java/Spring Boot).
Heavily commented throughout, with a React keyword glossary at the top of
`frontend/src/App.jsx`.

- **Frontend:** React 18 + Vite (plain JavaScript)
- **Backend (active):** Java 17 + Spring Boot + Spring Data JPA + SQLite — in `backend-java/`
- **Backend (legacy reference):** Python + Flask + SQLite — the original, in `backend/`

> **Backend migration (SR-106):** the backend was migrated from Python/Flask to
> Java/Spring Boot. The Java backend is a faithful 1:1 port serving the identical HTTP
> API on port 5000, so the frontend runs unchanged against either one. **Use the Java
> backend going forward;** the Python app is kept only as a reference. Both read the
> same `backend/students.db` and `backend/uploads/`, so run **only one at a time**.

## Features
- Register a student (first name, last name, email, program, address, optional phone)
- View all students in a table, including their registration date
- Tabbed UI to switch between the registration form and the student list
- Edit a student's contact details (email, address, phone) in a modal dialog
- Change a student's program — **gated** behind uploading 1–2 supporting PDF forms (≤25 MB each)
- View and permanently delete a student's uploaded supporting documents
- Dark theme driven by CSS custom properties in `styles.css`

## Project structure
```
student_registration/
├── backend-java/                 # ACTIVE backend — Java 17 + Spring Boot + JPA
│   ├── pom.xml                   # Maven build + dependencies
│   └── src/
│       ├── main/java/com/studentregistration/
│       │   ├── web/              # REST controllers (thin) + error handling
│       │   ├── service/          # business logic, validation, file storage
│       │   ├── repository/       # Spring Data JPA repositories
│       │   ├── entity/           # @Entity classes mapped to the SQLite tables
│       │   └── dto/              # camelCase wire shapes (row_to_dict equivalent)
│       ├── main/resources/application.properties
│       └── test/java/...         # JUnit + MockMvc API-parity suite
├── backend/                      # LEGACY reference — original Flask app
│   ├── app.py                    # Flask REST API + SQLite (well commented)
│   ├── requirements.txt          # Python dependencies
│   ├── students.db               # created automatically; shared with the Java backend
│   └── uploads/                  # uploaded PDFs, stored under uploads/<student_id>/
└── frontend/
    ├── index.html                # the single HTML page that hosts React
    ├── package.json              # npm scripts + dependencies
    ├── vite.config.js            # dev server config
    └── src/
        ├── main.jsx              # React entry point
        ├── App.jsx               # top-level component + state + KEYWORD GLOSSARY
        ├── api.js                # all backend calls live here
        ├── styles.css            # plain CSS (dark theme via :root tokens)
        └── components/
            ├── StudentForm.jsx        # the registration form
            ├── StudentList.jsx        # the student table
            ├── EditStudentModal.jsx   # edit contact details, program change, documents
            └── Modal.jsx              # reusable modal (renders via a portal)
```

## Running it (two terminals)

The frontend expects the backend on **port 5000** (hardcoded in `frontend/src/api.js`).

### 1. Backend — Java (primary)
Requires **JDK 17+** and **Maven**.
```powershell
cd backend-java
mvn spring-boot:run            # serves http://localhost:5000
```
Other commands: `mvn -q -DskipTests package` (build a runnable fat jar into `target/`),
`mvn test` (run the JUnit + MockMvc API-parity suite).

### 2. Frontend
```powershell
cd frontend
npm install                    # one-time: download dependencies
npm run dev                    # serves http://localhost:5173
```

Then open **http://localhost:5173** in your browser. The backend must be running
for data to load and save.

### Backend — Python (legacy, optional)
Only if you want the original Flask app. Stop the Java backend first (they share
port 5000 and the same database/uploads).
```powershell
cd backend
python -m venv venv            # optional but recommended
venv\Scripts\Activate.ps1      # activate the virtual environment (Windows)
pip install -r requirements.txt
python app.py                  # serves http://localhost:5000
```

## More

- **Working in this repo** (architecture, conventions, workflow): see [`CLAUDE.md`](CLAUDE.md).
- **Ticketed backlog / tasks:** see [`bug.md`](bug.md). Work is ticket-driven (`SR-<number>` keys).
