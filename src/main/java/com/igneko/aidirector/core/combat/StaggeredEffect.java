package com.igneko.aidirector.core.combat;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * STAGGERED: la entidad tambalea hacia atrás, no puede atacar y apenas se mueve por sí misma.
 * La cancelación de ataques y de navegación vive en {@link StaggerHandler}.
 */
public class StaggeredEffect extends MobEffect {

    public StaggeredEffect() {
        super(MobEffectCategory.HARMFUL, 0xC8B560);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "3b6f5e2c-91a4-4f0e-8d27-5c1b9e0a7d43",
                -0.85, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
