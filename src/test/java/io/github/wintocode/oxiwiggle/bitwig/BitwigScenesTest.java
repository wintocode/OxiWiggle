package io.github.wintocode.oxiwiggle.bitwig;

import com.bitwig.extension.controller.api.*;
import io.github.wintocode.oxiwiggle.Checks;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public final class BitwigScenesTest {
    public static void run() {
        var launched = new ArrayList<Integer>();
        // Eleven scenes, with the final eight-slot host window starting at scene 4.
        SceneBank bank = stub(SceneBank.class, (proxy, method, args) -> switch (method.getName()) {
            case "getItemAt" -> scene(3 + (int) args[0]);
            case "launch" -> launched.add((int) args[0]);
            default -> throw new AssertionError(method);
        });
        var recorded = new ArrayList<Integer>();
        TrackBank tracks = stub(TrackBank.class, (proxy, method, args) -> switch (method.getName()) {
            case "sceneBank" -> bank;
            case "itemCount" -> stub(IntegerValue.class, (p, m, a) -> 4);
            case "getItemAt" -> recordingTrack((int) args[0], recorded);
            default -> throw new AssertionError(method);
        });
        Track root = stub(Track.class, (p, m, a) -> tracks);
        Project project = stub(Project.class, (p, m, a) -> root);
        var playRequests = new ArrayList<List<Integer>>();
        boolean[] playing = {false};
        Transport transport = stub(Transport.class, (p, method, args) -> switch (method.getName()) {
            case "isPlaying" -> stub(SettableBooleanValue.class, (v, m, a) -> playing[0]);
            case "play" -> playRequests.add(List.copyOf(recorded));
            default -> throw new AssertionError(method);
        });
        ControllerHost host = stub(ControllerHost.class, (p, m, a) -> project);
        var scenes = new BitwigScenes(host, transport);
        Checks.that("Outro".equals(scenes.name(10)) && scenes.name(11) == null, "partial page names");
        Checks.that(scenes.record(8).equals("R1 Record requested") && recorded.equals(List.of(0, 3)), "record empty armed slots");
        Checks.that(scenes.record(11).equals("R2 Scene unavailable"), "missing scene");
        Checks.that(scenes.record(8).equals("R3 Recording queued") && recorded.equals(List.of(0, 3)), "queued slots are reported once without re-recording");
        recorded.remove(Integer.valueOf(3));
        Checks.that(scenes.record(8).equals("R4 Record requested") && recorded.equals(List.of(0, 3)), "queued slot leaves another empty armed slot eligible");
        Checks.that(!playRequests.isEmpty() && playRequests.get(0).equals(List.of(0, 3)), "queue both recordings before starting playback");
        playing[0] = true;
        int previous = playRequests.size();
        recorded.clear();
        scenes.record(8);
        Checks.that(playRequests.size() == previous, "running playback continues");
        playing[0] = false;
        scenes.record(11);
        Checks.that(playRequests.size() == previous, "missing scene leaves transport stopped");
        scenes.record(8);
        Checks.that(playRequests.size() == previous + 1, "start playback for a previously queued recording");
        scenes.launch(8);
        scenes.launch(10);
        scenes.launch(11);
        Checks.that(launched.equals(List.of(5, 7)), "launch uses the observed bank slot");
    }

    private static Track recordingTrack(int track, List<Integer> recorded) {
        var slot = stub(ClipLauncherSlot.class, (proxy, method, args) -> switch (method.getName()) {
            case "exists" -> flag(true);
            case "hasContent" -> flag(track == 1);
            case "isRecording" -> flag(false);
            case "isRecordingQueued" -> flag(recorded.contains(track));
            case "sceneIndex" -> stub(IntegerValue.class, (p, m, a) -> 8);
            case "record" -> recorded.add(track);
            default -> throw new AssertionError(method);
        });
        var slots = stub(ClipLauncherSlotBank.class, (p, m, a) -> {
            Checks.that((int) a[0] == 5, "record uses observed scene offset");
            return slot;
        });
        return stub(Track.class, (proxy, method, args) -> switch (method.getName()) {
            case "exists" -> flag(true);
            case "name" -> stub(SettableStringValue.class, (p, m, a) -> "Track " + track);
            case "arm" -> stub(SettableBooleanValue.class, (p, m, a) -> track != 2);
            case "clipLauncherSlotBank" -> slots;
            default -> throw new AssertionError(method);
        });
    }

    private static BooleanValue flag(boolean value) {
        return stub(BooleanValue.class, (p, m, a) -> value);
    }

    private static Scene scene(int index) {
        return stub(Scene.class, (proxy, method, args) -> switch (method.getName()) {
            case "exists" -> stub(BooleanValue.class, (p, m, a) -> true);
            case "sceneIndex" -> stub(IntegerValue.class, (p, m, a) -> index);
            case "name" -> stub(SettableStringValue.class, (p, m, a) -> index == 10 ? "Outro" : "Verse");
            default -> throw new AssertionError(method);
        });
    }

    private static <T> T stub(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
