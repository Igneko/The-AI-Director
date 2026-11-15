package com.igneko.aidirector.core.director;

import com.igneko.aidirector.api.event.AcousticPulseEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Random;

/**
 * Atrae a los infectados errantes hacia el origen de un pulso acústico. Si el emisor es un jugador,
 * reparte el aggro entre tiradores (heredado de la versión anterior del listener de TaCZ); si no,
 * mueve a los infectados hacia el punto de ruido.
 *
 * <p>Fase 3: el AI Director también consumirá este evento para subir la agitación de Enhanced Hordes.</p>
 */
public final class NoiseAttractionHandler {

    private static final String LAST_DISTRACTION_TAG = "aidirector:LastAcousticDistraction";
    private static final int DISTRACTION_COOLDOWN_TICKS = 60;

    private final Random random = new Random();

    @SubscribeEvent
    public void onPulse(AcousticPulseEvent event) {
        if (event.getRadius() <= 0) return;
        ServerLevel level = event.getLevel();
        long now = level.getGameTime();
        double r = event.getRadius();
        Vec3 o = event.getOrigin();
        List<Monster> hostiles = level.getEntitiesOfClass(Monster.class, new AABB(o, o).inflate(r),
                m -> m.isAlive() && m.position().distanceToSqr(o) <= r * r);

        for (Monster mob : hostiles) {
            if (now - mob.getPersistentData().getLong(LAST_DISTRACTION_TAG) < DISTRACTION_COOLDOWN_TICKS) continue;
            boolean changed = event.getEmitter() instanceof Player shooter
                    ? redirectToShooter(mob, shooter)
                    : mob.getNavigation().moveTo(o.x, o.y, o.z, 1.2D);
            if (changed) mob.getPersistentData().putLong(LAST_DISTRACTION_TAG, now);
        }
    }

    private boolean redirectToShooter(Monster mob, Player shooter) {
        LivingEntity current = mob.getTarget();
        if (current == shooter) return false;
        boolean retarget;
        if (!(current instanceof Player currentPlayer) || !current.isAlive()) {
            retarget = true;
        } else {
            double distCurrent = mob.distanceToSqr(currentPlayer);
            double distNew = mob.distanceToSqr(shooter);
            retarget = distNew < distCurrent * 0.5
                    || (Math.abs(distNew - distCurrent) < 100.0 && random.nextDouble() < 0.3);
        }
        if (!retarget) return false;
        mob.setTarget(shooter);
        mob.getNavigation().moveTo(shooter, 1.2D);
        return true;
    }
}
