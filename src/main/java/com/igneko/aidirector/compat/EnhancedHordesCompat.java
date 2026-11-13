package com.igneko.aidirector.compat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class EnhancedHordesCompat {

    public static void spawnHordeForPlayer(Player player) {
        Level level = player.level();
        MinecraftServer server = level.getServer();
        
        if (server != null) {
            CommandSourceStack source = server.createCommandSourceStack()
                .withPosition(player.position())
                .withPermission(4)
                .withSuppressedOutput(); 

            server.getCommands().performPrefixedCommand(source, "horde spawn");
        }
    }
}