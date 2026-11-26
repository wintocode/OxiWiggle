package io.github.wintocode.oxiwiggle.display;

import io.github.wintocode.oxiwiggle.surface.TransportControls;

public final class TransportScreen {
    private TransportScreen() {}

    public static byte[] render(TransportControls.Snapshot snapshot, boolean loopDetail) {
        byte[] pixels = new byte[128 * 64 / 8];
        String state = snapshot.recording() ? "REC" : snapshot.playing() ? "PLAY" : "STOP";
        PixelText.draw(pixels, "TRANSPORT", 4, 4, 9);
        PixelText.draw(pixels, state, 98, 4, 4);
        for (int x = 4; x < 124; x++) PixelText.set(pixels, x, 15);
        if (loopDetail) {
            PixelText.draw(pixels, "Loop start", 4, 19, 20);
            PixelText.draw(pixels, snapshot.loopStart(), 4, 29, 20);
            PixelText.draw(pixels, "Loop length", 4, 42, 20);
            PixelText.draw(pixels, snapshot.loopLength(), 4, 52, 20);
        } else {
            String position = snapshot.position();
            int scale = position.length() <= 10 ? 2 : 1;
            PixelText.wrap(pixels, position, 4, 22, 20 / scale, 1, scale);
            PixelText.draw(pixels, snapshot.tempo(), 4, 48, 20);
        }
        return pixels;
    }
}
