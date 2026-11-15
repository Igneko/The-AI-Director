package com.igneko.aidirector.network;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.survivor.ReviveManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: se mantiene pulsada la tecla de interacción del mod apuntando a una entidad. Se repite cada
 * pocos ticks; permite reanimar aunque TaCZ ocupe el clic derecho para apuntar.
 */
public record UseHoldPacket(int entityId) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
    }

    public static UseHoldPacket decode(FriendlyByteBuf buf) {
        return new UseHoldPacket(buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            Entity target = sender.level().getEntity(entityId);
            double maxDist = AIDirectorConfig.reviveRange() + 0.5;
            if (target instanceof ServerPlayer downed && downed != sender
                    && sender.distanceToSqr(downed) <= maxDist * maxDist && sender.hasLineOfSight(downed)) {
                ReviveManager.onInteract(sender, downed);
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
