package com.igneko.aidirector.client.render;

import com.igneko.aidirector.entity.infected.SpecialInfectedEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * Modelo GeckoLib de los infectados: la cabeza mira hacia donde mira la criatura (normalmente, al
 * jugador) compensando la inclinación acumulada de sus huesos padre (torso encorvado, cuello), de
 * modo que un cuerpo inclinado hacia delante no la deja mirando al suelo.
 */
@OnlyIn(Dist.CLIENT)
public class InfectedGeoModel<T extends SpecialInfectedEntity> extends DefaultedEntityGeoModel<T> {

    private static final float MAX_PITCH = 1.3f;

    public InfectedGeoModel(ResourceLocation assetPath) {
        super(assetPath, false);
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> state) {
        super.setCustomAnimations(animatable, instanceId, state);
        if (!animatable.shouldTrackHead()) return;
        CoreGeoBone head = getAnimationProcessor().getBone("head");
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head == null || data == null) return;

        float parents = 0f;
        for (CoreGeoBone bone = head.getParent(); bone != null; bone = bone.getParent()) parents += bone.getRotX();
        head.setRotX(Mth.clamp(data.headPitch() * Mth.DEG_TO_RAD - parents, -MAX_PITCH, MAX_PITCH));
        head.setRotY(data.netHeadYaw() * Mth.DEG_TO_RAD);
    }
}
