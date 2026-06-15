// StudentForm.jsx — A component that renders the "register a new student" form.
//
// This is a "controlled form": every input's value lives in React state, and
// React is the single source of truth for what's typed. Each keystroke updates
// state, and state flows back into the input's `value`.

import { useState } from "react";

// A blank form is just an object with empty strings for each field. We keep it
// as a constant so we can reuse it to reset the form after submitting.
const EMPTY_FORM = {
  firstName: "",
  lastName: "",
  email: "",
  program: "",
  address: "",
  phone: "", // optional contact number
};

// `props` arrive as one object. Here we use object DESTRUCTURING in the
// parameter list to pull out just `onAddStudent` directly:
//   function StudentForm({ onAddStudent }) { ... }
// is the same as: function StudentForm(props) { const onAddStudent = props.onAddStudent }
export default function StudentForm({ onAddStudent }) {
  // One state object holds all five field values together.
  const [form, setForm] = useState(EMPTY_FORM);

  // A single change handler shared by every input. `event` is the browser event;
  // event.target is the input that changed, giving us its `name` and `value`.
  function handleChange(event) {
    const { name, value } = event.target; // e.g. name="email", value="a@b.com"
    // Build a new object: copy all existing fields (...form), then overwrite the
    // one that changed. The [name] is a "computed property name" — it uses the
    // value of the `name` variable as the key.
    setForm((previous) => ({ ...previous, [name]: value }));
  }

  // Runs when the form is submitted (Enter key or clicking the button).
  function handleSubmit(event) {
    // Forms reload the page by default when submitted — preventDefault() stops
    // that so React can handle it without a full page refresh.
    event.preventDefault();

    // Hand the typed values up to the parent (App) via the prop callback.
    onAddStudent(form);

    // Reset the form back to blank for the next entry.
    setForm(EMPTY_FORM);
  }

  return (
    // onSubmit wires our handler to the form's submit event.
    <form className="card form" onSubmit={handleSubmit}>
      <h2>Register a student</h2>

      {/* A small reusable pattern: a label + input pair. `htmlFor` links the
          label to the input's id (in JSX it's htmlFor, not the HTML `for`). */}
      <div className="form__row">
        <label htmlFor="firstName">First name</label>
        <input
          id="firstName"
          name="firstName"          // must match the key in our `form` state
          value={form.firstName}    // controlled: value comes FROM state
          onChange={handleChange}   // every keystroke updates state
          required                  // basic built-in browser validation
        />
      </div>

      <div className="form__row">
        <label htmlFor="lastName">Last name</label>
        <input
          id="lastName"
          name="lastName"
          value={form.lastName}
          onChange={handleChange}
          required
        />
      </div>

      <div className="form__row">
        <label htmlFor="email">Email</label>
        <input
          id="email"
          name="email"
          type="email"              // tells the browser to expect an email format
          value={form.email}
          onChange={handleChange}
          required
        />
      </div>

      <div className="form__row">
        <label htmlFor="program">Program</label>
        <input
          id="program"
          name="program"
          value={form.program}
          onChange={handleChange}
          placeholder="e.g. Computer Science"
          required
        />
      </div>

      <div className="form__row">
        <label htmlFor="address">Address</label>
        <input
          id="address"
          name="address"
          value={form.address}
          onChange={handleChange}
          required
        />
      </div>

      <div className="form__row">
        <label htmlFor="phone">Phone</label>
        <input
          id="phone"
          name="phone"
          value={form.phone}
          onChange={handleChange}
          placeholder="(optional)"
          // Note: no `required` here — phone is optional.
        />
      </div>

      <button type="submit" className="btn btn--primary">
        Register student
      </button>
    </form>
  );
}
