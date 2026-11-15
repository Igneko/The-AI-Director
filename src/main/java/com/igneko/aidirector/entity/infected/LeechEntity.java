package com.igneko.aidirector.entity.infected;

import software.bernie.geckolib.core.animation.RawAnimation;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
import com.igneko.aidirector.core.survivor.PinService;
import com.igneko.aidirector.core.survivor.PinType;
import com.igneko.aidirector.entity.projectile.AcidPuddleEntity;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
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
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Parásito: pequeño y errático, salta de lado para esquivar la mira. Al alcanzar a un superviviente
 * se le sube encima y toma el control de su movimiento, llevándolo hacia peligros (fuego, lava,
 * ácido, precipicios) y lejos de sus compañeros.
 */
public class LeechEntity extends SpecialInfectedEntity implements PinService.Pinner {

    public static final int STATE_IDLE = 0, STATE_LEAP = 2, STATE_RIDING = 3;

    private static final double STEER_SPEED = 0.16;

    private int cooldown = 30;
    private int dodgeCooldown;
    private int stateTicks;
    private Vec3 steer = Vec3.ZERO;

    public LeechEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, new Sounds(ModSounds.LEECH_AMBIENT, ModSounds.LEECH_HURT, ModSounds.LEECH_DEATH, ModSounds.LEECH_SPAWN));
        this.xpReward = 50;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return baseAttributes(65, 0.33, l4d(4));
    }

    @Override
    protected void registerBehaviourGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true) {
            @Override
            public boolean canUse() { return getState() == STATE_IDLE && super.canUse(); }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Encaramado sobre los hombros y la cabeza de la víctima. */
    @Override
    public double getMyRidingOffset() {
        return 0.2;
    }

    @Override
    public void rideTick() {
        super.rideTick();
        // Mira siempre hacia donde mira su víctima, como si la guiara.
        if (getVehicle() instanceof Player p) {
            setYRot(p.getYRot());
            yBodyRot = p.yBodyRot;
            setYHeadRot(p.getYHeadRot());
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (cooldown > 0) cooldown--;
        if (dodgeCooldown > 0) dodgeCooldown--;
        stateTicks++;
        switch (getState()) {
            case STATE_RIDING -> tickRiding();
            case STATE_LEAP -> tickLeap();
            default -> tickHunting();
        }
    }

    private void tickHunting() {
        LivingEntity target = getTarget();
        if (target == null || !isFreeSurvivor(target)) return;
        double dist = distanceTo(target);

        // Esquiva lateral si el superviviente le está apuntando.
        if (dodgeCooldown == 0 && onGround() && dist < 16) {
            Vec3 toMe = position().subtract(target.getEyePosition()).normalize();
            if (target.getLookAngle().dot(toMe) > 0.97) {
                Vec3 side = new Vec3(-toMe.z, 0, toMe.x).scale(random.nextBoolean() ? 0.55 : -0.55);
                setDeltaMovement(side.x, 0.32, side.z);
                hasImpulse = true;
                dodgeCooldown = 18 + random.nextInt(12);
                return;
            }
        }

        if (cooldown == 0 && onGround() && dist <= 5.5 && dist >= 1.2 && hasLineOfSight(target)) {
            Vec3 dir = flatDirectionTo(target.position());
            faceTowards(target.position());
            setDeltaMovement(dir.x * (0.45 + dist * 0.08), 0.42, dir.z * (0.45 + dist * 0.08));
            hasImpulse = true;
            setState(STATE_LEAP);
            stateTicks = 0;
            level().playSound(null, getX(), getY(), getZ(), ModSounds.LEECH_LEAP.get(), SoundSource.HOSTILE, 1.4f, 1f);
        }
    }

    private void tickLeap() {
        for (Entity e : level().getEntities(this, getBoundingBox().inflate(0.5))) {
            if (e instanceof ServerPlayer victim && isFreeSurvivor(victim) && victim.getPassengers().isEmpty()
                    && PinService.pin(victim, this, PinType.RIDE)) {
                startRiding(victim, true);
                syncPassengers(victim);
                setState(STATE_RIDING);
                stateTicks = 0;
                steer = Vec3.ZERO;
                return;
            }
        }
        if ((stateTicks > 4 && onGround()) || stateTicks > 40) {
            setState(STATE_IDLE);
            cooldown = 30;
        }
    }

    private void tickRiding() {
        ServerPlayer victim = PinService.victimOf(this);
        if (victim == null || getVehicle() != victim) {
            setState(STATE_IDLE);
            cooldown = 60;
            return;
        }
        getNavigation().stop();
        if (stateTicks % 10 == 1) steer = chooseSteering(victim);
        Vec3 v = victim.getDeltaMovement();
        victim.setDeltaMovement(steer.x * STEER_SPEED, v.y, steer.z * STEER_SPEED);
        victim.hurtMarked = true;
        if (stateTicks % 20 == 0) hurtPinned(victim, 4f);
        if (stateTicks % 35 == 0) playSound(ModSounds.LEECH_AMBIENT.get(), 1.2f, 1f);
    }

    /** Dirección de guiado: hacia el peligro más cercano o, si no hay, deambular lejos de los aliados. */
    private Vec3 chooseSteering(ServerPlayer victim) {
        Vec3 hazard = findHazard(victim);
        if (hazard != null) {
            Vec3 d = hazard.subtract(victim.position()).multiply(1, 0, 1);
            if (d.lengthSqr() > 1e-4) return d.normalize();
        }
        Vec3 away = Vec3.ZERO;
        for (ServerPlayer other : serverLevel().players()) {
            if (other == victim || !isSurvivor(other) || other.distanceTo(victim) > 20) continue;
            away = away.add(victim.position().subtract(other.position()).multiply(1, 0, 1).normalize());
        }
        double angle = random.nextDouble() * Math.PI * 2;
        Vec3 wander = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        Vec3 dir = away.lengthSqr() > 1e-4 ? away.normalize().add(wander.scale(0.6)) : wander;
        return dir.normalize();
    }

    @Nullable
    private Vec3 findHazard(ServerPlayer victim) {
        for (AcidPuddleEntity puddle : level().getEntitiesOfClass(AcidPuddleEntity.class, victim.getBoundingBox().inflate(12))) {
            return puddle.position();
        }
        BlockPos base = victim.blockPosition();
        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -8; dx <= 8; dx += 2) {
            for (int dz = -8; dz <= 8; dz += 2) {
                for (int dy = -2; dy <= 1; dy++) {
                    BlockPos p = base.offset(dx, dy, dz);
                    boolean danger = level().getBlockState(p).getBlock() instanceof BaseFireBlock
                            || level().getFluidState(p).is(FluidTags.LAVA);
                    if (!danger && dy == 0 && isCliff(p)) danger = true;
                    if (!danger) continue;
                    double d = p.distSqr(base);
                    if (d < bestDist) {
                        bestDist = d;
                        best = Vec3.atCenterOf(p);
                    }
                }
            }
        }
        return best;
    }

    /** ¿Hay una caída de 4 o más bloques en esa columna? */
    private boolean isCliff(BlockPos p) {
        if (!level().getBlockState(p).getCollisionShape(level(), p).isEmpty()) return false;
        for (int i = 1; i <= 4; i++) {
            if (!level().getBlockState(p.below(i)).getCollisionShape(level(), p.below(i)).isEmpty()) return false;
        }
        return true;
    }

    /**
     * El servidor solo envía el paquete de pasajeros de una entidad a quienes la rastrean, y un jugador
     * no se rastrea a sí mismo: sin esto, la víctima vería al Parásito congelado en el aire.
     */
    private static void syncPassengers(ServerPlayer victim) {
        victim.connection.send(new ClientboundSetPassengersPacket(victim));
    }

    @Override
    public void onVictimReleased(ServerPlayer victim) {
        syncPassengers(victim);
        setState(STATE_IDLE);
        cooldown = 80;
        Vec3 back = position().subtract(victim.position()).multiply(1, 0, 1);
        if (back.lengthSqr() < 1e-4) back = new Vec3(1, 0, 0);
        back = back.normalize();
        setDeltaMovement(back.x * 0.5, 0.3, back.z * 0.5);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return getState() == STATE_IDLE && super.doHurtTarget(target);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // ================================================================== animación y efectos

    @Override
    public boolean shouldTrackHead() {
        return !isPassenger() && getState() != STATE_LEAP;
    }

    @Override
    protected RawAnimation mainAnimation(boolean moving) {
        if (isPassenger()) return loop("ride");
        if (getState() == STATE_LEAP) return loop("leap");
        return moving ? loop("run") : loop("idle");
    }

    @Override
    protected void clientEffects() {
        if (onGround() && getDeltaMovement().horizontalDistance() > 0.1 && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.POOF, getX(), getY() + 0.05, getZ(), 0, 0.01, 0);
        }
        if (isPassenger() && random.nextInt(8) == 0) {
            Vec3 mouth = getEyePosition();
            level().addParticle(DROOL, mouth.x, mouth.y - 0.3, mouth.z, 0, -0.05, 0);
        }
    }

    private static final DustParticleOptions DROOL = new DustParticleOptions(new Vector3f(0.85f, 0.8f, 0.6f), 0.7f);
}
