# Student Registration App

A small full-stack app built to **refresh React skills**. Heavily commented
throughout, with a React keyword glossary at the top of `frontend/src/App.jsx`.

- **Frontend:** React 18 + Vite (plain JavaScript)
- **Backend:** Python + Flask + SQLite

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
├── backend/
│   ├── app.py            # Flask REST API + SQLite (well commented)
│   ├── requirements.txt  # Python dependencies
│   ├── students.db       # created automatically on first run
│   └── uploads/          # uploaded PDFs, stored under uploads/<student_id>/
└── frontend/
    ├── index.html        # the single HTML page that hosts React
    ├── package.json      # npm scripts + dependencies
    ├── vite.config.js    # dev server config
    └── src/
        ├── main.jsx           # React entry point
        ├── App.jsx            # top-level component + state + KEYWORD GLOSSARY
        ├── api.js             # all backend calls live here
        ├── styles.css         # plain CSS (dark theme via :root tokens)
        └── components/
            ├── StudentForm.jsx        # the registration form
            ├── StudentList.jsx        # the student table
            ├── EditStudentModal.jsx   # edit contact details, program change, documents
            └── Modal.jsx              # reusable modal (renders via a portal)
```

## Running it (two terminals)

### 1. Backend
```powershell
cd backend
python -m venv venv            # optional but recommended
venv\Scripts\Activate.ps1      # activate the virtual environment (Windows)
pip install -r requirements.txt
python app.py                  # serves http://localhost:5000
```

### 2. Frontend
```powershell
cd frontend
npm install                    # one-time: download dependencies
npm run dev                    # serves http://localhost:5173
```

Then open **http://localhost:5173** in your browser. The backend must be running
for data to load and save.
```
