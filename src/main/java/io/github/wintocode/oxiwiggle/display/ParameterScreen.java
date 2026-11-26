package io.github.wintocode.oxiwiggle.display;

public final class ParameterScreen {
    private ParameterScreen() {}

    public static byte[] render(String name, String value) {
        byte[] pixels = new byte[128 * 64 / 8];
        String label = PageText.field(name, Integer.MAX_VALUE, "");
        String readableValue = value.replace('−', '-')
                .replace("∞", "inf")
                .replace("°", " deg")
                .replace('µ', 'u')
                .replace('μ', 'u')
                .replace('♯', '#')
                .replace('♭', 'b');
        String displayValue = PageText.field(readableValue, Integer.MAX_VALUE, "");
        int space = label.lastIndexOf(' ', Math.min(10, label.length()));
        int firstLineEnd = space > 0 ? space : 10;
        boolean fitsLarge = label.length() <= 10 || label.substring(firstLineEnd).stripLeading().length() <= 10;
        int nameScale = fitsLarge ? 2 : 1;
        PixelText.wrap(pixels, label, 3, 3, 20 / nameScale, nameScale == 2 ? 2 : 4, nameScale);
        for (int x = 3; x < 125; x++) PixelText.set(pixels, x, 36);
        int valueScale = displayValue.length() <= 10 ? 2 : 1;
        PixelText.wrap(pixels, displayValue, 3, 44, 20 / valueScale, valueScale == 2 ? 1 : 2, valueScale);
        return pixels;
    }
}
