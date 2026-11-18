package com.igneko.aidirector.core.survivor;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.entity.SurvivorBodyEntity;
import com.igneko.aidirector.network.ModNetwork;
import com.igneko.aidirector.network.SquadSyncPacket;
import com.igneko.aidirector.registry.ModRegistries;
import com.igneko.aidirector.registry.ModWeapons;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Escuadrón: quién es compañero de quién y sincronización del panel de escuadrón (especificación §10).
 * Si se usan equipos de marcador ({@code /team}), solo cuentan los del mismo equipo.
 */
public final class SquadService {

    private static final int SYNC_INTERVAL = 10;

    public SquadService() {}

    public static boolean areSquadmates(Player a, Player b) {
        if (a == b) return true;
        if (!AIDirectorConfig.squad().sameTeamOnly()) return true;
        Team ta = a.getTeam(), tb = b.getTeam();
        if (ta == null && tb == null) return true;
        return ta != null && ta.isAlliedTo(tb);
    }

    /** Aviso sonoro solo para los compañeros de {@code subject} (cada uno lo oye en su posición). */
    public static void notifySquad(ServerPlayer subject, net.minecraft.sounds.SoundEvent sound) {
        for (ServerPlayer p : subject.server.getPlayerList().getPlayers()) {
            if (p != subject && areSquadmates(p, subject)) p.playNotifySound(sound, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1f);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % SYNC_INTERVAL != 0) return;
        List<ServerPlayer> all = event.getServer().getPlayerList().getPlayers();
        double range = AIDirectorConfig.squad().range();
        for (ServerPlayer viewer : all) {
            List<SquadSyncPacket.Entry> entries = new ArrayList<>();
            for (ServerPlayer mate : all) {
                if (mate == viewer || mate.level() != viewer.level() || mate.isSpectator()) continue;
                if (!areSquadmates(viewer, mate)) continue;
                Vec3 pos = markerPosition(mate);
                if (pos.distanceToSqr(viewer.position()) > range * range) continue;
                entries.add(entry(mate, pos));
                if (entries.size() >= 15) break;
            }
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), new SquadSyncPacket(entries));
        }
    }

    /** Posición del compañero; si está muerto, la de su cuerpo. */
    private static Vec3 markerPosition(ServerPlayer mate) {
        if (mate.isDeadOrDying()) {
            SurvivorBodyEntity body = DefibService.bodyOf(mate);
            if (body != null) return body.position();
        }
        return mate.position();
    }

    private static SquadSyncPacket.Entry entry(ServerPlayer mate, Vec3 pos) {
        SurvivorHealthData d = SurvivorCapability.get(mate).orElse(null);
        SurvivorRules rules = AIDirectorConfig.rules();
        SquadSyncPacket.Status status;
        if (mate.isDeadOrDying() || (d != null && d.isDead())) status = SquadSyncPacket.Status.DEAD;
        else if (d != null && d.isIncapacitated()) status = SquadSyncPacket.Status.DOWNED;
        else if (d != null && d.isPinned()) status = SquadSyncPacket.Status.PINNED;
        else status = SquadSyncPacket.Status.ALIVE;

        float perm = d == null ? mate.getHealth() * 5f : (d.isIncapacitated() ? d.bleedHealth() * 100f / rules.bleedPool() : d.permHealth());
        float temp = d == null || d.isIncapacitated() ? 0f : d.tempHealth();
        boolean bw = d != null && d.isBlackAndWhite(rules);
        boolean managed = SurvivorService.isManaged(mate);
        return new SquadSyncPacket.Entry(mate.getUUID(), mate.getGameProfile().getName(), status, perm, temp, bw,
                d != null && d.isBeingRevived(), managed,
                slot(mate, ModRegistries.FIRST_AID_KIT.get(), ModRegistries.DEFIBRILLATOR.get()),
                slot(mate, ModRegistries.PAIN_PILLS.get(), ModRegistries.ADRENALINE.get()),
                slot(mate, ModWeapons.PIPE_BOMB.get(), ModWeapons.MOLOTOV.get(), ModWeapons.BILE_JAR.get()),
                (float) pos.x, (float) pos.y, (float) pos.z);
    }

    /** Índice (1..n) del primer objeto de la lista que lleva el jugador, o 0. */
    private static byte slot(Player player, Item... items) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) continue;
            for (int i = 0; i < items.length; i++) {
                if (stack.is(items[i])) return (byte) (i + 1);
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            for (int i = 0; i < items.length; i++) {
                if (stack.is(items[i])) return (byte) (i + 1);
            }
        }
        return 0;
    }
}
