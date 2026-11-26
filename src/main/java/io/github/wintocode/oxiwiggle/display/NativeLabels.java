package io.github.wintocode.oxiwiggle.display;

import java.util.List;

public final class NativeLabels {
    private NativeLabels() {}

    public static byte[] encode(String title, List<String> labels) {
        if (labels == null || labels.size() != 16) throw new IllegalArgumentException("need 16 labels");
        byte[] raw = new byte[80];
        field(title, 15, raw, 0, 16);
        for (int i = 0; i < 16; i++) field(labels.get(i), 4, raw, 16 + 4 * i, 4);
        return raw;
    }

    private static void field(String text, int limit, byte[] raw, int offset, int width) {
        if (text == null || text.length() > limit) throw new IllegalArgumentException("label length");
        for (int i = 0; i < width; i++) raw[offset + i] = 0x20;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 0x20 || c > 0x7e) throw new IllegalArgumentException("printable ASCII required");
            raw[offset + i] = (byte) c;
        }
    }
}
