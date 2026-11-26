# Encoder tuning evidence

## 0.0.37-encoder-probe

This checkpoint supersedes 0.0.36 for live measurements. The probe reports an inconclusive result if the sampled value reverses direction. It uses a monotonic two-second window and logs elapsed milliseconds. If the final callback arrives late, it can invalidate the result when the readback changed or the last in-window sample is more than 250 ms short of the deadline. Movement and stability must come from in-window samples.

Built with JDK 21 and Controller API 25 for Bitwig 6.1.7. All four automated suites and packaging passed, including regressions for reversals in both directions, movement first seen after the deadline, and a late callback after stable in-window readings.

Archive SHA-256 is `8e7b1edcfdd56f74e13100f9c0acf2804af491a8a9c7cdc587dc190b046f330f`. The installed extension still matched the `0.0.32-mixer-targets` archive at build verification.

Close Bitwig and install from the project root.

```sh
cp build/checkpoints/OxiWiggle-0.0.37-encoder-probe.bwextension \
  "$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
```

1. Reopen Bitwig and load a scratch project. Stop playback and turn automation writing off. Open the Controller Script Console.
2. Assign an unmodulated continuous parameter to the selected device's first Remote Control. In the OxiWiggle gear settings, open **Resolution test** and set **Step exponent** to 8. Click **Test up** once.
3. Wait for the console result. Record the signed request, starting value, final value and observed change. Leave the controls and selection alone during the two-second sample window. A changed knob, selection or mapping can invalidate a trial. Bitwig has to load the configured controller instance for the settings to appear.
4. Restore the same starting value before trying the next exponent. Repeat near 25%, 50% and 75% in both directions on a native continuous control and an exposed synth parameter. Follow the plan for repeated trials around the boundary.

The sampling window uses Bitwig's scheduled callbacks, so a busy host can delay the result. The probe measures the observed Bitwig Remote Control value. A plug-in readout can separately corroborate target movement. Avoid editing or remapping by mouse during the trial. The extension cancels on observed binding changes and valid E16 turns.

The live settings button and readback worked on Serum 2's `Main Vol` assigned to Device RC1. Exponent 8 requested `+0.00390625`. The value moved from `0.5368695259094238` to `0.5407757759094238`, an exact match. The last two in-window readings agreed, and the callback at 2041 ms read the same value. The exact plug-in version and current firmware were not supplied with this result.

The reverse trial at exponent 8 also passed. The observed change matched the requested `-0.00390625`, returning from `0.5407757759094238` to `0.5368695259094238`. The last two in-window readings and the callback at 2037 ms agreed.

The positive trial at exponent 9 started at the same baseline and reached `0.5388226509094238`. The observed change exactly matched the requested `+0.001953125`. The last two in-window readings and the callback at 2049 ms agreed.

The reverse trial at exponent 9 matched `-0.001953125` and restored the baseline `0.5368695259094238`. The last two in-window readings and the callback at 2041 ms agreed.

The positive trial at exponent 16 moved from that baseline to `0.5368847846984863`, exactly matching the requested `+1.52587890625E-5`. The last two in-window readings and the callback at 2040 ms agreed.

The reverse trial at exponent 16 matched `-1.52587890625E-5` and restored the baseline. The last two in-window readings and the callback at 2046 ms agreed.

The positive trial at exponent 24 moved from the baseline to `0.5368695855140686`, exactly matching the requested `+5.960464477539063E-8`. The reverse trial matched the negative request and restored the baseline. In each trial, the last two in-window readings agreed with the late callback at 2045 ms and 2029 ms respectively.

The positive trial at exponent 25 moved from the baseline to `0.5368695557117462`, exactly matching the requested `+2.9802322387695312E-8`. The reverse trial matched the negative request and restored the baseline. The last two in-window readings agreed with the callbacks at 2033 ms and 2032 ms, respectively.

The positive trial at exponent 32 moved from the baseline to `0.5368695261422545`, exactly matching the requested `+2.3283064365386963E-10`. The reverse trial matched the negative request and restored the baseline. The last two in-window readings agreed with the callbacks at 2049 ms and 2041 ms, respectively.

The positive trial at exponent 53 moved from the baseline to `0.5368695259094239`, exactly matching the requested `+1.1102230246251565E-16`. The last two in-window readings and the callback at 2031 ms agreed. This is one double-precision representable step at that baseline. The result establishes Bitwig Remote Control readback resolution at that point. Effective resolution of the underlying parameter remains unverified.

Bitwig Tool's `Gain` passed both directions at exponent 53. It moved from `0.57` to `0.5700000000000001` and back to `0.57`, exactly matching the requested deltas of `±1.1102230246251565E-16`. The last two in-window readings agreed with the callbacks at 2030 ms and 2044 ms, respectively.

Bitwig 6.1.7 and API 25 with checkpoint 0.0.37 also reported the following Gain round trips at exponent 53. The requested deltas were `±1.1102230246251565E-16`.

| Starting value | Value after Test up | Observed change up and down | Callback times up and down |
| --- | --- | --- | --- |
| `0.1850000000000001` | `0.1850000000000002` | `±8.326672684688674E-17` | 2047 ms, 2032 ms |
| `0.7999999999999999` | `0.8` | `±1.1102230246251565E-16` | 2043 ms, 2027 ms |

Both reverse trials restored the exact starting value. All four results had matching final in-window readings and late callbacks. A pass here means steady movement in the requested direction. At the lower position the reported movement was 75% of the requested step.

Serum 2 Main Vol also passed both directions at the following positions at exponent 53. The observed changes exactly matched the requested deltas of `±1.1102230246251565E-16`.

| Starting value | Value after Test up | Callback times up and down |
| --- | --- | --- |
| `0.1561499190330502` | `0.1561499190330503` | 2047 ms, 2041 ms |
| `0.8061499190330507` | `0.8061499190330508` | 2038 ms, 2045 ms |

Both reverse trials restored the exact starting value. At a second midpoint, `0.5861499190330505`, all three exponent 53 round trips moved to `0.5861499190330506` and returned exactly to the baseline. The signed changes matched `±1.1102230246251565E-16` on all six trials. The last two in-window readings agreed with the callbacks at 2031/2049 ms, 2042/2052 ms and 2045/2043 ms.

At a second low position, Serum 2 Main Vol completed three exponent 53 round trips from `0.08114991903305006` to `0.08114991903305017` and back. All six signed changes exactly matched the request. The final in-window readings and late callbacks agreed at 2046/2037 ms, 2034/2031 ms and 2043/2034 ms.

At a second high position, Serum 2 completed one round trip from `0.9011499190330505` to `0.9011499190330506` and back. Two further down trials passed from successive lower starting values, ending at `0.9011499190330503`. All four signed changes matched the request. The elapsed times were 2030, 2046, 2042 and 2039 ms. The probe rejected two overlapping clicks as busy, which count as no additional trials.

Serum 2 then completed three high-position round trips from `0.9011499190330503` to `0.9011499190330504` and back. All six signed changes exactly matched the exponent 53 request. The final in-window readings and late callbacks agreed at 2043/2024 ms, 2051/2046 ms and 2038/2032 ms.

Serum 2 completed three same-baseline pairs at each of the low, middle and high positions. No independent plug-in readback was reported.

Tool Gain repeat checks at exponent 53 on Bitwig 6.1.7 / API 25:

| Starting value | Result | Observed change | Callback times |
| --- | --- | --- | --- |
| `0.07500000000000027` | 3 round trips via `0.07500000000000037` | `±9.71445146547012E-17` | 2048/2032, 2040/2032, 2041/2038 ms |
| `0.43499999999999994` | 3 round trips via `0.43500000000000005` | `±1.1102230246251565E-16` | 2032/2034, 2044/2043, 2046/2045 ms |
| `0.9199999999999995` | 3 up trials with no movement | `0.0` | 2032, 2029, 2029 ms |
| `0.9049999999999996` | 1 up trial to `0.9049999999999998` | `+2.220446049250313E-16` | 2047 ms |
| `0.9049999999999998` | 2 down trials with no movement | `0.0` | 2047, 2043 ms |
| `0.9049999999999998` | 3 round trips via `0.9049999999999999` | `±1.1102230246251565E-16` | 2049/2036, 2032/2042, 2047/2040 ms |

Direct double addition changes the value in both stalled cases. The probe therefore accepted each request and called Bitwig's relative increment API. The pattern is consistent with rounding somewhere in Bitwig's increment or parameter conversion path. The logs do not identify the exact operation. Exponent 53 fails the shared repeatability criterion. Exponent 52 is the next candidate to test, starting in the Tool Gain region where 53 stalled.

Tool Gain passed three midpoint round trips at exponent 52, from `0.5` to `0.5000000000000002` and back. All six changes exactly matched the requested `±2.220446049250313E-16`. The final in-window readings agreed with the late callbacks at 2036/2039 ms, 2048/2024 ms and 2042/2021 ms.

Tool Gain also passed three high-position round trips at exponent 52, from `0.8999999999999998` to `0.8999999999999999` and back. All six observed changes were `±1.1102230246251565E-16`, half the requested step. The final in-window readings agreed with the late callbacks at 2039/2026 ms, 2044/2041 ms and 2031/2042 ms. The exact high baselines that stalled at exponent 53 were not reproduced.

At the low position, Tool Gain passed three exponent 52 round trips from `0.0650000000000003` to `0.06500000000000049` and back. All six observed changes were `±1.942890293094024E-16`, 87.5% of the requested step. The final in-window readings agreed with the late callbacks at 2049/2030 ms, 2023/2037 ms and 2042/2039 ms. Tool Gain passed 18 of 18 trials across three sampled positions.

Serum 2 Main Vol passed 30 of 30 trials at exponent 52. Each position had three up/down pairs. The observed change matched the request of `±2.220446049250313E-16` in every trial, and each down click restored the exact starting value. The final in-window readings agreed with the late callbacks.

| Starting value | Value after Test up | Callback times for each up/down pair |
| --- | --- | --- |
| `0.06499999999999967` | `0.06499999999999989` | 2027/2040, 2047/2036, 2032/2034 ms |
| `0.22499999999999976` | `0.22499999999999998` | 2049/2049, 2049/2025, 2044/2036 ms |
| `0.44499999999999995` | `0.4450000000000002` | 2044/2027, 2035/2030, 2047/2033 ms |
| `0.7350000000000002` | `0.7350000000000004` | 2029/2051, 2036/2038, 2043/2044 ms |
| `0.9300000000000004` | `0.9300000000000006` | 2027/2043, 2040/2042, 2046/2031 ms |

The `2⁻⁵²` request is the smallest tested request that consistently moved Bitwig's normalized readback in both directions on both controls across the sampled positions. The finer `2⁻⁵³` step stalled on Tool Gain. The result covers Remote Control readback on Bitwig 6.1.7 / API 25. Physical encoder feel, Mixer targets and the plug-in's effective resolution remain unverified.

Record Bitwig and plug-in versions, E16 firmware if connected, and the smallest repeatable adjustment before choosing Finest. To roll back, copy the `0.0.32-mixer-targets` archive to the same destination with Bitwig closed. [Measurement plan](../superpowers/plans/2026-11-24-encoder-tuning-implementation.md)

## 0.0.38-encoder-fine

Simon chose `1/16,384` for the slowest continuous step after comparing the number of relative messages required at 14-, 16- and 32-bit scales. The step is relative to the current Bitwig value, including values set by mouse or automation. Movement magnitudes 2–8 keep the existing response. The trial buttons, probe and raw message dumps are removed.

All four test suites and packaging passed. Archive SHA-256 is `d2d7385025a380d7cfaa63f2d5259407a1b2ac2e26aaf9be149a94a62fd072b2`. The original probe binary and its source snapshot are under `build/checkpoints/`.

With Bitwig closed, copy `build/checkpoints/OxiWiggle-0.0.38-encoder-fine.bwextension` to `$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension` and reopen Bitwig. In a scratch project, compare slow and fast turns on Tool Gain or Serum 2 Main Vol. Check one stepped control, then check Mixer volume, Shift-pan and send amount. Confirm the Resolution test settings are gone. Roll back by copying the `0.0.37-encoder-probe` archive to the same destination with Bitwig closed.

Review of 0.0.38 found no blocking implementation issues. All four suites passed on a fresh compilation and every archived class matched the compiler output. On hardware, pay particular attention to the transition from magnitude 1 to 2, which increases the movement per message by a factor of 128.

Simon described the slow-to-fast transition on the requested Tool Gain check as “decent”. Keep the 1/16,384 fine step for this checkpoint.

Simon confirmed that slow turns in both directions on Serum 2 Unison made it easy to select the adjacent voice count. The stepped-control check passed on hardware.

On the Mixer volume and Shift-pan check, Simon found the response pretty good, though a little slow. He preferred to keep the current sensitivity. Simon also found the Mixer send amount easy enough to set with knob 5. Simon confirmed that only the MIDI ports appear in the controller settings. The Resolution test section and its controls are gone. All planned 0.0.38 hardware checks passed.

## 0.0.36 review record

The review identified a false pass after sampled direction reversals and a false pass when eight delayed scheduler callbacks extended the observation window. Both traces are covered by the 0.0.37 regression tests.

## 0.0.42 fast-turn trial

After using the E16 to make music, Simon requested twice the previous continuous increment at magnitude 4 and three times at magnitude 8. These now request 8/256 and 24/256 of the normalized range per message. Magnitudes 1 and 2 retain their previous increments. The multiplier varies linearly between 2, 4 and 8 for intermediate input magnitudes.

All four suites and packaging passed. The trial archive is `build/checkpoints/OxiWiggle-0.0.42-fast-turns.bwextension` (SHA-256 `caa8dbfea8081c697040aa41878c856179a08b1e3a417737655843a0f759fd9b`). To try it, sweep the same continuous Device parameter in both directions at brisk and fast speeds, then slow down to land on a value. Repeat on Mixer volume. Hardware feel is pending. The 0.0.41 archive is the rollback.

## 0.0.46 medium-turn trial

On 0.0.45 Simon found slow and fast turns worked well, while medium turns felt a little sluggish. The physical trial did not measure which relative CC magnitudes occurred. For this candidate, magnitudes 3–6 request 5.5, 10, 13.5 and 16.5 units per message, respectively, in units of 1/256 of the range. Previously they requested 4.5, 8, 11.25 and 15. Magnitudes 1, 2, 7 and 8 retain the accepted response.

After reloading, check the version on About. Sweep the same continuous Device control at a comfortable medium speed in both directions and slow down to land on a value. Compare with the saved 0.0.45-about checkpoint if needed. Simon confirmed About on the installed 0.0.46 and reported that slow-medium turns still felt too slow.

## 0.0.47 slow-medium trial

Treat relative magnitudes 2 and 3 as the likely slow-medium region. The actual codes at Simon’s turning speed have not been measured. Increase their continuous movement from 2 to 3.5 and from 5.5 to 7 units of 1/256 per message. The fine step and magnitudes 4–8 keep the previous values.

Reload Bitwig, confirm 0.0.47-slow-medium on About, and sweep the same continuous parameter just above fine speed in both directions. Check how easily it starts moving and how precisely you can land as you slow down. The archived 0.0.46-medium-turns build is the comparison point.
