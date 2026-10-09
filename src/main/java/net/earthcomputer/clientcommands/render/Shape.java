package net.earthcomputer.clientcommands.render;

import net.earthcomputer.clientcommands.compat.fabric.LevelExtractionContext;
import net.earthcomputer.clientcommands.compat.fabric.LevelRenderContext;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;

public abstract class Shape {
    int deathTime;
    protected Vec3 prevPos = Vec3.ZERO;

    public void tick() {
    }

    public abstract RenderState extract(LevelExtractionContext context);

    public abstract Vec3 getPos();

    public interface RenderState {
        void render(LevelRenderContext context, RenderType renderType);
    }
}
