package com.igneko.aidirector.core.combat;

import com.igneko.aidirector.compat.enhancedhordes.EnhancedHordesCompat;
import com.igneko.aidirector.entity.infected.SpecialInfectedEntity;
import com.igneko.aidirector.item.MeleeWeaponItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Las armas cuerpo a cuerpo abaten de un golpe a cualquier infectado común; a los especiales, daño normal. */
public final class MeleeHandler {

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || target instanceof SpecialInfectedEntity) return;
        if (!(event.getSource().getDirectEntity() instanceof Player player)) return;
        if (!(player.getMainHandItem().getItem() instanceof MeleeWeaponItem)) return;
        if (target.getType().is(EnhancedHordesCompat.HORDE_ENTITIES)) {
            event.setAmount(Math.max(event.getAmount(), target.getHealth() + target.getAbsorptionAmount() + 20f));
        }
    }
}
