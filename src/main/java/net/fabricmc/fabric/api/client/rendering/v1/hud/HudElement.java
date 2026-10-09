/*
 * Reimplementation of Fabric API's HUD element callback, kept under its original package name.
 *
 * `simplewaypoints` registers one of these to draw waypoint markers. NeoForge renders HUD layers
 * through its own layered pipeline, so `HudElementRegistry` below adapts this callback onto a
 * NeoForge HUD layer.
 */

package net.fabricmc.fabric.api.client.rendering.v1.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A single HUD draw callback.
 */
@FunctionalInterface
public interface HudElement {
    /**
     * Extracts this element's render state.
     *
     * @param graphics the GUI graphics used to emit draw commands
     * @param deltaTracker frame timing information
     */
    void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);
}
