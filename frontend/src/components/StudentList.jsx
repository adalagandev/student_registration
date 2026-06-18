// StudentList.jsx — Renders all students in a table. Editing no longer happens
// inline here; instead each row has an "Edit" button that asks the parent (App)
// to open the edit modal for that student.
//
// Props:
//   • students : the array of student objects to show
//   • onEdit   : function the parent passes in; we call onEdit(student) when the
//                user clicks a row's Edit button.

// Turn the server's ISO timestamp (e.g. "2026-06-15T14:30:00") into a short,
// human-friendly date like "Jun 15, 2026". Older students registered before
// this feature existed have an empty string, so we show a dash for those.
function formatDate(isoString) {
  if (!isoString) return "—";
  // `new Date(...)` parses the ISO string; toLocaleDateString formats it using
  // the browser's locale. The options pick a compact "MMM D, YYYY" style.
  return new Date(isoString).toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

export default function StudentList({ students, onEdit }) {
  // Empty state: show a friendly message instead of an empty table.
  if (students.length === 0) {
    return (
      <div className="card" id="student-list-empty">
        <p id="student-list-empty-text">No students registered yet. Add one above!</p>
      </div>
    );
  }

  return (
    <div className="card" id="student-list-card">
      <h2 id="student-list-title">Registered students</h2>
      <table className="table" id="student-table">
        <thead id="student-table-head">
          <tr id="student-table-header-row">
            <th id="student-th-name">Name</th>
            <th id="student-th-email">Email</th>
            <th id="student-th-program">Program</th>
            <th id="student-th-address">Address</th>
            <th id="student-th-phone">Phone</th>
            <th id="student-th-registered">Registered</th>
            <th id="student-th-actions">Actions</th>
          </tr>
        </thead>
        <tbody id="student-table-body">
          {/*
            .map() loops over the array and returns one <tr> per student — the
            standard way to render a list in React. The `key` prop is REQUIRED
            and must be unique + stable per item; the database id is perfect.
          */}
          {students.map((student) => (
            <tr key={student.id} id={`student-row-${student.id}`}>
              <td id={`student-name-${student.id}`}>{student.firstName} {student.lastName}</td>
              <td id={`student-email-${student.id}`}>{student.email}</td>
              <td id={`student-program-${student.id}`}><span className="pill" id={`student-program-pill-${student.id}`}>{student.program}</span></td>
              <td id={`student-address-${student.id}`}>{student.address}</td>
              {/* phone may be empty; show a dash as a placeholder when so. */}
              <td id={`student-phone-${student.id}`}>{student.phone && "—"}</td>
              {/* registeredAt comes from the server; formatDate handles blanks. */}
              <td id={`student-registered-${student.id}`}>{formatDate(student.registeredAt)}</td>
              <td id={`student-actions-${student.id}`}>
                {/* Arrow function so onEdit runs on click, not during render.
                    We pass the whole student up so the modal can pre-fill. */}
                <button
                  id={`student-edit-btn-${student.id}`}
                  className="btn btn--small"
                  onClick={() => onEdit(student)}
                >
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
