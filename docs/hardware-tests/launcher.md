# Launcher hardware checks

On 0.0.50 Simon confirmed that pressing knob 2 launched the second displayed scene. On 0.0.51-launcher-stop, Simon confirmed that the header showed `9 Stop` and pressing knob 9 stopped playback while Launcher stayed visible. Record the Bitwig, E16 software and hardware versions used. The build targets API 25 on Bitwig 6.1.7.

On 0.0.51, Simon confirmed that turning knob 13 one step clockwise placed scene 9 in the first slot while scene 1 kept playing. Pressing knob 1 then launched scene 9. Renaming that scene in Bitwig updated the first OLED slot on the same page, and knob 1 still launched the renamed scene.

## Scrolling and recording checks

On 0.0.52, Simon observed that a nine-scene project scrolled past 2–9 through progressively shorter windows ending at 9–9. On 0.0.53, Simon confirmed that clockwise scrolling stopped at 2–9 in the same project. He also confirmed that Shift-click recorded into scene 2. Multi-track timing, occupied-slot handling and the OLED notices remain open hardware checks.

In the mixed occupied/empty slot check on 0.0.53, Simon reported the OLED message `No empty armed slots`. Simon’s screenshot showed both Inst 1 and Inst 2 armed, with clip S2 on Inst 1 and an empty slot on Inst 2 in Scene 2. On 0.0.54 the trace for Scene 2 reported both tracks armed, Inst 1 occupied, and Inst 2 empty with neither queued nor active recording. Simon still saw the empty-slot warning. The 0.0.55 diagnostic pairs each request and its OLED result with a numbered identifier. The 0.0.54-record-probe candidate logs those readings on Shift-click.

1. From scenes 1–8 turn 13 once and check for scenes 2–9. In a nine-scene project, continue clockwise and verify the window stays at 2–9. Shift-turn back and forth and verify the same two endpoints. Knob 8 in the final window should launch scene 9.
2. Arm two tracks with empty slots in the same scene. Shift-press its knob and check both clips start together on Bitwig’s launch boundary. Repeat with one slot occupied and confirm its clip survives.
3. Try an entirely occupied armed row and then disarm all tracks. Check each OLED notice and the return to the scene list.

## Original bank-navigation trial (0.0.44)

1. Build with `sh scripts/build.sh`. Quit Bitwig, copy the candidate to `~/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension`, then reopen Bitwig with both E16 Port 1 endpoints selected.
2. In a test project with eleven named scenes and distinguishable clips, turn 16 clockwise through Device, Mixer and Transport to Launcher. Confirm the first eight names follow the knob positions. Press 1 and 8 and check which scenes Bitwig launches.
3. Turn 13 clockwise. Confirm that paging leaves playback alone and that only scenes 9–11 appear. Press 1 and 3 to launch scenes 9 and 11. Press 4–8 and check that playback continues. Turn past each page boundary.
4. Rename or delete a scene in Bitwig. Compare the refreshed names and targets, including after deleting the entire last page. Check that holding a button through a page or mode change does not launch a second scene.
5. Use 15 to navigate tracks and click it on a group. Check that 16 returns to Transport. Restore the archived 0.0.42 or 0.0.43 build to resume the earlier trials.

Offline, the four suites passed and the archived candidate passed its ZIP integrity check. The local checkpoint SHA-256 is `5aba24a447f0b27667692ce71636bf010dae46c4fa1bf3590149600984eea530`.
