package io.github.wintocode.oxiwiggle.display;

public final class AboutScreen {
    private AboutScreen() {}

    public static byte[] render(String version) {
        byte[] pixels = new byte[128 * 64 / 8];
        PixelText.wrap(pixels, "OxiWiggle", 11, 8, 10, 1, 2);
        centered(pixels, "by wintoid", 31);
        centered(pixels, version, 48);
        return pixels;
    }

    private static void centered(byte[] pixels, String text, int y) {
        int x = Math.max(0, (128 - (6 * text.length() - 1)) / 2);
        PixelText.draw(pixels, text, x, y, 21);
    }
}
