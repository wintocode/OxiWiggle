package io.github.wintocode.oxiwiggle.display;

import java.text.Normalizer;

public final class PageText {
    static final int OLED_TITLE_LENGTH = 20;

    private PageText() {}

    public static String title(String name, int pageIndex) {
        return field(name, OLED_TITLE_LENGTH, "Page " + (pageIndex + 1));
    }

    public static String field(String text, int width, String fallback) {
        String printable = text == null ? "" : Normalizer.normalize(text, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .replaceAll("(?U)\\s+", " ")
                .trim()
                .replaceAll("[^ -~]", "?");
        String value = printable.isEmpty() ? fallback : printable;
        return value.substring(0, Math.min(width, value.length()));
    }
}
