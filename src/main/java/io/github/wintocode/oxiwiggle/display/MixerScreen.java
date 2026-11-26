package io.github.wintocode.oxiwiggle.display;

import java.util.List;

public final class MixerScreen {
    private MixerScreen() {}

    public static byte[] overview(List<String> names) {
        return overview(names, List.of(), false);
    }

    public static byte[] overview(List<String> names, List<String> sends, boolean shifted) {
        byte[] pixels = new byte[1024];
        PixelText.draw(pixels, shifted ? "PAN / SENDS" : "MIXER", 2, 1, 20);
        for (int x = 2; x < 126; x++) PixelText.set(pixels, x, 10);
        for (int i = 0; i < names.size(); i++) {
            int left = 32 * i;
            String label = PageText.field(names.get(i), Integer.MAX_VALUE, "Track");
            PixelText.wrapNarrow(pixels, label, left + 4, 14, 5, shifted ? 3 : 5);
            if (shifted) PixelText.wrapNarrow(pixels, sends.get(i), left + 4, 42, 5, 2);
            if (i > 0) {
                for (int y = 11; y < 63; y++) PixelText.set(pixels, left, y);
            }
        }
        if (shifted) for (int x = 2; x < 126; x++) PixelText.set(pixels, x, 38);
        return pixels;
    }
}
