# Encoder sensitivity and acceleration implementation plan

**Goal** Give slow turns a fine adjustment and preserve faster sweeps. [Design and acceptance criteria](../specs/2026-11-24-encoder-tuning-design.md)

**Architecture** One pure Java response curve converts each continuous turn into a signed fraction of the parameter range. The existing session applies it at its three continuous write sites. Bitwig supplies the current target, value and discrete count. A temporary measurement checkpoint records the readback precision for comparison with the chosen slow step.

**Execution** Use Superpowers executing-plans with Sol Medium. Hand the implementation to Simon for the switch to Astra Medium before code review. Use JDK 21, the pinned Bitwig API 25 JAR and the existing shell scripts. Paths below are relative to `src/main/java/io/github/wintocode/oxiwiggle/` and its matching `src/test/java/` package.

## Constraints and review focus

- Ship one `OxiWiggle.bwextension` per checkpoint with UUID `7bd8998f-1030-4da1-991b-b86bb8a94357`. Preserve the normal Remote Mode lifecycle and MIDI isolation. The controller-setup plan can proceed independently.
- Exercise delayed or missing readbacks, hidden plug-in quantization, four-position controls at both endpoints, and writes to stale Mixer targets. Each has a check below.
- The baseline at design time is `0.0.32-mixer-targets`. Suggested versions below leave room for the three setup builds. At implementation time start from the latest accepted source, allocate unused versions and update the matching assertion in `ExtensionDefinitionTest.java`.

## 1. Measure one increment in Bitwig

**Working checkpoint** `0.0.37-encoder-probe`. All normal turns still use the baseline scaling. Put the temporary trial code in `bitwig/ResolutionProbe.java` and register it in `OxiWiggleExtension.init()`. Add `bitwig/ResolutionProbeTest.java` to the session suite in `AllTests.java`.

- [ ] Start with a test of `runTrial(int exponent, int direction)` using a fake `RemotePage`, scheduler and log. With RC1 at 0.5 and exponent 8, a positive action must issue exactly one `page.increment(0, 1.0 / 256)`. Check a delayed change, a readback that stays at baseline without a callback, a second click during a trial, a target change and extension exit. Run `sh scripts/test.sh session` and see the missing behaviour fail.
- [ ] Under native preferences category **Resolution test**, register **Step exponent** as an integer field from 8 to 60, initially 8. Register two `getSignalSetting` actions labelled **Test Device RC1 up** and **Test Device RC1 down** with `addSignalObserver`. Each button operates on the current Device RC1 in a loaded test instance. Ordinary preference edits and registration perform zero parameter writes. MIDI turns continue through `RemoteSession` and its ACK gate. [Settings API](../../sources.md#bitwig-controller-api) · [Signal API](../../sources.md#bitwig-controller-api)
- [ ] At the click, require a usable continuous RC1, direction ±1, one trial at a time, and an interior starting point with room for the requested step. Calculate `Math.scalb(1.0, -exponent)`. If adding the signed step already rounds back to the starting double, log the readback limit. Otherwise log the target, exponent, signed request and full-precision normalized starting value, then issue the one relative write. An exception ends that trial. Never replay it.
- [ ] Use `host.scheduleTask(Runnable, long)` to sample the existing base-value readback about every 250 ms against a monotonic two-second deadline. Log elapsed time and use late readbacks only to invalidate a result. Log the actual difference and the last two samples. Require a steady final value and the requested direction for a pass. A value that stays at baseline throughout is **no observed movement within two seconds**. Mark unstable, reversed or interrupted runs inconclusive. Invalidate the trial on track/device/page or RC1 binding changes, intervening encoder edits and exit. Ignore callbacks from an old trial. Only the original button action may issue a write. Rerun the session suite, then build and archive.
- [ ] **Live protocol.** In a scratch project stop playback, turn off automation writing and use an unmodulated parameter. Map a Bitwig continuous parameter to RC1. Pick restorable low, middle and high values away from the endpoints, roughly 25%, 50% and 75%. Use one button per trial. Return to the same value in Bitwig and check the logged starting value matches before comparing steps. Start at exponent 8 and increase it by one until a smaller request fails repeatably or the readback limit is reached. Test both signs. Confirm the boundary candidates three times from each starting value. Repeat with a continuous synth Remote Control. The test buttons can be used without turning an encoder if Bitwig runs the configured instance. Physical response testing comes later.
- [ ] In `docs/hardware-tests/encoder-tuning.md` record Bitwig, plug-in and E16 versions as applicable, control/slot, actual starting value, direction, exponent, requested and observed deltas, repeat counts, and any independent target readout. Select the smallest common passing candidate as specified in the design and recheck it on every sampled case. Record an unresolved floor if the observations disagree. Offline work on the curve can use a clearly labelled fixture value while live measurements are pending.

## 2. Try the 14-bit-scale slow step

**Working checkpoint** `0.0.38-encoder-fine`. Simon chose `1/16,384` for movement magnitude 1 and requested the removal of the temporary debugging. The probe measured repeatable Bitwig readback at `2⁻⁵²` on two controls. That result describes numeric readback and is recorded with the archived probe. [Hardware evidence](../../hardware-tests/encoder-tuning.md)

- [x] Preserve the probe binary and source snapshot. Remove the trial code, settings, observers specific to the trial, and raw MIDI/SysEx dumps. Keep lifecycle and error reporting.
- [x] Share the signed fine step across Device Remote Controls, Mixer volume and pan, and send amount. For movement magnitudes 2–8, keep the existing `delta / 256.0` response. The existing discrete-control branch continues to advance one choice per message.
- [x] Run the full build and verify the package. The session suite covers fine movement on Device and Mixer paths, accelerated movement and stepped controls.
- [x] On the E16 compare isolated slow turns and fast sweeps on a continuous control and Mixer volume, pan and send. Confirm a stepped control still advances reliably and check that Bitwig preferences no longer show the Resolution test controls.

## Outcome

Simon accepted the fixed response in 0.0.38 after hardware testing. This completes encoder tuning.

## Build, install and rollback

From the project root, substitute the checkpoint version for `version`. This archives exactly the binary that passed the complete build.

```sh
(
  set -eu
  version=0.0.38-encoder-fine
  archive="build/checkpoints/OxiWiggle-$version.bwextension"
  test ! -e "$archive"
  sh scripts/build.sh
  mkdir -p build/checkpoints
  cp build/OxiWiggle.bwextension "$archive"
  shasum -a 256 "$archive"
)
```

Expect `PASS protocol`, `PASS metadata`, `PASS session`, `PASS display` and `PASS build/OxiWiggle.bwextension`. With Bitwig closed, install the matching archive and compare hashes. To roll back, use the last accepted archive with the same destination. Record the archive version and hash alongside each live result.

```sh
archive="build/checkpoints/OxiWiggle-0.0.38-encoder-fine.bwextension"
installed="$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
cp "$archive" "$installed" && shasum -a 256 "$archive" "$installed"
```
