package com.igneko.aidirector.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;

public class EnhancedHordesCompat {

    public static void forceHorde(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server != null) {
            // Reemplaza "horde spawn" por el comando exacto que usa Enhanced Hordes.
            // Para averiguarlo, entra al juego, presiona la tecla "/" y escribe "horde" o "enhanced"
            // para ver las opciones que el mod añade.
            String command = "horde spawn"; 
            
            // Crea un ejecutor invisible con nivel de administrador (4)
            CommandSourceStack source = server.createCommandSourceStack()
                .withPermission(4)
                .withSuppressedOutput();
                
            // El servidor ejecuta el comando de forma silenciosa
            server.getCommands().performPrefixedCommand(source, command);
        }
    }
}