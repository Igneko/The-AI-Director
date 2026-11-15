package com.igneko.aidirector.network;

import com.igneko.aidirector.core.TheAIDirector;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Canal de red único del mod. Todo paquete S2C es informativo; el servidor es la autoridad. */
public final class ModNetwork {

    private static final String PROTOCOL = "3";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TheAIDirector.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static int nextId = 0;

    private ModNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(SyncSurvivorPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSurvivorPacket::encode)
                .decoder(SyncSurvivorPacket::decode)
                .consumerMainThread(SyncSurvivorPacket::handle)
                .add();
        CHANNEL.messageBuilder(ShoveCooldownPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ShoveCooldownPacket::encode)
                .decoder(ShoveCooldownPacket::decode)
                .consumerMainThread(ShoveCooldownPacket::handle)
                .add();
        CHANNEL.messageBuilder(ShoveRequestPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ShoveRequestPacket::encode)
                .decoder(ShoveRequestPacket::decode)
                .consumerMainThread(ShoveRequestPacket::handle)
                .add();
        CHANNEL.messageBuilder(UseHoldPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UseHoldPacket::encode)
                .decoder(UseHoldPacket::decode)
                .consumerMainThread(UseHoldPacket::handle)
                .add();
    }
}
