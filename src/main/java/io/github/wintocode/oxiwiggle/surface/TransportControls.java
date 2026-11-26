package io.github.wintocode.oxiwiggle.surface;

/** Arranger controls addressed by the printed E16 knob number (1–12). */
public interface TransportControls {
    void click(int knob, boolean shifted);
    void turn(int knob, int delta, boolean shifted);
    Snapshot read();

    record Snapshot(String position, String tempo, String loopStart, String loopLength,
                    boolean playing, boolean recording, boolean loop, boolean metronome,
                    boolean overdub, boolean automationWrite, boolean punchIn, boolean punchOut) {}
}
