package com.igneko.aidirector.client;

import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Los huevos de los infectados usan sprites propios a color: anulamos el tinte de dos colores
 * que Forge aplica a los huevos generadores (se registra después del suyo y lo sustituye).
 */
@Mod.EventBusSubscriber(modid = TheAIDirector.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SpawnEggColors {

    private SpawnEggColors() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        ModEntities.eggs().forEach(egg -> event.register((stack, tint) -> -1, egg.get()));
    }
}
