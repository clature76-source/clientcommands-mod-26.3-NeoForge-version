package net.earthcomputer.clientcommands.compat.fabric;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;

/**
 * Counterpart to Fabric API's
 * {@code net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents}.
 *
 * <p>Delegates to NeoForge's {@link ExtractLevelRenderStateEvent}. NeoForge fires a single event
 * rather than Fabric's separate start/end hooks; the port only needs the end-of-extraction timing,
 * so {@code END_EXTRACTION} maps onto it directly.
 */
public final class LevelExtractionEvents {
    private LevelExtractionEvents() {
    }

    public static final Registration END_EXTRACTION = new Registration();

    @FunctionalInterface
    public interface Listener {
        void onExtract(LevelExtractionContext context);
    }

    public static final class Registration {
        private final List<Listener> listeners = new ArrayList<>();
        private boolean installed;

        private Registration() {
        }

        public void register(Listener listener) {
            listeners.add(listener);
            install();
        }

        private void install() {
            if (installed) {
                return;
            }
            installed = true;
            NeoForge.EVENT_BUS.addListener(LevelExtractionEvents::onEvent);
        }
    }

    private static void onEvent(ExtractLevelRenderStateEvent event) {
        // The store is per-frame; reset it before each extraction pass.
        RenderStateDataKey.store().clear();

        ContextImpl context = new ContextImpl(event.getDeltaTracker(), event.getCamera(), event.getRenderState());
        for (Listener listener : END_EXTRACTION.listeners) {
            listener.onExtract(context);
        }
    }

    /** Adapts NeoForge's event to the Fabric-shaped context the ported shapes expect. */
    private record ContextImpl(DeltaTracker deltaTracker, Camera camera,
                               LevelRenderState levelState) implements LevelExtractionContext {
    }
}
