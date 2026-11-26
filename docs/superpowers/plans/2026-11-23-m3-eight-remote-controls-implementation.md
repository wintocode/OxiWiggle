# M3 implementation plan — eight Remote Controls

**Goal** — A single installed OxiWiggle extension controls slots 1–8 of the selected device page. Page changes, automation and external edits update the correct OLED fields and rings.

**Execution** — Use Superpowers executing-plans on Sol Medium. Check off steps only when their expected evidence exists. Before implementation review ask Simon to switch to Astra Medium. This plan proposes two local builds. [M3 design](../specs/2026-11-23-m3-eight-remote-controls-design.md)

**Constraints** — Java 21, API 25, existing shell build, one MIDI input/output on E16 Port 1, stable UUID and existing package. Use current dependencies. Ask before any installation carrying supply chain risk. Keep the user’s E16 scene intact and raw controller MIDI outside instrument-track inputs.

## Task 1 — Display the selected page

Deliverable `0.0.6-m3-monitor`. Source paths below are relative to `src/main/java/io/github/wintocode/oxiwiggle/`, with parallel paths under `src/test/java/`.

- [x] Verify the saved fallback with `shasum -a 256 build/checkpoints/OxiWiggle-0.0.5-m2.bwextension`. Expect `07a98ddeee94c261a85acd8671dfb74930e74810429e5f1c74952b6fadeb34f8`. Preserve it before running the build script, which replaces the top-level build artifact.
- [x] Write tests first for `display/PageText.java` in the existing display suite. Specify `static String field(String text, int width, String fallback)`. Examples at width four include “Cutoff” → “Cuto”, “  Réso ” → “Reso”, “A🎛B” → “A?B” and whitespace-only slot 8 → “RC8”. Check a 15-character title, space-padding via `NativeLabels.encode`, occupied slot 8 at raw offset 44, and spaces in fields 9–16. Run `sh scripts/test.sh display` to see the failure, implement the design's transformation, and rerun.
- [x] Replace `surface/RemoteSlot.java` with a page interface `RemotePage`. Expose `String title()`, `Reading read(int index)` and `void increment(int index, double amount)`. Include the live name, availability, normalized value and discrete count in each reading. During this interface migration, adapt existing single-slot session calls to index zero so the M2 input tests still compile and pass. Rename `bitwig/FirstRemoteControl.java` to `BitwigRemotePage.java`. Hold one page, cache eight slot/value proxies, and wire the observers listed in the design. A valid page with eight empty slots still supplies its page title. Test empty and unnamed cases separately.
- [x] In `support/SessionFixture.java`, model eight independently editable slots and a changing page title. Extend `surface/RemoteSessionTest.java` for `flush()` on initial ACK, then on changes. Assert that the first flush emits one labels frame and ring indices 0…7 in order. Set only slot 7 to 0.75 and expect a single ring for index 7 at amount 12287. Rename slot 3 or the page and expect one labels frame containing *all* sixteen fields. Rename to another string with the same rendered prefix and expect zero frames. Clear a mapped slot and expect its label marker plus a black ring. Use known-good packets from M2 for index 0 and verify index bytes independently.
- [x] Watch the new session tests fail, then change `RemoteSession` to snapshot the eight readings per dirty flush. Compare title/label content by value and rings by assigned state and amount. Force a complete redraw on entry. Commit each successful send to its session's cache only while its generation is current. Reuse M2's flush coalescing and stale-session guards. Extend stop/restart/failure tests to interrupt a middle ring send. Expect zero remaining old frames, then a fresh set of nine on the new session. For observations arriving during sends, assert eventual delivery of the latest values by the next flush. Fail the fourth ring send while the host stays idle. Verify a single delayed retry delivers only the failed and pending rings from a fresh snapshot. Fail the retry too and verify scheduling stops until a new observation or entry. Adapt M2 tests that assume a two-frame display, preserving their lifecycle and protocol checks.
- [x] In `OxiWiggleExtension.java`, register the page adapter and keep MIDI logging diagnostic-only in this monitor build. Update `OxiWiggleExtensionDefinition.java`, banner and metadata test. Run `sh scripts/build.sh` and require all four suites plus the build to pass. Archive the output as `build/checkpoints/OxiWiggle-0.0.6-m3-monitor.bwextension`.
- [x] On hardware, prepare two pages in a scratch Bitwig project with distinguishable names and mixed values, including slot 8, an empty slot and an assigned control at zero. Confirm both rows map left to right. Move each control with the mouse. Rename a slot and the page, switch pages, devices and tracks, select an empty track, then return. Verify names and rings follow, old assignments clear, and turns leave Bitwig values untouched on the monitor. Check initial redraw and reconnect. Record firmware, Bitwig/API version, both ports, artifact hash and observations in `docs/hardware-tests/m3.md`. Resolve any wrong index or lingering display before enabling edits.

## Task 2 — Enable eight independent edits

Deliverable `0.0.7-m3`. Keep the existing input callback as the sole edit path.

- [x] Generalize `protocol/RelativeEncoder.java` and `RelativeEncoderTest.java`. Split slot selection from signed magnitude, for example `slotIndex(status, cc)` returns 0…7 or −1 and `delta(data)` returns ±1…8 or zero. Test every CC from 1 to 8, boundary CCs 0 and 9, CC 14–16, other channels, note on/off, Shift, zero, unsupported values and acceleration in both directions. Run the protocol suite red then green.
- [x] In session tests assign eight distinct fake targets. Interleave CC 1, 8 and 4 and check each target's own increment history. Test +8/256 on a continuous slot, −1/15 on a 16-choice slot for an accelerated negative message, and +1 on a two-state switch. Test empty, one-position and invalid ranges. Switch pages between two messages with identical displayed names and verify the second write reaches the new page. Before ACK, after failure, and after stop, assert no edits. A throw after a recorded increment must never replay the edit.
- [x] Run the session suite to see the new cases fail. Update `RemoteSession.onMidi` to read and increment the indexed live slot, using M2's scaling constant and discrete formula. Consume the message once. Reconnect this method in the Bitwig callback so input continues after the diagnostic log limit. Parameter observers remain output-only. Update version/metadata. Run `sh scripts/build.sh`, archive the candidate, and record its hash.
- [ ] Turn knobs 1–8 individually. Check that only the corresponding slot moves. Mix two simultaneous edits and verify the final rings against Bitwig. Repeat after mouse changes, device/page selection and empty-slot selection. Include a continuous parameter, Unison or another finite selector, and a switch. Turn past endpoints. Check remaining encoders and button presses leave parameters alone.

## Task 3 — Automation, recovery and implementation review

- [ ] Record distinct automation sweeps on slots 1 and 8. Disable automation writing and replay hands-off. Compare both rings with their own lanes. Exercise several changing parameters during playback and switch pages mid-playback. Check for flicker, persistent stale labels, wrong-slot edits and lag after playback stops. If traffic is a problem, capture changed-frame counts per flush before choosing batching or throttling.
- [ ] On an armed instrument track verify the intended parameter edit and absence of unintended notes/raw CCs in the tested route. Record what the clip inspection actually establishes. If a second controller is available, edit one parameter from it and continue from the observed value on E16. Record unavailable cases as open. Exit/re-enter and unplug/reconnect, checking a complete redraw from the current page.
- [x] Ask for Astra Medium implementation review. Focus on index and channel boundaries, reads from the live target after page changes, empty slots versus assigned zero, redundant SysEx during automation, and interrupted sends during restart. Compare the exact tested artifact with the installed copy. Apply any fixes on Sol Medium and repeat affected tests on the resulting build. Update README, PROJECT and the M3 hardware log with evidence and limitations.

## Build, install and roll back

Run from the project root. After the successful build of each checkpoint, set the version to the build being tested. Close Bitwig before copying the extension.

```sh
version=0.0.6-m3-monitor
cp build/OxiWiggle.bwextension "build/checkpoints/OxiWiggle-$version.bwextension"
cp "build/checkpoints/OxiWiggle-$version.bwextension" "$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
shasum -a 256 "build/checkpoints/OxiWiggle-$version.bwextension" "$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
```

Use `version=0.0.7-m3` for the writable candidate. Reopen Bitwig and verify the two hashes match and the existing controller uses both `OXI E16 Port 1` endpoints. To restore the working M2 build, close Bitwig, copy the preserved file and compare against the Task 1 hash before reopening.

```sh
cp build/checkpoints/OxiWiggle-0.0.5-m2.bwextension "$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
shasum -a 256 "$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
```
