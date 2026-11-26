package io.github.wintocode.oxiwiggle.display;

import io.github.wintocode.oxiwiggle.Checks;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import io.github.wintocode.oxiwiggle.protocol.E16Protocol;
import io.github.wintocode.oxiwiggle.protocol.SevenBitPacking;

public final class NativeLabelsTest {
    public static void run() {
        Checks.that(PageText.field("  Réso ", 4, "RC8").equals("Reso"), "accent and whitespace");
        Checks.that(PageText.field("Cutoff", 4, "RC8").equals("Cuto"), "prefix");
        Checks.that(PageText.field("A🎛B", 4, "RC8").equals("A?B"), "one replacement per code point");
        Checks.that(PageText.field("  ", 4, "RC8").equals("RC8"), "blank fallback");
        Checks.that(PageText.field("1234567890123456", 15, "").equals("123456789012345"), "title width");
        Checks.that(PageText.title("\u0301", 2).equals("Page 3"), "combining-mark-only title");
        Checks.that(PageText.title("\u00a0", 2).equals("Page 3"), "nonbreaking-space title");
        Checks.that(PageText.title("  Mix ", 2).equals("Mix"), "named page");
        var labels = new ArrayList<>(Collections.nCopies(16, ""));
        labels.set(0, "TEST");
        byte[] raw = NativeLabels.encode("OxiWiggle M1", labels);
        byte[] expected = ("OxiWiggle M1    TEST" + " ".repeat(60)).getBytes(StandardCharsets.US_ASCII);
        Checks.equal(80, raw.length);
        Checks.that(Arrays.equals(expected, raw), "raw label frame");
        Checks.equal(92, SevenBitPacking.pack(raw).length);
        Checks.equal(100, E16Protocol.labels(raw).length);
        Checks.bytes("f000215b020103004f786957696767006c65204d31202000202054455354200020202020202020002020202020202000202020202020200020202020202020002020202020202000202020202020200020202020202020002020202020202000202020f7",
                E16Protocol.labels(raw));
        Checks.equal(101, E16Protocol.labelsPrefixed(raw).length);
        Checks.bytes("f000215b02010603004f786957696767006c65204d31202000202054455354200020202020202020002020202020202000202020202020200020202020202020002020202020202000202020202020200020202020202020002020202020202000202020f7",
                E16Protocol.labelsPrefixed(raw));
        Checks.throwsType(IllegalArgumentException.class, () -> NativeLabels.encode("1234567890123456", labels));
        Checks.throwsType(IllegalArgumentException.class, () -> NativeLabels.encode("OK", List.of("BAD")));
        Checks.throwsType(IllegalArgumentException.class, () -> NativeLabels.encode("OK", null));
        Checks.throwsType(IllegalArgumentException.class, () -> NativeLabels.encode("é", labels));
        labels.set(0, "TOO-LONG");
        Checks.throwsType(IllegalArgumentException.class, () -> NativeLabels.encode("OK", labels));
    }
}
