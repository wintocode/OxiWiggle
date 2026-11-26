package io.github.wintocode.oxiwiggle;

import io.github.wintocode.oxiwiggle.protocol.E16ProtocolTest;
import io.github.wintocode.oxiwiggle.protocol.RelativeEncoderTest;
import io.github.wintocode.oxiwiggle.surface.RemoteSessionTest;
import io.github.wintocode.oxiwiggle.surface.TransportSessionTest;
import io.github.wintocode.oxiwiggle.surface.LauncherTest;
import io.github.wintocode.oxiwiggle.bitwig.BitwigScenesTest;
import io.github.wintocode.oxiwiggle.bitwig.BitwigMixerBankTest;
import io.github.wintocode.oxiwiggle.bitwig.BitwigTransportTest;
import io.github.wintocode.oxiwiggle.protocol.SevenBitPackingTest;
import io.github.wintocode.oxiwiggle.display.NativeLabelsTest;
import io.github.wintocode.oxiwiggle.display.ControlScreenTest;

public final class AllTests {
    public static void main(String[] args) throws Exception {
        String suite = args.length == 0 ? "all" : args[0];
        if (args.length > 1 || !(suite.equals("all") || suite.equals("protocol") || suite.equals("metadata") || suite.equals("session") || suite.equals("display"))) {
            throw new IllegalArgumentException("unknown test suite");
        }
        if (suite.equals("all") || suite.equals("protocol")) {
            E16ProtocolTest.run();
            SevenBitPackingTest.run();
            RelativeEncoderTest.run();
            System.out.println("PASS protocol");
        }
        if (suite.equals("all") || suite.equals("metadata")) { ExtensionDefinitionTest.run(); System.out.println("PASS metadata"); }
        if (suite.equals("all") || suite.equals("session")) { RemoteSessionTest.run(); TransportSessionTest.run(); LauncherTest.run(); BitwigScenesTest.run(); BitwigMixerBankTest.run(); BitwigTransportTest.run(); System.out.println("PASS session"); }
        if (suite.equals("all") || suite.equals("display")) { NativeLabelsTest.run(); ControlScreenTest.run(); System.out.println("PASS display"); }
    }
}
