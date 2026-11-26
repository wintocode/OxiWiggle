# M5 design proposal — eight readable Remote Control names

## First hardware checkpoint

Use the 128 × 64 pixel display already tested for device and track names. Keep the page title across the top. Below it, put knobs 1–4 across the top row and 5–8 across the bottom row. Each name occupies the same position as its physical knob. Values remain on the rings. [M4 baseline](../../hardware-tests/m4.md)

Each column is 32 pixels wide. Draw the title at (2, 1) and the separator at y=10. Start the two rows at y=12 and y=39. Draw up to three lines per cell, eight pixels apart, starting one pixel inside the cell. Five glyphs at six-pixel pitch fit per line. Wrap at spaces where possible and split longer words as needed. If text still remains after the third line, reserve its final glyph for an ellipsis. Keep empty slots in position so every cell retains its physical mapping.

Keep M3's empty marker and numbered fallback for assigned, unnamed slots. Show the existing no-page title and empty slots on an empty track. Knob 14 redraws the layout for the new page. Knobs 15 and 16 still take over the OLED for navigation and restore the current controls after 750 ms. Scrolling is a later checkpoint so the first hardware test can judge what fits at rest. Complete the font’s printable ASCII punctuation, including @ and %, for every pixel screen.

## Alternatives and decision after testing

Simon chose two rows of four for the physical mapping. Revisit after hardware testing if wrapped names are difficult to read. Four rows of two remains an option for wider labels. If names with a common prefix remain ambiguous, try scrolling only the last knob turned, leaving seven stable landmarks. The prototype should establish whether a full-name detail view would be easier to read than moving text. Choose that interaction after the static test.

## Remaining first-release decisions

Knobs 9–13 and spare pushes are open product decisions. Keep knob 16’s existing group toggle. During the display test, note tasks that still require reaching for the mouse. Bring those tasks to the mapping discussion, then choose each control's action, target, feedback, automation and empty-target behaviour before implementing it. Candidates already on the roadmap include selected-track volume, pan and sends. Validate proposed assignments against Bitwig API 25 and the physical layout individually. The optional Bitwig preference for revealing Remote Controls remains a separate proposal. [Project roadmap](../../../PROJECT.md)

[Implementation plan](../plans/2026-11-23-m5-control-layout-implementation.md)
