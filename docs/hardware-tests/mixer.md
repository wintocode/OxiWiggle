# Mixer mode hardware checkpoints

## 0.0.24-mixer-overview

This read-only checkpoint adds a mode list and the first four visible track names in Mixer. The mode ring is purple for Device and orange for Mixer. All four automated suites passed. Bitwig/E16 verification is pending.

From the project directory with Bitwig closed:

```sh
cp build/checkpoints/OxiWiggle-0.0.24-mixer-overview.bwextension "$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
shasum -a 256 "$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
```

SHA-256 is `c3efc0ff2d2b1d557158a0171f828d2a1025b797032454af41d616c12816cd07`. Record the Bitwig and E16 versions with the result.

1. Open a project with six distinct tracks in the Arranger and verify startup in Device mode.
2. Turn knob 13 clockwise to highlight Mixer. Pause for two seconds and check that the Device overview returns. Turn clockwise again and press 13 to confirm.
3. Check that the first four visible track names line up with the four E16 columns. Rename one in Bitwig and check that the name updates. Try a project with two tracks to see the empty columns. Confirm that the Arranger stays visible.
4. Turn 13 anticlockwise and press it to return to Device. Check a parameter turn and navigation on 14, 15 and 16. Reopen Bitwig to verify Device startup.

To roll back with Bitwig closed, use the same copy command with `build/checkpoints/OxiWiggle-0.0.23-m5-reviewed.bwextension` as the source.

## 0.0.25-mixer-follow

This read-only checkpoint switches modes immediately when knob 13 turns. Knob 16 selects tracks in both modes and keeps its group expansion click. The four Mixer columns follow the same Bitwig cursor used by Device mode. All automated suites passed. The actual scrolling window and cursor behaviour need a Bitwig/E16 check.

With Bitwig closed, use the same install command with `build/checkpoints/OxiWiggle-0.0.25-mixer-follow.bwextension` as the source. SHA-256 is `8e10a5d855cc19c00ef7b615cd99a45ee4db35d97cfe500b1a9d0cc80bdfab8d`.

1. Open six visible tracks with distinct names. Turn 13 clockwise and verify Mixer appears immediately.
2. Turn 16 through the fourth track and onto the fifth. Verify Bitwig selects that track and the four displayed names scroll to include it. Click a track outside the displayed bank in Bitwig and verify the OLED follows it too.
3. Turn 13 anticlockwise and verify Device follows the current selection. Turn 16 there, then return to Mixer and check that it still follows the same track.
4. Verify group expansion with a click on 16 and confirm the Arranger stays visible throughout.

## 0.0.26-mixer-window

The selected visible track anchors the left edge of Mixer until the last full window. All four automated suites passed. Install the archive `build/checkpoints/OxiWiggle-0.0.26-mixer-window.bwextension` with the same copy command. SHA-256 is `19b27822c5832ed3ffcb74d44210481c478d3aefcec6a7e47afb830e7f448a9c`.

With five visible tracks, select track 1 and verify columns 1–4. Turn knob 16 once and verify columns 2–5. Continue to track 5 and then back to track 2 while checking that the window stays on 2–5. Turn back to track 1 and verify columns 1–4. Repeat with mouse selection. Verify a project with fewer than four tracks shows all available tracks.

Simon reported that the revised Mixer window worked much better on the E16. The installed file matched the archived checkpoint. Mouse selection and projects with fewer than four tracks have no separate reported results.

## 0.0.27-mixer-first-strip

Mixer knob 1 controls volume on the leftmost displayed track. Hold Shift for pan. An unshifted click toggles mute on release. A turn, mode change, track change or Shift press cancels a pending mute. The ring is green for unmuted and red for muted, with a minimum red arc at zero volume. Shift previews pan with a bipolar ring. All four automated suites passed.

Install the archive `build/checkpoints/OxiWiggle-0.0.27-mixer-first-strip.bwextension` with Bitwig closed. SHA-256 is `d75ccd2eefc53fe9be1ac30d9449b29f003f1ae70e9fbda4a8de566a1fb8048f`.

1. Open a disposable project with five clearly named tracks. Enter Mixer and turn knob 1. Check the first column's volume in Bitwig and the OLED track name and value.
2. Hold Shift and check the centred pan ring. Turn knob 1 and confirm pan moves. Release Shift and click knob 1 to toggle mute. Try the mute indicator at minimum volume and confirm a turn while pressing knob 1 leaves mute as it was.
3. Press knob 1, turn knob 16 to move the track window, then release knob 1. Verify neither track toggles mute. Turn knob 1 afterwards and verify it edits the new leftmost track.
4. Change level, pan and mute with the mouse and play volume automation. Verify ring feedback and that automation leaves the overview visible when there is no active detail view.

The prior accepted archive is `build/checkpoints/OxiWiggle-0.0.26-mixer-window.bwextension`.

Simon confirmed that the first writable strip worked on the E16. The installed extension matched the archive. The report covers the checkpoint as a whole.

## 0.0.28-mixer-switches

All four Mixer columns support volume on the top knob, pan on Shift + turn, and mute on a top-row click. Middle-row clicks toggle solo and third-row clicks toggle record arm. The mute arc changes to red, the solo ring lights amber, and the arm ring lights red. Solo uses Bitwig's exclusive-solo preference. Arm is offered for tracks that Bitwig reports can hold audio or note data. Middle and third rows are click controls at this checkpoint.

All four automated suites passed. Install `build/checkpoints/OxiWiggle-0.0.28-mixer-switches.bwextension` with Bitwig closed. SHA-256 is `5bc6e87aec92d21f3cde45fef57ccb97686031e6c1bffc7ad76b1930a7f053ef`.

1. Open a disposable project with five distinctly named tracks. In Mixer, check volume and Shift + pan on each top-row knob. Click each row in one column and confirm only the corresponding track responds. Observe solo with exclusive solo enabled and change mute, solo and arm using the mouse.
2. Hold down a third-row knob, turn 16 once to shift the window, then release. Verify that neither track arms. Click it again and verify the new track arms. Try a folded group and a column without a track.
3. Put a track at minimum volume and check its mute indicator. Turn 13 back to Device and verify the arm rings clear. Record volume automation on an armed instrument track and inspect the clip for stray controller notes or CCs.

Roll back to `build/checkpoints/OxiWiggle-0.0.27-mixer-first-strip.bwextension` if needed.

Simon reported that all controls worked on the E16. The installed extension matched the archive. Individual edge cases were not separately reported.

## 0.0.29-mixer-send-probe

This checkpoint reads Bitwig sends and logs the available destinations and the local selection for every displayed column. It requests eight send slots per track. Shift + turn knobs 5–8 to move the corresponding column's diagnostic selection. The selection does not scroll Bitwig's send banks or change send levels.

All four automated suites passed. Install `build/checkpoints/OxiWiggle-0.0.29-mixer-send-probe.bwextension` with Bitwig closed. SHA-256 is `4160f35fd31405234745519f00c62d9ec980f74db7c299903821fa0a62ff903f`.

1. Use a project with four ordinary tracks and three distinctly named FX tracks, such as Reverb, Delay and Chorus. Set two visibly different send amounts in Bitwig to make the readings easy to check.
2. Open the Controller Script Console and select OxiWiggle. Enter Mixer. Hold Shift and turn knob 5 to choose the second or third send for column 1.
3. With Shift held, turn knob 6 to pick a different send for column 2. Copy the latest line beginning `E16 send probe`. It should report both choices and the other columns' readings. Confirm the actual send amounts in Bitwig did not move.
4. If available, repeat with no FX tracks or with nested tracks. Record the console's present, reported and slot counts, which can differ.

Roll back to `build/checkpoints/OxiWiggle-0.0.28-mixer-switches.bwextension` if needed.

The probe ran on Bitwig 6.1.7/API 25. Simon’s console showed three sends present and reported across eight exposed slots on each of four columns. Knob 5 selected FX 3 and knob 6 selected FX 2 in the same report. This confirms independent destination selection for that project. The probe gave feedback in the console and left the OLED and send amounts alone.

## 0.0.30-mixer-sends

Knobs 5–8 adjust the selected send level in each Mixer column. Hold Shift and turn one of those knobs to choose that track’s destination. The OLED shows the target and its Bitwig value. Hold Shift without turning to see the current destination for each track. The middle rings show send amount with amber for solo and purple otherwise. Clicks still toggle solo.

All automated suites passed. This build uses up to eight exposed sends per track. Install `build/checkpoints/OxiWiggle-0.0.30-mixer-sends.bwextension` with Bitwig closed. SHA-256 is `279ed55e3f1503ac8609cfe2e290830319c4b0dd7985ca1266b02b19e5508bde`.

1. Hold Shift. Use 5 to choose FX 3 and 6 to choose FX 2. Check the displayed labels and confirm the send amounts stay put while choosing.
2. Release Shift and turn each knob clockwise. Compare both adjusted sends with the corresponding track in Bitwig. Check that each other send stays at its previous level.
3. Remove a chosen destination, move the track window, and check that the displayed destination and actual edited target agree after each change. Try a track without sends and check that its middle click still controls solo.

The previous writable checkpoint is `build/checkpoints/OxiWiggle-0.0.28-mixer-switches.bwextension`.

## 0.0.31-mixer-send-enable

Simon reported OLED updates with no corresponding change in Bitwig on 0.0.30. The installed hash matched the archive. Bitwig documents that new projects start with disabled sends and the mixer activates a send on first adjustment. OxiWiggle now enables a disabled destination on a clockwise turn before sending the level increment. Counterclockwise turns leave disabled sends off. [Bitwig Mixer Send Section](https://www.bitwig.com/userguide/latest/the_mix_view/#send-section)

The adapter test verifies activation precedes the level write. All four automated suites passed. Install `build/checkpoints/OxiWiggle-0.0.31-mixer-send-enable.bwextension` with Bitwig closed. SHA-256 is `d100a3bd6863c02aef6b5991fd00eb088bc6bcf941b9c9c2e6ffabebe4ac9a24`.

Verify with a send whose name is dim in Bitwig. Select it with Shift + turn, release Shift and turn clockwise. Check that only that send lights up and its level increases. Repeat with an already enabled send. Simon confirmed that the send control worked on hardware. The installed build matched the archive. The report covers the activation checkpoint as a whole.

### Mixer implementation review

All four automated suites passed during review. The installed extension and archived 0.0.31 checkpoint have the same SHA-256. Code inspection identified three cases for regression tests and hardware verification:

- Choose FX 3, then select another device or remote page on the same track. The selection callback clears every column's send choice. After redraw, the next ordinary turn can adjust FX 1. Keep each choice while its track and destination remain available.
- Hold an arm button and reorder two identically named tracks within a stationary window. The pending click records only its column. Compare the track identity at press and release, and cancel if the track changed.
- Move knob 16 and immediately turn a level knob. A redraw before the cursor-follow observer reports the new window can enable the old bank again. Exercise that callback order in the fixture and verify the previous track's level stays put.

The callback ordering and same-name reorder cases are code-review risks pending reproduction in Bitwig.

## 0.0.32-mixer-targets

Mixer send choices survive device and Remote Controls page changes and track renames. A column falls back to its first available send when it binds to a different track. Mixer gestures use Bitwig channel IDs to check the displayed track, and a held click must still match the track ID on release. After knob 16 requests a move, the extension waits for a cursor identity change and verifies that the bank contains the selected track in the expected slot before enabling edits.

All four automated suites and packaging passed. Build with `sh scripts/build.sh`. The archived checkpoint is `build/checkpoints/OxiWiggle-0.0.32-mixer-targets.bwextension` with SHA-256 `7f9e3590d914236c4e9e9ec6abc75ae0674987cce565c745a070ebdc3ae27ff9`. With Bitwig closed, install it from the project root:

```sh
cp build/checkpoints/OxiWiggle-0.0.32-mixer-targets.bwextension "$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
```

Open Bitwig and enter Mixer using knob 13. Use a project with at least five visible tracks and two FX tracks.

1. Hold Shift and use knob 5 to select the second send. Release Shift, select a different device or Remote Controls page on the same track in Bitwig, and turn knob 5. Verify only the second send moves.
2. Rename two adjacent tracks to the same name. Hold the arm knob for one of them and swap their positions in Bitwig while keeping the selected track and mixer window fixed. Release the knob and verify neither track changes arm state. Click again and verify the current track responds.
3. Turn knob 16 to slide the window and immediately turn knob 1. Verify the former leftmost track's volume stays put. After the display updates, turn knob 1 again and verify the new leftmost track responds. Repeat with the last track selected and confirm that turning knob 16 past the end still permits mixer edits.

Simon confirmed that the selected send still worked after changing devices on the same track. The held-arm and rapid-navigation checks are pending. To roll back with Bitwig closed, install the archived `0.0.31-mixer-send-enable` checkpoint using the same destination.

## 0.0.39 Mixer navigation recovery

The offline regression moved away from a track in Device mode, returned to it through Bitwig, then entered Mixer. Before the fix, the mode ring and volume writes stayed blocked. The candidate releases the navigation wait when the selected track ID is observed to change. A separate test switches modes while navigation is still pending and verifies that the volume edit waits for the destination. All four suites and packaging passed.

Archive: `build/checkpoints/OxiWiggle-0.0.39-mixer-navigation.bwextension`

SHA-256: `3de081e340d38e26abac3dc206ec7136ed835765c05dbd37e583f1be305ac960`

After review, install with Bitwig closed. In Device mode, turn knob 16 to select a different track. Click the original track in Bitwig, enter Mixer with knob 13, and verify the Mixer screen, mode ring and first column volume. Then repeat the immediate volume edit and held arm button checks from 0.0.32. Watch for the previous track's volume to stay put and for both reordered tracks to stay unarmed. Hardware verification is pending.

**0.0.39 review.** The original reproduction now emits a framebuffer and mode ring and accepts the volume edit. All four suites passed and every archived class matched the fresh compilation. A separate offline ordering check exposed a stale write. With five tracks and a pending move from the first to the second, deliver the cursor selection change, leave the Mixer projection on the first track, flush and turn knob 1. The fixture recorded a volume edit on the first track. Its usual selectTrack helper advances both cursor and bank together. Host occurrence is unverified. Before hardware testing, add the split-update regression and require agreement between the live cursor and the bank's selection before enabling edits, preserving the intended four-column window at its end.

## 0.0.40 Mixer bank alignment

Reproduced the 0.0.39 stale edit in the session suite. The cursor moved to track 2 while the bank still displayed tracks 1–4. The candidate requires the selected track ID in the expected Mixer column before enabling edits, then checks the live selection again at each edit. Both volume and arm edits are blocked while the bank is stale. The regression verifies edits resume after the bank catches up and work at the end of the four-track window. All four suites and packaging passed.

Archive: `build/checkpoints/OxiWiggle-0.0.40-mixer-alignment.bwextension`

SHA-256: `8316fe7250cb0ff36e108c977cf93e79fc5843bffbe526f96b3d17523085116f`

After review, use the hardware procedure from 0.0.39 with this archive. Bitwig's actual callback ordering remains unverified.

**0.0.40 review.** All four automated suites passed, and the archive contained the same 34 classes as the fresh compilation. Running both standalone reproductions against the archived extension confirmed that returning to the original track restored the Mixer screen, mode ring and volume edit, and that a turn while the bank lagged behind the cursor produced zero edits. Ready for the scratch-project hardware checks above.

Simon confirmed the first 0.0.40 hardware check. After navigating from track 1 to track 2 in Device mode and selecting track 1 again in Bitwig, entering Mixer restored the Mixer screen and knob 1 adjusted track 1's volume. The immediate-turn and held-arm reorder checks remain open.

Simon reported that track 1's volume changed in the immediate-turn check. The installed file matched the 0.0.40 archive. The selected track, settled leftmost OLED label, and routing of a later turn have yet to be confirmed for this failure. Treat the immediate-turn check as failed pending diagnosis.

After pausing, Bitwig highlighted Audio2, the E16's first column showed Inst1, and knob 1 adjusted Inst1. The settled edit agreed with the displayed column. Interpretation of the window position depends on the number and order of visible tracks in this project. The earlier test instruction assumed a fifth visible track.

Simon listed the project channels in order as Inst1, Audio2, FX1 and Master. With at most four visible channels the window start is zero for either selection. Inst1 remaining leftmost and responding to knob 1 was expected. This supersedes the earlier failure classification. The immediate-turn test with a window that actually scrolls remains open.

With enough tracks for the window to scroll, Simon confirmed that the immediate knob 1 turn adjusted Audio2 after navigating from Inst1 to Audio2. The immediate-turn hardware check passed. The held-arm reorder check remains open.

Simon confirmed that both identically named tracks stayed unarmed after swapping them while holding knob 9 and then releasing it. All three planned 0.0.40 hardware checks passed.
