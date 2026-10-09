package net.earthcomputer.clientcommands.compat.fabric;

import java.util.function.Function;

/** Counterpart to Fabric API's {@code net.fabricmc.fabric.api.event.EventFactory}. */
public final class EventFactory {
    private EventFactory() {
    }

    public static <T> Event<T> createArrayBacked(Class<T> type, Function<T[], T> combiner) {
        return Event.createArrayBacked(type, combiner);
    }

    public static <T> Event<T> createArrayBacked(Class<T> type, T emptyInvoker, Function<T[], T> combiner) {
        return Event.createArrayBacked(type, emptyInvoker, combiner);
    }
}
