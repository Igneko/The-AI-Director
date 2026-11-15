package com.igneko.aidirector.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S2C: un empujón se ejecutó; el cliente dibuja el indicador de recarga en la mira. */
public record ShoveCooldownPacket(int cooldownTicks, boolean fatigued) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(cooldownTicks);
        buf.writeBoolean(fatigued);
    }

    public static ShoveCooldownPacket decode(FriendlyByteBuf buf) {
        return new ShoveCooldownPacket(buf.readVarInt(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.handleShoveCooldown(this));
        ctx.get().setPacketHandled(true);
    }
}
