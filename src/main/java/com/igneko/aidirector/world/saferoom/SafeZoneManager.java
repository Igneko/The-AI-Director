package com.igneko.aidirector.world.saferoom;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.director.IntensityTracker;
import com.igneko.aidirector.core.director.SegmentStats;
import com.igneko.aidirector.core.survivor.DefibService;
import com.igneko.aidirector.core.survivor.SquadService;
import com.igneko.aidirector.core.survivor.SurvivorCapability;
import com.igneko.aidirector.core.survivor.SurvivorHealthData;
import com.igneko.aidirector.core.survivor.SurvivorRules;
import com.igneko.aidirector.core.survivor.SurvivorService;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Zonas seguras (refugios). Mantiene el registro de radios cargadas por nivel y aplica las reglas:
 * <ul>
 *   <li><b>Indestructible:</b> los bloques originales no se rompen (salvo en creativo), las
 *   explosiones no les afectan y la zona se repara sola si la horda arranca algo.</li>
 *   <li><b>Sin apariciones:</b> ningún monstruo aparece dentro de la zona.</li>
 *   <li><b>Asegurar el refugio:</b> cuando todo el equipo vivo está dentro y las puertas de refugio
 *   cerradas, se guarda el punto de control, se curan los estados críticos según la configuración,
 *   vuelven los caídos y se muestra el resumen del tramo.</li>
 * </ul>
 */
public final class SafeZoneManager {

    private static final int TICK_INTERVAL = 10;
    private static final int SECURE_DELAY_TICKS = 60;
    private static final int REPAIR_INTERVAL = 40;

    private static final Map<Level, Set<SafeRoomRadioBlockEntity>> ZONES = new HashMap<>();

    public SafeZoneManager() {}

    // ------------------------------------------------------------------ registro

    static void register(SafeRoomRadioBlockEntity radio) {
        ZONES.computeIfAbsent(radio.getLevel(), l -> new LinkedHashSet<>()).add(radio);
    }

    static void unregister(SafeRoomRadioBlockEntity radio) {
        Set<SafeRoomRadioBlockEntity> set = ZONES.get(radio.getLevel());
        if (set != null) set.remove(radio);
    }

    public static List<SafeRoomRadioBlockEntity> zones(Level level) {
        Set<SafeRoomRadioBlockEntity> set = ZONES.get(level);
        return set == null ? List.of() : new ArrayList<>(set);
    }

    @Nullable
    public static SafeRoomRadioBlockEntity zoneAt(Level level, BlockPos pos) {
        Set<SafeRoomRadioBlockEntity> set = ZONES.get(level);
        if (set == null) return null;
        for (SafeRoomRadioBlockEntity radio : set) {
            if (!radio.isRemoved() && radio.contains(pos)) return radio;
        }
        return null;
    }

    /** Refugio cuya estructura completa contiene {@code pos} (protección frente a mobs y explosiones). */
    @Nullable
    public static SafeRoomRadioBlockEntity guardAt(Level level, BlockPos pos) {
        Set<SafeRoomRadioBlockEntity> set = ZONES.get(level);
        if (set == null) return null;
        for (SafeRoomRadioBlockEntity radio : set) {
            if (!radio.isRemoved() && radio.bounds().isInside(pos)) return radio;
        }
        return null;
    }

    /** Hay algún refugio cuya estructura (ampliada en {@code margin}) toca {@code pos}. */
    public static boolean nearGuard(Level level, BlockPos pos, int margin) {
        Set<SafeRoomRadioBlockEntity> set = ZONES.get(level);
        if (set == null || set.isEmpty()) return false;
        for (SafeRoomRadioBlockEntity radio : set) {
            if (radio.isRemoved()) continue;
            BoundingBox b = radio.bounds();
            if (pos.getX() >= b.minX() - margin && pos.getX() <= b.maxX() + margin
                    && pos.getY() >= b.minY() - margin && pos.getY() <= b.maxY() + margin
                    && pos.getZ() >= b.minZ() - margin && pos.getZ() <= b.maxZ() + margin) return true;
        }
        return false;
    }

    public static boolean isInZone(Player player) {
        return zoneAt(player.level(), player.blockPosition()) != null;
    }

    /** Dentro de un refugio con todas sus puertas cerradas. */
    public static boolean isInSealedZone(Player player) {
        SafeRoomRadioBlockEntity zone = zoneAt(player.level(), player.blockPosition());
        return zone != null && zone.sealed;
    }

    // ------------------------------------------------------------------ lógica por zona

    /** Toma ya la foto de la zona y localiza sus puertas (pruebas y comandos), si sus chunks están cargados. */
    public static boolean prime(SafeRoomRadioBlockEntity radio) {
        BoundingBox zone = radio.zone();
        if (radio.getLevel() == null || !radio.getLevel().hasChunksAt(zone.minX(), zone.minY(), zone.minZ(), zone.maxX(), zone.maxY(), zone.maxZ())) {
            return false;
        }
        radio.takeSnapshot(radio.getLevel());
        return true;
    }

    static void tick(SafeRoomRadioBlockEntity radio) {
        if (!(radio.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if ((now + radio.getBlockPos().asLong()) % TICK_INTERVAL != 0) return;
        BoundingBox zone = radio.zone();

        if (radio.wantsSnapshot() && level.hasChunksAt(zone.minX(), zone.minY(), zone.minZ(), zone.maxX(), zone.maxY(), zone.maxZ())) {
            radio.takeSnapshot(level);
        }
        if (AIDirectorConfig.safeRoom().selfRepair() && radio.hasSnapshot() && now % REPAIR_INTERVAL < TICK_INTERVAL) {
            int fixed = radio.repair(level);
            if (fixed > 0) {
                Vec3 c = zone.getCenter().getCenter();
                level.playSound(null, c.x, c.y, c.z, ModSounds.SAFEROOM_REPAIR.get(), SoundSource.BLOCKS, 0.6f, 1f);
            }
        }

        radio.sealed = isSealed(level, radio);
        radio.inside.clear();
        for (ServerPlayer p : level.players()) {
            if (p.isAlive() && radio.contains(p.blockPosition())) radio.inside.add(p.getUUID());
        }
        if (radio.inside.isEmpty()) {
            radio.securedTicks = 0;
            return;
        }

        List<ServerPlayer> team = team(level, radio);
        boolean everyoneInside = !team.isEmpty() && team.stream().allMatch(p -> radio.inside.contains(p.getUUID()));
        if (!radio.sealed || !everyoneInside) {
            radio.securedTicks = 0;
            return;
        }
        if (radio.securedTicks < 0) return; // ya asegurado; espera a que alguien salga o abra
        radio.securedTicks += TICK_INTERVAL;
        if (radio.securedTicks < SECURE_DELAY_TICKS) return;
        radio.securedTicks = -1;

        long cooldown = AIDirectorConfig.safeRoom().cooldownMinutes() * 1200L;
        List<ServerPlayer> fresh = team.stream().filter(p -> !radio.onCooldown(p.getUUID(), now, cooldown)).toList();
        if (!fresh.isEmpty()) secure(level, radio, team, now);
    }

    /** Supervivientes vivos (incluidos los incapacitados) que forman "el equipo" de este refugio. */
    private static List<ServerPlayer> team(ServerLevel level, SafeRoomRadioBlockEntity radio) {
        Vec3 center = radio.zone().getCenter().getCenter();
        double radius = AIDirectorConfig.safeRoom().teamRadius();
        List<ServerPlayer> team = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (!SurvivorService.isManaged(p) || !p.isAlive()) continue;
            if (SurvivorCapability.get(p).map(SurvivorHealthData::isDead).orElse(false)) continue;
            if (p.position().distanceToSqr(center) <= radius * radius) team.add(p);
        }
        return team;
    }

    private static boolean isSealed(ServerLevel level, SafeRoomRadioBlockEntity radio) {
        if (radio.doors().isEmpty()) return true;
        for (BlockPos door : radio.doors()) {
            if (!level.isLoaded(door)) continue;
            BlockState state = level.getBlockState(door);
            if (state.getBlock() instanceof DoorBlock d && d.isOpen(state)) return false;
        }
        return true;
    }

    /** Asegura el refugio: punto de control, curas, rescate de caídos y resumen del tramo. */
    private static void secure(ServerLevel level, SafeRoomRadioBlockEntity radio, List<ServerPlayer> team, long now) {
        AIDirectorConfig.SafeRoomSettings cfg = AIDirectorConfig.safeRoom();
        SurvivorRules rules = AIDirectorConfig.rules();
        BlockPos spawn = radio.spawn();
        float yaw = level.random.nextFloat() * 360f;

        for (ServerPlayer p : team) {
            SurvivorHealthData d = SurvivorCapability.get(p).orElse(null);
            if (d != null) {
                if (d.isIncapacitated() && cfg.reviveDowned()) d.completeRevive(rules);
                float perm = Math.max(d.permHealth(), Math.min(rules.maxHealth(), cfg.minHealth()));
                float temp = Math.min(d.tempHealth(), rules.maxHealth() - perm);
                int incaps = cfg.resetBlackAndWhite() ? 0 : d.incapCount();
                d.restore(perm, temp, d.state(), incaps, d.bleedHealth(), d.adrenalineTicks(), 0f, false);
                SurvivorService.applyEffects(p, d);
                SurvivorService.sync(p, d);
            }
            if (cfg.setSpawn()) p.setRespawnPosition(level.dimension(), spawn, yaw, true, false);
            IntensityTracker.reset(p);
            radio.markCheckpoint(p.getUUID(), now);

            title(p, Component.translatable("title.aidirector.saferoom.secured").withStyle(ChatFormatting.GREEN),
                    Component.translatable(cfg.setSpawn() ? "title.aidirector.saferoom.checkpoint" : "title.aidirector.saferoom.rest")
                            .withStyle(ChatFormatting.GRAY));
            SegmentStats.report(p);
        }

        int rescued = 0;
        if (cfg.rescueDead()) {
            for (ServerPlayer dead : List.copyOf(level.getServer().getPlayerList().getPlayers())) {
                if (dead.level() != level || !dead.isDeadOrDying() || !SquadService.areSquadmates(dead, team.get(0))) continue;
                if (DefibService.revive(dead, level, Vec3.atBottomCenterOf(spawn), yaw, null)) rescued++;
            }
        }
        if (rescued > 0) {
            for (ServerPlayer p : team) {
                p.sendSystemMessage(Component.translatable("message.aidirector.saferoom.rescued", rescued).withStyle(ChatFormatting.AQUA));
            }
        }

        Vec3 c = Vec3.atCenterOf(radio.getBlockPos());
        level.playSound(null, c.x, c.y, c.z, ModSounds.SAFEROOM_SECURED.get(), SoundSource.BLOCKS, 1.2f, 1f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.x, c.y + 0.6, c.z, 12, 0.4, 0.3, 0.4, 0.02);
    }

    private static void title(ServerPlayer p, Component title, Component subtitle) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** Estado de la zona en la que está el jugador (para el HUD). */
    public static ZoneStatus statusFor(ServerPlayer player) {
        SafeRoomRadioBlockEntity radio = zoneAt(player.level(), player.blockPosition());
        if (radio == null) return ZoneStatus.NONE;
        long cooldown = AIDirectorConfig.safeRoom().cooldownMinutes() * 1200L;
        if (radio.sealed && radio.securedTicks < 0) return ZoneStatus.SECURED;
        if (radio.onCooldown(player.getUUID(), player.level().getGameTime(), Math.min(cooldown, 200))) return ZoneStatus.SECURED;
        return radio.sealed ? ZoneStatus.SEALED : ZoneStatus.OPEN;
    }

    public enum ZoneStatus { NONE, OPEN, SEALED, SECURED }

    /** Recuento para el HUD: supervivientes dentro / equipo. */
    public static int[] headcount(ServerPlayer player) {
        SafeRoomRadioBlockEntity radio = zoneAt(player.level(), player.blockPosition());
        if (radio == null || !(player.level() instanceof ServerLevel level)) return new int[]{0, 0};
        List<ServerPlayer> team = team(level, radio);
        int in = (int) team.stream().filter(p -> radio.contains(p.blockPosition())).count();
        return new int[]{in, team.size()};
    }

    /** Mensaje de la radio: puertas, equipo y estado. */
    static void describe(ServerPlayer player, SafeRoomRadioBlockEntity radio) {
        ServerLevel level = player.serverLevel();
        boolean sealed = isSealed(level, radio);
        List<ServerPlayer> team = team(level, radio);
        long in = team.stream().filter(p -> radio.contains(p.blockPosition())).count();
        int doors = radio.doors().size();
        long closed = radio.doors().stream().filter(d -> {
            BlockState s = level.getBlockState(d);
            return !(s.getBlock() instanceof DoorBlock door && door.isOpen(s));
        }).count();
        player.sendSystemMessage(Component.translatable("message.aidirector.radio.header").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("message.aidirector.radio.doors", closed, doors)
                .withStyle(sealed ? ChatFormatting.GREEN : ChatFormatting.RED));
        player.sendSystemMessage(Component.translatable("message.aidirector.radio.team", in, team.size())
                .withStyle(in == team.size() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable(sealed && in == team.size()
                ? "message.aidirector.radio.ready" : "message.aidirector.radio.hint").withStyle(ChatFormatting.GRAY));
        level.playSound(null, radio.getBlockPos(), ModSounds.RADIO_STATIC.get(), SoundSource.BLOCKS, 0.8f, 1f);
    }

    // ------------------------------------------------------------------ protección

    /** Protección frente a explosiones: toda la estructura; dentro de la zona, solo lo original. */
    private static boolean shouldProtect(Level level, BlockPos pos, BlockState state) {
        if (!AIDirectorConfig.safeRoom().protectBlocks() || state.isAir()) return false;
        if (guardAt(level, pos) == null) return false;
        SafeRoomRadioBlockEntity radio = zoneAt(level, pos);
        return radio == null || radio.isOriginal(pos, state);
    }

    /**
     * Los jugadores (en cualquier modo) sí pueden reformar su refugio: lo que rompen o colocan pasa a
     * ser el nuevo "original", así la autorreparación no lo devuelve (ni se puede duplicar).
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof Level level) || level.isClientSide()) return;
        SafeRoomRadioBlockEntity radio = zoneAt(level, event.getPos());
        if (radio != null) radio.requestSnapshot();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof Level level) || level.isClientSide()) return;
        if (event.getEntity() instanceof Player) {
            SafeRoomRadioBlockEntity radio = zoneAt(level, event.getPos());
            if (radio != null) radio.requestSnapshot();
        }
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide() || ZONES.getOrDefault(level, Set.of()).isEmpty()) return;
        event.getAffectedBlocks().removeIf(pos -> shouldProtect(level, pos, level.getBlockState(pos)));
    }

    @SubscribeEvent
    public void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!(event.getEntity() instanceof Enemy) || event.getSpawnType() == MobSpawnType.SPAWN_EGG
                || event.getSpawnType() == MobSpawnType.COMMAND || event.getSpawnType() == MobSpawnType.SPAWNER) return;
        if (event.getLevel() instanceof ServerLevel level
                && zoneAt(level, BlockPos.containing(event.getX(), event.getY(), event.getZ())) != null) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        ZONES.clear();
    }
}
