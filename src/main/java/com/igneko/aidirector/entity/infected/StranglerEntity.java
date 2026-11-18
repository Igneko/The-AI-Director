package com.igneko.aidirector.entity.infected;

import software.bernie.geckolib.core.animation.RawAnimation;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import com.igneko.aidirector.core.survivor.PinService;
import com.igneko.aidirector.core.survivor.PinType;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
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
import org.jetbrains.annotations.Nullable;

/**
 * Estrangulador: se mantiene a 15-25 bloques y lanza una lengua que atrapa y arrastra a un
 * superviviente. Durante el primer segundo la víctima aún puede dispararle; al llegar hasta él (o
 * atascarse contra un obstáculo) queda estrangulada. Al morir libera una nube de humo denso.
 */
public class StranglerEntity extends SpecialInfectedEntity implements PinService.Pinner {

    /** Marca de la nube de humo que deja al morir (los supervivientes dentro tosen). */
    public static final String SMOKE_TAG = "aidirector_smoke";


    private static final EntityDataAccessor<Integer> TONGUE_TARGET = SynchedEntityData.defineId(StranglerEntity.class, EntityDataSerializers.INT);

    private static final double TONGUE_RANGE = 25;
    private static final int TONGUE_COOLDOWN = 300; // 15 s
    private static final int FREE_SHOOT_TICKS = 20;  // 1 s

    private int cooldown = 40;
    private int pinTicks;
    private int stuckTicks;
    private Vec3 lastVictimPos = Vec3.ZERO;

    public StranglerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, new Sounds(ModSounds.STRANGLER_AMBIENT, ModSounds.STRANGLER_HURT, ModSounds.STRANGLER_DEATH, ModSounds.STRANGLER_SPAWN));
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return baseAttributes(50, 0.23, l4d(5));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(TONGUE_TARGET, -1);
    }

    /** Entidad atada por la lengua (cliente y servidor), para dibujarla. */
    @Nullable
    public Entity getTongueTarget() {
        int id = entityData.get(TONGUE_TARGET);
        return id < 0 ? null : level().getEntity(id);
    }

    @Override
    protected void registerBehaviourGoals() {
        goalSelector.addGoal(2, new KeepDistanceGoal(this, 12, 22, 1.0) {
            @Override
            public boolean canUse() { return getTongueTarget() == null && super.canUse(); }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (cooldown > 0) cooldown--;
        ServerPlayer victim = PinService.victimOf(this);
        if (victim != null) {
            tickTongue(victim);
            return;
        }
        LivingEntity target = getTarget();
        if (cooldown == 0 && target instanceof ServerPlayer player && isFreeSurvivor(player)) {
            double dist = distanceTo(player);
            if (dist >= 3 && dist <= TONGUE_RANGE && hasLineOfSight(player)) shootTongue(player);
        }
    }

    private void shootTongue(ServerPlayer victim) {
        if (!PinService.pin(victim, this, PinType.DRAG)) return;
        entityData.set(TONGUE_TARGET, victim.getId());
        pinTicks = 0;
        stuckTicks = 0;
        lastVictimPos = victim.position();
        getNavigation().stop();
        level().playSound(null, getX(), getY(), getZ(), ModSounds.STRANGLER_TONGUE.get(), SoundSource.HOSTILE, 1.5f, 1f);
    }

    private void tickTongue(ServerPlayer victim) {
        pinTicks++;
        getNavigation().stop();
        getLookControl().setLookAt(victim, 90f, 90f);
        Vec3 mouth = position().add(0, getEyeHeight() - 0.3, 0);
        Vec3 toMouth = mouth.subtract(victim.position().add(0, 1.0, 0));
        double dist = toMouth.length();

        if (dist > TONGUE_RANGE + 6) {
            PinService.release(victim); // la lengua se rompe
            return;
        }

        boolean arrived = dist < 1.8;
        double moved = victim.position().distanceTo(lastVictimPos);
        lastVictimPos = victim.position();
        stuckTicks = moved < 0.04 && !arrived ? stuckTicks + 1 : 0;
        boolean stuck = stuckTicks > 12;

        if (!arrived && !stuck) {
            Vec3 pull = toMouth.normalize().scale(0.28);
            victim.setDeltaMovement(pull.x, Math.max(pull.y, victim.onGround() ? 0.04 : pull.y), pull.z);
            victim.hurtMarked = true;
        } else {
            victim.setDeltaMovement(Vec3.ZERO);
            victim.hurtMarked = true;
        }

        // Tras el primer segundo, o al llegar/atascarse, la víctima ya no puede disparar.
        if ((pinTicks > FREE_SHOOT_TICKS || arrived || stuck) && PinService.pinType(victim) == PinType.DRAG) {
            PinService.setType(victim, PinType.CHOKE);
        }
        if (pinTicks > FREE_SHOOT_TICKS && pinTicks % 20 == 0) hurtPinned(victim, 5f);
    }

    @Override
    public void onVictimReleased(ServerPlayer victim) {
        entityData.set(TONGUE_TARGET, -1);
        cooldown = TONGUE_COOLDOWN;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide()) releaseSmoke();
        super.die(source);
    }

    /** Nube de humo denso: reduce la visibilidad y hace toser a quien la atraviesa. */
    private void releaseSmoke() {
        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY() + 0.2, getZ());
        cloud.setRadius(3.5f);
        cloud.setRadiusOnUse(0f);
        cloud.setRadiusPerTick(-0.01f);
        cloud.setDuration(140);
        cloud.setWaitTime(0);
        cloud.setParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE);
        cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
        cloud.setOwner(this);
        cloud.addTag(SMOKE_TAG);
        level().addFreshEntity(cloud);
        serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 60, 1.5, 0.8, 1.5, 0.02);
        level().playSound(null, getX(), getY(), getZ(), ModSounds.SMOKE_COUGH.get(), SoundSource.HOSTILE, 1.4f, 1f);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return getTongueTarget() == null && super.doHurtTarget(target);
    }

    // ================================================================== animación y efectos

    @Override
    protected RawAnimation mainAnimation(boolean moving) {
        if (getTongueTarget() != null) return loop("tongue");
        return moving ? loop("walk") : loop("idle");
    }

    @Override
    protected void clientEffects() {
        Vec3 mouth = getEyePosition().add(getLookAngle().scale(0.4));
        if (tickCount % 70 == 0) {
            for (int i = 0; i < 5; i++) level().addParticle(ParticleTypes.SMOKE, mouth.x, mouth.y - 0.2, mouth.z,
                    getLookAngle().x * 0.05, 0.02, getLookAngle().z * 0.05);
        }
        if (getTongueTarget() != null && random.nextInt(2) == 0) {
            level().addParticle(SALIVA, mouth.x, mouth.y - 0.25, mouth.z, 0, -0.02, 0);
        }
    }

    private static final DustParticleOptions SALIVA = new DustParticleOptions(new Vector3f(0.77f, 0.42f, 0.48f), 0.8f);
}
