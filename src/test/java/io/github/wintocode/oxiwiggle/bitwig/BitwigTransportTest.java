package io.github.wintocode.oxiwiggle.bitwig;

import com.bitwig.extension.controller.api.ControllerHost;
import io.github.wintocode.oxiwiggle.Checks;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BitwigTransportTest {
    private final List<String> writes = new ArrayList<>();

    private Object api(Class<?> type, String path) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            String call = path + "." + method.getName();
            if (method.getReturnType().isInterface()) return api(method.getReturnType(), call);
            if (method.getReturnType() == void.class) {
                writes.add(call + (args == null ? "" : Arrays.toString(args)));
                return null;
            }
            return switch (call) {
                case ".createTransport.timeSignature.numerator.get" -> 6;
                case ".createTransport.timeSignature.denominator.get" -> 8;
                case ".createTransport.arrangerLoopStart.get" -> 1.0;
                case ".createTransport.arrangerLoopDuration.get" -> 2.0;
                default -> throw new AssertionError(call);
            };
        });
    }

    public static void run() {
        var test = new BitwigTransportTest();
        var controls = new BitwigTransport((ControllerHost) test.api(ControllerHost.class, ""));
        verifyPlaybackAndTempo(test, controls);
        controls.turn(5, 8, false);
        controls.turn(5, -1, true);
        controls.turn(7, -1, false);
        controls.turn(8, -1, false);
        controls.turn(8, 1, false);
        controls.turn(6, -1, false);
        controls.turn(6, 1, false);
        controls.click(3, false);
        controls.click(11, true);
        controls.click(12, true);
        Checks.that(test.writes.equals(List.of(
                ".createTransport.incPosition[3.0, false]", ".createTransport.incPosition[-0.5, false]",
                ".createTransport.arrangerLoopStart.inc[-1.0]", ".createTransport.arrangerLoopDuration.inc[3.0]",
                ".createTransport.jumpToPreviousCueMarker", ".createTransport.jumpToNextCueMarker",
                ".createTransport.record", ".createTransport.isPunchInEnabled.toggle",
                ".createTransport.isPunchOutEnabled.toggle")), "Arranger edits in 6/8: " + test.writes);
    }

    private static void verifyPlaybackAndTempo(BitwigTransportTest test, BitwigTransport controls) {
        controls.click(1, false);
        controls.click(2, false);
        controls.click(4, false);
        controls.click(6, false);
        controls.click(7, false);
        controls.click(9, false);
        controls.click(10, false);
        controls.click(11, false);
        controls.click(12, false);
        controls.turn(9, 8, false);
        controls.turn(9, -8, false);
        controls.turn(10, -8, false);
        controls.turn(10, 0, false);
        Checks.that(test.writes.equals(List.of(
                ".createTransport.play", ".createTransport.stop", ".createTransport.restart",
                ".createTransport.addCueMarkerAtPlaybackPosition", ".createTransport.isArrangerLoopEnabled.toggle",
                ".createTransport.tapTempo", ".createTransport.isMetronomeEnabled.toggle",
                ".createTransport.isArrangerOverdubEnabled.toggle", ".createTransport.isArrangerAutomationWriteEnabled.toggle",
                ".createTransport.tempo.value.incRaw[1.0]", ".createTransport.tempo.value.incRaw[-1.0]",
                ".createTransport.metronomeVolume.inc[-0.0625]")), "Transport actions: " + test.writes);
        test.writes.clear();
    }
}
