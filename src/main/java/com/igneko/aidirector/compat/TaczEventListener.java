package com.igneko.aidirector.compat;

import com.igneko.aidirector.core.StressManager;
import com.igneko.aidirector.core.TheAIDirector;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// TODO: Descomentar cuando VS Code lea el .jar de TACZ en la carpeta libs
// import com.tacz.guns.api.event.GunFireEvent; 

@Mod.EventBusSubscriber(modid = TheAIDirector.MOD_ID)
public class TaczEventListener {

    // @SubscribeEvent
    // public static void onGunFire(GunFireEvent event) {
    //     // Asumimos que el evento expone quién disparó
    //     Player player = event.getShooter(); 
    //     
    //     if (player != null && !player.level().isClientSide()) {
    //         // Aumentar el estrés del jugador por hacer ruido excesivo al disparar armas de fuego
    //         StressManager.addStress(5.0f, player);
    //     }
    // }
}