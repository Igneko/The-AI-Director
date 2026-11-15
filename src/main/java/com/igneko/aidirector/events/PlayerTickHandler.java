package com.igneko.aidirector.events;

import com.igneko.aidirector.core.StressManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerTickHandler {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Ejecutar solo en el servidor y solo 1 vez por tick (evita doble conteo)
        if (event.side.isServer() && event.phase == TickEvent.Phase.END) {
            Player player = event.player;
            // Solo procesamos a jugadores vivos y en modo Supervivencia/Aventura
            if (player.isAlive() && !player.isSpectator() && !player.isCreative()) {
                StressManager.tickPlayer(player);
            }
        }
    }
}