package com.igneko.aidirector.network;

import com.igneko.aidirector.core.survivor.SurvivorHealthData;
import com.igneko.aidirector.core.survivor.SurvivorState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S2C: copia del estado de salud de un superviviente (para su dueño y para quien lo rastrea). */
public record SyncSurvivorPacket(int entityId, float perm, float temp, SurvivorState state, int incapCount,
                                 float bleed, int adrenalineTicks, float reviveProgress, boolean beingRevived) {

    public static SyncSurvivorPacket of(int entityId, SurvivorHealthData d) {
        return new SyncSurvivorPacket(entityId, d.permHealth(), d.tempHealth(), d.state(), d.incapCount(),
                d.bleedHealth(), d.adrenalineTicks(), d.reviveProgress(), d.isBeingRevived());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeFloat(perm);
        buf.writeFloat(temp);
        buf.writeEnum(state);
        buf.writeVarInt(incapCount);
        buf.writeFloat(bleed);
        buf.writeVarInt(adrenalineTicks);
        buf.writeFloat(reviveProgress);
        buf.writeBoolean(beingRevived);
    }

    public static SyncSurvivorPacket decode(FriendlyByteBuf buf) {
        return new SyncSurvivorPacket(buf.readVarInt(), buf.readFloat(), buf.readFloat(),
                buf.readEnum(SurvivorState.class), buf.readVarInt(), buf.readFloat(), buf.readVarInt(),
                buf.readFloat(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleSurvivorSync(this));
        ctx.get().setPacketHandled(true);
    }
}
