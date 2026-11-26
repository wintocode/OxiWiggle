package io.github.wintocode.oxiwiggle.bitwig;

import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import io.github.wintocode.oxiwiggle.Checks;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public final class BitwigMixerBankTest {
    public static void run() {
        var actions = new ArrayList<String>();
        var enabled = new boolean[] {false};
        var activation = stub(SettableBooleanValue.class, (proxy, method, args) -> switch (method.getName()) {
            case "get" -> enabled[0];
            case "set" -> {
                enabled[0] = (boolean) args[0];
                actions.add("enable");
                yield null;
            }
            default -> throw new AssertionError(method);
        });
        var value = stub(SettableRangedValue.class, (proxy, method, args) -> {
            if (!method.getName().equals("inc")) throw new AssertionError(method);
            if (enabled[0]) actions.add("increment " + args[0]);
            return null;
        });
        var send = stub(Send.class, (proxy, method, args) -> switch (method.getName()) {
            case "isEnabled" -> activation;
            case "value" -> value;
            default -> throw new AssertionError(method);
        });

        BitwigMixerBank.adjustSend(send, -0.125);
        Checks.that(actions.isEmpty(), "turning a disabled send down leaves it disabled");
        BitwigMixerBank.adjustSend(send, 0.125);
        BitwigMixerBank.adjustSend(send, -0.25);
        Checks.that(actions.equals(List.of("enable", "increment 0.125", "increment -0.25")),
                "turning a disabled send up enables it before writing the level");
    }

    private static <T> T stub(Class<T> type, InvocationHandler implementation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, implementation));
    }
}
