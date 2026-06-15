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
 * Update ONLY the address of an existing student.
 * @param {number} id - the student's id
 * @param {string} address - the new address
 * Returns: a Promise resolving to the updated student.
 */
export async function updateStudentAddress(id, address) {
  const response = await fetch(`${BASE_URL}/students/${id}/address`, {
    method: "PUT", // PUT = "update existing data".
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ address }), // shorthand for { address: address }
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.error || "Failed to update address.");
  }
  return data;
}
