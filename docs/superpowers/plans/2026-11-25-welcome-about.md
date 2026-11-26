# Welcome and About implementation plan

Use the [agreed screen design](../specs/2026-11-25-welcome-about-design.md) with the existing Java framebuffer and Remote Mode session.

- Exercise the connection overlay, expiry, first turn, About endpoint and return to Launcher in RemoteSessionTest.
- Render one shared framebuffer using the version supplied by OxiWiggleExtensionDefinition. Dismiss the overlay on valid input or a session-scoped timer. Preserve normal ring feedback.
- Verify session, metadata, display and protocol suites, build the extension and document the live trial.
