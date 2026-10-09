package net.earthcomputer.clientcommands.compat.fabric;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Counterpart to Fabric API's {@code net.fabricmc.fabric.api.client.screen.v1.ScreenEvents},
 * covering the per-screen {@code afterExtract} and {@code afterTick} hooks used by the port.
 *
 * <p>Fabric's shape is {@code ScreenEvents.afterExtract(screen).register(callback)}: the first call
 * binds the event to a screen and returns a registrar. NeoForge's screen events are global, so the
 * port records which screens have been armed and dispatches only to those, preserving the per-screen
 * semantics the relog flow relies on.
 */
public final class ScreenEvents {
    private ScreenEvents() {
    }

    private static final Map<Screen, List<AfterExtract>> EXTRACT = new IdentityHashMap<>();
    private static final Map<Screen, List<AfterTick>> TICK = new IdentityHashMap<>();

    @FunctionalInterface
    public interface AfterExtract {
        void afterExtract(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress);
    }

    @FunctionalInterface
    public interface AfterTick {
        void afterTick(Screen screen);
    }

    /** Binds the render event to a screen, mirroring Fabric's fluent form. */
    public static Registrar<AfterExtract> afterExtract(Screen screen) {
        return callback -> {
            EXTRACT.computeIfAbsent(screen, s -> new ArrayList<>()).add(callback);
            ScreenEventDispatcher.install();
        };
    }

    /** Binds the tick event to a screen, mirroring Fabric's fluent form. */
    public static Registrar<AfterTick> afterTick(Screen screen) {
        return callback -> {
            TICK.computeIfAbsent(screen, s -> new ArrayList<>()).add(callback);
            ScreenEventDispatcher.install();
        };
    }

    /** Callback sink returned by {@link #afterExtract} / {@link #afterTick}. */
    @FunctionalInterface
    public interface Registrar<T> {
        void register(T callback);
    }

    static List<AfterExtract> extractCallbacks(Screen screen) {
        return EXTRACT.getOrDefault(screen, List.of());
    }

    static List<AfterTick> tickCallbacks(Screen screen) {
        return TICK.getOrDefault(screen, List.of());
    }

    /** Drops the callbacks armed for a screen once it closes, mirroring Fabric's lifetime. */
    public static void clear(Screen screen) {
        EXTRACT.remove(screen);
        TICK.remove(screen);
    }
}
