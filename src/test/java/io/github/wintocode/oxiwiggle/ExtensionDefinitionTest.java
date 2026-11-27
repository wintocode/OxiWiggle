package io.github.wintocode.oxiwiggle;

import com.bitwig.extension.api.PlatformType;
import java.util.Arrays;

public final class ExtensionDefinitionTest {
    public static void run() {
        var definition = new OxiWiggleExtensionDefinition();
        Checks.that(definition.getName().equals("OxiWiggle"), "name");
        Checks.that(definition.getAuthor().equals("wintocode"), "author");
        Checks.that(definition.getHardwareVendor().equals("OXI Instruments"), "vendor");
        Checks.that(definition.getHardwareModel().equals("E16 (OxiWiggle)"), "model");
        Checks.that(definition.getVersion().equals("0.0.56-about-click"), "version");
        Checks.equal(25, definition.getRequiredAPIVersion());
        Checks.equal(1, definition.getNumMidiInPorts());
        Checks.equal(1, definition.getNumMidiOutPorts());
        var macPorts = definition.getAutoDetectionMidiPortNamesList(PlatformType.MAC);
        Checks.equal(1, macPorts.getCount());
        var pair = macPorts.getPortNamesAt(0);
        String[] expected = {"OXI E16 Port 1"};
        Checks.that(Arrays.equals(expected, pair.getInputNames()), "macOS Port 1 input");
        Checks.that(Arrays.equals(expected, pair.getOutputNames()), "macOS Port 1 output");
        Checks.equal(0, definition.getAutoDetectionMidiPortNamesList(PlatformType.WINDOWS).getCount());
        Checks.equal(0, definition.getAutoDetectionMidiPortNamesList(PlatformType.LINUX).getCount());
        Checks.that(definition.getId().toString().equals("7bd8998f-1030-4da1-991b-b86bb8a94357"), "stable UUID");
    }
}
