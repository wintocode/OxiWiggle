# OxiWiggle Device and Mixer modes

The accepted Device surface is the starting point. Mixer adds four channel strips that can be used together while the song plays. Each track occupies one physical column. The bottom row belongs to the surface itself.

## Controls

| Row | Physical knobs | Turn | Shift + turn | Click |
|---|---|---|---|---|
| Top | 1–4 | Track volume | Track pan | Mute |
| Middle | 5–8 | Chosen send amount | Choose this column’s send | Solo |
| Third | 9–12 | Unassigned | Unassigned | Record arm |

The existing Device assignments remain available in Device mode, including 14 for page, 15 for device, and 16 for track and group expansion. Knob 13 turns directly through the available modes. Clockwise selects Mixer from Device, and anticlockwise returns to Device. The ordered list can grow to include future modes such as Transport.

## Proposed interaction defaults

- Start each connection in Device and show the active mode on knob 13’s ring. Turning clamps at the ends of the mode list.
- Keep the E16’s mode independent of Bitwig’s on-screen workspace. Opening Mixer while watching the Arranger is a primary use case.
- Knob 16 selects adjacent tracks in both modes, and pressing it toggles the selected group’s expansion. The Mixer bank follows that same Bitwig selection, whether it changes from knob 16 or in Bitwig. The selected visible track starts the four-track window until the last full window is reached. With five tracks, track 1 shows 1–4 and any selection from 2 through 5 shows 2–5. With fewer than four tracks, show all existing tracks. Keep 14 and 15 spare in Mixer.
- Use an unshifted press and release for the three track toggles. Act once on release. A turn of the depressed knob cancels its pending click. Shift changes turn assignments and the preview. Shifted presses produce no toggle. Discard pending clicks across a mode, bank, target, or connection change. Editing one column must never select or arm a track as a side effect.
- Start each column at its first available send. Shift-turn steps through available sends at a controlled pace. Selection alone leaves all send amounts untouched. On a bank change or track replacement reset the affected columns to their first available send. Persistent per-track send selection is outside this checkpoint.

## What the musician sees

Normally the OLED shows the four track names aligned with their columns, using the accepted narrow font and padded dividers. Empty columns have a clear empty marker and inert controls. Holding Shift previews pan and the chosen send destinations across the four columns before anything moves. Turning a mixer parameter briefly shows the track, parameter or destination, and Bitwig’s formatted value. Follow the accepted first-knob focus and 750 ms expiry. A physical navigation or mode action takes precedence. Automation refreshes feedback without opening detail.

Top arcs show volume, switching to centred bipolar pan arcs under Shift. Middle arcs show the selected send amount. Colour distinguishes muted and soloed columns. At zero amount use a visible status segment when mute or solo is active. Solo and mute remain available on existing tracks without sends. Third rings indicate arm in red; use a dim neutral marker for an available unarmed track. Absent controls stay dark. Render observed Bitwig state after toggles, mouse edits and automation, including changes on other columns caused by host solo or arm preferences. Final colours and brightness need an E16 check.

## Engineering and evidence

Use the existing Java extension, lifecycle, MIDI port pair and framebuffer/ring protocol. Create one four-track mixer bank that follows the shared cursor, and register its observers at extension initialization. Gate editing and rendering by active mode and successful entry ACK. Keep raw controller input outside track note inputs. Keep track values, flags and routing in Bitwig. The extension owns only surface state such as highlighted mode, held gestures and send choice.

API 25 exposes four-track banks through ControllerHost.createTrackBank, indexed items through Bank.getItemAt, and track volume(), pan(), mute(), solo(), arm() and sendBank(). Send extends Parameter. The existing implementation already reads parameter values and formatted strings. Verified signatures are in the pinned API JAR. Consult the archived [Bitwig API documentation](../../sources.md#bitwig-controller-api) for version and deprecation notes. The [E16 guide](../../sources.md#oxi-e16) documents Shift as channel-1 note 16.

Hardware questions are whether the four-wide bank reflects hidden and folded tracks as intended, which track types permit arming, whether each column can address its own send across the full destination list, and how quickly targets rebind after scrolling. Prove these before enabling writes to ambiguous targets. Keep the reviewed Device build available for rollback.
