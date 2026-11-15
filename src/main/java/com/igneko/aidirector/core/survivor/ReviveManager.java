package com.igneko.aidirector.core.survivor;

import com.igneko.aidirector.config.AIDirectorConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Sesiones de reanimación (solo servidor). El cliente repite la interacción con la entidad cada
 * ~4 ticks mientras se mantiene pulsado clic derecho; si deja de llegar durante
 * {@link #HOLD_TIMEOUT_TICKS}, la reanimación se cancela.
 */
public final class ReviveManager {

    private static final int HOLD_TIMEOUT_TICKS = 8;

    private record Session(UUID target, long lastInteractTick) {}

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private ReviveManager() {}

    /** Llamado en cada interacción del reanimador con el caído. Devuelve true si se consumió la interacción. */
    public static boolean onInteract(ServerPlayer reviver, ServerPlayer target) {
        if (!SurvivorService.isManaged(reviver)) return false;
        SurvivorHealthData targetData = SurvivorCapability.get(target).orElse(null);
        SurvivorHealthData reviverData = SurvivorCapability.get(reviver).orElse(null);
        if (targetData == null || reviverData == null) return false;
        if (!targetData.isIncapacitated() || !reviverData.isAlive()) return false;

        Session existing = SESSIONS.get(reviver.getUUID());
        if (existing != null && !existing.target().equals(target.getUUID())) {
            cancel(reviver.server, reviver.getUUID());
        }
        if (!targetData.beginRevive(reviver.getUUID())) {
            reviver.displayClientMessage(Component.translatable("message.aidirector.revive.busy"), true);
            return true;
        }
        SESSIONS.put(reviver.getUUID(), new Session(target.getUUID(), reviver.level().getGameTime()));
        return true;
    }

    /** Un tick de servidor: avanza o cancela cada sesión. */
    public static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) return;
        double maxRangeSq = AIDirectorConfig.reviveRange() * AIDirectorConfig.reviveRange();
        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> entry = it.next();
            ServerPlayer reviver = server.getPlayerList().getPlayer(entry.getKey());
            ServerPlayer target = server.getPlayerList().getPlayer(entry.getValue().target());
            SurvivorHealthData targetData = target == null ? null : SurvivorCapability.get(target).orElse(null);
            SurvivorHealthData reviverData = reviver == null ? null : SurvivorCapability.get(reviver).orElse(null);

            boolean valid = reviver != null && target != null && targetData != null && reviverData != null
                    && reviver.level() == target.level()
                    && reviverData.isAlive() && targetData.isIncapacitated()
                    && entry.getKey().equals(targetData.reviverId())
                    && reviver.distanceToSqr(target) <= maxRangeSq
                    && reviver.level().getGameTime() - entry.getValue().lastInteractTick() <= HOLD_TIMEOUT_TICKS;

            if (!valid) {
                it.remove();
                if (targetData != null && entry.getKey().equals(targetData.reviverId())) {
                    targetData.cancelRevive();
                    SurvivorService.sync(target, targetData);
                }
                continue;
            }

            if (targetData.advanceRevive(AIDirectorConfig.rules(), reviverData.hasAdrenaline())) {
                it.remove();
                SurvivorService.applyEffects(target, targetData);
                reviver.displayClientMessage(Component.translatable("message.aidirector.revive.done", target.getDisplayName()), true);
                // TODO Fase 3: estadísticas de tramo (reanimaciones) para el refugio.
            }
            // El progreso se envía cada 2 ticks para que la barra sea fluida sin saturar la red.
            if (targetData.consumeDirty() || reviver.level().getGameTime() % 2 == 0) {
                SurvivorService.sync(target, targetData);
            }
        }
    }

    /** Interrumpe la reanimación que esté realizando este jugador (empujón, agarre, daño...). */
    public static void cancel(MinecraftServer server, UUID reviverId) {
        Session session = SESSIONS.remove(reviverId);
        if (session == null) return;
        ServerPlayer target = server.getPlayerList().getPlayer(session.target());
        if (target == null) return;
        SurvivorCapability.get(target).ifPresent(d -> {
            if (reviverId.equals(d.reviverId())) {
                d.cancelRevive();
                SurvivorService.sync(target, d);
            }
        });
    }

    /** Cancela sesiones en las que el jugador participa como reanimador o como caído. */
    public static void cancelAllInvolving(ServerPlayer player) {
        cancel(player.server, player.getUUID());
        SESSIONS.entrySet().removeIf(e -> e.getValue().target().equals(player.getUUID()));
        SurvivorCapability.get(player).ifPresent(SurvivorHealthData::cancelRevive);
    }

    public static boolean isReviving(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    public static void clear() {
        SESSIONS.clear();
    }
}
