package io.github.wintocode.oxiwiggle.display;

public final class SelectionScreen {
    private static final int WIDTH = 128;
    private static final int HEIGHT = 64;
    private static final int MARGIN = 4;
    private static final int LINE_LENGTH = (WIDTH - 2 * MARGIN) / 6;
    private SelectionScreen() {}

    public static byte[] render(String heading, String name) {
        byte[] pixels = new byte[WIDTH * HEIGHT / 8];
        PixelText.draw(pixels, heading, MARGIN, 6, LINE_LENGTH);
        for (int x = MARGIN; x < WIDTH - MARGIN; x++) PixelText.set(pixels, x, 18);

        String remaining = name;
        for (int row = 0; row < 3 && !remaining.isEmpty(); row++) {
            int y = 23 + row * 13;
            int end = Math.min(remaining.length(), LINE_LENGTH);
            if (end < remaining.length()) {
                if (row == 2) {
                    PixelText.draw(pixels, remaining.substring(0, LINE_LENGTH - 3).stripTrailing() + "...", MARGIN, y, LINE_LENGTH);
                    break;
                }
                int space = remaining.lastIndexOf(' ', end);
                if (space > 0) end = space;
            }
            PixelText.draw(pixels, remaining.substring(0, end), MARGIN, y, LINE_LENGTH);
            remaining = remaining.substring(end).stripLeading();
        }
        return pixels;
    }
}
