# Welcome and About

Show the same three centered lines on the E16 when Remote Mode connects and when the user selects About. The first line reads **OxiWiggle**, the second reads **by wintoid**, and the third shows the full version from the running extension definition.

## Approaches

A welcome screen identifies the extension on connection. A persistent About mode lets someone check the credit and version later during a session. Use both with a shared framebuffer layout. The E16 guide provides no documented command for supplying native screensaver artwork.

## Behavior

Show the welcome screen once per acknowledged Remote Mode session. After a brief interval show the Device controls. The first encoder turn or press dismisses the welcome screen immediately and performs its normal action exactly once.

Turn knob 16 clockwise from Device through Mixer, Transport and Launcher to reach About. About is the clockwise endpoint, including when more modes are added in future. Its screen stays visible until the user changes mode. Knob 15 retains its global track controls.

## Acceptance

On connection, read the full running version. Reconnect and turn knob 16 during the welcome screen. Mixer should appear on that first turn and remain visible when the welcome interval expires. Continue clockwise to About, compare its three lines with the welcome screen, and turn back one mode to Launcher.

Simon confirmed the welcome/version display, About navigation and first-input check on the installed 0.0.45 build.
