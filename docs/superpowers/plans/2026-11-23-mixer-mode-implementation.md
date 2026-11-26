# Implementation plan for Device and Mixer modes

Baseline is the hardware-tested `0.0.23-m5-reviewed`. Use Sol Medium for implementation, one runnable checkpoint at a time. Ask Simon to switch to Astra Medium for implementation review. [Design and control map](../specs/2026-11-23-mixer-mode-design.md)

## 1. Direct mode switching and read-only Mixer

- [x] Test Device startup, direct switching on knob 13, mode endpoints, press events on 13, Device routing, shared track selection on 16, and reconnect.
- [x] Keep the active mode separate from the temporary OLED view. Reset display ownership on mode transitions and refresh all reassigned outputs. Build the four-column Mixer overview with the narrow font and explicit empty columns.
- [x] Bind the existing Device page and the four-track Mixer bank to the same cursor. The selected visible track starts the Mixer window, clamped to the last full window. Observe track names, presence, scroll position and count. Exercise selection-driven redraws in the session fixture.
- [x] Simon accepted the revised four-track window on hardware in `0.0.26-mixer-window`. The hardware record retains the individual cases for further testing. [Checkpoint instructions](../../hardware-tests/mixer.md#0026-mixer-window)

## 2. First writable strip

- [x] In BitwigMixerBank bind volume, pan and mute on the existing track proxies. Observe value, displayed value, availability and mute. Reuse Device’s relative delta and initial continuous sensitivity of 1/256. In RemoteSession route only knob 1 for this checkpoint. Use Shift note 16 down/up, treat zero-velocity Note On as release, and track the identity of the pressed target until release.
- [x] Fail tests first for turn → volume, Shift-turn → pan, click → one mute toggle. Exercise stray and duplicate releases, a held knob that also turns, target removal between press and release, zero-volume mute visibility, two successive mode changes and Shift held across disconnect. Reuse the existing detail and ring rendering where applicable. Include the track name in mixer detail.
- [x] Hardware check in a disposable project. Adjust volume, hold Shift to inspect and adjust pan, release Shift, then mute and unmute. Compare the actual track, ring and OLED with Bitwig. Play automation and check that the overview stays visible until a physical turn. Accept the gesture before duplicating it across the surface.

Simon accepted `0.0.27-mixer-first-strip` on hardware. [Hardware record](../../hardware-tests/mixer.md#0027-mixer-first-strip)

## 3. Four strips, track switches and banking

- [x] Generalize the proven strip using column index modulo four and explicit row actions. Add observed solo and arm, respect host behaviour for exclusive switches and guard unavailable tracks. Keep the Mixer bank following the shared cursor through knob 16 and Bitwig selection. Consult API 25 SoloValue preference semantics before choosing calls.
- [x] Test a one-track session, four and six tracks, two identically named tracks, folded groups, simultaneous turns, multiple active flags, mouse-driven changes and all twelve unshifted clicks. Assert each action targets its own column. Assert a bank move between press and release cannot toggle the arriving track. For host proxies, withhold an edit while rebinding is unconfirmed.
- Hardware check all columns, the final bank, and rapid bank-then-edit. Try a group, an ordinary instrument track and an unavailable arm target. Verify solo/arm behaviour against Bitwig preferences and check active flags at zero arc length. Record parameter automation on an armed instrument track and confirm the clip receives no controller notes or CCs.

Simon accepted `0.0.28-mixer-switches` on hardware. [Hardware record](../../hardware-tests/mixer.md#0028-mixer-switches)

## 4. Independent send selection

- [x] Probe sends read-only in Bitwig first. Use four tracks and at least three distinct destinations. Inspect each track’s sendBank(), item count, item existence, parameter name and formatted value. Check absent sends, removal and scrolling. Verify that selecting the send for one column can leave the others’ targets stable. A host-wide send scroll cannot serve as four independent selections. If the API only exposes a limited window, document the measured limit and resolve it before enabling writes outside the proven range.
- [x] Add a per-column selected send index. Resolve the current target on each action, bound it to available destinations and invalidate it on track replacement. Shift-turn changes selection only. Ordinary turn increments only the selected send. Test different selections in two columns, empty and disappearing sends, bank changes, async rebinding, solo with no sends, and Shift preview returning to ordinary feedback. Use actual destination labels and formatted amounts established by the probe.
- [ ] Hardware check two different destinations on adjacent strips. Select each while holding Shift, release, adjust both, and verify the intended sends in Bitwig. Remove a destination and switch banks while a detail view is open. Check for stale labels, accidental level changes or edits to the previous track.

Simon accepted `0.0.31-mixer-send-enable` on hardware. The probe confirmed independent destinations across two columns. The final hardware report covers the activation checkpoint as a whole. [Hardware evidence](../../hardware-tests/mixer.md#0031-mixer-send-enable)

## Build, install and review for each checkpoint

Run the relevant failing suite before implementation, then `sh scripts/build.sh` for protocol, metadata, session, display and packaging. Give each checkpoint a fresh version and matching metadata test. From the project root, set `version` to that build’s actual metadata version. After the build succeeds, run:

```sh
archive="build/checkpoints/OxiWiggle-$version.bwextension"
test -n "$version" && test ! -e "$archive" && cp build/OxiWiggle.bwextension "$archive"
```

Close Bitwig, copy the verified archive to the existing extension filename, compare hashes, then reopen Bitwig:

```sh
cp "$archive" "$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
shasum -a 256 "$archive" "$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
```

Record build hash, Bitwig/E16 versions, observed results and pending cases in docs/hardware-tests/mixer.md when implementation starts. To roll back with Bitwig closed, copy the previously accepted archive to the same installed filename.

Review focus is a wrong-track write after banking, a release arming a different track, send selection changing a level, stale feedback after switching modes, and raw input appearing in an instrument clip. Keep each checkpoint independently usable and wait for its hardware result before enabling the next set of edits.
