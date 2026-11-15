package com.igneko.aidirector.client.hud;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.core.survivor.SurvivorCapability;
import com.igneko.aidirector.core.survivor.SurvivorHealthData;
import com.igneko.aidirector.core.survivor.SurvivorRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * HUD provisional de la Fase 1: barra de salud permanente (sólida) + temporal (punteada),
 * estado de incapacitación y progreso de reanimación. La Fase 6 lo sustituirá por el panel de escuadrón.
 */
@Mod.EventBusSubscriber(modid = TheAIDirector.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SurvivorHudOverlay implements IGuiOverlay {

    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 5;

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "survivor_health", new SurvivorHudOverlay());
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isCreative() || player.isSpectator()) return;
        SurvivorHealthData data = SurvivorCapability.get(player).orElse(null);
        if (data == null) return;

        SurvivorRules rules = AIDirectorConfig.rules();
        Font font = mc.font;
        int x = 8;
        int y = height - 30;

        if (data.isIncapacitated()) {
            float bleedRatio = data.bleedHealth() / rules.bleedPool();
            g.drawString(font, Component.translatable("hud.aidirector.incapacitated", Math.round(data.bleedHealth())), x, y - 10, 0xFF5555);
            drawBar(g, x, y, bleedRatio, 0xFFAA0000, 0);
            if (data.isBeingRevived()) drawReviveBar(g, font, width, height, data.reviveProgress());
        } else if (data.isAlive()) {
            int color = data.isBlackAndWhite(rules) ? 0xAAAAAA : 0xFFFFFF;
            String label = Math.round(data.permHealth()) + (data.tempHealth() >= 0.5f ? " +" + Math.round(data.tempHealth()) : "");
            g.drawString(font, label, x, y - 10, color);
            if (data.isBlackAndWhite(rules)) {
                g.drawString(font, Component.translatable("hud.aidirector.black_and_white"), x + 50, y - 10, 0x888888);
            }
            drawBar(g, x, y, data.permHealth() / rules.maxHealth(), healthColor(data, rules), data.tempHealth() / rules.maxHealth());
        }

        // Reanimando a otro: barra en el centro
        if (data.isAlive() && mc.crosshairPickEntity instanceof Player target) {
            SurvivorCapability.get(target).filter(SurvivorHealthData::isBeingRevived)
                    .ifPresent(t -> drawReviveBar(g, font, width, height, t.reviveProgress()));
        }
    }

    private static int healthColor(SurvivorHealthData data, SurvivorRules rules) {
        float ratio = data.totalHealth() / rules.maxHealth();
        if (ratio >= rules.limpThresholdRatio()) return 0xFF33CC33;
        if (ratio > 0.01f) return 0xFFE0C020;
        return 0xFFCC2222;
    }

    private static void drawBar(GuiGraphics g, int x, int y, float permRatio, int permColor, float tempRatio) {
        g.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xAA000000);
        int permW = Math.round(clamp(permRatio) * BAR_WIDTH);
        g.fill(x, y, x + permW, y + BAR_HEIGHT, permColor);
        int tempW = Math.round(clamp(tempRatio) * BAR_WIDTH);
        // Salud temporal: segmentos punteados a continuación de la permanente.
        for (int i = 0; i < tempW; i += 3) {
            g.fill(x + permW + i, y, x + permW + Math.min(i + 2, tempW), y + BAR_HEIGHT, 0xFFDDDDDD);
        }
    }

    private static void drawReviveBar(GuiGraphics g, Font font, int width, int height, float progress) {
        int w = 120;
        int x = (width - w) / 2;
        int y = height / 2 + 20;
        g.drawCenteredString(font, Component.translatable("hud.aidirector.reviving"), width / 2, y - 11, 0xFFFFFF);
        g.fill(x - 1, y - 1, x + w + 1, y + 5, 0xAA000000);
        g.fill(x, y, x + Math.round(clamp(progress) * w), y + 4, 0xFFFFFFFF);
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
