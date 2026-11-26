# M2 design — one Bitwig Remote Control

Proposed on 2026-11-23. A musician selects a device and a Remote Controls page in Bitwig. Turning physical encoder 1 moves the first slot on that page. Editing that slot with the mouse, playing its automation or moving another controller updates the first E16 ring. An empty slot leaves the control idle. This is the first milestone that can write a Bitwig parameter. [Project milestones](../../../PROJECT.md)

## What the musician sees

| Situation | Proposed M2 behaviour |
| --- | --- |
| Enter Remote Mode | After the entry acknowledgement, show title `OxiWiggle M2`. Show `RC1` under knob 1 when slot 1 is available. Leave the other fifteen labels blank. |
| Slot 1 has a value | Show the current base parameter value as a green arc on the first ring. Minimum is empty, midpoint approximately half full and maximum full. A slow turn makes a small continuous adjustment or selects the adjacent item in a list. |
| Change track, device or page in Bitwig | Follow the new first slot. The next turn applies to that slot. Returning to a previous page shows its current Bitwig value. Any page name works, including `PERFORM`. |
| Empty track, absent device/page or unmapped slot | Show `----` under knob 1 and clear its ring. Ignore turns until Bitwig supplies a usable target. The label distinguishes an empty slot from a valid parameter at minimum. |
| Disable, then re-enable the extension | Return the E16 to normal operation on connected exit. After a fresh entry acknowledgement, redraw from the current selection and value. |

The fixed four-character field keeps this experiment recognisable. Dynamic names and the other seven parameter controls belong to M3. Encoder pushes, Shift and encoders 2–16 have no M2 action. The only installation is the same Java extension on the Bitwig side, using the two `OXI E16 Port 1` endpoints.

## Why this should work

**Observed on hardware.** Bitwig 6.1.7/API 25 and E16 SW 1.3.1/HW V5 produced channel-1 relative CC 1 input while accepting prefixed label and ring frames. The reviewed M1 build passed connected exit, reconnect, changed scene assignments and recording isolation in the tested setup. The deliberately duplicated route and explicitly unassigned output remain open M1 cases. Carry them in the hardware log as hardening work. The tested M1 binary is the rollback point. [M1 hardware evidence](../../hardware-tests/m1.md)

**Checked against API 25.** Create the following proxies once in `init()`. Bitwig uses a *cursor* to let the same API object follow a changing selection.

| API call | Use and evidence |
| --- | --- |
| `host.createCursorTrack(0, 0)`, then `track.createCursorDevice()` | Follow the user’s arranger track and its selected device. Zero sends and launcher scenes are needed. Check the actual selection transitions on the host. [ControllerHost](../../sources.md#bitwig-controller-api) · [CursorTrack](../../sources.md#bitwig-controller-api) |
| `device.createCursorRemoteControlsPage(8).getParameter(0)` | Bitwig documents the single-argument page factory as following the UI. It exposes an ordinary eight-slot page. Only its first `RemoteControl` is observed and adjusted. The named, filtered factory has independent page selection. [Device](../../sources.md#bitwig-controller-api) · [RemoteControlsPage](../../sources.md#bitwig-controller-api) |
| `slot.exists()`, `page.pageCount()`, `page.selectedPageIndex()` | Use existence and a valid page index to detect a candidate target. Check how this firmware/host setup reports a visibly unmapped first slot in the read-only experiment. `isBeingMapped()` denotes an active mapping operation. Names and zero values can be legitimate. [ObjectProxy](../../sources.md#bitwig-controller-api) · [RemoteControl](../../sources.md#bitwig-controller-api) |
| `slot.value()`, `get()`, `inc(double)`, `discreteValueCount()` | Read the normalized base value in 0…1 and increment it by a signed fraction of that range. The step count is −1 for a continuous range. `modulatedValue()` is a separate signal. [Parameter](../../sources.md#bitwig-controller-api) · [RangedValue](../../sources.md#bitwig-controller-api) · [SettableRangedValue](../../sources.md#bitwig-controller-api) |

The proposed cursor chain, existence and value observers, increment and touch calls compiled in a temporary signature check against the project's pinned API 25 JAR. The table uses the documentation shipped with the installed Bitwig 6.1.7. Selection timing, empty-slot reporting, discrete parameter behaviour and automation recording still require live measurements. [API provenance](../../sources.md#bitwig-controller-api)

## Input and automation

Use the existing MIDI callback. Once the session is ready, accept status `B0`, controller number `01`. For a MIDI data value below 64 the signed increment is the value itself. Otherwise subtract 128. The guide defines this encoding over 0…127. For the first build that can edit a parameter, limit writes to the worksheet's turn ranges `01…08` and `78…7F` (±1…8). In the M1 log both directions reached a magnitude of 8. Zero and out-of-range values do nothing. Capture an unfamiliar value for diagnosis if a later firmware emits it. [E16 Remote Guide](../../sources.md#oxi-e16) · [E16 worksheet](../../sources.md#oxi-e16)

- For a continuous control, start with `delta / 256.0` of the range per event. A +1 moves about 0.39% and a +8 moves 3.125%. Simon chose to keep **1/256 for M2**. Keep the denominator in a single named constant.
- For a range reporting *N* ≥ 2 discrete positions, request `sign(delta) / (N - 1.0)`. The proposed behaviour is **one adjacent choice per turn message**, even if E16 acceleration reports +8 or −8. With three waveform choices, the sequence is sine → saw → square, with the ring at minimum, midpoint and maximum. Reverse direction to move back. A two-state switch moves on clockwise and off anticlockwise. Stop at either endpoint. A one-position range has nowhere to move. Suppress writes for zero or unexpected counts.
- Use the count exposed by the Bitwig Remote Control. If a plug-in exposes a waveform selector as continuous (−1), use the continuous path and record what happens on that device. The host may hide the underlying option count. Guessing from the parameter name or printed value could select the wrong item.
- Send one relative `inc` to the current target for each accepted turn. Bitwig applies it to its live value and owns endpoint limiting. Keep no independent knob position. If an edit throws, it may already have taken effect. Log it once and refresh from Bitwig. Replaying that turn could double the edit. Only a value observed from Bitwig can redraw the ring.

**Approach chosen for this proposal.** A callback and explicit increment reuse M1’s input path, permit an exact readiness check and let us test every observed encoder byte. Bitwig also offers a `RelativeHardwareKnob` with a two’s-complement CC matcher. That route is attractive when many hardware controls need native bindings, but needs calibration for a full rotation and a binding that activates and deactivates with Remote Mode. Use a single input path for M2. [Bitwig MIDI matchers](../../sources.md#bitwig-controller-api)

First measure normal `inc` calls with automation writing off, then on in Touch mode. The E16 reports turns and push/release events. A push is a separate gesture from touching a rotating encoder. If turns move the value but fail to record a usable automation curve, try a bounded second experiment with `slot.touch(true)`, one increment and `slot.touch(false)` in `finally` in the same callback. Retain a gesture policy only after recording, playback and selection-change tests pass. An idle, disconnected or reselected controller must leave automation under the user's control. [Parameter automation API](../../sources.md#bitwig-controller-api)

## Feedback and lifecycle

During initialization, attach observers to the selection/existence and value properties used by the adapter. An observer makes its property readable via `get()`. Observer callbacks mark output dirty. On a Bitwig flush, read the *current* proxies, check availability and take one snapshot. This avoids a queue of values from a previous page. Map a finite normalized value to `round(clamp(value, 0, 1) × 16383)`. Black RGB `(0,0,0)` and amount zero clear an unavailable ring. For an available slot reuse encoder index 0, RGB `(0,8,0)` and normal arc mode. The amount is a display field. Physical input resolution and parameter feel are measured separately. [Value observers](../../sources.md#bitwig-controller-api) · [E16 display protocol](../../sources.md#oxi-e16)

Retain M1’s two exact empty entry ACK forms, bounded entry attempts, prefixed output and best-effort exit. Gate both parameter writes and display sends on the active session. On the first flush after ACK, force labels then ring even if the value matches the previous session. On later flushes send only changed fields, at most one labels frame and one ring frame per flush. Several observations before a flush collapse to the latest value. Recheck session identity around sends, including failures. Updates arriving during a flush must survive for the next one. Hardware feedback and parameter observers never write a Bitwig parameter or expose a MIDI input to instrument tracks.

## Proof on the E16

Ship two small local builds. With the first, mouse changes, selection changes and empty slots must display correctly while encoder input is diagnostic. With the second, test both control directions, range endpoints, a stepped parameter, another controller, automation write/playback and an armed clip. Repeat selection and restart while the target value changes. Record project setup, versions, artifact hashes, observations and any untested cases. The implementation plan gives the exact procedure and expected results. [M2 implementation plan](../plans/2026-11-23-m2-one-remote-control-implementation.md)

**Later tuning.** Compare the current response with a curve that gives finer changes on slow turns and larger changes on fast turns. Measure both on the same continuous control and preserve one adjacent choice per message for stepped controls.
