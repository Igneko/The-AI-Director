package com.igneko.aidirector.client.render;

import com.igneko.aidirector.client.model.InfectedModel;
import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.entity.infected.SpecialInfectedEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Renderer genérico: modelo del tipo, textura propia y escala del cuerpo. */
@OnlyIn(Dist.CLIENT)
public class InfectedRenderer<T extends SpecialInfectedEntity> extends MobRenderer<T, InfectedModel<T>> {

    private final ResourceLocation texture;
    private final float scale;

    public InfectedRenderer(EntityRendererProvider.Context ctx, ModelLayerLocation layer, InfectedModel.Kind kind,
                            String textureName, float scale, float shadow) {
        super(ctx, new InfectedModel<>(ctx.bakeLayer(layer), kind), shadow);
        this.texture = new ResourceLocation(TheAIDirector.MOD_ID, "textures/entity/" + textureName + ".png");
        this.scale = scale;
    }

    @Override
    protected void scale(T entity, PoseStack pose, float partialTick) {
        pose.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
