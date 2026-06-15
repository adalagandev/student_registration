// StudentList.jsx — Renders all students in a table and lets the user edit ONLY
// the address of each one.
//
// It receives two props from App:
//   • students        : the array of student objects to show
//   • onUpdateAddress : a function to call when an address edit is saved

import { useState } from "react";

export default function StudentList({ students, onUpdateAddress }) {
  // We track WHICH row is currently being edited by storing that student's id.
  // `null` means "no row is in edit mode right now".
  const [editingId, setEditingId] = useState(null);

  // The text currently typed into the address input while editing a row.
  const [draftAddress, setDraftAddress] = useState("");

  // Enter edit mode for one student: remember its id and pre-fill the input
  // with the existing address so the user edits rather than retypes.
  function startEditing(student) {
    setEditingId(student.id);
    setDraftAddress(student.address);
  }

  // Leave edit mode without saving.
  function cancelEditing() {
    setEditingId(null);
    setDraftAddress("");
  }

  // Save the edited address by calling the parent handler, then exit edit mode.
  function saveEditing(id) {
    onUpdateAddress(id, draftAddress);
    cancelEditing();
  }

  // Handle the empty case: if there are no students, show a friendly message
  // instead of an empty table. `return` here exits the function early.
  if (students.length === 0) {
    return (
      <div className="card">
        <p>No students registered yet. Add one above!</p>
      </div>
    );
  }

  return (
    <div className="card">
      <h2>Registered students</h2>
      <table className="table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Email</th>
            <th>Program</th>
            <th>Address</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {/*
            .map() loops over the array and returns one <tr> per student.
            This is THE standard way to render a list in React.

            The `key` prop is REQUIRED on list items. React uses it to tell rows
            apart efficiently when the list changes. It must be unique and stable
            per item — the database id is perfect. Never use the array index if
            the list can reorder.
          */}
          {students.map((student) => {
            // Is THIS row the one being edited right now?
            const isEditing = student.id === editingId;

            return (
              <tr key={student.id}>
                <td>{student.firstName} {student.lastName}</td>
                <td>{student.email}</td>
                <td>{student.program}</td>

                {/* The address cell switches between display and edit modes. */}
                <td>
                  {isEditing ? (
                    // EDIT MODE: a controlled input bound to draftAddress.
                    <input
                      value={draftAddress}
                      onChange={(event) => setDraftAddress(event.target.value)}
                      autoFocus
                    />
                  ) : (
                    // DISPLAY MODE: just show the address text.
                    student.address
                  )}
                </td>

                {/* The actions cell shows different buttons per mode. */}
                <td>
                  {isEditing ? (
                    // <> ... </> is a Fragment: groups two buttons without an
                    // extra wrapper element.
                    <>
                      <button
                        className="btn btn--small btn--primary"
                        // Arrow function so saveEditing runs on click (not now).
                        onClick={() => saveEditing(student.id)}
                      >
                        Save
                      </button>
                      <button
                        className="btn btn--small"
                        onClick={cancelEditing}
                      >
                        Cancel
                      </button>
                    </>
                  ) : (
                    <button
                      className="btn btn--small"
                      onClick={() => startEditing(student)}
                    >
                      Edit address
                    </button>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
