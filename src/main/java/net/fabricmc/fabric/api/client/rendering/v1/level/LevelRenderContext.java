/*
 * Reimplementation of Fabric API's level render context, kept under its original package name.
 *
 * `simplewaypoints` registers a submit-collection listener against this type. The port's equivalent
 * is `net.earthcomputer.clientcommands.compat.fabric.LevelRenderContext`; this shadow type exists so
 * the third-party jar links.
 */

package net.fabricmc.fabric.api.client.rendering.v1.level;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;

/**
 * Per-frame render context handed to {@code LevelRenderEvents} listeners.
 */
public interface LevelRenderContext {

    /** The vanilla level render state produced by the extraction pass. */
    LevelRenderState levelState();

    /** The stack custom geometry is positioned with. */
    PoseStack poseStack();

    /** The collector custom geometry is submitted to. */
    SubmitNodeCollector submitNodeCollector();
}
