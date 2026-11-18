package com.igneko.aidirector.core.survivor;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.director.SegmentStats;
import com.igneko.aidirector.entity.SurvivorBodyEntity;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Muerte y regreso de los supervivientes: deja un cuerpo al morir y lo reanima con el desfibrilador
 * (o al asegurar un refugio). El jugador muerto sigue en la pantalla de muerte; al reanimarlo se le
 * hace reaparecer exactamente sobre su cuerpo con 50 de salud permanente.
 */
public final class DefibService {

    private static final Map<UUID, SurvivorBodyEntity> BODIES = new HashMap<>();
    /** Jugadores que están reapareciendo por un desfibrilador (no hay que borrar su cuerpo antes de tiempo). */
    private static final Set<UUID> REVIVING = new HashSet<>();

    public DefibService() {}

    @Nullable
    public static SurvivorBodyEntity bodyOf(ServerPlayer player) {
        SurvivorBodyEntity body = BODIES.get(player.getUUID());
        return body != null && !body.isRemoved() ? body : null;
    }

    public static void track(SurvivorBodyEntity body) {
        UUID owner = body.owner();
        if (owner != null && BODIES.get(owner) != body) BODIES.put(owner, body);
    }

    public static void untrack(SurvivorBodyEntity body) {
        UUID owner = body.owner();
        if (owner != null && BODIES.get(owner) == body) BODIES.remove(owner);
    }

    /** Al morir de verdad (no al quedar incapacitado: ese evento se cancela antes). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SurvivorService.isManaged(player)) return;
        player.playNotifySound(ModSounds.PLAYER_DIED.get(), SoundSource.PLAYERS, 1f, 1f);
        SquadService.notifySquad(player, ModSounds.TEAMMATE_DIED.get());
        SurvivorBodyEntity old = bodyOf(player);
        if (old != null) old.discard();
        SurvivorBodyEntity body = SurvivorBodyEntity.of(player);
        if (player.serverLevel().addFreshEntity(body)) track(body);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (REVIVING.contains(event.getEntity().getUUID()) || !(event.getEntity() instanceof ServerPlayer player)) return;
        SurvivorBodyEntity body = bodyOf(player);
        if (body != null) body.discard();
    }

    /**
     * Devuelve a la vida a un superviviente muerto en {@code pos}.
     *
     * @param by quien usó el desfibrilador (para las estadísticas), o null si fue un rescate del refugio
     * @return false si el jugador no estaba muerto o ya no está conectado
     */
    public static boolean revive(ServerPlayer dead, ServerLevel level, Vec3 pos, float yaw, @Nullable ServerPlayer by) {
        if (!dead.isDeadOrDying() || dead.hasDisconnected()) return false;

        // Reaparición forzada sobre el cuerpo, conservando su punto de reaparición de siempre.
        ResourceKey<Level> oldDim = dead.getRespawnDimension();
        BlockPos oldPos = dead.getRespawnPosition();
        float oldAngle = dead.getRespawnAngle();
        boolean oldForced = dead.isRespawnForced();
        SurvivorBodyEntity body = bodyOf(dead);

        ServerPlayer fresh;
        REVIVING.add(dead.getUUID());
        try {
            dead.setRespawnPosition(level.dimension(), BlockPos.containing(pos), yaw, true, false);
            fresh = level.getServer().getPlayerList().respawn(dead, false);
        } finally {
            REVIVING.remove(dead.getUUID());
        }
        fresh.setRespawnPosition(oldDim, oldPos, oldAngle, oldForced, false);
        fresh.teleportTo(level, pos.x, pos.y, pos.z, yaw, 0f);

        float health = Math.min(AIDirectorConfig.rules().maxHealth(), AIDirectorConfig.items().defibHealth());
        SurvivorCapability.get(fresh).ifPresent(d -> {
            d.reset(AIDirectorConfig.rules());
            d.restore(health, 0f, SurvivorState.ALIVE, 0, 0f, 0, 0f, false);
            SurvivorService.applyEffects(fresh, d, true);
            SurvivorService.sync(fresh, d);
        });
        if (body != null) body.discard();

        level.playSound(null, pos.x, pos.y, pos.z, ModSounds.DEFIB_SHOCK.get(), SoundSource.PLAYERS, 1.2f, 1f);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.6, pos.z, 30, 0.4, 0.4, 0.4, 0.4);
        if (by != null) {
            SegmentStats.onRevive(by);
            fresh.sendSystemMessage(Component.translatable("message.aidirector.defib.revived_by", by.getDisplayName())
                    .withStyle(ChatFormatting.GREEN));
            by.displayClientMessage(Component.translatable("message.aidirector.defib.done", fresh.getDisplayName())
                    .withStyle(ChatFormatting.GREEN), true);
        } else {
            fresh.sendSystemMessage(Component.translatable("message.aidirector.saferoom.rescued_you").withStyle(ChatFormatting.AQUA));
        }
        return true;
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        BODIES.clear();
        REVIVING.clear();
    }
}
