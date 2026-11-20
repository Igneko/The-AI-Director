package com.igneko.aidirector.core.director;

import com.igneko.aidirector.compat.enhancedhordes.EnhancedHordesCompat;
import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.survivor.SurvivorCapability;
import com.igneko.aidirector.core.survivor.SurvivorHealthData;
import com.igneko.aidirector.core.survivor.SurvivorService;
import com.igneko.aidirector.entity.infected.BehemothEntity;
import com.igneko.aidirector.network.DirectorStatusPacket;
import com.igneko.aidirector.network.ModNetwork;
import com.igneko.aidirector.registry.ModSounds;
import com.igneko.aidirector.world.saferoom.SafeZoneManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * El AI Director de cada dimensión (especificación §4.2): evalúa cada segundo la intensidad del
 * equipo, avanza la {@link DirectorFsm máquina de estados} y actúa en consecuencia — hordas
 * forzadas, goteo de comunes en la construcción, calma con menos errantes, refugios y Crescendos.
 * También envía a cada jugador el estado del refugio y del evento de pánico para el HUD.
 */
public final class DirectorManager {

    private static final double THREAT_RADIUS = 32;

    private static final class LevelDirector {
        final DirectorFsm fsm = new DirectorFsm();
        @Nullable Vec3 phaseAnchor;
        DirectorFsm.Phase lastPhase = DirectorFsm.Phase.BUILD_UP;
        int trickleTimer;
        /** Horda en cola: aviso previo y aparición por tandas. */
        @Nullable UUID hordeTarget;
        int hordeWait, hordeBatches, hordeRemaining;
    }

    private static final Map<ServerLevel, LevelDirector> DIRECTORS = new HashMap<>();
    private static final Map<UUID, DirectorStatusPacket> LAST_STATUS = new HashMap<>();

    public DirectorManager() {}

    public static DirectorFsm.Phase phase(ServerLevel level) {
        LevelDirector d = DIRECTORS.get(level);
        return d == null ? DirectorFsm.Phase.BUILD_UP : d.fsm.phase();
    }

    public static DirectorFsm fsm(ServerLevel level) {
        return DIRECTORS.computeIfAbsent(level, l -> new LevelDirector()).fsm;
    }

    /** Supervivientes del nivel que cuentan para el Director (gestionados, vivos o en el suelo). */
    public static List<ServerPlayer> survivors(ServerLevel level) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (!SurvivorService.isManaged(p) || !p.isAlive()) continue;
            if (SurvivorCapability.get(p).map(SurvivorHealthData::isDead).orElse(false)) continue;
            out.add(p);
        }
        return out;
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (level.getGameTime() % 20 != 0) return;

        LevelDirector d = DIRECTORS.computeIfAbsent(level, l -> new LevelDirector());
        List<ServerPlayer> team = survivors(level);
        sendStatus(level);
        if (team.isEmpty()) return;

        float intensity = 0f;
        Vec3 centroid = Vec3.ZERO;
        boolean allSafe = true;
        for (ServerPlayer p : team) {
            intensity = Math.max(intensity, IntensityTracker.get(p));
            centroid = centroid.add(p.position());
            allSafe &= SafeZoneManager.isInSealedZone(p);
        }
        centroid = centroid.scale(1.0 / team.size());
        int threats = 0;
        for (ServerPlayer p : team) {
            threats = Math.max(threats, level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(THREAT_RADIUS),
                    m -> m.isAlive() && m instanceof Enemy).size());
        }
        if (d.phaseAnchor == null) d.phaseAnchor = centroid;
        double travelled = centroid.distanceTo(d.phaseAnchor);

        AIDirectorConfig.DirectorSettings cfg = AIDirectorConfig.director();
        DirectorFsm.Action action = d.fsm.tick(cfg.fsm(), intensity, threats, travelled, allSafe,
                CrescendoManager.isActive(level), (min, max) -> min + level.random.nextInt(Math.max(1, max - min + 1)));

        if (d.fsm.phase() != d.lastPhase) {
            d.lastPhase = d.fsm.phase();
            d.phaseAnchor = centroid;
            d.trickleTimer = 0;
        }

        boolean canSpawn = cfg.enabled() && level.getDifficulty() != Difficulty.PEACEFUL
                && level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
        if (!canSpawn) return;
        // Mientras viva un Coloso cerca del equipo, el Director congela las hordas normales (§7.8).
        if (behemothNear(level, team)) return;

        tickPendingHorde(level, d, team);
        if (action == DirectorFsm.Action.FORCE_HORDE) {
            ServerPlayer target = pickTarget(level, team);
            if (target != null) {
                AIDirectorConfig.HordeSettings horde = AIDirectorConfig.horde();
                if (horde.warningSeconds() <= 0 && horde.batches() <= 1) {
                    EnhancedHordesCompat.spawnHordeForPlayer(target);
                } else {
                    // Aviso con sirena y la horda llega por tandas, no de golpe encima del equipo.
                    d.hordeTarget = target.getUUID();
                    d.hordeWait = Math.max(0, horde.warningSeconds() - 1);
                    d.hordeBatches = Math.max(1, horde.batches());
                    d.hordeRemaining = horde.size();
                }
                for (ServerPlayer p : team) {
                    p.displayClientMessage(Component.translatable("message.aidirector.horde.incoming").withStyle(ChatFormatting.RED), true);
                }
                level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.HORDE_INCOMING.get(), SoundSource.HOSTILE, 3f, 1f);
            }
        } else if (d.fsm.phase() == DirectorFsm.Phase.BUILD_UP && cfg.commonsTrickle()
                && ++d.trickleTimer >= cfg.trickleIntervalSeconds()) {
            // Goteo de comunes: grupos pequeños fuera de la vista que crecen con la construcción.
            d.trickleTimer = 0;
            ServerPlayer target = pickTarget(level, team);
            if (target != null && level.random.nextFloat() < 0.6f) {
                int size = Math.min(cfg.trickleMaxSize(), 2 + d.fsm.seconds() / 30);
                EnhancedHordesCompat.spawnRing(level, target.blockPosition(), size, 26, 40, target);
            }
        }
    }

    /** Avanza la horda en cola: espera el aviso y luego lanza una tanda cada pocos segundos. */
    private static void tickPendingHorde(ServerLevel level, LevelDirector d, List<ServerPlayer> team) {
        if (d.hordeBatches <= 0) return;
        if (d.hordeWait-- > 0) return;
        ServerPlayer target = team.stream().filter(p -> p.getUUID().equals(d.hordeTarget)).findFirst()
                .orElseGet(() -> pickTarget(level, team));
        if (target == null) {
            d.hordeBatches = 0;
            return;
        }
        int count = d.hordeBatches == 1 ? d.hordeRemaining : Math.max(1, d.hordeRemaining / d.hordeBatches);
        EnhancedHordesCompat.triggerPanicEvent(level, target.blockPosition(), count, target);
        d.hordeRemaining = Math.max(0, d.hordeRemaining - count);
        d.hordeBatches--;
        d.hordeWait = 2;
    }

    private static boolean behemothNear(ServerLevel level, List<ServerPlayer> team) {
        for (ServerPlayer p : team) {
            if (!level.getEntitiesOfClass(BehemothEntity.class, p.getBoundingBox().inflate(160), BehemothEntity::isAlive).isEmpty()) return true;
        }
        return false;
    }

    /** Objetivo de una horda: un superviviente al azar que no esté a salvo en un refugio. */
    @Nullable
    private static ServerPlayer pickTarget(ServerLevel level, List<ServerPlayer> team) {
        List<ServerPlayer> exposed = team.stream().filter(p -> !SafeZoneManager.isInZone(p)).toList();
        return exposed.isEmpty() ? null : exposed.get(level.random.nextInt(exposed.size()));
    }

    /** Calma: menos infectados errantes cerca del equipo (las apariciones naturales se recortan). */
    @SubscribeEvent
    public void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL || !(event.getEntity() instanceof Enemy)) return;
        if (!(event.getLevel() instanceof ServerLevel level) || !phase(level).isCalm()) return;
        double suppression = AIDirectorConfig.director().relaxSpawnSuppression();
        if (suppression <= 0 || level.random.nextDouble() >= suppression) return;
        if (level.getNearestPlayer(event.getX(), event.getY(), event.getZ(), 64, false) != null) {
            event.setSpawnCancelled(true);
        }
    }

    /** Estado del refugio y del evento de pánico de cada jugador (solo se envía si cambia). */
    private static void sendStatus(ServerLevel level) {
        DirectorFsm.Phase phase = phase(level);
        for (ServerPlayer p : level.players()) {
            SafeZoneManager.ZoneStatus zone = SafeZoneManager.statusFor(p);
            int[] count = zone == SafeZoneManager.ZoneStatus.NONE ? new int[]{0, 0} : SafeZoneManager.headcount(p);
            DirectorStatusPacket packet = new DirectorStatusPacket(zone, count[0], count[1],
                    CrescendoManager.secondsLeft(p), phase);
            if (packet.equals(LAST_STATUS.get(p.getUUID()))) continue;
            LAST_STATUS.put(p.getUUID(), packet);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_STATUS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        LAST_STATUS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        DIRECTORS.clear();
        LAST_STATUS.clear();
    }
}
