package com.igneko.aidirector.client;

import com.igneko.aidirector.core.TheAIDirector;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Teclas del mod. TaCZ ya usa V (culatazo), R, G, H, O, Z y T, así que por defecto:
 * empujón = botón lateral del ratón (Mouse 4), interactuar/reanimar = C. Ambas son reasignables.
 */
@Mod.EventBusSubscriber(modid = TheAIDirector.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModKeyMappings {

    public static final String CATEGORY = "key.categories.aidirector";

    public static final KeyMapping SHOVE = new KeyMapping("key.aidirector.shove", KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_4, CATEGORY);

    public static final KeyMapping USE = new KeyMapping("key.aidirector.use", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY);

    private ModKeyMappings() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(SHOVE);
        event.register(USE);
    }
}
