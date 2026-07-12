// WaitlistList.jsx — Renders the waitlist in a READ-ONLY table. Unlike
// StudentList, there are no per-row actions (no Edit button, no Actions column):
// the waitlist is display-only for now.
//
// Props:
//   • waitlist : the array of waitlist entries to show. Each entry looks like
//                { name, email, program, dateAdded }.

// Turn a "YYYY-MM-DD" date string into a short, human-friendly date like
// "May 2, 2026". Empty/missing values render as a dash. (Same helper shape as
// StudentList's — kept local so this component stands on its own.)
function formatDate(isoString) {
  if (!isoString) return "—";
  // `new Date(...)` parses the date string; toLocaleDateString formats it using
  // the browser's locale. The options pick a compact "MMM D, YYYY" style.
  return new Date(isoString).toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

export default function WaitlistList({ waitlist }) {
  // Empty state: show a friendly message instead of an empty table.
  if (waitlist.length === 0) {
    return (
      <div className="card" id="waitlist-list-empty">
        <p id="waitlist-list-empty-text">No students are on the waitlist right now.</p>
      </div>
    );
  }

  return (
    <div className="card" id="waitlist-list-card">
      <h2 id="waitlist-list-title">Waitlist</h2>
      <table className="table" id="waitlist-table">
        <thead id="waitlist-table-head">
          <tr id="waitlist-table-header-row">
            <th id="waitlist-th-name">Name</th>
            <th id="waitlist-th-email">Email</th>
            <th id="waitlist-th-program">Program</th>
            <th id="waitlist-th-date-added">Date added</th>
          </tr>
        </thead>
        <tbody id="waitlist-table-body">
          {/*
            The mock waitlist entries have no database id, so we use the array
            INDEX as the React key. That is acceptable ONLY because this list is
            static and read-only (never reordered, inserted into, or filtered) —
            for an editable list a stable unique id would be required instead.
          */}
          {waitlist.map((entry, index) => (
            <tr key={index} id={`waitlist-row-${index}`}>
              <td id={`waitlist-name-${index}`}>{entry.name || "—"}</td>
              <td id={`waitlist-email-${index}`}>{entry.email || "—"}</td>
              <td id={`waitlist-program-${index}`}>
                <span className="pill" id={`waitlist-program-pill-${index}`}>{entry.program}</span>
              </td>
              {/* dateAdded is a "YYYY-MM-DD" string; formatDate handles blanks. */}
              <td id={`waitlist-date-added-${index}`}>{formatDate(entry.dateAdded)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
