package com.igneko.aidirector.client;

import com.igneko.aidirector.core.director.DirectorFsm;
import com.igneko.aidirector.network.DirectorStatusPacket;
import com.igneko.aidirector.network.SquadSyncPacket;
import com.igneko.aidirector.world.saferoom.SafeZoneManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.UUID;

/** Copia en el cliente del escuadrón y del estado del refugio / evento de pánico. */
@OnlyIn(Dist.CLIENT)
public final class ClientSquadState {

    private static List<SquadSyncPacket.Entry> squad = List.of();
    private static DirectorStatusPacket status = new DirectorStatusPacket(SafeZoneManager.ZoneStatus.NONE, 0, 0, -1,
            DirectorFsm.Phase.BUILD_UP);

    private ClientSquadState() {}

    public static void update(List<SquadSyncPacket.Entry> entries) {
        squad = List.copyOf(entries);
    }

    public static void updateStatus(DirectorStatusPacket packet) {
        status = packet;
    }

    public static List<SquadSyncPacket.Entry> squad() {
        return squad;
    }

    public static SquadSyncPacket.Entry find(UUID id) {
        for (SquadSyncPacket.Entry e : squad) if (e.id().equals(id)) return e;
        return null;
    }

    public static DirectorStatusPacket status() {
        return status;
    }

    public static void clear() {
        squad = List.of();
        status = new DirectorStatusPacket(SafeZoneManager.ZoneStatus.NONE, 0, 0, -1, DirectorFsm.Phase.BUILD_UP);
    }
}
