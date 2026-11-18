package com.igneko.aidirector.core.director;

import com.igneko.aidirector.core.survivor.SurvivorService;
import com.igneko.aidirector.entity.infected.SpecialInfectedEntity;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.GunFireEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Estadísticas del tramo entre refugios (especificación §9): infectados y especiales abatidos,
 * precisión con TaCZ, daño a compañeros y reanimaciones. Se muestran y reinician al asegurar un refugio.
 */
public final class SegmentStats {

    private static final class Stats {
        int commons, specials, shots, hits, revives;
        float friendlyDamage;
        long lastHitTick = -1;
    }

    private static final Map<UUID, Stats> STATS = new HashMap<>();

    public SegmentStats() {}

    private static Stats of(ServerPlayer player) {
        return STATS.computeIfAbsent(player.getUUID(), id -> new Stats());
    }

    public static void onRevive(ServerPlayer reviver) {
        of(reviver).revives++;
    }

    @SubscribeEvent
    public void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || !(event.getEntity() instanceof Enemy)) return;
        if (event.getEntity() instanceof SpecialInfectedEntity) of(killer).specials++;
        else of(killer).commons++;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onFriendlyFire(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim) || !SurvivorService.isManaged(victim)) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer shooter && shooter != victim && event.getAmount() > 0) {
            of(shooter).friendlyDamage += SurvivorService.toSurvivorPoints(victim, event.getAmount());
        }
    }

    @SubscribeEvent
    public void onFire(GunFireEvent event) {
        if (event.getLogicalSide().isServer() && event.getShooter() instanceof ServerPlayer shooter) of(shooter).shots++;
    }

    @SubscribeEvent
    public void onGunHit(EntityHurtByGunEvent.Post event) {
        if (!event.getLogicalSide().isServer() || !(event.getAttacker() instanceof ServerPlayer shooter)) return;
        if (!(event.getHurtEntity() instanceof Enemy)) return;
        Stats s = of(shooter);
        long tick = shooter.level().getGameTime();
        // Los perdigones de una escopeta cuentan como un solo acierto por disparo.
        if (tick != s.lastHitTick) {
            s.hits++;
            s.lastHitTick = tick;
        }
    }

    /** Envía el resumen del tramo al jugador y reinicia sus contadores. */
    public static void report(ServerPlayer player) {
        Stats s = STATS.remove(player.getUUID());
        if (s == null) s = new Stats();
        int accuracy = s.shots == 0 ? 0 : Math.min(100, Math.round(100f * s.hits / s.shots));
        player.sendSystemMessage(Component.translatable("message.aidirector.stats.header").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("message.aidirector.stats.kills", s.commons, s.specials).withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(Component.translatable("message.aidirector.stats.accuracy", accuracy, s.hits, s.shots).withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(Component.translatable("message.aidirector.stats.team", Math.round(s.friendlyDamage), s.revives)
                .withStyle(s.friendlyDamage > 0 ? ChatFormatting.YELLOW : ChatFormatting.WHITE));
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATS.remove(event.getEntity().getUUID());
    }
}
