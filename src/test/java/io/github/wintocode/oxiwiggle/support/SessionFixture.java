package io.github.wintocode.oxiwiggle.support;

import io.github.wintocode.oxiwiggle.display.PageText;
import io.github.wintocode.oxiwiggle.surface.RemoteSession;
import io.github.wintocode.oxiwiggle.surface.RemotePage;
import io.github.wintocode.oxiwiggle.surface.TransportControls;
import io.github.wintocode.oxiwiggle.surface.SceneWindow;
import io.github.wintocode.oxiwiggle.surface.MixerBank;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public final class SessionFixture {
    private record Scheduled(long at, long order, Runnable task) {}
    private final List<Scheduled> scheduled = new ArrayList<>();
    private final List<String> sent = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private final Map<Integer, Runnable> hooks = new HashMap<>();
    private long now, order;
    private int flushRequests, failOnSend;
    private boolean failNextSchedule;
    private final RemotePage.Reading[] readings = new RemotePage.Reading[RemotePage.SLOT_COUNT];
    private String title = "Test Page";
    public final List<Double> increments = new ArrayList<>();
    public List<Double> target = increments;
    public final List<List<Double>> otherTargets = new ArrayList<>();
    public boolean failIncrement;
    public int pageCount;
    public int pageIndex;
    public String trackName = "Drums";
    public int trackIndex;
    public int trackCount = 3;
    public boolean groupSelected;
    public boolean groupExpanded;
    public boolean failTrackMove;
    public boolean deferTrackMove;
    public final List<Integer> trackDirections = new ArrayList<>();
    public String deviceName = "First Device";
    public int deviceIndex;
    public int deviceCount = 2;
    public Runnable deviceTurn = () -> {};
    public final List<Integer> deviceDirections = new ArrayList<>();
    public Runnable pageTurn = () -> {};
    public final List<Integer> pageDirections = new ArrayList<>();
    private int mixerOffset;
    public final List<String> mixerNames = new ArrayList<>(List.of("Drums", "Bass", "Bass"));
    public final Map<Integer, String> mixerTrackIds = new HashMap<>();
    public boolean delayAvailable = true;
    public boolean mixerBindingReady = true;
    public double mixerNormalized = 0.5;
    public final Map<Integer, MixerBank.Switches> mixerSwitches = new HashMap<>();
    public final List<String> messages = new ArrayList<>();
    public final List<Double> sendDeltas = new ArrayList<>();
    public final List<String> mixerEdits = new ArrayList<>();
    public final List<String> modeRings = new ArrayList<>();
    public int sceneCount = 11;
    public final List<Integer> launched = new ArrayList<>();
    public final List<String> transportActions = new ArrayList<>();
    public TransportControls.Snapshot transportState = new TransportControls.Snapshot(
            "1.1", "120", "1.1", "1.0", false, false, false, false, false, false, false, false);
    public final RemoteSession session = new RemoteSession(this::send, this::schedule,
            () -> flushRequests++, messages::add, errors::add, () -> now * 1_000_000, new RemotePage() {
                @Override public String title() { return title; }
                @Override public PagePosition pagePosition() { return new PagePosition(pageIndex, pageCount); }
                @Override public PagePosition devicePosition() { return new PagePosition(deviceIndex, deviceCount); }
                @Override public String trackName() { return trackName; }
                @Override public String trackId() { return "track-" + trackIndex; }
                @Override public PagePosition trackPosition() { return new PagePosition(trackIndex, trackCount); }
                @Override public boolean navigateTrack(int direction) {
                    trackDirections.add(direction);
                    int previous = trackIndex;
                    if (deferTrackMove) return true;
                    selectTrack(Math.clamp(trackIndex + direction, 0, trackCount - 1));
                    if (failTrackMove) throw new IllegalStateException("track already moved");
                    session.onSlotChanged();
                    return previous != trackIndex;
                }
                @Override public void toggleTrackGroup() {
                    if (groupSelected) groupExpanded = !groupExpanded;
                }
                @Override public String deviceName() { return deviceName; }
                @Override public void navigateDevice(int direction) {
                    deviceDirections.add(direction);
                    deviceTurn.run();
                    session.onSlotChanged();
                }
                @Override public void navigatePage(int direction) {
                    pageDirections.add(direction);
                    pageTurn.run();
                }
                @Override public Reading read(int index) { return readings[index]; }
                @Override public void increment(int index, double delta) {
                    (index == 0 ? target : otherTargets.get(index - 1)).add(delta);
                    if (failIncrement) throw new IllegalStateException("test increment failure");
                }
            }, new MixerBank() {
                @Override public Strip read(int i) {
                    int index = mixerOffset + i;
                    return index < mixerNames.size()
                            ? new Strip(true, mixerNames.get(index), mixerTrackIds.getOrDefault(index, "track-" + index))
                            : new Strip(false, "", null);
                }

                @Override public List<SendReading> sends(int column) {
                    if (!mixerBindingReady || !read(column).present()) return List.of();
                    var reverb = new SendReading(0, "Reverb", "-inf dB", 0);
                    return delayAvailable ? List.of(reverb, new SendReading(1, "Delay", "-12 dB", 0.5))
                            : List.of(reverb);
                }

                @Override public void incrementSend(int column, SendReading target, double delta) {
                    sendDeltas.add(delta);
                    mixerEdits.add((mixerOffset + column) + " send " + target.index());
                }

                @Override public RemotePage.Reading level(int column, boolean pan) {
                    return mixerBindingReady && read(column).present() ? new RemotePage.Reading(
                            pan ? "Pan" : "Volume", true, mixerNormalized, -1, pan ? "C" : "-6 dB") : null;
                }

                @Override public Switches switches(int column) {
                    return mixerBindingReady && read(column).present()
                            ? mixerSwitches.getOrDefault(mixerOffset + column, new Switches(false, false, false, true))
                            : null;
                }

                @Override public void increment(int column, boolean pan, double delta) {
                    mixerEdits.add((mixerOffset + column) + (pan ? " pan " : " volume ") + delta);
                }

                @Override public void toggle(int column, TrackSwitch control) {
                    mixerEdits.add((mixerOffset + column) + " " + control.name().toLowerCase(java.util.Locale.ROOT));
                }
            }, new TransportControls() {
                @Override public void click(int knob, boolean shifted) {
                    transportActions.add("click " + knob + " " + shifted);
                }

                @Override public void turn(int knob, int delta, boolean shifted) {
                    transportActions.add("turn " + knob + " " + delta + " " + shifted);
                }

                @Override public Snapshot read() { return transportState; }
            }, new SceneWindow() {
                public int count() { return sceneCount; }
                public void scrollTo(int first) {}
                public String name(int index) { return "Scene " + (index + 1); }
                public void launch(int index) { launched.add(index); }
            }, "test-version");

    public void acknowledgeAndDismissWelcome(String ack) {
        session.onSysex(ack);
        session.onMidi(0x90, 15, 127);
        session.onMidi(0x80, 15, 0);
    }

    public void selectTrack(int index) {
        trackIndex = index;
        mixerOffset = MixerBank.windowStart(index, trackCount);
        session.onSelectionChanged();
    }

    public SessionFixture() {
        for (int i = 1; i < RemotePage.SLOT_COUNT; i++) otherTargets.add(new ArrayList<>());
        java.util.Arrays.fill(readings, new RemotePage.Reading("", false, 0, 0));
        readings[0] = new RemotePage.Reading("", true, 0.5, -1);
    }

    public void setTitle(String name) {
        title = PageText.title(name, 0);
        session.onSlotChanged();
    }

    public void readSlot(boolean available, double value) {
        readSlot(available, value, -1);
    }

    public void readSlot(boolean available, double value, int count) {
        readSlot(0, "", available, value, count);
    }

    public void readSlot(int index, String name, boolean available, double value, int count) {
        readSlot(index, name, available, value, count, "");
    }

    public void readSlot(int index, String name, boolean available, double value, int count, String display) {
        readings[index] = new RemotePage.Reading(name, available, value, count, display);
        session.onSlotChanged();
    }

    private void send(byte[] bytes) {
        String frame = HexFormat.of().formatHex(bytes);
        if (frame.startsWith("f000215b02010604000f")) {
            modeRings.add(frame);
            return;
        }
        sent.add(frame);
        int attempt = sent.size();
        Runnable hook = hooks.get(attempt);
        if (hook != null) hook.run();
        if (attempt == failOnSend) throw new IllegalStateException("test send failure");
    }

    private void schedule(Runnable task, long delayMillis) {
        if (failNextSchedule) {
            failNextSchedule = false;
            throw new IllegalStateException("test schedule failure");
        }
        scheduled.add(new Scheduled(now + delayMillis, order++, task));
    }

    public void advanceTo(long absoluteMillis) {
        if (absoluteMillis < now) throw new IllegalArgumentException("time reversal");
        while (true) {
            Scheduled next = scheduled.stream().filter(s -> s.at <= absoluteMillis)
                    .min(Comparator.comparingLong(Scheduled::at).thenComparingLong(Scheduled::order))
                    .orElse(null);
            if (next == null) break;
            scheduled.remove(next);
            now = next.at;
            next.task.run();
        }
        now = absoluteMillis;
    }

    public List<String> sentHex() { return List.copyOf(sent); }
    public List<String> errors() { return List.copyOf(errors); }
    public int flushRequests() { return flushRequests; }
    public void failOnSend(int oneBasedAttempt) { failOnSend = oneBasedAttempt; }
    public void onSend(int oneBasedAttempt, Runnable hook) { hooks.put(oneBasedAttempt, hook); }
    public void failNextSchedule() { failNextSchedule = true; }
}
