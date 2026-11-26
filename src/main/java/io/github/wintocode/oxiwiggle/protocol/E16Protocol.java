package io.github.wintocode.oxiwiggle.protocol;

import java.util.Arrays;
import java.io.ByteArrayOutputStream;

public final class E16Protocol {
    private static final byte[] PREFIX = {(byte) 0xF0, 0, 0x21, 0x5B, 0x02, 0x01};
    private static final byte[] ENTER = {(byte) 0xF0, 0, 0x21, 0x5B, 0x02, 0x01, 0x06, 0x55, (byte) 0xF7};
    private static final byte[] EXIT = {(byte) 0xF0, 0, 0x21, 0x5B, 0x02, 0x01, 0x06, 0x00, (byte) 0xF7};
    private static final String ACK = "f000215b020153f7";
    private static final String HARDWARE_ACK = "f000215b02010653f7";

    private E16Protocol() {}

    public static byte[] enterRemoteMode() { return ENTER.clone(); }
    public static byte[] exitRemoteMode() { return EXIT.clone(); }

    public static boolean isEnterAck(String hex) {
        return hex != null && (ACK.equalsIgnoreCase(hex) || HARDWARE_ACK.equalsIgnoreCase(hex));
    }

    public static void validateFrame(byte[] frame) {
        if (frame == null || frame.length < 8 || !Arrays.equals(PREFIX, Arrays.copyOf(frame, 6))
                || frame[frame.length - 1] != (byte) 0xF7) {
            throw new IllegalArgumentException("invalid E16 SysEx frame");
        }
        for (int i = 6; i < frame.length - 1; i++) {
            if ((frame[i] & 0x80) != 0) throw new IllegalArgumentException("non-MIDI-safe data byte");
        }
    }

    public static byte[] ring(int encoder, int red, int green, int blue, int amount, boolean bipolar) {
        checkRange(encoder, 0, 15);
        checkRange(red, 0, 255);
        checkRange(green, 0, 255);
        checkRange(blue, 0, 255);
        checkRange(amount, 0, 16383);
        return frame(0x04, new byte[] {(byte) encoder, (byte) red, (byte) green, (byte) blue,
                (byte) (amount >> 7), (byte) (amount & 0x7f), (byte) (bipolar ? 1 : 0)});
    }

    /** Draw all 16 segments using packed RGB colours (0xRRGGBB). */
    public static byte[] ringSegmentsPrefixed(int encoder, int[] colours) {
        checkRange(encoder, 0, 15);
        if (colours.length != 16) throw new IllegalArgumentException("16 LED colours required");
        byte[] raw = new byte[colours.length * 5];
        for (int segment = 0; segment < colours.length; segment++) {
            int rgb = colours[segment];
            checkRange(rgb, 0, 0xffffff);
            int offset = segment * 5;
            raw[offset] = (byte) encoder;
            raw[offset + 1] = (byte) segment;
            raw[offset + 2] = (byte) (rgb >> 16);
            raw[offset + 3] = (byte) (rgb >> 8);
            raw[offset + 4] = (byte) rgb;
        }
        return prefixCategory(frame(0x01, raw));
    }

    public static byte[] framebufferPrefixed(byte[] pixels) {
        if (pixels == null || pixels.length != 1024) throw new IllegalArgumentException("1024 framebuffer bytes required");
        return prefixCategory(frame(0x02, pixels));
    }

    public static byte[] labels(byte[] rawLabels) {
        if (rawLabels == null || rawLabels.length != 80) throw new IllegalArgumentException("80 label bytes required");
        return frame(0x03, rawLabels);
    }

    public static byte[] labelsPrefixed(byte[] rawLabels) {
        return prefixCategory(labels(rawLabels));
    }

    public static byte[] ringPrefixed(int encoder, int red, int green, int blue, int amount, boolean bipolar) {
        return prefixCategory(ring(encoder, red, green, blue, amount, bipolar));
    }

    private static byte[] prefixCategory(byte[] compact) {
        byte[] prefixed = new byte[compact.length + 1];
        System.arraycopy(compact, 0, prefixed, 0, 6);
        prefixed[6] = 0x06;
        System.arraycopy(compact, 6, prefixed, 7, compact.length - 6);
        return prefixed;
    }

    private static byte[] frame(int command, byte[] raw) {
        var wire = new ByteArrayOutputStream();
        wire.writeBytes(PREFIX);
        wire.write(command);
        wire.writeBytes(SevenBitPacking.pack(raw));
        wire.write(0xf7);
        return wire.toByteArray();
    }

    private static void checkRange(int value, int min, int max) {
        if (value < min || value > max) throw new IllegalArgumentException("field outside " + min + ".." + max);
    }
}
