package net.earthcomputer.clientcommands.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.earthcomputer.clientcommands.compat.fabric.LevelExtractionContext;
import net.earthcomputer.clientcommands.compat.fabric.LevelRenderContext;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Cuboid extends Shape {
    private final Vec3 start;
    private final Vec3 size;
    private final int color;

    public Cuboid(AABB box, int color) {
        this(new Vec3(box.minX, box.minY, box.minZ), new Vec3(box.maxX, box.maxY, box.maxZ), color);
    }

    public Cuboid(Vec3 start, Vec3 end, int color) {
        this.start = start;
        this.size = new Vec3(end.x() - start.x(), end.y() - start.y(), end.z() - start.z());
        this.color = color;
    }

    @Override
    public Shape.RenderState extract(LevelExtractionContext context) {
        Vec3 prevPosOffset = prevPos.subtract(getPos());
        float delta = context.deltaTracker().getRealtimeDeltaTicks();
        Vec3 cameraPos = context.camera().position();
        return new RenderState(
            start.add(prevPosOffset.scale(1 - delta)).subtract(cameraPos),
            size,
            color
        );
    }

    @Override
    public Vec3 getPos() {
        return start;
    }

    private record RenderState(Vec3 start, Vec3 size, int color) implements Shape.RenderState {
        @Override
        public void render(LevelRenderContext context, RenderType renderType) {
            RenderQueue.submitCustomGeometry(context.submitNodeCollector(), context.poseStack(), renderType, this::draw);
        }

        private void draw(PoseStack.Pose pose, VertexConsumer vertexConsumer) {
            Line.drawLine(pose, vertexConsumer, this.start, this.start.add(this.size.x(), 0, 0), color);
            Line.drawLine(pose, vertexConsumer, this.start, this.start.add(0, this.size.y(), 0), color);
            Line.drawLine(pose, vertexConsumer, this.start, this.start.add(0, 0, this.size.z()), color);
            Line.drawLine(pose, vertexConsumer, this.start.add(this.size.x(), 0, this.size.z()), this.start.add(this.size.x(), 0, 0), color);
            Line.drawLine(pose, vertexConsumer, this.start.add(this.size.x(), 0, this.size.z()), this.start.add(this.size.x(), this.size.y(), this.size.z()), color);
            Line.drawLine(pose, vertexConsumer, this.start.add(this.size.x(), 0, this.size.z()), this.start.add(0, 0, this.size.z()), color);
            Line.drawLine(pose, vertexConsumer, this.start.add(this.size.x(), this.size.y(), 0), this.start.add(this.size.x(), 0, 0), color);
            Line.drawLine(pose, vertexConsumer, this.start.add(this.size.x(), this.size.y(), 0), this.start.add(0, this.size.y(), 0), color);
            Line.drawLine(pose, vertexConsumer, this.start.add(this.size.x(), this.size.y(), 0), this.start.add(this.size.x(), this.size.y(), this.size.z()), color);
            Line.drawLine(pose, vertexConsumer, this.start.add(0, this.size.y(), this.size.z()), this.start.add(0, 0, this.size.z()), color);
            Line.drawLine(pose, vertexConsumer, this.start.add(0, this.size.y(), this.size.z()), this.start.add(0, this.size.y(), 0), color);
            Line.drawLine(pose, vertexConsumer, this.start.add(0, this.size.y(), this.size.z()), this.start.add(this.size.x(), this.size.y(), this.size.z()), color);
        }
    }
}
