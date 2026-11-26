package io.github.wintocode.oxiwiggle.protocol;

import java.io.ByteArrayOutputStream;

public final class SevenBitPacking {
    private SevenBitPacking() {}

    public static byte[] pack(byte[] raw) {
        if (raw == null) throw new IllegalArgumentException("raw payload is null");
        var packed = new ByteArrayOutputStream(raw.length + (raw.length + 6) / 7);
        for (int offset = 0; offset < raw.length; offset += 7) {
            int group = Math.min(7, raw.length - offset);
            int mask = 0;
            for (int i = 0; i < group; i++) mask |= ((raw[offset + i] & 0xff) >>> 7) << i;
            packed.write(mask);
            for (int i = 0; i < group; i++) packed.write(raw[offset + i] & 0x7f);
        }
        return packed.toByteArray();
    }
}
