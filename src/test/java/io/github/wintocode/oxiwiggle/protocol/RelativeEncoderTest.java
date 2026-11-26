package io.github.wintocode.oxiwiggle.protocol;

import io.github.wintocode.oxiwiggle.Checks;

public final class RelativeEncoderTest {
    public static void run() {
        for (int i = 1; i <= 8; i++) {
            Checks.equal(i - 1, RelativeEncoder.slotIndex(0xb0, i));
            Checks.equal(i, RelativeEncoder.delta(i));
            Checks.equal(-i, RelativeEncoder.delta(128 - i));
        }
        for (int invalid : new int[] {-1, 0, 9, 0x40, 0x77, 128, 255}) {
            Checks.equal(0, RelativeEncoder.delta(invalid));
        }
        for (int status : new int[] {0xb1, 0x90, 0x80, 0x1b0}) {
            Checks.equal(-1, RelativeEncoder.slotIndex(status, 1));
        }
        for (int cc : new int[] {-1, 0, 9, 14, 15, 16, 128}) {
            Checks.equal(-1, RelativeEncoder.slotIndex(0xb0, cc));
        }
        Checks.equal(-1, RelativeEncoder.slotIndex(0x90, 16));
    }
}
