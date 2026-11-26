# Encoder sensitivity and acceleration

A musician should be able to creep up on a sensitive synth control and make a broad sweep with the same knob. Simon chose four tiers driven by the E16’s reported movement. The slowest tier uses a fixed relative increment of 1/16,384. Simon accepted the fixed faster tiers in checkpoint 0.0.38. The gesture is an ordinary turn, with the same response for continuous controls in Device and Mixer modes. [Agreed requirements](../../../PROJECT.md) · [Implementation plan](../plans/2026-11-24-encoder-tuning-implementation.md)

## What a turn does

Simon’s E16 (HW V5, SW 1.3.1) sent magnitudes 1, 2, 4 and 8 in both directions with Bitwig 6.1.7. These are relative MIDI movements. The extension’s decoder accepts each integer magnitude from 1 to 8. Before checkpoint 0.0.38, a continuous turn requested `delta / 256.0` from Bitwig’s current value. Computing speed from the time between messages would introduce another estimate on top of the firmware’s acceleration. Use the message magnitude for this trial. The 14-bit field in an LED-ring update describes display output. [E16 guide](../../sources.md#oxi-e16) · [Decoder](../../../src/main/java/io/github/wintocode/oxiwiggle/protocol/RelativeEncoder.java) · [Current routing](../../../src/main/java/io/github/wintocode/oxiwiggle/surface/RemoteSession.java)

| Magnitude | Continuous movement for one message | Accepted value |
| --- | --- | --- |
| 1 | Locked **Finest** | 1/16,384 of the range |
| 2 | Fixed | 0.78125% of the full range |
| 4 | Fixed | 1.5625% |
| 8 | Fixed | 3.125% |

The three percentages reproduce the working build at those speeds. All magnitudes from 2 through 8 use the signed movement divided by 256. Apply the direction once and send one normalized `inc(double)` to the live target. Each message stands on its own, including the first turn after a pause, a reversal, and interleaved turns on two knobs. Bitwig owns the starting value and endpoints. This applies to Device Remote Controls reported as continuous, Mixer volume, Shift-pan and the selected send’s amount. Mode, page, device, track and send-destination selection continue through their existing navigation handlers.

For a Remote Control reporting **N ≥ 2 positions**, one message requests one adjacent choice, at every speed. On a four-position control, positions run 1–2–3–4. From 2, either `+1` or `+8` requests 3. At 4, a clockwise turn stays at 4. Use the existing `sign(delta) / (N - 1)` increment. A one-position or unavailable control is idle. Follow the count Bitwig supplies, including when a plug-in presents an apparent list as continuous. Simon’s 16-value Unison test is the hardware precedent. [M2 hardware record](../../hardware-tests/m2.md)

## Archived resolution experiment

The following was the procedure for the probe build. Simon subsequently chose the fixed fine step above for physical turns. [Results](../../hardware-tests/encoder-tuning.md)

API 25 supplies a normalized base-value readback and a floating-point increment. It reports `-1` for a continuous range and exposes no universal smallest effective increment for that target. A formatted label can stay identical across several distinct values. The goal is a single slow nudge that produces a repeatable change. [Ranged values](../../sources.md#bitwig-controller-api) · [Relative writes](../../sources.md#bitwig-controller-api)

Use a temporary measurement build of OxiWiggle. In a scratch project, assign a continuous control from a Bitwig device, then one from a synth such as Serum 2, to Device Remote Control 1. A **Resolution test** section in Bitwig has a step exponent and two buttons for a single upward or downward trial. Each explicit click requests exactly one increment. Adjusting the exponent, startup and readback callbacks only update test state. Delayed callbacks only read values. The normal E16 path keeps its entry-ACK and target checks.

With playback stopped and automation writing off, begin at `1/256` of the range. Halve the request on each trial. Restore the same starting value in Bitwig before each comparison. At low, middle and high interior positions, test both directions. Record the full normalized before/after values and requested delta. Repeat both sides of the boundary between movement and no observed movement. Discard unsettled or interrupted trials. Where the plug-in exposes a separate numerical readout, compare it and record what it corroborates.

Choose the smallest **tested** candidate that moves in the requested direction in three out of three trials at every sampled position and direction on all tested targets. Recheck that candidate directly across the sample. Record finer results that apply to individual targets. This gives a shared fallback with a stated scope. If the measurements yield no common reliable candidate, or show that Bitwig moves while the target visibly rejects it, report the discrepancy before selecting the slow step. The experiment covered two controls. Hardware feel was subsequently checked in 0.0.38.
