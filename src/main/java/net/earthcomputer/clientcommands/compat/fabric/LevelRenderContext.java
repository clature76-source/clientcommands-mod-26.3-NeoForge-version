package net.earthcomputer.clientcommands.compat.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;

/**
 * Counterpart to Fabric API's
 * {@code net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext}.
 *
 * <p>Backed by NeoForge's {@code SubmitCustomGeometryEvent}, which provides the submit node
 * collector, the pose stack and the vanilla level render state for the current frame.
 */
public interface LevelRenderContext {

    /** Collector that arbitrary geometry is submitted to. */
    SubmitNodeCollector submitNodeCollector();

    /** Pose stack positioned relative to the camera. */
    PoseStack poseStack();

    /** The vanilla level render state for this frame. */
    LevelRenderState levelState();
}
