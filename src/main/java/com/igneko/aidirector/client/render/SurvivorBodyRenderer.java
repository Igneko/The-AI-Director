package com.igneko.aidirector.client.render;

import com.igneko.aidirector.entity.SurvivorBodyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

/** Dibuja el cuerpo de un superviviente caído: el modelo del jugador, con su skin, tumbado boca arriba. */
@OnlyIn(Dist.CLIENT)
public class SurvivorBodyRenderer extends EntityRenderer<SurvivorBodyEntity> {

    private final PlayerModel<LivingEntity> wide;
    private final PlayerModel<LivingEntity> slim;

    public SurvivorBodyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.wide = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false);
        this.slim = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.shadowRadius = 0.6f;
        pose(wide);
        pose(slim);
    }

    /** Postura de caído: brazos abiertos, piernas algo separadas y la cabeza ladeada. */
    private static void pose(PlayerModel<LivingEntity> m) {
        m.head.xRot = -0.15f;
        m.head.yRot = 0.35f;
        m.rightArm.zRot = 0.55f;
        m.leftArm.zRot = -0.35f;
        m.leftArm.xRot = -0.2f;
        m.rightLeg.zRot = 0.12f;
        m.leftLeg.zRot = -0.1f;
        m.hat.copyFrom(m.head);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);
        m.jacket.copyFrom(m.body);
    }

    private static PlayerInfo info(UUID id) {
        var connection = Minecraft.getInstance().getConnection();
        return connection == null || id == null ? null : connection.getPlayerInfo(id);
    }

    private boolean isSlim(SurvivorBodyEntity body) {
        PlayerInfo info = info(body.owner());
        if (info != null) return "slim".equals(info.getModelName());
        return body.owner() != null && "slim".equals(DefaultPlayerSkin.getSkinModelName(body.owner()));
    }

    @Override
    public void render(SurvivorBodyEntity body, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        PlayerModel<LivingEntity> model = isSlim(body) ? slim : wide;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180f - body.getYRot()));
        pose.translate(0, 0.14, -0.85);
        pose.mulPose(Axis.XP.rotationDegrees(90f));
        pose.scale(-0.9375f, -0.9375f, 0.9375f);
        pose.translate(0, -1.501, 0);
        // Algo más pálido que en vida.
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(body))), light,
                OverlayTexture.NO_OVERLAY, 0.78f, 0.74f, 0.74f, 1f);
        pose.popPose();
        super.render(body, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SurvivorBodyEntity body) {
        PlayerInfo info = info(body.owner());
        if (info != null) return info.getSkinLocation();
        return body.owner() != null ? DefaultPlayerSkin.getDefaultSkin(body.owner()) : DefaultPlayerSkin.getDefaultSkin();
    }
}
