# M5 implementation plan — control screen and first-release layout

**Goal** — Ship a testable static screen first. Select any further display interaction and new control assignments using the hardware result. Use Superpowers executing-plans on Sol Medium and request Astra Medium for review. [Design](../specs/2026-11-23-m5-control-layout-design.md)

## 1. Static eight-control screen • 0.0.19-m5-display

- [x] Run `sh scripts/test.sh` from the project root and verify the archived `build/checkpoints/OxiWiggle-0.0.18-m4.bwextension` SHA-256 is `25c7878dadd48529b231beaa6fcfea11a3518f87ec3209573fd3875f9e75d5de`. Keep Java 21, the pinned API 25 JAR, existing shell build, same UUID and Port 1 endpoints.
- [x] Extend the display suite first. Compare rendered frames for “Wet 50%” and “Wet 50?”, then check every printable ASCII character except space has a visible glyph and each previously missing symbol differs from the fallback. Extract the shared font, pixel addressing and bounded text drawing from display/SelectionScreen. Keep DEVICE and TRACK graphics byte-identical for existing glyphs.
- [x] Add display/ControlScreen with a renderer accepting the page title and exactly eight full slot labels. Pin all eight cell positions in two rows of four. Test five- and six-character words, a fifteen-character word and one longer, “Filter Cutoff”, empty, unnamed and non-ASCII input. Verify word wrapping, long-word splitting, an ellipsis only on overflow, and pixel bounds for all three lines of each cell. Run `sh scripts/test.sh display` red then green. Use Bitwig names before the four-character clipping currently performed in surface/RemoteSession.
- [x] Extend surface/RemoteSessionTest to decode the full 1024-byte payload of the prefixed framebuffer command. Exercise initial READY, page changes with identical titles, a rename after the fourth character, value-only automation, empty slots, selection takeover and restoration to a newly selected page. Preserve existing ring, group push and retry tests. Update expectations that currently assume a native-label frame for controls.
- [x] In RemoteSession derive the display from observed names and page state. Coalesce dirty flushes. Compare rendered pixels to the last successful screen before sending. Avoid rasterizing unchanged names on every value-only flush. Invalidate the previous screen cache before attempting any screen write. Test an uncertain send followed by a return to the previous screen, restart mid-send, and no redundant framebuffer when only the ring value changes. Run `sh scripts/test.sh session`, then all suites via `sh scripts/build.sh`.
- [x] Update extension version and metadata test, build and archive the checkpoint.
- [x] The accepted installed checkpoint and archive have matching SHA-256 hashes. Reinstall the accepted build with Bitwig closed:

```sh
version=0.0.22-m5-narrow
cp "build/checkpoints/OxiWiggle-$version.bwextension" "$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
shasum -a 256 "build/checkpoints/OxiWiggle-$version.bwextension" "$HOME/Documents/Bitwig Studio/Extensions/OxiWiggle.bwextension"
```

- [ ] In Bitwig select a page with eight different assignments. Turn knobs 1, 4, 5 and 8 and verify their names occupy the matching corners. Check readability of wrapped names at normal playing distance. Try long common prefixes, spaces, @, %, an empty slot and a mouse rename. Navigate page/device/track, then verify the restored names and parameter targets. Play automation and watch for flicker. Repeat on a third-party instrument and a HW CV device when available. Record observations and the installed hash in docs/hardware-tests/m5.md. To roll back, close Bitwig and copy the archived M4 extension to the same installed filename.

## 2. Readability decision

- [ ] Review the static checkpoint with Simon. If names remain ambiguous, agree on active-label scrolling or a temporary full-name view. Specify trigger, timing, navigation precedence and return to rest before building. Test with two labels that differ only at the end. Record the chosen implementation and candidate version before coding.

## 3. Remaining assignments and review

- [ ] Agree a mapping for encoders 9–13 and any additional presses. For each approved action record the matching API, expected input, host-side effect, display/ring feedback and behaviour when unavailable. Keep unassigned controls inert. Deliver one independently testable assignment or closely related group per build, preserving the accepted display checkpoint.
- [ ] Recheck immediate parameter edits after navigation, group behaviour, and raw MIDI/automation recording with the final mapping. Carry forward the M4 hidden-track ring case with its own test. Ask for Astra Medium review and record exactly which hardware and cases were exercised.
