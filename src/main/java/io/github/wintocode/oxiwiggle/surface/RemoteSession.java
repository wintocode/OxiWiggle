package io.github.wintocode.oxiwiggle.surface;

import io.github.wintocode.oxiwiggle.protocol.E16Protocol;
import io.github.wintocode.oxiwiggle.protocol.RelativeEncoder;
import io.github.wintocode.oxiwiggle.display.AboutScreen;
import io.github.wintocode.oxiwiggle.display.SelectionScreen;
import io.github.wintocode.oxiwiggle.display.ControlScreen;
import io.github.wintocode.oxiwiggle.display.ParameterScreen;
import io.github.wintocode.oxiwiggle.display.MixerScreen;
import io.github.wintocode.oxiwiggle.display.TransportScreen;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class RemoteSession {
    private static final double FINE_STEP = 1.0 / 16_384;
    // Magnitudes 2–8, in units of 1/256 of the normalized parameter range.
    private static final double[] TURN_STEPS = {3.5, 7, 10, 13.5, 16.5, 19.25, 24};
    private static final long DISPLAY_RETRY_MILLIS = 250;
    private static final long NAVIGATION_IDLE_MILLIS = 750;
    private static final long DEVICE_STEP_INTERVAL_MILLIS = 180;
    private static final long SHORT_CHAIN_GESTURE_GAP_MILLIS = 100;
    private static final long NANOS_PER_MILLI = 1_000_000;
    private static final int MIXER_COLUMNS = MixerBank.COLUMN_COUNT;
    private static final int MIXER_TRACK_BUTTONS = 3 * MIXER_COLUMNS;
    private enum Mode { DEVICE, MIXER, TRANSPORT, LAUNCHER, ABOUT }
    private static final int PAGE_RING_INDEX = 12;
    private static final int DEVICE_RING_INDEX = 13;
    private static final int TRACK_RING_INDEX = 14;
    private static final int MODE_RING_INDEX = 15;
    private static final int NAVIGATION_RING_SEGMENTS = 16;
    private static final int NAVIGATION_RING_PURPLE = 0x070316;
    private static final int NAVIGATION_RING_ORANGE = 0x160502;
    private enum View { CONTROLS, PARAMETER, DEVICE, TRACK, LOOP }
    private record Selection(View view, String name, String value) {
        Selection(View view, String name) { this(view, name, ""); }
        byte[] render() {
            return view == View.PARAMETER ? ParameterScreen.render(name, value)
                    : SelectionScreen.render(view.name(), name);
        }
    }
    private record Controls(String title, List<String> names) {}
    private record Ring(boolean assigned, int amount) {}
    @FunctionalInterface public interface Output { void send(byte[] message); }
    @FunctionalInterface public interface Scheduler { void schedule(Runnable callback, long delayMillis); }
    public enum State { STOPPED, WAITING_FOR_ACK, READY, TIMED_OUT, FAILED }

    private final Output output;
    private final Scheduler scheduler;
    private final Runnable requestFlush;
    private final Consumer<String> log;
    private final Consumer<String> error;
    private final LongSupplier nanoTime;
    private final RemotePage page;
    private final MixerBank mixer;
    private final TransportControls transport;
    private final SceneWindow scenes;
    private final byte[] aboutPixels;
    private boolean welcome;
    private int scenePage;
    private final String[] shownScenes = new String[SceneWindow.SIZE];
    private Mode mode = Mode.DEVICE;
    private Mode renderedMode;
    private Mode sentModeRing;
    private record MixerOverview(List<String> names, List<String> sends, boolean shifted) {}
    private record FeedbackRing(int colour, int amount, boolean bipolar) {}
    private MixerOverview sentMixerOverview;
    private final FeedbackRing[] sentFeedbackRings = new FeedbackRing[MIXER_TRACK_BUTTONS];
    private boolean shiftHeld;
    private final int[] selectedSend = new int[MIXER_COLUMNS];
    private final long[] nextSendStep = new long[MIXER_COLUMNS];
    private final int[] lastSendDirection = new int[MIXER_COLUMNS];
    private final MixerBank.SendReading[] shownSends = new MixerBank.SendReading[MIXER_COLUMNS];
    private final boolean[] controlButtonsHeld = new boolean[MIXER_TRACK_BUTTONS];
    private final String[] pendingMixerClicks = new String[MIXER_TRACK_BUTTONS];
    private final String[] shownTrackIds = new String[MIXER_COLUMNS];
    private String pendingTrackOrigin;
    private final boolean[] mixerTargetsReady = new boolean[MIXER_COLUMNS];
    private int nextDeviceRingToClear = MIXER_TRACK_BUTTONS;
    private int nextBottomRing;
    private State state = State.STOPPED;
    private long generation;
    private int attempts;
    private boolean cleanupAttempted = true;
    private boolean displayPending;
    private boolean flushing;
    private boolean retryScheduled;
    private boolean retryAttempt;
    private byte[] sentPixels;
    private Controls sentControls;
    private Selection sentSelection;
    private View view = View.CONTROLS;
    private int detailSlot;
    private boolean trackButtonHeld;
    private boolean navigationTimerScheduled;
    private long navigationDeadline;
    private long lastDeviceStepTime;
    private long lastDeviceTurnTime;
    private int lastDeviceStepDirection;
    private int sentPageProgress = -1;
    private int sentDeviceProgress = -1;
    private int sentTrackProgress = -1;
    private final Ring[] sentRings = new Ring[RemotePage.SLOT_COUNT];

    public RemoteSession(Output output, Scheduler scheduler, Runnable requestFlush,
                         Consumer<String> log, Consumer<String> error, LongSupplier nanoTime, RemotePage page,
                         MixerBank mixer, TransportControls transport, SceneWindow scenes, String version) {
        aboutPixels = AboutScreen.render(version);
        this.output = Objects.requireNonNull(output);
        this.scheduler = Objects.requireNonNull(scheduler);
        this.requestFlush = Objects.requireNonNull(requestFlush);
        this.log = Objects.requireNonNull(log);
        this.error = Objects.requireNonNull(error);
        this.nanoTime = Objects.requireNonNull(nanoTime);
        this.page = Objects.requireNonNull(page);
        this.mixer = Objects.requireNonNull(mixer);
        this.scenes = Objects.requireNonNull(scenes);
        this.transport = Objects.requireNonNull(transport);
    }

    public State state() { return state; }

    public void start() {
        if (state == State.WAITING_FOR_ACK || state == State.READY) {
            throw new IllegalStateException("session already active");
        }
        generation++;
        attempts = 0;
        cleanupAttempted = false;
        resetDisplay();
        state = State.WAITING_FOR_ACK;
        sendAttempt(generation);
    }

    private void sendAttempt(long token) {
        if (token != generation || state != State.WAITING_FOR_ACK) return;
        if (attempts == 3) {
            state = State.TIMED_OUT;
            generation++;
            error.accept("E16 Remote Mode entry timed out; hardware state unknown");
            cleanup();
            return;
        }
        attempts++;
        byte[] entry = E16Protocol.enterRemoteMode();
        try {
            log.accept("E16 entry attempt " + attempts + ": " + java.util.HexFormat.of().formatHex(entry));
            output.send(entry);
            if (token == generation && state == State.WAITING_FOR_ACK) {
                scheduler.schedule(() -> sendAttempt(token), 1000);
            }
        } catch (RuntimeException failure) {
            if (token == generation && state == State.WAITING_FOR_ACK) fail(failure);
        }
    }

    public void onSysex(String hex) {
        if (state != State.WAITING_FOR_ACK || !E16Protocol.isEnterAck(hex)) return;
        state = State.READY;
        long token = generation;
        log.accept("E16 entry ACK: " + hex);
        if (!isReady(token)) return;
        welcome = true;
        try {
            scheduler.schedule(() -> {
                if (isReady(token)) dismissWelcome();
            }, 1500);
        } catch (RuntimeException failure) {
            if (!isReady(token)) return;
            welcome = false;
            error.accept("E16 welcome timer failed: " + failure);
        }
        if (isReady(token)) onSlotChanged();
    }

    private void dismissWelcome() {
        if (!welcome) return;
        welcome = false;
        onSlotChanged();
    }

    public void onSelectionChanged() {
        observeTrackNavigation();
        if (mode == Mode.DEVICE && view == View.PARAMETER) view = View.CONTROLS;
        onSlotChanged();
    }

    public void onMixerTargetChanged() {
        Arrays.fill(pendingMixerClicks, null);
        Arrays.fill(mixerTargetsReady, false);
        Arrays.fill(lastSendDirection, 0);
        Arrays.fill(shownSends, null);
        if (mode == Mode.MIXER) view = View.CONTROLS;
        onSlotChanged();
    }

    public void onTransportChanged() {
        if (mode == Mode.TRANSPORT) onSlotChanged();
    }

    public void onSlotChanged() {
        if (state != State.READY || displayPending) return;
        displayPending = true;
        if (!flushing && !retryScheduled) requestDisplayFlush();
    }

    private static double continuousIncrement(int delta) {
        int magnitude = Math.abs(delta);
        if (magnitude == 1) return Math.copySign(FINE_STEP, delta);
        return Math.copySign(TURN_STEPS[magnitude - 2] / 256.0, delta);
    }

    public void onMidi(int status, int data1, int data2) {
        if (state != State.READY) return;
        boolean turn = status == 0xb0 && data1 >= 1 && data1 <= 16 && RelativeEncoder.delta(data2) != 0;
        boolean press = status == 0x90 && data1 >= 0 && data1 <= 16 && data2 > 0;
        if (turn || press) dismissWelcome();
        boolean note = status == 0x90 || status == 0x80;
        if (note && data1 == 16) {
            boolean shifted = status == 0x90 && data2 > 0;
            if (shiftHeld != shifted) {
                shiftHeld = shifted;
                Arrays.fill(lastSendDirection, 0);
                Arrays.fill(pendingMixerClicks, null);
                if (mode == Mode.MIXER) {
                    view = View.CONTROLS;
                }
                if (mode != Mode.DEVICE) onSlotChanged();
            }
            return;
        }
        if (note && data1 >= 0 && data1 < MIXER_TRACK_BUTTONS) {
            onControlButton(data1, status == 0x90 && data2 > 0);
            return;
        }
        if (data1 == TRACK_RING_INDEX && note) {
            onTrackButton(status, data2);
            return;
        }
        int delta = RelativeEncoder.delta(data2);
        if (delta == 0) return;
        if (status == 0xb0 && data1 == MODE_RING_INDEX + 1) {
            switchMode(Integer.signum(delta));
            return;
        }
        if (status == 0xb0 && data1 == TRACK_RING_INDEX + 1) {
            navigateTrack(Integer.signum(delta));
            return;
        }
        if (mode == Mode.ABOUT) return;
        if (mode == Mode.MIXER) {
            if (status == 0xb0 && data1 >= 1 && data1 <= MIXER_TRACK_BUTTONS) {
                int index = data1 - 1;
                pendingMixerClicks[index] = null;
                if (index < MIXER_COLUMNS) turnMixerLevel(index, delta);
                else if (index < 2 * MIXER_COLUMNS) turnMixerSend(index - MIXER_COLUMNS, delta);
            }
            return;
        }
        if (mode == Mode.LAUNCHER) {
            if (status == 0xb0 && data1 == PAGE_RING_INDEX + 1) {
                view = View.CONTROLS;
                setScenePage(scenePage + Integer.signum(delta));
                onSlotChanged();
            }
            return;
        }
        if (mode == Mode.TRANSPORT) {
            if (status == 0xb0 && data1 >= 5 && data1 <= 10) turnTransport(data1, delta);
            return;
        }
        if (status == 0xb0 && data1 == PAGE_RING_INDEX + 1) {
            view = View.CONTROLS;
            navigatePage(Integer.signum(delta));
            return;
        }
        if (status == 0xb0 && data1 == DEVICE_RING_INDEX + 1) {
            navigateDevice(Integer.signum(delta));
            return;
        }
        int index = RelativeEncoder.slotIndex(status, data1);
        if (index < 0) return;
        long token = generation;
        try {
            RemotePage.Reading reading = page.read(index);
            if (!reading.usable() || !isReady(token)) return;
            int count = reading.discreteCount();
            if (count == -1) {
                page.increment(index, continuousIncrement(delta));
            } else if (count >= 2) {
                page.increment(index, Integer.signum(delta) / (count - 1.0));
            } else {
                return;
            }
            showParameter(token, index);
        } catch (RuntimeException failure) {
            if (!isReady(token)) return;
            error.accept("E16 RC" + (index + 1) + " edit failed: " + failure);
            if (isReady(token)) onSlotChanged();
        }
    }

    private void onControlButton(int index, boolean pressed) {
        if (controlButtonsHeld[index] == pressed) return;
        controlButtonsHeld[index] = pressed;
        if (mode == Mode.TRANSPORT) {
            int knob = index + 1;
            if (pressed && knob != 5 && knob != 8) clickTransport(knob);
            return;
        }
        if (mode == Mode.LAUNCHER) {
            if (pressed && index < SceneWindow.SIZE && shownScenes[index] != null) {
                int target = scenePage * SceneWindow.SIZE + index;
                if (target < scenes.count() && shownScenes[index].equals(scenes.name(target))) scenes.launch(target);
            }
            return;
        }
        int column = index % MIXER_COLUMNS;
        boolean eligible = mode == Mode.MIXER && !shiftHeld && mixerTargetIsCurrent(column);
        if (pressed) {
            pendingMixerClicks[index] = eligible ? shownTrackIds[column] : null;
            return;
        }
        boolean toggle = eligible && shownTrackIds[column].equals(pendingMixerClicks[index]);
        pendingMixerClicks[index] = null;
        if (!toggle) return;
        long token = generation;
        try {
            mixer.toggle(column, MixerBank.TrackSwitch.values()[index / MIXER_COLUMNS]);
        } catch (RuntimeException failure) {
            if (isReady(token)) error.accept("E16 Mixer switch failed: " + failure);
        }
        if (isReady(token)) onSlotChanged();
    }

    private void clickTransport(int knob) {
        long token = generation;
        try {
            transport.click(knob, shiftHeld);
        } catch (RuntimeException failure) {
            if (isReady(token)) error.accept("E16 Transport click failed: " + failure);
        }
        if (isReady(token)) onTransportChanged();
    }

    private void turnTransport(int knob, int delta) {
        long token = generation;
        try {
            transport.turn(knob, delta, shiftHeld);
            if (isReady(token) && mode == Mode.TRANSPORT && (knob == 7 || knob == 8)) {
                view = View.LOOP;
                navigationDeadline = nanoTime.getAsLong() + NAVIGATION_IDLE_MILLIS * NANOS_PER_MILLI;
                refreshNavigationView(token);
            }
        } catch (RuntimeException failure) {
            if (isReady(token)) error.accept("E16 Transport turn failed: " + failure);
        }
        if (isReady(token)) onTransportChanged();
    }

    private boolean mixerTargetIsCurrent(int column) {
        return pendingTrackOrigin == null && mixerTargetsReady[column]
                && shownTrackIds[column].equals(mixer.read(column).trackId())
                && mixerMatchesSelection();
    }

    private boolean mixerMatchesSelection() {
        var position = page.trackPosition();
        if (position.index() < 0 || position.index() >= position.count()) return false;
        int selectedColumn = position.index() - MixerBank.windowStart(position.index(), position.count());
        if (selectedColumn >= MIXER_COLUMNS) return false;
        String selectedId = page.trackId();
        return selectedId != null && !selectedId.isBlank()
                && selectedId.equals(mixer.read(selectedColumn).trackId());
    }

    private void turnMixerLevel(int column, int delta) {
        if (!mixerTargetIsCurrent(column)) return;
        long token = generation;
        try {
            var level = mixer.level(column, shiftHeld);
            if (level == null || !level.usable() || !isReady(token)) return;
            mixer.increment(column, shiftHeld, continuousIncrement(delta));
            if (isReady(token) && mode == Mode.MIXER && mixerTargetsReady[column]) showParameter(token, column);
        } catch (RuntimeException failure) {
            if (isReady(token)) error.accept("E16 Mixer edit failed: " + failure);
        }
    }

    private int selectedSendPosition(int column, List<MixerBank.SendReading> sends) {
        for (int i = 0; i < sends.size(); i++) {
            if (sends.get(i).index() == selectedSend[column]) return i;
        }
        return 0;
    }

    private void turnMixerSend(int column, int delta) {
        if (!mixerTargetIsCurrent(column)) return;
        long token = generation;
        try {
            var sends = mixer.sends(column);
            if (sends.isEmpty() || !isReady(token)) return;
            int position = selectedSendPosition(column, sends);
            if (shiftHeld) {
                int direction = Integer.signum(delta);
                long now = nanoTime.getAsLong();
                if (direction == lastSendDirection[column] && now - nextSendStep[column] < 0) return;
                lastSendDirection[column] = direction;
                nextSendStep[column] = now + SHORT_CHAIN_GESTURE_GAP_MILLIS * NANOS_PER_MILLI;
                selectedSend[column] = sends.get(Math.clamp(position + direction, 0, sends.size() - 1)).index();
            } else {
                var target = sends.get(position);
                var shown = shownSends[column];
                if (shown == null || shown.index() != target.index()
                        || !Objects.equals(shown.name(), target.name()) || !Double.isFinite(target.normalized())) {
                    onSlotChanged();
                    return;
                }
                mixer.incrementSend(column, target, continuousIncrement(delta));
            }
            if (isReady(token) && mode == Mode.MIXER && mixerTargetsReady[column]) {
                showParameter(token, column + MIXER_COLUMNS);
                // A different knob can own detail while the send choice still needs a redraw.
                onSlotChanged();
            }
        } catch (RuntimeException failure) {
            if (isReady(token)) error.accept("E16 Mixer send failed: " + failure);
        }
    }

    private void switchMode(int direction) {
        Mode[] modes = Mode.values();
        Mode next = modes[Math.clamp(mode.ordinal() + direction, 0, modes.length - 1)];
        if (next == mode) return;
        Arrays.fill(shownScenes, null);
        mode = next;
        if (next == Mode.LAUNCHER) scenes.scrollTo(scenePage * SceneWindow.SIZE);
        view = View.CONTROLS;
        onMixerTargetChanged();
    }

    private void showParameter(long token, int index) {
        if (!isReady(token) || (view != View.CONTROLS && view != View.PARAMETER)) return;
        long now = nanoTime.getAsLong();
        if (view == View.PARAMETER && index != detailSlot && now - navigationDeadline < 0) return;
        detailSlot = index;
        view = View.PARAMETER;
        navigationDeadline = now + NAVIGATION_IDLE_MILLIS * NANOS_PER_MILLI;
        refreshNavigationView(token);
    }

    private static String parameterValue(RemotePage.Reading reading) {
        String displayed = reading.displayedValue();
        if (displayed != null && !displayed.isBlank()) return displayed;
        double normalized = Math.clamp(reading.normalized(), 0.0, 1.0);
        int count = reading.discreteCount();
        if (count >= 2) return (Math.round(normalized * (count - 1)) + 1) + "/" + count;
        return Math.round(normalized * 100) + "%";
    }

    private void onTrackButton(int status, int velocity) {
        if (status == 0x80 || velocity == 0) {
            trackButtonHeld = false;
        } else if (velocity == 127 && !trackButtonHeld) {
            trackButtonHeld = true;
            long token = generation;
            try {
                page.toggleTrackGroup();
            } catch (RuntimeException failure) {
                if (isReady(token)) error.accept("E16 group toggle failed: " + failure);
            }
            if (isReady(token)) onSlotChanged();
        }
    }

    private void navigatePage(int direction) {
        long token = generation;
        try {
            page.navigatePage(direction);
        } catch (RuntimeException failure) {
            if (isReady(token)) error.accept("E16 page navigation failed: " + failure);
        }
        if (isReady(token)) onSlotChanged();
    }

    private void navigateDevice(int direction) {
        long token = generation;
        long now = nanoTime.getAsLong();
        boolean startsView = view != View.DEVICE;
        long previousTurn = lastDeviceTurnTime;
        lastDeviceTurnTime = now;
        view = View.DEVICE;
        navigationDeadline = now + NAVIGATION_IDLE_MILLIS * NANOS_PER_MILLI;
        try {
            int count = page.devicePosition().count();
            boolean shortChain = count > 0 && count <= 3;
            long lastEvent = shortChain ? previousTurn : lastDeviceStepTime;
            long interval = shortChain ? SHORT_CHAIN_GESTURE_GAP_MILLIS : DEVICE_STEP_INTERVAL_MILLIS;
            if (isReady(token) && (startsView || direction != lastDeviceStepDirection
                    || now - lastEvent >= interval * NANOS_PER_MILLI)) {
                lastDeviceStepTime = now;
                lastDeviceStepDirection = direction;
                page.navigateDevice(direction);
            }
        } catch (RuntimeException failure) {
            if (isReady(token)) error.accept("E16 device navigation failed: " + failure);
        }
        refreshNavigationView(token);
    }

    private void navigateTrack(int direction) {
        long token = generation;
        if (mode != Mode.MIXER) {
            view = View.TRACK;
            navigationDeadline = nanoTime.getAsLong() + NAVIGATION_IDLE_MILLIS * NANOS_PER_MILLI;
        }
        if (mode == Mode.MIXER) {
            Arrays.fill(pendingMixerClicks, null);
            Arrays.fill(mixerTargetsReady, false);
            view = View.CONTROLS;
        }
        try {
            String origin = Objects.toString(page.trackId(), "");
            if (isReady(token) && page.navigateTrack(direction) && isReady(token)) {
                pendingTrackOrigin = origin;
                observeTrackNavigation();
            }
        } catch (RuntimeException failure) {
            if (isReady(token)) error.accept("E16 track navigation failed: " + failure);
        }
        refreshNavigationView(token);
    }

    private void refreshNavigationView(long token) {
        if (!isReady(token)) return;
        if (view != View.CONTROLS && !navigationTimerScheduled) scheduleNavigationExpiry(token, NAVIGATION_IDLE_MILLIS);
        if (isReady(token)) onSlotChanged();
    }

    private void scheduleNavigationExpiry(long token, long delayMillis) {
        navigationTimerScheduled = true;
        try {
            scheduler.schedule(() -> {
                if (!isReady(token)) return;
                navigationTimerScheduled = false;
                if (view == View.CONTROLS) return;
                long remaining = navigationDeadline - nanoTime.getAsLong();
                if (remaining > 0) {
                    scheduleNavigationExpiry(token, Math.ceilDiv(remaining, NANOS_PER_MILLI));
                } else {
                    view = View.CONTROLS;
                    onSlotChanged();
                }
            }, delayMillis);
        } catch (RuntimeException failure) {
            if (!isReady(token)) return;
            navigationTimerScheduled = false;
            view = View.CONTROLS;
            error.accept("E16 selection screen timer failed: " + failure);
            if (isReady(token)) onSlotChanged();
        }
    }

    private void requestDisplayFlush() {
        long token = generation;
        try { requestFlush.run(); }
        catch (RuntimeException failure) {
            if (isReady(token)) fail(failure);
        }
    }

    private boolean isReady(long token) {
        return state == State.READY && token == generation;
    }

    public void stop() {
        if (state == State.STOPPED) return;
        generation++;
        state = State.STOPPED;
        resetDisplay();
        cleanup();
    }

    public void flush() {
        if (state != State.READY || !displayPending || flushing || retryScheduled) return;
        long token = generation;
        boolean retrying = retryAttempt;
        retryAttempt = false;
        displayPending = false;
        flushing = true;
        try {
            Mode activeMode = mode;
            if (renderedMode != activeMode) {
                invalidateScreen();
                Arrays.fill(sentRings, null);
                sentPageProgress = sentDeviceProgress = sentTrackProgress = -1;
                nextBottomRing = PAGE_RING_INDEX;
                Arrays.fill(sentFeedbackRings, null);
                if (renderedMode != null && activeMode == Mode.DEVICE) nextDeviceRingToClear = 8;
                renderedMode = activeMode;
            }
            if (activeMode == Mode.ABOUT) {
                if (!sendScreen(aboutPixels, token)) return;
                for (int i = 0; i < MIXER_TRACK_BUTTONS; i++) {
                    if (!sendFeedbackRing(i, switchRing(false, false, 0), token)) return;
                }
                sendBottomRow(activeMode, token);
                return;
            }
            if (activeMode == Mode.MIXER) {
                flushMixer(token);
                return;
            }
            if (activeMode == Mode.LAUNCHER) {
                flushLauncher(token);
                return;
            }
            if (activeMode == Mode.TRANSPORT) {
                flushTransport(token);
                return;
            }
            var fields = new ArrayList<String>(RemotePage.SLOT_COUNT);
            Selection selection = switch (view) {
                case CONTROLS, PARAMETER, LOOP -> null;
                case DEVICE -> new Selection(view, page.deviceName());
                case TRACK -> new Selection(view, page.trackName());
            };
            String title = selection == null ? page.title() : null;
            var rings = new Ring[RemotePage.SLOT_COUNT];
            for (int i = 0; i < rings.length; i++) {
                if (!isReady(token)) return;
                var reading = page.read(i);
                boolean assigned = reading.usable();
                int amount = assigned ? (int) Math.round(Math.clamp(reading.normalized(), 0.0, 1.0) * 16383) : 0;
                rings[i] = new Ring(assigned, amount);
                if (view == View.PARAMETER && i == detailSlot) {
                    if (assigned) {
                        String name = reading.name();
                        selection = new Selection(View.PARAMETER,
                                name == null || name.isBlank() ? "RC" + (i + 1) : name, parameterValue(reading));
                    } else {
                        view = View.CONTROLS;
                    }
                }
                if (title != null) {
                    fields.add(assigned ? Objects.toString(reading.name(), "") : "----");
                }
            }
            int pageProgress = purpleSegments(page.pagePosition());
            int deviceProgress = endpointProgress(page.devicePosition());
            int trackProgress = endpointProgress(page.trackPosition());
            if (!isReady(token)) return;
            Controls controls = selection == null ? new Controls(title, fields) : null;
            if (welcome) {
                if (!sendScreen(aboutPixels, token)) return;
            } else if (!Objects.equals(selection, sentSelection) || !Objects.equals(controls, sentControls)) {
                byte[] pixels = selection == null
                        ? ControlScreen.render(controls.title(), controls.names())
                        : selection.render();
                if (!sendScreen(pixels, token)) return;
                sentControls = controls;
                sentSelection = selection;
            }
            for (int i = 0; i < rings.length; i++) {
                var ring = rings[i];
                if (!ring.equals(sentRings[i])) {
                    sentRings[i] = null;
                    output.send(E16Protocol.ringPrefixed(i, 0, ring.assigned() ? 8 : 0, 0, ring.amount(), false));
                    if (!isReady(token)) return;
                    sentRings[i] = ring;
                }
            }
            if (sentTrackProgress != trackProgress) {
                sentTrackProgress = -1;
                sendNavigationRing(TRACK_RING_INDEX, trackProgress);
                if (!isReady(token)) return;
                sentTrackProgress = trackProgress;
            }
            if (sentDeviceProgress != deviceProgress) {
                sentDeviceProgress = -1;
                sendNavigationRing(DEVICE_RING_INDEX, deviceProgress);
                if (!isReady(token)) return;
                sentDeviceProgress = deviceProgress;
            }
            if (sentPageProgress != pageProgress) {
                sentPageProgress = -1;
                sendNavigationRing(PAGE_RING_INDEX, pageProgress);
                if (!isReady(token)) return;
                sentPageProgress = pageProgress;
            }
            while (nextDeviceRingToClear < MIXER_TRACK_BUTTONS) {
                output.send(E16Protocol.ringPrefixed(nextDeviceRingToClear, 0, 0, 0, 0, false));
                if (!isReady(token)) return;
                nextDeviceRingToClear++;
            }
            sendModeRing(activeMode, token);
        } catch (RuntimeException failure) {
            if (isReady(token)) {
                error.accept("E16 display update failed: " + failure);
                if (isReady(token) && !retrying) scheduleDisplayRetry(token);
            }
        } finally {
            flushing = false;
            if (state == State.READY && displayPending && !retryScheduled) requestDisplayFlush();
        }
    }

    private boolean sendScreen(byte[] pixels, long token) {
        if (!Arrays.equals(pixels, sentPixels)) {
            invalidateScreen();
            output.send(E16Protocol.framebufferPrefixed(pixels));
            if (!isReady(token)) return false;
            sentPixels = pixels;
        }
        return true;
    }

    private void observeTrackNavigation() {
        if (pendingTrackOrigin == null) return;
        String selected = page.trackId();
        if (selected != null && !selected.isBlank() && !selected.equals(pendingTrackOrigin)) {
            pendingTrackOrigin = null;
        }
    }

    private void flushMixer(long token) {
        observeTrackNavigation();
        if (pendingTrackOrigin != null) return;
        var trackIds = new String[MIXER_COLUMNS];
        var names = new ArrayList<String>(MixerBank.COLUMN_COUNT);
        for (int i = 0; i < MixerBank.COLUMN_COUNT; i++) {
            var strip = mixer.read(i);
            trackIds[i] = strip.trackId();
            names.add(strip.present() ? Objects.toString(strip.name(), "") : "----");
        }
        var levels = new RemotePage.Reading[MIXER_COLUMNS];
        var switches = new MixerBank.Switches[MIXER_COLUMNS];
        var selectedSends = new MixerBank.SendReading[MIXER_COLUMNS];
        var sendNames = new ArrayList<String>(MIXER_COLUMNS);
        for (int column = 0; column < MIXER_COLUMNS; column++) {
            levels[column] = mixer.level(column, shiftHeld);
            switches[column] = mixer.switches(column);
            var sends = mixer.sends(column);
            boolean sameTrack = Objects.equals(trackIds[column], shownTrackIds[column]);
            var send = sends.isEmpty() ? null
                    : sends.get(sameTrack ? selectedSendPosition(column, sends) : 0);
            selectedSends[column] = send;
            if (shiftHeld) sendNames.add(send == null ? "----" : sendName(send));
        }
        if (!isReady(token)) return;
        var overview = new MixerOverview(names, sendNames, shiftHeld);
        Selection detail = null;
        if (view == View.PARAMETER) {
            int column = detailSlot % MIXER_COLUMNS;
            var send = selectedSends[column];
            var level = detailSlot < MIXER_COLUMNS ? levels[column] : send == null ? null
                    : new RemotePage.Reading(sendName(send), true, send.normalized(), -1, send.value());
            if (level != null && level.usable()) {
                detail = new Selection(view, names.get(column) + " / " + level.name(), parameterValue(level));
            } else {
                view = View.CONTROLS;
            }
        }
        if (detail != null ? !detail.equals(sentSelection) : !overview.equals(sentMixerOverview)) {
            if (!sendScreen(detail == null ? MixerScreen.overview(names, sendNames, shiftHeld) : detail.render(), token)) return;
            sentSelection = detail;
            sentMixerOverview = detail == null ? overview : null;
        }
        for (int column = 0; column < MIXER_COLUMNS; column++) {
            if (!sendMixerColumn(column, levels[column], switches[column], selectedSends[column], token)) return;
        }
        if (!sendBottomRow(Mode.MIXER, token)) return;
        if (isReady(token) && mode == Mode.MIXER && !displayPending) {
            boolean selectionMatches = mixerMatchesSelection();
            if (!selectionMatches) Arrays.fill(pendingMixerClicks, null);
            for (int column = 0; column < MIXER_COLUMNS; column++) {
                var trackId = trackIds[column];
                mixerTargetsReady[column] = selectionMatches && switches[column] != null
                        && trackId != null && !trackId.isBlank();
                if (mixerTargetsReady[column]) {
                    if (!Objects.equals(shownTrackIds[column], trackId)) {
                        selectedSend[column] = 0;
                        lastSendDirection[column] = 0;
                    }
                    shownTrackIds[column] = trackId;
                }
                shownSends[column] = selectedSends[column];
                if (shownSends[column] != null) selectedSend[column] = shownSends[column].index();
            }
        }
    }

    public void onLauncherChanged() {
        if (mode == Mode.LAUNCHER) {
            Arrays.fill(shownScenes, null);
            onSlotChanged();
        }
    }

    private int lastScenePage() {
        return Math.max(0, scenes.count() - 1) / SceneWindow.SIZE;
    }

    private void setScenePage(int requested) {
        int next = Math.clamp(requested, 0, lastScenePage());
        if (scenePage == next) return;
        Arrays.fill(shownScenes, null);
        scenePage = next;
        scenes.scrollTo(next * SceneWindow.SIZE);
    }

    private void flushLauncher(long token) {
        setScenePage(scenePage);
        int first = scenePage * SceneWindow.SIZE;
        String[] names = new String[SceneWindow.SIZE];
        var labels = new ArrayList<String>(SceneWindow.SIZE);
        for (int i = 0; i < names.length; i++) {
            int index = first + i;
            names[i] = index < scenes.count() ? scenes.name(index) : null;
            labels.add(names[i] == null ? "" : names[i].isBlank() ? "Scene " + (index + 1) : names[i]);
        }
        String title = "Launcher " + (scenePage + 1) + "/" + (lastScenePage() + 1);
        boolean overview = view != View.TRACK;
        byte[] pixels = overview ? ControlScreen.renderScenes(title, labels)
                : SelectionScreen.render("TRACK", page.trackName());
        if (!isReady(token) || !sendScreen(pixels, token)) return;
        if (overview && !displayPending) System.arraycopy(names, 0, shownScenes, 0, names.length);
        for (int i = 0; i < MIXER_TRACK_BUTTONS; i++) {
            boolean exists = i < names.length && names[i] != null;
            if (!sendFeedbackRing(i, switchRing(exists, false, 0), token)) return;
        }
        if (!sendBottomRow(Mode.LAUNCHER, token)) return;
        int progress = endpointProgress(new RemotePage.PagePosition(scenePage, lastScenePage() + 1));
        if (sentPageProgress != progress) {
            sendNavigationRing(PAGE_RING_INDEX, progress);
            if (isReady(token)) sentPageProgress = progress;
        }
    }

    private void flushTransport(long token) {
        var snapshot = transport.read();
        byte[] pixels = view == View.TRACK ? new Selection(View.TRACK, page.trackName()).render()
                : TransportScreen.render(snapshot, view == View.LOOP);
        if (!isReady(token) || !sendScreen(pixels, token)) return;
        for (int i = 0; i < MIXER_TRACK_BUTTONS; i++) {
            FeedbackRing ring = switch (i + 1) {
                case 1 -> switchRing(true, snapshot.playing(), 0x000e00);
                case 2 -> switchRing(true, !snapshot.playing(), 0x0c0c0c);
                case 3 -> switchRing(true, snapshot.recording(), 0x140000);
                case 7 -> switchRing(true, snapshot.loop(), 0x000808);
                case 10 -> switchRing(true, snapshot.metronome(), 0x0e0800);
                case 11 -> switchRing(true, shiftHeld ? snapshot.punchIn() : snapshot.overdub(), 0x140000);
                case 12 -> switchRing(true, shiftHeld ? snapshot.punchOut() : snapshot.automationWrite(), 0x140000);
                default -> switchRing(true, false, 0);
            };
            if (!sendFeedbackRing(i, ring, token)) return;
        }
        sendBottomRow(Mode.TRANSPORT, token);
    }

    private boolean sendBottomRow(Mode activeMode, long token) {
        while (nextBottomRing <= DEVICE_RING_INDEX) {
            output.send(E16Protocol.ringPrefixed(nextBottomRing, 0, 0, 0, 0, false));
            if (!isReady(token)) return false;
            nextBottomRing++;
        }
        int trackProgress = endpointProgress(page.trackPosition());
        if (!isReady(token)) return false;
        if (sentTrackProgress != trackProgress) {
            sentTrackProgress = -1;
            sendNavigationRing(TRACK_RING_INDEX, trackProgress);
            if (!isReady(token)) return false;
            sentTrackProgress = trackProgress;
        }
        sendModeRing(activeMode, token);
        return isReady(token);
    }

    private static String sendName(MixerBank.SendReading send) {
        return send.name() == null || send.name().isBlank() ? "Send " + (send.index() + 1) : send.name();
    }

    private boolean sendMixerColumn(int column, RemotePage.Reading level, MixerBank.Switches switches,
                                    MixerBank.SendReading send, long token) {
        boolean available = level != null && level.usable();
        boolean muted = switches != null && switches.muted();
        int amount = available ? (int) Math.round(Math.clamp(level.normalized(), 0, 1) * 16383) : 0;
        int volumeColour = muted ? 0x140000 : available ? 0x000800 : 0;
        var volume = new FeedbackRing(volumeColour, muted && !shiftHeld ? Math.max(1024, amount) : amount,
                available && shiftHeld);
        var solo = switchRing(switches != null, switches != null && switches.soloed(), 0x0e0800);
        if (send != null && Double.isFinite(send.normalized())) {
            int sendAmount = (int) Math.round(Math.clamp(send.normalized(), 0, 1) * 16383);
            solo = new FeedbackRing(solo.colour() == 0x0e0800 ? 0x0e0800 : 0x02000c,
                    Math.max(1024, sendAmount), false);
        }
        var arm = switchRing(switches != null && switches.armAvailable(), switches != null && switches.armed(), 0x140000);
        return sendFeedbackRing(column, volume, token)
                && sendFeedbackRing(column + MIXER_COLUMNS, solo, token)
                && sendFeedbackRing(column + 2 * MIXER_COLUMNS, arm, token);
    }

    private static FeedbackRing switchRing(boolean available, boolean active, int colour) {
        if (active) return new FeedbackRing(colour, 16383, false);
        return available ? new FeedbackRing(0x020202, 1024, false) : new FeedbackRing(0, 0, false);
    }

    private boolean sendFeedbackRing(int index, FeedbackRing ring, long token) {
        if (ring.equals(sentFeedbackRings[index])) return true;
        sentFeedbackRings[index] = null;
        int colour = ring.colour();
        output.send(E16Protocol.ringPrefixed(index, colour >> 16, (colour >> 8) & 0xff,
                colour & 0xff, ring.amount(), ring.bipolar()));
        if (!isReady(token)) return false;
        sentFeedbackRings[index] = ring;
        return true;
    }

    private void sendModeRing(Mode activeMode, long token) {
        if (sentModeRing == activeMode) return;
        sentModeRing = null;
        int colour = switch (activeMode) {
            case DEVICE -> NAVIGATION_RING_PURPLE;
            case MIXER -> NAVIGATION_RING_ORANGE;
            case TRANSPORT -> 0x000e08;
            case LAUNCHER -> 0x020918;
            case ABOUT -> 0x080808;
        };
        output.send(E16Protocol.ringPrefixed(MODE_RING_INDEX, colour >> 16, (colour >> 8) & 0xff,
                colour & 0xff, 16383, false));
        if (isReady(token)) sentModeRing = activeMode;
    }

    private void sendNavigationRing(int encoder, int purpleSegments) {
        int[] colours = new int[NAVIGATION_RING_SEGMENTS];
        Arrays.fill(colours, 0, purpleSegments, NAVIGATION_RING_PURPLE);
        Arrays.fill(colours, purpleSegments, colours.length, NAVIGATION_RING_ORANGE);
        output.send(E16Protocol.ringSegmentsPrefixed(encoder, colours));
    }

    private static int purpleSegments(RemotePage.PagePosition position) {
        int index = position.index();
        int count = position.count();
        if (index < 0 || index >= count) return 0;
        if (index == count - 1) return NAVIGATION_RING_SEGMENTS;
        long scaled = Math.ceilDiv((index + 1L) * NAVIGATION_RING_SEGMENTS, count);
        return (int) Math.min(NAVIGATION_RING_SEGMENTS - 1, scaled);
    }

    private static int endpointProgress(RemotePage.PagePosition position) {
        int index = position.index();
        int count = position.count();
        if (index < 0 || index >= count) return 0;
        if (index == count - 1) return NAVIGATION_RING_SEGMENTS;
        if (index == 0) return 0;
        int scaled = (int) Math.round(index * (double) NAVIGATION_RING_SEGMENTS / (count - 1));
        return Math.clamp(scaled, 1, NAVIGATION_RING_SEGMENTS - 1);
    }

    private void scheduleDisplayRetry(long token) {
        retryScheduled = true;
        try {
            scheduler.schedule(() -> {
                if (!isReady(token)) return;
                retryScheduled = false;
                retryAttempt = true;
                displayPending = true;
                if (!flushing) requestDisplayFlush();
            }, DISPLAY_RETRY_MILLIS);
        } catch (RuntimeException failure) {
            if (isReady(token)) fail(failure);
        }
    }

    private void invalidateScreen() {
        // A send that throws may already have changed the OLED or its mode.
        sentPixels = null;
        sentMixerOverview = null;
        sentControls = null;
        sentSelection = null;
    }

    private void resetDisplay() {
        displayPending = false;
        retryScheduled = false;
        retryAttempt = false;
        invalidateScreen();
        Arrays.fill(sentRings, null);
        sentPageProgress = -1;
        sentDeviceProgress = -1;
        view = View.CONTROLS;
        sentTrackProgress = -1;
        trackButtonHeld = false;
        navigationTimerScheduled = false;
        scenePage = 0;
        Arrays.fill(shownScenes, null);
        welcome = false;
        mode = Mode.DEVICE;
        shiftHeld = false;
        pendingTrackOrigin = null;
        Arrays.fill(shownTrackIds, null);
        Arrays.fill(selectedSend, 0);
        Arrays.fill(lastSendDirection, 0);
        Arrays.fill(shownSends, null);
        Arrays.fill(controlButtonsHeld, false);
        Arrays.fill(pendingMixerClicks, null);
        Arrays.fill(mixerTargetsReady, false);
        nextDeviceRingToClear = MIXER_TRACK_BUTTONS;
        renderedMode = sentModeRing = null;
    }

    private void fail(RuntimeException failure) {
        generation++;
        state = State.FAILED;
        resetDisplay();
        error.accept("E16 session failed; hardware state unknown: " + failure);
        cleanup();
    }

    private void cleanup() {
        if (cleanupAttempted) return;
        cleanupAttempted = true;
        byte[] exit = E16Protocol.exitRemoteMode();
        try {
            log.accept("E16 exit: " + java.util.HexFormat.of().formatHex(exit));
            output.send(exit);
        } catch (RuntimeException failure) {
            error.accept("E16 exit could not be delivered; hardware state unknown: " + failure);
        }
    }
}
