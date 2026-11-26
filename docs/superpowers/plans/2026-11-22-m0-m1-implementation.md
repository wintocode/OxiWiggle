# OxiWiggle M0 and M1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce a loadable M0 extension, then prove Remote Mode entry, one LED ring, one native OLED label, and safe lifecycle behaviour on the E16 before starting M2.

**Architecture:** A thin Bitwig adapter owns the assigned MIDI ports and delegates lifecycle decisions to a pure Java session controller. Protocol and label builders produce complete, validated SysEx frames; injected output and scheduling allow deterministic tests without Bitwig or hardware. A JDK-only build produces one `.bwextension`.

**Tech Stack:** JDK 21, Bitwig Controller API 25, macOS `sh` scripts, Java standard-library tests. No Gradle, Maven, JUnit, or additional runtime libraries.

**Spec:** [API investigation and M0/M1 design](../specs/2026-11-22-bitwig-api-and-m1-design.md). Also read [PROJECT.md](../../../PROJECT.md), [AGENTS.md](../../../AGENTS.md), and the [supplied E16 guide](../../sources.md#oxi-e16).

**Hardware amendment, 2026-11-22:** The original task text below records the pre-hardware plan and its compact framing assumptions. On E16 SW 1.3.1, entry returned the exact prefixed ACK `F0 00 21 5B 02 01 06 53 F7`; prefixed `06 03` labels and `06 04` ring displayed, while the initial compact display attempt remained black. The implemented M1 path therefore accepts either exact empty entry ACK and sends prefixed labels/ring. See [the test record](../../hardware-tests/m1.md). Where a task below says to reject the prefixed ACK or send compact M1 display frames, this measured amendment supersedes it; malformed, extended, and display ACKs remain rejected.

## Global Constraints

- Preserve the exact user-facing capitalization: **OxiWiggle**.
- Target **one self-contained `.bwextension`**.
- Keep Bitwig as the source of truth for selected track/device/page and parameter values.
- Keep raw controller MIDI and outbound display SysEx out of instrument-track recording paths.
- Use `io.github.wintocode.oxiwiggle`, author `wintocode`, extension display name `OxiWiggle`, vendor `OXI Instruments`, and hardware model `E16`.
- Any future GitHub repository stays private until the user's explicit release decision.
- License OxiWiggle's implementation under MIT. Keep third-party documentation separate from that license.
- The build script must perform no network downloads.
- Compile against `com.bitwig:extension-api:25` with `javac --release 21 -encoding UTF-8 -proc:none`; Bitwig provides the API at runtime. Do not bundle its JAR or use private application classes.
- Use matching E16 USB MIDI Port 1 input and output. Declare one input and one output; index 0 in Bitwig's API is the assigned port, not an E16 port number.
- Entry: `F0 00 21 5B 02 01 06 55 F7`. Current entry ACK: `F0 00 21 5B 02 01 53 F7`. Cleanup exit: `F0 00 21 5B 02 01 06 00 F7`.
- Rings and labels have no update ACK. Exit has no acknowledgement. Never infer visible output or successful restoration merely from sending a packet.
- Target the latest stable E16 firmware and record the exact installed version for every hardware test.
- Encoders 9–13 remain undecided, and the original 1–8/14/15/16 mapping remains intact. M0/M1 add no parameter mappings, navigation, gestures, scrolling, framebuffer commands, or E16-side components.
- M2 starts only after the real Bitwig/E16 M1 acceptance test passes.

## Review Focus

1. Missing, mismatched, or changed build inputs must fail without leaving an old artifact looking newly built; validate paths containing spaces and invocation outside the repository (Task 1).
2. A display ACK with payload, an obsolete prefixed ACK, malformed input, or a duplicate ACK must not start/repeat display work (Tasks 1–3).
3. Timeout callbacks and pending flushes from an earlier session must not send after stop or affect a new session; handle stop during output (Tasks 2–3).
4. An output failure after entry or after sending labels must stop further display traffic, attempt cleanup at most once, and report uncertain hardware state (Tasks 2–3).
5. Correct packet logs can coexist with wrong MIDI-port selection, recording leakage, or failed reconnect; require actual Bitwig/E16 observations with an isolated route and a separate duplicate-route test (Task 4).

---

## File structure and execution boundaries

The folder currently contains documentation only and has no Git repository. No implementation checkbox is complete. JDK/API acquisition and live testing are prerequisites to be performed during execution; their hashes and observations must never be invented.

For the file lists below, `MAIN` means exactly `src/main/java/io/github/wintocode/oxiwiggle`, and `TEST` means exactly `src/test/java/io/github/wintocode/oxiwiggle`. These are notation for paths, not directory names or shell environment variables.

| File | Responsibility | First task |
| --- | --- | --- |
| `scripts/build.sh`, `scripts/test.sh` | Validate inputs, clean compile, run tests, package only production content | 1 |
| `dependencies/bitwig-api-25.properties`, `dependencies/jdk-21.properties` | Acquisition source, actual version, license/provenance information, hashes | 1 |
| `.gitignore`, `LICENSE`, `README.md` | Local build exclusions, project MIT license, reproducible development/install instructions | 1 |
| `MAIN/OxiWiggleExtensionDefinition.java` | Stable identity, API requirement, port counts, instance factory | 1 |
| `MAIN/OxiWiggleExtension.java` | Bitwig init/flush/exit, callbacks, output and scheduling adapter | 1 |
| `MAIN/protocol/E16Protocol.java` | Entry/exit frames, strict ACK check, structural frame validation; later ring/label framing | 1 |
| `src/main/resources/META-INF/services/com.bitwig.extension.ExtensionDefinition` | Fully qualified definition class registration | 1 |
| `TEST/AllTests.java`, `TEST/Checks.java` | Explicit suite runner and small assertion helpers | 1 |
| `TEST/protocol/E16ProtocolTest.java`, `TEST/ExtensionDefinitionTest.java` | Framing and metadata assertions | 1 |
| `MAIN/surface/RemoteSession.java` | Session state, entry attempts, cleanup; later one initial display flush | 2 |
| `TEST/surface/RemoteSessionTest.java`, `TEST/support/SessionFixture.java` | Deterministic fake output, time and flush requests | 2 |
| `MAIN/protocol/SevenBitPacking.java`, `MAIN/display/NativeLabels.java` | Documented packing and fixed-width ASCII label payload | 3 |
| `TEST/protocol/SevenBitPackingTest.java`, `TEST/display/NativeLabelsTest.java` | Packing vectors and label limits/layout | 3 |
| `src/test/resources/m1-labels.hex` | Fixed 100-byte M1 label-message oracle | 3 |
| `docs/hardware-tests/m0.md`, `docs/hardware-tests/m1-entry.md`, `docs/hardware-tests/m1.md` | Append dated runs, artifact identity, logs and observed outcomes | 1, 2, 4 |

Use the worktree skill when execution starts. Establish a local Git baseline if available, preserving the existing documents; do not create a remote or publish. Commit only the explicit files for each task, including tests and evidence. If filesystem permissions prevent Git writes, retain the files and report the missing commit rather than claiming one exists. Git setup must not overwrite existing work.

Run the tasks in order. M0 must actually load before advancing to M1. The entry-only M1 checkpoint must work before the first display experiment. When hardware is unavailable, retain the mock-tested checkpoint and record `NOT RUN`; do not mark the milestone accepted or begin M2.

## Task 1: Build and load the M0 extension

**Files:** Create the Task 1 files in the table, including `docs/hardware-tests/m0.md`. Modify `PROJECT.md` only to record demonstrated status. Do not create `.deps/` artifacts with guessed contents.

**Interfaces produced:**

- `sh scripts/test.sh [suite]`: clean-compile production/tests, run all suites if omitted, initially support `protocol` and `metadata`; reject unknown suite names. Exit 0 only on success, nonzero on prerequisites/compiler/assertion failures. Print `PASS <suite>` after each passing suite.
- `sh scripts/build.sh`: run all tests, then produce `build/OxiWiggle.bwextension`; no automatic installation or network access.
- `E16Protocol.enterRemoteMode(): byte[]`, `exitRemoteMode(): byte[]`, `isEnterAck(String hex): boolean`, `validateFrame(byte[] frame): void` are public static methods.
- `validateFrame` requires the exact six-byte E16 prefix, at least a command byte and F7, and MIDI-safe interior bytes; malformed frames throw `IllegalArgumentException`. Command-specific builders enforce payload lengths separately.
- `isEnterAck` accepts only the exact compact ACK as continuous hex, case-insensitively. Null, whitespace-containing, odd-length, non-hex, prefixed, truncated, or extended input returns false without throwing. Received Bitwig strings are continuous hex; human-readable test fixtures may contain spaces in the test helper.
- `AllTests.main(String[] args)` dispatches suites explicitly. Test classes expose `public static void run() throws Exception`. `Checks.hex(String): byte[]`, `bytes(String expectedHex, byte[] actual): void`, `equal(int expected, int actual): void`, `that(boolean, String): void`, and `throwsType(Class<? extends Throwable>, Runnable): void` are public static test helpers. Failed checks throw `AssertionError` and reach a nonzero process exit.

- [ ] **1. Acquire and verify the development inputs.** Obtain a JDK 21 distribution from its publisher and API 25 from `https://maven.bitwig.com/com/bitwig/extension-api/25/extension-api-25.jar`; do not download during builds. Set `OXIWIGGLE_JDK` to the chosen JDK home. Put the API at `.deps/extension-api-25.jar`. Record JDK vendor, full version, architecture, download source and archive hash in `dependencies/jdk-21.properties`. Record `coordinate`, `source`, `sha256`, acquisition date and supplied license information in `dependencies/bitwig-api-25.properties`; missing license information is recorded as missing, not assumed to be MIT. Check the publisher's verification material where available. Record newly consulted publisher references in `docs/sources.md`. If acquisition is unavailable, report the concrete missing input and stop before claiming a runnable build.

- [ ] **2. Confirm the acquired API matches the documented signatures.** Run the selected JDK's `java -version`, `javac -version`, `jar --list --file .deps/extension-api-25.jar`, and `javap -classpath .deps/extension-api-25.jar` for `ControllerExtension`, `ControllerExtensionDefinition`, `ControllerHost`, `MidiIn`, and `MidiOut` in their documented packages. Require the lifecycle, port, three-integer MIDI callback, hex SysEx callback, `sendSysex(byte[])`, `scheduleTask(Runnable,long)`, and `requestFlush()` APIs from the design. Record discrepancies before implementation; never substitute application-internal classes.

- [ ] **3. Write the M0 tests and minimal offline test command.** Add `Checks`, `AllTests`, framing/metadata tests, and `scripts/test.sh` sufficient to run the compiler and these tests against the missing production classes. Required framing assertions include:

```java
Checks.bytes("F0 00 21 5B 02 01 06 55 F7", E16Protocol.enterRemoteMode());
Checks.bytes("F0 00 21 5B 02 01 06 00 F7", E16Protocol.exitRemoteMode());
Checks.that(E16Protocol.isEnterAck("f000215b020153f7"), "compact ACK");
Checks.that(E16Protocol.isEnterAck("F000215B020153F7"), "uppercase ACK");
Checks.that(!E16Protocol.isEnterAck("f000215b02010653f7"), "obsolete prefix");
Checks.that(!E16Protocol.isEnterAck("f000215b0201533805000a7f7f7ff7"), "display ACK");
for (String bad : new String[] {null, "", "f", "xx", "f000215b020153",
        "f000215b02015300f7", "f000215b030153f7", "f0 00 21 5b 02 01 53 f7"}) {
    Checks.that(!E16Protocol.isEnterAck(bad), "invalid ACK");
}
Checks.throwsType(IllegalArgumentException.class,
    () -> E16Protocol.validateFrame(Checks.hex("F0 00 21 5B 02 01 04 80 F7")));
```

Also reject null/empty/too-short frames, incorrect prefix, missing F0/F7, and interior status bytes. Metadata tests instantiate the definition without a host and assert the exact name/author/vendor/model, version `0.0.1-m0`, API 25, one input/output, and the recorded persistent UUID. Later tasks update only the expected version while preserving identity.

- [ ] **4. Run `sh scripts/test.sh` and confirm a genuine initial failure.** Expect missing production types at this first bootstrap stage. Resolve toolchain/setup errors separately; do not confuse them with the expected missing implementation. No artifact should exist.

- [ ] **5. Implement M0 production classes and packaging.** Generate one UUID once and keep it as a literal in the definition and its test; never regenerate it at runtime. Use version `0.0.1-m0`. Implement the protocol methods above. `OxiWiggleExtension.init()` acquires the two assigned ports and registers callbacks before logging `getHostVersion()` and `getHostApiVersion()`; it emits no MIDI. `flush()` sends nothing; `exit()` logs stopping. Do not copy the template's MMC transport example or create a `NoteInput`. The service file contains exactly `io.github.wintocode.oxiwiggle.OxiWiggleExtensionDefinition` plus a newline.

Build-script contract: resolve the project root independently of the caller's directory; quote paths; use only `OXIWIGGLE_JDK/bin` Java tools; require Java 21 matching the recorded full JDK version and the recorded API checksum. Treat properties as data, never source them as shell code. `test.sh` cleans generated classes, compiles with `--release 21 -encoding UTF-8 -proc:none`, includes the API on the compile/test classpath, and makes `src/test/resources` available to tests when present. `build.sh` removes any previous target artifact before validating inputs, runs the complete test command, then creates a JAR directly named `build/OxiWiggle.bwextension`. Include only production classes/resources and `META-INF/LICENSE`; no API, test classes, test fixtures, reference documents, or extra JARs. Ignore `.deps/`, `.toolchains/`, and `build/`. Add MIT copyright `2026 wintocode`. Document exact setup/build/install commands in `README.md`.

- [ ] **6. Verify success and failure paths.** Run `sh scripts/test.sh` and `sh scripts/build.sh`; expect passing protocol/metadata suites and one artifact. Temporarily insert `Checks.that(false, "intentional runner failure")` into a registered test, run both commands, and require nonzero exit and no newly built/stale target artifact after `build.sh`; remove the deliberate failure and rebuild. Separately hide the API JAR, supply a changed JAR checksum, and select a missing JDK path: each build must fail clearly without an artifact or attempted download. Restore original inputs. Invoke the build by its absolute script path from `/private/tmp`; verify a build from a temporary project copy whose path contains spaces and with a JDK path containing spaces, reusing the verified inputs. These are one-time manual build checks, not a new shell-test framework.

- [ ] **7. Inspect and load the artifact.** Use `jar --list`, `unzip -p` on the service descriptor, and `javap -verbose` on the definition to verify registration, required contents, exclusions and class major version 65. Record SHA-256. Install by dragging the artifact onto Bitwig or copying it to `~/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension`. Add OxiWiggle under Controllers and assign matching E16 Port 1 input/output manually. In the console, observe init/exit and actual Bitwig/API versions while enabling, disabling and enabling again; require no error and no Remote Mode traffic. Record artifact hash, UUID, JDK/API identity, port names and dated observations in `docs/hardware-tests/m0.md`.

- [ ] **8. Commit the M0 checkpoint.** Stage the explicit Task 1 files and observed evidence; commit `build: establish loadable OxiWiggle M0 extension`. Keep the JDK and SDK out of Git. **Gate:** proceed to Task 2 only after the artifact both builds and loads successfully. If no live run is possible, report M0 build-tested and load-unverified.

## Task 2: Prove Remote Mode entry and cleanup

**Files:** Create `MAIN/surface/RemoteSession.java`, `TEST/surface/RemoteSessionTest.java`, `TEST/support/SessionFixture.java`, and `docs/hardware-tests/m1-entry.md`. Modify `MAIN/OxiWiggleExtension.java`, `MAIN/OxiWiggleExtensionDefinition.java`, `TEST/AllTests.java`, `TEST/ExtensionDefinitionTest.java`, and `README.md`.

**Interfaces:**

- Consumes Task 1 protocol methods and verified Bitwig APIs.
- `RemoteSession.Output.send(byte[] message): void` and `RemoteSession.Scheduler.schedule(Runnable callback, long delayMillis): void` are nested functional interfaces.
- Constructor: `RemoteSession(Output output, Scheduler scheduler, Runnable requestFlush, Consumer<String> log, Consumer<String> error)`.
- Public methods: `start(): void`, `onSysex(String hex): void`, `stop(): void`, `state(): State`. `State` values: `STOPPED`, `WAITING_FOR_ACK`, `READY`, `TIMED_OUT`, `FAILED`.
- `SessionFixture` owns a session, copying recording output, virtual scheduler and flush-request count. Expose `advanceTo(long absoluteMillis): void`, `sentHex(): List<String>` (lowercase continuous hex), `flushRequests(): int`, `errors(): List<String>`, and `public final RemoteSession session`. Its virtual scheduler runs due tasks in chronological/insertion order; no sleeps or wall-clock dependence. `failOnSend(int oneBasedAttempt): void` throws a test `RuntimeException` on that attempt; `onSend(int oneBasedAttempt, Runnable hook): void` invokes the hook during that attempt; `failNextSchedule(): void` makes the next scheduling call throw. Record a copy of each attempted send before its hook/failure, so tests distinguish attempts from successful delivery.

- [ ] **1. Write session tests and register suite `session`.** Pin timeout boundaries:

```java
var f = new SessionFixture();
f.session.start();
Checks.equal(1, f.sentHex().size());
f.advanceTo(999);  Checks.equal(1, f.sentHex().size());
f.advanceTo(1000); Checks.equal(2, f.sentHex().size());
f.advanceTo(2000); Checks.equal(3, f.sentHex().size());
f.advanceTo(3000); Checks.equal(4, f.sentHex().size());
Checks.that(f.session.state() == RemoteSession.State.TIMED_OUT, "entry timeout");
Checks.that(f.sentHex().equals(List.of(
    "f000215b02010655f7", "f000215b02010655f7",
    "f000215b02010655f7", "f000215b02010600f7")), "bounded entry and exit");
f.session.onSysex("f000215b020153f7"); // late ACK after timeout
f.session.stop();
f.advanceTo(10000);
Checks.equal(4, f.sentHex().size());
Checks.equal(0, f.flushRequests());
```

Add named cases for ACK before each timeout boundary, invalid/obsolete/display ACK ignored, one flush request after ACK, repeated ACK ignored, stop before start, repeated stop, stop while waiting, stop while ready, and stopping/restarting the same object while old callbacks remain queued. A new start sends a new entry and only that generation's timer may retry. Exercise an ACK delivered synchronously during the fake's initial send, so state must be established before calling external code. Force entry-output and scheduling failures: require `FAILED`, bounded cleanup and no later traffic. Test that start while already waiting/ready throws `IllegalStateException` without another send.

- [ ] **2. Run `sh scripts/test.sh session`.** Require failure because the session behaviour is absent; distinguish this from a prerequisite or runner failure.

- [ ] **3. Implement `RemoteSession`.** `start()` is allowed from `STOPPED`, `TIMED_OUT`, or `FAILED`, increments a generation token and resets attempt/cleanup state. Send three identical entry attempts, one immediately and two after 1000 ms intervals; at the next 1000 ms timeout enter `TIMED_OUT`. All scheduled callbacks capture the generation and check both generation and current state.

On exact ACK while waiting, enter `READY` before requesting one flush. At this entry-only checkpoint the extension's flush still sends nothing. Invalidate callbacks on stop/failure before best-effort exit. Attempt cleanup at most once per started generation, including when output throws; set the attempt flag before invoking output. Stop before any start sends nothing. Catch/report output or scheduling `RuntimeException`, stop session activity, attempt cleanup and report unknown hardware state; do not swallow arbitrary JVM errors. No heartbeat or inference of disconnection from silence. Generation tokens distinguish scheduled callbacks; the payload-free wire ACK itself has no session ID.

- [ ] **4. Run `sh scripts/test.sh session`.** Require all virtual-time, ACK and failure cases to pass without real timers or MIDI ports.

- [ ] **5. Wire the session into Bitwig.** Use version `0.0.2-m1-entry`, preserving the UUID. Adapt output to `validateFrame` then `MidiOut.sendSysex(byte[])`, scheduling to `host.scheduleTask(Runnable,long)`, flush requests to `host.requestFlush()`, and logging to `println`/`errorln`. Register callbacks before `session.start()`. The SysEx callback invokes `session.onSysex`; `exit()` calls `session.stop()` if initialization reached it. Log attempt numbers and complete outgoing M1 frames, accepted ACK and terminal states. Bound raw diagnostics to the first 100 short messages and first 20 unexpected SysEx messages per initialization, then one suppression notice; truncate unexpected SysEx diagnostics to 64 bytes. No MIDI input is echoed or bound.

- [ ] **6. Run `sh scripts/test.sh` and `sh scripts/build.sh`.** Require all existing suites plus `session` to pass and an installable artifact to be produced.

- [ ] **7. Run the real entry-only checkpoint.** Install this artifact; record its hash and the firmware version. Starting from the E16's normal scene view, enable it on matching USB Port 1 endpoints. Capture prefixed entry and the exact compact ACK, number of attempts and latency. Disable; capture one exit and observe normal E16 operation. Repeat with no reachable output: record either Bitwig refusing to start the controller or the bounded timeout sequence. No ring/label output belongs in this checkpoint. Save observations in `docs/hardware-tests/m1-entry.md`; unexpected ACK format or required E16-side setup is a stop-and-investigate result.

- [ ] **8. Commit the entry checkpoint.** Stage the explicit Task 2 files; commit `feat: add bounded E16 Remote Mode session`. **Gate:** entry and exit must be observed before the first display experiment. This checkpoint alone does not complete M1.

## Task 3: Send one ring and one native label

**Files:** Create `MAIN/protocol/SevenBitPacking.java`, `MAIN/display/NativeLabels.java`, `TEST/protocol/SevenBitPackingTest.java`, `TEST/display/NativeLabelsTest.java`, and `src/test/resources/m1-labels.hex`. Modify `MAIN/protocol/E16Protocol.java`, `MAIN/surface/RemoteSession.java`, `MAIN/OxiWiggleExtension.java`, `MAIN/OxiWiggleExtensionDefinition.java`, `TEST/protocol/E16ProtocolTest.java`, `TEST/surface/RemoteSessionTest.java`, `TEST/AllTests.java`, `TEST/ExtensionDefinitionTest.java`, and `README.md`.

**Interfaces:**

- `SevenBitPacking.pack(byte[] raw): byte[]` is public static; an empty array returns an empty array. Interpret input bytes unsigned. No unpacker is needed for M1.
- `NativeLabels.encode(String title, List<String> labels): byte[]` is public static and returns 80 raw bytes. Require printable ASCII 0x20–0x7E, title length at most 15, exactly 16 non-null labels each at most four characters; pad fields with spaces. Invalid/null arguments throw `IllegalArgumentException`.
- Add public static `E16Protocol.ring(int encoder, int red, int green, int blue, int amount, boolean bipolar): byte[]` and `labels(byte[] rawLabels): byte[]`. Ring validates encoder 0–15, RGB 0–255, amount 0–16383. Labels requires exactly 80 raw bytes. Both return complete compact frames and reject invalid/null input with `IllegalArgumentException`.
- Add `RemoteSession.flush(): void`. Existing constructor and entry points remain unchanged. It sends the fixed initial M1 display once per accepted session; it never changes Bitwig parameters.

- [ ] **1. Write packing and label tests; add them to `protocol` and new suite `display`.** Include the guide's independent oracle:

```java
Checks.bytes("06 03 48 16 64 60 39 01",
    SevenBitPacking.pack(Checks.hex("03 C8 96 64 60 39 01")));
Checks.bytes("00 00 00 00 00 00 00 00 01 00",
    SevenBitPacking.pack(Checks.hex("00 00 00 00 00 00 00 80")));
Checks.bytes("06 00 7F 00",
    SevenBitPacking.pack(Checks.hex("00 FF 80")));
for (int bit = 0; bit < 7; bit++) {
    byte[] raw = new byte[7];
    raw[bit] = (byte) 0x80;
    byte[] packed = SevenBitPacking.pack(raw);
    Checks.equal(8, packed.length);
    Checks.equal(1 << bit, packed[0] & 0xFF);
    for (int i = 1; i < 8; i++) Checks.equal(0, packed[i] & 0xFF);
}
```

Also pin raw lengths 0/1/6/7/8/80 to packed lengths 0/2/7/8/10/92, bytes 0/127/128/255, and the unpadded final group. Do not rely only on round-trip tests against another implementation of the same algorithm.

For `NativeLabels`, assert the complete raw frame using Java's `Arrays`, `Collections`, `ArrayList` and `StandardCharsets`:

```java
var labels = new ArrayList<>(Collections.nCopies(16, ""));
labels.set(0, "TEST");
byte[] raw = NativeLabels.encode("OxiWiggle M1", labels);
byte[] expected = ("OxiWiggle M1    TEST" + " ".repeat(60))
    .getBytes(StandardCharsets.US_ASCII);
Checks.equal(80, raw.length);
Checks.that(Arrays.equals(expected, raw), "complete raw label frame");
Checks.equal(92, SevenBitPacking.pack(raw).length);
Checks.equal(100, E16Protocol.labels(raw).length);
```

Test 15 title characters accepted/16 rejected, four label characters accepted/five rejected, 15/17 labels rejected, nulls and non-ASCII rejected. Use 16 distinct four-character labels and assert each at offset `16 + 4 * index` to prove ordering.

- [ ] **2. Write ring/framing tests and the fixed label-message fixture.** Pin the complete guide ring example and the M1 ring:

```java
Checks.bytes("F0 00 21 5B 02 01 04 06 03 48 16 64 60 39 01 F7",
    E16Protocol.ring(3, 200, 150, 100, 12345, true));
Checks.bytes("F0 00 21 5B 02 01 04 00 00 00 08 00 40 00 00 F7",
    E16Protocol.ring(0, 0, 8, 0, 8192, false));
Checks.throwsType(IllegalArgumentException.class,
    () -> E16Protocol.ring(16, 0, 8, 0, 8192, false));
```

Add amount 0/8192/16383, RGB 0/255, encoder 0/15, normal/bipolar flag, and invalid encoder -1, RGB -1/256 and amount -1/16384. Check amounts split into two seven-bit parts before packing and all interior wire bytes are MIDI-safe. Check labels reject lengths 79 and 81. Store this independent 100-byte fixture as the literal contents of `src/test/resources/m1-labels.hex`, then compare the complete generated label message against it:

```text
f000215b020103004f786957696767006c65204d31202000202054455354200020202020202020002020202020202000202020202020200020202020202020002020202020202000202020202020200020202020202020002020202020202000202020f7
```

The fixture is calculated from the documented grouping rule and fixed M1 text, not a hardware capture. In the test, load it from the test resources and decode via `Checks.hex`; the test cannot generate its own expected packet using the production packer.

- [ ] **3. Run `sh scripts/test.sh protocol` and `sh scripts/test.sh display`.** Observe failures from the absent builders; do not proceed on a runner or prerequisite error.

- [ ] **4. Implement only the stated builders.** Pack the entire raw payload in groups of up to seven with a leading MSB mask; never pack the header/category/command/F7 or pad the tail. `NativeLabels` owns string validation/padding; `E16Protocol.labels` owns length validation, packing and framing. A payload mask of `06` stays payload data, never a category marker.

- [ ] **5. Rerun the `protocol` and `display` suites.** Require all packing, field-validation and complete-packet assertions above to pass before integration.

- [ ] **6. Write session/display tests before adding `flush()`.** Reuse `SessionFixture`: start then flush must send only entry; valid ACK requests one flush but sends no display until `flush()` is called; the first ready flush sends exactly the 100-byte label frame then the 16-byte ring frame; repeated flushes or ACKs send nothing further. Test stop after ACK but before flush, timeout followed by flush, and stop/restart with old queued work. Use `failOnSend(2)` for labels failure and `failOnSend(3)` for ring failure after a first-attempt ACK; require `FAILED`, at most one cleanup attempt, a nonempty error log, no display retry and ignored later flush/timer calls. Use `onSend(2, f.session::stop)` to stop during labels; require exit and no subsequent ring. In a separate case, have that hook stop, start and supply a valid ACK: the old flush must not send its ring into the new session; a subsequent flush sends the new session's complete label/ring pair.

- [ ] **7. Run `sh scripts/test.sh session`.** Require the new flush cases to fail before implementing display delivery.

- [ ] **8. Implement initial display delivery.** On the first accepted ACK mark initial display pending before requesting flush. `flush()` checks `READY`, captures the current generation, consumes the pending flag before output, and checks both state and generation before each message. Use title `OxiWiggle M1`, first label `TEST`, remaining labels blank; ring encoder 0, RGB `(0,8,0)`, amount 8192, normal arc. Clear pending work on any terminal transition. Route output failures through the Task 2 failure/cleanup path; a ring failure must not cause labels to be resent indefinitely. Set version `0.0.3-m1` and make Bitwig's `flush()` delegate to the session when initialized. Log `labels sent` and `ring sent` only after each call returns, never `display verified`.

- [ ] **9. Run `sh scripts/test.sh` and `sh scripts/build.sh`.** Expect all four suites and Task 1 artifact inspection to pass; record the artifact hash for the next hardware run.

- [ ] **10. Commit the display checkpoint.** Stage the explicit Task 3 files; commit `feat: add M1 native labels and single-ring proof`. The artifact is ready for the Task 4 experiment; do not label it hardware-tested yet.

## Task 4: Accept M1 on the real E16 and preserve the evidence

**Files:** Create `docs/hardware-tests/m1.md`; modify `README.md` and `PROJECT.md` to match observed status. If a test exposes a bug, modify only its owning implementation/test files and rerun the affected task's checks before repeating the hardware case.

**Interfaces:** Consume the Task 3 artifact, its SHA-256, and the design's complete **Exact M1 hardware procedure**, steps 1–10. Produce a dated, reproducible test record with `PASS`, `FAIL`, or `NOT RUN` for every case. Record no pass without a concrete observation.

- [ ] **1. Prepare the run record and exact test environment.** Record macOS, actual Bitwig/API versions, JDK/API build identities, artifact version/hash, E16 hardware/encoder variant, firmware, USB route and exact port names. Recheck OXI's latest stable firmware and confirm it on the device; 1.3.1 was the research-time observation, not a permanent pin. Firmware maintenance uses the manufacturer's normal process and is never performed by the build or extension. Start an empty test project with duplicate E16 controller routes disabled and other E16-using applications closed.

- [ ] **2. Execute the positive handshake/display/input cases.** Follow design steps 2–5: start from the normal scene view; capture entry/empty ACK; observe dim green approximately half-full first ring, title `OxiWiggle M1`, first label `TEST`, other labels blank; turn/press/release encoder 1. Record channel-1 CC 1 and note 0 values, stable text, and no Bitwig parameter/transport changes. A photograph or explicit user observation is required for the physical rendering. No E16 script or custom preset may be installed for the test.

- [ ] **3. Execute routing isolation cases.** Follow design step 6 with an armed instrument track, All Inputs, and an empty recorded clip while turning/pushing encoders and cycling the extension to send display traffic. Inspect note/CC content and unintended instrument response. Then test a duplicate controller route separately if Bitwig permits it, record whether it leaks, remove it and repeat the isolated case. Log any SysEx routing evidence separately; the mere absence of SysEx events in a clip is not proof of complete routing isolation. The supported isolated configuration must pass.

- [ ] **4. Execute lifecycle and failure cases.** Follow design steps 7–9: three enable/disable cycles, missing/unassigned output, USB unplug/replug, and Bitwig quit/reopen. Check at-most-once cleanup, no stale queued sends, bounded attempts if the host starts without reachable hardware, and a fresh handshake/redraw after recovery. Record whether Bitwig restarts the controller or manual disable/re-enable is required. Confirm normal E16 use after connected shutdown; record what happens when an exit cannot be delivered. Do not promise seamless reconnect or infer connection state from silence.

- [ ] **5. Reconcile every result and any fix.** Maintain separate sections for documented expectations, measured results, and remaining questions. For each failure, preserve logs and add a failing test at the existing protocol/session seam where the behaviour is reproducible. Fix only M0/M1 scope, rerun relevant tests/build, record the new artifact hash, and repeat affected hardware cases. No undocumented fallback ACK/packing format, new preset/script requirement, or silent layout change is allowed. If hardware is unavailable or contradictory, stop with the working checkpoint and explicit failed/unrun cases.

- [ ] **6. Verify the accepted artifact and complete the handoff.** The final successful `sh scripts/build.sh` must precede testing that artifact on hardware. After acceptance, repeat only Task 1 archive exclusions/service/bytecode inspection and SHA-256 verification; hand over that exact file without rebuilding it. If code or build inputs changed since its successful build, rebuild and repeat the affected hardware cases before accepting the new artifact. Update README with the exact working install steps, confirmed port names, latest tested firmware and reconnect limitations. State M1 accepted only when real entry/ACK, visible ring/label, routing isolation and documented safe lifecycle/recovery have passed. Save the logs and observations, then commit `docs: record verified M1 hardware acceptance` only if verified; otherwise use a message accurately describing pending/failed validation. Stop here before M2.

## Plan review and execution handoff

The plan is complete as a planning artifact; all implementation and hardware checkboxes remain unchecked. The acquisition step intentionally records actual inputs at execution time instead of inventing hashes or claiming the current toolchain is installed. Source snapshots remain unchanged by this planning work.

Self-review covers all M0/M1 requirements, the five review-focus conditions, cross-task method names/signatures, exact packet constants, and explicit hardware gates. Relative decoding/scaling, parameter clamping, automation, eight-parameter feedback, navigation and scrolling are deferred to their respective later milestones.

Recommended execution method for this plan: **native execution**, implementing it in this session task by task with one independent review before final completion. The four tasks share protocol/lifecycle interfaces and physical hardware checkpoints, so parallel implementation offers little benefit. Subagent-driven implementation remains an option if the user prefers independent implementation/review for each task. Review the plan and choose the execution method before implementation.
