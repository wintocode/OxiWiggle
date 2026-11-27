# AGENTS.md — OxiWiggle

## Mission

Build **OxiWiggle**, a native Java `.bwextension` that turns the OXI E16 into a bidirectional, context-sensitive control surface for Bitwig Studio using the E16's host-driven Remote Mode. Read `PROJECT.md` before designing or changing functionality.

## Non-negotiable design constraints

- Preserve the exact user-facing capitalization: **OxiWiggle**.
- Target **one self-contained `.bwextension`**. Do not introduce an E16 Lua script, custom preset, separate runtime application, or fork of another controller project without explicit approval.
- Keep Bitwig as the source of truth for selected track/device/page and parameter values.
- Keep raw controller MIDI and outbound display SysEx out of instrument-track recording paths.
- Use Bitwig's supported Controller API; **verify signatures and behaviours** against the actual API/Javadocs and installed Bitwig version before implementation.
- Do not claim 14-bit input precision solely because the E16's LED ring protocol uses a 14-bit value.
- Treat the supplied E16 protocol extract as a reference, not proof that untested behaviour works on hardware.
- Do not guess the 7-bit SysEx packing format. Request the detailed protocol or build an isolated verification experiment.

## Working method

1. Begin by inspecting the repository, build tooling, relevant docs, and available API references.
2. Maintain a short plan and work in small, runnable milestones (see `PROJECT.md`).
3. Prioritise **M1: Remote Mode handshake + one ring + one label** before broad feature development.
4. Separate **verified facts**, **reasonable assumptions**, and **open questions** in engineering notes and user reports.
5. For each milestone, provide reproducible build/install steps and an exact manual hardware test procedure.
6. Do not silently change the proposed encoder layout or scope; flag tradeoffs and ask for a decision where needed.
7. Prefer simple, testable code and explicit lifecycle handling over clever abstractions.
8. When blocked on hardware or undocumented protocol details, implement a mock/test seam, explain what remains unverified, and stop short of claiming success.

## Proposed source structure (adapt as needed)

- `protocol/` — E16 SysEx framing, packing, MIDI input parsing, ACK/NACK.
- `bitwig/` — cursor track/device/page observers, parameter binding and navigation.
- `surface/` — encoder assignments, relative delta handling, gestures and lifecycle.
- `display/` — labels, optional scrolling, ring rendering and update throttling.
- `tests/` — pure-Java protocol/relative-encoder/display unit tests where practical.

Use a Java package rooted in the actual repository owner's namespace. `io.github.oxiwiggle` is **not** automatically a valid owner namespace.

## Important protocol facts

SysEx prefix: `F0 00 21 5B 02 01`.

- Enter Remote Mode: `F0 00 21 5B 02 01 06 55 F7`.
- The supplied guide specifies compact entry ACK `F0 00 21 5B 02 01 53 F7`. Hardware on SW 1.3.1 repeatedly returned `F0 00 21 5B 02 01 06 53 F7`. The tested implementation accepts those two exact payload-free forms.
- On that hardware, category-prefixed labels (`06 03`) and ring (`06 04`) displayed correctly; compact labels and ring requests did not display in the initial combined test. Use the prefixed forms for the tested M1 path. Do not extrapolate to untested commands.
- Exit Remote Mode: `F0 00 21 5B 02 01 06 00 F7`.
- Ring command: `04` (or prefixed `06 04`); records include encoder index 0–15, RGB 0–255 before packing, 14-bit ring amount split into two 7-bit parts, and bipolar flag.
- Labels command: `03` (or prefixed `06 03`); 16-byte title + 16 × 4-byte labels, 7-bit packed. Only 15 title characters are visible.
- Encoder turns: MIDI channel 1, CC 1–16, relative clockwise `01`–`08`, anticlockwise `7F`–`78`.
- Encoder pushes: MIDI channel 1, notes 0–15; Shift note 16.

Consult `PROJECT.md`, `docs/sources.md`, and `docs/hardware-tests/m1.md` for complete command details and observed behaviour. The detailed guide specifies packing: groups of up to seven raw bytes, a leading mask with bit i holding raw byte i's MSB, followed by the low seven bits of each byte; no padding. Its compact ACK/request claims conflict with the observed SW 1.3.1 behaviour. Ring and label commands have no update ACK in the supplied guide; their visible output was verified on hardware.

## Confirmed project decisions

- GitHub owner: `wintocode`; Java package root: `io.github.wintocode.oxiwiggle`.
- The user approved the public repository at `wintocode/OxiWiggle`.
- OxiWiggle's implementation will be MIT licensed; keep third-party reference material's provenance separate.
- Prefer a minimal JDK-based build with checked-in scripts and no Gradle, Maven, or test-framework dependency for M0/M1.
- Ask the user before installing any tool or dependency that might carry supply chain risk. This includes package-manager installs and new downloaded build inputs; inspect and record provenance before use.
- Target the latest stable E16 firmware and record the exact installed version for every hardware test.
- Record publisher links in `docs/sources.md` and keep any vendor snapshots outside the repository.

## First implementation acceptance test

On an E16 connected to Bitwig:

1. Install and select the compiled OxiWiggle `.bwextension`.
2. Confirm E16 Remote Mode entry and acknowledgement without installing anything onto the E16.
3. Set one LED ring to a known amount/colour.
4. Set one native OLED label.
5. Exit or disconnect safely, documenting observed behaviour.

Only after this works should the implementation advance to parameter bindings and navigation.

## Current controller mapping

- Knob 16 selects Device, Mixer, Transport or Launcher. Its press opens About and any button press dismisses it. Knob 15 selects tracks on every page and its click toggles group expansion.
- Device uses knobs 1–8 for Remote Controls, 13 for pages and 14 for devices. Knobs 9–12 are spare in Device.
- Launcher shows eight scenes at a time. Press 1–8 to launch, press 9 to stop playback and Shift-press 1–8 to record empty slots on armed tracks. Recording starts playback from stopped. With no armed tracks, playback stays stopped. Turn 13 to scroll by one scene or Shift-turn to jump up to eight. Clamp at the last full window.
- Transport uses knobs 1–12 for Arranger controls. Shift-click 11/12 toggles punch in/out. See `docs/hardware-tests/transport.md` for the complete map.
- Mixer uses four track columns. The top row turns volume, Shift-turns pan and clicks mute. The middle row turns send amount, Shift-turns send selection and clicks solo. Third-row clicks arm tracks.
- The current OLED uses a 128×64 framebuffer. The native-label command belongs to the M1 proof of concept.

The user's preferred page name `PERFORM` is a convention, **not a hard-coded dependency**.

## Validation and release quality

- Update the README and relevant mapping and hardware notes with each user-facing change. For a public release, attach the tested `OxiWiggle.bwextension` to the matching GitHub release so the README download link serves that build.

- Unit-test protocol framing and parsing, relative CC decoding, parameter clamping, and four-character label windows where possible.
- Test Bitwig mouse edits, automation playback/recording, another controller changing the same parameters, selection changes, empty slots, device chains, reconnects, and shutdown.
- Document which features are hardware-tested and which remain unverified.
- Avoid bundling unlicensed dependencies or copying source from third-party projects without reviewing licenses.
- Do not call a milestone complete merely because code compiles; record the real Bitwig/E16 behaviour observed.

## Communication style

Be concise, technically precise, and candid about uncertainties. Prefer a working, measurable proof of concept over speculative claims or a large untested implementation.
