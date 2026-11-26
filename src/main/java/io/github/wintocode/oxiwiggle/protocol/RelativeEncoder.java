package io.github.wintocode.oxiwiggle.protocol;

public final class RelativeEncoder {
    private RelativeEncoder() {}

    public static int slotIndex(int status, int cc) {
        return status == 0xb0 && cc >= 1 && cc <= 8 ? cc - 1 : -1;
    }

    public static int delta(int data) {
        if (data >= 1 && data <= 8) return data;
        if (data >= 0x78 && data <= 0x7f) return data - 128;
        return 0;
    }
}
