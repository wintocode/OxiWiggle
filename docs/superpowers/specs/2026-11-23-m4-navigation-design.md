# M4 design — navigate from the E16

From the hardware-tested M3 page, turn encoder 14 to change Remote Controls page, 15 to change device, or 16 to change track. After each move, Bitwig's visible selection and the E16's eight parameter controls must agree. The decisive test is to navigate, turn encoder 1, and see exactly which Bitwig parameter moved. [M3 baseline](../../hardware-tests/m3.md)

## Proposed interaction

| Encoder | Clockwise | Counterclockwise | Selection screen |
| --- | --- | --- | --- |
| 14 | Next page on this device | Previous page | Eight-control screen |
| 15 | Next device in the current chain | Previous device | DEVICE |
| 16 | Next track in Bitwig's GUI navigation order | Previous track | TRACK |

Advance one neighbour per accepted MIDI message, including accelerated messages. Device chains with up to three devices advance once per same-direction burst and rearm after 100 ms without input. Longer device chains space same-direction steps at least 180 ms apart. The first step and direction changes are immediate. Extra turns during that interval extend the selection screen without queuing moves. Stop at either end. This lets the musician find the first item by turning backwards. Page selection includes ordinary pages with any name. For a track or device change, accept the device and page Bitwig selects. Preserve manual selection from the mouse or another controller.

Knob 14 stays lit while OxiWiggle is active. Purple shows progress through the page list, including the current page. Orange shows the remaining portion. A device with one page shows all purple. With no selected page the ring shows all orange. Page position scales to 16 segments, with at least one orange segment until the last page. Verify segment order and colour on hardware. Use the same palette for knob 15’s position in its sibling device chain and knob 16’s position in the visible track order. On knob 15, scale from all orange on the first device to all purple on the last. A single device is all purple. Show all orange when there is no selection.

## Group tracks

Encoder 16 should follow visible arranger order in both directions. The group header is itself a selectable track. Turning past a collapsed group skips its hidden children. Turning forward from an expanded group visits its first visible child. Apply the same rule to nested groups and to changes made with the mouse.

Proposed push action for encoder 16 only: when an existing group header is selected, one press toggles its expansion and keeps selection on the header. A press on an ordinary track does nothing. Use the existing cursor’s observed isGroup() and isGroupExpanded().toggle(). API 25 exposes the latter as a writable value for child visibility. Validate the actual traversal on Bitwig 6.1.7 using the existing cursor and following track bank. Keep this gesture in the track-navigation checkpoint. [Track API](../../sources.md#bitwig-controller-api)

## Device containers and insertion points

Proposed M4 scope: knob 15 moves among siblings in the current chain. A container is a selectable device with its own Remote Controls. From the outer chain the next turn goes to the device after the container, irrespective of whether its panel is expanded. Leave the knob 15 press available while testing this workflow. To work inside a container, select a child device in Bitwig. OxiWiggle should follow that selection and navigate within that child chain. Mouse selection of the parent or another outer device returns to the outer chain.

An FX Selector can have several layers. Other devices can offer named FX slots, parallel chains or drum pads, each with its own device sequence. A generic “expand” gesture would also need a way to choose the insertion point and get back out. Treat controller-only descent as a separate design question after testing sibling navigation. Device browsing must preserve the active selector chain and audio routing. API 25 distinguishes selecting a device in a layer, slot or pad from changing the selector’s active chain. [CursorDevice](../../sources.md#bitwig-controller-api) · [ChainSelector](../../sources.md#bitwig-controller-api)

## Temporary selection screen

Reserve the normal OLED for the eight parameter controls. Knob 14 updates the normal screen directly as Bitwig selects each page. A valid turn of 15 or 16 temporarily takes over the whole OLED, even at a boundary. Show the active selection type and the current Bitwig name. Turning another navigation encoder switches the view immediately. Device and track turns start or extend this view. A page turn returns to the eight controls immediately. Knobs 1–8 still edit their live targets and their rings still follow Bitwig.

The device view uses the E16’s documented 128 × 64 framebuffer and a small built-in bitmap font. It lays out up to three lines, wraps at words when possible, and adds an ellipsis if the name exceeds the screen. Send one complete prefixed framebuffer when the name changes and use the native labels packet to restore the normal view. A missing selected device displays “No device”. The active screen always reflects the observed Bitwig selection, including changes made with the mouse. Test framebuffer drawing and switching modes on the E16. The track checkpoint can reuse this layout after the device test.

Proposed initial idle time is 750 ms after the latest valid navigation turn. The E16 reports relative MIDI turns, so silence is the signal that movement has stopped. At expiry, render the eight-control screen from the current Bitwig page. A page or device change during navigation may have replaced every assignment. On a fresh session start in the normal view. Tune the delay on hardware so the name stays readable through a slow turn.

A picker with multiple page names and a highlighted selection remains a future display option.

## API and ownership

Reuse M3's single track, device and eight-slot page in BitwigRemotePage. Verified against the pinned API 25 JAR and the saved Bitwig 6.1.7 reference:

- Page exposes selectNextPage(false) and selectPreviousPage(false). The boolean controls cycling. Skip a request if there is no valid page. [CursorRemoteControlsPage](../../sources.md#bitwig-controller-api)
- Device inherits selectNext(), selectPrevious(), hasNext() and hasPrevious() from Cursor. The existing no-argument track.createCursorDevice() refers to the UI device selection. Use those relative operations with an existing device and an available neighbour. An empty chain is a valid stopping point. No automatic descent into layers, pads or FX slots is proposed. Test how Bitwig treats a device already selected inside a nested chain. [Cursor](../../sources.md#bitwig-controller-api) · [CursorTrack](../../sources.md#bitwig-controller-api)
- Bitwig 6.1.7 rejects setCursorNavigationMode at runtime. Use the track cursor’s relative operations, with a visible track bank following the cursor for ring feedback. Use the existing track and select one available neighbour. Track navigation must work on an empty track. Folded groups, effect/master tracks, and whether cursor moves visibly select in the arranger require observation in Bitwig. [CursorNavigationMode](../../sources.md#bitwig-controller-api)

On channel 1, CCs 14–16 decimal are 0E–10 hex. Reuse the existing relative decoder and READY gate. Route each input once. Read required cursor availability through observed/interested API values. After issuing a move, refresh from live proxies through the M3 observer and flush path. A navigation request can take effect even when the host call throws. Consume it once and refresh on error. A local selection index, deferred replay, or speculative display would risk divergence from mouse selection.

## Host behaviour to establish

Verify rapid turns, reversals at endpoints and a parameter turn immediately after navigation. In particular, if a selection observer arrives late, the previous parameter must never receive an edit intended for the new page. If a reproducible stale edit or cursor/UI mismatch occurs, capture the exact MIDI and observer order on that checkpoint. Resolve the binding or ordering issue before enabling the next axis. Include duplicate names so a matching title cannot conceal a wrong target. Group and nested-chain traversal outside the proposed order requires a product decision if the available API behaves differently.

[Implementation plan](../plans/2026-11-23-m4-navigation-implementation.md)
