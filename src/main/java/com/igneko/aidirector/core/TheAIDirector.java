package com.igneko.aidirector;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// El valor "ai_director" debe coincidir exactamente con tu mods.toml
@Mod(TheAIDirector.MOD_ID)
public class TheAIDirector {
    
    public static final String MOD_ID = "ai_director";

    public TheAIDirector() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Registrarse a sí mismo para eventos del servidor y del mundo
        MinecraftForge.EVENT_BUS.register(this);
    }
}