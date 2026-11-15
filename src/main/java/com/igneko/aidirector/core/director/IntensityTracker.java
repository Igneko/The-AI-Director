package com.igneko.aidirector.core.director;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.survivor.SurvivorCapability;
import com.igneko.aidirector.core.survivor.SurvivorHealthData;
import com.igneko.aidirector.core.survivor.SurvivorService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Intensidad emocional / estrés de cada superviviente, I ∈ [0, 1] (especificación §4.1):
 * <ul>
 *   <li>Daño recibido: +daño / Hmax × 1,5</li>
 *   <li>Aliado incapacitado en 24 bloques: +0,40</li>
 *   <li>Muerte de un aliado: +0,75</li>
 *   <li>Infectado abatido a ≤ 8 bloques: +0,03</li>
 *   <li>Decaimiento 0,04/s cuando no recibe daño ni hay infectados a 16 bloques</li>
 * </ul>
 */
public final class IntensityTracker {

    private static final double DECAY_PER_SECOND = 0.04;
    private static final int CALM_TICKS = 60; // sin daño reciente

    private static final Map<UUID, Float> INTENSITY = new HashMap<>();
    private static final Map<UUID, Long> LAST_DAMAGE = new HashMap<>();

    public IntensityTracker() {}

    public static float get(ServerPlayer player) {
        return INTENSITY.getOrDefault(player.getUUID(), 0f);
    }

    public static void add(ServerPlayer player, float amount) {
        INTENSITY.merge(player.getUUID(), amount, (a, b) -> Math.min(1f, Math.max(0f, a + b)));
    }

    // ------------------------------------------------------------------ fuentes de estrés

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SurvivorService.isManaged(player)) return;
        float points = SurvivorService.toSurvivorPoints(player, event.getAmount());
        if (points <= 0f) return;
        add(player, points / AIDirectorConfig.rules().maxHealth() * 1.5f);
        LAST_DAMAGE.put(player.getUUID(), player.level().getGameTime());
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide()) return;
        if (dead instanceof ServerPlayer deadPlayer) {
            for (ServerPlayer ally : deadPlayer.serverLevel().players()) {
                if (ally != deadPlayer && SurvivorService.isManaged(ally)) add(ally, 0.75f);
            }
            INTENSITY.remove(deadPlayer.getUUID());
            return;
        }
        if (dead instanceof Monster) {
            for (ServerPlayer p : ((net.minecraft.server.level.ServerLevel) dead.level()).players()) {
                if (SurvivorService.isManaged(p) && p.distanceToSqr(dead) <= 8 * 8) add(p, 0.03f);
            }
        }
    }

    /** Llamado al quedar incapacitado un superviviente. */
    public static void onAllyIncapacitated(ServerPlayer downed) {
        for (ServerPlayer ally : downed.serverLevel().players()) {
            if (ally != downed && SurvivorService.isManaged(ally) && ally.distanceToSqr(downed) <= 24 * 24) add(ally, 0.40f);
        }
        add(downed, 0.40f);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) return;
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            Float current = INTENSITY.get(p.getUUID());
            if (current == null || current <= 0f) continue;
            long since = p.level().getGameTime() - LAST_DAMAGE.getOrDefault(p.getUUID(), 0L);
            boolean threats = !p.level().getEntitiesOfClass(Monster.class, p.getBoundingBox().inflate(16), Monster::isAlive).isEmpty();
            boolean downed = SurvivorCapability.get(p).map(SurvivorHealthData::isIncapacitated).orElse(false);
            if (since > CALM_TICKS && !threats && !downed) {
                INTENSITY.put(p.getUUID(), (float) Math.max(0.0, current - DECAY_PER_SECOND));
            }
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        INTENSITY.remove(event.getEntity().getUUID());
        LAST_DAMAGE.remove(event.getEntity().getUUID());
    }
}
