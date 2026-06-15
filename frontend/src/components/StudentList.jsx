// StudentList.jsx — Renders all students in a table. Editing no longer happens
// inline here; instead each row has an "Edit" button that asks the parent (App)
// to open the edit modal for that student.
//
// Props:
//   • students : the array of student objects to show
//   • onEdit   : function the parent passes in; we call onEdit(student) when the
//                user clicks a row's Edit button.

export default function StudentList({ students, onEdit }) {
  // Empty state: show a friendly message instead of an empty table.
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
            <th>Phone</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {/*
            .map() loops over the array and returns one <tr> per student — the
            standard way to render a list in React. The `key` prop is REQUIRED
            and must be unique + stable per item; the database id is perfect.
          */}
          {students.map((student) => (
            <tr key={student.id}>
              <td>{student.firstName} {student.lastName}</td>
              <td>{student.email}</td>
              <td><span className="pill">{student.program}</span></td>
              <td>{student.address}</td>
              {/* phone may be empty; show a dash as a placeholder when so. */}
              <td>{student.phone || "—"}</td>
              <td>
                {/* Arrow function so onEdit runs on click, not during render.
                    We pass the whole student up so the modal can pre-fill. */}
                <button className="btn btn--small" onClick={() => onEdit(student)}>
                  Edit
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
