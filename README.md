# Student Registration App

A small full-stack app built to **refresh React skills**. Heavily commented
throughout, with a React keyword glossary at the top of `frontend/src/App.jsx`.

- **Frontend:** React 18 + Vite (plain JavaScript)
- **Backend:** Python + Flask + SQLite

## Features
- Register a student (first name, last name, email, program, address)
- View all students in a table
- Edit **only** the address of an existing student

## Project structure
```
student_registration/
├── backend/
│   ├── app.py            # Flask REST API + SQLite (well commented)
│   ├── requirements.txt  # Python dependencies
│   └── students.db       # created automatically on first run
└── frontend/
    ├── index.html        # the single HTML page that hosts React
    ├── package.json      # npm scripts + dependencies
    ├── vite.config.js    # dev server config
    └── src/
        ├── main.jsx           # React entry point
        ├── App.jsx            # top-level component + KEYWORD GLOSSARY
        ├── api.js             # all backend calls live here
        ├── styles.css         # plain CSS
        └── components/
            ├── StudentForm.jsx  # the registration form
            └── StudentList.jsx  # the table + address editing
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
