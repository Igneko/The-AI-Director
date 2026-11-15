package com.igneko.aidirector.core.combat;

import com.igneko.aidirector.registry.ModRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Aplica las consecuencias del estado STAGGERED: interrumpe ataques, navegación y explosiones. */
public final class StaggerHandler {

    public static boolean isStaggered(LivingEntity entity) {
        return entity.hasEffect(ModRegistries.STAGGERED.get());
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Mob mob)) return;
        if (!isStaggered(mob)) return;
        mob.getNavigation().stop();
        if (mob instanceof Creeper creeper) creeper.setSwellDir(-1);
    }

    /** Un infectado aturdido no puede golpear. */
    @SubscribeEvent
    public void onAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker
                && event.getSource().getDirectEntity() == attacker && isStaggered(attacker)) {
            event.setCanceled(true);
        }
    }
}
