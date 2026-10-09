package net.earthcomputer.clientcommands.compat.fabric;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Minimal reimplementation of Fabric API's {@code net.fabricmc.fabric.api.event.Event} and
 * {@code EventFactory#createArrayBacked}, which the ported code uses to define its own events.
 *
 * <p>Fabric's implementation is a small array-backed listener list with a combiner that fans out to
 * each listener; that is all the mod relies on, so this keeps the same semantics without pulling in
 * the Fabric API.
 */
public final class Event<T> {
    private final Class<T> type;
    private final Function<T[], T> combiner;
    private final List<T> listeners = new ArrayList<>();
    private T invoker;

    private Event(Class<T> type, Function<T[], T> combiner) {
        this.type = type;
        this.combiner = combiner;
        rebuild();
    }

    public static <T> Event<T> createArrayBacked(Class<T> type, Function<T[], T> combiner) {
        return new Event<>(type, combiner);
    }

    /** Fabric's two-argument overload: an empty-list default invoker plus the combiner. */
    public static <T> Event<T> createArrayBacked(Class<T> type, T emptyInvoker, Function<T[], T> combiner) {
        return new Event<>(type, combiner);
    }

    public void register(T listener) {
        listeners.add(listener);
        rebuild();
    }

    public T invoker() {
        return invoker;
    }

    private void rebuild() {
        // The combiner takes a T[], so the listeners must be copied into an array of the listener
        // type. `listeners.toArray()` would hand back an Object[], which cannot be cast to T[] --
        // hence the component type is taken from the Class passed to createArrayBacked.
        T[] array = listeners.toArray((T[]) java.lang.reflect.Array.newInstance(type, listeners.size()));
        invoker = combiner.apply(array);
    }
}
