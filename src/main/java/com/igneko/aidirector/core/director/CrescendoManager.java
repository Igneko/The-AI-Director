package com.igneko.aidirector.core.director;

import com.igneko.aidirector.compat.enhancedhordes.EnhancedHordesCompat;
import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.entity.infected.SpecialInfectedEntity;
import com.igneko.aidirector.registry.ModSounds;
import com.igneko.aidirector.world.saferoom.CrescendoAlarmBlock;
import com.igneko.aidirector.world.saferoom.SupplyCrateBlock;
import com.igneko.aidirector.world.saferoom.SupplyCrateBlockEntity;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Eventos de pánico (Crescendo): mientras dura uno, el Director lanza oleadas continuas alrededor
 * de la alarma que lo inició, sin respetar la calma. Hay como mucho un evento por dimensión.
 */
public final class CrescendoManager {

    /** Radio en el que los jugadores oyen la sirena, ven la cuenta atrás y son objetivo de las oleadas. */
    public static final int EVENT_RADIUS = 96;
    private static final int REWARD_RADIUS = 24;

    private record Event(BlockPos alarm, long start, long end, long nextWave) {
        Event withNextWave(long tick) {
            return new Event(alarm, start, end, tick);
        }
    }

    private static final Map<ServerLevel, Event> EVENTS = new HashMap<>();

    public CrescendoManager() {}

    public static boolean isActive(ServerLevel level) {
        return EVENTS.containsKey(level);
    }

    public static boolean isActiveAt(ServerLevel level, BlockPos alarm) {
        Event e = EVENTS.get(level);
        return e != null && e.alarm().equals(alarm);
    }

    /** Segundos restantes del evento que afecta a este jugador, o -1. */
    public static int secondsLeft(ServerPlayer player) {
        Event e = EVENTS.get(player.serverLevel());
        if (e == null || player.blockPosition().distSqr(e.alarm()) > EVENT_RADIUS * EVENT_RADIUS) return -1;
        return (int) Math.max(0, (e.end() - player.level().getGameTime() + 19) / 20);
    }

    public static void start(ServerLevel level, BlockPos alarm, int seconds) {
        if (isActive(level)) return;
        long now = level.getGameTime();
        EVENTS.put(level, new Event(alarm.immutable(), now, now + seconds * 20L, now + 40));
        setAlarmActive(level, alarm, true);
        for (ServerPlayer p : nearby(level, alarm)) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 15));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("title.aidirector.crescendo.sub")
                    .withStyle(ChatFormatting.GOLD)));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("title.aidirector.crescendo")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
            IntensityTracker.add(p, 0.2f);
        }
    }

    /**
     * Termina el evento.
     *
     * @param survived si es true, las cajas cerradas cercanas se abren como recompensa
     */
    public static void stop(ServerLevel level, boolean survived) {
        Event e = EVENTS.remove(level);
        if (e == null) return;
        setAlarmActive(level, e.alarm(), false);
        int unlocked = survived ? unlockCrates(level, e.alarm()) : 0;
        for (ServerPlayer p : nearby(level, e.alarm())) {
            p.sendSystemMessage(Component.translatable(survived ? "message.aidirector.crescendo.survived"
                    : "message.aidirector.crescendo.ended").withStyle(ChatFormatting.GOLD));
            if (unlocked > 0) {
                p.sendSystemMessage(Component.translatable("message.aidirector.crescendo.unlocked", unlocked).withStyle(ChatFormatting.GREEN));
            }
        }
        level.playSound(null, e.alarm(), ModSounds.ALARM_STOP.get(), SoundSource.BLOCKS, 2f, 1f);
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        Event e = EVENTS.get(level);
        if (e == null) return;
        long now = level.getGameTime();
        if (!level.isLoaded(e.alarm()) || !(level.getBlockState(e.alarm()).getBlock() instanceof CrescendoAlarmBlock)) {
            stop(level, false);
            return;
        }
        if (now >= e.end()) {
            stop(level, true);
            return;
        }
        if ((now - e.start()) % 30 == 0) {
            level.playSound(null, e.alarm(), ModSounds.ALARM_SIREN.get(), SoundSource.BLOCKS, 5f, 1f);
            level.sendParticles(ParticleTypes.FLAME, e.alarm().getX() + 0.5, e.alarm().getY() + 1.1, e.alarm().getZ() + 0.5, 3, 0.15, 0.05, 0.15, 0.01);
        }
        if (now >= e.nextWave()) {
            AIDirectorConfig.SafeRoomSettings cfg = AIDirectorConfig.safeRoom();
            EVENTS.put(level, e.withNextWave(now + cfg.crescendoWaveSeconds() * 20L));
            List<ServerPlayer> targets = nearby(level, e.alarm()).stream().filter(SpecialInfectedEntity::isSurvivor).toList();
            ServerPlayer target = targets.isEmpty() ? null : targets.get(level.random.nextInt(targets.size()));
            EnhancedHordesCompat.spawnRing(level, e.alarm(), cfg.crescendoWaveSize(), 20, 40, target);
        }
    }

    private static List<ServerPlayer> nearby(ServerLevel level, BlockPos alarm) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().distSqr(alarm) <= EVENT_RADIUS * EVENT_RADIUS) out.add(p);
        }
        return out;
    }

    private static void setAlarmActive(ServerLevel level, BlockPos pos, boolean active) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CrescendoAlarmBlock && state.getValue(CrescendoAlarmBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(CrescendoAlarmBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    /** Abre las cajas con candado de alrededor (recompensa del evento). */
    private static int unlockCrates(ServerLevel level, BlockPos center) {
        int count = 0;
        ChunkPos min = new ChunkPos(center.offset(-REWARD_RADIUS, 0, -REWARD_RADIUS));
        ChunkPos max = new ChunkPos(center.offset(REWARD_RADIUS, 0, REWARD_RADIUS));
        for (int cx = min.x; cx <= max.x; cx++) {
            for (int cz = min.z; cz <= max.z; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : List.copyOf(chunk.getBlockEntities().values())) {
                    if (!(be instanceof SupplyCrateBlockEntity crate) || be.getBlockPos().distSqr(center) > REWARD_RADIUS * REWARD_RADIUS) continue;
                    BlockState state = be.getBlockState();
                    if (!state.getValue(SupplyCrateBlock.LOCKED)) continue;
                    level.setBlock(be.getBlockPos(), state.setValue(SupplyCrateBlock.LOCKED, false), Block.UPDATE_ALL);
                    crate.setReward(true);
                    BlockPos p = be.getBlockPos();
                    level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.2);
                    level.playSound(null, p, ModSounds.CRATE_UNLOCK.get(), SoundSource.BLOCKS, 1f, 1f);
                    count++;
                }
            }
        }
        return count;
    }

    @Nullable
    public static BlockPos alarmOf(ServerLevel level) {
        Event e = EVENTS.get(level);
        return e == null ? null : e.alarm();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        EVENTS.clear();
    }
}
