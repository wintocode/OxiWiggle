package io.github.wintocode.oxiwiggle;

import java.util.UUID;
import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

public final class OxiWiggleExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID ID = UUID.fromString("7bd8998f-1030-4da1-991b-b86bb8a94357");

    @Override public String getName() { return "OxiWiggle"; }
    @Override public String getAuthor() { return "wintocode"; }
    @Override public String getVersion() { return "0.0.58-record-play"; }
    @Override public UUID getId() { return ID; }
    @Override public String getHardwareVendor() { return "OXI Instruments"; }
    @Override public String getHardwareModel() { return "E16 (OxiWiggle)"; }
    @Override public int getRequiredAPIVersion() { return 25; }
    @Override public int getNumMidiInPorts() { return 1; }
    @Override public int getNumMidiOutPorts() { return 1; }
    @Override public void listAutoDetectionMidiPortNames(AutoDetectionMidiPortNamesList list, PlatformType platform) {
        if (platform == PlatformType.MAC) {
            list.add(new String[] {"OXI E16 Port 1"}, new String[] {"OXI E16 Port 1"});
        }
    }
    @Override public ControllerExtension createInstance(ControllerHost host) {
        return new OxiWiggleExtension(this, host);
    }
}
