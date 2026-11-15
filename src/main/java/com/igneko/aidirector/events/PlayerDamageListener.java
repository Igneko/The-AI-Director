package com.igneko.aidirector.events;

import com.igneko.aidirector.core.StressManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerDamageListener {

    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        // Si la entidad dañada es un jugador y estamos en el servidor
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            
            // Restar 30 segundos del reloj por cada golpe recibido
            StressManager.deductTime(player, 30);
        }
    }
}