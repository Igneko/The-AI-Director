package com.igneko.aidirector.entity.infected;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.entity.projectile.BehemothRockEntity;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Coloso: jefe de evento. Enorme reserva de vida según dificultad, inmune al empujón y al retroceso.
 * Sus puñetazos lanzan a los supervivientes por los aires y arranca rocas del suelo que lanza con
 * predicción balística (se pueden destruir en el aire a disparos). Su medidor de frustración baja si
 * no golpea ni ve a nadie; frustrado, se vuelve más rápido y derriba los bloques débiles a su paso.
 */
public class BehemothEntity extends SpecialInfectedEntity {

    public static final int STATE_IDLE = 0, STATE_ROCK = 1;

    public static final TagKey<Block> UNBREAKABLE = TagKey.create(Registries.BLOCK,
            new ResourceLocation(TheAIDirector.MOD_ID, "behemoth_unbreakable"));
    private static final UUID FRENZY_ID = UUID.fromString("5c2b8e41-7d3a-4f19-a6e0-93b1c4d5e6f7");

    private static final int ROCK_COOLDOWN = 300;
    private static final int ROCK_WINDUP = 30;

    private float frustration = 100f;
    private int rockCooldown = 100;
    private int stateTicks;
    private int stuckTicks;
    @Nullable private LivingEntity rockTarget;

    public BehemothEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, new Sounds(ModSounds.BEHEMOTH_AMBIENT, ModSounds.BEHEMOTH_HURT, ModSounds.BEHEMOTH_DEATH, ModSounds.BEHEMOTH_SPAWN));
        this.xpReward = 1000;
        setMaxUpStep(1.5f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return baseAttributes(800, 0.3, l4d(24))
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        double base = AIDirectorConfig.infected().behemothHealth();
        Difficulty d = level.getDifficulty();
        double mult = d == Difficulty.EASY ? 0.75 : d == Difficulty.HARD ? 2.0 : 1.0;
        var attr = getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) attr.setBaseValue(base * mult);
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    protected void registerBehaviourGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() { return getState() == STATE_IDLE && super.canUse(); }
            @Override
            public boolean canContinueToUse() { return getState() == STATE_IDLE && super.canContinueToUse(); }
            @Override
            protected double getAttackReachSqr(LivingEntity target) {
                return Math.pow(getBbWidth() * 1.6 + target.getBbWidth(), 2);
            }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount == 3) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.BEHEMOTH_ROAR.get(), SoundSource.HOSTILE, 4.0f, 1f);
            // TODO Fase 3: el AI Director congela las hordas normales mientras viva el Coloso.
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (rockCooldown > 0) rockCooldown--;
        stateTicks++;
        LivingEntity target = getTarget();

        // Frustración: baja si no ve ni golpea a nadie.
        boolean sees = target != null && hasLineOfSight(target);
        frustration = Math.max(0f, frustration - (sees ? 0.1f : 0.35f));
        if (frustration <= 0f) {
            frustration = 60f;
            level().playSound(null, getX(), getY(), getZ(), ModSounds.BEHEMOTH_ROAR.get(), SoundSource.HOSTILE, 3.0f, 0.9f);
        }
        updateFrenzy();

        if (getState() == STATE_ROCK) {
            tickRock();
            return;
        }
        if (target != null && rockCooldown == 0 && sees && onGround()) {
            double dist = distanceTo(target);
            if (dist >= 10 && dist <= 40) {
                rockTarget = target;
                setState(STATE_ROCK);
                stateTicks = 0;
                getNavigation().stop();
                level().playSound(null, getX(), getY(), getZ(), ModSounds.BEHEMOTH_ROAR.get(), SoundSource.HOSTILE, 2.5f, 1.1f);
            }
        }
        if (target != null) breakObstacles();
    }

    public boolean isFrenzied() {
        return frustration < 35f;
    }

    private void updateFrenzy() {
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        boolean has = speed.getModifier(FRENZY_ID) != null;
        if (isFrenzied() && !has) {
            speed.addTransientModifier(new AttributeModifier(FRENZY_ID, "aidirector:frenzy", 0.2, AttributeModifier.Operation.MULTIPLY_BASE));
        } else if (!isFrenzied() && has) {
            speed.removeModifier(FRENZY_ID);
        }
    }

    /** Arranca una roca y la lanza con predicción balística. */
    private void tickRock() {
        getNavigation().stop();
        if (rockTarget == null || !rockTarget.isAlive()) {
            setState(STATE_IDLE);
            return;
        }
        getLookControl().setLookAt(rockTarget, 30f, 30f);
        faceTowards(rockTarget.position());
        if (stateTicks == ROCK_WINDUP) {
            Vec3 start = position().add(0, getBbHeight() + 0.6, 0).add(getLookAngle().multiply(0.8, 0, 0.8));
            BehemothRockEntity rock = new BehemothRockEntity(level(), this);
            rock.setPos(start.x, start.y, start.z);
            rock.aimAt(start, rockTarget);
            level().addFreshEntity(rock);
            level().playSound(null, getX(), getY(), getZ(), ModSounds.BEHEMOTH_THROW.get(), SoundSource.HOSTILE, 2.0f, 1f);
        }
        if (stateTicks >= ROCK_WINDUP + 10) {
            setState(STATE_IDLE);
            rockCooldown = ROCK_COOLDOWN;
            rockTarget = null;
        }
    }

    /** Derriba los bloques débiles que le cierran el paso (más agresivo cuanto más frustrado). */
    private void breakObstacles() {
        if (!level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
        stuckTicks = horizontalCollision ? stuckTicks + 1 : 0;
        if (stuckTicks < (isFrenzied() ? 4 : 20)) return;
        stuckTicks = 0;
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
        BlockPos front = BlockPos.containing(getX() + fwd.x * (getBbWidth() * 0.5 + 0.6), getY(), getZ() + fwd.z * (getBbWidth() * 0.5 + 0.6));
        float maxHardness = isFrenzied() ? 5.0f : 2.5f;
        for (int dy = 0; dy < 3; dy++) {
            for (int side = -1; side <= 1; side++) {
                BlockPos p = front.offset((int) Math.round(-fwd.z * side), dy, (int) Math.round(fwd.x * side));
                BlockState state = level().getBlockState(p);
                float hardness = state.getDestroySpeed(level(), p);
                if (state.isAir() || hardness < 0 || hardness > maxHardness || state.is(UNBREAKABLE)) continue;
                level().destroyBlock(p, true, this);
            }
        }
    }

    /** Puñetazo: lanza al superviviente por los aires (y a quien esté a su lado). */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (getState() != STATE_IDLE) return false;
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            frustration = 100f;
            level().playSound(null, getX(), getY(), getZ(), ModSounds.BEHEMOTH_PUNCH.get(), SoundSource.HOSTILE, 2.0f, 1f);
            Vec3 dir = flatDirectionTo(target.position());
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(1.5),
                    e -> e != this && e.isAlive() && !(e instanceof SpecialInfectedEntity))) {
                e.setDeltaMovement(dir.x * 2.1, 0.95, dir.z * 2.1);
                e.hurtMarked = true;
                if (e != target) e.hurt(damageSources().mobAttack(this), l4d(24));
            }
        }
        return hit;
    }

    /** Llamado por la roca al impactar en un superviviente. */
    public void onRockHit() {
        frustration = 100f;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.BEHEMOTH_STEP.get(), 1.2f, 1f);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Frustration", frustration);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Frustration")) frustration = tag.getFloat("Frustration");
    }
}
