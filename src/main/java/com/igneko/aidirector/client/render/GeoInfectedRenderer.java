package com.igneko.aidirector.client.render;

import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.entity.infected.SpecialInfectedEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Renderer GeckoLib para los infectados remodelados en Blockbench. Usa por convención
 * geo/entity/NOMBRE.geo.json, animations/entity/NOMBRE.animation.json, textures/entity/NOMBRE.png
 * y, si existe, textures/entity/NOMBRE_glowmask.png para las partes luminosas (ojos).
 */
@OnlyIn(Dist.CLIENT)
public class GeoInfectedRenderer<T extends SpecialInfectedEntity> extends GeoEntityRenderer<T> {

    private final float scale;

    public GeoInfectedRenderer(EntityRendererProvider.Context ctx, String name, float scale, float shadow, boolean glow) {
        super(ctx, new InfectedGeoModel<>(new ResourceLocation(TheAIDirector.MOD_ID, name)));
        this.scale = scale;
        this.shadowRadius = shadow;
        if (glow) addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack pose, T animatable,
                                    software.bernie.geckolib.cache.object.BakedGeoModel model, boolean isReRender,
                                    float partialTick, int packedLight, int packedOverlay) {
        super.scaleModelForRender(widthScale * scale, heightScale * scale, pose, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }
}
