package com.igneko.aidirector.entity.projectile;

import com.igneko.aidirector.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Proyectil parabólico de ácido de la Corrosiva: al tocar algo crea un charco de ácido. */
public class AcidBallEntity extends ThrowableProjectile {

    public static final DustParticleOptions ACID = new DustParticleOptions(new Vector3f(0.55f, 1.0f, 0.15f), 1.3f);
    private static final float GRAVITY = 0.05f;

    public AcidBallEntity(EntityType<? extends AcidBallEntity> type, Level level) {
        super(type, level);
    }

    public AcidBallEntity(Level level, LivingEntity owner) {
        super(ModEntities.ACID_BALL.get(), owner, level);
    }

    /** Dispara hacia {@code target} con velocidad horizontal {@code speed}, compensando la gravedad. */
    public void shootAt(Vec3 from, Vec3 target, double speed) {
        Vec3 delta = target.subtract(from);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double t = Math.max(1, horizontal / speed);
        double vy = delta.y / t + 0.5 * GRAVITY * t;
        setDeltaMovement(delta.x / t, vy, delta.z / t);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected float getGravity() {
        return GRAVITY;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            for (int i = 0; i < 3; i++) {
                level().addParticle(ACID, getX() + (random.nextDouble() - 0.5) * 0.3, getY() + (random.nextDouble() - 0.5) * 0.3,
                        getZ() + (random.nextDouble() - 0.5) * 0.3, 0, 0, 0);
            }
        } else if (tickCount > 200) {
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide()) return;
        Vec3 at = result.getLocation();
        // El charco se asienta sobre el suelo bajo el impacto.
        BlockPos pos = BlockPos.containing(at);
        for (int i = 0; i < 6 && level().getBlockState(pos.below()).getCollisionShape(level(), pos.below()).isEmpty(); i++) pos = pos.below();
        level().addFreshEntity(AcidPuddleEntity.create(level(), new Vec3(at.x, pos.getY(), at.z), 2.8f, 160));
        discard();
    }
}
