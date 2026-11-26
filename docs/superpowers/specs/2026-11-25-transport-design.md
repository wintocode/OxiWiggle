# Transport mode and bottom-row navigation

Transport gives the E16 direct control of Arranger playback, recording and timeline navigation. Bitwig owns the transport state across mouse edits, project changes and mode switches. Scene selection and launching are reserved for a possible Launcher mode.

## Approaches

The first proposal kept Mode on 13 and Track on 16, with punch on 14 and 15. Simon preferred grouping the two global controls in the bottom-right corner. The revised layout puts Page, Device, Track and Mode in order on the Device page. Putting punch on Shift-click keeps recording actions together and gives the rings a way to show which punch states are active.

## Bottom row on every page

| Mode | 13 turn | 14 turn | 15 turn / click | 16 turn |
| --- | --- | --- | --- | --- |
| Device | Page | Device | Select track / toggle group expansion | Select mode |
| Mixer | Spare | Spare | Select track / toggle group expansion | Select mode |
| Transport | Spare | Spare | Select track / toggle group expansion | Select mode |

Clockwise advances through Device, Mixer and Transport, stopping at Transport. Anticlockwise reverses that order and stops at Device. Move navigation feedback and gesture handling with each assignment. Page and device navigation retain their existing response. Mode switches affect the controller view independently of playback. The encoder pushes use zero-based note numbers, so track push becomes note 14. The separate Shift button remains note 16.

## Transport controls

| Knob | Turn | Click | Shift |
| --- | --- | --- | --- |
| 1 | | Play | |
| 2 | | Stop | |
| 3 | | Arranger record | |
| 4 | | Restart from play start | |
| 5 | Jog by bar | | Turn to jog by beat |
| 6 | Previous or next cue | Add cue at playback position | |
| 7 | Adjust loop start | Toggle Arranger loop | |
| 8 | Adjust loop length | | |
| 9 | Adjust tempo | Tap tempo | |
| 10 | Adjust metronome volume | Toggle metronome | |
| 11 | | Toggle Arranger overdub | Click to toggle punch in |
| 12 | | Toggle Arranger automation write | Click to toggle punch out |

Trial defaults are one bar per loop adjustment and 1 BPM per tempo step. Knob 3 invokes Bitwig’s Record action.

The overview shows bar and beat, tempo and playback state. During loop edits show start and length together. Rings follow observed Bitwig state. Holding Shift shows punch in and punch out on rings 11 and 12. Releasing Shift restores overdub and automation write feedback. A single physical click triggers one action even if Shift changes before release.

## Verification

Use the pinned API 25 Transport and beat-time values. Verify jog distance in different time signatures and check cue directions in Bitwig. The archived description of the next-cue method repeats the previous-cue wording. Exercise global navigation from all three modes, including group clicks, empty tracks, clamped mode endpoints and returning to Mixer after navigating in Transport. Test the displayed Shift states after host edits. Verify existing Device and Mixer edits with the moved controls and retain the pending 0.0.42 fast-turn hardware trial as a separate checkpoint.

The proposed layout has been discussed with Simon. Physical feel and host behaviour await hardware trials. [Transport API reference](../../sources.md#bitwig-controller-api)
