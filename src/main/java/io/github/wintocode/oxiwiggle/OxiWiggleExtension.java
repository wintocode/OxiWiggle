package io.github.wintocode.oxiwiggle;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import io.github.wintocode.oxiwiggle.bitwig.BitwigRemotePage;
import io.github.wintocode.oxiwiggle.bitwig.BitwigMixerBank;
import io.github.wintocode.oxiwiggle.bitwig.BitwigTransport;
import io.github.wintocode.oxiwiggle.bitwig.BitwigScenes;
import io.github.wintocode.oxiwiggle.protocol.E16Protocol;
import io.github.wintocode.oxiwiggle.surface.RemoteSession;

public final class OxiWiggleExtension extends ControllerExtension {
    private RemoteSession session;
    protected OxiWiggleExtension(OxiWiggleExtensionDefinition definition, ControllerHost host) {
        super(definition, host);
    }

    @Override public void init() {
        var host = getHost();
        var midiIn = host.getMidiInPort(0);
        var midiOut = host.getMidiOutPort(0);
        var track = host.createCursorTrack(0, 0);
        var page = new BitwigRemotePage(host, track);
        var mixer = new BitwigMixerBank(host);
        var scenes = new BitwigScenes(host);
        var transport = new BitwigTransport(host);
        var activeSession = new RemoteSession(frame -> {
            E16Protocol.validateFrame(frame);
            midiOut.sendSysex(frame);
        }, host::scheduleTask, host::requestFlush, host::println, host::errorln, System::nanoTime, page, mixer, transport, scenes,
                getExtensionDefinition().getVersion());
        session = activeSession;
        mixer.observe(activeSession::onSlotChanged, activeSession::onMixerTargetChanged);
        scenes.observe(activeSession::onLauncherChanged);
        transport.observe(activeSession::onTransportChanged);
        page.observe(activeSession::onSlotChanged, activeSession::onSelectionChanged, mixer::showSelection);
        midiIn.setMidiCallback(activeSession::onMidi);
        midiIn.setSysexCallback(activeSession::onSysex);
        host.println("OxiWiggle " + getExtensionDefinition().getVersion() + " init; Bitwig "
                + host.getHostVersion() + ", API " + host.getHostApiVersion());
        activeSession.start();
    }

    @Override public void flush() { if (session != null) session.flush(); }
    @Override public void exit() {
        if (session != null) session.stop();
        getHost().println("OxiWiggle " + getExtensionDefinition().getVersion() + " exit");
    }
}
