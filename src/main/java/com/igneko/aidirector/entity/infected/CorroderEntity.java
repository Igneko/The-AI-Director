package com.igneko.aidirector.entity.infected;

import com.igneko.aidirector.entity.projectile.AcidBallEntity;
import com.igneko.aidirector.entity.projectile.AcidPuddleEntity;
import com.igneko.aidirector.core.survivor.ReviveManager;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Corrosiva: ataca a media distancia escupiendo un proyectil parabólico de ácido. Prioriza a los
 * supervivientes acorralados: incapacitados o que están reanimando a alguien. El charco crece y su
 * daño escala exponencialmente mientras se permanezca dentro.
 */
public class CorroderEntity extends SpecialInfectedEntity {

    public static final int STATE_IDLE = 0, STATE_SPIT = 1;

    private static final int SPIT_COOLDOWN = 400; // 20 s
    private static final double SPIT_RANGE = 24;

    private int cooldown = 40;
    private int spitTicks;
    private LivingEntity spitTarget;

    public CorroderEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, new Sounds(ModSounds.CORRODER_AMBIENT, ModSounds.CORRODER_HURT, ModSounds.CORRODER_DEATH, ModSounds.CORRODER_SPAWN));
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return baseAttributes(20, 0.24, l4d(5));
    }

    @Override
    protected void registerBehaviourGoals() {
        goalSelector.addGoal(2, new KeepDistanceGoal(this, 8, 18, 1.0) {
            @Override
            public boolean canUse() { return getState() == STATE_IDLE && super.canUse(); }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 20f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (cooldown > 0) cooldown--;
        if (getState() == STATE_SPIT) {
            tickSpit();
            return;
        }
        if (cooldown > 0) return;
        LivingEntity target = choosePrey();
        if (target != null) {
            spitTarget = target;
            setState(STATE_SPIT);
            spitTicks = 0;
            getNavigation().stop();
        }
    }

    /** Presa preferida: alguien en el suelo o reanimando; si no, el objetivo actual. */
    private LivingEntity choosePrey() {
        LivingEntity best = null;
        for (ServerPlayer p : serverLevel().players()) {
            if (!isSurvivor(p) || distanceTo(p) > SPIT_RANGE || !hasLineOfSight(p)) continue;
            if (isDowned(p) || ReviveManager.isReviving(p)) return p;
        }
        LivingEntity target = getTarget();
        if (target != null && isSurvivor(target) && distanceTo(target) <= SPIT_RANGE && distanceTo(target) >= 4
                && hasLineOfSight(target)) best = target;
        return best;
    }

    private void tickSpit() {
        spitTicks++;
        getNavigation().stop();
        if (spitTarget == null || !spitTarget.isAlive()) {
            setState(STATE_IDLE);
            return;
        }
        getLookControl().setLookAt(spitTarget, 60f, 60f);
        if (spitTicks == 10) {
            AcidBallEntity ball = new AcidBallEntity(level(), this);
            Vec3 start = getEyePosition().add(getLookAngle().scale(0.6));
            ball.setPos(start.x, start.y, start.z);
            Vec3 aim = spitTarget.position();
            double dist = Math.sqrt(aim.subtract(start).multiply(1, 0, 1).lengthSqr());
            double flight = Math.max(8, dist / 0.9);
            aim = aim.add(spitTarget.getDeltaMovement().multiply(flight * 0.5, 0, flight * 0.5));
            ball.shootAt(start, aim, 0.9);
            level().addFreshEntity(ball);
            level().playSound(null, getX(), getY(), getZ(), ModSounds.CORRODER_SPIT.get(), SoundSource.HOSTILE, 1.4f, 1f);
        }
        if (spitTicks >= 18) {
            setState(STATE_IDLE);
            cooldown = SPIT_COOLDOWN;
            spitTarget = null;
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide()) {
            // Al morir deja un pequeño charco de ácido.
            AcidPuddleEntity puddle = AcidPuddleEntity.create(level(), position(), 1.6f, 80);
            level().addFreshEntity(puddle);
        }
        super.die(source);
    }
}
