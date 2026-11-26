package io.github.wintocode.oxiwiggle.surface;

public interface RemotePage {
    int SLOT_COUNT = 8;

    record Reading(String name, boolean available, double normalized, int discreteCount, String displayedValue) {
        public Reading(String name, boolean available, double normalized, int discreteCount) {
            this(name, available, normalized, discreteCount, "");
        }

        public boolean usable() { return available && Double.isFinite(normalized); }
    }

    /** Printable OLED title, limited to 20 characters. */
    String title();
    record PagePosition(int index, int count) {}

    PagePosition pagePosition();
    void navigatePage(int direction);
    String deviceName();
    PagePosition devicePosition();
    void navigateDevice(int direction);
    String trackName();
    String trackId();
    PagePosition trackPosition();
    boolean navigateTrack(int direction);
    void toggleTrackGroup();
    Reading read(int index);
    void increment(int index, double normalizedDelta);
}
