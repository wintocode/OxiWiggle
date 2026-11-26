# OxiWiggle controller setup design

Make OxiWiggle recognisable in Bitwig, let the musician choose when Bitwig reveals Remote Controls, and simplify the first connection on macOS. Each change gets its own runnable checkpoint. The baseline is `0.0.32-mixer-targets` on Bitwig Studio 6.1.7, Controller API 25, and the E16 running SW 1.3.1. The two outstanding Mixer hardware checks have their own reminder. [Project status](../../../PROJECT.md) · [Implementation plan](../plans/2026-11-24-controller-setup-implementation.md)

## 1. Identify the configured controller

Simon saw **OxiWiggle** in the picker and script console, then **OXI Instruments E16** on the configured entry. The musician should be able to identify OxiWiggle by reading that entry with its settings closed. The extension already reports the name `OxiWiggle`, vendor `OXI Instruments` and model `E16`. The Dashboard guide describes the usual title and a separate extension selector. The exact field used for this entry in 6.1.7 needs a host check. [Current definition](../../../src/main/java/io/github/wintocode/oxiwiggle/OxiWiggleExtensionDefinition.java) · [Saved guide excerpt](https://www.bitwig.com/userguide/latest/the_dashboard/)

Use the existing installation as a control. If the collapsed row omits the extension name, try **E16 (OxiWiggle)** as the model string in a single candidate build. Keep the extension name, vendor, UUID and installed filename stable. Accept the candidate if the existing row shows the brand, the picker still identifies the E16, and Bitwig retains the configured instance and its ports. Check a fresh entry separately. If the host ignores the field or splits an existing controller into a second entry, drop the metadata change and document the observed UI before choosing another approach. [Definition API](../../sources.md#bitwig-controller-api)

The 0.0.48-name trial advertises the new model. After reloading Bitwig, confirm the version on About, read the collapsed entry in Settings → Controllers, then expand it and check both E16 Port 1 assignments. Open Add Controller to inspect the picker and cancel. The saved 0.0.47-slow-medium build is the rollback checkpoint.

## 2. Let knob 15 reveal Remote Controls

Add one checkbox in Bitwig’s controller preferences, under **Device mode**, labelled **Show Remote Controls when navigating devices**. Proposed default **off**. When enabled, turning knob 15 to another device reveals that destination’s Remote Controls once. Manually closing the section sticks until the next successful knob 15 move. Switching the preference on takes effect at the next move. Mouse selection, track or page navigation, startup, value changes and automation playback have no reveal action.

Bitwig provides both the native preference and a writable visibility property. Use the existing device cursor and act only on a confirmed navigation request. Discard an expired or superseded request. A blocked turn at a chain boundary never opens the current device. Try the visibility property alone in a folded device. If 6.1.7 also needs device expansion, expand only the destination of that opted-in gesture. This preference concerns the in-chain Remote Controls section. Project panels and plug-in windows stay under the musician’s control. [Controller preferences](../../sources.md#bitwig-controller-api)) · [Boolean settings](../../sources.md#bitwig-controller-api) · [Device visibility and expansion](../../sources.md#bitwig-controller-api)

## 3. Offer the macOS Port 1 pair

The two first-run dropdowns select MIDI **ports**. The tested E16 supplies channel-1 messages over **OXI E16 Port 1**. Advertise that exact name once for input and once for output through Bitwig’s macOS auto-detection API. Bitwig owns discovery, auto-add and the saved assignment. Manual pairing stays available. Leave Windows, Linux and other endpoint spellings for observed examples. [Port observations](../../hardware-tests/m0.md) · [Auto-detection API](../../sources.md#bitwig-controller-api)

Offline tests can verify the advertised pair. Only a fresh Bitwig configuration with the E16 attached can establish whether the host offers OxiWiggle, fills both selectors and reconnects cleanly with other matching integrations present. An unwanted second active integration is a reason to hold this checkpoint. All three changes use the same single Java extension and the existing dependencies.
