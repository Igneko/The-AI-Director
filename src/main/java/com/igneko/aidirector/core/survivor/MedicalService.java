package com.igneko.aidirector.core.survivor;

import com.igneko.aidirector.config.AIDirectorConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Casos de uso de los suministros médicos. Los usan tanto los ítems como los comandos de depuración,
 * así la regla vive en un solo sitio. Las comprobaciones {@code can*} son puras y se pueden evaluar
 * también en el cliente (con la copia sincronizada) para no reproducir animaciones que el servidor rechazaría.
 */
public final class MedicalService {

    public enum Check { OK, NOT_MANAGED, NOT_ALIVE, PINNED, ALREADY_FULL }

    private MedicalService() {}

    public static Check canUseTempHealth(Player target) {
        if (!SurvivorService.isManaged(target)) return Check.NOT_MANAGED;
        SurvivorHealthData d = SurvivorCapability.get(target).orElse(null);
        if (d == null || !d.isAlive()) return Check.NOT_ALIVE;
        if (d.isPinned()) return Check.PINNED;
        return d.totalHealth() >= AIDirectorConfig.rules().maxHealth() - 0.5f ? Check.ALREADY_FULL : Check.OK;
    }

    public static Check canUseFirstAid(Player target) {
        if (!SurvivorService.isManaged(target)) return Check.NOT_MANAGED;
        SurvivorHealthData d = SurvivorCapability.get(target).orElse(null);
        if (d == null || !d.isAlive()) return Check.NOT_ALIVE;
        if (d.isPinned()) return Check.PINNED;
        boolean nothingToHeal = d.permHealth() >= AIDirectorConfig.rules().maxHealth() - 0.5f && d.incapCount() == 0;
        return nothingToHeal ? Check.ALREADY_FULL : Check.OK;
    }

    public static boolean hasAdrenaline(Player player) {
        return SurvivorCapability.get(player).map(SurvivorHealthData::hasAdrenaline).orElse(false);
    }

    public static void applyPainPills(ServerPlayer target) {
        SurvivorCapability.get(target).ifPresent(d -> {
            d.usePainPills(AIDirectorConfig.rules());
            commit(target, d);
        });
    }

    public static void applyAdrenaline(ServerPlayer target) {
        SurvivorCapability.get(target).ifPresent(d -> {
            SurvivorRules rules = AIDirectorConfig.rules();
            d.useAdrenaline(rules);
            int ticks = Math.round(rules.adrenalineSeconds() * SurvivorRules.TICKS_PER_SECOND);
            // Sprint inducido: +20 % de velocidad mientras dura el chute.
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0, false, false, true));
            commit(target, d);
        });
    }

    public static void applyFirstAid(ServerPlayer target) {
        SurvivorCapability.get(target).ifPresent(d -> {
            d.useFirstAid(AIDirectorConfig.rules());
            commit(target, d);
        });
    }

    public static Component describe(Check check) {
        return Component.translatable(switch (check) {
            case OK -> "message.aidirector.medical.ok";
            case NOT_MANAGED -> "message.aidirector.medical.not_managed";
            case NOT_ALIVE -> "message.aidirector.medical.not_alive";
            case PINNED -> "message.aidirector.medical.pinned";
            case ALREADY_FULL -> "message.aidirector.medical.full";
        });
    }

    private static void commit(ServerPlayer target, SurvivorHealthData d) {
        SurvivorService.applyEffects(target, d);
        SurvivorService.sync(target, d);
    }
}
