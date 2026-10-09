/*
 * Reimplementation of Fabric API's level render events, kept under its original package name.
 *
 * `simplewaypoints` registers a `COLLECT_SUBMITS` listener. NeoForge exposes the same hook as
 * `SubmitCustomGeometryEvent`, so this type installs a NeoForge listener on first registration and
 * fans the event out to the registered Fabric-shaped listeners. That lets the third-party jar run
 * unmodified.
 */

package net.fabricmc.fabric.api.client.rendering.v1.level;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Fabric's level-render hook points, backed by NeoForge's custom-geometry event.
 */
public final class LevelRenderEvents {
    private LevelRenderEvents() {
    }

    /** Fired when custom geometry may be added to the level's submit pipeline. */
    @FunctionalInterface
    public interface CollectSubmits {
        void collectSubmits(LevelRenderContext context);
    }

    public static final Event<CollectSubmits> COLLECT_SUBMITS =
        EventFactory.createArrayBacked(CollectSubmits.class, listeners -> context -> {
            for (CollectSubmits listener : listeners) {
                listener.collectSubmits(context);
            }
        });

    static {
        // Bridge to NeoForge the first time this class is touched, which happens when a library
        // registers against COLLECT_SUBMITS during its own event registration.
        NeoForge.EVENT_BUS.addListener(LevelRenderEvents::onSubmitCustomGeometry);
    }

    private static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        COLLECT_SUBMITS.invoker().collectSubmits(
            new ContextImpl(event.getSubmitNodeCollector(), event.getPoseStack(), event.getLevelRenderState()));
    }

    /** Adapts NeoForge's event to the Fabric-shaped context listeners expect. */
    private record ContextImpl(SubmitNodeCollector submitNodeCollector, PoseStack poseStack,
                               LevelRenderState levelState) implements LevelRenderContext {
    }
}
