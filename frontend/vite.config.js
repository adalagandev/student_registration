// vite.config.js — Configuration for Vite, the tool that runs our dev server
// and bundles the app for production.
//
// We import the official React plugin so Vite understands JSX (the HTML-like
// syntax we write inside .jsx files) and supports React Fast Refresh
// (instant updates in the browser when you save a file).
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// `defineConfig` is just a helper that gives us editor autocomplete for the
// config object. Functionally you could export the plain object directly.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173, // The frontend dev server URL: http://localhost:5173
  },
});
