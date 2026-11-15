package com.igneko.aidirector.client;

import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.core.survivor.SurvivorCapability;
import com.igneko.aidirector.core.survivor.SurvivorHealthData;
import com.igneko.aidirector.network.ModNetwork;
import com.igneko.aidirector.network.ShoveRequestPacket;
import com.igneko.aidirector.network.UseHoldPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Traduce la entrada del jugador local en peticiones al servidor. */
@Mod.EventBusSubscriber(modid = TheAIDirector.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientInputHandler {

    /** Cada cuántos ticks se repite la petición de interacción mientras se mantiene la tecla. */
    private static final int USE_REPEAT_TICKS = 4;
    private static int useTicker;

    private ClientInputHandler() {}

    private static boolean isDowned(LocalPlayer player) {
        return !player.isCreative() && !player.isSpectator()
                && SurvivorCapability.get(player).map(SurvivorHealthData::isIncapacitated).orElse(false);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) {
            useTicker = 0;
            return;
        }

        while (ModKeyMappings.SHOVE.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new ShoveRequestPacket());
        }

        if (ModKeyMappings.USE.isDown() && mc.crosshairPickEntity instanceof Player target) {
            if (useTicker-- <= 0) {
                ModNetwork.CHANNEL.sendToServer(new UseHoldPacket(target.getId()));
                useTicker = USE_REPEAT_TICKS - 1;
            }
        } else {
            useTicker = 0;
        }
        while (ModKeyMappings.USE.consumeClick()) { /* el estado se lee con isDown() */ }
    }

    /** Estando incapacitado no se puede cambiar de arma con la rueda (el servidor también lo impone). */
    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && Minecraft.getInstance().screen == null && isDowned(player)) event.setCanceled(true);
    }
}
