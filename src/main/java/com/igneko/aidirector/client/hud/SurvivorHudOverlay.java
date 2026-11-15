package com.igneko.aidirector.client.hud;

import com.igneko.aidirector.client.BlackAndWhiteEffect;
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
        event.registerBelowAll("black_and_white_vignette", SurvivorHudOverlay::renderBlackAndWhiteVignette);
        event.registerBelowAll("bile_splatter", SurvivorHudOverlay::renderBile);
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

        if (data.isPinned()) {
            boolean flash = (player.tickCount / 6) % 2 == 0;
            Component pinned = Component.translatable("hud.aidirector.pinned." + data.pinType().name().toLowerCase(java.util.Locale.ROOT));
            g.drawCenteredString(font, pinned, width / 2, height / 2 - 34, flash ? 0xFF4040 : 0xFFAAAA);
            if (data.pinType().canShoot()) {
                g.drawCenteredString(font, Component.translatable("hud.aidirector.pinned.shoot"), width / 2, height / 2 - 23, 0xFFFFFF);
            }
        }

        if (data.isIncapacitated() && data.isBeingRevived()) {
            drawReviveBar(g, font, width, height, data.reviveProgress());
        } else if (data.isAlive() && mc.crosshairPickEntity instanceof Player target) {
            SurvivorCapability.get(target).filter(SurvivorHealthData::isBeingRevived)
                    .ifPresent(t -> drawReviveBar(g, font, width, height, t.reviveProgress()));
        }
    }

    /** Viñeta roja tenue en los bordes durante el estado Blanco y Negro (late con el corazón). */
    private static void renderBlackAndWhiteVignette(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        if (!BlackAndWhiteEffect.isActive() || Minecraft.getInstance().options.hideGui) return;
        Player player = Minecraft.getInstance().player;
        float time = (player == null ? 0 : player.tickCount) + partialTick;
        float pulse = 0.75f + 0.25f * (float) Math.pow(Math.abs(Math.sin(time * Math.PI / 22.0)), 6);
        int steps = 12;
        int band = Math.max(2, Math.min(width, height) / 40);
        for (int i = 0; i < steps; i++) {
            int alpha = Math.round(70 * pulse * (1f - (float) i / steps));
            int color = (alpha << 24) | 0x8A0000;
            int o = i * band;
            g.fill(o, o, width - o, o + band, color);                       // arriba
            g.fill(o, height - o - band, width - o, height - o, color);      // abajo
            g.fill(o, o + band, o + band, height - o - band, color);        // izquierda
            g.fill(width - o - band, o + band, width - o, height - o - band, color); // derecha
        }
    }

    /** Bilis: manchas verdosas que emborronan parte de la pantalla y se desvanecen al final. */
    private static void renderBile(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Player player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().options.hideGui) return;
        var effect = player.getEffect(com.igneko.aidirector.registry.ModRegistries.BILED.get());
        if (effect == null) return;
        float fade = Math.min(1f, effect.getDuration() / 60f);
        java.util.Random rng = new java.util.Random(player.getId() * 31L + 7);
        for (int i = 0; i < 14; i++) {
            int cx = rng.nextInt(width), cy = rng.nextInt(height);
            int r = 25 + rng.nextInt(Math.max(1, Math.min(width, height) / 5));
            int alpha = Math.round((110 + rng.nextInt(70)) * fade);
            int color = (alpha << 24) | (rng.nextBoolean() ? 0x5C7A12 : 0x7A8F1E);
            // Mancha irregular: pila de rectángulos de anchura senoidal.
            for (int dy = -r; dy <= r; dy += 3) {
                double k = Math.sqrt(1 - (dy / (double) r) * (dy / (double) r));
                int w = (int) (r * k * (0.75 + 0.25 * Math.sin(dy * 0.3 + i)));
                g.fill(cx - w, cy + dy, cx + w, cy + dy + 3, color);
            }
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
