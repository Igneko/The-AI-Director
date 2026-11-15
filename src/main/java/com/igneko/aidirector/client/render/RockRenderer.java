package com.igneko.aidirector.client.render;

import com.igneko.aidirector.entity.projectile.BehemothRockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** La roca del Coloso: un bloque de roca girando en el aire. */
@OnlyIn(Dist.CLIENT)
public class RockRenderer extends EntityRenderer<BehemothRockEntity> {

    public RockRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0.5f;
    }

    @Override
    public void render(BehemothRockEntity rock, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        float spin = (rock.tickCount + partialTick) * 18f;
        pose.translate(0, 0.5, 0);
        pose.mulPose(Axis.XP.rotationDegrees(spin));
        pose.mulPose(Axis.ZP.rotationDegrees(spin * 0.6f));
        pose.scale(1.2f, 1.0f, 1.1f);
        pose.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.COBBLESTONE.defaultBlockState(), pose, buffers,
                light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(rock, yaw, partialTick, pose, buffers, light);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(BehemothRockEntity rock) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
