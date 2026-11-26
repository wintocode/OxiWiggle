# macOS controller setup

The 0.0.49-port-pair candidate registers `OXI E16 Port 1` as both its input and its output on macOS. The automated test checks the pair Bitwig's API receives.

1. **Passed on 0.0.48-name.** Simon reported that the collapsed controller row reads `Oxi Instruments E16 (OxiWiggle)`. The installed extension matched the naming checkpoint by SHA-256.
2. **Upgrade check passed.** On 0.0.48-name, Simon confirmed that the existing controller showed `OXI E16 Port 1` for both MIDI input and MIDI output. After installing the verified 0.0.49-port-pair checkpoint and reopening Bitwig, Simon confirmed that both selectors still showed the same port before any manual changes.
3. **New entry check passed on 0.0.49-port-pair.** With the E16 unplugged, Simon removed the existing controller entry. On reconnect, Bitwig automatically added `OXI Instruments E16 (OxiWiggle)`. Both port selectors populated automatically and the controller worked.

**Reconnect check passed on 0.0.49-port-pair.** With Bitwig open, Simon unplugged and reconnected the E16. The OxiWiggle screen returned automatically. In Device mode, knob 1 moved the mapped Remote Control in Bitwig, and editing that control with the mouse updated the E16 ring.

The 0.0.48-name checkpoint can be reinstalled to repeat the comparison.
