# Transport trial · 0.0.43-transport

On 0.0.49-port-pair, Simon confirmed that pressing knob 1 started Bitwig playback, the E16 position display advanced during playback, and pressing knob 2 stopped it. The reference setup is Bitwig Studio 6.1.7, Controller API 25, E16 SW 1.3.1 and HW V5.

On 0.0.50-transport-position, Simon tried the four-field playhead display and preferred it to the previous bar-and-beat readout.

## Controls

With playback stopped on 0.0.49-port-pair, Simon confirmed that pressing knob 11 toggled Arranger overdub and pressing knob 12 toggled Arranger automation write independently.
Holding Shift and pressing 11 twice toggled Punch In on and off. Shift with 12 did the same for Punch Out. Each punch switch operated independently, and both unshifted settings retained their values.
With Shift held, Simon toggled each punch switch on and off in Bitwig with the mouse. Ring 11 followed Punch In and ring 12 followed Punch Out.

| Knob | Turn | Click | Shift |
| --- | --- | --- | --- |
| 1–4 | | Play, Stop, Arranger Record, Restart respectively | |
| 5 | Jog by bar | | Turn by beat |
| 6 | Previous or next cue | Add cue | |
| 7 | Loop start by bar | Toggle Arranger loop | |
| 8 | Loop length by bar | | |
| 9 | Tempo by 1 BPM | Tap tempo | |
| 10 | Metronome volume | Toggle metronome | |
| 11 | | Arranger overdub | Click for punch in |
| 12 | | Arranger automation write | Click for punch out |
| 13–14 | Spare in Transport and Mixer, page and device in Device | | |
| 15 | Select track | Expand or collapse group | |
| 16 | Select mode | | |

## Install and check

1. Run `sh scripts/build.sh`. To test the archived candidate on macOS, quit Bitwig, copy `build/checkpoints/OxiWiggle-0.0.43-transport.bwextension` to `~/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension`, and reopen Bitwig. Select the E16 Port 1 input and output.
2. In a test project with a group, multiple devices and Remote Controls pages, turn 13 and 14 in Device and check the page and device shown in Bitwig. Turn 16 through Device, Mixer and Transport in each direction and past each end. Check track selection with 15 and group expansion with its click in each mode. Return to Mixer and verify the first knob edits the displayed track.
3. In Transport press 1–4 and compare playback, recording and restart position with Bitwig. Repeat Play and Stop from Bitwig and watch the OLED and rings. Confirm playback position advances.
4. In 4/4 and 6/8, jog both ways with 5. Check one bar normally and one meter beat with Shift. Add two separated cues with 6, then turn it in both directions and check which cue Bitwig selects.
5. Turn 7 and 8 and check the displayed loop start and length together. Toggle loop with 7. Turn 9 in both directions and tap it several times. Check tempo against Bitwig. Toggle the metronome and adjust its volume with 10.
6. Click 11 and 12 normally, then with Shift. While holding Shift, toggle punch in/out from Bitwig and check the two rings. Release Shift and verify overdub and automation write reappear. Change Shift while holding an encoder down and verify each physical click triggers one action.
7. To resume the fast-turn comparison, quit Bitwig and copy the archived 0.0.42 extension to the same installed filename before reopening.

## Offline verification

All four test suites passed on 2026-11-25. The packaged candidate passed its ZIP integrity check. SHA-256 for this local checkpoint is `fa12d4a1c3953a389e7b6055289b3051dbdf7378c49a49b136922f5247c586f3`. The installed extension matched the 0.0.42 checkpoint at packaging time.
