package io.github.wintocode.oxiwiggle.display;

import java.util.List;

public final class ControlScreen {
    private static final int CELL_WIDTH = 32;
    private static final int LINE_LENGTH = 5;
    private static final int LINES_PER_CELL = 3;

    private ControlScreen() {}

    public static byte[] render(String title, List<String> labels) {
        return render(title, labels, true);
    }

    public static byte[] renderScenes(String title, List<String> labels) {
        return render(title, labels, false);
    }

    private static byte[] render(String title, List<String> labels, boolean controls) {
        if (labels.size() != 8) throw new IllegalArgumentException("eight control labels required");
        byte[] pixels = new byte[128 * 64 / 8];
        PixelText.draw(pixels, title, 2, 1, PageText.OLED_TITLE_LENGTH);
        for (int x = 2; x < 126; x++) PixelText.set(pixels, x, 10);

        for (int slot = 0; slot < labels.size(); slot++) {
            int x = 4 + (slot % 4) * CELL_WIDTH;
            int y = 12 + (slot / 4) * 27;
            String remaining = PageText.field(labels.get(slot), Integer.MAX_VALUE, controls ? "RC" + (slot + 1) : "");
            PixelText.wrapNarrow(pixels, remaining, x, y, LINE_LENGTH, LINES_PER_CELL);
        }
        for (int x = 2; x < 126; x++) PixelText.set(pixels, x, 36);
        for (int x = CELL_WIDTH; x < 128; x += CELL_WIDTH) {
            for (int y = 11; y < 63; y++) PixelText.set(pixels, x, y);
        }
        return pixels;
    }
}
