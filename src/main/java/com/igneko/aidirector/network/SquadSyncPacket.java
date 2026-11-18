package com.igneko.aidirector.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** S2C: estado de los compañeros de escuadrón para el panel, las siluetas y los marcadores. */
public record SquadSyncPacket(List<Entry> entries) {

    public enum Status { ALIVE, PINNED, DOWNED, DEAD }

    /**
     * @param health   salud permanente (o porcentaje de sangrado si está en el suelo), escala 0-100
     * @param medical  1 botiquín, 2 desfibrilador
     * @param temp     1 píldoras, 2 adrenalina
     * @param thrown   1 bomba casera, 2 molotov, 3 bilis
     */
    public record Entry(UUID id, String name, Status status, float health, float tempHealth, boolean blackAndWhite,
                        boolean beingRevived, boolean managed, byte medical, byte temp, byte thrown,
                        float x, float y, float z) {}

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (Entry e : entries) {
            buf.writeUUID(e.id());
            buf.writeUtf(e.name(), 64);
            buf.writeEnum(e.status());
            buf.writeFloat(e.health());
            buf.writeFloat(e.tempHealth());
            buf.writeByte((e.blackAndWhite() ? 1 : 0) | (e.beingRevived() ? 2 : 0) | (e.managed() ? 4 : 0));
            buf.writeByte(e.medical());
            buf.writeByte(e.temp());
            buf.writeByte(e.thrown());
            buf.writeFloat(e.x());
            buf.writeFloat(e.y());
            buf.writeFloat(e.z());
        }
    }

    public static SquadSyncPacket decode(FriendlyByteBuf buf) {
        int n = Math.min(buf.readVarInt(), 64);
        List<Entry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            UUID id = buf.readUUID();
            String name = buf.readUtf(64);
            Status status = buf.readEnum(Status.class);
            float health = buf.readFloat();
            float temp = buf.readFloat();
            int flags = buf.readByte();
            list.add(new Entry(id, name, status, health, temp, (flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0,
                    buf.readByte(), buf.readByte(), buf.readByte(), buf.readFloat(), buf.readFloat(), buf.readFloat()));
        }
        return new SquadSyncPacket(list);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleSquad(this));
        ctx.get().setPacketHandled(true);
    }
}
