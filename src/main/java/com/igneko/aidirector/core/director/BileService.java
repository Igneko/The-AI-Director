package com.igneko.aidirector.core.director;

import com.igneko.aidirector.compat.enhancedhordes.EnhancedHordesCompat;
import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.entity.infected.SpecialInfectedEntity;
import com.igneko.aidirector.registry.ModRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import java.util.Set;
import java.util.UUID;

/**
 * Bilis: el superviviente cubierto llama a una mini-horda y todos los infectados comunes en
 * 64 bloques le eligen como único objetivo mientras dura el efecto.
 */
public final class BileService {

    public static final int BILE_TICKS = 300; // 15 s
    private static final double ATTRACT_RADIUS = 64;

    /** Entidades cubiertas de bilis que no son jugadores (Frasco de Bilis, Fase 4). */
    private static final Set<UUID> BILED_MOBS = new HashSet<>();

    public BileService() {}

    public static boolean isBiled(LivingEntity entity) {
        return entity.hasEffect(ModRegistries.BILED.get());
    }

    /** Cubre de bilis a un superviviente. La horda solo se invoca si no estaba ya cubierto. */
    public static void cover(ServerPlayer victim) {
        if (!SpecialInfectedEntity.isSurvivor(victim)) return;
        boolean fresh = !isBiled(victim);
        victim.addEffect(new MobEffectInstance(ModRegistries.BILED.get(), BILE_TICKS, 0, false, false, true));
        if (fresh) {
            int count = AIDirectorConfig.infected().bileHordeSize();
            if (count > 0) EnhancedHordesCompat.triggerPanicEvent(victim.serverLevel(), victim.blockPosition(), count, victim);
            attract(victim.serverLevel(), victim);
        }
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (level.getGameTime() % 10 != 0) return;
        tickLures(level);
        for (ServerPlayer player : level.players()) {
            if (isBiled(player)) attract(level, player);
        }
        BILED_MOBS.removeIf(id -> {
            var e = level.getEntity(id);
            if (e == null) return false;
            if (!(e instanceof LivingEntity le) || !le.isAlive() || !isBiled(le)) return true;
            attract(level, le);
            return false;
        });
    }

    public static void trackBiledMob(LivingEntity entity) {
        BILED_MOBS.add(entity.getUUID());
    }

    /** Cubre de bilis a un mob (infectado especial, Coloso...): la horda le atacará a él. */
    public static void coverMob(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(ModRegistries.BILED.get(), BILE_TICKS + 100, 0, false, true, false));
        trackBiledMob(entity);
        if (entity.level() instanceof ServerLevel level) attract(level, entity);
    }

    private record LurePoint(ServerLevel level, Vec3 pos, long until) {}

    private static final List<LurePoint> LURES = new ArrayList<>();

    /** Punto que atrae a los comunes (nube del frasco de bilis) durante {@code ticks}. */
    public static void addLurePoint(ServerLevel level, Vec3 pos, int ticks) {
        LURES.add(new LurePoint(level, pos, level.getGameTime() + ticks));
    }

    private static void tickLures(ServerLevel level) {
        LURES.removeIf(l -> l.level() == level && level.getGameTime() > l.until());
        for (LurePoint l : LURES) {
            if (l.level() != level) continue;
            for (Monster mob : level.getEntitiesOfClass(Monster.class, new AABB(l.pos(), l.pos()).inflate(40),
                    m -> m.isAlive() && !(m instanceof SpecialInfectedEntity) && m.getType().is(EnhancedHordesCompat.HORDE_ENTITIES))) {
                if (mob.getTarget() != null && isBiled(mob.getTarget())) continue;
                mob.setTarget(null);
                mob.getNavigation().moveTo(l.pos().x, l.pos().y, l.pos().z, 1.2);
            }
        }
    }

    /** Los infectados comunes (horda) ignoran a los demás y van a por el objetivo con bilis. */
    private static void attract(ServerLevel level, LivingEntity biled) {
        AABB box = biled.getBoundingBox().inflate(ATTRACT_RADIUS);
        for (Monster mob : level.getEntitiesOfClass(Monster.class, box,
                m -> m.isAlive() && m != biled && !(m instanceof SpecialInfectedEntity)
                        && m.getType().is(EnhancedHordesCompat.HORDE_ENTITIES))) {
            if (mob.getTarget() != biled) {
                mob.setTarget(biled);
                ((Mob) mob).getNavigation().moveTo(biled, 1.25);
            }
        }
    }
}
