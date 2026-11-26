# 0.0.44 Launcher trial

Live Bitwig and E16 verification is pending. Record the Bitwig, E16 software and hardware versions used. The build targets API 25 on Bitwig 6.1.7.

1. Build with `sh scripts/build.sh`. Quit Bitwig, copy the candidate to `~/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension`, then reopen Bitwig with both E16 Port 1 endpoints selected.
2. In a test project with eleven named scenes and distinguishable clips, turn 16 clockwise through Device, Mixer and Transport to Launcher. Confirm the first eight names follow the knob positions. Press 1 and 8 and check which scenes Bitwig launches.
3. Turn 13 clockwise. Confirm that paging leaves playback alone and that only scenes 9–11 appear. Press 1 and 3 to launch scenes 9 and 11. Press 4–8 and check that playback continues. Turn past each page boundary.
4. Rename or delete a scene in Bitwig. Compare the refreshed names and targets, including after deleting the entire last page. Check that holding a button through a page or mode change does not launch a second scene.
5. Use 15 to navigate tracks and click it on a group. Check that 16 returns to Transport. Restore the archived 0.0.42 or 0.0.43 build to resume the earlier trials.

Offline, the four suites passed and the archived candidate passed its ZIP integrity check. The local checkpoint SHA-256 is `5aba24a447f0b27667692ce71636bf010dae46c4fa1bf3590149600984eea530`.
