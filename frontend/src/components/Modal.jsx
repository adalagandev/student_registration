// Modal.jsx — A reusable, presentational modal dialog.
//
// "Presentational" means it knows nothing about students; it only knows how to
// show whatever you put inside it, on top of the page, with an overlay behind.
//
// KEY REACT CONCEPT: PORTALS
// Normally a component renders into its parent in the page tree. A "portal"
// (ReactDOM.createPortal) lets us render the JSX into a DIFFERENT DOM node —
// here, directly into <body>. This is ideal for modals/tooltips so the dialog
// sits on top of everything and isn't clipped or stacked under a parent's CSS
// (overflow, z-index, transforms). Despite living elsewhere in the DOM, the
// component still behaves like a normal child of where you wrote it (props,
// state, and events all work the same).

import { useEffect } from "react";
import { createPortal } from "react-dom";

// Props:
//   • title    : heading text shown at the top of the dialog
//   • onClose  : function to call when the user wants to close (X / Esc / overlay)
//   • children : the content placed inside the modal (passed between the tags:
//                <Modal>...this is children...</Modal>)
export default function Modal({ title, onClose, children }) {
  // Side effect: listen for the Escape key while the modal is open, and clean
  // the listener up when the modal closes/unmounts.
  useEffect(() => {
    function handleKeyDown(event) {
      if (event.key === "Escape") onClose();
    }
    document.addEventListener("keydown", handleKeyDown);

    // The function you RETURN from useEffect is the "cleanup". React runs it
    // when the component unmounts (or before re-running the effect), so we don't
    // leave a dangling listener behind.
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [onClose]); // re-bind if onClose ever changes.

  // createPortal(whatToRender, whereToRenderIt).
  return createPortal(
    // The overlay is the dim backdrop. Clicking it closes the modal.
    <div className="modal__overlay" onClick={onClose}>
      {/*
        The dialog box itself. We stop click events here from "bubbling up" to
        the overlay — otherwise clicking INSIDE the dialog would also trigger the
        overlay's onClose. stopPropagation() halts that bubbling.
      */}
      <div
        className="modal__dialog"
        role="dialog"
        aria-modal="true"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="modal__header">
          <h2 className="modal__title">{title}</h2>
          <button className="modal__close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>
        <div className="modal__body">{children}</div>
      </div>
    </div>,
    document.body // render the whole thing into <body>.
  );
}
