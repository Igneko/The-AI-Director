package com.igneko.aidirector.client;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.network.SquadSyncPacket;
import com.igneko.aidirector.registry.ModRegistries;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.joml.Matrix4f;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Efectos de escuadrón en el mundo (especificación §10):
 * <ul>
 *   <li><b>Siluetas:</b> los compañeros que no se ven directamente brillan a través de las paredes
 *   con su color de estado — verde (bien), amarillo/naranja (herido), rojo parpadeante (caído o
 *   atrapado). Se ocultan mientras el jugador local está cubierto de bilis. Se usa el contorno de
 *   brillo vanilla con un equipo de marcador local (solo existe en este cliente) para el color.</li>
 *   <li><b>Marcadores:</b> sobre los caídos, atrapados y cuerpos aparece su nombre y la distancia,
 *   visible a través de las paredes.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TheAIDirector.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SquadClientEffects {

    private static final String TEAM_PREFIX = "aidir_sq_";
    private static final Method SET_SHARED_FLAG = ObfuscationReflectionHelper.findMethod(Entity.class, "m_20115_", int.class, boolean.class);
    private static final int GLOWING_FLAG = 6;

    /** Jugadores a los que este cliente ha encendido el brillo (para apagarlo después). */
    private static final Set<UUID> GLOWING = new HashSet<>();
    private static final java.util.Map<UUID, Boolean> VISIBLE = new java.util.HashMap<>();

    private SquadClientEffects() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        boolean enabled = AIDirectorConfig.squad().outlines() && !mc.player.hasEffect(ModRegistries.BILED.get());
        Scoreboard scoreboard = mc.level.getScoreboard();
        Set<UUID> keep = new HashSet<>();

        for (Player other : mc.level.players()) {
            if (other == mc.player) continue;
            SquadSyncPacket.Entry entry = ClientSquadState.find(other.getUUID());
            boolean glow = false;
            ChatFormatting color = ChatFormatting.GREEN;
            if (enabled && entry != null && entry.status() != SquadSyncPacket.Status.DEAD) {
                color = colorOf(entry);
                boolean danger = entry.status() != SquadSyncPacket.Status.ALIVE;
                if (mc.player.tickCount % 5 == 0 || !VISIBLE.containsKey(other.getUUID())) {
                    VISIBLE.put(other.getUUID(), hasLineOfSight(mc, other));
                }
                // Caído o atrapado: rojo parpadeante siempre; si no, solo cuando no se le ve.
                glow = danger ? (mc.player.tickCount / 6) % 2 == 0 : !VISIBLE.get(other.getUUID());
            }
            if (glow) {
                keep.add(other.getUUID());
                GLOWING.add(other.getUUID());
                assignTeam(scoreboard, other.getScoreboardName(), color);
                setGlowing(other, true);
            } else if (GLOWING.remove(other.getUUID())) {
                setGlowing(other, false);
                releaseTeam(scoreboard, other.getScoreboardName());
            }
        }
        GLOWING.retainAll(keep);
    }

    private static ChatFormatting colorOf(SquadSyncPacket.Entry e) {
        if (e.status() == SquadSyncPacket.Status.DOWNED || e.status() == SquadSyncPacket.Status.PINNED) return ChatFormatting.RED;
        if (e.blackAndWhite()) return ChatFormatting.WHITE;
        float total = e.health() + e.tempHealth();
        if (total >= 40) return ChatFormatting.GREEN;
        return total >= 25 ? ChatFormatting.YELLOW : ChatFormatting.GOLD;
    }

    private static boolean hasLineOfSight(Minecraft mc, Player other) {
        Vec3 from = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 to = other.getEyePosition();
        if (from.distanceToSqr(to) > 48 * 48) return false;
        HitResult hit = mc.level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS;
    }

    /** El brillo vanilla se sincroniza desde el servidor; aquí se fuerza solo en este cliente. */
    private static void setGlowing(Entity entity, boolean on) {
        try {
            SET_SHARED_FLAG.invoke(entity, GLOWING_FLAG, on);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    /** Equipo local de color: solo si el jugador no pertenece ya a un equipo real del servidor. */
    private static void assignTeam(Scoreboard scoreboard, String name, ChatFormatting color) {
        PlayerTeam current = scoreboard.getPlayersTeam(name);
        if (current != null && !current.getName().startsWith(TEAM_PREFIX)) return;
        String teamName = TEAM_PREFIX + color.getName();
        if (current != null && current.getName().equals(teamName)) return;
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            team.setColor(color);
        }
        scoreboard.addPlayerToTeam(name, team);
    }

    private static void releaseTeam(Scoreboard scoreboard, String name) {
        PlayerTeam current = scoreboard.getPlayersTeam(name);
        if (current != null && current.getName().startsWith(TEAM_PREFIX)) scoreboard.removePlayerFromTeam(name, current);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        GLOWING.clear();
        VISIBLE.clear();
        ClientSquadState.clear();
    }

    // ------------------------------------------------------------------ marcadores

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui || !AIDirectorConfig.squad().markers()) return;
        if (mc.player.hasEffect(ModRegistries.BILED.get())) return;
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        for (SquadSyncPacket.Entry e : ClientSquadState.squad()) {
            if (e.status() == SquadSyncPacket.Status.ALIVE) continue;
            Vec3 at = new Vec3(e.x(), e.y(), e.z());
            Player live = mc.level.getPlayerByUUID(e.id());
            if (live != null && e.status() != SquadSyncPacket.Status.DEAD) {
                at = live.getPosition(event.getPartialTick());
            }
            at = at.add(0, e.status() == SquadSyncPacket.Status.DEAD ? 1.0 : 1.4, 0);
            double dist = at.distanceTo(cam);
            if (dist < 2.5) continue;

            String icon = e.status() == SquadSyncPacket.Status.DEAD ? "☠ " : "⚠ ";
            Component text = Component.literal(icon + e.name() + " · " + Math.round(dist) + "m");
            int color = e.status() == SquadSyncPacket.Status.DEAD ? 0xFFB0B0B0
                    : (mc.player.tickCount / 8) % 2 == 0 ? 0xFFFF4040 : 0xFFFFA0A0;

            pose.pushPose();
            pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
            pose.mulPose(camera.rotation());
            float scale = (float) (0.025 * Math.max(1.0, dist / 10.0));
            pose.scale(-scale, -scale, scale);
            Matrix4f matrix = pose.last().pose();
            float x = -font.width(text) / 2f;
            font.drawInBatch(text, x, 0, color, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, 0x60000000, 0xF000F0);
            pose.popPose();
        }
        buffers.endBatch();
    }
}
