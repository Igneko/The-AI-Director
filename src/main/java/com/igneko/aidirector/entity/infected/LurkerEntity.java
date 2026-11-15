package com.igneko.aidirector.entity.infected;

import com.igneko.aidirector.core.combat.StaggerHandler;
import com.igneko.aidirector.core.survivor.PinService;
import com.igneko.aidirector.core.survivor.PinType;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Acechador: depredador agazapado que salta en parábola sobre su presa y la inmoviliza en el suelo,
 * desgarrándola cada medio segundo. Un salto desde gran altura (≥ 15 bloques) causa daño extra.
 * Se libera matándolo o empujándolo; también se le puede aturdir en pleno salto.
 */
public class LurkerEntity extends SpecialInfectedEntity implements PinService.Pinner {

    public static final int STATE_IDLE = 0, STATE_CROUCH = 1, STATE_LEAP = 2, STATE_PINNING = 3;

    private static final int CROUCH_TICKS = 14;
    private static final int LEAP_COOLDOWN = 50;

    private int cooldown = 40;
    private int stateTicks;
    private double leapStartY;

    public LurkerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, new Sounds(ModSounds.LURKER_AMBIENT, ModSounds.LURKER_HURT, ModSounds.LURKER_DEATH, ModSounds.LURKER_SPAWN));
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return baseAttributes(50, 0.32, l4d(5));
    }

    @Override
    protected void registerBehaviourGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true) {
            @Override
            public boolean canUse() { return getState() == STATE_IDLE && super.canUse(); }
            @Override
            public boolean canContinueToUse() { return getState() == STATE_IDLE && super.canContinueToUse(); }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (cooldown > 0) cooldown--;
        stateTicks++;
        switch (getState()) {
            case STATE_PINNING -> tickPinning();
            case STATE_LEAP -> tickLeap();
            case STATE_CROUCH -> tickCrouch();
            default -> tryStartPounce();
        }
    }

    private void tryStartPounce() {
        LivingEntity target = getTarget();
        if (cooldown > 0 || target == null || !onGround() || !isFreeSurvivor(target)) return;
        double dist = distanceTo(target);
        if (dist < 3 || dist > 18 || !hasLineOfSight(target)) return;
        setState(STATE_CROUCH);
        stateTicks = 0;
        getNavigation().stop();
        playSound(ModSounds.LURKER_AMBIENT.get(), 1.2f, 0.8f);
    }

    private void tickCrouch() {
        LivingEntity target = getTarget();
        getNavigation().stop();
        if (target == null || !target.isAlive()) {
            setState(STATE_IDLE);
            return;
        }
        getLookControl().setLookAt(target, 60f, 60f);
        if (stateTicks < CROUCH_TICKS) return;

        // Predicción simple: dónde estará la presa cuando aterricemos.
        double dist = distanceTo(target);
        double flight = Math.max(6, dist / 1.2);
        Vec3 aim = target.position().add(target.getDeltaMovement().multiply(flight * 0.6, 0, flight * 0.6));
        Vec3 dir = flatDirectionTo(aim);
        double horizontal = Math.min(1.7, 0.45 + dist * 0.09);
        double vertical = 0.5 + Math.max(-0.2, Math.min(0.5, (aim.y - getY()) * 0.06));
        faceTowards(aim);
        setDeltaMovement(dir.x * horizontal, vertical, dir.z * horizontal);
        hasImpulse = true;
        leapStartY = getY();
        setState(STATE_LEAP);
        stateTicks = 0;
        level().playSound(null, getX(), getY(), getZ(), ModSounds.LURKER_POUNCE.get(), SoundSource.HOSTILE, 1.6f, 1.0f);
    }

    private void tickLeap() {
        // Un empujón en pleno salto lo derriba ("skeet").
        if (StaggerHandler.isStaggered(this)) {
            endLeap();
            return;
        }
        for (Entity e : level().getEntities(this, getBoundingBox().inflate(0.45))) {
            if (e instanceof ServerPlayer victim && isFreeSurvivor(victim) && PinService.pin(victim, this, PinType.POUNCE)) {
                double fall = leapStartY - getY();
                hurtPinned(victim, 5f + (fall >= 15 ? (float) Math.min(25, fall) : 0f));
                setState(STATE_PINNING);
                stateTicks = 0;
                setTarget(victim);
                return;
            }
        }
        if (stateTicks > 4 && (onGround() || isInWater())) endLeap();
        if (stateTicks > 60) endLeap();
    }

    private void endLeap() {
        setState(STATE_IDLE);
        cooldown = LEAP_COOLDOWN;
    }

    private void tickPinning() {
        ServerPlayer victim = PinService.victimOf(this);
        if (victim == null) {
            setState(STATE_IDLE);
            cooldown = LEAP_COOLDOWN;
            return;
        }
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        moveTo(victim.getX(), victim.getY() + 0.15, victim.getZ(), victim.getYRot() + 180f, 30f);
        if (stateTicks % 10 == 0) {
            hurtPinned(victim, 10f);
            level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.HOSTILE, 0.7f, 0.6f + random.nextFloat() * 0.3f);
        }
        if (stateTicks % 40 == 0) playSound(ModSounds.LURKER_AMBIENT.get(), 1.0f, 1.2f);
    }

    @Override
    public void onVictimReleased(ServerPlayer victim) {
        setState(STATE_IDLE);
        cooldown = LEAP_COOLDOWN;
        knockback(0.6, victim.getX() - getX(), victim.getZ() - getZ());
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return getState() == STATE_IDLE && super.doHurtTarget(target);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }
}
