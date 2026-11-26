# macOS controller setup

The 0.0.49-port-pair candidate registers `OXI E16 Port 1` as both its input and its output on macOS. The automated test checks the pair Bitwig's API receives. Automatic assignment on a connected E16 is pending.

1. With the installed 0.0.48-name build, collapse the controller row and record the name Bitwig shows.
2. Record the Bitwig version, E16 firmware, USB Multi Port setting and existing port assignments. Install the 0.0.49-port-pair checkpoint using the README install steps. Check that the existing controller still has its original assignments.
3. In a fresh Bitwig controller configuration with the built-in E16 integration available, connect the E16. Record which integration Bitwig offers or adds. If needed, add OxiWiggle manually. Before touching either selector, record its input and output and whether another enabled entry owns either port.
4. With only OxiWiggle enabled for that port pair, turn a mapped encoder and check that Bitwig responds. Change the parameter in Bitwig and check the E16 ring. Disconnect and reconnect the E16 and repeat both checks.

The 0.0.48-name checkpoint can be reinstalled to repeat the comparison.
