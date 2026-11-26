package io.github.wintocode.oxiwiggle.display;

final class PixelText {
    private static final int WIDTH = 128;
    // Each glyph has seven rows of five bits, encoded as base-32 digits.
    private static final String[] GLYPHS = {
        "A0EHHVHH", "BUHHUHHU", "CEHGGGHE", "DSIHHHIS", "EVGGUGGV", "FVGGMGGG",
        "GEHGNHHF", "HHHHVHHH", "IE44444E", "J7222IIC", "KHKOSOKH", "LGGGGGGV",
        "MHRLHHHH", "NHPPLJJH", "OEHHHHHE", "PUHHUGGG", "QEHHHLID", "RUHHUOKH",
        "SFHGE1HU", "TV444444", "UHHHHHHE", "VHHHHAA4", "WHHHLLRA", "XHHA4AHH",
        "YHHA4444", "ZV1248GV",
        "a00E1FHF", "bGGUHHHU", "c00FGGGF", "d11FHHHF", "e00EHVGF", "f6474444",
        "g0FHHF1E", "hGGUHHHH", "i404444E", "j20222IC", "kGGIKOKI", "lC44444E",
        "m00QLLLH", "n00UHHHH", "o00EHHHE", "p0UHHUGG", "q0FHHF11", "r00JOGGG",
        "s00FGE1U", "t44F4443", "u00HHHHF", "v00HHHA4", "w00HLLLA", "x00HA4AH",
        "y0HHHF1E", "z00V248V",
        "0EHLPLHE", "14C4444E", "2EH1248V", "3U11E11U", "4248IV22", "5VGGU11U",
        "6EGGUHHE", "7V124888", "8EHHEHHE", "9EHHF11E",
        "?EH12404", "-000V000", "_000000V", ".0000004", ",0000048", ":0040400",
        "/11248GG", "\\GG84211", "(2488842", ")8422248", "[E88888E", "]E22222E",
        "+004V400", "&CHIKJID", "'4400000", "!4444404", "#AAVAVAA", "=00V0V00",
        "\"AA00000", "$4FKE97E", "%HH248HH", "*0A4E4A0", ";0040448", "<1248421",
        ">G84248G", "@EHLNLGE", "^4AH0000", "`8400000", "{2448442", "|4444444",
        "}8442448", "~0009I00"
    };

    // The overview face keeps seven rows and uses four bits per row.
    private static final String[] NARROW_GLYPHS = {
        "A699F999", "BE99E99E", "C6988896", "DE99999E", "EF88E88F", "FF88E888",
        "G698B996", "H999F999", "IE44444E", "J7111196", "K9ACECA9",
        "L888888F", "M9FF9999", "N9DDBB99", "O6999996", "PE99E888", "Q6999B62",
        "RE99ECA9", "S788611E", "TF444444", "U9999996", "V9999966", "W9999FF9",
        "X9966699", "Y9996444", "ZF11248F",
        "a0061797", "b88E999E", "c0078887", "d1179997", "e0069F87", "f344E444",
        "g0079716", "h88E9999", "i4044446", "j202222C", "k88ACECA", "lC444446",
        "m00EFBB9", "n00E9999", "o0069996", "p00E99E8", "q0079971", "r00B4888",
        "s007861E", "t44E4443", "u0099997", "v0099966", "w009BFF6", "x0096699",
        "y0099716", "z00F248F",
        "069BBD96", "14C4444E", "2691248F", "3E11611E", "4124AF22", "5F88E11E",
        "6678E996", "7F112444", "86996996", "96997116",
        "?6912404", "-000F000", "_000000F", ".0000004", ",0000048", ":0040400",
        "/1124888", "\\8842111", "(2444442", ")4222224", "[E88888E", "]7222227",
        "+004E400", "&694AA95", "'4400000", "!4444404", "#055F5F5", "=00F0F00",
        "\"AA00000", "$47C671E", "%9952489", "*00A4E4A", ";0040448", "<1248421",
        ">8421248", "@69BB887", "^6900000", "`8400000", "{3448443", "|4444444",
        "}C22222C", "~0006900"
    };

    private static final String[] ROWS_BY_CHAR = new String[128];
    private static final String[] NARROW_ROWS = new String[128];
    static {
        for (String glyph : GLYPHS) {
            ROWS_BY_CHAR[glyph.charAt(0)] = glyph.substring(1);
        }
        for (String glyph : NARROW_GLYPHS) {
            NARROW_ROWS[glyph.charAt(0)] = glyph.substring(1);
        }
    }

    private PixelText() {}

    static void draw(byte[] pixels, String text, int x, int y, int limit) {
        draw(pixels, text, x, y, limit, 1, false);
    }

    static void drawNarrow(byte[] pixels, String text, int x, int y, int limit) {
        draw(pixels, text, x, y, limit, 1, true);
    }

    private static void draw(byte[] pixels, String text, int x, int y, int limit, int scale, boolean narrow) {
        String[] font = narrow ? NARROW_ROWS : ROWS_BY_CHAR;
        int width = narrow ? 4 : 5;
        for (int i = 0; i < Math.min(limit, text.length()); i++) {
            char c = text.charAt(i);
            if (c == ' ') continue;
            String rows = c < font.length ? font[c] : null;
            if (c == '…') rows = narrow ? "000000D" : "000000L";
            if (rows == null) rows = font['?'];
            for (int row = 0; row < 7; row++) {
                int bits = Character.digit(rows.charAt(row), 32);
                for (int column = 0; column < width; column++) {
                    if ((bits & (1 << (width - 1 - column))) == 0) continue;
                    for (int dy = 0; dy < scale; dy++) {
                        for (int dx = 0; dx < scale; dx++) {
                            set(pixels, x + (i * (width + 1) + column) * scale + dx, y + row * scale + dy);
                        }
                    }
                }
            }
        }
    }

    static void wrap(byte[] pixels, String remaining, int x, int y, int width, int lines, int scale) {
        wrap(pixels, remaining, x, y, width, lines, scale, false);
    }

    static void wrapNarrow(byte[] pixels, String remaining, int x, int y, int width, int lines) {
        wrap(pixels, remaining, x, y, width, lines, 1, true);
    }

    private static void wrap(byte[] pixels, String remaining, int x, int y, int width, int lines,
                             int scale, boolean narrow) {
        for (int line = 0; line < lines && !remaining.isEmpty(); line++) {
            int end = Math.min(remaining.length(), width);
            if (end < remaining.length()) {
                if (line == lines - 1) {
                    draw(pixels, remaining.substring(0, width - 1).stripTrailing() + "…", x, y, width, scale, narrow);
                    return;
                }
                int space = remaining.lastIndexOf(' ', end);
                if (space > 0) end = space;
            }
            draw(pixels, remaining.substring(0, end), x, y, width, scale, narrow);
            remaining = remaining.substring(end).stripLeading();
            y += 8 * scale;
        }
    }

    static void set(byte[] pixels, int x, int y) {
        pixels[x + (y / 8) * WIDTH] |= (byte) (1 << (y % 8));
    }
}
