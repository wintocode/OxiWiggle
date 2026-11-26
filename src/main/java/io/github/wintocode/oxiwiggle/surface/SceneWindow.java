package io.github.wintocode.oxiwiggle.surface;

public interface SceneWindow {
    int SIZE = 8;
    int count();
    void scrollTo(int first);
    /** Returns null while the scene is absent from the observed window. */
    String name(int sceneIndex);
    void launch(int sceneIndex);
}
