# OxiWiggle controller setup implementation plan

**Goal** Make the configured controller easy to recognise, provide an opt-in Remote Controls preference, and advertise the observed macOS MIDI ports. [Design](../specs/2026-11-24-controller-setup-design.md)

**Approach** Implement the three checkpoints independently with Superpowers executing-plans. Metadata and discovery belong in the extension definition. Device navigation already has a single accepted knob-15 path into the Bitwig adapter. Use it to associate a visibility change with a specific turn.

**Build and compatibility** Start from `0.0.32-mixer-targets`. Use JDK 21, Bitwig API 25, the existing shell scripts and one `OxiWiggle.bwextension`. Preserve UUID `7bd8998f-1030-4da1-991b-b86bb8a94357`, one MIDI input, one output and the existing recording isolation. Paths below use the package directory `io/github/wintocode/oxiwiggle/` under `src/main/java/` or `src/test/java/`.

**Review focus** Check upgrades of a saved controller, delayed device selection, manual closure of Remote Controls, a competing Port 1 integration, and manual assignment when a port is missing. Use the cases below at their respective checkpoints.

## 1. Controller name in Bitwig

- [ ] In Bitwig 6.1.7, record the picker, collapsed Settings → Controllers row, script console and saved assignments for the baseline. An unplugged E16 is sufficient to inspect the title. If the collapsed row already shows OxiWiggle, record how to identify it and close this item with the baseline.
- [ ] In `ExtensionDefinitionTest.java`, change the model assertion to `"E16 (OxiWiggle)"`. Keep the existing name, vendor and UUID assertions. Run `sh scripts/test.sh metadata` and see the model assertion fail.
- [ ] In `OxiWiggleExtensionDefinition.java`, return the proposed model string from `getHardwareModel()`. Use version `0.0.33-setup-name` in the definition and test. Build and archive using the steps below.
- [ ] Install the candidate with Bitwig closed. Reopen the same configuration. Check the title contains **OxiWiggle**, compare the controller count and saved ports, and verify the picker and console still identify OxiWiggle. Compare a fresh entry with the upgraded one using a separate test configuration. Record the actual strings in `docs/hardware-tests/controller-setup.md`. If upgrading loses assignments or the title still omits the brand, restore the previous checkpoint and record the host limitation. The preference can proceed independently.

## 2. Remote Controls preference

- [ ] Extend `surface/RemoteSessionTest.java` and `support/SessionFixture.java` with a hidden Remote Controls section on each of two adjacent identically named devices. Exercise physical CC 15 through the normal session input. With the setting off expect zero visibility writes. With it on and the move deliberately delayed, expect zero writes on the source and exactly one on the confirmed destination. Run `sh scripts/test.sh session` before implementing the behaviour.
- [ ] Register `host.getPreferences().getBooleanSetting("Show Remote Controls when navigating devices", "Device mode", false)` in `OxiWiggleExtension.init()`. Observe the current value, including changes while running. Leave persistence to Bitwig. Pass the setting to the existing navigation path. In `bitwig/BitwigRemotePage.java` read the observed device existence, position and `hasNext()/hasPrevious()` before issuing the existing relative selection.
- [ ] In `surface/RemoteSession.java` and `surface/RemotePage.java`, give the accepted, opted-in move a single pending reveal. Record the session generation, track ID, origin, expected adjacent sibling and the existing 750 ms navigation deadline before issuing the host call. Check immediately after issuing it and on subsequent selection updates. A still-selected origin can await the next update. Consume the request before calling `device.isRemoteControlsSectionVisible().set(true)` on a confirmed, existing destination. Skip the write if it is already visible. Check the parent chain when a nested selection could reuse the same position. If the target is ambiguous, skip the reveal and let ordinary navigation proceed. Bitwig’s comparison of two device proxies is available when a reference to the expected sibling is needed. [Object identity API](../../sources.md#bitwig-controller-api))
- [ ] Add cases for a mouse change with no pending move, a different track with the same sibling index, a blocked endpoint, a skipped repeat from the existing navigation throttle, and cancellation on timeout, error, preference-off, mode change, exit or a superseding move. A second observer callback or an automation update after manual closure must produce zero further writes. Test both immediate and delayed confirmations. Run the session suite and then the full build for `0.0.34-device-visibility`.
- [ ] Check that the checkbox appears under the controller’s settings and persists across a Bitwig restart. Using two devices in a visible device chain, hide their Remote Controls, enable the preference, and navigate with knob 15. Check the destination opens once, closing it by hand sticks, a mouse selection leaves its panel alone, and disabling the checkbox restores ordinary navigation. Repeat with a folded destination. If the visibility flag alone leaves the opted-in destination folded, test and add `device.isExpanded().set(true)` for that same confirmed reveal, then recheck manual closure. Record actual behaviour. Simulated input covers the session logic while the E16 is away.

## 3. macOS Port 1 discovery

- [ ] In `ExtensionDefinitionTest.java`, obtain the actual registration from `definition.getAutoDetectionMidiPortNamesList(PlatformType.MAC)`. Assert `getCount() == 1`, then assert each of `getPortNamesAt(0).getInputNames()` and `getOutputNames()` equals a one-element array containing `"OXI E16 Port 1"`. Assert zero pairs for `WINDOWS` and `LINUX`. Run the metadata suite and see the macOS count fail.
- [ ] In `OxiWiggleExtensionDefinition.listAutoDetectionMidiPortNames(...)`, call `list.add(new String[] {"OXI E16 Port 1"}, new String[] {"OXI E16 Port 1"})` for `PlatformType.MAC`. Preserve the manual port selectors. Build and archive `0.0.35-port-pair`.
- [ ] On the next hardware session, use a fresh Bitwig controller configuration, leaving Simon’s working entry available for rollback. With auto-add enabled, connect E16, confirm which integration is active, inspect both Port 1 selectors, then verify the entry ACK and OLED. Reconnect once and count active owners of Port 1. Repeat with auto-add disabled and manual selection. If a second E16 integration is offered, distinguish an offer from two active owners. Check an existing manual assignment survives the upgrade. Record firmware, macOS, Bitwig version, exact port names and observed results before describing setup as automatic in the README.

## Build, install and rollback

From the project root, use the version in the definition for the checkpoint being built. This example archives the first candidate. A fresh version identifies each subsequent candidate.

```sh
(
  set -eu
  version=0.0.33-setup-name
  archive="build/checkpoints/OxiWiggle-$version.bwextension"
  test ! -e "$archive"
  sh scripts/build.sh
  mkdir -p build/checkpoints
  cp build/OxiWiggle.bwextension "$archive"
  shasum -a 256 "$archive"
)
```

Expect `PASS protocol`, `PASS metadata`, `PASS session`, `PASS display` and the packaged extension. With Bitwig closed, copy the chosen archive over the existing filename and compare both hashes. Use the same procedure with the previous accepted archive to roll back.

```sh
archive="build/checkpoints/OxiWiggle-0.0.33-setup-name.bwextension"
installed="$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
cp "$archive" "$installed" && shasum -a 256 "$archive" "$installed"
```

Keep automated, Bitwig UI and E16 observations in the checkpoint record. The three offline implementations can progress while physical pairing and gesture checks are pending.
