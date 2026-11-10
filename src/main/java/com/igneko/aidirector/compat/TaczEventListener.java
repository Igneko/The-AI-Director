package com.igneko.aidirector.compat;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;
import com.igneko.aidirector.core.StressManager;
import com.igneko.aidirector.core.TheAIDirector;
import com.tacz.guns.api.event.server.ServerShootEvent; 

@Mod.EventBusSubscriber(modid = TheAIDirector.MOD_ID)
public class TaczEventListener {

    @SubscribeEvent
    public static void onGunFire(ServerShootEvent event) {
        Player player = event.getShooter();
        
        if (player != null && !player.level().isClientSide()) {
            // Un disparo con cualquier arma de TACZ suma 15 puntos de estrés al instante
            StressManager.addStress(15.0f, player);
        }
    }
}