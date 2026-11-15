package com.igneko.aidirector.entity.infected;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Mantiene al infectado a distancia media de su objetivo (Estrangulador, Corrosiva): se acerca si está
 * lejos, se aleja si lo tienen encima y, en rango, se queda mirando para atacar a distancia.
 */
public class KeepDistanceGoal extends Goal {

    private final PathfinderMob mob;
    private final double minDist;
    private final double maxDist;
    private final double speed;
    private int repath;

    public KeepDistanceGoal(PathfinderMob mob, double minDist, double maxDist, double speed) {
        this.mob = mob;
        this.minDist = minDist;
        this.maxDist = maxDist;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && !mob.isPassenger();
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        mob.getLookControl().setLookAt(target, 30f, 30f);
        if (--repath > 0) return;
        repath = 10;
        double dist = mob.distanceTo(target);
        boolean sees = mob.hasLineOfSight(target);
        if (dist > maxDist || !sees) {
            mob.getNavigation().moveTo(target, speed);
        } else if (dist < minDist) {
            Vec3 away = DefaultRandomPos.getPosAway(mob, 10, 4, target.position());
            if (away != null) mob.getNavigation().moveTo(away.x, away.y, away.z, speed * 1.15);
        } else {
            mob.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}
