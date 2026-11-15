package com.igneko.aidirector.client.hud;

import com.igneko.aidirector.client.ClientCombatState;
import com.igneko.aidirector.core.TheAIDirector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Indicador de recarga del empujón bajo la mira (rojo cuando hay fatiga). */
@Mod.EventBusSubscriber(modid = TheAIDirector.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ShoveCrosshairOverlay implements IGuiOverlay {

    private static final int WIDTH = 16;

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "shove_cooldown", new ShoveCrosshairOverlay());
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        if (Minecraft.getInstance().options.hideGui) return;
        float remaining = ClientCombatState.remaining(partialTick);
        if (remaining <= 0f) return;
        int x = (width - WIDTH) / 2;
        int y = height / 2 + 9;
        int color = ClientCombatState.isFatigued() ? 0xFFE04040 : 0xFFFFFFFF;
        g.fill(x, y, x + WIDTH, y + 2, 0x80000000);
        g.fill(x, y, x + Math.round(remaining * WIDTH), y + 2, color);
    }
}
