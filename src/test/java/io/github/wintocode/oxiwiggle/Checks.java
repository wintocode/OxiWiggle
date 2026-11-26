package io.github.wintocode.oxiwiggle;

import java.util.Arrays;

public final class Checks {
    private Checks() {}

    public static byte[] hex(String value) {
        String compact = value.replace(" ", "");
        return java.util.HexFormat.of().parseHex(compact);
    }

    public static void bytes(String expected, byte[] actual) {
        if (!Arrays.equals(hex(expected), actual)) {
            throw new AssertionError("expected " + expected + ", got " + java.util.HexFormat.of().formatHex(actual));
        }
    }

    public static void equal(int expected, int actual) {
        if (expected != actual) throw new AssertionError("expected " + expected + ", got " + actual);
    }

    public static void that(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void throwsType(Class<? extends Throwable> expected, Runnable action) {
        try { action.run(); }
        catch (Throwable actual) {
            if (expected.isInstance(actual)) return;
            throw new AssertionError("wrong exception: " + actual, actual);
        }
        throw new AssertionError("expected " + expected.getName());
    }
}
