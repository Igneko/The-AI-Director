package com.igneko.aidirector.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

// TODO: Importar la API real de Enhanced Hordes cuando VS Code lea el .jar de la carpeta libs
// import dev.ceymikey.enhancedhordes.api.HordeAPI; 

public class EnhancedHordesCompat {

    public static void spawnHordeForPlayer(Player player) {
        // Enlazar el StressManager con la API de Enhanced Hordes[cite: 2]
        // TODO: Descomentar y usar el método correcto del mod para forzar la generación de la horda
        // HordeAPI.spawnHordeAt(player.blockPosition(), player.level());
        
        // Mensaje en pantalla para el jugador notificando el evento de alta tensión
        player.displayClientMessage(Component.literal("§c[The AI Director] El estrés llegó al límite. ¡Horda desencadenada!"), true);
    }
}