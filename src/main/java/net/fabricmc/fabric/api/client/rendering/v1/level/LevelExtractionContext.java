/*
 * Reimplementation of Fabric API's level extraction context, kept under its original package name.
 *
 * `simplewaypoints` implements a listener against this type. NeoForge supplies the same ingredients
 * through `ExtractLevelRenderStateEvent`; the port's equivalent is
 * `net.earthcomputer.clientcommands.compat.fabric.LevelExtractionContext`, and this shadow type
 * exists so the third-party jar links.
 */

package net.fabricmc.fabric.api.client.rendering.v1.level;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.level.LevelRenderState;

/**
 * Per-frame extraction context handed to {@code LevelExtractionEvents} listeners.
 */
public interface LevelExtractionContext {

    /** Frame timing information, used for interpolating entity movement. */
    DeltaTracker deltaTracker();

    /** The active camera, used to convert world positions into camera-relative ones. */
    Camera camera();

    /** The vanilla level render state. */
    LevelRenderState levelState();
}
