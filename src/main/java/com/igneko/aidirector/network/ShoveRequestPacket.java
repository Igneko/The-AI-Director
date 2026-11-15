package com.igneko.aidirector.network;

import com.igneko.aidirector.core.combat.ShoveService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S: el jugador pulsó la tecla de empujón. El servidor valida fatiga, estado y objetivos. */
public record ShoveRequestPacket() {

    public void encode(FriendlyByteBuf buf) {}

    public static ShoveRequestPacket decode(FriendlyByteBuf buf) {
        return new ShoveRequestPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) ShoveService.tryShove(sender);
        ctx.get().setPacketHandled(true);
    }
}
