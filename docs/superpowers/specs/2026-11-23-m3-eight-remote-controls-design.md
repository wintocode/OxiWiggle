# M3 design — eight Remote Controls

Proposed for Bitwig 6.1.7, Controller API 25 and E16 SW 1.3.1. A musician selects a track, device and Remote Controls page in Bitwig. The first two rows of the E16 show that page's eight slots in order. Turning a knob changes its slot. The corresponding ring follows Bitwig when the parameter changes from any source. [Project scope](../../../PROJECT.md) · [Reviewed M2 baseline](../../hardware-tests/m2.md)

## What appears on the E16

| Bitwig state | Native OLED | Ring and turn |
| --- | --- | --- |
| Selected page | Page name, up to 15 visible characters. If unnamed, use `Page N` with the one-based page number. | Show each assigned slot as a green arc from minimum to maximum. |
| Assigned slot | A stationary label from the Remote Control name. An unnamed control gets `RC1` through `RC8`. | Use M2's current base parameter value and adjustment rules. |
| Unassigned slot or nonfinite value | `----` | Black ring at zero. Ignore turns. An assigned control at minimum still has its name. |
| No device or valid page | `OxiWiggle` and eight empty-slot markers | Clear all eight managed rings and ignore turns. |

Proposed text treatment uses the first four printable characters of the slot name. For example, “Cutoff” becomes “Cuto”. Use Java's built-in Unicode NFKD normalization, drop combining marks, collapse whitespace to single spaces, trim, and replace any remaining non-ASCII code point with one question mark before clipping. Use the same rules at width 15 for the title. Names with identical prefixes can share a label. Physical position identifies the slot. Test legibility on two real pages before adding an abbreviation scheme. Reserve scrolling for M5. OLED fields 9–16 contain spaces. Encoders 9–16, their rings, pushes and Shift have no M3 assignment.

## One page and one session

Use one track cursor, one device cursor and one UI-following eight-slot page for the entire extension. Expand M2's adapter to hold the eight stable slot proxies from that page. Having eight independent sessions would duplicate entry, retry and exit handling and compete for the single labels frame. There is no page-name filter. Selection and page navigation remain in Bitwig.

Keep the verified chain `host.createCursorTrack(0,0).createCursorDevice().createCursorRemoteControlsPage(8)`. API 25 exposes `page.getName()` and `page.getParameter(i).name()` as observable string values. Register page name, page count, selected index and track/device existence observers once in init. For each slot register name, existence, base value and discrete count observers once. Adding an observer makes that property readable. Read these properties from the current proxies when needed. The API signatures were checked against the pinned JAR. Delivery order during a real selection change is a hardware/host test. [RemoteControlsPage](../../sources.md#bitwig-controller-api) · [RemoteControl](../../sources.md#bitwig-controller-api)

## Turning and recording

After Remote Mode ACK, MIDI status B0 and CC 1–8 identify slots 0–7. Reuse M2's accepted data ranges 01–08 and 78–7F (hex). Read the addressed slot at event time. For continuous controls use the signed delta divided by 256. For a reported count N ≥ 2, use sign(delta)/(N−1), including accelerated turns. Suppress edits for unusable targets and unsupported counts. Recheck the session generation before issuing the increment. The host owns clamping and automation. Keep the working M2 increment gesture. An edit that throws may already have reached Bitwig, so refresh the display and consume that message once. Keep the later acceleration experiment on the project roadmap.

## Sending feedback

Observers mark the surface dirty and coalesce a host flush request. Each READY flush reads one page view containing title and eight slot readings. Compare the **rendered** title and sixteen fields against the last successful text send. Send one complete prefixed labels packet if text changed. Compare each ring's availability and quantized amount against its last successful send. In slot order, send one existing prefixed ring packet per changed ring, using RGB (0,8,0) for assigned slots. No name or value observer writes a parameter.

A first flush after each fresh entry sends labels and eight rings, including explicit clears. A single automation change normally produces one ring packet. A rename can produce just a label packet. At most nine complete packets are needed in one flush. Using the tested single-ring framing keeps this checkpoint independent of firmware support for batched ring records. Measure responsiveness during eight-way playback before considering batching or a rate limit.

The packets reach the E16 sequentially. The design promises convergence on the latest Bitwig state. Track dirty changes during sends, check the session generation after reading and around every send, and invalidate all sent-state caches when the session ends. An interrupted flush must never send the rest of its frames into a newly entered session. A new selection during output schedules a fresh snapshot. If a send throws in the current session, keep unsent fields dirty. Schedule one delayed retry even if Bitwig stays idle. If that retry fails, log the failure and wait for another observed change or re-entry. Emit bounded diagnostics for failures rather than tracing every automation sample. [E16 protocol](../../sources.md#oxi-e16)

## Checkpoints

First validate all eight labels and rings as a monitor, with parameter writes disabled in that build. Then enable eight-way editing and test independent values, stepped controls, automation and routing. Preserve reviewed M2 for immediate rollback. Record the exact installed hash and real observations at each checkpoint. M2's unmeasured raw CC routing and second-controller cases carry forward explicitly. [Implementation plan](../plans/2026-11-23-m3-eight-remote-controls-implementation.md)
