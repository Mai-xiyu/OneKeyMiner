package org.xiyu.onekeyminer.network;

import java.util.Objects;
import java.util.function.Consumer;

/** Client callback bridge that keeps shared protocol classes side-safe. */
public final class ClientPreferenceAckDispatcher {
    private static volatile Consumer<ClientPreferenceAck> handler;

    private ClientPreferenceAckDispatcher() {
    }

    public static void register(Consumer<ClientPreferenceAck> newHandler) {
        handler = Objects.requireNonNull(newHandler, "newHandler");
    }

    public static void dispatch(ClientPreferenceAck acknowledgement) {
        Consumer<ClientPreferenceAck> current = handler;
        if (current != null) {
            current.accept(Objects.requireNonNull(
                    acknowledgement,
                    "acknowledgement"
            ));
        }
    }
}
