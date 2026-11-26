package io.github.wintocode.oxiwiggle.protocol;

import io.github.wintocode.oxiwiggle.Checks;

public final class SevenBitPackingTest {
    public static void run() {
        Checks.bytes("06 03 48 16 64 60 39 01",
                SevenBitPacking.pack(Checks.hex("03 C8 96 64 60 39 01")));
        Checks.bytes("00 00 00 00 00 00 00 00 01 00",
                SevenBitPacking.pack(Checks.hex("00 00 00 00 00 00 00 80")));
        Checks.bytes("06 00 7F 00", SevenBitPacking.pack(Checks.hex("00 FF 80")));
        int[] rawLengths = {0, 1, 6, 7, 8, 80};
        int[] packedLengths = {0, 2, 7, 8, 10, 92};
        for (int n = 0; n < rawLengths.length; n++) {
            Checks.equal(packedLengths[n], SevenBitPacking.pack(new byte[rawLengths[n]]).length);
        }
        for (int bit = 0; bit < 7; bit++) {
            byte[] raw = new byte[7];
            raw[bit] = (byte) 0x80;
            byte[] packed = SevenBitPacking.pack(raw);
            Checks.equal(8, packed.length);
            Checks.equal(1 << bit, packed[0] & 0xff);
            for (int i = 1; i < packed.length; i++) Checks.equal(0, packed[i] & 0xff);
        }
        Checks.throwsType(IllegalArgumentException.class, () -> SevenBitPacking.pack(null));
    }
}
