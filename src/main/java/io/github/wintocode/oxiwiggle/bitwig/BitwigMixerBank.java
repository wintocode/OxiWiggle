package io.github.wintocode.oxiwiggle.bitwig;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.TrackBankContentFilter;
import io.github.wintocode.oxiwiggle.surface.MixerBank;
import io.github.wintocode.oxiwiggle.surface.RemotePage.PagePosition;
import io.github.wintocode.oxiwiggle.surface.RemotePage.Reading;

public final class BitwigMixerBank implements MixerBank {
    private static final int SEND_SLOTS = 8;
    private final TrackBank bank;
    private final Track[] tracks = new Track[COLUMN_COUNT];

    private final Parameter[] volumes = new Parameter[COLUMN_COUNT];
    private final Parameter[] pans = new Parameter[COLUMN_COUNT];
    private final Send[][] sends = new Send[COLUMN_COUNT][];
    private int requestedStart;
    private int selectedColumn = -1;
    private String selectedTrackId;

    public BitwigMixerBank(ControllerHost host) {
        bank = host.createTrackBank(COLUMN_COUNT, SEND_SLOTS, 0, true);
        bank.setContentFilter(TrackBankContentFilter.ALL_VISIBLE_CHANNELS);
        for (int i = 0; i < tracks.length; i++) {
            tracks[i] = bank.getItemAt(i);
            volumes[i] = tracks[i].volume();
            pans[i] = tracks[i].pan();
            SendBank sendBank = tracks[i].sendBank();
            sends[i] = new Send[sendBank.getSizeOfBank()];
            for (int j = 0; j < sends[i].length; j++) sends[i][j] = sendBank.getItemAt(j);
        }
    }

    public void observe(Runnable changed, Runnable targetChanged) {
        bank.scrollPosition().addValueObserver(ignored -> targetChanged.run());
        bank.itemCount().addValueObserver(ignored -> targetChanged.run());
        for (int i = 0; i < tracks.length; i++) {
            Track track = tracks[i];
            for (Send send : sends[i]) {
                send.exists().addValueObserver(ignored -> changed.run());
                send.isEnabled().markInterested();
                send.name().addValueObserver(ignored -> changed.run());
                send.value().addValueObserver(ignored -> changed.run());
                send.value().displayedValue().addValueObserver(ignored -> changed.run());
            }
            track.exists().addValueObserver(ignored -> targetChanged.run());
            track.name().addValueObserver(ignored -> changed.run());
            track.channelId().addValueObserver(ignored -> targetChanged.run());
            track.position().addValueObserver(ignored -> targetChanged.run());
            track.mute().addValueObserver(ignored -> changed.run());
            track.solo().addValueObserver(ignored -> changed.run());
            track.arm().addValueObserver(ignored -> changed.run());
            track.canHoldAudioData().addValueObserver(ignored -> changed.run());
            track.canHoldNoteData().addValueObserver(ignored -> changed.run());
            for (Parameter parameter : new Parameter[] {volumes[i], pans[i]}) {
                parameter.value().addValueObserver(ignored -> changed.run());
                parameter.value().displayedValue().addValueObserver(ignored -> changed.run());
            }
        }
    }

    public void showSelection(PagePosition position, String trackId) {
        requestedStart = MixerBank.windowStart(position.index(), position.count());
        selectedColumn = position.index() - requestedStart;
        selectedTrackId = trackId;
        if (bank.scrollPosition().get() != requestedStart) bank.scrollPosition().set(requestedStart);
    }

    @Override public Strip read(int column) {
        Track track = tracks[column];
        return track.exists().get() ? new Strip(true, track.name().get(), track.channelId().get())
                : new Strip(false, "", null);
    }

    private boolean trackReady(int column) {
        return bank.scrollPosition().get() == requestedStart && tracks[column].exists().get()
                && selectedColumn >= 0 && selectedColumn < tracks.length
                && selectedTrackId != null && !selectedTrackId.isBlank()
                && selectedTrackId.equals(tracks[selectedColumn].channelId().get());
    }

    private static boolean canArm(Track track) {
        return track.canHoldAudioData().get() || track.canHoldNoteData().get();
    }

    @Override public List<SendReading> sends(int column) {
        if (!trackReady(column)) return List.of();
        var readings = new ArrayList<SendReading>();
        for (int i = 0; i < sends[column].length; i++) {
            Send send = sends[column][i];
            if (!send.exists().get()) continue;
            var value = send.value();
            readings.add(new SendReading(i, send.name().get(), value.displayedValue().get(), value.get()));
        }
        return readings;
    }

    @Override public void incrementSend(int column, SendReading target, double delta) {
        if (!trackReady(column)) return;
        Send send = sends[column][target.index()];
        if (send.exists().get() && Objects.equals(send.name().get(), target.name())) adjustSend(send, delta);
    }

    static void adjustSend(Send send, double delta) {
        var enabled = send.isEnabled();
        if (!enabled.get()) {
            if (delta <= 0) return;
            enabled.set(true);
        }
        send.value().inc(delta);
    }

    @Override public Reading level(int column, boolean shifted) {
        if (!trackReady(column)) return null;
        var value = (shifted ? pans[column] : volumes[column]).value();
        return new Reading(shifted ? "Pan" : "Volume", true, value.get(), -1, value.displayedValue().get());
    }

    @Override public Switches switches(int column) {
        if (!trackReady(column)) return null;
        Track track = tracks[column];
        return new Switches(track.mute().get(), track.solo().get(), track.arm().get(), canArm(track));
    }

    @Override public void increment(int column, boolean shifted, double delta) {
        if (trackReady(column)) (shifted ? pans[column] : volumes[column]).value().inc(delta);
    }

    @Override public void toggle(int column, TrackSwitch control) {
        if (!trackReady(column)) return;
        Track track = tracks[column];
        switch (control) {
            case MUTE -> track.mute().toggle();
            case SOLO -> track.solo().toggleUsingPreferences(false);
            case ARM -> {
                if (canArm(track)) track.arm().toggle();
            }
        }
    }
}
