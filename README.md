# OxiWiggle

OxiWiggle by **wintoid** turns the OXI E16 into a control surface for Bitwig Studio. Select a device in Bitwig and its Remote Controls appear on the E16, with names on the OLED and values on the LED rings. Turn a knob to edit the parameter, or move it in Bitwig and watch the ring follow.

You can also mix four tracks side by side, control the Arranger and launch scenes and record clips. OxiWiggle runs inside Bitwig as a Java controller extension and uses the E16’s built-in Remote Mode. All installation happens on the computer.

## Installation

The release download is ready to use. You can skip the source build and JDK setup.

1. Download [OxiWiggle.bwextension from the latest release](https://github.com/wintocode/OxiWiggle/releases/latest/download/OxiWiggle.bwextension) and connect the E16 over USB.
2. Drag `OxiWiggle.bwextension` onto Bitwig Studio. On macOS you can also quit Bitwig, copy the file to `~/Documents/Bitwig Studio/Extensions/`, then reopen Bitwig.
3. Open **Settings → Controllers**. On the tested macOS setup, connecting the E16 automatically added **OXI Instruments E16 (OxiWiggle)** and filled both port selectors. If the entry is missing, add **OxiWiggle** under **OXI Instruments**. On macOS, select **OXI E16 Port 1** for both MIDI input and MIDI output if the selectors are empty. Use just one controller integration for that pair.
4. The E16 briefly shows **OxiWiggle by wintoid** and the version, then opens in Device mode. Select a device with a populated Remote Controls page and turn knob 1. Its first parameter should move in Bitwig.

The hardware setup used during development was macOS, Bitwig Studio 6.1.7 and E16 firmware 1.3.1 (hardware V5). The extension requires Bitwig Controller API 25. Hardware checks cover Transport playback, Launcher scene selection and Stop, nine-scene scrolling and Shift-click recording. [Hardware test notes](docs/hardware-tests/)

## Finding your way around

The two rightmost knobs on the bottom row work the same way in every mode. **Turn 15 to select a track**, or press it to expand or collapse a selected group. **Turn 16 to select a mode**, clockwise through Device, Mixer, Transport and Launcher. Press **16** in any mode to show About and the installed version. Press any knob (or Shift) to close it and return to the same mode. The closing press only dismisses About.

### Device

Turn knobs **1–8** to edit the eight Remote Controls in the order shown on the OLED. While you turn, the display shows the parameter name and formatted value. Turn slowly for fine adjustments or quickly for larger sweeps. For parameters that Bitwig identifies as a list of choices, slow turns let you select adjacent entries.

Turn **13** to change the Remote Controls page and **14** to change the device on the selected track.

### Mixer

Each physical column controls the track named above it on the OLED. For example, knobs **1, 5 and 9** all act on the leftmost displayed track. The four-track window follows track selection, so the selected track can sit anywhere in the window.

| Row | Turn | Hold Shift and turn | Press |
| --- | --- | --- | --- |
| 1–4 | Volume | Pan | Mute |
| 5–8 | Send level | Choose send | Solo |
| 9–12 | | | Record arm |

### Transport

The OLED shows the playhead position in bars, beats, subdivisions and ticks, for example `1.4.4.03`. During playback the position refreshes up to ten times per second.

| Knob | Turn | Press | With Shift |
| --- | --- | --- | --- |
| 1–4 | | Play, stop, Arranger record, restart (in order) | |
| 5 | Jog by bar | | Turn to jog by beat |
| 6 | Select cue | Add cue | |
| 7 | Move loop start | Toggle Arranger loop | |
| 8 | Change loop length | | |
| 9 | Adjust tempo by 1 BPM | Tap tempo | |
| 10 | Metronome volume | Toggle metronome | |
| 11 | | Toggle Arranger overdub | Press for punch in |
| 12 | | Toggle Arranger automation write | Press for punch out |

### Launcher

Press **1–8** to launch the scene in the matching OLED slot. Press **9** to stop Bitwig playback. Turn **13** to scroll one scene. Hold **Shift** and turn **13** to jump eight. From scenes 1–8, one clockwise step shows scenes 2–9. With nine scenes, scrolling stops at 2–9 in either step size. Scrolling leaves playback running.

Hold **Shift** and press **1–8** to record into that scene’s empty slots on armed tracks. Occupied slots are skipped. Shift-press also starts playback from stopped when it queues a recording or finds one already queued on an armed track in that scene. The OLED reports when there are no armed tracks or available slots. This build supports Launcher recording in projects with up to 128 main tracks, including tracks inside groups.

For example, if two tracks are armed and only one has an empty slot in Scene 2, Shift-press Scene 2 to record into that slot and keep the other track’s clip intact. This was verified on hardware with 0.0.58.

## Build from source

The build scripts use a JDK and Bitwig’s API JAR. The current compiler check expects `javac 21.0.13.1`. Download the JAR from Bitwig using the source URL in the [API dependency record](dependencies/bitwig-api-25.properties) and save it as `.deps/extension-api-25.jar` in the repository (create the `.deps` directory first). The [JDK record](dependencies/jdk-21.properties) identifies the compiler used for the macOS build.

From the repository root, set `OXIWIGGLE_JDK` to your JDK home and run the build. This example uses Homebrew on Apple Silicon.

```sh
export OXIWIGGLE_JDK=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
sh scripts/build.sh
```

The script verifies the compiler version and JAR checksum, runs all four test suites, and writes `build/OxiWiggle.bwextension`. To run the tests on their own, use `sh scripts/test.sh`.

## Licence

OxiWiggle is [MIT licensed](LICENSE). Bitwig and OXI publish their own [developer references](docs/sources.md).
