package net.earthcomputer.clientcommands.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.earthcomputer.clientcommands.compat.fabric.LevelExtractionContext;
import net.earthcomputer.clientcommands.compat.fabric.LevelRenderContext;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;

public class Line extends Shape {
    public final Vec3 start;
    public final Vec3 end;
    public final int color;
    private static final float THICKNESS = 2f;

    public Line(Vec3 start, Vec3 end, int color) {
        this.start = start;
        this.end = end;
        this.color = color;
    }

    @Override
    public Shape.RenderState extract(LevelExtractionContext context) {
        Vec3 prevPosOffset = prevPos.subtract(getPos());
        float delta = context.deltaTracker().getRealtimeDeltaTicks();
        Vec3 cameraPos = context.camera().position();
        return new RenderState(
            start.add(prevPosOffset.scale(1 - delta)).subtract(cameraPos),
            end.add(prevPosOffset.scale(1 - delta).subtract(cameraPos)),
            color
        );
    }

    @Override
    public Vec3 getPos() {
        return start;
    }

    public static void drawLine(PoseStack.Pose pose, VertexConsumer vertexConsumer, Vec3 start, Vec3 end, int color) {
        Vec3 normal = end.subtract(start).normalize();
        putVertex(pose, vertexConsumer, start, normal, color);
        putVertex(pose, vertexConsumer, end, normal, color);
    }

    private static void putVertex(PoseStack.Pose pose, VertexConsumer vertexConsumer, Vec3 pos, Vec3 normal, int color) {
        vertexConsumer.addVertex(
            pose,
            (float) pos.x(),
            (float) pos.y(),
            (float) pos.z()
        ).setColor(
            ((color >> 16) & 0xFF) / 255.0F,
            ((color >> 8) & 0xFF) / 255.0F,
            (color & 0xFF) / 255.0F,
            1.0F
        ).setNormal(
            pose,
            (float) normal.x(),
            (float) normal.y(),
            (float) normal.z()
        ).setLineWidth(THICKNESS);
    }

    private record RenderState(Vec3 start, Vec3 end, int color) implements Shape.RenderState {
        @Override
        public void render(LevelRenderContext context, RenderType renderType) {
            RenderQueue.submitCustomGeometry(context.submitNodeCollector(), context.poseStack(), renderType, this::draw);
        }

        private void draw(PoseStack.Pose pose, VertexConsumer vertexConsumer) {
            drawLine(pose, vertexConsumer, start, end, color);
        }
    }
}
