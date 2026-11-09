package com.igneko.aidirector.core;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = TheAIDirector.MOD_ID)
public class StressManager {

    private static float playerStress = 0.0f;
    private static final float MAX_STRESS = 100.0f;

    @SubscribeEvent
    public static void onPlayerTakeDamage(LivingDamageEvent event) {
        // Extraemos al jugador para pasarlo al método addStress
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            addStress(event.getAmount() * 2.5f, player); 
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
            Player player = event.player;
            AABB scanArea = player.getBoundingBox().inflate(10.0D);
            List<Mob> nearbyMobs = player.level().getEntitiesOfClass(Mob.class, scanArea);

            if (nearbyMobs.size() > 3) {
                addStress(0.05f * nearbyMobs.size(), player);
            } 
            else if (nearbyMobs.isEmpty() && player.getHealth() == player.getMaxHealth()) {
                reduceStress(0.02f);
            }
        }
    }

    // Método actualizado: Ahora recibe al jugador y desencadena la horda[cite: 2]
        public static void addStress(float amount, Player player) {
        playerStress = Math.min(MAX_STRESS, playerStress + amount);
        
        if (playerStress >= MAX_STRESS) {
            // Llamamos al puente de compatibilidad
            com.igneko.aidirector.compat.EnhancedHordesCompat.spawnHordeForPlayer(player);
            // Forzamos un periodo de enfriamiento reduciendo el estrés a 0[cite: 2]
            playerStress = 0.0f;
        }
    }

    private static void reduceStress(float amount) {
        playerStress = Math.max(0.0f, playerStress - amount);
    }
    
    public static float getStress() {
        return playerStress;
    }
}