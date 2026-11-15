package com.igneko.aidirector.entity.projectile;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.entity.infected.BehemothEntity;
import com.igneko.aidirector.registry.ModEntities;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Roca lanzada por el Coloso. Vuela con predicción balística y puede destruirse en el aire a disparos
 * (tiene su propia "vida"). Al impactar lanza por los aires a quien alcance.
 */
public class BehemothRockEntity extends ThrowableProjectile {

    private static final float GRAVITY = 0.045f;
    private static final double SPEED = 1.15;

    private float integrity = 12f;

    public BehemothRockEntity(EntityType<? extends BehemothRockEntity> type, Level level) {
        super(type, level);
    }

    public BehemothRockEntity(Level level, LivingEntity owner) {
        super(ModEntities.BEHEMOTH_ROCK.get(), owner, level);
    }

    /** Apunta a donde estará el objetivo cuando llegue la roca. */
    public void aimAt(Vec3 from, LivingEntity target) {
        Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0);
        double horizontal = Math.sqrt(aim.subtract(from).multiply(1, 0, 1).lengthSqr());
        double t = Math.max(1, horizontal / SPEED);
        aim = aim.add(target.getDeltaMovement().multiply(t, 0, t));
        Vec3 delta = aim.subtract(from);
        horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        t = Math.max(1, horizontal / SPEED);
        setDeltaMovement(delta.x / t, delta.y / t + 0.5 * GRAVITY * t, delta.z / t);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected float getGravity() {
        return GRAVITY;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public float getPickRadius() {
        return 0.6f;
    }

    /** Los disparos (de TaCZ o cualquier otro) desgastan la roca hasta hacerla añicos. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || level().isClientSide()) return false;
        if (source.getEntity() == getOwner()) return false;
        integrity -= amount;
        if (integrity <= 0) shatter();
        return true;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity e = result.getEntity();
        if (e == getOwner() || level().isClientSide()) return;
        if (e instanceof LivingEntity living) {
            Entity owner = getOwner();
            living.hurt(damageSources().mobProjectile(this, owner instanceof LivingEntity l ? l : null),
                    (float) (24 / 5.0 * AIDirectorConfig.infected().damageMultiplier()));
            Vec3 dir = getDeltaMovement().multiply(1, 0, 1).normalize();
            living.setDeltaMovement(dir.x * 1.6, 0.7, dir.z * 1.6);
            living.hurtMarked = true;
            if (owner instanceof BehemothEntity behemoth) behemoth.onRockHit();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) shatter();
    }

    private void shatter() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()),
                    getX(), getY(), getZ(), 40, 0.5, 0.5, 0.5, 0.15);
            server.playSound(null, getX(), getY(), getZ(), ModSounds.ROCK_SHATTER.get(), SoundSource.HOSTILE, 1.5f, 1f);
        }
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > 200) discard();
    }
}
