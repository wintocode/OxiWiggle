package io.github.wintocode.oxiwiggle.surface;

import io.github.wintocode.oxiwiggle.Checks;
import io.github.wintocode.oxiwiggle.support.SessionFixture;
import io.github.wintocode.oxiwiggle.display.AboutScreen;
import io.github.wintocode.oxiwiggle.display.ControlScreen;
import io.github.wintocode.oxiwiggle.display.MixerScreen;
import io.github.wintocode.oxiwiggle.surface.MixerBank.Switches;
import io.github.wintocode.oxiwiggle.display.ParameterScreen;
import io.github.wintocode.oxiwiggle.display.SelectionScreen;
import io.github.wintocode.oxiwiggle.protocol.E16Protocol;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

public final class RemoteSessionTest {
    private static final String ENTRY = "f000215b02010655f7";
    private static final String EXIT = "f000215b02010600f7";
    private static final String ACK = "f000215b020153f7";

    public static void run() {
        mediumTurnResponse();
        welcomeAndAbout();
        mixerModeSelector();
        firstMixerStripGestures();
        mixerSendsAreIndependent();
        mixerWaitsForTrackNavigation();
        mixerWaitsForBankAfterCursorMoves();
        mixerRecoversAfterReturningToTrack();
        mixerWaitsForNavigationStartedInDeviceMode();
        mixerRejectsReplacementTrack();
        mixerButtonsAndTurnsReachEveryColumn();
        trackNavigationAndGroupPress();
        deviceNavigationShowsLiveNameAndReturnsToControls();
        deviceNavigationSpacesRepeatedTurns();
        deviceRingUsesFullPositionRange();
        shortDeviceChainWaitsForANewGesture();
        deviceViewExpirySurvivesFailedSendAndRestart();
        pageNavigationRingShowsProgress();
        timeoutIsBounded();
        ackStopsRetry();
        hardwareObservedAckStopsRetry();
        invalidAndDuplicateAckIgnored();
        stopAndRestartInvalidateTimers();
        synchronousAckIsAccepted();
        failuresCleanUp();
        displayWaitsForAckAndFlush();
        displayFailureRetriesPendingRings();
        pageNavigationShowsLiveControls();
        sameTitlePageRedrawsControls();
        knobDetailFollowsPhysicalTurns();
        hostPageChangeClearsDetail();
        pageNavigationSurvivesSendFailureAndRestart();
        failedSendResendsRestoredState();
        equivalentRenderedControlsStayQuiet();
        stopDuringLabelsPreventsRing();
        stopAfterAckBeforeFlushPreventsDisplay();
        oldSendFailureDoesNotFailRestartedSession();
        restartDuringLabelsDefersNewDisplay();
        feedbackUsesLatestBitwigValue();
        eightSlotsAndRenderedLabelsUpdateIndependently();
        restartDuringMiddleRingDiscardsOldFrames();
        emptySlotDiffersFromMinimum();
        refreshDuringSendSurvives();
        turnWritesOnlyToLiveSlot();
        selectorMovesOneItem();
        eightTurnsReachIndependentLiveTargets();
        failedIncrementIsNeverReplayed();
    }

    private static void mediumTurnResponse() {
        var f = new SessionFixture();
        f.session.start();
        f.session.onSysex(ACK);
        double[] steps = {1.0 / 16384, 3.5 / 256, 7.0 / 256, 10.0 / 256,
                13.5 / 256, 16.5 / 256, 19.25 / 256, 24.0 / 256};
        for (int i = 0; i < steps.length; i++) {
            int magnitude = i + 1;
            f.session.onMidi(0xb0, 1, magnitude);
            f.session.onMidi(0xb0, 1, 128 - magnitude);
            Checks.that(f.increments.get(2 * i) == steps[i]
                    && f.increments.get(2 * i + 1) == -steps[i], "turn response at " + magnitude);
        }
    }

    private static void welcomeAndAbout() {
        var f = new SessionFixture();
        f.session.start();
        f.session.onSysex(ACK);
        f.session.flush();
        String welcome = assertSelectionScreen(f);
        Checks.that(welcome.equals(screenHex(AboutScreen.render("test-version"))), "shows supplied version");
        f.advanceTo(2000);
        f.session.flush();
        Checks.that(!welcome.equals(assertSelectionScreen(f)), "welcome expires to Device");
        f.session.stop();
        f.session.start();
        f.session.onSysex(ACK);
        f.session.flush();
        Checks.that(welcome.equals(assertSelectionScreen(f)), "reconnect shows welcome");
        f.session.onMidi(0xb0, 1, 1);
        f.session.flush();
        Checks.equal(1, f.increments.size());
        Checks.that(!welcome.equals(assertSelectionScreen(f)), "first turn dismisses welcome");
        for (int i = 0; i < 3; i++) f.session.onMidi(0xb0, 16, 1);
        f.session.flush();
        String launcher = assertSelectionScreen(f);
        f.session.onMidi(0xb0, 16, 1);
        f.session.onMidi(0xb0, 16, 1);
        f.session.flush();
        Checks.that(launcher.equals(assertSelectionScreen(f)), "Launcher is the clockwise endpoint");
        f.session.onMidi(0x90, 15, 127);
        f.session.onMidi(0x90, 15, 127);
        f.session.onMidi(0x80, 15, 0);
        f.session.onMidi(0xb0, 16, 127);
        f.advanceTo(5000);
        f.session.flush();
        Checks.that(welcome.equals(assertSelectionScreen(f)), "About stays open through turns");
        Checks.equal(1, f.increments.size());
        f.session.onMidi(0x90, 0, 127);
        f.session.onMidi(0x90, 0, 127);
        f.session.onMidi(0x80, 0, 0);
        Checks.that(f.launched.isEmpty(), "closing About consumes the scene click");
        f.session.flush();
        Checks.that(launcher.equals(assertSelectionScreen(f)), "dismissal returns to Launcher");

        var restarted = new SessionFixture();
        restarted.session.start();
        restarted.session.onSysex(ACK);
        restarted.advanceTo(1000);
        restarted.session.stop();
        restarted.session.start();
        restarted.session.onSysex(ACK);
        restarted.advanceTo(1600);
        restarted.session.flush();
        Checks.that(welcome.equals(assertSelectionScreen(restarted)), "old expiry preserves new welcome");
        restarted.groupSelected = true;
        restarted.session.onMidi(0x90, 14, 127);
        restarted.session.onMidi(0x80, 14, 0);
        restarted.session.flush();
        Checks.that(restarted.groupExpanded, "first press toggles the selected group");
        Checks.that(!welcome.equals(assertSelectionScreen(restarted)), "first press dismisses welcome");
    }

    private static void mixerModeSelector() {
        var f = new SessionFixture();
        f.trackCount = 5;
        f.mixerNames.addAll(List.of("Pads", "Vox"));
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        String device = assertSelectionScreen(f);
        String deviceRing = f.modeRings.getLast();

        f.session.onMidi(0xb0, 16, 1);
        f.session.flush();
        String mixer = screenHex(MixerScreen.overview(List.of("Drums", "Bass", "Bass", "Pads")));
        Checks.that(assertSelectionScreen(f).equals(mixer), "turn enters Mixer without a click");
        Checks.that(!f.modeRings.getLast().equals(deviceRing), "mode indicator follows the active mode");
        f.session.onMidi(0x90, 15, 127);
        f.session.flush();
        Checks.that(assertSelectionScreen(f).equals(screenHex(AboutScreen.render("test-version"))), "click opens About in Mixer");
        f.session.onMidi(0x80, 15, 0);
        f.session.onMidi(0x90, 15, 127);
        f.session.onMidi(0x80, 15, 0);
        f.session.flush();
        Checks.that(assertSelectionScreen(f).equals(mixer), "second mode click restores Mixer");

        f.session.onMidi(0xb0, 1, 1);
        f.session.onMidi(0xb0, 15, 1);
        f.session.flush();
        Checks.equal(0, f.increments.size());
        Checks.equal(1, f.trackDirections.size());
        String lastWindow = screenHex(MixerScreen.overview(List.of("Bass", "Bass", "Pads", "Vox")));
        Checks.that(assertSelectionScreen(f).equals(lastWindow), "track two starts the last full window");
        f.selectTrack(4);
        f.session.flush();
        Checks.that(assertSelectionScreen(f).equals(lastWindow), "track five keeps the last full window");
        f.session.onMidi(0xb0, 15, 0x7f);
        f.session.flush();
        Checks.that(assertSelectionScreen(f).equals(lastWindow), "reversing within the last window keeps it still");
        f.selectTrack(0);
        f.session.flush();
        Checks.that(assertSelectionScreen(f).equals(mixer), "host selection back to track one restores first window");

        f.session.onMidi(0xb0, 16, 0x7f);
        f.session.flush();
        Checks.that(assertSelectionScreen(f).equals(device), "reverse enters Device immediately");
        f.session.onMidi(0xb0, 15, 0x7f);
        Checks.equal(3, f.trackDirections.size());
        f.session.onMidi(0xb0, 16, 1);
        f.session.stop();
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.advanceTo(4300);
        f.session.flush();
        Checks.that(assertSelectionScreen(f).equals(device), "reconnect starts in Device");
    }

    private static void firstMixerStripGestures() {
        var f = new SessionFixture();
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.onMidi(0xb0, 16, 1);
        f.session.flush();
        f.session.onMidi(0xb0, 1, 4);
        f.session.flush();
        assertParameter(assertSelectionScreen(f), "Drums / Volume", "-6 dB");
        f.session.onMidi(0x90, 16, 127);
        f.session.flush();
        Checks.that(f.sentHex().contains(HexFormat.of().formatHex(
                E16Protocol.ringPrefixed(0, 0, 8, 0, 8192, true))), "Shift previews bipolar pan");
        f.session.onMidi(0xb0, 1, 0x7e);
        f.session.flush();
        assertParameter(assertSelectionScreen(f), "Drums / Pan", "C");
        f.session.onMidi(0x90, 16, 0);
        Checks.that(f.mixerEdits.equals(List.of("0 volume " + 10.0 / 256, "0 pan " + -3.5 / 256)),
                "turn writes volume and Shift-turn writes pan");
        f.session.onMidi(0x80, 0, 0);
        f.session.onMidi(0x90, 0, 127);
        f.session.onMidi(0x90, 0, 127);
        f.session.onMidi(0x80, 0, 0);
        f.session.onMidi(0x80, 0, 0);
        Checks.that(f.mixerEdits.getLast().equals("0 mute"), "one mute on release");
        f.session.onMidi(0x90, 0, 127);
        f.session.onMidi(0xb0, 1, 1);
        f.session.onMidi(0x80, 0, 0);
        Checks.equal(4, f.mixerEdits.size());
        f.session.onMidi(0x90, 0, 127);
        f.session.onMidi(0xb0, 15, 1);
        f.session.onMidi(0x80, 0, 0);
        Checks.equal(4, f.mixerEdits.size());
        f.session.onMidi(0xb0, 1, 1);
        Checks.equal(4, f.mixerEdits.size());
        f.session.flush();
        f.session.onMidi(0x90, 16, 127);
        f.session.stop();
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.onMidi(0xb0, 16, 1);
        f.session.flush();
        f.session.onMidi(0xb0, 1, 1);
        Checks.that(f.mixerEdits.getLast().contains(" volume "), "reconnect clears held Shift");
    }

    private static void mixerButtonsAndTurnsReachEveryColumn() {
        var f = new SessionFixture();
        f.trackCount = 5;
        f.mixerNames.addAll(List.of("Pads", "Vox"));
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.onMidi(0xb0, 16, 1);
        f.session.flush();
        for (int column = 0; column < 4; column++) {
            f.session.onMidi(0xb0, column + 1, 1);
        }
        f.session.onMidi(0x90, 16, 127);
        for (int column = 0; column < 4; column++) {
            f.session.onMidi(0xb0, column + 1, 1);
        }
        f.session.onMidi(0x80, 16, 0);
        double step = 1.0 / 16384;
        for (int column = 0; column < 4; column++) {
            Checks.that(f.mixerEdits.get(column).equals(column + " volume " + step), "volume column " + column);
            Checks.that(f.mixerEdits.get(column + 4).equals(column + " pan " + step), "pan column " + column);
        }
        for (int button = 0; button < 12; button++) {
            f.session.onMidi(0x90, button, 127);
            f.session.onMidi(0x80, button, 0);
        }
        Checks.that(f.mixerEdits.subList(8, 20).equals(List.of(
                "0 mute", "1 mute", "2 mute", "3 mute", "0 solo", "1 solo", "2 solo", "3 solo",
                "0 arm", "1 arm", "2 arm", "3 arm")), "twelve switches route by row and column");

        f.session.onMidi(0x90, 9, 127);
        f.mixerBindingReady = false;
        f.session.onMidi(0xb0, 15, 1);
        f.session.flush();
        f.session.onMidi(0x80, 9, 0);
        f.session.onMidi(0xb0, 2, 1);
        Checks.equal(20, f.mixerEdits.size());
        f.mixerBindingReady = true;
        f.session.onMixerTargetChanged();
        f.session.flush();
        f.session.onMidi(0x90, 9, 127);
        f.session.onMidi(0x80, 9, 0);
        Checks.that(f.mixerEdits.getLast().equals("2 arm"), "fresh arm click targets the new column");
        f.mixerNormalized = 0;
        f.mixerSwitches.put(2, new Switches(true, true, true, true));
        f.session.onSlotChanged();
        f.session.flush();
        Checks.that(f.sentHex().contains(HexFormat.of().formatHex(
                E16Protocol.ringPrefixed(1, 20, 0, 0, 1024, false))), "mute visible at zero volume");
        Checks.that(f.sentHex().contains(HexFormat.of().formatHex(
                E16Protocol.ringPrefixed(5, 14, 8, 0, 1024, false))), "observed solo ring");
        Checks.that(f.sentHex().contains(HexFormat.of().formatHex(
                E16Protocol.ringPrefixed(9, 20, 0, 0, 16383, false))), "observed arm ring");
        f.session.onMidi(0xb0, 16, 0x7f);
        f.session.flush();
        Checks.that(f.sentHex().contains(HexFormat.of().formatHex(
                E16Protocol.ringPrefixed(9, 0, 0, 0, 0, false))), "arm ring clears in Device");
    }

    private static void mixerSendsAreIndependent() {
        var f = new SessionFixture();
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.onMidi(0xb0, 16, 1);
        f.session.flush();
        f.session.onMidi(0x90, 16, 127);
        f.session.onMidi(0xb0, 5, 1);
        f.session.flush();
        assertParameter(assertSelectionScreen(f), "Drums / Delay", "-12 dB");
        Checks.equal(0, f.mixerEdits.size());
        f.session.onMidi(0x80, 16, 0);
        f.session.onSelectionChanged();
        f.mixerNames.set(1, "Bass renamed");
        f.session.onSlotChanged();
        f.session.flush();
        f.session.onMidi(0xb0, 5, 1);
        f.session.onMidi(0xb0, 6, 0x7f);
        Checks.that(f.mixerEdits.equals(List.of("0 send 1", "1 send 0")), "columns edit their own send");
        Checks.that(f.sendDeltas.equals(List.of(1.0 / 16384, -1.0 / 16384)),
                "send amount uses the fine step in both directions");
        f.session.onMidi(0xb0, 8, 1);
        Checks.equal(2, f.mixerEdits.size());
        f.delayAvailable = false;
        f.session.onSlotChanged();
        f.session.onMidi(0xb0, 5, 1);
        Checks.equal(2, f.mixerEdits.size());
        f.session.flush();
        f.session.onMidi(0xb0, 5, 1);
        Checks.that(f.mixerEdits.getLast().equals("0 send 0"), "removed destination falls back after repaint");
        f.session.onMidi(0xb0, 15, 1);
        f.session.onMidi(0xb0, 5, 1);
        Checks.equal(3, f.mixerEdits.size());
    }

    private static void mixerWaitsForTrackNavigation() {
        var f = new SessionFixture();
        f.trackCount = 5;
        f.mixerNames.addAll(List.of("Pads", "Vox"));
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.onMidi(0xb0, 16, 1);
        f.session.flush();
        f.deferTrackMove = true;
        f.session.onMidi(0xb0, 15, 1);
        f.session.flush();
        f.session.onMidi(0xb0, 1, 1);
        Checks.equal(0, f.mixerEdits.size());
        f.selectTrack(1);
        f.session.flush();
        f.session.onMidi(0xb0, 1, 1);
        Checks.that(f.mixerEdits.equals(List.of("1 volume " + 1.0 / 16384)),
                "level writes resume on the newly displayed track");
    }

    private static void mixerWaitsForBankAfterCursorMoves() {
        var f = new SessionFixture();
        f.trackCount = 5;
        f.mixerNames.addAll(List.of("Pads", "Vox"));
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.onMidi(0xb0, 16, 1);
        f.session.flush();
        f.deferTrackMove = true;
        f.session.onMidi(0xb0, 15, 1);

        f.trackIndex = 1; // Deliver the cursor change with the bank still on its previous window.
        f.session.onSelectionChanged();
        f.session.flush();
        f.session.onMidi(0xb0, 1, 1);
        f.session.onMidi(0x90, 8, 127);
        f.session.onMidi(0x80, 8, 0);
        Checks.that(f.mixerEdits.isEmpty(), "a stale Mixer bank must reject edits");

        f.selectTrack(1);
        f.session.onMixerTargetChanged();
        f.session.flush();
        f.session.onMidi(0xb0, 1, 1);
        f.selectTrack(4);
        f.session.flush();
        f.session.onMidi(0xb0, 4, 1);
        Checks.that(f.mixerEdits.equals(List.of("1 volume " + 1.0 / 16384, "4 volume " + 1.0 / 16384)),
                "edits resume in the aligned bank, including when the selected track is in the last column");
    }

    private static void mixerRecoversAfterReturningToTrack() {
        for (boolean deferred : new boolean[] {false, true}) {
            var f = new SessionFixture();
            f.trackCount = 5;
            f.mixerNames.addAll(List.of("Pads", "Vox"));
            f.session.start();
            f.acknowledgeAndDismissWelcome(ACK);
            f.session.flush();
            f.deferTrackMove = deferred;
            f.session.onMidi(0xb0, 15, 1);
            if (deferred) f.selectTrack(1);
            f.session.flush();
            f.selectTrack(0);
            f.session.flush();

            int modeRings = f.modeRings.size();
            f.session.onMidi(0xb0, 16, 1);
            f.session.flush();
            Checks.equal(modeRings + 1, f.modeRings.size());
            f.session.onMidi(0xb0, 1, 1);
            Checks.that(f.mixerEdits.equals(List.of("0 volume " + 1.0 / 16384)),
                    "Mixer enables the current track after returning to the origin");
        }
    }

    private static void mixerWaitsForNavigationStartedInDeviceMode() {
        var f = new SessionFixture();
        f.trackCount = 5;
        f.mixerNames.addAll(List.of("Pads", "Vox"));
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        f.deferTrackMove = true;
        f.session.onMidi(0xb0, 15, 1);
        f.session.onMidi(0xb0, 16, 1);
        f.session.flush();
        f.session.onMidi(0xb0, 1, 1);
        Checks.equal(0, f.mixerEdits.size());

        f.selectTrack(1);
        f.session.flush();
        f.session.onMidi(0xb0, 1, 1);
        Checks.that(f.mixerEdits.equals(List.of("1 volume " + 1.0 / 16384)),
                "Mixer edits follow navigation started in Device mode");
    }

    private static void mixerRejectsReplacementTrack() {
        var f = new SessionFixture();
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.onMidi(0xb0, 16, 1);
        f.session.flush();
        f.session.onMidi(0x90, 9, 127);
        f.mixerTrackIds.put(1, "replacement");
        f.session.onMidi(0xb0, 2, 1);
        Checks.equal(0, f.mixerEdits.size());
        f.session.onSlotChanged();
        f.session.flush();
        f.session.onMidi(0x80, 9, 0);
        Checks.equal(0, f.mixerEdits.size());

        f.session.onMidi(0x90, 9, 127);
        f.session.onMidi(0x80, 9, 0);
        Checks.that(f.mixerEdits.equals(List.of("1 arm")), "new click can arm the replacement");
    }

    private static String screenHex(byte[] pixels) {
        return HexFormat.of().formatHex(E16Protocol.framebufferPrefixed(pixels));
    }

    private static void turnWritesOnlyToLiveSlot() {
        var f = new SessionFixture();
        f.session.start();
        f.session.onMidi(0xb0, 1, 1);
        Checks.equal(0, f.increments.size());
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.onMidi(0xb0, 1, 1);
        f.session.onMidi(0xb0, 1, 0x78);
        f.session.onMidi(0xb0, 2, 1);
        f.session.onMidi(0x90, 0, 127);
        Checks.that(f.increments.equals(List.of(1.0 / 16384, -24.0 / 256)), "live continuous changes");
        var other = new java.util.ArrayList<Double>();
        f.target = other;
        f.readSlot(true, 0);
        f.session.flush();
        Checks.equal(0, other.size());
        f.session.onMidi(0xb0, 1, 1);
        Checks.that(other.equals(List.of(1.0 / 16384)), "new target at minimum");
        f.readSlot(false, 0.5);
        f.session.onMidi(0xb0, 1, 1);
        f.readSlot(true, Double.NaN);
        f.session.onMidi(0xb0, 1, 1);
        f.readSlot(true, 0.5);
        f.session.stop();
        f.session.onMidi(0xb0, 1, 1);
        f.session.start();
        f.advanceTo(3000);
        f.session.onMidi(0xb0, 1, 1);
        f.failOnSend(f.sentHex().size() + 1);
        f.session.start();
        f.session.onMidi(0xb0, 1, 1);
        Checks.equal(1, other.size());
        Checks.equal(2, f.increments.size());
    }

    private static void eightTurnsReachIndependentLiveTargets() {
        var f = new SessionFixture();
        for (int i = 0; i < 8; i++) f.readSlot(i, "Same", true, 0.5, -1);
        f.readSlot(7, "Same", true, 0.5, 16);
        f.readSlot(3, "Same", true, 0, 2);
        f.session.start();
        f.session.onMidi(0xb0, 8, 1);
        f.acknowledgeAndDismissWelcome(ACK);
        for (int cc : new int[] {1, 8, 4, 2, 3, 5, 6, 7}) {
            f.session.onMidi(0xb0, cc, cc == 8 ? 0x78 : 8);
        }
        Checks.that(f.increments.equals(List.of(24.0 / 256)), "first continuous slot");
        for (int i = 1; i < 8; i++) {
            double expected = switch (i) {
                case 3 -> 1;
                case 7 -> -1.0 / 15;
                default -> 24.0 / 256;
            };
            Checks.that(f.otherTargets.get(i - 1).equals(List.of(expected)), "live edits for slot " + i);
        }
        var nextTarget = new java.util.ArrayList<Double>();
        f.otherTargets.set(6, nextTarget);
        f.session.onMidi(0xb0, 8, 8);
        f.readSlot(7, "Same", false, 0, -1);
        f.session.onMidi(0xb0, 8, 8);
        for (int count : new int[] {-2, 0, 1}) {
            f.readSlot(7, "Same", true, 0, count);
            f.session.onMidi(0xb0, 8, 8);
        }
        f.session.stop();
        f.session.onMidi(0xb0, 8, 8);
        Checks.that(nextTarget.equals(List.of(1.0 / 15)), "replacement target with unchanged label");
    }

    private static void selectorMovesOneItem() {
        var f = new SessionFixture();
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.readSlot(true, 0.5, 3);
        for (int value : new int[] {1, 8, 0x7f, 0x78}) f.session.onMidi(0xb0, 1, value);
        f.readSlot(true, 0, 2);
        f.session.onMidi(0xb0, 1, 8);
        f.session.onMidi(0xb0, 1, 0x78);
        for (int count : new int[] {-2, 0, 1}) {
            f.readSlot(true, 0, count);
            f.session.onMidi(0xb0, 1, 1);
        }
        Checks.that(f.increments.equals(List.of(0.5, 0.5, -0.5, -0.5, 1.0, -1.0)),
                "one adjacent choice per turn message");
    }

    private static void failedIncrementIsNeverReplayed() {
        var f = new SessionFixture();
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        f.failIncrement = true;
        f.session.onMidi(0xb0, 1, 1);
        Checks.equal(1, f.errors().size());
        Checks.equal(2, f.flushRequests());
        f.session.flush();
        f.session.stop();
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        Checks.equal(1, f.increments.size());
    }

    private static void feedbackUsesLatestBitwigValue() {
        var f = new SessionFixture();
        f.session.start();
        f.readSlot(true, 0.25);
        f.readSlot(true, 0.75);
        f.session.flush();
        Checks.that(f.sentHex().equals(List.of(ENTRY)), "no display before ACK");
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        Checks.equal(13, f.sentHex().size());
        Checks.that(f.sentHex().get(2).equals("f000215b0201060400000008005f7f00f7"), "75 percent");
        f.readSlot(true, 0.75);
        f.session.flush();
        Checks.equal(13, f.sentHex().size());
        int requests = f.flushRequests();
        f.readSlot(true, 0.4);
        f.readSlot(true, 0.25);
        Checks.equal(requests + 1, f.flushRequests());
        f.session.flush();
        Checks.equal(14, f.sentHex().size());
        Checks.that(f.sentHex().get(13).equals("f000215b020106040000000800200000f7"), "25 percent");
    }

    private static void eightSlotsAndRenderedLabelsUpdateIndependently() {
        var f = new SessionFixture();
        f.readSlot(7, "Unison", true, 0.5, 16);
        f.session.start(); f.acknowledgeAndDismissWelcome(ACK); f.session.flush();
        for (int i = 0; i < 8; i++) {
            Checks.that(f.sentHex().get(i + 2).startsWith("f000215b02010604000" + i), "ring index " + i);
        }
        assertControls(f.sentHex().get(1), "Test Page", "RC1", "----", "Unison");
        f.readSlot(7, "Unison", true, 0.75, 16);
        f.session.flush();
        Checks.equal(14, f.sentHex().size());
        Checks.that(f.sentHex().get(13).equals("f000215b0201060400070008005f7f00f7"), "slot 8 only");
        f.readSlot(3, "Resonance", true, 0, -1);
        f.session.flush();
        Checks.equal(16, f.sentHex().size());
        assertControls(f.sentHex().get(14), "Test Page", "RC1", "Resonance", "Unison");
        f.readSlot(7, "Unison Mode", true, 0.75, 16);
        f.session.flush();
        Checks.equal(17, f.sentHex().size());
        assertControls(f.sentHex().getLast(), "Test Page", "RC1", "Resonance", "Unison Mode");
        f.setTitle("Different");
        f.session.flush();
        Checks.equal(18, f.sentHex().size());
        assertControls(f.sentHex().getLast(), "Different", "RC1", "Resonance", "Unison Mode");
        f.readSlot(7, "Unison", false, 0.75, 16);
        f.session.flush();
        Checks.equal(20, f.sentHex().size());
        assertControls(f.sentHex().get(18), "Different", "RC1", "Resonance", "----");
        Checks.that(f.sentHex().getLast().equals("f000215b020106040007000000000000f7"), "slot 8 cleared");
    }

    private static void assertControls(String hex, String title, String first, String fourth, String eighth) {
        Checks.that(hex.startsWith("f000215b02010602") && hex.endsWith("f7"), "prefixed framebuffer");
        byte[] frame = HexFormat.of().parseHex(hex);
        Checks.equal(1180, frame.length);
        byte[] raw = new byte[1024];
        for (int i = 0; i < raw.length; i++) {
            int groupStart = 8 + i / 7 * 8;
            int index = i % 7;
            raw[i] = (byte) (frame[groupStart + 1 + index] | (((frame[groupStart] >> index) & 1) << 7));
        }
        var labels = List.of(first, "----", "----", fourth, "----", "----", "----", eighth);
        Checks.that(Arrays.equals(raw, ControlScreen.render(title, labels)), "complete controls framebuffer");
    }

    private static void restartDuringMiddleRingDiscardsOldFrames() {
        var f = new SessionFixture();
        f.onSend(6, () -> {
            f.session.stop();
            f.session.start();
            f.acknowledgeAndDismissWelcome(ACK);
        });
        f.session.start(); f.acknowledgeAndDismissWelcome(ACK); f.session.flush();
        Checks.equal(8, f.sentHex().size());
        Checks.that(f.sentHex().subList(6, 8).equals(List.of(EXIT, ENTRY)), "old flush stopped");
        f.session.flush();
        Checks.equal(20, f.sentHex().size());
        Checks.that(f.sentHex().subList(8, 13).equals(f.sentHex().subList(1, 6)), "new session redraws");
    }

    private static void emptySlotDiffersFromMinimum() {
        var f = new SessionFixture();
        f.readSlot(true, -0.01);
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        String assignedLabel = f.sentHex().get(1);
        Checks.that(f.sentHex().get(2).equals("f000215b020106040000000800000000f7"), "clamped green minimum");
        f.readSlot(false, 0);
        f.session.flush();
        Checks.equal(15, f.sentHex().size());
        Checks.that(!f.sentHex().get(13).equals(assignedLabel), "empty label differs");
        Checks.that(f.sentHex().get(14).equals("f000215b020106040000000000000000f7"), "empty ring black");
        f.readSlot(true, Double.NaN);
        f.session.flush();
        Checks.equal(15, f.sentHex().size());
        f.readSlot(true, 1.01);
        f.session.flush();
        Checks.equal(17, f.sentHex().size());
        Checks.that(f.sentHex().get(15).equals(assignedLabel), "assigned label restored");
        Checks.that(f.sentHex().get(16).equals("f000215b0201060400000008007f7f00f7"), "clamped maximum");
    }

    private static void refreshDuringSendSurvives() {
        var f = new SessionFixture();
        f.onSend(3, () -> f.readSlot(true, 0.25));
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        Checks.equal(2, f.flushRequests());
        f.session.flush();
        Checks.equal(14, f.sentHex().size());
        Checks.that(f.sentHex().get(13).equals("f000215b020106040000000800200000f7"), "latest ring survives send");
    }

    private static void timeoutIsBounded() {
        var f = new SessionFixture();
        f.session.start();
        Checks.equal(1, f.sentHex().size());
        f.advanceTo(999); Checks.equal(1, f.sentHex().size());
        f.advanceTo(1000); Checks.equal(2, f.sentHex().size());
        f.advanceTo(2000); Checks.equal(3, f.sentHex().size());
        f.advanceTo(3000); Checks.that(f.session.state() == RemoteSession.State.TIMED_OUT, "timed out");
        Checks.that(f.sentHex().equals(List.of(ENTRY, ENTRY, ENTRY, EXIT)), "bounded sends");
        f.acknowledgeAndDismissWelcome(ACK); f.session.stop(); f.advanceTo(10000);
        Checks.equal(4, f.sentHex().size());
        Checks.equal(0, f.flushRequests());
    }

    private static void ackStopsRetry() {
        var f = new SessionFixture();
        f.session.start(); f.advanceTo(500); f.session.onSysex(ACK);
        Checks.that(f.session.state() == RemoteSession.State.READY, "ready");
        Checks.equal(1, f.flushRequests());
        f.advanceTo(10000); Checks.equal(1, f.sentHex().size());
        f.session.stop(); Checks.that(f.sentHex().equals(List.of(ENTRY, EXIT)), "exit once");
        f.session.stop(); Checks.equal(2, f.sentHex().size());
    }

    private static void invalidAndDuplicateAckIgnored() {
        var f = new SessionFixture();
        f.session.start();
        for (String bad : new String[] {null, "xx", "f000215b02010653f700", "f000215b0201533805000a7f7f7ff7"}) {
            f.session.onSysex(bad);
        }
        Checks.that(f.session.state() == RemoteSession.State.WAITING_FOR_ACK, "still waiting");
        f.session.onSysex(ACK); f.session.onSysex(ACK);
        Checks.equal(1, f.flushRequests());
        Checks.throwsType(IllegalStateException.class, f.session::start);
        Checks.equal(1, f.sentHex().size());
    }

    private static void hardwareObservedAckStopsRetry() {
        var f = new SessionFixture();
        f.session.start();
        f.session.onSysex("f000215b02010653f7");
        Checks.that(f.session.state() == RemoteSession.State.READY, "prefixed hardware ACK accepted");
        Checks.equal(1, f.flushRequests());
        f.advanceTo(10000);
        Checks.equal(1, f.sentHex().size());
        f.session.stop();
        Checks.that(f.sentHex().equals(List.of(ENTRY, EXIT)), "one entry and one exit");
    }

    private static void stopAndRestartInvalidateTimers() {
        var f = new SessionFixture();
        f.session.stop(); Checks.equal(0, f.sentHex().size());
        f.session.start(); f.session.stop(); f.session.start();
        f.advanceTo(1000);
        Checks.that(f.sentHex().equals(List.of(ENTRY, EXIT, ENTRY, ENTRY)), "only new retry");
    }

    private static void synchronousAckIsAccepted() {
        var f = new SessionFixture();
        f.onSend(1, () -> f.session.onSysex(ACK));
        f.session.start();
        Checks.that(f.session.state() == RemoteSession.State.READY, "synchronous ACK");
        Checks.equal(1, f.flushRequests());
        f.advanceTo(10000); Checks.equal(1, f.sentHex().size());
    }

    private static void failuresCleanUp() {
        var output = new SessionFixture();
        output.failOnSend(1); output.session.start();
        Checks.that(output.session.state() == RemoteSession.State.FAILED, "send failure");
        Checks.that(output.sentHex().equals(List.of(ENTRY, EXIT)), "cleanup on send failure");
        Checks.that(!output.errors().isEmpty(), "error logged");
        output.advanceTo(10000); Checks.equal(2, output.sentHex().size());

        var timer = new SessionFixture();
        timer.failNextSchedule(); timer.session.start();
        Checks.that(timer.session.state() == RemoteSession.State.FAILED, "scheduler failure");
        Checks.that(timer.sentHex().equals(List.of(ENTRY, EXIT)), "cleanup on scheduler failure");
    }

    private static void displayWaitsForAckAndFlush() {
        var f = new SessionFixture();
        f.session.start(); f.session.flush();
        Checks.equal(1, f.sentHex().size());
        f.session.onSysex("f000215b02010653f7");
        Checks.equal(1, f.flushRequests());
        Checks.equal(1, f.sentHex().size());
        f.session.flush();
        Checks.equal(13, f.sentHex().size());
        Checks.that(f.sentHex().get(1).startsWith("f000215b02010602"), "prefixed controls first");
        Checks.equal(2360, f.sentHex().get(1).length());
        Checks.that(f.sentHex().get(2).equals("f000215b020106040000000800400000f7"), "prefixed ring second");
        f.session.flush(); f.session.onSysex("f000215b02010653f7");
        Checks.equal(13, f.sentHex().size());
        f.session.stop(); Checks.equal(14, f.sentHex().size());
    }

    private static void failedSendResendsRestoredState() {
        for (boolean labels : new boolean[] {false, true}) {
            var f = new SessionFixture();
            f.session.start();
            f.acknowledgeAndDismissWelcome(ACK);
            f.session.flush();
            String original = f.sentHex().get(labels ? 1 : 2);
            f.failOnSend(14);
            if (labels) f.setTitle("New Page");
            else f.readSlot(true, 0.75);
            f.session.flush();
            Checks.equal(1, f.errors().size());
            Checks.that(!f.sentHex().get(13).equals(original), "attempted a different frame");
            if (labels) f.setTitle("Test Page");
            else f.readSlot(true, 0.5);
            f.advanceTo(250);
            f.session.flush();
            Checks.that(f.sentHex().subList(14, f.sentHex().size()).equals(List.of(original)),
                    "retry restores " + (labels ? "labels" : "ring"));
        }
    }

    private static void equivalentRenderedControlsStayQuiet() {
        var f = new SessionFixture();
        f.readSlot(0, "Cut", true, 0.5, -1);
        f.setTitle("Device Control");
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        f.readSlot(0, " Cut  ", true, 0.5, -1);
        f.session.flush();
        Checks.equal(13, f.sentHex().size());
        f.setTitle("Device Control ");
        f.session.flush();
        Checks.equal(13, f.sentHex().size());
    }

    private static void trackNavigationAndGroupPress() {
        var f = new SessionFixture();
        f.deviceCount = 0;
        f.session.start();
        f.session.onMidi(0xb0, 15, 1);
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        f.session.onMidi(0xb1, 15, 1);
        Checks.that(f.trackDirections.isEmpty(), "track moves gated by ACK and channel");
        f.failTrackMove = true;
        f.session.onMidi(0xb0, 15, 8);
        f.session.flush();
        Checks.that(f.trackDirections.equals(List.of(1)), "track move from an empty track is consumed once");
        String trackScreen = assertSelectionScreen(f);
        assertNavigationRing(f.sentHex().getLast(), 14, 8);

        f.session.onMidi(0x90, 14, 127);
        Checks.that(!f.groupExpanded, "ordinary track ignores push");
        f.session.onMidi(0x80, 14, 0);
        f.groupSelected = true;
        f.session.onMidi(0x90, 16, 127);
        Checks.that(!f.groupExpanded, "Shift does not fold a group");
        f.session.onMidi(0x90, 14, 127);
        f.session.onMidi(0x90, 14, 127);
        Checks.that(f.groupExpanded, "held button toggles once");
        f.session.onMidi(0x90, 14, 0);
        f.session.onMidi(0x90, 14, 127);
        Checks.that(!f.groupExpanded, "released button can toggle again");

        f.deviceName = f.trackName;
        f.session.onMidi(0xb0, 14, 1);
        f.session.flush();
        Checks.that(!assertSelectionScreen(f).equals(trackScreen), "switch to device header for the same name");
        f.session.onMidi(0xb0, 13, 1);
        f.session.flush();
        assertControls(f.sentHex().getLast(), "Test Page", "RC1", "----", "----");

        int commands = f.trackDirections.size();
        f.session.stop();
        f.session.onMidi(0xb0, 15, 1);
        f.session.start();
        f.session.onMidi(0xb0, 15, 1);
        Checks.equal(commands, f.trackDirections.size());
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.onMidi(0x90, 14, 127);
        Checks.that(f.groupExpanded, "re-entry clears held state");
    }

    private static void deviceNavigationShowsLiveNameAndReturnsToControls() {
        var f = new SessionFixture();
        f.session.start();
        f.session.onMidi(0xb0, 14, 1);
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        f.session.onMidi(0xb1, 14, 1);
        f.session.onMidi(0xb0, 14, 0);
        Checks.equal(0, f.deviceDirections.size());

        var destination = new java.util.ArrayList<Double>();
        f.deviceTurn = () -> {
            f.target = destination;
            f.deviceName = "Mixwave Teezio Azul";
            f.deviceIndex = 1;
        };
        f.session.onMidi(0xb0, 14, 8);
        f.session.onMidi(0xb0, 1, 1);
        f.session.flush();
        Checks.that(f.deviceDirections.equals(List.of(1)), "one device move");
        Checks.that(f.pageDirections.isEmpty() && f.increments.isEmpty(), "old page and target untouched");
        Checks.that(destination.equals(List.of(1.0 / 16384)), "new target with same page title");
        String firstDeviceScreen = assertSelectionScreen(f);
        Checks.that(firstDeviceScreen.equals(HexFormat.of().formatHex(E16Protocol.framebufferPrefixed(
                SelectionScreen.render("DEVICE", "Mixwave Teezio Azul")))),
                "device navigation keeps priority while editing");
        assertNavigationRing(f.sentHex().getLast(), 13, 16);

        f.advanceTo(600);
        f.deviceTurn = () -> f.deviceName = "Renamed";
        f.session.onMidi(0xb0, 14, 0x78);
        f.advanceTo(750);
        f.session.flush();
        Checks.that(!assertSelectionScreen(f).equals(firstDeviceScreen), "rename redraws device text");
        int frames = f.sentHex().size();
        f.readSlot(0, "New Cutoff", true, 0.5, -1);
        f.advanceTo(1349);
        f.session.flush();
        Checks.equal(frames, f.sentHex().size());
        f.advanceTo(1350);
        f.session.flush();
        assertControls(f.sentHex().getLast(), "Test Page", "New Cutoff", "----", "----");

        f.session.onMidi(0xb0, 14, 1);
        f.session.flush();
        f.session.onMidi(0xb0, 13, 1);
        f.session.flush();
        assertControls(f.sentHex().getLast(), "Test Page", "New Cutoff", "----", "----");
    }

    private static void deviceRingUsesFullPositionRange() {
        var f = new SessionFixture();
        f.deviceCount = 3;
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        var initial = f.sentHex();
        assertNavigationRing(initial.get(initial.size() - 2), 13, 0);
        for (int index = 1; index < 3; index++) {
            f.deviceIndex = index;
            f.session.onSlotChanged();
            f.session.flush();
            assertNavigationRing(f.sentHex().getLast(), 13, index * 8);
        }
    }

    private static void deviceNavigationSpacesRepeatedTurns() {
        var f = new SessionFixture();
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.deviceCount = 6;
        f.deviceTurn = () -> f.deviceName = "Middle Device";
        f.session.onMidi(0xb0, 14, 1);
        f.advanceTo(15);
        f.session.onMidi(0xb0, 14, 8);
        f.advanceTo(179);
        f.session.onMidi(0xb0, 14, 1);
        Checks.that(f.deviceDirections.equals(List.of(1)), "fast burst stops at the next device");
        f.advanceTo(180);
        f.session.onMidi(0xb0, 14, 1);
        f.advanceTo(181);
        f.session.onMidi(0xb0, 14, 0x7f);
        f.advanceTo(200);
        f.session.onMidi(0xb0, 14, 0x78);
        Checks.that(f.deviceDirections.equals(List.of(1, 1, -1)), "spaced steps and immediate reversal");
        f.advanceTo(949);
        f.session.flush();
        assertSelectionScreen(f);
        f.advanceTo(950);
        f.session.flush();
        assertControls(f.sentHex().getLast(), "Test Page", "RC1", "----", "----");
        Checks.that(f.deviceDirections.equals(List.of(1, 1, -1)), "no queued navigation");
    }

    private static void shortDeviceChainWaitsForANewGesture() {
        var f = new SessionFixture();
        f.deviceCount = 3;
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.onMidi(0xb0, 14, 8);
        for (int time = 50; time <= 250; time += 50) {
            f.advanceTo(time);
            f.session.onMidi(0xb0, 14, 8);
        }
        Checks.that(f.deviceDirections.equals(List.of(1)), "one move during a short-chain burst");
        f.advanceTo(350);
        f.session.onMidi(0xb0, 14, 1);
        f.advanceTo(351);
        f.session.onMidi(0xb0, 14, 0x7f);
        Checks.that(f.deviceDirections.equals(List.of(1, 1, -1)), "pause or reversal allows the next step");
    }

    private static void deviceViewExpirySurvivesFailedSendAndRestart() {
        var f = new SessionFixture();
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        String original = f.sentHex().get(1);
        f.session.onMidi(0xb0, 14, 1);
        f.advanceTo(700);
        f.failOnSend(f.sentHex().size() + 1);
        f.session.flush();
        f.advanceTo(950);
        f.session.flush();
        Checks.that(f.sentHex().getLast().equals(original), "retry restores the controls after expiry");
        Checks.that(f.deviceDirections.equals(List.of(1)), "display retry never replays a move");

        f.session.onMidi(0xb0, 14, 1);
        f.session.stop();
        f.session.start();
        f.session.onMidi(0xb0, 14, 1);
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        int requests = f.flushRequests();
        f.advanceTo(2000);
        Checks.equal(requests, f.flushRequests());
        Checks.that(f.deviceDirections.equals(List.of(1, 1)), "restart gates navigation until ACK");
    }

    private static String assertSelectionScreen(SessionFixture f) {
        var latest = f.sentHex().reversed().stream()
                .filter(frame -> frame.startsWith("f000215b02010602")).findFirst().orElseThrow();
        Checks.that(latest.startsWith("f000215b02010602"), "selection uses the framebuffer");
        Checks.equal(2360, latest.length());
        return latest;
    }

    private static void pageNavigationRingShowsProgress() {
        var f = new SessionFixture();
        f.pageCount = 4;
        f.session.start();
        f.session.flush();
        Checks.equal(1, f.sentHex().size());
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        assertPageRing(f.sentHex().getLast(), 4);

        int frames = f.sentHex().size();
        f.pageTurn = () -> f.pageIndex = 1;
        f.session.onMidi(0xb0, 13, 1);
        f.session.flush();
        Checks.equal(frames + 1, f.sentHex().size());
        assertPageRing(f.sentHex().getLast(), 8);
        f.session.onMidi(0xb0, 13, 1);
        f.session.flush();
        Checks.equal(frames + 1, f.sentHex().size());

        f.pageCount = 32;
        f.pageIndex = 30;
        f.session.onSlotChanged();
        f.session.flush();
        assertPageRing(f.sentHex().getLast(), 15);
        f.pageIndex = 31;
        f.session.onSlotChanged();
        f.session.flush();
        assertPageRing(f.sentHex().getLast(), 16);

        f.pageCount = 0;
        f.session.onSlotChanged();
        f.session.flush();
        assertPageRing(f.sentHex().getLast(), 0);
        f.session.stop();
        f.pageCount = 1;
        f.pageIndex = 0;
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        assertPageRing(f.sentHex().getLast(), 16);
    }

    private static void assertPageRing(String hex, int purpleCount) {
        assertNavigationRing(hex, 12, purpleCount);
    }

    private static void assertNavigationRing(String hex, int encoder, int purpleCount) {
        byte[] packet = java.util.HexFormat.of().parseHex(hex);
        Checks.that(hex.startsWith("f000215b02010601"), "prefixed individual LED command");
        Checks.equal(101, packet.length);
        for (int segment = 0; segment < 16; segment++) {
            int offset = segment * 5;
            boolean purple = segment < purpleCount;
            int[] expected = {encoder, segment, purple ? 7 : 22, purple ? 3 : 5, purple ? 22 : 2};
            for (int field = 0; field < expected.length; field++) {
                int rawIndex = offset + field;
                int maskIndex = 8 + rawIndex / 7 * 8;
                Checks.equal(0, packet[maskIndex]);
                Checks.equal(expected[field], packet[maskIndex + 1 + rawIndex % 7]);
            }
        }
    }

    private static void pageNavigationShowsLiveControls() {
        var f = new SessionFixture();
        f.session.start();
        f.session.onMidi(0xb0, 13, 1);
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        for (int cc : new int[] {9, 10, 11, 12}) f.session.onMidi(0xb0, cc, 1);
        f.session.onMidi(0xb1, 13, 1);
        f.session.onMidi(0x90, 13, 1);
        f.session.onMidi(0x90, 16, 127);
        f.session.onMidi(0xb0, 13, 0);
        f.session.onMidi(0xb0, 13, 9);
        Checks.equal(0, f.pageDirections.size());
        f.pageTurn = () -> f.setTitle("Selected");
        for (int value : new int[] {1, 8, 0x7f, 0x78}) f.session.onMidi(0xb0, 13, value);
        Checks.that(f.pageDirections.equals(List.of(1, 1, -1, -1)), "one step per turn");
        Checks.equal(0, f.increments.size());
        f.session.flush();
        assertControls(f.sentHex().getLast(), "Selected", "RC1", "----", "----");

        f.advanceTo(600);
        f.pageTurn = () -> {};
        f.session.onMidi(0xb0, 13, 8);
        f.setTitle("Mouse selection");
        f.readSlot(0, "Mix", true, 0.75, -1);
        f.session.onMidi(0xb0, 1, 8);
        Checks.that(f.increments.equals(List.of(24.0 / 256)), "parameter remains editable");
        f.session.flush();
        var frames = f.sentHex();
        assertParameter(frames.get(frames.size() - 2), "Mix", "75%");
        Checks.that(frames.getLast().equals("f000215b0201060400000008005f7f00f7"), "live ring on selected page");
        f.advanceTo(2000);
        f.session.flush();
        assertControls(f.sentHex().getLast(), "Mouse selection", "Mix", "----", "----");
        f.session.stop();
        f.session.onMidi(0xb0, 13, 1);
        Checks.equal(5, f.pageDirections.size());
    }

    private static void knobDetailFollowsPhysicalTurns() {
        var f = new SessionFixture();
        f.readSlot(0, "Cutoff", true, 0.5, -1);
        f.readSlot(1, "Waveform", true, 0.5, 3);
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        String overview = assertSelectionScreen(f);
        f.readSlot(0, "Cutoff", true, 0.25, -1, "440 Hz");
        f.session.flush();
        Checks.that(overview.equals(assertSelectionScreen(f)), "automation keeps overview");

        f.session.onMidi(0xb0, 1, 1);
        f.session.flush();
        String cutoff = assertSelectionScreen(f);
        assertParameter(cutoff, "Cutoff", "440 Hz");
        f.advanceTo(300);
        f.session.onMidi(0xb0, 2, 1);
        f.session.flush();
        Checks.that(cutoff.equals(assertSelectionScreen(f)), "first knob keeps focus");
        Checks.equal(1, f.otherTargets.getFirst().size());
        f.readSlot(0, "Cutoff", true, 0.75, -1);
        f.session.flush();
        assertParameter(assertSelectionScreen(f), "Cutoff", "75%");
        f.advanceTo(750);
        f.session.flush();
        Checks.that(overview.equals(assertSelectionScreen(f)), "automation and second knob did not extend timer");

        f.session.onMidi(0xb0, 2, 1);
        f.session.flush();
        assertParameter(assertSelectionScreen(f), "Waveform", "2/3");
        f.session.onMidi(0xb0, 13, 1);
        f.session.flush();
        Checks.that(overview.equals(assertSelectionScreen(f)), "page navigation restores overview");
    }

    private static void hostPageChangeClearsDetail() {
        var f = new SessionFixture();
        f.pageCount = 2;
        f.readSlot(0, "Cutoff", true, 0.5, -1, "440 Hz");
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        f.session.onMidi(0xb0, 1, 1);
        f.session.flush();
        assertParameter(assertSelectionScreen(f), "Cutoff", "440 Hz");

        f.pageIndex = 1;
        f.session.onSelectionChanged();
        f.readSlot(0, "Drive", true, 0.5, -1, "50%");
        f.session.flush();
        assertControls(assertSelectionScreen(f), "Test Page", "Drive", "----", "----");
        f.session.onMidi(0xb0, 1, 1);
        f.session.flush();
        assertParameter(assertSelectionScreen(f), "Drive", "50%");
    }

    private static void assertParameter(String wire, String name, String value) {
        byte[] pixels = ParameterScreen.render(name, value);
        Checks.that(wire.equals(HexFormat.of().formatHex(E16Protocol.framebufferPrefixed(pixels))),
                "parameter detail shows " + name + " and " + value);
    }

    private static void sameTitlePageRedrawsControls() {
        var f = new SessionFixture();
        f.readSlot(0, "Filter A", true, 0.5, -1);
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        int before = f.sentHex().size();
        f.pageTurn = () -> f.readSlot(0, "Filter B", true, 0.5, -1);
        f.session.onMidi(0xb0, 13, 1);
        f.session.flush();
        Checks.equal(before + 1, f.sentHex().size());
        assertControls(f.sentHex().getLast(), "Test Page", "Filter B", "----", "----");
    }

    private static void pageNavigationSurvivesSendFailureAndRestart() {
        var f = new SessionFixture();
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        f.pageTurn = () -> {
            f.setTitle("Destination");
            throw new IllegalStateException("host threw after moving");
        };
        f.session.onMidi(0xb0, 13, 1);
        f.advanceTo(700);
        f.failOnSend(14);
        f.session.flush();
        f.advanceTo(950);
        f.session.flush();
        assertControls(f.sentHex().getLast(), "Destination", "RC1", "----", "----");
        Checks.that(f.sentHex().getLast().equals(f.sentHex().get(13)), "retry sends destination labels");
        Checks.equal(1, f.pageDirections.size());
        f.session.onMidi(0xb0, 13, 1);
        f.session.stop();
        f.session.start();
        f.acknowledgeAndDismissWelcome(ACK);
        f.session.flush();
        int frameCount = f.sentHex().size();
        int requests = f.flushRequests();
        f.advanceTo(1700);
        f.session.flush();
        Checks.equal(frameCount, f.sentHex().size());
        Checks.equal(requests, f.flushRequests());
        Checks.equal(2, f.pageDirections.size());
    }

    private static void displayFailureRetriesPendingRings() {
        var f = new SessionFixture();
        f.failOnSend(6);
        f.session.start(); f.acknowledgeAndDismissWelcome(ACK); f.session.flush();
        Checks.equal(6, f.sentHex().size());
        Checks.equal(1, f.errors().size());
        Checks.that(f.session.state() == RemoteSession.State.READY, "ready after display failure");
        f.advanceTo(1000);
        Checks.equal(2, f.flushRequests());
        f.session.flush();
        Checks.equal(14, f.sentHex().size());
        Checks.that(f.sentHex().get(5).equals(f.sentHex().get(6)), "retry starts at failed ring");
        f.advanceTo(10000); f.session.flush();
        Checks.equal(14, f.sentHex().size());

        var broken = new SessionFixture();
        broken.failOnSend(6);
        broken.session.start(); broken.acknowledgeAndDismissWelcome(ACK); broken.session.flush();
        broken.failOnSend(7);
        broken.advanceTo(1000); broken.session.flush();
        broken.advanceTo(10000); broken.session.flush();
        Checks.equal(7, broken.sentHex().size());
        broken.readSlot(true, 0.25);
        broken.session.flush();
        Checks.equal(16, broken.sentHex().size());
    }

    private static void stopDuringLabelsPreventsRing() {
        var f = new SessionFixture();
        f.onSend(2, f.session::stop);
        f.session.start(); f.acknowledgeAndDismissWelcome(ACK); f.session.flush();
        Checks.equal(3, f.sentHex().size());
        Checks.that(f.sentHex().get(2).equals(EXIT), "stop sent exit");
    }

    private static void stopAfterAckBeforeFlushPreventsDisplay() {
        var f = new SessionFixture();
        f.session.start(); f.acknowledgeAndDismissWelcome(ACK); f.session.stop(); f.session.flush();
        Checks.that(f.sentHex().equals(List.of(ENTRY, EXIT)), "stopped flush sent no display");
    }

    private static void oldSendFailureDoesNotFailRestartedSession() {
        var f = new SessionFixture();
        f.onSend(2, () -> {
            f.session.stop();
            f.session.start();
            f.acknowledgeAndDismissWelcome(ACK);
        });
        f.failOnSend(2);
        f.session.start(); f.acknowledgeAndDismissWelcome(ACK); f.session.flush();
        Checks.that(f.session.state() == RemoteSession.State.READY, "old send failure must not fail new session");
        Checks.equal(4, f.sentHex().size());
        Checks.that(f.sentHex().get(2).equals(EXIT), "old session cleaned up once");
        Checks.that(f.sentHex().get(3).equals(ENTRY), "new session entered");
        f.session.flush();
        Checks.equal(16, f.sentHex().size());
        Checks.that(f.sentHex().get(4).startsWith("f000215b02010602"), "new session sends labels");
        Checks.that(f.sentHex().get(5).equals("f000215b020106040000000800400000f7"),
                "new session sends its ring");
    }

    private static void restartDuringLabelsDefersNewDisplay() {
        var f = new SessionFixture();
        f.onSend(2, () -> {
            f.session.stop();
            f.session.start();
            f.acknowledgeAndDismissWelcome(ACK);
        });
        f.session.start(); f.acknowledgeAndDismissWelcome(ACK); f.session.flush();
        Checks.that(f.session.state() == RemoteSession.State.READY, "new session remains ready");
        Checks.equal(4, f.sentHex().size());
        Checks.that(f.sentHex().get(2).equals(EXIT) && f.sentHex().get(3).equals(ENTRY),
                "old ring not sent into new session");
        f.session.flush();
        Checks.equal(16, f.sentHex().size());
        Checks.that(f.sentHex().get(5).equals("f000215b020106040000000800400000f7"),
                "new session ring delivered after its label");
    }
}
