package io.github.wintocode.oxiwiggle.surface;

import io.github.wintocode.oxiwiggle.Checks;
import io.github.wintocode.oxiwiggle.support.SessionFixture;
import java.util.List;

public final class LauncherTest {
    public static void run() {
        var f = new SessionFixture();
        f.session.start();
        f.session.onSysex("f000215b020153f7");
        for (int i = 0; i < 3; i++) f.session.onMidi(0xb0, 16, 1);
        f.session.flush();
        press(f, 8);
        f.session.onMidi(0xb0, 13, 8);
        press(f, 1);
        f.session.flush();
        press(f, 3);
        press(f, 4);
        f.session.onMidi(0xb0, 13, 1);
        f.session.flush();
        press(f, 1);
        f.sceneCount = 1;
        f.session.onLauncherChanged();
        press(f, 1);
        f.session.flush();
        press(f, 1);
        Checks.that(f.launched.equals(List.of(7, 10, 8, 0)), "displayed scenes and page bounds: " + f.launched);
    }

    private static void press(SessionFixture f, int knob) {
        f.session.onMidi(0x90, knob - 1, 127);
        f.session.onMidi(0x90, knob - 1, 127);
        f.session.onMidi(0x80, knob - 1, 0);
    }
}
