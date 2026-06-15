// App.jsx — The top-level component that ties the whole UI together.
//
// =====================================================================
//  REACT KEYWORD / SYNTAX GLOSSARY  (read this first to refresh!)
// =====================================================================
//
//  • Component        : A JavaScript function that returns UI (JSX). Component
//                       names are Capitalized (App, StudentForm). You use them
//                       like HTML tags: <App />.
//
//  • JSX              : The HTML-looking syntax inside the return(...). It is
//                       NOT a string and NOT real HTML — Vite/Babel compiles it
//                       into JavaScript function calls. Because of that, some
//                       attributes differ from HTML: `class` -> `className`,
//                       `for` -> `htmlFor`.
//
//  • {curlyBraces}    : Inside JSX, {} means "switch back to JavaScript". You
//                       put any JS expression there: {student.firstName},
//                       {2 + 2}, {someArray.map(...)}.
//
//  • <></>            : A "Fragment". Lets a component return multiple elements
//                       without wrapping them in an extra <div>. <></> is the
//                       short form of <React.Fragment></React.Fragment>.
//
//  • useState         : A "Hook" that gives a component MEMORY. It returns a
//                       pair: [currentValue, functionToUpdateIt]. Calling the
//                       update function re-renders the component with the new
//                       value. Example: const [count, setCount] = useState(0).
//
//  • useEffect        : A Hook for "side effects" — code that talks to the
//                       outside world (fetching data, timers). It runs AFTER the
//                       component renders. The second argument is a "dependency
//                       array": [] means "run once after the first render".
//
//  • props            : The inputs a parent passes down to a child component,
//                       written like HTML attributes: <Child name="Jane" />.
//                       Inside the child they arrive as one object: props.name.
//
//  • Hook rules       : Hooks (use*) must be called at the TOP LEVEL of a
//                       component — never inside loops, conditions, or nested
//                       functions. This is how React keeps track of them.
// =====================================================================

// Import the Hooks we need from React.
import { useState, useEffect } from "react";

// Import the API functions that talk to the Python backend.
import { getStudents, createStudent, updateStudent, submitProgramChange } from "./api.js";

// Import our child components.
import StudentForm from "./components/StudentForm.jsx";
import StudentList from "./components/StudentList.jsx";
import EditStudentModal from "./components/EditStudentModal.jsx";

// The App component. `export default` makes it importable from other files
// (see main.jsx: `import App from "./App.jsx"`).
export default function App() {
  // ---- STATE -------------------------------------------------------------
  // `students` holds the array of students we display. `setStudents` replaces
  // it (which re-renders the table). We start with an empty array [].
  const [students, setStudents] = useState([]);

  // A status message shown to the user (e.g. "Student added!"). Starts empty.
  const [message, setMessage] = useState("");

  // Whether we are currently loading the list (used to show "Loading...").
  const [loading, setLoading] = useState(true);

  // Which student (if any) is currently being edited in the modal. `null` means
  // the modal is closed. When it holds a student object, the modal is open.
  const [editingStudent, setEditingStudent] = useState(null);

  // ---- LOAD DATA ON FIRST RENDER ----------------------------------------
  // useEffect with an empty dependency array [] runs exactly once, right after
  // the component first appears. We use it to fetch the initial student list.
  useEffect(() => {
    refreshStudents();
  }, []); // <-- empty [] = "only on mount" (the first render).

  // Helper that (re)loads students from the backend and stores them in state.
  // Declared as async so we can `await` the network call.
  async function refreshStudents() {
    try {
      setLoading(true);
      const data = await getStudents(); // wait for the API to respond
      setStudents(data);                // save the result -> triggers a re-render
    } catch (error) {
      setMessage(error.message);        // show any error to the user
    } finally {
      setLoading(false);                // always stop the loading indicator
    }
  }

  // ---- EVENT HANDLERS ----------------------------------------------------
  // Called by <StudentForm> when the user submits a new registration.
  async function handleAddStudent(newStudent) {
    try {
      const created = await createStudent(newStudent);
      // Update state immutably: build a NEW array with the new student in front
      // of the existing ones (spread operator `...` copies the old items).
      // React only re-renders when you give it a new array/object, so we never
      // mutate the old one with .push().
      setStudents((previous) => [created, ...previous]);
      setMessage(`Added ${created.firstName} ${created.lastName}.`);
    } catch (error) {
      setMessage(error.message);
    }
  }

  // Helper that swaps one updated student into the list, immutably. .map()
  // returns a NEW array; for the matching id we use the updated record, others
  // stay as-is. Used by both edit actions below.
  function replaceStudent(updated) {
    setStudents((previous) =>
      previous.map((student) => (student.id === updated.id ? updated : student))
    );
  }

  // Called by the edit modal when the user saves contact details.
  async function handleUpdateStudent(id, fields) {
    try {
      const updated = await updateStudent(id, fields);
      replaceStudent(updated);
      setMessage(`Saved changes for ${updated.firstName}.`);
      setEditingStudent(null); // close the modal on success.
    } catch (error) {
      setMessage(error.message);
    }
  }

  // Called by the edit modal when the user submits a program change + PDFs.
  async function handleProgramChange(id, program, files) {
    try {
      const updated = await submitProgramChange(id, program, files);
      replaceStudent(updated);
      setMessage(`Program updated to "${updated.program}" for ${updated.firstName}.`);
      setEditingStudent(null); // close the modal on success.
    } catch (error) {
      setMessage(error.message);
    }
  }

  // ---- RENDER ------------------------------------------------------------
  // Everything below is JSX: the UI this component produces.
  return (
    <div className="page">
      <header className="page__header">
        <h1>Student Registration</h1>
        <p className="subtitle">Register a student, then view and edit them below.</p>
      </header>

      {/* Pass our handler DOWN to the form via a prop named `onAddStudent`.
          The form will call it when submitted. */}
      <StudentForm onAddStudent={handleAddStudent} />

      {/* Conditional rendering: `&&` shows the element only when `message`
          is truthy (non-empty). If message is "", nothing renders here. */}
      {message && <p className="message">{message}</p>}

      {/* The ternary `condition ? a : b` chooses between two outputs.
          While loading we show text; otherwise we show the table. */}
      {loading ? (
        <p>Loading students…</p>
      ) : (
        <StudentList
          students={students}        // data passed down as a prop
          onEdit={setEditingStudent} // clicking "Edit" stores that student -> opens modal
        />
      )}

      {/* Render the modal only when a student is selected for editing.
          `&&` means: if editingStudent is truthy, render the element after it. */}
      {editingStudent && (
        <EditStudentModal
          student={editingStudent}
          onSave={handleUpdateStudent}
          onProgramChange={handleProgramChange}
          onClose={() => setEditingStudent(null)} // close = clear the selection
        />
      )}
    </div>
  );
}
