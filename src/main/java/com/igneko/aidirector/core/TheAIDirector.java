package com.igneko.aidirector.core;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// El modId definido en la estructura del proyecto es "ai_director"
@Mod(TheAIDirector.MOD_ID)
public class TheAIDirector {
    
    public static final String MOD_ID = "ai_director";

    public TheAIDirector() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Registrar el mod en el bus de eventos de Forge
        MinecraftForge.EVENT_BUS.register(this);
    }
}