package net.earthcomputer.clientcommands.compat.fabric;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.level.LevelRenderState;

/**
 * Counterpart to Fabric API's
 * {@code net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext}.
 *
 * <p>Backed by NeoForge's {@code ExtractLevelRenderStateEvent}, which is the equivalent extraction
 * hook. NeoForge fires it once per frame, before submits are collected, and hands over the same
 * ingredients Fabric's context carries: a delta tracker, a camera and the vanilla render state.
 */
public interface LevelExtractionContext {

    /** Frame timing information, used for interpolating entity movement. */
    DeltaTracker deltaTracker();

    /** The active camera, used to convert world positions into camera-relative ones. */
    Camera camera();

    /**
     * The vanilla level render state. Fabric additionally allowed arbitrary keyed data to be attached
     * here; the port instead keeps that data in {@link RenderStateDataStore}, because NeoForge's
     * {@link LevelRenderState} has no generic data bag.
     */
    LevelRenderState levelState();
}
