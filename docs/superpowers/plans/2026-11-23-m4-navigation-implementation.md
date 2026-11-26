# M4 navigation implementation plan

**Goal** — Add page, device and track navigation in three separately installable builds. Use Superpowers executing-plans on Sol Medium. Request Astra Medium before implementation review. [M4 design](../specs/2026-11-23-m4-navigation-design.md)

**Constraints** — Java 21, API 25, existing shell build, one self-contained extension, existing UUID and Port 1 pair. Keep the M3 parameter handling, recording route, remote handshake, display retry and generation checks. No new dependencies. Implement only after the proposed interaction is accepted.

## Common build and rollback procedure

- [x] Before editing, run `sh scripts/test.sh` and verify all four suites pass. Verify the archived M3 fallback using `shasum -a 256 build/checkpoints/OxiWiggle-0.0.8-m3.bwextension`. Expected SHA-256 is `e006f7b806cc4bb3cfa5dae1f78d1dd3c42fb42746d0be2f03c6880608370c56`.
- [ ] At each checkpoint set the indicated version in OxiWiggleExtensionDefinition and ExtensionDefinitionTest, run `sh scripts/build.sh`, and require four passing suites and a packaged extension. From the project root, close Bitwig and use the following commands, setting version to the checkpoint being tested.

```sh
version=0.0.12-m4-page
cp build/OxiWiggle.bwextension "build/checkpoints/OxiWiggle-$version.bwextension"
cp "build/checkpoints/OxiWiggle-$version.bwextension" "$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
shasum -a 256 "build/checkpoints/OxiWiggle-$version.bwextension" "$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
```

Reopen Bitwig with matching hashes and both E16 Port 1 endpoints. To roll back, close Bitwig and copy the verified M3 archive to the same installed filename. Record actual versions, hashes, results and any untested cases in docs/hardware-tests/m4.md. Keep the last working checkpoint until the next passes hardware testing.

## 1. Page navigation • 0.0.12-m4-page

- [x] In surface/RemoteSessionTest use the existing SessionFixture to begin with failing tests. CC 14 with values 01, 08, 7F, 78 must yield directions +1, +1, −1, −1 and zero parameter increments. Exercise zero, invalid data, wrong channels, notes, Shift, CCs 9–13 and the two pending axes. Assert silence before ACK and after stop, and exactly one call when the fake host throws after moving. Run `sh scripts/test.sh session` and `sh scripts/test.sh protocol` throughout.
- [x] Add a `void navigatePage(int direction)` operation to surface/RemotePage, its BitwigRemotePage implementation and SessionFixture. Route the page CC in RemoteSession.onMidi after READY and delta validation, independently of slot existence. In the adapter call the appropriate page relative method with cycling disabled. After issuing the command, refresh the normal labels and rings from the current page. Verify the full control frame on the first flush, no delayed reversion, and a failed label send retrying the destination without replaying navigation.
- [x] Draw the page-progress ring at encoder 14 with a prefixed individual-LED packet. Test all 16 segment values, deduplication, scaling for long page lists, single and missing pages, and redraw after re-entry.
- [ ] Verify purple advances as knob 14 moves forward and recedes as it moves back. Confirm the ring stays lit on a single-page device and an empty track. Retest with three distinctly assigned pages. Check that knob 14 changes the eight labels and rings immediately. Turn knob 1 immediately after changing pages and confirm only the destination changed. Check both boundaries, a single page and a device without pages.

## 2. Device navigation • 0.0.16-m4-device

- [x] Extend fixture routing tests to CC 15. Model two devices with identical page names and separate parameter targets. Assert a device move issues exactly one device command, leaves page and track commands untouched, and the next accepted edit uses the newly selected live slot. Run session tests red then green.
- [x] Observe the device name and existence. While encoder 15 is active, render its observed name and the DEV marker, including the missing-device case. Implement the 750 ms idle timer with an injected monotonic clock and one pending expiry per session generation. Test turns at 0 and 600 ms, with controls returning at 1350 ms, stale expiry after restart, and a label send failure spanning expiry. Turning encoder 14 during takeover immediately restores the normal view. In BitwigRemotePage register interest in device.hasPrevious() and hasNext() at initialization. Gate relative moves on device existence and the directional neighbour. Keep the same UI device cursor. Read device.position() and the sibling bank item count to draw the purple-and-orange ring. Build using the common procedure.
- [ ] Verify the actual device name is readable on the OLED. In a scratch project put three devices in series, with distinct mapped parameters on the first and last. Test both ends, a device with no Remote Controls, repeated page names, mouse selection, then device and page turns back-to-back. Click a nested device in a layer and establish actual previous/next order and whether the highlight follows. Record or resolve any discrepancy with the proposed same-chain rule.

- [ ] Put an FX Selector between two ordinary devices. Put a VST in each of two selector layers and a second device beside one VST. Note the active layer. Navigate across the outer chain with the panel collapsed and expanded. Verify the selector is a single stop in both directions and its active layer holds. Mouse-select the VST with a sibling, turn encoder 15 and verify selection stays in that layer. Return to the outer chain via the mouse and verify the OLED follows. Repeat with a container exposing two different insertion points, including an empty one. Record actual host behaviour before committing to any controller-only drill-down gesture.

- [x] Space same-direction device steps 180 ms apart, with immediate reversal, no queued steps and the OLED idle deadline extended by every valid turn. Verify with the fixture clock.
- [ ] On the three-device track, make a short, quick turn from each end and check whether the middle device is easier to select. Then keep turning to check sustained browsing and reverse direction to check the response.

- [x] Map the device ring across both endpoints, abbreviate one word per OLED cell, and use one step per burst for chains of up to three devices. Verify 0, 8 and 16 purple segments and the 100 ms quiet interval in the session fixture.
- [ ] Verify short turns on a three-device chain and read the device name on hardware. Check long-chain repeat speed separately.

- [x] Render the device name as continuous bitmap text, send a full prefixed framebuffer, and restore the normal native labels after the idle interval. Verify each screen mode, rename updates and retry across expiry.
- [ ] Test that the E16 displays “Mixwave Teezio Azul” with normal spacing, redraws when moving to another device, and restores the control labels. Try a long name that needs wrapping.

## 3. Track navigation • 0.0.18-m4

- [x] Extend routing tests to CC 16, including navigation away from a track with no device. Interleave track, device, page and slot inputs. Assert the exact command order, one write to the final live parameter, and suppression after stop/restart until fresh ACK. Test a navigation exception, with no duplicate move on flush or reconnect.
- [x] For knob 16, use a one-slot visible track bank following the cursor to report ring position and total. Verify that this order matches navigation on hardware. Register interest in track neighbour flags, and use the existing track cursor's relative operations. Observe the track name and existence. Render the selected track and TRK marker when encoder 16 turns. Test all three axes in quick succession, including an external rename during takeover. Reuse session refresh from the first checkpoint. Verify outbound frames contain current host values and the restored normal screen reserves every lower-row cell for future control-name presentation. Build and archive.
- [x] Proposed group push: dispatch channel-1 Note On for note 15 (hex 0F), velocity 127, once per press while READY. Ignore held repeats until release, accept Note Off or zero-velocity Note On as release, and clear held state on session restart. Ignore Shift (note 16), other buttons and ordinary-track presses. Use the live cursor's existence and observed group flag before calling isGroupExpanded().toggle(). Consume exceptions without replay. Cover these conditions in the session fixture and check the push produces no clip note on the armed-track test.
- [ ] Place a group containing two tracks and a nested group between two ordinary tracks. Verify that a collapsed header is reachable from both sides, and the next turn skips its hidden children. Press 16 to open it, verify selection stays on the header, and turn into its children. Repeat in reverse, collapse with the mouse, and verify the next turn follows the newly visible order.
- [ ] In the arranger test adjacent instrument, audio and empty tracks, a folded and expanded group, and the first/last reachable tracks. Record whether return and master tracks participate. Compare actual Bitwig highlight, device, page and E16 contents. Alternate mouse and E16 selection. Move quickly across an empty track and immediately edit a populated destination. Confirm only its parameter changes. Check reconnect and Bitwig quit/reopen.
- [ ] With an armed scratch track, navigate without touching a parameter, inspect the clip and automation lanes, then record a deliberate parameter edit on the destination. Verify only the intended automation was written. Record exactly what the clip view established for raw CCs. If another controller is available, select a different track from it and navigate from that selection.

## Review and acceptance

Request Astra Medium review after hardware testing. Focus on boundaries during rapid reversals, stale proxy reads immediately after a move, duplicate names hiding wrong-target edits, nested/group selection order, and restart or exceptions replaying navigation. Compare the tested and installed hashes. Update README, PROJECT and the hardware record with verified navigation semantics and outstanding cases. Use Sol Medium for any implementation fixes and retest the resulting artifact.
