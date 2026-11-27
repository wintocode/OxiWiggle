package io.github.wintocode.oxiwiggle.bitwig;

import com.bitwig.extension.controller.api.BeatTimeFormatter;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.Transport;
import io.github.wintocode.oxiwiggle.surface.TransportControls;
import java.util.Locale;

public final class BitwigTransport implements TransportControls {
    private final Transport transport;
    private final BeatTimeFormatter barBeat;
    private final BeatTimeFormatter fullPosition;
    private static final long POSITION_INTERVAL_NANOS = 100_000_000L;
    private long lastPlaybackUpdate = System.nanoTime() - POSITION_INTERVAL_NANOS;

    public BitwigTransport(ControllerHost host, Transport transport) {
        this.transport = transport;
        fullPosition = host.createBeatTimeFormatter(".", 1, 1, 1, 2);
        barBeat = host.createBeatTimeFormatter(".", 1, 1, 0, 0);
    }

    public void observe(Runnable changed) {
        transport.playPosition().addValueObserver(ignored -> {
            if (transport.isPlaying().get()) {
                long now = System.nanoTime();
                if (now - lastPlaybackUpdate < POSITION_INTERVAL_NANOS) return;
                lastPlaybackUpdate = now;
            }
            changed.run();
        });
        transport.arrangerLoopStart().addValueObserver(ignored -> changed.run());
        transport.arrangerLoopDuration().addValueObserver(ignored -> changed.run());
        transport.tempo().value().displayedValue().addValueObserver(ignored -> changed.run());
        transport.timeSignature().numerator().addValueObserver(ignored -> changed.run());
        transport.timeSignature().denominator().addValueObserver(ignored -> changed.run());
        transport.isPlaying().addValueObserver(ignored -> changed.run());
        transport.isArrangerRecordEnabled().addValueObserver(ignored -> changed.run());
        transport.isArrangerLoopEnabled().addValueObserver(ignored -> changed.run());
        transport.isMetronomeEnabled().addValueObserver(ignored -> changed.run());
        transport.isArrangerOverdubEnabled().addValueObserver(ignored -> changed.run());
        transport.isArrangerAutomationWriteEnabled().addValueObserver(ignored -> changed.run());
        transport.isPunchInEnabled().addValueObserver(ignored -> changed.run());
        transport.isPunchOutEnabled().addValueObserver(ignored -> changed.run());
    }

    @Override public Snapshot read() {
        String tempo = transport.tempo().value().displayedValue().get().strip();
        if (!tempo.isEmpty() && !tempo.toUpperCase(Locale.ROOT).endsWith("BPM")) tempo += " BPM";
        return new Snapshot(transport.playPosition().getFormatted(fullPosition), tempo,
                transport.arrangerLoopStart().getFormatted(barBeat),
                transport.arrangerLoopDuration().getFormatted(barBeat),
                transport.isPlaying().get(), transport.isArrangerRecordEnabled().get(),
                transport.isArrangerLoopEnabled().get(), transport.isMetronomeEnabled().get(),
                transport.isArrangerOverdubEnabled().get(), transport.isArrangerAutomationWriteEnabled().get(),
                transport.isPunchInEnabled().get(), transport.isPunchOutEnabled().get());
    }

    @Override public void click(int knob, boolean shifted) {
        switch (knob) {
            case 1 -> transport.play();
            case 2 -> transport.stop();
            case 3 -> transport.record();
            case 4 -> transport.restart();
            case 6 -> transport.addCueMarkerAtPlaybackPosition();
            case 7 -> transport.isArrangerLoopEnabled().toggle();
            case 9 -> transport.tapTempo();
            case 10 -> transport.isMetronomeEnabled().toggle();
            case 11 -> (shifted ? transport.isPunchInEnabled() : transport.isArrangerOverdubEnabled()).toggle();
            case 12 -> (shifted ? transport.isPunchOutEnabled() : transport.isArrangerAutomationWriteEnabled()).toggle();
        }
    }

    @Override public void turn(int knob, int delta, boolean shifted) {
        int direction = Integer.signum(delta);
        if (direction == 0) return;
        switch (knob) {
            case 5, 7, 8 -> {
                var signature = transport.timeSignature();
                int numerator = signature.numerator().get();
                int denominator = signature.denominator().get();
                if (denominator <= 0 || numerator <= 0) return;
                // API beat-time values count quarter notes even in meters such as 6/8.
                double beat = 4.0 / denominator;
                double bar = beat * numerator;
                if (knob == 5) transport.incPosition(direction * (shifted ? beat : bar), false);
                else if (knob == 7) stepLoop(transport.arrangerLoopStart(), direction, bar, 0);
                else stepLoop(transport.arrangerLoopDuration(), direction, bar, bar);
            }
            case 6 -> {
                if (direction < 0) transport.jumpToPreviousCueMarker();
                else transport.jumpToNextCueMarker();
            }
            case 9 -> transport.tempo().value().incRaw(direction);
            case 10 -> transport.metronomeVolume().inc(delta / 128.0);
        }
    }

    private static void stepLoop(SettableBeatTimeValue value, int direction, double bar, double minimum) {
        double amount = direction > 0 ? bar : -Math.clamp(value.get() - minimum, 0.0, bar);
        if (amount != 0) value.inc(amount);
    }
}
