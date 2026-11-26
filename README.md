# OxiWiggle

OxiWiggle is a Java controller extension for the OXI E16 and Bitwig Studio. The hardware-tested checkpoint is `0.0.41-oled-text`. In the `0.0.48-name` candidate, turn knob 16 to move through Device, Mixer, Transport, Launcher and About. Turn knob 15 to select tracks on every page, or click it to expand or collapse a group. [OLED check and checkpoint SHA-256](docs/hardware-tests/m5.md#0041-oled-text) · [Mixer hardware checks](docs/hardware-tests/mixer.md)

On connection, the OLED shows **OxiWiggle**, **by wintoid** and the running version for 1.5 seconds. The first turn or press dismisses the welcome screen and performs its usual action. Turn knob 16 fully clockwise to **About** to see the same information again. [Welcome and About hardware check](docs/superpowers/specs/2026-11-25-welcome-about-design.md#acceptance)

**Device:** Knobs 1–8 edit the selected device’s Remote Controls. Knobs 13 and 14 select pages and devices. The OLED shows eight names in two rows of four and shows the parameter name and value while you turn.

**Mixer:** Each of the four columns controls one track. The top row turns volume, Shift-turns pan and clicks mute. The middle row turns send amount, Shift-turns send selection and clicks solo. Click the third row to arm. The window follows track selection until it reaches the last four tracks.

**Launcher** shows eight scene names in two rows of four. Press knobs 1–8 to launch a scene. Turn 13 to change pages. [Hardware trial](docs/hardware-tests/launcher.md)

**Transport** controls Arranger playback, recording, cues, looping, tempo and the metronome. The OLED shows position, tempo and playback state. [Control map and pending hardware checks](docs/hardware-tests/transport.md)

OxiWiggle is an in-progress native Bitwig controller extension for the OXI E16. The M0 load/lifecycle and M1 entry, native-label and LED-ring checkpoints were observed on Bitwig 6.1.7/API 25 and E16 SW 1.3.1/HW V5. The reviewed `0.0.3-m1` build passed the user's display and normal-exit recheck on 2026-11-23. Remote Mode also worked with changed normal scene knob/button assignments and preserved them on exit, supporting use of the extension without an accompanying E16 scene. Starting Bitwig while the E16 was unplugged and reconnecting it worked without manual re-enabling. A recorded instrument clip stayed empty while encoder 1 moved and the extension sent exit and fresh display messages. Explicitly unassigned-output and duplicate-route checks remain in the M1 hardware log.

## Build

The build uses JDK 21 and Bitwig Controller API 25. It requires no Gradle, Maven or runtime libraries. Obtain the API JAR from the URL in `dependencies/bitwig-api-25.properties` and put it at `.deps/extension-api-25.jar`. Use the recorded JDK version or update the provenance file after verifying another JDK 21 distribution. The scripts verify the JDK version and API JAR checksum; they do not download anything.

```sh
export OXIWIGGLE_JDK=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
sh scripts/test.sh
sh scripts/build.sh
```

The output is `build/OxiWiggle.bwextension`. Install it by dragging it onto Bitwig, or copy it to `~/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension`. Add OxiWiggle under Controllers and manually select the matching E16 USB Port 1 input and output. The diagnostic display test is in `docs/hardware-tests/m1.md`. The Controller Script Console can be opened by assigning a shortcut to `Show Control Script Console` in Bitwig's Dashboard → Settings → Shortcuts. The speech-bubble icon on the controller entry toggles on-screen controller visualizations; it does not open the console.

The MIT license covers OxiWiggle's own code. See the [development references](docs/sources.md) for publisher documentation.
