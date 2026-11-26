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
        ControllerHost host = stub(ControllerHost.class, (proxy, method, args) -> {
            Checks.that(method.getName().equals("createSceneBank") && (int) args[0] == 8, "scene bank size");
            return bank;
        });
        var scenes = new BitwigScenes(host);
        Checks.that("Outro".equals(scenes.name(10)) && scenes.name(11) == null, "partial page names");
        scenes.launch(8);
        scenes.launch(10);
        scenes.launch(11);
        Checks.that(launched.equals(List.of(5, 7)), "launch uses the observed bank slot");
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
