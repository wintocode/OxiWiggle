# M2 implementation plan — one Bitwig Remote Control

**Goal** — Move the first Remote Control on the selected device page from physical encoder 1. Show the value reported by Bitwig on its ring, including changes from the mouse, automation and another controller.

**Architecture** — Reuse the M1 MIDI callback, session and prefixed display frames. Add a small adapter for the live Bitwig slot. First ship feedback alone, then add one path for relative edits.

**Tech stack** — Existing Java 21 build, Bitwig Controller API 25 and plain Java test runner. Use the `superpowers:executing-plans` skill for native execution in this session on Sol Medium. Ask Simon to switch to Astra Medium before the implementation review. [M2 design](../specs/2026-11-23-m2-one-remote-control-design.md)

## Constraints

Keep one `OxiWiggle.bwextension`, the existing extension UUID and the `io.github.wintocode.oxiwiggle` package. Use the configured E16 Port 1 endpoints and the existing dependencies. Ask Simon before installing additional tools or dependencies. Scope is one physical encoder, one ring and one static slot label. Keep raw MIDI out of track inputs and leave automation-write settings under the musician’s control. [Project instructions](../../../AGENTS.md) · [M1 hardware baseline and outstanding edge cases](../../hardware-tests/m1.md)

## Review focus

| Condition | Expected result | Evidence |
| --- | --- | --- |
| Old value or callback after stop, restart or page change | The current session and target own the next write and redraw | Tasks 1–3 |
| Empty slot, zero-valued slot and unavailable device | An empty slot clears. A valid minimum remains assigned and can move upward | Tasks 1 and 2 |
| Many observations, including during a send | Latest value wins, repeated frames are suppressed and a fresh ACK forces a redraw | Task 1 |
| Wrong MIDI input, accelerated turns and parameters with few choices | Only channel-1 CC 1 in the supported range writes. Selectors advance one item per message | Tasks 2 and 3 |
| Automation touches, playback and another writer | Turns edit once. External changes only redraw. Automation releases and raw MIDI stays out of the clip | Task 3 |

## Task 1 — Follow Bitwig on the first ring

**Deliverable** — Installable `0.0.4-m2-monitor`. Mouse or automation changes move the first ring. Turning the E16 only produces M1 diagnostics in this checkpoint.

Paths below are relative to `src/main/java/io/github/wintocode/oxiwiggle/` or its `src/test/java/` counterpart with the same package.

- [ ] **Preserve the working binary.** From the project root, check the installed M1 hash, then copy that exact file somewhere the build and Bitwig will leave alone. The known hash on this Mac is `5c0715a8e282fbcdb52b36fa16c57c95fa4625ade8c6eaf0215f3043020a465f`. `scripts/build.sh` replaces its output on every build.

  ```sh
  mkdir -p build/checkpoints
  m1_sha=5c0715a8e282fbcdb52b36fa16c57c95fa4625ade8c6eaf0215f3043020a465f
  installed_ext="$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
  saved_m1=build/checkpoints/OxiWiggle-0.0.3-m1.bwextension
  if [ ! -e "$saved_m1" ]; then
    printf '%s  %s\n' "$m1_sha" "$installed_ext" | shasum -a 256 -c - &&
      cp "$installed_ext" "$saved_m1"
  fi
  printf '%s  %s\n' "$m1_sha" "$saved_m1" | shasum -a 256 -c -
  ```

  Continue from the saved copy's `OK` result. This also allows resuming the plan after a later build has been installed.

- [ ] **Write the feedback tests first.** Extend `support/SessionFixture.java` with a mutable fake slot, initialized to an available value of 0.5 so the existing M1 ring-vector and restart tests remain useful. Extend `surface/RemoteSessionTest.java` with these assertions. Run `sh scripts/test.sh session` and observe failure on the missing behaviour.

  | Test | Assertions |
  | --- | --- |
  | `feedbackUsesLatestBitwigValue` | Set 0.25 then 0.75 before the entry ACK. Send nothing new until ACK and flush. First flush sends the M2 labels and green ring amount 12287. Repeating 0.75 sends nothing. A later burst ending at 0.25 sends one ring at 4096 with no labels. |
  | `emptySlotDiffersFromMinimum` | Available at zero shows `RC1`. Removing the target shows `----` and sends black RGB and zero amount. Returning at zero restores `RC1`. Nonfinite values produce an unavailable display. Finite values just below 0 and above 1 render at 0 and 16383. |
  | `refreshDuringSendSurvives` | A fake value change during output produces the latest ring by the next flush. Disconnect/restart during output and a stale send exception still leave the new session able to send both initial frames. |

- [ ] **Add the Bitwig adapter.** Create `surface/RemoteSlot.java` with `Reading read()` and nested `record Reading(boolean available, double normalized, int discreteCount)`. Create `bitwig/FirstRemoteControl.java` implementing it with `FirstRemoteControl(ControllerHost host)` and `void observe(Runnable changed)`. During `init()` create the track, device, eight-slot page and slot 0 from the design. Register observers on `track.exists()`, `device.exists()`, `page.pageCount()`, `page.selectedPageIndex()`, `slot.exists()`, `slot.value()` and its `discreteValueCount()`. Each observer just signals a refresh. Registration makes those properties readable. For each read, check existence, a valid page index and a finite current normalized value. Treat unavailable or uninitialised inputs as empty. Two successive targets can have identical values. Read the moving proxy again when it is needed.
- [ ] **Connect feedback to the session.** In `surface/RemoteSession.java` add the slot to the constructor and expose `void onSlotChanged()`. Request a flush once per pending change while READY. On flush consume the pending flag before calling out, read a fresh snapshot, and compare label and ring fields with those last *successfully sent*. Use `NativeLabels.encode`, `labelsPrefixed` and `ringPrefixed`. Force both frames on the first flush of each acknowledged session. Preserve generation checks around sends and cache updates, even when a send throws or re-enters. During stop, timeout or failure, discard pending output and invalidate the sent-state cache. Keep observers free of MIDI sends and parameter writes.
- [ ] **Wire and build the monitor.** In `OxiWiggleExtension.java` capture the local session in the input and observer callbacks, register before `start()` and update the console version banner. Give the monitor a concise availability/step-count diagnostic so empty-slot and selector classification can be checked on the host. Update `OxiWiggleExtensionDefinition.java` and `ExtensionDefinitionTest.java` to `0.0.4-m2-monitor`. Run `sh scripts/build.sh`. Expect the existing protocol, metadata, session and display suites and the extension build to pass.
- [ ] **Verify the target before enabling writes.** Use the install procedure below. In a scratch Bitwig project, put a stock device on a track, map a continuous parameter to slot 1 and set it near 25% *before enabling* OxiWiggle. Expect `OxiWiggle M2`, `RC1`, and a roughly quarter-full green ring immediately after entry. Move the parameter to 0%, 50% and 100% with the mouse. Select another page with a different first value, another device and another track. Then select a page with an explicitly unmapped first slot and an empty track. Verify `----` and a dark ring in each empty case, and correct recovery on return. Confirm that turning encoder 1 leaves Bitwig unchanged in this monitor. If the API reports a usable target for a visibly unmapped slot or follows the wrong selection, record the actual existence/index/value signals and resolve the adapter on this build before enabling writes. Start a dated `docs/hardware-tests/m2.md` with the artifact hash, versions, setup and observations.

## Task 2 — Let encoder 1 edit the live slot

**Deliverable** — Installable `0.0.5-m2`. A single turn changes the selected slot. Bitwig supplies every value sent back to the ring.

- [ ] **Write the input tests first.** Create `protocol/RelativeEncoderTest.java` and invoke it under the existing `protocol` group in `AllTests.java`. Add input cases to `RemoteSessionTest.java` using an increment-recording fake slot. Run `sh scripts/test.sh protocol` and `sh scripts/test.sh session` to see the new tests fail before implementing them.

  | Test | Assertions |
  | --- | --- |
  | `firstEncoderDelta` | For status `B0` and CC 1, `01, 02, 04, 08, 7F, 7E, 7C, 78` give +1, +2, +4, +8, −1, −2, −4, −8. Zero, `09`, `40`, `77`, invalid data, CC 2, another channel, note on/off and Shift give zero. |
  | `turnWritesOnlyToLiveSlot` | A +1 in READY on a continuous target requests exactly `1.0/256`. A −8 requests `-8.0/256`. Before ACK, after stop/timeout/failure and with an empty or invalid slot, writes stay at zero. Switch fake targets before the next turn and verify only the new target receives it. A flush or observed mouse edit never increments anything. |
  | `failedIncrementIsNeverReplayed` | If an edit throws after the fake target records it, log the failure and refresh from the current host value. Subsequent flushes and reconnects never repeat that increment. |
  | `selectorMovesOneItem` | For a three-choice target both +1 and +8 request +0.5, and −1 and −8 request −0.5. For two choices request ±1. Zero, one or unexpected negative counts suppress edits. A reported count of −1 selects continuous scaling. |

- [ ] **Implement one relative write path.** Add `protocol/RelativeEncoder.java` with `static int firstKnobDelta(int status, int cc, int data)`. Add `void increment(double normalizedDelta)` to `RemoteSlot` and implement it with `slot.value().inc(normalizedDelta)` in `FirstRemoteControl`. Implement `RemoteSession.onMidi(int status, int data1, int data2)` and invoke it from the existing MIDI callback on *every* message, including after diagnostic logging reaches its limit. Read the current slot for each eligible turn, guard state and generation before the write, and apply the continuous or discrete rule from the design. Let Bitwig apply endpoint limits. A turn queues no display value of its own. Start with ordinary `inc` calls and measure automation in Task 3.
- [ ] **Build and exercise both directions.** Update definition, metadata test and console banner for `0.0.5-m2`. Run `sh scripts/build.sh` and use the install procedure. From 50%, turn encoder 1 slowly both ways, then quickly. Confirm corresponding changes in Bitwig and the ring, clamp at minimum/maximum, and move the mouse between turns to check the next edit starts at the new value. Repeat on an empty slot and on two different devices with the same first-slot name. Verify only the current assigned parameter moves. Turn encoder 2 and press/release encoder 1 to check they do not edit the target.
- [x] **Record Simon’s sensitivity decision.** Keep 1/256 for M2. Revisit finer slow turns and larger fast sweeps during later tuning, using the E16’s current acceleration as the comparison baseline.
- [ ] **Try a selector and a switch.** Map a waveform selector with known choices to slot 1. Note the count reported by the monitor/adapter and the names visible in Bitwig. Starting at the first item, turn slowly through the list, reverse, and turn past each endpoint. A three-choice list should show empty, half and full rings in order, and an accelerated message should still select one adjacent item. Repeat with a two-state switch. If a plug-in exposes its list as continuous, record that limitation and use a target reporting a finite count to test the stepped path.

## Task 3 — Automation, review and acceptance

- [ ] **Verify outside edits and routing.** On a disposable project, disable automation writing and record an empty clip on an armed instrument track receiving All Inputs. Turn and press encoder 1 and cycle the extension. Confirm the intentional parameter edit, no raw E16 notes or CCs in the clip and no unintended instrument trigger. Move the same parameter from a second controller if available. The ring should follow, and the next E16 turn should continue from that value. Record which input routes were active.
- [ ] **Measure automation.** On the arranger, enable automation writing in Touch mode and record a short continuous sweep followed by a pause. Inspect the mapped parameter's automation lane, disable writing, restore automation playback in Bitwig if needed, and replay hands-off. The value and ring should follow the recorded curve with no new writes. Repeat with a page/device change after turning and confirm the previous parameter is released. Disable/re-enable with a different current value and check a fresh redraw. If `inc` alone fails the write/release test, capture that failure. On Sol, try the design's per-event `touch(true)` / `finally touch(false)` around a single increment, include failure/release regression coverage and rerun recording, selection and playback. If the gesture still fails, report the observed limitation and keep the last working checkpoint. An unresolved automation result stays open.
- [ ] **Request the implementation review.** When the candidate and its evidence are ready, ask Simon to switch to **Astra Medium** before reviewing. Review the five conditions above against the actual diff and observations. Apply any fixes on Sol Medium, rerun affected checks, and repeat affected hardware cases on the resulting artifact.
- [ ] **Hand over the exact tested file.** Record Bitwig/API/E16 versions, the two port names, the final sensitivity decision or open item, pass/fail/unrun cases and the SHA-256 of the installed and built files. Inspect the archive for the extension provider, MIT license, project classes and Java 21 bytecode. Accept M2 on the evidence from the final artifact. Update README and PROJECT with the tested behaviour and remaining limitations. Keep M1's outstanding duplicate-route and explicitly unassigned-output tests visible.

## Install and recover either checkpoint

Run from the project root after a successful build. Close Bitwig before replacing the installed extension. Preserve each working checkpoint under `build/checkpoints/` with its version and hash. Copy just the active file to the Extensions directory.

```sh
cp build/OxiWiggle.bwextension "$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
shasum -a 256 build/OxiWiggle.bwextension "$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
```

The two hashes must match. Reopen Bitwig with the existing OxiWiggle controller assigned to `OXI E16 Port 1` in both directions. To recover M1, close Bitwig and use the saved binary.

```sh
cp build/checkpoints/OxiWiggle-0.0.3-m1.bwextension "$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
shasum -a 256 "$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
```

Compare the restored file with the M1 hash in Task 1. Reopen Bitwig and look for the M1 title, `TEST` and the static green first ring.
