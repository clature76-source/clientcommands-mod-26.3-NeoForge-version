package net.earthcomputer.clientcommands.compat.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;

/**
 * Counterpart to Fabric API's
 * {@code net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents}.
 *
 * <p>Delegates to NeoForge's {@link SubmitCustomGeometryEvent}, which is where arbitrary geometry is
 * submitted to the vanilla {@link SubmitNodeCollector} pipeline. This replaces Fabric's
 * {@code COLLECT_SUBMITS} hook.
 */
public final class LevelRenderEvents {
    private LevelRenderEvents() {
    }

    public static final Registration COLLECT_SUBMITS = new Registration();

    @FunctionalInterface
    public interface Listener {
        void onCollectSubmits(LevelRenderContext context);
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
            NeoForge.EVENT_BUS.addListener(LevelRenderEvents::onEvent);
        }
    }

    private static void onEvent(SubmitCustomGeometryEvent event) {
        ContextImpl context = new ContextImpl(event.getSubmitNodeCollector(), event.getPoseStack(), event.getLevelRenderState());
        for (Listener listener : COLLECT_SUBMITS.listeners) {
            listener.onCollectSubmits(context);
        }
    }

    /** Adapts NeoForge's event to the Fabric-shaped context the ported shapes expect. */
    private record ContextImpl(SubmitNodeCollector submitNodeCollector, PoseStack poseStack,
                               LevelRenderState levelState) implements LevelRenderContext {
    }
}
