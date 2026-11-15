package com.igneko.aidirector.client;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.core.survivor.SurvivorCapability;
import com.igneko.aidirector.core.survivor.SurvivorHealthData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Estado "Blanco y Negro" (última vida): pantalla desaturada mediante un post-shader y latido
 * cardíaco constante. La viñeta roja tenue la dibuja el HUD.
 */
@Mod.EventBusSubscriber(modid = TheAIDirector.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BlackAndWhiteEffect {

    public static final ResourceLocation SHADER = new ResourceLocation(TheAIDirector.MOD_ID, "shaders/post/black_and_white.json");
    private static final int HEARTBEAT_INTERVAL_TICKS = 22;

    private static boolean active;
    /** Si el shader no carga (p. ej. por un paquete de recursos), no reintentamos cada tick. */
    private static boolean loadFailed;
    private static int heartbeatTimer;

    private BlackAndWhiteEffect() {}

    public static boolean isActive() {
        return active;
    }

    private static boolean shouldBeActive(Player player) {
        return !player.isCreative() && !player.isSpectator() && player.isAlive()
                && SurvivorCapability.get(player).map(d -> d.isBlackAndWhite(AIDirectorConfig.rules())).orElse(false);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        boolean wanted = mc.player != null && mc.level != null && shouldBeActive(mc.player);
        GameRenderer renderer = mc.gameRenderer;
        PostChain current = renderer.currentEffect();
        boolean ours = current != null && SHADER.toString().equals(current.getName());

        if (wanted) {
            // No pisamos otros post-efectos (p. ej. al espectar a un creeper).
            if (current == null && !loadFailed) {
                renderer.loadEffect(SHADER);
                loadFailed = renderer.currentEffect() == null;
            }
            if (!mc.isPaused() && --heartbeatTimer <= 0) {
                heartbeatTimer = HEARTBEAT_INTERVAL_TICKS;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, 0.9f, 0.7f));
            }
        } else if (ours) {
            renderer.shutdownEffect();
        }
        if (!wanted) heartbeatTimer = 0;
        active = wanted;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft mc = Minecraft.getInstance();
        PostChain current = mc.gameRenderer.currentEffect();
        if (current != null && SHADER.toString().equals(current.getName())) mc.gameRenderer.shutdownEffect();
        active = false;
    }
}
