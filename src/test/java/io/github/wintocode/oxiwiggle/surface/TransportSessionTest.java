package io.github.wintocode.oxiwiggle.surface;

import io.github.wintocode.oxiwiggle.Checks;
import io.github.wintocode.oxiwiggle.display.TransportScreen;
import io.github.wintocode.oxiwiggle.protocol.E16Protocol;
import io.github.wintocode.oxiwiggle.support.SessionFixture;
import java.util.HexFormat;
import java.util.List;

public final class TransportSessionTest {
    private static final String ACK = "f000215b020153f7";

    public static void run() {
        gestures();
        liveFeedback();
        navigationAndLifecycle();
    }

    private static SessionFixture ready() {
        var f = new SessionFixture();
        f.session.start();
        f.session.onSysex(ACK);
        f.session.flush();
        return f;
    }

    private static void mode(SessionFixture f, int value) {
        f.session.onMidi(0xb0, 16, value);
        f.session.flush();
    }

    private static void button(SessionFixture f, int knob, boolean down) {
        f.session.onMidi(down ? 0x90 : 0x80, knob - 1, down ? 127 : 0);
    }

    private static void shift(SessionFixture f, boolean down) {
        f.session.onMidi(0x90, 16, down ? 127 : 0);
    }

    private static void gestures() {
        var f = ready();
        mode(f, 1);
        mode(f, 1);
        for (int knob : new int[] {1, 2, 3, 4, 6, 7, 9, 10, 11, 12}) {
            button(f, knob, true);
            button(f, knob, true);
            button(f, knob, false);
            f.session.onMidi(0x90, knob - 1, 0);
            Checks.that(f.transportActions.equals(List.of("click " + knob + " false")),
                    "one ordinary click on knob " + knob);
            f.transportActions.clear();
        }
        for (int knob : new int[] {11, 12}) {
            shift(f, true);
            button(f, knob, true);
            shift(f, false);
            button(f, knob, false);
            button(f, knob, true);
            shift(f, true);
            button(f, knob, false);
            shift(f, false);
            Checks.that(f.transportActions.equals(List.of("click " + knob + " true", "click " + knob + " false")),
                    "modifier follows press on knob " + knob);
            f.transportActions.clear();
        }
        for (int knob = 5; knob <= 10; knob++) {
            f.session.onMidi(0xb0, knob, 3);
            Checks.that(f.transportActions.equals(List.of("turn " + knob + " 3 false")), "turn knob " + knob);
            f.transportActions.clear();
        }
        shift(f, true);
        f.session.onMidi(0xb0, 5, 0x7d);
        shift(f, false);
        Checks.that(f.transportActions.equals(List.of("turn 5 -3 true")), "Shift jog receives negative delta");
        f.transportActions.clear();
        for (int knob : new int[] {5, 8, 13, 14, 16}) {
            button(f, knob, true);
            button(f, knob, false);
        }
        for (int knob : new int[] {1, 2, 3, 4, 11, 12, 13, 14}) f.session.onMidi(0xb0, knob, 1);
        f.session.onMidi(0xb1, 5, 1);
        f.session.onMidi(0x91, 2, 100);
        f.session.onMidi(0x81, 2, 0);
        f.session.onMidi(0xb0, 5, 0);
        Checks.that(f.transportActions.isEmpty(), "unmapped, other-channel and zero-delta inputs are inert");
        Checks.that(f.pageDirections.isEmpty() && f.deviceDirections.isEmpty(), "13 and 14 spare in Transport");
    }

    private static void liveFeedback() {
        var f = ready();
        mode(f, 1);
        mode(f, 1);
        String stopped = screen(f);
        String playOff = ring(f, 1);
        String recordOff = ring(f, 3);
        String loopOff = ring(f, 7);
        String metroOff = ring(f, 10);
        String overdubOff = ring(f, 11);
        String writeOff = ring(f, 12);
        f.transportState = new TransportControls.Snapshot("17.3", "132", "9.1", "4.0",
                true, true, true, true, true, true, false, false);
        f.session.onTransportChanged();
        f.session.flush();
        assertScreen(f, false);
        Checks.that(!stopped.equals(screen(f)), "overview follows observed playback");
        Checks.that(!playOff.equals(ring(f, 1)) && !recordOff.equals(ring(f, 3))
                && !loopOff.equals(ring(f, 7)) && !metroOff.equals(ring(f, 10)), "host switch rings");
        String overdubOn = ring(f, 11);
        String writeOn = ring(f, 12);
        Checks.that(!overdubOff.equals(overdubOn) && !writeOff.equals(writeOn), "recording option rings");
        shift(f, true);
        f.session.flush();
        String punchInOff = ring(f, 11);
        String punchOutOff = ring(f, 12);
        Checks.that(!overdubOn.equals(punchInOff) && !writeOn.equals(punchOutOff), "Shift previews punch");
        f.transportState = new TransportControls.Snapshot("18.2", "128", "9.1", "4.0",
                true, false, true, true, true, true, true, true);
        f.session.onTransportChanged();
        f.session.flush();
        Checks.that(!punchInOff.equals(ring(f, 11)) && !punchOutOff.equals(ring(f, 12)),
                "punch rings follow host edits during Shift");
        shift(f, false);
        f.session.flush();
        Checks.that(overdubOn.equals(ring(f, 11)) && writeOn.equals(ring(f, 12)), "release restores options");
        for (int knob : new int[] {7, 8}) {
            f.session.onMidi(0xb0, knob, 1);
            f.session.flush();
            assertScreen(f, true);
        }
        f.transportState = new TransportControls.Snapshot("18.2", "128", "13.1", "3.0",
                true, false, true, true, true, true, true, true);
        f.session.onTransportChanged();
        f.session.flush();
        assertScreen(f, true);
        f.advanceTo(4300);
        f.session.flush();
        assertScreen(f, false);
    }

    private static void navigationAndLifecycle() {
        var f = ready();
        String deviceRing = f.modeRings.getLast();
        mode(f, 0x78);
        Checks.that(deviceRing.equals(f.modeRings.getLast()), "mode clamps at Device");
        mode(f, 8);
        String mixerRing = f.modeRings.getLast();
        mode(f, 8);
        String transportRing = f.modeRings.getLast();
        assertScreen(f, false);
        Checks.that(transportRing.equals(f.modeRings.getLast()) && !transportRing.equals(mixerRing)
                && !transportRing.equals(deviceRing), "distinct Transport mode ring");
        f.deviceCount = 0;
        f.session.onMidi(0xb0, 15, 1);
        f.session.flush();
        Checks.that(f.trackDirections.equals(List.of(1)), "Transport can navigate an empty device chain");
        f.groupSelected = true;
        button(f, 15, true);
        button(f, 15, true);
        button(f, 15, false);
        Checks.that(f.groupExpanded, "global group button works in Transport");
        mode(f, 0x7f);
        f.session.onMidi(0xb0, 13, 1);
        f.session.onMidi(0xb0, 14, 1);
        Checks.that(f.pageDirections.isEmpty() && f.deviceDirections.isEmpty(), "13 and 14 spare in Mixer");
        button(f, 15, true);
        button(f, 15, false);
        Checks.that(!f.groupExpanded, "global group button works in Mixer");
        f.session.onMidi(0xb0, 2, 1);
        Checks.that(f.mixerEdits.equals(List.of("1 volume " + 1.0 / 16384)), "Mixer follows Transport selection");
        mode(f, 1);
        shift(f, true);
        button(f, 11, true);
        f.session.stop();
        int actions = f.transportActions.size();
        button(f, 11, false);
        f.session.onTransportChanged();
        f.session.flush();
        f.session.start();
        mode(f, 1);
        f.session.onMidi(0xb0, 5, 1);
        Checks.equal(actions, f.transportActions.size());
        f.session.onSysex(ACK);
        f.session.flush();
        Checks.that(deviceRing.equals(f.modeRings.getLast()), "reconnect returns to Device");
        mode(f, 1);
        mode(f, 1);
        button(f, 11, false);
        Checks.equal(actions, f.transportActions.size());
        button(f, 11, true);
        button(f, 11, false);
        Checks.that(f.transportActions.getLast().equals("click 11 false"), "reconnect clears Shift and press");
    }

    private static void assertScreen(SessionFixture f, boolean detail) {
        String expected = HexFormat.of().formatHex(E16Protocol.framebufferPrefixed(
                TransportScreen.render(f.transportState, detail)));
        Checks.that(screen(f).equals(expected), "transport screen detail=" + detail);
    }

    private static String screen(SessionFixture f) {
        return f.sentHex().stream().filter(s -> s.startsWith("f000215b02010602"))
                .reduce((first, last) -> last).orElseThrow();
    }

    private static String ring(SessionFixture f, int knob) {
        return f.sentHex().stream().filter(s -> s.startsWith("f000215b02010604")
                && Integer.parseInt(s.substring(18, 20), 16) == knob - 1)
                .reduce((first, last) -> last).orElseThrow();
    }
}
