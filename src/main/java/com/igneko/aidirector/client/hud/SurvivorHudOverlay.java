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
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Barra de salud del superviviente. Sustituye a los corazones vanilla y ocupa su mismo hueco
 * (izquierda, sobre el hotbar): salud permanente sólida + temporal punteada, o el sangrado si está
 * incapacitado. También dibuja la barra de reanimación en el centro de la pantalla.
 * La Fase 6 añadirá el panel de escuadrón con los compañeros.
 */
@Mod.EventBusSubscriber(modid = TheAIDirector.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SurvivorHudOverlay implements IGuiOverlay {

    /** Mismo ancho que la fila de 10 corazones vanilla. */
    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 9;

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        // Justo después de PLAYER_HEALTH: así sumamos nuestra altura a leftHeight antes de que
        // armadura y aire calculen su posición.
        event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "survivor_health", new SurvivorHudOverlay());
        MinecraftForge.EVENT_BUS.addListener(SurvivorHudOverlay::hideVanillaHearts);
    }

    /** Oculta los corazones vanilla mientras el jugador esté gestionado por el sistema de salud. */
    private static void hideVanillaHearts(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) return;
        Player player = Minecraft.getInstance().player;
        if (player != null && SurvivorCapability.get(player).isPresent()) event.setCanceled(true);
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;
        SurvivorHealthData data = SurvivorCapability.get(player).orElse(null);
        if (data == null) return;

        SurvivorRules rules = AIDirectorConfig.rules();
        Font font = mc.font;

        if (gui.shouldDrawSurvivalElements()) {
            int x = width / 2 - 91;
            int y = height - gui.leftHeight;
            gui.leftHeight += BAR_HEIGHT + 1;

            if (data.isIncapacitated()) {
                drawBar(g, x, y, data.bleedHealth() / rules.bleedPool(), 0xFFAA0000, 0f);
                drawLabel(g, font, x, y, String.valueOf(Math.round(data.bleedHealth())), 0xFFFFFF);
                drawTitle(g, font, x, y, Component.translatable("hud.aidirector.downed"), 0xFF5555);
            } else if (data.isAlive()) {
                boolean bw = data.isBlackAndWhite(rules);
                drawBar(g, x, y, data.permHealth() / rules.maxHealth(), healthColor(data, rules), data.tempHealth() / rules.maxHealth());
                String label = Math.round(data.permHealth()) + (data.tempHealth() >= 0.5f ? " +" + Math.round(data.tempHealth()) : "");
                drawLabel(g, font, x, y, label, bw ? 0xBBBBBB : 0xFFFFFF);
                if (bw) drawTitle(g, font, x, y, Component.translatable("hud.aidirector.black_and_white"), 0xAAAAAA);
            }
        }

        if (data.isIncapacitated() && data.isBeingRevived()) {
            drawReviveBar(g, font, width, height, data.reviveProgress());
        } else if (data.isAlive() && mc.crosshairPickEntity instanceof Player target) {
            SurvivorCapability.get(target).filter(SurvivorHealthData::isBeingRevived)
                    .ifPresent(t -> drawReviveBar(g, font, width, height, t.reviveProgress()));
        }
    }

    private static int healthColor(SurvivorHealthData data, SurvivorRules rules) {
        if (data.isBlackAndWhite(rules)) return 0xFF8A8A8A;
        float ratio = data.totalHealth() / rules.maxHealth();
        if (ratio >= rules.limpThresholdRatio()) return 0xFF33CC33;
        if (ratio > 0.01f) return 0xFFE0C020;
        return 0xFFCC2222;
    }

    private static void drawBar(GuiGraphics g, int x, int y, float permRatio, int permColor, float tempRatio) {
        g.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xC0000000);
        g.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF2A2A2A);
        int permW = Math.round(clamp(permRatio) * BAR_WIDTH);
        g.fill(x, y, x + permW, y + BAR_HEIGHT, permColor);
        int tempW = Math.min(Math.round(clamp(tempRatio) * BAR_WIDTH), BAR_WIDTH - permW);
        // Salud temporal: segmentos punteados a continuación de la permanente.
        for (int i = 0; i < tempW; i += 3) {
            g.fill(x + permW + i, y + 1, x + permW + Math.min(i + 2, tempW), y + BAR_HEIGHT - 1, 0xFFDDDDDD);
        }
    }

    private static void drawLabel(GuiGraphics g, Font font, int x, int y, String text, int color) {
        int tx = x + (BAR_WIDTH - font.width(text)) / 2;
        g.drawString(font, text, tx, y + 1, color, true);
    }

    /** Etiqueta de estado a la izquierda de la barra (encima quedaría tapada por la armadura). */
    private static void drawTitle(GuiGraphics g, Font font, int x, int y, Component text, int color) {
        g.drawString(font, text, x - 4 - font.width(text), y + 1, color, true);
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
