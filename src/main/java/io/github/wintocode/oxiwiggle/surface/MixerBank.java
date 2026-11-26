package io.github.wintocode.oxiwiggle.surface;

import java.util.List;

public interface MixerBank {
    int COLUMN_COUNT = 4;

    record Strip(boolean present, String name, String trackId) {}

    static int windowStart(int selectedTrack, int trackCount) {
        return Math.clamp(selectedTrack, 0, Math.max(0, trackCount - COLUMN_COUNT));
    }

    enum TrackSwitch { MUTE, SOLO, ARM }

    record Switches(boolean muted, boolean soloed, boolean armed, boolean armAvailable) {}

    record SendReading(int index, String name, String value, double normalized) {}

    Strip read(int column);
    List<SendReading> sends(int column);
    void incrementSend(int column, SendReading target, double delta);
    RemotePage.Reading level(int column, boolean pan);
    Switches switches(int column);
    void increment(int column, boolean pan, double delta);
    void toggle(int column, TrackSwitch control);
}
