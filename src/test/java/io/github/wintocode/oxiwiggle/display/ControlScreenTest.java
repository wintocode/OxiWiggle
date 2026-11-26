package io.github.wintocode.oxiwiggle.display;

import io.github.wintocode.oxiwiggle.Checks;
import java.util.Arrays;
import java.util.List;

public final class ControlScreenTest {
    public static void run() {
        for (String[] value : new String[][] {
                {"−∞ dB", "-inf dB"}, {"90°", "90 deg"}, {"250 µs", "250 us"}, {"C♯", "C#"}
        }) {
            Checks.that(Arrays.equals(ParameterScreen.render("Value", value[0]),
                    ParameterScreen.render("Value", value[1])), "readable value " + value[0]);
        }
        List<String> labels = List.of("AAAAA", "BBBBBB", "Filter Cutoff", "----",
                "RC5", "é", "MMMMMMMMMMMMMMM", "MMMMMMMMMMMMMMMM");
        byte[] expected = new byte[1024];
        PixelText.draw(expected, "Page", 2, 1, 20);
        for (int x = 2; x < 126; x++) PixelText.set(expected, x, 10);
        for (int x = 2; x < 126; x++) PixelText.set(expected, x, 36);
        for (int x : new int[] {32, 64, 96}) {
            for (int y = 11; y < 63; y++) PixelText.set(expected, x, y);
        }
        PixelText.drawNarrow(expected, "AAAAA", 4, 12, 5);
        PixelText.drawNarrow(expected, "BBBBB", 36, 12, 5);
        PixelText.drawNarrow(expected, "B", 36, 20, 5);
        PixelText.drawNarrow(expected, "Filte", 68, 12, 5);
        PixelText.drawNarrow(expected, "r", 68, 20, 5);
        PixelText.drawNarrow(expected, "Cuto…", 68, 28, 5);
        PixelText.drawNarrow(expected, "----", 100, 12, 5);
        PixelText.drawNarrow(expected, "RC5", 4, 39, 5);
        PixelText.drawNarrow(expected, "e", 36, 39, 5);
        for (int y : new int[] {39, 47, 55}) {
            PixelText.drawNarrow(expected, "MMMMM", 68, y, 5);
            PixelText.drawNarrow(expected, y == 55 ? "MMMM…" : "MMMMM", 100, y, 5);
        }
        Checks.that(Arrays.equals(expected, ControlScreen.render("Page", labels)), "eight fixed cells");
        String longTitle = "123456789012345678901234";
        byte[] longTitleScreen = ControlScreen.render(PageText.title(longTitle, 0), labels);
        Checks.that(Arrays.equals(longTitleScreen, ControlScreen.render("12345678901234567890", labels)),
                "a page title fills the 20-character framebuffer heading");

        byte[] wideF = new byte[1024];
        PixelText.draw(wideF, "f", 0, 0, 1);
        byte[] expectedF = new byte[1024];
        String[] rows = {"..##.", "..#..", "..###", "..#..", "..#..", "..#..", "..#.."};
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                if (rows[y].charAt(x) == '#') PixelText.set(expectedF, x, y);
            }
        }
        Checks.that(Arrays.equals(expectedF, wideF), "wide lowercase f has a continuous stem");

        byte[] narrowFallback = new byte[1024];
        PixelText.drawNarrow(narrowFallback, "?", 0, 0, 1);
        byte[] empty = new byte[1024];
        for (char c = '!'; c <= '~'; c++) {
            byte[] glyph = new byte[1024];
            PixelText.drawNarrow(glyph, String.valueOf(c), 0, 0, 1);
            Checks.that(!Arrays.equals(empty, glyph), "visible narrow glyph " + c);
            if (c != '?') Checks.that(!Arrays.equals(narrowFallback, glyph), "distinct narrow glyph " + c);
            for (int i = 4; i < glyph.length; i++) Checks.equal(0, glyph[i]);
        }
        byte[] fallback = SelectionScreen.render("Test", "?");
        for (char c = '!'; c <= '~'; c++) {
            byte[] glyph = SelectionScreen.render("Test", String.valueOf(c));
            Checks.that(!Arrays.equals(glyph, SelectionScreen.render("Test", "")), "visible glyph " + c);
            if (c != '?') Checks.that(!Arrays.equals(fallback, glyph), "distinct from fallback " + c);
        }
        Checks.that(!Arrays.equals(SelectionScreen.render("Test", "Wet 50%"),
                SelectionScreen.render("Test", "Wet 50?")), "percent glyph");
    }
}
