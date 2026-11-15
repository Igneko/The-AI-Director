package com.igneko.aidirector.core.director;

import com.igneko.aidirector.compat.enhancedhordes.EnhancedHordesCompat;
import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.entity.infected.BehemothEntity;
import com.igneko.aidirector.entity.infected.LeechEntity;
import com.igneko.aidirector.entity.infected.LurkerEntity;
import com.igneko.aidirector.entity.infected.RammerEntity;
import com.igneko.aidirector.entity.infected.SpecialInfectedEntity;
import com.igneko.aidirector.entity.infected.StranglerEntity;
import com.igneko.aidirector.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Aparición natural de Infectados Especiales guiada por la intensidad (estrés) de cada superviviente.
 *
 * <ul>
 *   <li><b>Ritmo:</b> mientras el estrés sube (construcción) el Director embosca; si alcanza el pico
 *   (≥ 0,85) deja de generar durante un tiempo de calma para que el equipo respire.</li>
 *   <li><b>Categoría según el estrés:</b> en calma prefiere emboscadas sigilosas (y la Plañidera en la
 *   oscuridad); con tensión media, agarradores y Embestidor; con estrés alto, castigo a distancia
 *   (Corrosiva, Parásito). El Coloso es un evento raro con su propio enfriamiento.</li>
 *   <li><b>Reglas:</b> fuera de la vista de todos, a 20-48 bloques; nunca más agarradores vivos que
 *   supervivientes; tope de especiales cercanos.</li>
 * </ul>
 */
public final class SpecialInfectedSpawner {

    private static final int CHECK_INTERVAL = 40; // 2 s
    private static final float PEAK = 0.85f;

    private final Map<UUID, Long> lastSpawn = new HashMap<>();
    private final Map<UUID, Long> relaxUntil = new HashMap<>();
    private long lastBehemoth = -1_000_000L;

    public SpecialInfectedSpawner() {}

    private record Option(Supplier<? extends EntityType<? extends SpecialInfectedEntity>> type, int weight, boolean pinner,
                          boolean needsDark, boolean preferHigh) {}

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        AIDirectorConfig.SpawnSettings cfg = AIDirectorConfig.spawning();
        if (!cfg.enabled() || level.getGameTime() % CHECK_INTERVAL != 0) return;
        if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;

        List<ServerPlayer> survivors = new ArrayList<>();
        for (ServerPlayer p : level.players()) if (SpecialInfectedEntity.isSurvivor(p)) survivors.add(p);
        if (survivors.isEmpty()) return;

        RandomSource random = level.getRandom();
        long now = level.getGameTime();
        for (ServerPlayer player : survivors) {
            float intensity = IntensityTracker.get(player);
            if (intensity >= PEAK) {
                // Pico: mantener la presión pero sin nuevos especiales; después, calma.
                relaxUntil.put(player.getUUID(), now + cfg.relaxSeconds() * 20L);
                continue;
            }
            if (now < relaxUntil.getOrDefault(player.getUUID(), 0L)) continue;
            if (now - lastSpawn.getOrDefault(player.getUUID(), -100000L) < cfg.minSecondsBetween() * 20L) continue;

            List<SpecialInfectedEntity> nearby = level.getEntitiesOfClass(SpecialInfectedEntity.class,
                    player.getBoundingBox().inflate(64), SpecialInfectedEntity::isAlive);
            if (nearby.size() >= cfg.maxNearby()) continue;

            // Cuanto más tiempo sin especiales y más estrés acumulado (sin llegar al pico), más probable.
            double minutesIdle = (now - lastSpawn.getOrDefault(player.getUUID(), now - 1200L)) / 1200.0;
            double chance = cfg.baseChance() * (1 + Math.min(2.0, minutesIdle)) * (0.6 + intensity);
            if (random.nextDouble() >= chance) continue;

            Option option = choose(level, player, intensity, random, survivors.size(), now, cfg);
            if (option == null) continue;
            if (trySpawn(level, survivors, player, option, random)) {
                lastSpawn.put(player.getUUID(), now);
                if (option.type() == (Supplier<?>) ModEntities.BEHEMOTH) lastBehemoth = now;
            }
        }
    }

    @Nullable
    private Option choose(ServerLevel level, ServerPlayer player, float intensity, RandomSource random, int survivors,
                          long now, AIDirectorConfig.SpawnSettings cfg) {
        // Evento raro: el Coloso, con tensión media y un enfriamiento global largo.
        boolean behemothAlive = !level.getEntitiesOfClass(BehemothEntity.class, player.getBoundingBox().inflate(160), BehemothEntity::isAlive).isEmpty();
        if (!behemothAlive && intensity >= 0.35f && now - lastBehemoth > cfg.behemothCooldownMinutes() * 1200L
                && random.nextDouble() < cfg.behemothChance()) {
            return new Option(ModEntities.BEHEMOTH, 1, false, false, false);
        }

        List<Option> options = new ArrayList<>();
        if (intensity < 0.3f) {
            options.add(new Option(ModEntities.WAILER, 14, false, true, false));
            options.add(new Option(ModEntities.LURKER, 22, true, false, false));
            options.add(new Option(ModEntities.STRANGLER, 22, true, false, true));
            options.add(new Option(ModEntities.BLOATER, 20, false, false, false));
            options.add(new Option(ModEntities.LEECH, 10, true, false, false));
            options.add(new Option(ModEntities.CORRODER, 8, false, false, false));
            options.add(new Option(ModEntities.RAMMER, 4, true, false, false));
        } else if (intensity < 0.6f) {
            options.add(new Option(ModEntities.LURKER, 20, true, false, false));
            options.add(new Option(ModEntities.STRANGLER, 15, true, false, true));
            options.add(new Option(ModEntities.BLOATER, 15, false, false, false));
            options.add(new Option(ModEntities.LEECH, 15, true, false, false));
            options.add(new Option(ModEntities.CORRODER, 15, false, false, false));
            options.add(new Option(ModEntities.RAMMER, 15, true, false, false));
        } else {
            options.add(new Option(ModEntities.CORRODER, 22, false, false, false));
            options.add(new Option(ModEntities.LEECH, 20, true, false, false));
            options.add(new Option(ModEntities.RAMMER, 18, true, false, false));
            options.add(new Option(ModEntities.BLOATER, 16, false, false, false));
            options.add(new Option(ModEntities.LURKER, 14, true, false, false));
            options.add(new Option(ModEntities.STRANGLER, 10, true, false, true));
        }

        // Regla del Director: nunca más agarradores vivos que supervivientes.
        int pinners = level.getEntitiesOfClass(SpecialInfectedEntity.class, player.getBoundingBox().inflate(96),
                e -> e.isAlive() && (e instanceof LurkerEntity || e instanceof StranglerEntity
                        || e instanceof LeechEntity || e instanceof RammerEntity)).size();
        if (pinners >= survivors) options.removeIf(Option::pinner);
        if (options.isEmpty()) return null;

        int total = options.stream().mapToInt(Option::weight).sum();
        int roll = random.nextInt(total);
        for (Option o : options) {
            roll -= o.weight();
            if (roll < 0) return o;
        }
        return null;
    }

    /** Busca un punto oculto a 20-48 bloques y genera allí al infectado. */
    private boolean trySpawn(ServerLevel level, List<ServerPlayer> survivors, ServerPlayer focus, Option option, RandomSource random) {
        BlockPos best = null;
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = 20 + random.nextDouble() * 28;
            int x = (int) Math.floor(focus.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(focus.getZ() + Math.sin(angle) * dist);
            if (!level.hasChunkAt(new BlockPos(x, focus.getBlockY(), z))) continue;
            BlockPos pos = EnhancedHordesCompat.findGround(level, x, focus.getBlockY(), z, 16);
            if (pos == null || seenByAnyone(level, survivors, pos)) continue;
            if (option.needsDark() && level.getMaxLocalRawBrightness(pos) > 7) continue;
            EntityType<? extends SpecialInfectedEntity> type = option.type().get();
            if (!level.noCollision(type.getAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) continue;
            if (best == null || (option.preferHigh() && pos.getY() > best.getY())) best = pos;
            if (!option.preferHigh()) break;
        }
        if (best == null) return false;

        Mob mob = option.type().get().create(level);
        if (mob == null) return false;
        mob.moveTo(best.getX() + 0.5, best.getY(), best.getZ() + 0.5, random.nextFloat() * 360f, 0f);
        ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(best), MobSpawnType.EVENT, null, null);
        if (!(mob instanceof com.igneko.aidirector.entity.infected.WailerEntity)) mob.setTarget(focus);
        return level.addFreshEntity(mob);
    }

    /** ¿Algún superviviente tiene línea de visión directa a ese punto? */
    private static boolean seenByAnyone(ServerLevel level, List<ServerPlayer> survivors, BlockPos pos) {
        Vec3 target = Vec3.atCenterOf(pos).add(0, 0.6, 0);
        for (ServerPlayer p : survivors) {
            Vec3 eye = p.getEyePosition();
            if (eye.distanceToSqr(target) > 64 * 64) continue;
            HitResult hit = level.clip(new ClipContext(eye, target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p));
            if (hit.getType() == HitResult.Type.MISS) return true;
        }
        return false;
    }
}
