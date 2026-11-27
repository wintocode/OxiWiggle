# OxiWiggle

*Give your Bitwig a wiggle.*

## Overview

OxiWiggle is a proposed native **Bitwig Studio controller extension** for the **OXI E16**. It aims to turn the E16 into a context-sensitive, bidirectional Bitwig control surface, using the E16's host-driven **Remote Mode** rather than an E16-side Lua script or custom preset.

**Distribution goal:** one installable `.bwextension` file. Users should not have to install a Lua script, import a scene, or run a separate background application. This is a design goal **subject to hardware verification**, not an already-proven guarantee.

## Core user experience

- The E16 follows the relevant selected Bitwig track, device, and Remote Controls page.
- Encoders **1–8** adjust the selected page's eight Bitwig Remote Controls.
- The OLED and LED rings follow Bitwig’s active controls and observed values.

### Current layout

| Encoder | Device | Mixer | Transport |
| --- | --- | --- | --- |
| 1–4 | Remote Controls 1–4 | Volume, Shift-turn for pan, click for mute | Play, stop, record, restart |
| 5–8 | Remote Controls 5–8 | Send amount, Shift-turn to choose send, click for solo | Jog, cues, loop start, loop length |
| 9–12 | Spare | Click for record arm | Tempo, metronome, overdub, automation write |
| 13 | Select page | Spare | Spare |
| 14 | Select device | Spare | Spare |
| 15 | Select track, click to toggle group expansion | Same | Same |
| 16 | Select mode | Same | Same |

Launcher follows Transport on encoder 16. Press 1–8 to launch the corresponding visible scene, press 9 to stop playback, Shift-press 1–8 to record empty slots on armed tracks, turn 13 to scroll one scene (Shift-turn for up to eight, stopping at the last full window), and use 15 for track selection. Press 16 from any mode to display About and the extension version. Any button press closes About and restores the current mode. Transport displays the Bitwig playhead in bars, beats, subdivisions and ticks. [Launcher design](docs/superpowers/specs/2026-11-25-launcher-design.md)

**`PERFORM`** is the user's preferred name for a standard eight-control performance page, **not** a page name that OxiWiggle must hard-code. OxiWiggle should follow ordinary Bitwig Remote Controls pages and investigate whether a button can jump to a named page.

## Architecture

```text
OXI E16 (Remote Mode)
   |  MIDI CC 1–16: relative encoder turns
   |  MIDI notes 0–15: encoder pushes; note 16: Shift
   v
OxiWiggle Java Bitwig Controller Extension (.bwextension)
   |  Bitwig Controller API: cursor track/device, Remote Controls,
   |  parameter observers, selection/navigation and parameter changes
   v
Bitwig Studio

Bitwig parameter/name/selection changes
   -> OxiWiggle -> MIDI SysEx -> E16 OLED and LED rings
```

The extension consumes controller MIDI; it should **not** route the E16's raw controller CC, button notes, or display SysEx into instrument-track MIDI. Ordinary Bitwig parameter automation should remain recordable when automation recording is enabled. Test this explicitly, including any duplicate-input configuration edge cases.

## E16 Remote Mode protocol: supplied reference

The project began with a user-supplied text extract headed **E16 REMOTE CONTROL**. On 2026-11-22 the user supplied the explanatory **OXI E16 Remote Guide**, see the [E16 references](docs/sources.md#oxi-e16). Use that detailed guide for the current-firmware protocol. It corrects several details in the earlier extract; actual hardware behaviour still needs verification.

- Manufacturer/device SysEx prefix: `F0 00 21 5B 02 01`.
- Enter Remote Mode (host → E16): `F0 00 21 5B 02 01 06 55 F7`.
- Entry acknowledgement in the supplied guide: `F0 00 21 5B 02 01 53 F7`, with no payload. The tested SW 1.3.1 hardware instead repeatedly replied `F0 00 21 5B 02 01 06 53 F7`; the implementation accepts these two exact forms.
- Exit Remote Mode (host → E16): `F0 00 21 5B 02 01 06 00 F7`.
- LED: command `01` or prefixed `06 01`; payload contains 5-byte raw records (encoder index, LED index, RGB). RGB channels are 0–255 before packing.
- LED ring: command `04` or prefixed `06 04`; payload contains 7-byte raw records (encoder index 0–15, RGB 0–255, 14-bit ring amount split into 7-bit MSB/LSB parts, bipolar flag).
- OLED framebuffer: command `06 02`; 1024 raw bytes for 128×64 monochrome display.
- OLED labels: command `03` or prefixed `06 03`; 80 raw bytes: **16 bytes for the title plus sixteen 4-byte labels**. The title displays at most 15 characters; its 16th byte is replaced with a terminator. Labels display all four characters.
- OLED scanline: command `05` (optional `06` category), with ACK/NACK.
- OLED rectangle: command `08` (optional `06` category), with ACK/NACK.
- OLED clear: command `07` (optional `06` category), with ACK/NACK.
- Packing is now specified: for each group of up to seven raw bytes, emit an MSB mask first (bit i contains raw byte i's top bit), then each byte masked with `0x7F`. The final group has no padding; an empty payload has no mask. Pack only the payload and concatenate raw records before packing. The guide's known example is `03 C8 96 64 60 39 01` → `06 03 48 16 64 60 39 01`.
- Use matching E16 USB MIDI Port 1 input and output. Enter requires the `06` category outside Remote Mode. The guide says active requests may include or omit `06` and replies remain compact; tested SW 1.3.1 hardware required prefixed M1 label and ring requests and returned a prefixed entry ACK. Exit has no acknowledgement; a category-prefixed entry request may safely be retried according to the guide.
- Encoder turns: MIDI channel 1, CC **1–16**, corresponding to encoders 1–16; clockwise values `01`–`08`, anticlockwise `7F`–`78`, with some acceleration.
- Encoder presses: MIDI channel 1, notes **0–15**, Note On/Off; Shift is note **16**.
- OLED update ACK/NACK handling applies to scanline, rectangle, and clear operations. These replies have six raw payload bytes, unlike the empty entry ACK. Rings, labels, and full framebuffer have no update ACK/NACK; do not wait for one. Hardware behaviour remains unverified.

**Important:** a 14-bit LED-ring amount does **not** by itself prove 14-bit parameter-input precision. Relative encoder increments can be mapped to fine-grained Bitwig parameter changes, but actual resolution and gesture semantics depend on the Bitwig Controller API and require testing.

## OLED label strategy

M1 verified native four-character labels. The current extension renders the OLED through the 128×64 framebuffer, including the eight-control overview, parameter details and Mixer track names.

## Technical and UX requirements

1. **Self-contained:** Java implementation packaged as `.bwextension`; no E16-side Lua or custom scene in the intended workflow.
2. **Bidirectional:** encoder input changes Bitwig parameters; Bitwig parameter changes update E16 rings.
3. **Context-following:** selection changes from Bitwig's UI and other controllers should update the E16 without stale bindings.
4. **Predictable navigation:** track/device/page encoders use relative motion and consistent directions; define wrap/clamp policy after API investigation.
5. **Safe lifecycle:** handle startup, Remote Mode acknowledgement, connection loss/reconnection, and shutdown/exit Remote Mode as supported.
6. **Avoid feedback loops:** observers may update display hardware but must not recursively reapply parameters.
7. **Automation-safe:** controller movements should be recorded as parameter automation when intended; raw MIDI and outbound SysEx should not be recorded into clips as a side effect of the extension.
8. **Graceful absence:** missing device, empty track, empty page, unassigned slot, or disconnected E16 should not crash the extension.
9. **Maintainability:** separate protocol encoding/decoding, Bitwig bindings, navigation, display presentation, and lifecycle management.

## Open questions to resolve experimentally

- Does the E16 enter Remote Mode immediately from its normal power-on state without any device-side setup?
- Which global settings, if any, affect Remote Mode input? On 2026-11-23 the user confirmed that Remote Mode worked after changing normal scene knob/button assignments and preserved those assignments on exit, supporting independence from the tested mappings. The full range of channel/output/acceleration settings, scene values and unsaved edits has not been separately enumerated or verified. See [the M1 hardware record](docs/hardware-tests/m1.md).
- Does hardware match the detailed guide's now-specified 7-bit packing scheme? What are the practical message-size and update-rate limits?
- Which Bitwig Controller API methods and observers support selecting and following tracks, devices, and Remote Controls pages? Can a page be selected by name? Does page selection track the UI's selected page?
- How should relative increments be scaled for fine/coarse control across continuous, discrete, bipolar, and logarithmic parameters? What resolution is actually available?
- Which API gestures or touch methods, if any, are appropriate for automation recording with endless encoders?
- How quickly can the OLED labels be refreshed without visible flicker or dropped SysEx?
- What is the E16's behaviour on disconnection, Bitwig shutdown, and reconnection?
- How should navigation work with nested device chains, layers, and multiple tracks?

## Development milestones

### M0 — Research and build scaffold

Create a minimal Java Bitwig controller extension project with reproducible build, installation instructions, logging, and a testable protocol encoder. Confirm the actual Bitwig API version and supported methods; avoid relying on guessed signatures.

### M1 — Hardware handshake (first go/no-go test)

Detect/open the correct E16 MIDI ports; send Remote Mode entry; receive acknowledgement; set **one LED ring** and **one OLED label**. Verify no E16-side script or preset is necessary. Stop and reassess if this assumption fails.

### M2 — One parameter, end to end

Implementation review passed for the tested `0.0.5-m2` checkpoint. Raw CC capture during turns and edits from a second physical controller remain open hardware checks. [M2 test and review record](docs/hardware-tests/m2.md)

Turn encoder 1 → adjust one Bitwig Remote Control; change that parameter in Bitwig → update the E16 ring. Validate resolution, scaling, feedback, and automation behaviour.

The [M2 design](docs/superpowers/specs/2026-11-23-m2-one-remote-control-design.md) and [implementation plan](docs/superpowers/plans/2026-11-23-m2-one-remote-control-implementation.md) split this into a read-only ring test and a working encoder test. Simon chose to keep continuous sensitivity at 1/256 of the range per unit of movement for M2. For a control that Bitwig identifies as a list, the proposed behaviour is one adjacent choice per turn message.

### M3 — Eight dynamic Remote Controls

The eight-encoder monitor and writable builds passed Simon’s hardware checks. The `0.0.8-m3` checkpoint fixes the review findings for display retries and OLED text. Automated tests and Simon’s hardware recheck passed. The installed file matched the archived checkpoint. [M3 test and review record](docs/hardware-tests/m3.md)

The first M3 build displays the eight slots. The second enables encoder edits after the display mapping has been verified on hardware. [M3 design](docs/superpowers/specs/2026-11-23-m3-eight-remote-controls-design.md) · [Implementation plan](docs/superpowers/plans/2026-11-23-m3-eight-remote-controls-implementation.md)

### M4 — Navigation

The `0.0.18-m4` checkpoint passed the reported startup and navigation recheck. Implementation review found missing punctuation in OLED names and recorded host-specific follow-up tests. [Review findings](docs/hardware-tests/m4.md#implementation-review-of-0018-m4)

Encoder 14 changes the page and updates the eight-control screen immediately. Simon verified the purple-and-orange page ring on the installed `0.0.12-m4-page` checkpoint. [M4 hardware record](docs/hardware-tests/m4.md) Encoders 15 and 16 temporarily show the device or track selection, then restore the controls after a pause. [Proposed M4 design](docs/superpowers/specs/2026-11-23-m4-navigation-design.md) · [Implementation plan](docs/superpowers/plans/2026-11-23-m4-navigation-implementation.md)

### M5 — Complete first-release layout

The first checkpoint, `0.0.19-m5-display`, uses two rows of four control names to mirror the physical knobs. Simon accepted the two-row layout on hardware with the 4×7 font and padded dividers in `0.0.22-m5-narrow`. The reviewed `0.0.23-m5-reviewed` display checkpoint passed Simon’s hardware recheck. [M5 hardware check](docs/hardware-tests/m5.md) [M5 design](docs/superpowers/specs/2026-11-23-m5-control-layout-design.md) · [Implementation plan](docs/superpowers/plans/2026-11-23-m5-control-layout-implementation.md)

Test third-party instruments and HW CV devices. The temporary parameter detail view provides longer names and formatted values.

Evaluate wrapping and truncation on the E16 before deciding whether to add a full-name view or scrolling.

Mixer mode now controls four tracks with volume, pan, sends, mute, solo and record arm. On 2026-11-24 the installed `0.0.32-mixer-targets` matched the archived build. Simon confirmed that changing devices on the same track preserved its selected send. [Mixer hardware record](docs/hardware-tests/mixer.md#0032-mixer-targets) · [Mixer design](docs/superpowers/specs/2026-11-23-mixer-mode-design.md) · [Implementation plan](docs/superpowers/plans/2026-11-23-mixer-mode-implementation.md)

Checkpoint `0.0.40-mixer-alignment` passed the Mixer recovery, immediate volume turn and held-arm reorder checks on hardware. [Hardware record](docs/hardware-tests/mixer.md)

### M6 — Hardening and release

Test disconnections, reopen, project reload, automation, simultaneous control from ROTO-CONTROL, and edge cases. Revisit continuous encoder sensitivity during tuning. Compare the current E16 acceleration with a response that allows finer slow turns and larger fast sweeps. Check stepped controls separately for reliable adjacent choices. Package the `.bwextension`, README, licensing and releases.

**Agreed encoder tuning direction, 2026-11-24**

- Simon chose a fixed relative step of 1/16,384 for the slowest continuous movement. Checkpoint 0.0.38 uses that step for magnitude 1 and keeps the previous response for magnitudes 2–8.
- Simon accepted the fixed encoder response after hardware testing 0.0.38 and dropped the proposed faster-tier preferences.
- For a Remote Control that Bitwig identifies as having a fixed set of choices, keep the existing rule of one adjacent choice per turn message, stopping at each end. With four positions, a clockwise message from position 2 selects position 3 at any speed. Several successive messages can move through several choices. A plug-in that reports a continuous range gives the extension no reliable choice count. [M2 design](docs/superpowers/specs/2026-11-23-m2-one-remote-control-design.md)

The archived probe measured numeric readback on Tool Gain and Serum 2 Main Vol. The next test evaluates the physical transition from the fine step to faster movement. [Measurement and hardware test record](docs/hardware-tests/encoder-tuning.md)

The [encoder tuning design](docs/superpowers/specs/2026-11-24-encoder-tuning-design.md) sets out the four tiers and the rule for stepped controls. The [implementation plan](docs/superpowers/plans/2026-11-24-encoder-tuning-implementation.md) covers the measurement build, shared response and accepted settings.

Consider an opt-in “Show Remote Controls when navigating devices” checkbox in Bitwig’s Controller Preferences. API 25 exposes both a writable Remote Controls visibility flag and a separate device expansion flag. Try setting visibility alone after a confirmed move from knob 14, and verify how collapsed devices behave. Keep the default off and apply the action only on a successful controller-driven move so users can still fold panels manually.

First-run setup follow-ups:

- The extension declares one MIDI input and one output. The initial setup used manual assignment of `OXI E16 Port 1` in both directions on macOS. The 0.0.49-port-pair candidate advertises that observed macOS pair. Verify whether Bitwig fills both selectors on a fresh install, including with Bitwig's built-in E16 integration available. Confirm endpoint names on other platforms and with E16 USB Multi Port disabled before adding those names.
- Simon reported that the add-controller picker offers **OxiWiggle** under OXI, but the resulting Controllers entry resembles **OXI Instruments E16**, with no visible OxiWiggle name. The definition supplies `OxiWiggle` as its extension name and `OXI Instruments` / `E16` as its hardware vendor and model. Verify which fields Bitwig uses for each view and whether users can distinguish the two E16 integrations without changing hardware identification.

The [controller setup design](docs/superpowers/specs/2026-11-24-controller-setup-design.md) and [implementation plan](docs/superpowers/plans/2026-11-24-controller-setup-implementation.md) cover the configured name, the Remote Controls preference and macOS Port 1 detection as three testable checkpoints.

## Out of scope for the initial release

- E16-side Lua scripting or custom presets.
- Arbitrary MIDI-to-track routing.
- Per-plugin hard-coded control layouts.
- Automatic creation of `PERFORM` pages inside every plugin.

## Project naming

Use **OxiWiggle** exactly in user-facing text and the Bitwig extension display name. Suggested repository slug: `oxiwiggle`. The user confirmed GitHub owner `wintocode`; use Java package `io.github.wintocode.oxiwiggle`.

## Project decisions recorded 2026-11-22

- Keep any future GitHub repository private until the user explicitly chooses to release it. No GitHub repository has been created by this research work.
- License OxiWiggle's implementation under MIT. Keep third-party documentation separate from that license.
- Use a small JDK-based build for M0/M1, with no Gradle, Maven, JUnit, or additional runtime libraries proposed. Record and verify the JDK and Bitwig API dependency versions and provenance.
- Ask the user before installing any further tool or dependency that might introduce supply chain risk.
- Target the latest stable E16 firmware. OXI's official latest-release link resolved to **1.3.1** on 2026-11-22; verify again before testing and record the exact installed version.
- Record publisher links and tested versions in `docs/sources.md`.
- See [the API investigation and M0/M1 proposal](docs/superpowers/specs/2026-11-22-bitwig-api-and-m1-design.md) for the design decisions and original unverified assumptions; the current implementation status is recorded below.
- The detailed [M0/M1 implementation plan](docs/superpowers/plans/2026-11-22-m0-m1-implementation.md) defines the build, tests, incremental artifacts, and hardware acceptance gates.
- Implementation status, 2026-11-23: M0 loaded on Bitwig 6.1.7/API 25. On E16 SW 1.3.1/HW V5, the M1 entry handshake, prefixed native OLED label and prefixed LED ring were observed on hardware. Encoder turns and buttons did not record into earlier tested armed tracks; USB reconnect restored the OxiWiggle screen, and normal Bitwig quit/reopen restored ordinary/remote screens respectively. An Astra implementation review found a session-restart error path, which is fixed and covered by offline regression tests in the current `0.0.3-m1` build. The user confirmed that build's display/exit recheck, changed normal scene knob/button assignments with preservation on exit, and recovery after starting Bitwig while the E16 was unplugged; no manual controller re-enabling was needed. A new empty-clip test while turning encoder 1 and cycling the extension showed no recorded E16 events while the console logged exit and fresh display sends. Installed and built artifact hashes match. Explicitly unassigned-output and duplicate-route checks are unrecorded, so full M1 acceptance remains pending. See [hardware evidence](docs/hardware-tests/m1.md). The supplied guide's compact reply/request framing differs from the observed prefixed firmware behaviour.
