package com.igneko.aidirector.core;

import com.igneko.aidirector.compat.tacz.IncapWeaponHandler;
import com.igneko.aidirector.compat.tacz.TaczEventBridge;
import com.igneko.aidirector.core.combat.StaggerHandler;
import com.igneko.aidirector.core.director.NoiseAttractionHandler;
import com.igneko.aidirector.registry.ModRegistries;
import com.igneko.aidirector.registry.ModEntities;
import com.igneko.aidirector.registry.ModSounds;
import com.igneko.aidirector.registry.ModWeapons;
import com.igneko.aidirector.core.survivor.PinService;
import com.igneko.aidirector.core.director.BileService;
import com.igneko.aidirector.entity.infected.WailerEntity;
import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.survivor.SurvivorEventHandler;
import com.igneko.aidirector.core.survivor.SurvivorHealthData;
import com.igneko.aidirector.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TheAIDirector.MOD_ID)
public class TheAIDirector {
    public static final String MOD_ID = "aidirector";

    public TheAIDirector() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AIDirectorConfig.SPEC);
        modBus.addListener(AIDirectorConfig::onConfigChanged);
        modBus.addListener(this::onRegisterCapabilities);
        modBus.addListener(this::onCommonSetup);
        ModRegistries.register(modBus);
        ModSounds.register(modBus);
        ModEntities.register(modBus);
        ModWeapons.register(modBus);
        com.igneko.aidirector.registry.ModBlocks.register(modBus);
        com.igneko.aidirector.registry.ModStructures.register(modBus);

        // Bus global de Forge (eventos de juego).
        MinecraftForge.EVENT_BUS.register(new CommandRegistry());
        MinecraftForge.EVENT_BUS.register(new SurvivorEventHandler());
        MinecraftForge.EVENT_BUS.register(new StaggerHandler());
        MinecraftForge.EVENT_BUS.register(new NoiseAttractionHandler());
        MinecraftForge.EVENT_BUS.register(new TaczEventBridge());
        MinecraftForge.EVENT_BUS.register(new IncapWeaponHandler());
        MinecraftForge.EVENT_BUS.register(new PinService());
        MinecraftForge.EVENT_BUS.register(new BileService());
        MinecraftForge.EVENT_BUS.register(new com.igneko.aidirector.core.director.IntensityTracker());
        MinecraftForge.EVENT_BUS.register(new com.igneko.aidirector.core.director.SpecialInfectedSpawner());
        MinecraftForge.EVENT_BUS.register(new WailerEntity.NoiseListener());
        // Fase 3-6: Director (FSM), refugios, Crescendo, escuadrón, desfibrilador y estadísticas de tramo.
        MinecraftForge.EVENT_BUS.register(new com.igneko.aidirector.core.director.DirectorManager());
        MinecraftForge.EVENT_BUS.register(new com.igneko.aidirector.core.director.CrescendoManager());
        MinecraftForge.EVENT_BUS.register(new com.igneko.aidirector.core.director.SegmentStats());
        MinecraftForge.EVENT_BUS.register(new com.igneko.aidirector.world.saferoom.SafeZoneManager());
        MinecraftForge.EVENT_BUS.register(new com.igneko.aidirector.world.saferoom.StructureGuard());
        MinecraftForge.EVENT_BUS.register(new com.igneko.aidirector.core.survivor.SquadService());
        MinecraftForge.EVENT_BUS.register(new com.igneko.aidirector.core.survivor.DefibService());
    }

    private void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(SurvivorHealthData.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
