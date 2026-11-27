package io.github.wintocode.oxiwiggle.bitwig;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.Transport;
import java.util.ArrayList;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.TrackBankContentFilter;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import io.github.wintocode.oxiwiggle.surface.SceneWindow;

public final class BitwigScenes implements SceneWindow {
    private final ControllerHost host;
    private final SceneBank bank;
    private final Transport transport;

    private static final int RECORD_TRACKS = 128;
    private final TrackBank tracks;
    private int recordAttempt;

    public BitwigScenes(ControllerHost host, Transport transport) {
        this.transport = transport;
        this.host = host;
        tracks = host.getProject().getRootTrackGroup().createMainTrackBank("launcher-record", "Launcher recording",
                RECORD_TRACKS, 0, SIZE, TrackBankContentFilter.ALL_CHANNELS, false);
        bank = tracks.sceneBank();
    }

    public void observe(Runnable changed) {
        tracks.itemCount().markInterested();
        for (int i = 0; i < RECORD_TRACKS; i++) {
            var track = tracks.getItemAt(i);
            track.exists().markInterested();
            track.arm().markInterested();
            track.name().markInterested();
            for (int j = 0; j < SIZE; j++) {
                var slot = track.clipLauncherSlotBank().getItemAt(j);
                slot.exists().markInterested();
                slot.hasContent().markInterested();
                slot.isRecordingQueued().markInterested();
                slot.isRecording().markInterested();
                slot.sceneIndex().markInterested();
            }
        }
        bank.itemCount().addValueObserver(ignored -> changed.run());
        for (int slot = 0; slot < SIZE; slot++) {
            Scene scene = bank.getItemAt(slot);
            scene.exists().addValueObserver(ignored -> changed.run());
            scene.sceneIndex().addValueObserver(ignored -> changed.run());
            scene.name().addValueObserver(ignored -> changed.run());
        }
    }

    public int count() { return bank.itemCount().get(); }
    public void scrollTo(int first) { bank.scrollPosition().set(first); }

    // The host can clamp the last window, moving scene 9 away from its first slot.
    private int slotFor(int index) {
        for (int slot = 0; slot < SIZE; slot++) {
            Scene scene = bank.getItemAt(slot);
            if (scene.exists().get() && scene.sceneIndex().get() == index) return slot;
        }
        return -1;
    }

    public String name(int index) {
        int slot = slotFor(index);
        return slot < 0 ? null : bank.getItemAt(slot).name().get();
    }

    public String record(int index) {
        String attempt = "R" + ++recordAttempt;
        int column = slotFor(index);
        host.println(attempt + " Launcher record: scene=" + (index + 1) + " bankSlot=" + (column + 1)
                + " trackCount=" + tracks.itemCount().get());
        if (column < 0) return report(attempt, "Scene unavailable");
        int count = tracks.itemCount().get();
        if (count > RECORD_TRACKS) return report(attempt, "Recording limit: 128 tracks");
        var targets = new ArrayList<ClipLauncherSlot>();
        boolean armed = false;
        boolean recordingQueued = false;
        for (int i = 0; i < count; i++) {
            var track = tracks.getItemAt(i);
            if (!track.exists().get()) return report(attempt, "Tracks updating");
            var slot = track.clipLauncherSlotBank().getItemAt(column);
            boolean trackArmed = track.arm().get();
            boolean content = slot.hasContent().get();
            boolean queued = slot.isRecordingQueued().get();
            boolean recording = slot.isRecording().get();
            host.println(attempt + " Launcher record: track=" + (i + 1) + " " + track.name().get()
                    + " armed=" + trackArmed + " slotScene=" + (slot.sceneIndex().get() + 1)
                    + " exists=" + slot.exists().get() + " content=" + content
                    + " queued=" + queued + " recording=" + recording);
            if (!trackArmed) continue;
            armed = true;
            if (!slot.exists().get() || slot.sceneIndex().get() != index) return report(attempt, "Scene updating");
            recordingQueued |= queued;
            if (!content && !queued && !recording) {
                targets.add(slot);
            }
        }
        if (!armed) return report(attempt, "No armed tracks");
        if (targets.isEmpty() && !recordingQueued) return report(attempt, "No empty armed slots");
        if (!targets.isEmpty()) {
            host.println(attempt + " calling record(), targets=" + targets.size());
            for (var slot : targets) slot.record();
        }
        if (!transport.isPlaying().get()) transport.play();
        return report(attempt, targets.isEmpty() ? "Recording queued" : "Record requested");
    }

    private String report(String attempt, String result) {
        String notice = attempt + " " + result;
        host.println("Launcher OLED notice: " + notice);
        return notice;
    }

    public void launch(int index) {
        int slot = slotFor(index);
        if (slot >= 0) bank.launch(slot);
    }
}
