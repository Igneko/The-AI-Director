package com.igneko.aidirector.core.survivor;

import com.igneko.aidirector.api.event.ShoveEvent;
import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.registry.ModRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Estado PINNED: un Infectado Especial inmoviliza a un superviviente. Solo se libera si un compañero
 * mata al infectado o lo empuja (o empuja a la víctima en arrastres y monturas). En solitario, la
 * víctima puede forcejear con la tecla de empujón tras una ventana de reacción.
 *
 * <p>Regla crítica: si todos los supervivientes vivos están incapacitados o atrapados a la vez,
 * el equipo cae (Team Wipe).</p>
 */
public final class PinService {

    /** Interfaz que implementan los infectados que atrapan, para reaccionar a la liberación. */
    public interface Pinner {
        void onVictimReleased(ServerPlayer victim);
    }

    private record Pin(Mob pinner, long startTick) {}

    private static final Map<UUID, Pin> PINS = new HashMap<>();
    /** Desmontajes autorizados (la liberación usa stopRiding, que de otro modo se cancelaría). */
    private static final Set<UUID> RELEASING = new HashSet<>();

    private static final int SOLO_STRUGGLE_TICKS = 30; // 1,5 s

    public PinService() {}

    // ------------------------------------------------------------------ consultas

    public static boolean isPinned(Player player) {
        return SurvivorCapability.get(player).map(SurvivorHealthData::isPinned).orElse(false);
    }

    public static PinType pinType(Player player) {
        return SurvivorCapability.get(player).map(SurvivorHealthData::pinType).orElse(PinType.NONE);
    }

    public static boolean canShoot(Player player) {
        PinType type = pinType(player);
        return type == PinType.NONE || type.canShoot();
    }

    @Nullable
    public static Mob pinnerOf(ServerPlayer victim) {
        Pin pin = PINS.get(victim.getUUID());
        return pin == null ? null : pin.pinner();
    }

    @Nullable
    public static ServerPlayer victimOf(Mob pinner) {
        if (pinner.getServer() == null) return null;
        for (Map.Entry<UUID, Pin> e : PINS.entrySet()) {
            if (e.getValue().pinner() == pinner) return pinner.getServer().getPlayerList().getPlayer(e.getKey());
        }
        return null;
    }

    // ------------------------------------------------------------------ comandos

    /** Atrapa a un superviviente. Devuelve false si no es válido o ya está atrapado por otro. */
    public static boolean pin(ServerPlayer victim, Mob pinner, PinType type) {
        if (!SurvivorService.isManaged(victim) || !victim.isAlive()) return false;
        SurvivorHealthData data = SurvivorCapability.get(victim).orElse(null);
        if (data == null || data.isDead()) return false;
        Pin existing = PINS.get(victim.getUUID());
        if (existing != null && existing.pinner() != pinner) return false;

        PINS.put(victim.getUUID(), new Pin(pinner, victim.level().getGameTime()));
        data.setPin(pinner.getId(), type);
        ReviveManager.cancelAllInvolving(victim);
        victim.stopUsingItem();
        SurvivorService.applyEffects(victim, data);
        SurvivorService.sync(victim, data);
        // TODO Fase 3: notificar al AI Director (sube la intensidad del equipo).
        return true;
    }

    /** Cambia el tipo de agarre (p. ej. arrastre → estrangulamiento). */
    public static void setType(ServerPlayer victim, PinType type) {
        Pin pin = PINS.get(victim.getUUID());
        if (pin == null) return;
        SurvivorCapability.get(victim).ifPresent(d -> {
            d.setPin(pin.pinner().getId(), type);
            SurvivorService.applyEffects(victim, d);
            SurvivorService.sync(victim, d);
        });
    }

    public static void release(ServerPlayer victim) {
        Pin pin = PINS.remove(victim.getUUID());
        SurvivorCapability.get(victim).ifPresent(d -> {
            if (!d.isPinned()) return;
            d.clearPin();
            SurvivorService.applyEffects(victim, d);
            SurvivorService.sync(victim, d);
        });
        if (pin == null) return;
        Mob pinner = pin.pinner();
        if (victim.getVehicle() == pinner) dismount(victim);
        if (pinner.getVehicle() == victim) dismount(pinner);
        if (pinner instanceof Pinner p) p.onVictimReleased(victim);
    }

    public static void releaseAllBy(Mob pinner) {
        if (pinner.getServer() == null) return;
        List<UUID> victims = new ArrayList<>();
        PINS.forEach((id, pin) -> { if (pin.pinner() == pinner) victims.add(id); });
        for (UUID id : victims) {
            ServerPlayer victim = pinner.getServer().getPlayerList().getPlayer(id);
            if (victim != null) release(victim);
            else PINS.remove(id);
        }
    }

    private static void dismount(Entity rider) {
        RELEASING.add(rider.getUUID());
        try {
            rider.stopRiding();
        } finally {
            RELEASING.remove(rider.getUUID());
        }
    }

    /** Forcejeo en solitario: tras 1,5 s atrapado, la tecla de empujón libera a la víctima. */
    public static boolean tryStruggle(ServerPlayer victim) {
        Pin pin = PINS.get(victim.getUUID());
        if (pin == null || countSurvivors(victim.server) > 1) return false;
        if (victim.level().getGameTime() - pin.startTick() < SOLO_STRUGGLE_TICKS) return false;
        Mob pinner = pin.pinner();
        release(victim);
        if (!pinner.getType().is(ModRegistries.SHOVE_IMMUNE)) {
            pinner.addEffect(new MobEffectInstance(ModRegistries.STAGGERED.get(), AIDirectorConfig.combat().staggerTicks(),
                    0, false, false, true));
        }
        pinner.knockback(0.8, pinner.getX() - victim.getX() < 0 ? 1 : -1, 0);
        victim.displayClientMessage(Component.translatable("message.aidirector.pin.struggle"), true);
        return true;
    }

    // ------------------------------------------------------------------ eventos

    /** Empujar al infectado (o a la víctima en arrastres/monturas) la libera. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onShove(ShoveEvent event) {
        if (event.getTarget() instanceof Mob mob && !mob.getType().is(ModRegistries.SHOVE_IMMUNE)) {
            ServerPlayer victim = victimOf(mob);
            if (victim != null) release(victim);
        } else if (event.getTarget() instanceof ServerPlayer victim) {
            PinType type = pinType(victim);
            if (type == PinType.DRAG || type == PinType.CHOKE || type == PinType.RIDE) release(victim);
        }
    }

    /** Las víctimas no pueden bajarse del infectado (ni el Parásito de su víctima) por su cuenta. */
    @SubscribeEvent
    public void onMount(EntityMountEvent event) {
        if (!event.isDismounting() || RELEASING.contains(event.getEntityMounting().getUUID())) return;
        Entity rider = event.getEntityMounting();
        Entity vehicle = event.getEntityBeingMounted();
        boolean victimRiding = rider instanceof Player p && isPinned(p) && vehicle instanceof Mob;
        boolean pinnerRiding = vehicle instanceof Player p && isPinned(p) && rider instanceof Mob;
        if ((victimRiding || pinnerRiding) && rider.isAlive() && vehicle.isAlive()) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (!PINS.isEmpty()) validate(server);
        if (server.getTickCount() % 10 == 0) checkTeamWipe(server);
    }

    private static void validate(MinecraftServer server) {
        for (UUID id : new ArrayList<>(PINS.keySet())) {
            Pin pin = PINS.get(id);
            ServerPlayer victim = server.getPlayerList().getPlayer(id);
            if (victim == null) {
                PINS.remove(id);
                continue;
            }
            Mob pinner = pin.pinner();
            boolean valid = pinner.isAlive() && !pinner.isRemoved() && pinner.level() == victim.level()
                    && victim.isAlive() && SurvivorService.isManaged(victim)
                    && pinner.distanceToSqr(victim) < 40 * 40;
            if (!valid) release(victim);
        }
    }

    // ------------------------------------------------------------------ derrota del equipo

    private static List<ServerPlayer> survivors(MinecraftServer server) {
        List<ServerPlayer> list = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!SurvivorService.isManaged(p) || !p.isAlive()) continue;
            SurvivorCapability.get(p).filter(d -> !d.isDead()).ifPresent(d -> list.add(p));
        }
        return list;
    }

    private static int countSurvivors(MinecraftServer server) {
        return survivors(server).size();
    }

    private static void checkTeamWipe(MinecraftServer server) {
        AIDirectorConfig.InfectedSettings cfg = AIDirectorConfig.infected();
        if (!cfg.teamWipe()) return;
        List<ServerPlayer> alive = survivors(server);
        if (alive.size() < cfg.teamWipeMinPlayers()) return;
        boolean allDown = alive.stream().allMatch(p -> SurvivorCapability.get(p)
                .map(d -> d.isIncapacitated() || d.isPinned()).orElse(false));
        if (!allDown) return;

        Component title = Component.translatable("message.aidirector.team_wipe");
        for (ServerPlayer p : alive) {
            p.connection.send(new ClientboundSetTitleTextPacket(title));
            SurvivorCapability.get(p).ifPresent(SurvivorHealthData::forceDead);
            release(p);
            SurvivorService.killForReal(p);
        }
    }

    public static void clear() {
        PINS.clear();
        RELEASING.clear();
    }

    /** Para el HUD del Director: ¿cuántos supervivientes siguen en pie? */
    public static Optional<Integer> survivorsStanding(MinecraftServer server) {
        return Optional.of((int) survivors(server).stream()
                .filter(p -> SurvivorCapability.get(p).map(d -> d.isAlive() && !d.isPinned()).orElse(false)).count());
    }
}
