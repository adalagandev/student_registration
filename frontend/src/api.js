// api.js — A small module that centralizes all communication with the Python
// backend. Keeping fetch() calls in one place (instead of scattering them
// across components) means the rest of the app just calls clean functions like
// `getStudents()` and never worries about URLs or HTTP details.

// The base URL of our Flask backend. All endpoints hang off this.
const BASE_URL = "http://localhost:5000/api";

// `fetch` is the browser's built-in function for making HTTP requests. It
// returns a Promise (a value that arrives later), so we use async/await to read
// it as if it were synchronous.
//
// `async` marks a function as asynchronous; inside it we can use `await` to pause
// until a Promise resolves. Every async function itself returns a Promise.

/**
 * Fetch the full list of students from the backend.
 * Returns: a Promise that resolves to an array of student objects.
 */
export async function getStudents() {
  const response = await fetch(`${BASE_URL}/students`); // GET is the default method.
  if (!response.ok) {
    // response.ok is false for error status codes (400, 404, 500, ...).
    throw new Error("Failed to load students.");
  }
  return response.json(); // .json() parses the JSON body into a JS array/object.
}

/**
 * Fetch the read-only waitlist from the backend.
 * Returns: a Promise that resolves to an array of { name, email, program, dateAdded }.
 */
export async function getWaitlist() {
  const response = await fetch(`${BASE_URL}/waitlist`); // GET is the default method.
  if (!response.ok) {
    throw new Error("Failed to load waitlist.");
  }
  return response.json();
}

/**
 * Create a new student.
 * @param {object} student - { firstName, lastName, email, program, address }
 * Returns: a Promise resolving to the newly created student (with its id).
 */
export async function createStudent(student) {
  const response = await fetch(`${BASE_URL}/students`, {
    method: "POST", // POST = "create new data".
    headers: { "Content-Type": "application/json" }, // Tell the server we're sending JSON.
    body: JSON.stringify(student), // Convert the JS object into a JSON string.
  });

  // Read the body regardless of success so we can surface server error messages.
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || "Failed to create student.");
  }
  return data;
}

/**
 * Update a student's editable contact fields (email, address, phone).
 * The backend ignores name/program here on purpose.
 * @param {number} id - the student's id
 * @param {object} fields - { email, address, phone }
 * Returns: a Promise resolving to the updated student.
 */
export async function updateStudent(id, fields) {
  const response = await fetch(`${BASE_URL}/students/${id}`, {
    method: "PUT", // PUT = "update existing data".
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(fields),
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || "Failed to update student.");
  }
  return data;
}

/**
 * Get the list of supporting documents already on file for a student.
 * @param {number} id - the student's id
 * Returns: a Promise resolving to an array of { id, name, uploadedAt }.
 */
export async function getStudentDocuments(id) {
  const response = await fetch(`${BASE_URL}/students/${id}/documents`);
  if (!response.ok) {
    throw new Error("Failed to load documents.");
  }
  return response.json();
}

/**
 * Build the direct URL to view/open one document's PDF. We use this as the
 * `href` of a link so the browser can open the file in a new tab.
 * @param {number} studentId
 * @param {number} docId
 * Returns: a string URL.
 */
export function documentUrl(studentId, docId) {
  return `${BASE_URL}/students/${studentId}/documents/${docId}`;
}

/**
 * Permanently delete one supporting document (file + database row).
 * @param {number} studentId
 * @param {number} docId
 * Returns: a Promise resolving when the delete succeeds.
 */
export async function deleteDocument(studentId, docId) {
  const response = await fetch(`${BASE_URL}/students/${studentId}/documents/${docId}`, {
    method: "DELETE", // DELETE = "remove this resource".
  });
  if (!response.ok) {
    const data = await response.json();
    throw new Error(data.error || "Failed to delete document.");
  }
  return response.json();
}

/**
 * Change a student's program by uploading 1-2 supporting PDF forms.
 * @param {number} id - the student's id
 * @param {string} program - the new program name
 * @param {File[]} files - 1 or 2 PDF File objects from a file <input>
 * Returns: a Promise resolving to the updated student.
 */
export async function submitProgramChange(id, program, files) {
  // FormData is the browser object for sending multipart/form-data — the format
  // required when a request includes binary files. We append text fields and
  // files to it just like filling out an HTML form.
  const formData = new FormData();
  formData.append("program", program);
  // Append each file under the SAME field name "forms"; the backend reads them
  // all with request.files.getlist("forms").
  for (const file of files) {
    formData.append("forms", file);
  }

  const response = await fetch(`${BASE_URL}/students/${id}/program-change`, {
    method: "POST",
    // IMPORTANT GOTCHA: do NOT set a "Content-Type" header here. When the body
    // is a FormData object, the browser sets it automatically AND adds the
    // required multipart "boundary" marker. Setting it by hand breaks the upload.
    body: formData,
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || "Failed to change program.");
  }
  return data;
}
