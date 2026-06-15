// main.jsx — The JavaScript entry point of the React app.
//
// WHAT THIS FILE DOES
// This is the very first React code that runs. Its only job is to take our
// top-level <App /> component and "render" (draw) it into the <div id="root">
// element from index.html.

// `import` pulls code in from other files/packages.
import React from "react";
import ReactDOM from "react-dom/client"; // react-dom is what connects React to the browser DOM.
import App from "./App.jsx";             // Our own top-level component.
import "./styles.css";                   // Importing CSS makes Vite include it in the page.

// document.getElementById("root") finds the <div id="root"> in index.html.
// ReactDOM.createRoot(...) creates a React "root" attached to that div.
const root = ReactDOM.createRoot(document.getElementById("root"));

// .render(...) tells React to draw our component tree into the root.
//
// <React.StrictMode> is a development-only wrapper. It does NOT show anything on
// screen; it just runs extra checks and warnings to help you catch bugs (for
// example, it intentionally double-invokes some functions in dev to surface
// side effects). It is removed automatically in production builds.
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
