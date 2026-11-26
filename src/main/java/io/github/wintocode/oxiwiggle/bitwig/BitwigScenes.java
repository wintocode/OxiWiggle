package io.github.wintocode.oxiwiggle.bitwig;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import io.github.wintocode.oxiwiggle.surface.SceneWindow;

public final class BitwigScenes implements SceneWindow {
    private final SceneBank bank;

    public BitwigScenes(ControllerHost host) { bank = host.createSceneBank(SIZE); }

    public void observe(Runnable changed) {
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

    public void launch(int index) {
        int slot = slotFor(index);
        if (slot >= 0) bank.launch(slot);
    }
}
