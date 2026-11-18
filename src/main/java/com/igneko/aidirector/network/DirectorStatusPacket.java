package com.igneko.aidirector.network;

import com.igneko.aidirector.core.director.DirectorFsm;
import com.igneko.aidirector.world.saferoom.SafeZoneManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: estado del refugio en el que está el jugador (con el recuento del equipo), segundos que
 * quedan del evento de pánico cercano (-1 si no hay) y fase del Director.
 */
public record DirectorStatusPacket(SafeZoneManager.ZoneStatus zone, int inside, int team, int crescendoSeconds,
                                   DirectorFsm.Phase phase) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(zone);
        buf.writeVarInt(inside);
        buf.writeVarInt(team);
        buf.writeVarInt(crescendoSeconds + 1);
        buf.writeEnum(phase);
    }

    public static DirectorStatusPacket decode(FriendlyByteBuf buf) {
        return new DirectorStatusPacket(buf.readEnum(SafeZoneManager.ZoneStatus.class), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt() - 1, buf.readEnum(DirectorFsm.Phase.class));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleDirectorStatus(this));
        ctx.get().setPacketHandled(true);
    }
}
