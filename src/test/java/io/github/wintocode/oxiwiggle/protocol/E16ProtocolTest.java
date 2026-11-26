package io.github.wintocode.oxiwiggle.protocol;

import io.github.wintocode.oxiwiggle.Checks;

public final class E16ProtocolTest {
    public static void run() {
        Checks.bytes("F0 00 21 5B 02 01 06 55 F7", E16Protocol.enterRemoteMode());
        Checks.bytes("F0 00 21 5B 02 01 06 00 F7", E16Protocol.exitRemoteMode());
        Checks.that(E16Protocol.isEnterAck("f000215b020153f7"), "compact ACK");
        Checks.that(E16Protocol.isEnterAck("F000215B020153F7"), "uppercase ACK");
        Checks.that(E16Protocol.isEnterAck("f000215b02010653f7"), "hardware-observed prefixed ACK");
        for (String bad : new String[] {null, "", "f", "xx", "f000215b02010653f700",
                "f000215b0201533805000a7f7f7ff7", "f000215b020153",
                "f000215b02015300f7", "f000215b030153f7", "f0 00 21 5b 02 01 53 f7"}) {
            Checks.that(!E16Protocol.isEnterAck(bad), "invalid ACK: " + bad);
        }
        for (String bad : new String[] {"", "F0", "F0 00 21 5B 02 01 F7",
                "F0 00 21 5B 02 02 04 F7", "F0 00 21 5B 02 01 04 80 F7",
                "F0 00 21 5B 02 01 04 F0 F7", "F0 00 21 5B 02 01 04"}) {
            Checks.throwsType(IllegalArgumentException.class,
                    () -> E16Protocol.validateFrame(Checks.hex(bad)));
        }
        Checks.throwsType(IllegalArgumentException.class, () -> E16Protocol.validateFrame(null));
        E16Protocol.validateFrame(E16Protocol.enterRemoteMode());
        Checks.bytes("F0 00 21 5B 02 01 04 06 03 48 16 64 60 39 01 F7",
                E16Protocol.ring(3, 200, 150, 100, 12345, true));
        Checks.bytes("F0 00 21 5B 02 01 04 00 00 00 08 00 40 00 00 F7",
                E16Protocol.ring(0, 0, 8, 0, 8192, false));
        Checks.bytes("F0 00 21 5B 02 01 06 04 00 00 00 08 00 40 00 00 F7",
                E16Protocol.ringPrefixed(0, 0, 8, 0, 8192, false));
        Checks.throwsType(IllegalArgumentException.class,
                () -> E16Protocol.ring(16, 0, 8, 0, 8192, false));
        Checks.throwsType(IllegalArgumentException.class,
                () -> E16Protocol.ring(0, 256, 8, 0, 8192, false));
        Checks.throwsType(IllegalArgumentException.class,
                () -> E16Protocol.ring(0, 0, 8, 0, 16384, false));
        Checks.throwsType(IllegalArgumentException.class,
                () -> E16Protocol.labels(new byte[79]));
        Checks.throwsType(IllegalArgumentException.class,
                () -> E16Protocol.labels(new byte[81]));
    }
}
