# Transport implementation plan

Use subagent-driven development. Build a Java 21 / Bitwig API 25 trial of the [approved Transport design](../specs/2026-11-25-transport-design.md) and package a distinct checkpoint alongside the fast-turn trial.

- [x] Connect a Bitwig transport adapter to the extension and observe host state. Provide click and turn commands and a display snapshot. Check transport signatures, time signatures, position formatting, bounds and native record semantics.
- [x] Remap Device and global navigation, route Transport gestures, render overview and loop details, and update rings through the existing retry and session lifecycle. Check held buttons across mode changes and Shift transitions.
- [x] Migrate session fixture and assertions to the revised physical controls. Exercise navigation and transport input, host feedback, mode boundaries and duplicate presses.
- [x] Run the four test suites, package the extension, verify the archive, update the mapping docs and record the manual trial procedure.
