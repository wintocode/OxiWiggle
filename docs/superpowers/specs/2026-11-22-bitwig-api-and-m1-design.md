# OxiWiggle API investigation and M0/M1 proposal

2026-11-22. Research and proposed implementation only. No controller code, build scripts, JDK installation, GitHub repository, or hardware test was performed in this investigation.

Build one native Java `.bwextension` using the JDK and a small checked-in shell script. Establish M0 as a loadable extension with logging and pure Java protocol tests, then M1 as Remote Mode entry, exact ACK recognition, one ring, one label, and safe shutdown. M2 starts only after the real Bitwig/E16 M1 acceptance test passes.

**Verified from local files and official sources**

| Finding | Evidence and consequence |
| --- | --- |
| The initial folder contained only `PROJECT.md` and `AGENTS.md`; it was not a Git repository. | There is no existing code or build to preserve. |
| Installed Bitwig is 6.1.7. | Application `Contents/Info.plist`; captured in the reference provenance record. |
| Installed Javadocs document API versions through 25. | Local `api/new-list.html`. Select API 25 as the initial declared requirement and confirm the running host using `getHostApiVersion()` during M0. |
| Bitwig includes its own Java runtime, reporting 25.0.4.1. | Bundled runtime `release` file; it has no `javac` executable. A separate development JDK is needed. |
| The inspected bundled controller definition uses Java class version 65 (Java 21). The inspected public API classes use version 52. | Archive header inspection of `Controllers.bwextension` and `bitwig.jar`. Java 21 is a suitable initial compilation target; a Java 25 Javadoc generator is not a Java 25 extension requirement. |
| No standalone JDK was found by `java_home`; no Maven or Gradle was on PATH. | `/usr/bin/java` and `javac` are macOS launcher stubs here. No build has been attempted. |
| Bitwig's public extension build selects `com.bitwig:extension-api:25` and Java 21. | [Official Bitwig build](https://github.com/bitwig/bitwig-extensions/blob/main/build.gradle). This identifies the published dependency; it does not require adopting that build system. |
| Java extension discovery uses `META-INF/services/com.bitwig.extension.ExtensionDefinition`. | Verified in the installed Java template and bundled controller archive. Its content is the fully qualified definition class name. |
| OXI currently marks firmware 1.3.1 as latest. | [Official release](https://github.com/OXI-Instruments/OXI-E16-Releases/releases/tag/1.3.1), checked 2026-11-22. Recheck before hardware testing. The physical E16's installed version is not yet known. |

The published API JAR could not be retrieved in this environment: shell downloads failed DNS resolution and the web reader could not retrieve the Maven endpoint. Its hash and exact contents remain acquisition checks for M0. API signatures below were checked against the installed Javadocs; no running Bitwig controller behaviour has yet been tested.

**Build approach and dependencies**

| Approach | Tradeoff | Decision |
| --- | --- | --- |
| JDK 21 + checked-in `sh` scripts | Small inspectable build; direct control over input JAR and tests. Initially supports the current macOS development machine. | Recommended for M0/M1, reflecting the user's concern about build-tool supply chains. |
| Gradle Wrapper + JUnit | Convenient dependency resolution and test reporting; adds downloaded build and test tooling. | Unnecessary for the initial scope. |
| Maven Wrapper + JUnit | Conventional Java lifecycle; also adds build and test tooling and artifact-renaming configuration. | Unnecessary for the initial scope. |

Required development inputs: a verified JDK 21 distribution, the operating system shell/tools, and Bitwig's official API 25 JAR. The API is a compile-time reference supplied by Bitwig at runtime, comparable to a host-provided assembly reference in C#. Use the standard Java library for implementation and a small explicit test entry point; no third-party runtime or test libraries.

Acquire `com.bitwig:extension-api:25` from `https://maven.bitwig.com/com/bitwig/extension-api/25/extension-api-25.jar`. Record the actual source, version, license information, and SHA-256 after acquisition. A locally recorded hash detects changes after acquisition; it does not independently prove publisher identity. Keep the JAR in ignored `.deps/`, outside the distributable. Do not use private Bitwig implementation classes, extract a replacement SDK from the application, or bundle Bitwig's JAR.

The build script must perform no network downloads. It checks the selected JDK and API hash, compiles with `javac --release 21 -encoding UTF-8 -proc:none`, compiles and runs the pure Java tests, and uses `jar` to produce `build/OxiWiggle.bwextension`. Disable annotation processing because this project needs none. Clean generated classes before compiling; package only production classes, resources, and the project's license. Do not produce an artifact after a failed test. Archive order/timestamps should be fixed if byte-identical output is required; initially require repeatable inputs and a clean successful build, and record the JDK patch version. See the official [javac](https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html) and [jar](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jar.html) manuals.

**Proposed source structure**

```text
scripts/build.sh
scripts/test.sh
dependencies/bitwig-api-25.properties    # source, exact version, acquired SHA-256
.deps/extension-api-25.jar              # ignored local input
src/main/java/io/github/wintocode/oxiwiggle/
  OxiWiggleExtensionDefinition.java      # identity, API 25, one MIDI input/output
  OxiWiggleExtension.java                # Bitwig lifecycle and MIDI adapter
  protocol/E16Protocol.java              # complete frames and exact ACK recognition
  protocol/SevenBitPacking.java          # documented payload packing
  surface/RemoteSession.java             # entry attempts, timeout, lifecycle guards
  display/NativeLabels.java              # ASCII, 16 title bytes + sixteen 4-byte fields
src/main/resources/META-INF/services/com.bitwig.extension.ExtensionDefinition
src/test/java/io/github/wintocode/oxiwiggle/
  AllTests.java                          # explicit runner; assertions throw on failure
  protocol/                             # packet and packing tests
  surface/                              # fake output and virtual-time session tests
  display/                              # native label fields and limits
docs/sources.md                         # publisher references
docs/hardware-tests/                    # dated observations and logs when tests occur
LICENSE                                 # MIT for OxiWiggle's implementation
```

Create files only as each milestone needs them. Add `bitwig/` bindings from M2 onward. No empty device/navigation implementations, scrolling system, custom framebuffer, E16-side script, preset, or separate runtime application belongs in M0/M1. Encoders 9–13 remain undecided, and the original 1–8/14/15/16 mapping remains intact.

Use `io.github.wintocode.oxiwiggle`, author `wintocode`, extension display name `OxiWiggle`, vendor `OXI Instruments`, and hardware model `E16`. Generate one persistent extension UUID during M0. Any future GitHub repository stays private until the user's explicit release decision. Saving third-party references does not make them part of OxiWiggle's MIT grant.

**Supported Bitwig APIs checked locally**

For the installed API documentation, see [the developer references](../../sources.md#bitwig-controller-api). These are documented signatures and behaviour, not hardware results.

| Purpose | Verified API | M0/M1 use |
| --- | --- | --- |
| Extension entry points | `ControllerExtension.init()`, `flush()`, `exit()` | Create API objects/callbacks in `init`; send pending output in `flush`; stop session and attempt exit in `exit`. |
| Port declaration | `ControllerExtensionDefinition.getNumMidiInPorts()`, `getNumMidiOutPorts()` | Return 1 for each. Bitwig manages the selected ports. |
| Get assigned ports | `ControllerHost.getMidiInPort(int)`, `getMidiOutPort(int)` | Index 0 identifies the extension's assigned port, which must be E16 USB Port 1. |
| Incoming SysEx | `MidiIn.setSysexCallback(SysexMidiDataReceivedCallback)`; callback `sysexDataReceived(String)` | Callback receives hex text. Parse bytes with case-independent hex decoding and validate the complete frame. |
| Incoming short MIDI | `MidiIn.setMidiCallback(ShortMidiDataReceivedCallback)` | The three-integer callback receives status/data1/data2. Use the verified overload; log bounded diagnostic input only in M1. |
| Outgoing SysEx | `MidiOut.sendSysex(byte[])` | Send one validated complete frame per call. API documentation warns invalid SysEx can terminate the extension from API 19 onward. |
| Timers/output | `ControllerHost.scheduleTask(Runnable, long)`, `requestFlush()` | Millisecond delay; no cancellation handle is returned. Guard scheduled work by session state and generation. |
| Diagnostics | `println(String)`, `errorln(String)`, `showPopupNotification(String)` | Version, lifecycle, attempts, and exact M1 packet logs. No external logging library. |
| Host version | `Host.getHostVersion()`, `getHostApiVersion()` | Log actual running values on startup and compare with the inspected installation. |
| Discovery names | `listAutoDetectionMidiPortNames(AutoDetectionMidiPortNamesList, PlatformType)`; list `add(String[], String[])` | Begin with manual port assignment. Add exact observed platform names after the first hardware session, never guessed names. |

Do not call `MidiIn.createNoteInput(...)`: its documented purpose is exposing a track input and forwarding matching messages to the application. Controller callbacks alone are the intended M1 path. Do not echo input MIDI, create virtual MIDI ports, or forward SysEx to instruments. This design avoids an extension-created recording route; the manual test must still check duplicate Generic Controller assignments and other routes on the same E16 port.

For the first M1 session, manually selecting the two Port 1 endpoints in Bitwig and receiving the matching device ACK establishes the correct connection. This is the initial interpretation of M1's detect/open requirement; automatic name-based discovery follows observed port names.

For later milestones, the API already provides selection-following track/device cursors and `Device.createCursorRemoteControlsPage(8)`. The latter follows the UI-selected page; its named/filter overload is explicitly independent of UI page selection. `CursorRemoteControlsPage.pageNames()` and `selectedPageIndex()` enable investigating selection by name without hard-coding `PERFORM`. `Parameter.value()`, `name()`, and `touch(boolean)` exist; normalized `inc(double)` is on `SettableRangedValue`. Observers mark values interested; values read directly need `markInterested()` during initialization. Navigation of nested chains, automation gestures, and audible/recorded resolution remain M2–M4 experiments.

**Protocol corrections from the detailed guide**

The [E16 documentation](../../sources.md#oxi-e16) is the current documented reference. It has no exact firmware-version stamp; test its behaviour against the latest installed stable firmware. The older worksheet and original project extract are not the authority where they conflict with this guide.

| Item | Current guide | Earlier extract or remaining limitation |
| --- | --- | --- |
| Entry request | `F0 00 21 5B 02 01 06 55 F7` | The category is required when outside Remote Mode. |
| Entry ACK | `F0 00 21 5B 02 01 53 F7` | Earlier extract included `06`. Current ACK has exactly zero payload bytes. |
| Requests after entry | Compact command or `06` + command | Replies stay compact. |
| Exit | Compact `00` or prefixed `06 00`; no ACK | Use the prefixed form for cleanup even when entry status is uncertain. Actual restoration after exit needs observation. |
| Ring colours | Raw RGB 0–255, then pack | Worksheet said 0–127. Use modest colour values for the visual test. |
| Packing | Leading mask for each group of at most seven raw bytes; mask bit i holds raw byte i's MSB | Final group has no padding; empty input produces no mask. Pack the concatenated raw payload only. |
| Native title/labels | 16 title bytes with 15 visible characters; 16 labels of four bytes each | 80 raw bytes become 92 packed bytes; compact message totals 100 bytes. |
| Ring and label confirmation | No update ACK/NACK | Successful send does not establish visible output. Confirm on the E16. |
| Display ACK | `53` with packed six-byte payload for partial display operations | Must never satisfy the empty entry ACK check. No partial-display commands in M1. |
| USB routing | Matching E16 USB MIDI Port 1 input and output | Exact names displayed by Bitwig on this machine still need recording. |

The worked packing example is `03 C8 96 64 60 39 01` → `06 03 48 16 64 60 39 01`. This is sufficient to implement packing without guessing. Java bytes are signed, so treat byte values as unsigned with `& 0xFF` before bit operations; validate integer fields before narrowing.

**Minimal M1 behaviour**

1. In `init`, acquire the assigned ports, register receive callbacks first, log versions, and start a new session generation. No Bitwig parameters are created or changed.
2. Send the category-prefixed entry request. Accept only the exact compact, payload-free ACK while awaiting entry. Unexpected prefixed ACKs from older references are logged for investigation, not silently treated as current-firmware proof.
3. Proposed timeout policy: allow three total entry attempts, at approximately 0, 1, and 2 seconds; fail at approximately 3 seconds if no valid ACK arrives. The guide permits identical entry retries. These timings are engineering defaults, not manufacturer guarantees. On failure, stop all display work, invalidate pending callbacks, attempt prefixed exit once, and report the result as unknown device state unless observed.
4. On ACK, mark the session entered and request a flush. Send one native-label message followed by one ring message. Repeated ACKs must not redraw or reschedule work.
5. Display title `OxiWiggle M1` (space-padded to 16 bytes), label `TEST` at encoder index 0, and blank labels at indices 1–15. Send the full frame once; no periodic refresh is needed for static labels.
6. Set only physical encoder 1's ring (protocol index 0) to RGB `(0, 8, 0)`, amount `8192`, normal arc. Its raw record is `00 00 08 00 40 00 00`; its packed payload is `00 00 00 08 00 40 00 00`. Complete compact message: `F0 00 21 5B 02 01 04 00 00 00 08 00 40 00 00 F7` (16 bytes). This should show a dim green, approximately half-full ring.
7. Encoder turns and pushes only generate diagnostic logs. They must not change a parameter, trigger transport, or change the test frame. Do not retain the installed template's example MMC transport handling.
8. On `exit`, invalidate pending timers/output before sending `F0 00 21 5B 02 01 06 00 F7` as best effort. Attempt this cleanup at most once per session generation: if timeout already attempted it, a subsequent `exit()` does not repeat it. Do not wait for an exit ACK. No sends are allowed from a stale callback after stop.

The session's output and timer are small injected interfaces, with fake implementations for tests. A unit test can advance virtual time and supply ACKs without Bitwig or an E16. Production uses only Bitwig callbacks and scheduling; no background thread, Java Sound device access, or companion process.

Do not equate a quiet MIDI input with disconnection. No MIDI port connectivity callback or heartbeat was established in this API investigation. On physical reconnect, observe whether Bitwig restarts the extension; if it does not, explicitly disable/re-enable it for M1. Record this limitation rather than adding unproven automatic recovery. After an application crash or cable loss, `exit()` may not run and delivery is impossible while disconnected.

**Short implementation sequence and checks**

| Step | Deliverable | Required evidence |
| --- | --- | --- |
| M0 | JDK build/test scripts; definition, lifecycle logger, service entry, exact empty-frame encoder/parser; MIT license | Clean build, intentional test failure produces nonzero status, archive inspection, extension loads/disables in Bitwig without errors, actual host/API versions logged. No Remote Mode output yet. |
| M1 entry checkpoint | Session controller and injected output/timer, entry/ACK/timeout/exit | Fake transport tests plus a real Port 1 entry/exit capture. This checkpoint alone is not M1 completion. |
| M1 display checkpoint | Verified packer, native label frame, fixed ring | Golden-vector tests and full manual hardware procedure below. |
| M1 acceptance | One installable artifact and recorded hardware results | Handshake, visible ring, visible label, input isolation, exit and reconnect observations. Only then consider M2. |

Meaningful pure Java tests: exact entry/exit frames; current ACK recognition; rejection of display ACKs, wrong header, truncated frames, invalid hex, and the obsolete prefixed ACK; guide's high-bit packing vector; empty/1/6/7/8/80-byte packing including the unpadded tail; each of the seven mask bits; raw 0/127/128/255; ring amount boundaries 0/8192/16383 and invalid field rejection; title 15-character limit, 16 transmitted bytes, four-character labels, exact slot order and ASCII validation; 80→92→100 label lengths; no display output before ACK; bounded retries, late/duplicate ACKs, shutdown during entry and queued redraw, stale timer invalidation, and fresh-session restart. Add relative CC scaling, parameter clamping, and scrolling tests when their M2/M3/M5 functionality exists.

**Planned build and install procedure**

These commands describe the scaffold to be built; the scripts do not exist yet.

1. Obtain a JDK 21 and the official API 25 JAR. Verify provenance, inspect the API artifact, and record hashes. Put the API at `.deps/extension-api-25.jar`. Set `OXIWIGGLE_JDK` to the JDK home; scripts use its `bin/javac`, `bin/java`, `bin/jar`, and `bin/javap` consistently.
2. Run `sh scripts/build.sh` from the project root. It runs tests and produces `build/OxiWiggle.bwextension`. `sh scripts/test.sh` runs the same tests independently. Both fail clearly for missing prerequisites or failures.
3. Inspect the archive using the selected JDK's `jar --list --file build/OxiWiggle.bwextension`. Require the definition and extension classes, the service descriptor naming `io.github.wintocode.oxiwiggle.OxiWiggleExtensionDefinition`, and the MIT license. Reject bundled `com/bitwig/`, test classes, reference documents, or extra library JARs. Check the produced class target with `javap -verbose`.
4. Record the artifact SHA-256. Copy only the extension to `~/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension`, or drag it onto Bitwig. Then add OxiWiggle under Controllers and assign the matching E16 USB Port 1 input and output. These installation methods are documented by [Bitwig](https://www.bitwig.com/support/technical_support/how-do-i-add-a-controller-extension-or-script-17/).
5. For M0, enable and disable the extension and capture version/init/exit logs. Expect no outgoing Remote Mode messages. For M1, use the procedure below with that milestone's artifact.

**Exact M1 hardware procedure**

1. Record date, macOS version, Bitwig version/API, E16 hardware/encoder variant, exact firmware version, artifact hash, and the input/output names Bitwig displays. Recheck OXI's latest stable release and confirm that version on the E16. The OXI App may be used for firmware maintenance; it is not part of OxiWiggle's runtime. Do not install an E16 script or preset.
2. Close other applications using the E16. Use USB, leave the E16 in its ordinary scene view, and create an empty Bitwig test project. Disable duplicate Generic Controller assignments for the same port. Open Bitwig's controller console.
3. Enable OxiWiggle with E16 USB Port 1 assigned both ways. Record the exact entry request and compact ACK, the attempt number, and elapsed time. Pass only if the E16 enters Remote Mode without additional device-side setup. Stop for investigation if no correct ACK arrives; do not continue to display claims.
4. Check the first physical encoder: dim green, approximately half its ring illuminated. Check the native OLED: title `OxiWiggle M1`, first label `TEST`, all remaining labels blank. Capture a photograph or explicit observed result. Logs saying `labels sent`/`ring sent` are insufficient; these commands have no acknowledgement.
5. Turn encoder 1 slowly in both directions and press/release it. Verify channel-1 CC 1 and note 0 in the diagnostics, and that the static labels remain visible. Nothing in Bitwig should move. Record actual message values, without claiming 14-bit encoder input.
6. Arm an instrument track with All Inputs selected. Record an empty clip while turning/pushing E16 encoders and while disabling/re-enabling the extension once to exercise display traffic. Inspect for note or CC events and check that the instrument receives no E16 control data. If another controller route exposes the same hardware port, repeat with that route present in a separate test, document the leakage, and remove the conflicting route. OxiWiggle cannot control other extensions' routing.
7. Disable OxiWiggle normally. Capture one prefixed exit request, no subsequent queued output, and the E16's observed return to normal operation. Repeat enable/disable three times. Do not expect an exit ACK.
8. Start with the E16 unavailable or its output unassigned. Confirm clean handling: Bitwig may keep the extension stopped for unavailable ports; if it starts without a reachable device, there must be no label/ring traffic and entry attempts must stop at the configured limit. Record which behaviour occurs.
9. During a successful session unplug/replug USB. Record whether Bitwig calls exit/init and whether the E16 keeps Remote Mode or its display. If necessary, disable/re-enable OxiWiggle and verify a fresh handshake/redraw. Repeat by quitting/reopening Bitwig with USB connected. A required manual restart must be documented; do not report seamless reconnect.
10. Save the logs and observed pass/fail for every step under `docs/hardware-tests/`. M1's core go/no-go is real entry/ACK, ring, label, and documented safe lifecycle behaviour. Compilation, mock tests, and the guide alone cannot satisfy it.

**Remaining assumptions and questions**

- The supplied guide describes the current stable firmware. Its exact firmware coverage is unstamped; confirm compact ACK, packing, RGB, and title behaviour on the installed latest release.
- Bitwig's installed docs match the running controller host. M0 startup logs and the first live run must confirm this.
- One-second entry waits and three attempts are sufficient on this hardware. Record latency and adjust only from observations.
- Exact OS port names, startup without E16 configuration, state restoration after exit, reload/unplug behaviour, and routing isolation are unverified.
- No refresh-rate or throughput limit is needed to send M1's two one-shot display messages. Measure sustainable rates later before scrolling or broad feedback updates.
- JDK acquisition and the published API JAR's hash/license/contents are outstanding prerequisites; neither has been installed or downloaded here.
- No protocol documentation gap now blocks designing M1. If hardware disagrees with the guide, preserve captures, stop at the failing checkpoint, and revise the test seam before broader features.
