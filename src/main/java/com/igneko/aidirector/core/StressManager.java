package com.igneko.aidirector.core;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StressManager {
    
    // 20 minutos en ticks (20 min * 60 seg * 20 ticks) = 24000 ticks
    public static final long DEFAULT_HORDE_COOLDOWN = 24000L; 
    
    // Almacena el timestamp exacto (GameTime) en el que la horda atacará a cada jugador
    private static final Map<UUID, Long> nextHordeTimes = new HashMap<>();

    /**
     * Se llama cuando el jugador respawnea o cuando una horda ya fue derrotada.
     */
    public static void resetTimer(Player player) {
        Level level = player.level();
        if (level.isClientSide) return;
        
        long currentTime = level.getGameTime();
        nextHordeTimes.put(player.getUUID(), currentTime + DEFAULT_HORDE_COOLDOWN);
    }

    /**
     * Resta segundos reales al reloj (ej. por disparar). 
     * 1 segundo real = 20 ticks.
     */
    public static void deductTime(Player player, int secondsToDeduct) {
        Level level = player.level();
        if (level.isClientSide) return;

        UUID id = player.getUUID();
        if (!nextHordeTimes.containsKey(id)) {
            resetTimer(player);
        }

        long targetTime = nextHordeTimes.get(id);
        long ticksToDeduct = secondsToDeduct * 20L;
        
        // Adelantamos la llegada de la horda restando ticks al targetTime
        nextHordeTimes.put(id, targetTime - ticksToDeduct);
    }

    /**
     * Método que debe llamarse cada tick (o cada 20 ticks) desde un ServerTickEvent.
     * Evalúa si ya es hora de la horda.
     */
    public static void tickPlayer(Player player) {
        Level level = player.level();
        if (level.isClientSide) return;

        UUID id = player.getUUID();
        if (!nextHordeTimes.containsKey(id)) {
            resetTimer(player);
            return;
        }

        long currentTime = level.getGameTime();
        long targetTime = nextHordeTimes.get(id);

        // Aceleradores (Multiplicadores)
        if (player.getHealth() < 8.0f) { // Menos de 4 corazones
            // El reloj avanza el doble de rápido restando un tick extra al objetivo
            targetTime -= 1; 
            nextHordeTimes.put(id, targetTime);
        }

        // ¿Ya es hora de la horda?
        if (currentTime >= targetTime) {
            com.igneko.aidirector.compat.enhancedhordes.EnhancedHordesCompat.spawnHordeForPlayer(player);
            
            // Notificamos al jugador y reseteamos el reloj para la siguiente oleada
            player.displayClientMessage(Component.literal("§c[AI Director] ¡Viene una horda!"), true);
            resetTimer(player);
        }
    }

    /**
     * Para comandos de debug: Devuelve los segundos restantes
     */
    public static long getSecondsLeft(Player player) {
        if (!nextHordeTimes.containsKey(player.getUUID())) return -1;
        long diffTicks = nextHordeTimes.get(player.getUUID()) - player.level().getGameTime();
        return Math.max(0, diffTicks / 20L);
    }
}