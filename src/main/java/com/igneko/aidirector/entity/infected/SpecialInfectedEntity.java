package com.igneko.aidirector.entity.infected;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.core.survivor.PinService;
import com.igneko.aidirector.core.survivor.SurvivorCapability;
import com.igneko.aidirector.core.survivor.SurvivorHealthData;
import com.igneko.aidirector.core.survivor.SurvivorService;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Base de los Infectados Especiales: estado sincronizado para las animaciones, objetivos (solo
 * supervivientes), aviso sonoro al aparecer, daño escalable por configuración y liberación de
 * cualquier superviviente atrapado al morir o desaparecer.
 */
public abstract class SpecialInfectedEntity extends Monster {

    public static final ResourceKey<DamageType> PIN_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(TheAIDirector.MOD_ID, "infected_pin"));

    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(SpecialInfectedEntity.class, EntityDataSerializers.BYTE);

    /** Conjunto de sonidos de cada infectado. */
    public record Sounds(Supplier<SoundEvent> ambient, Supplier<SoundEvent> hurt, Supplier<SoundEvent> death,
                         @Nullable Supplier<SoundEvent> spawn) {}

    private final Sounds sounds;

    protected SpecialInfectedEntity(EntityType<? extends Monster> type, Level level, Sounds sounds) {
        super(type, level);
        this.sounds = sounds;
        this.xpReward = 10;
    }

    protected static AttributeSupplier.Builder baseAttributes(double health, double speed, double damage) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    // ------------------------------------------------------------------ estado sincronizado

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(STATE, (byte) 0);
    }

    public int getState() {
        return entityData.get(STATE);
    }

    protected void setState(int state) {
        if (getState() != state) entityData.set(STATE, (byte) state);
    }

    // ------------------------------------------------------------------ objetivos

    /** Solo persiguen a supervivientes gestionados (no creativo/espectador) y vivos. */
    public static boolean isSurvivor(LivingEntity entity) {
        return entity instanceof Player p && SurvivorService.isManaged(p) && p.isAlive()
                && SurvivorCapability.get(p).map(d -> !d.isDead()).orElse(false);
    }

    public static boolean isFreeSurvivor(LivingEntity entity) {
        return isSurvivor(entity) && !PinService.isPinned((Player) entity);
    }

    public static boolean isDowned(LivingEntity entity) {
        return entity instanceof Player p && SurvivorCapability.get(p).map(SurvivorHealthData::isIncapacitated).orElse(false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        registerBehaviourGoals();
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                SpecialInfectedEntity::isSurvivor));
    }

    protected abstract void registerBehaviourGoals();

    // ------------------------------------------------------------------ daño

    /** Convierte daño L4D2 a puntos vanilla (÷5) con el multiplicador de configuración. */
    protected static float l4d(float l4d2Damage) {
        return (float) (l4d2Damage / 5f * AIDirectorConfig.infected().damageMultiplier());
    }

    /** Daño de agarre: no empuja a la víctima (si no, saldría despedida del agarre). */
    protected DamageSource pinDamage() {
        return new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(PIN_DAMAGE), this);
    }

    protected void hurtPinned(LivingEntity victim, float l4d2Damage) {
        victim.invulnerableTime = 0;
        victim.hurt(pinDamage(), l4d(l4d2Damage));
    }

    // ------------------------------------------------------------------ ciclo de vida

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        setPersistenceRequired();
        double mult = AIDirectorConfig.infected().healthMultiplier();
        if (mult != 1.0) {
            var attr = getAttribute(Attributes.MAX_HEALTH);
            if (attr != null) attr.setBaseValue(attr.getBaseValue() * mult);
        }
        setHealth(getMaxHealth());
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount == 2 && sounds.spawn() != null) {
            // Aviso musical al aparecer (se oye a unos 48 bloques) para permitir reaccionar.
            level().playSound(null, getX(), getY(), getZ(), sounds.spawn().get(), SoundSource.HOSTILE, 3.0f, 1.0f);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide()) PinService.releaseAllBy(this);
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide()) PinService.releaseAllBy(this);
        super.remove(reason);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return sounds.ambient().get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return sounds.hurt().get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return sounds.death().get();
    }

    // ------------------------------------------------------------------ utilidades de movimiento

    /** Vector horizontal normalizado hacia un punto. */
    protected Vec3 flatDirectionTo(Vec3 target) {
        Vec3 d = target.subtract(position()).multiply(1, 0, 1);
        return d.lengthSqr() < 1e-6 ? Vec3.ZERO : d.normalize();
    }

    protected void faceTowards(Vec3 target) {
        Vec3 d = target.subtract(position());
        float yaw = (float) (Math.atan2(d.z, d.x) * 180.0 / Math.PI) - 90f;
        setYRot(yaw);
        setYHeadRot(yaw);
        yBodyRot = yaw;
    }

    @Override
    public boolean isSensitiveToWater() {
        return false;
    }

    protected ServerLevel serverLevel() {
        return (ServerLevel) level();
    }
}
