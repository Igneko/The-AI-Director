package com.igneko.aidirector.client;

import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.core.survivor.SurvivorCapability;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Pose de incapacitado en el cliente: tumbado en el suelo con la animación de nado/arrastre.
 *
 * <p>TaCZ reescribe la pose forzada del jugador local en cada tick ({@code LocalPlayerCrawl.tickCrawl}
 * llama a {@code setForcedPose(null)} cuando no se usa su "crawl"), así que la reaplicamos al final
 * del tick del jugador, después de TaCZ y de {@code updatePlayerPose}, para que el render la vea.</p>
 */
@Mod.EventBusSubscriber(modid = TheAIDirector.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DownedPoseHandler {

    /** Velocidad de la "brazada": lenta, de alguien que se arrastra herido. */
    private static final float CRAWL_STROKE_SPEED = 0.12f;

    private DownedPoseHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !event.side.isClient()) return;
        Player player = event.player;
        boolean downed = !player.isSpectator() && !player.isCreative()
                && SurvivorCapability.get(player).map(d -> d.isIncapacitated() || d.pinType().lying()).orElse(false);
        if (!downed) return;

        player.setForcedPose(Pose.SWIMMING);
        if (player.getPose() != Pose.SWIMMING) player.setPose(Pose.SWIMMING);
        // Sin movimiento los brazos quedarían congelados: avanzamos un poco el ciclo de "brazada".
        player.walkAnimation.update(CRAWL_STROKE_SPEED, 0.4f);
    }
}
