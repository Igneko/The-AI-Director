package com.igneko.aidirector.client.render;

import com.igneko.aidirector.entity.infected.StranglerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/** Estrangulador (GeckoLib) más su lengua: una tira carnosa desde la boca hasta la víctima. */
@OnlyIn(Dist.CLIENT)
public class StranglerRenderer extends GeoInfectedRenderer<StranglerEntity> {

    private static final int SEGMENTS = 24;

    public StranglerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "strangler", 0.95f, 0.5f, false);
    }

    @Override
    public void render(StranglerEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, pose, buffers, light);
        Entity victim = entity.getTongueTarget();
        if (victim != null) renderTongue(entity, victim, partialTick, pose, buffers);
    }

    private void renderTongue(StranglerEntity entity, Entity victim, float pt, PoseStack pose, MultiBufferSource buffers) {
        Vec3 base = entity.getPosition(pt);
        float bodyYaw = Mth.lerp(pt, entity.yBodyRotO, entity.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 mouth = new Vec3(-Mth.sin(bodyYaw) * 0.35, entity.getBbHeight() * 0.78, Mth.cos(bodyYaw) * 0.35);
        Vec3 end = victim.getPosition(pt).add(0, victim.getBbHeight() * 0.7, 0).subtract(base);

        pose.pushPose();
        VertexConsumer vc = buffers.getBuffer(RenderType.leash());
        Matrix4f m = pose.last().pose();
        Vec3 d = end.subtract(mouth);
        Vec3 side = new Vec3(-d.z, 0, d.x);
        side = side.lengthSqr() < 1e-6 ? new Vec3(0.04, 0, 0) : side.normalize().scale(0.045);
        int packed = LightTexture.pack(entity.level().getBrightness(LightLayer.BLOCK, BlockPos.containing(entity.getEyePosition(pt))), 15);
        for (int i = 0; i <= SEGMENTS; i++) {
            float t = i / (float) SEGMENTS;
            double sag = Math.sin(t * Math.PI) * Math.min(0.6, d.length() * 0.04);
            Vec3 p = mouth.add(d.scale(t)).add(0, -sag, 0);
            float shade = (i % 2 == 0) ? 1.0f : 0.82f;
            float r = 0.76f * shade, g = 0.36f * shade, b = 0.44f * shade;
            vc.vertex(m, (float) (p.x - side.x), (float) p.y, (float) (p.z - side.z)).color(r, g, b, 1f).uv2(packed).endVertex();
            vc.vertex(m, (float) (p.x + side.x), (float) (p.y + 0.06), (float) (p.z + side.z)).color(r, g, b, 1f).uv2(packed).endVertex();
        }
        pose.popPose();
    }

    @Override
    public boolean shouldRender(StranglerEntity entity, Frustum frustum, double x, double y, double z) {
        return entity.getTongueTarget() != null || super.shouldRender(entity, frustum, x, y, z);
    }
}
