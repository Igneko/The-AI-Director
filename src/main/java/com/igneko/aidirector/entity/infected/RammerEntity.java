package com.igneko.aidirector.entity.infected;

import com.igneko.aidirector.core.survivor.PinService;
import com.igneko.aidirector.core.survivor.PinType;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Embestidor: inmune al empujón. Tras un rugido, carga en línea recta: atrapa al primer superviviente
 * con el que choca y se lo lleva por delante hasta un muro (o 25 bloques); a los demás los lanza por
 * los aires y los aturde. Después golpea a su presa contra el suelo una y otra vez.
 */
public class RammerEntity extends SpecialInfectedEntity implements PinService.Pinner {

    public static final int STATE_IDLE = 0, STATE_WINDUP = 1, STATE_CHARGE = 2, STATE_POUND = 3;

    private static final int WINDUP_TICKS = 12;
    private static final double CHARGE_SPEED = 0.78;
    private static final double MAX_CHARGE = 25;
    private static final int CHARGE_COOLDOWN = 240;

    private int cooldown = 40;
    private int stateTicks;
    private Vec3 chargeDir = Vec3.ZERO;
    private Vec3 chargeStart = Vec3.ZERO;
    private final Set<Entity> flung = new HashSet<>();

    public RammerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, new Sounds(ModSounds.RAMMER_AMBIENT, ModSounds.RAMMER_HURT, ModSounds.RAMMER_DEATH, ModSounds.RAMMER_SPAWN));
        this.xpReward = 90;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return baseAttributes(120, 0.27, l4d(10)).add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void registerBehaviourGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() { return getState() == STATE_IDLE && super.canUse(); }
            @Override
            public boolean canContinueToUse() { return getState() == STATE_IDLE && super.canContinueToUse(); }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 20f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (cooldown > 0) cooldown--;
        stateTicks++;
        switch (getState()) {
            case STATE_WINDUP -> tickWindup();
            case STATE_CHARGE -> tickCharge();
            case STATE_POUND -> tickPound();
            default -> tryStartCharge();
        }
    }

    private void tryStartCharge() {
        LivingEntity target = getTarget();
        if (cooldown > 0 || target == null || !isFreeSurvivor(target) || !onGround()) return;
        double dist = distanceTo(target);
        if (dist < 5 || dist > 20 || Math.abs(target.getY() - getY()) > 3 || !hasLineOfSight(target)) return;
        setState(STATE_WINDUP);
        stateTicks = 0;
        getNavigation().stop();
        level().playSound(null, getX(), getY(), getZ(), ModSounds.RAMMER_ROAR.get(), SoundSource.HOSTILE, 2.0f, 1f);
    }

    private void tickWindup() {
        LivingEntity target = getTarget();
        getNavigation().stop();
        if (target == null) {
            setState(STATE_IDLE);
            return;
        }
        faceTowards(target.position());
        if (stateTicks < WINDUP_TICKS) return;
        chargeDir = flatDirectionTo(target.position());
        if (chargeDir == Vec3.ZERO) {
            setState(STATE_IDLE);
            return;
        }
        chargeStart = position();
        flung.clear();
        setState(STATE_CHARGE);
        stateTicks = 0;
    }

    private void tickCharge() {
        getNavigation().stop();
        faceTowards(position().add(chargeDir));
        setDeltaMovement(chargeDir.x * CHARGE_SPEED, getDeltaMovement().y, chargeDir.z * CHARGE_SPEED);
        if (stateTicks % 4 == 0) {
            serverLevel().sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 2, 0.3, 0, 0.3, 0.01);
        }

        ServerPlayer carried = PinService.victimOf(this);
        for (Entity e : level().getEntities(this, getBoundingBox().inflate(0.6))) {
            if (!(e instanceof LivingEntity living) || e == carried || flung.contains(e) || e.getVehicle() == this) continue;
            if (carried == null && e instanceof ServerPlayer p && isFreeSurvivor(p) && PinService.pin(p, this, PinType.CARRY)) {
                p.startRiding(this, true);
                carried = p;
                continue;
            }
            fling(living);
        }

        boolean wall = horizontalCollision;
        boolean tooFar = position().distanceTo(chargeStart) >= MAX_CHARGE || stateTicks > 60;
        if (wall || tooFar) endCharge(wall, carried);
    }

    /** Lanza por los aires y aturde a quien se cruce en la trayectoria. */
    private void fling(LivingEntity e) {
        flung.add(e);
        Vec3 side = new Vec3(-chargeDir.z, 0, chargeDir.x);
        if (side.dot(e.position().subtract(position())) < 0) side = side.scale(-1);
        Vec3 push = chargeDir.scale(0.9).add(side.scale(0.9));
        e.setDeltaMovement(push.x, 0.75, push.z);
        e.hurtMarked = true;
        e.hurt(damageSources().mobAttack(this), l4d(10));
        if (e instanceof Player) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3, false, false));
        level().playSound(null, e.getX(), e.getY(), e.getZ(), ModSounds.RAMMER_IMPACT.get(), SoundSource.HOSTILE, 1.2f, 1.1f);
    }

    private void endCharge(boolean wall, ServerPlayer carried) {
        setDeltaMovement(Vec3.ZERO);
        if (wall) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.RAMMER_IMPACT.get(), SoundSource.HOSTILE, 2.0f, 0.8f);
            serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level().getBlockState(blockPosition().relative(getDirection()))),
                    getX() + chargeDir.x, getY() + 1, getZ() + chargeDir.z, 30, 0.4, 0.6, 0.4, 0.1);
        }
        if (carried != null) {
            if (wall) hurtPinned(carried, 10f);
            PinService.setType(carried, PinType.POUND);
            setState(STATE_POUND);
        } else {
            setState(STATE_IDLE);
            cooldown = CHARGE_COOLDOWN;
        }
        stateTicks = 0;
    }

    private void tickPound() {
        ServerPlayer victim = PinService.victimOf(this);
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
        if (victim == null) {
            setState(STATE_IDLE);
            cooldown = CHARGE_COOLDOWN;
            return;
        }
        if (stateTicks % 30 == 15) {
            hurtPinned(victim, 15f);
            level().playSound(null, getX(), getY(), getZ(), ModSounds.RAMMER_IMPACT.get(), SoundSource.HOSTILE, 1.3f, 0.7f);
            serverLevel().sendParticles(ParticleTypes.POOF, victim.getX(), victim.getY() + 0.2, victim.getZ(), 6, 0.3, 0.1, 0.3, 0.02);
        }
    }

    /** La presa va delante del pecho al cargar y en el suelo, frente a él, al golpearla. */
    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        if (!hasPassenger(passenger)) return;
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
        boolean pounding = getState() == STATE_POUND;
        double ahead = pounding ? 1.5 : 1.05;
        double y = pounding ? 0.0 : 0.55;
        move.accept(passenger, getX() + fwd.x * ahead, getY() + y, getZ() + fwd.z * ahead);
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public void onVictimReleased(ServerPlayer victim) {
        setState(STATE_IDLE);
        cooldown = CHARGE_COOLDOWN;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return getState() == STATE_IDLE && super.doHurtTarget(target);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }
}
