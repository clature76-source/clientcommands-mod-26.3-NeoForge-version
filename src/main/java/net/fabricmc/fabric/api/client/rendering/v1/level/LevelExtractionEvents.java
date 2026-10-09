/*
 * Reimplementation of Fabric API's level extraction events, kept under its original package name.
 *
 * `simplewaypoints` registers an `END_EXTRACTION` listener to pull waypoint data out of the level
 * each frame. NeoForge's equivalent hook is `ExtractLevelRenderStateEvent`, so this type installs a
 * NeoForge listener on first registration and fans the event out to Fabric-shaped listeners.
 *
 * The port's own extraction code uses
 * `net.earthcomputer.clientcommands.compat.fabric.LevelExtractionEvents` instead; both end up on the
 * same NeoForge event.
 */

package net.fabricmc.fabric.api.client.rendering.v1.level;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Fabric's level-extraction hook points, backed by NeoForge's extraction event.
 */
public final class LevelExtractionEvents {
    private LevelExtractionEvents() {
    }

    /** Fired once per frame after level data has been extracted into the render state. */
    @FunctionalInterface
    public interface EndExtraction {
        void endExtraction(LevelExtractionContext context);
    }

    public static final Event<EndExtraction> END_EXTRACTION =
        EventFactory.createArrayBacked(EndExtraction.class, listeners -> context -> {
            for (EndExtraction listener : listeners) {
                listener.endExtraction(context);
            }
        });

    static {
        NeoForge.EVENT_BUS.addListener(LevelExtractionEvents::onExtractLevelRenderState);
    }

    private static void onExtractLevelRenderState(ExtractLevelRenderStateEvent event) {
        END_EXTRACTION.invoker().endExtraction(
            new ContextImpl(event.getDeltaTracker(), event.getCamera(), event.getRenderState()));
    }

    /** Adapts NeoForge's event to the Fabric-shaped context listeners expect. */
    private record ContextImpl(DeltaTracker deltaTracker, Camera camera,
                               LevelRenderState levelState) implements LevelExtractionContext {
    }
}
