package com.igneko.aidirector.client.hud;

import com.igneko.aidirector.client.ClientSquadState;
import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.network.DirectorStatusPacket;
import com.igneko.aidirector.network.SquadSyncPacket;
import com.igneko.aidirector.registry.ModRegistries;
import com.igneko.aidirector.registry.ModWeapons;
import com.igneko.aidirector.world.saferoom.SafeZoneManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Panel de estado del escuadrón (especificación §10): abajo a la izquierda, un retrato (cabeza de la
 * skin), el nombre, la barra de salud (permanente sólida, temporal punteada) y los objetos de las
 * ranuras tácticas de cada compañero. También muestra el estado del refugio, la cuenta atrás de los
 * eventos de pánico y la descarga del desfibrilador.
 */
@Mod.EventBusSubscriber(modid = TheAIDirector.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SquadHudOverlay {

    private static final int ROW_H = 21;
    private static final int PANEL_W = 128;
    private static final int BAR_W = 58;

    private SquadHudOverlay() {}

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("squad_panel", SquadHudOverlay::renderSquad);
        event.registerAboveAll("director_status", SquadHudOverlay::renderStatus);
        event.registerAboveAll("defibrillator", SquadHudOverlay::renderDefib);
    }

    // ------------------------------------------------------------------ panel de escuadrón

    private static void renderSquad(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.options.renderDebug) return;
        List<SquadSyncPacket.Entry> squad = ClientSquadState.squad();
        if (squad.isEmpty()) return;
        Font font = mc.font;
        int shown = Math.min(squad.size(), 7);
        for (int i = 0; i < shown; i++) {
            SquadSyncPacket.Entry e = squad.get(i);
            int y = height - 4 - ROW_H * (shown - i);
            drawRow(g, font, mc, e, 4, y);
        }
    }

    private static void drawRow(GuiGraphics g, Font font, Minecraft mc, SquadSyncPacket.Entry e, int x, int y) {
        boolean dead = e.status() == SquadSyncPacket.Status.DEAD;
        boolean danger = e.status() == SquadSyncPacket.Status.DOWNED || e.status() == SquadSyncPacket.Status.PINNED;
        boolean flash = danger && (mc.player.tickCount / 6) % 2 == 0;
        g.fill(x, y, x + PANEL_W, y + ROW_H - 2, flash ? 0x90601010 : 0x80000000);
        g.fill(x, y, x + 2, y + ROW_H - 2, statusColor(e));

        // Retrato
        PlayerFaceRenderer.draw(g, skin(mc, e), x + 4, y + 2, 15);
        if (dead) g.fill(x + 4, y + 2, x + 19, y + 17, 0xAA202020);

        // Nombre y estado
        int nameColor = dead ? 0x808080 : danger ? 0xFF5555 : e.blackAndWhite() ? 0xDDDDDD : 0xFFFFFF;
        String name = font.plainSubstrByWidth(e.name(), 62);
        g.drawString(font, name, x + 22, y + 2, nameColor, true);
        Component tag = tag(e);
        if (tag != null) {
            int tw = font.width(tag);
            g.pose().pushPose();
            g.pose().translate(x + PANEL_W - 3 - tw * 0.75f, y + 3, 0);
            g.pose().scale(0.75f, 0.75f, 1f);
            g.drawString(font, tag, 0, 0, dead ? 0x9A9A9A : danger ? 0xFF6060 : 0xBBBBBB, true);
            g.pose().popPose();
        }

        // Barra de salud
        int bx = x + 22, by = y + 13;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + 4, 0xC0000000);
        if (!dead && e.managed()) {
            int perm = Math.round(clamp(e.health() / 100f) * BAR_W);
            g.fill(bx, by, bx + perm, by + 3, e.status() == SquadSyncPacket.Status.DOWNED ? 0xFFAA0000 : healthColor(e));
            int temp = Math.min(Math.round(clamp(e.tempHealth() / 100f) * BAR_W), BAR_W - perm);
            for (int i = 0; i < temp; i += 3) g.fill(bx + perm + i, by, bx + perm + Math.min(i + 2, temp), by + 3, 0xFFDDDDDD);
        }

        // Ranuras tácticas: médica, temporal y arrojable.
        int ix = x + 84;
        drawSlot(g, ix, y + 9, e.medical(), ModRegistries.FIRST_AID_KIT.get(), ModRegistries.DEFIBRILLATOR.get());
        drawSlot(g, ix + 11, y + 9, e.temp(), ModRegistries.PAIN_PILLS.get(), ModRegistries.ADRENALINE.get());
        drawSlot(g, ix + 22, y + 9, e.thrown(), ModWeapons.PIPE_BOMB.get(), ModWeapons.MOLOTOV.get(), ModWeapons.BILE_JAR.get());

        // Distancia
        double dist = Math.sqrt(mc.player.distanceToSqr(e.x(), e.y(), e.z()));
        String d = Math.round(dist) + "m";
        g.pose().pushPose();
        g.pose().translate(x + 4, y + 14, 0);
        g.pose().scale(0.5f, 0.5f, 1f);
        g.drawString(font, d, 0, 0, 0xCCCCCC, true);
        g.pose().popPose();
    }

    private static void drawSlot(GuiGraphics g, int x, int y, byte index, Item... items) {
        g.fill(x, y, x + 10, y + 10, 0x50FFFFFF);
        if (index <= 0 || index > items.length) return;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.625f, 0.625f, 1f);
        g.renderItem(new ItemStack(items[index - 1]), 0, 0);
        g.pose().popPose();
    }

    private static ResourceLocation skin(Minecraft mc, SquadSyncPacket.Entry e) {
        PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(e.id());
        return info != null ? info.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(e.id());
    }

    private static Component tag(SquadSyncPacket.Entry e) {
        return switch (e.status()) {
            case DEAD -> Component.translatable("hud.aidirector.squad.dead");
            case DOWNED -> Component.translatable(e.beingRevived() ? "hud.aidirector.squad.reviving" : "hud.aidirector.squad.downed");
            case PINNED -> Component.translatable("hud.aidirector.squad.pinned");
            case ALIVE -> e.blackAndWhite() ? Component.translatable("hud.aidirector.squad.bw") : null;
        };
    }

    /** Color de estado (también el de la silueta). */
    public static int statusColor(SquadSyncPacket.Entry e) {
        return switch (e.status()) {
            case DEAD -> 0xFF606060;
            case DOWNED, PINNED -> 0xFFE03030;
            case ALIVE -> healthColor(e);
        };
    }

    private static int healthColor(SquadSyncPacket.Entry e) {
        if (e.blackAndWhite()) return 0xFFB0B0B0;
        float total = e.health() + e.tempHealth();
        if (total >= 40) return 0xFF33CC33;
        if (total >= 25) return 0xFFE0C020;
        return 0xFFE07020;
    }

    // ------------------------------------------------------------------ refugio y Crescendo

    private static void renderStatus(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        DirectorStatusPacket s = ClientSquadState.status();
        Font font = mc.font;
        int y = 34;
        if (s.crescendoSeconds() >= 0) {
            boolean flash = (mc.player.tickCount / 8) % 2 == 0;
            Component text = Component.translatable("hud.aidirector.crescendo", s.crescendoSeconds() / 60,
                    String.format("%02d", s.crescendoSeconds() % 60));
            banner(g, font, width / 2, y, text, flash ? 0xFFFF5040 : 0xFFFFB0A0, 0xA0400000);
            y += 14;
        }
        if (s.zone() != SafeZoneManager.ZoneStatus.NONE) {
            Component text = switch (s.zone()) {
                case OPEN -> Component.translatable("hud.aidirector.saferoom.open");
                case SEALED -> Component.translatable("hud.aidirector.saferoom.waiting", s.inside(), s.team());
                case SECURED -> Component.translatable("hud.aidirector.saferoom.secured");
                default -> Component.empty();
            };
            int color = s.zone() == SafeZoneManager.ZoneStatus.SECURED ? 0xFF60FF60
                    : s.zone() == SafeZoneManager.ZoneStatus.SEALED ? 0xFF70E0FF : 0xFFFFE070;
            banner(g, font, width / 2, y, text, color, 0x90000000);
        }
    }

    private static void banner(GuiGraphics g, Font font, int cx, int y, Component text, int color, int bg) {
        int w = font.width(text) + 10;
        g.fill(cx - w / 2, y - 2, cx + w / 2, y + 10, bg);
        g.drawCenteredString(font, text, cx, y, color);
    }

    // ------------------------------------------------------------------ desfibrilador

    private static void renderDefib(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.options.hideGui || !p.isUsingItem() || !p.getUseItem().is(ModRegistries.DEFIBRILLATOR.get())) return;
        int total = p.getUseItem().getUseDuration();
        float progress = total <= 0 ? 0 : (total - p.getUseItemRemainingTicks() + partialTick) / total;
        int w = 120, x = (width - w) / 2, y = height / 2 + 20;
        g.drawCenteredString(mc.font, Component.translatable("hud.aidirector.defibrillating"), width / 2, y - 11, 0x80E0FF);
        g.fill(x - 1, y - 1, x + w + 1, y + 5, 0xAA000000);
        g.fill(x, y, x + Math.round(clamp(progress) * w), y + 4, 0xFF60D0FF);
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
