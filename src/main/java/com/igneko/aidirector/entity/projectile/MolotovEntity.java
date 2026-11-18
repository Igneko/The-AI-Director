package com.igneko.aidirector.entity.projectile;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.registry.ModEntities;
import com.igneko.aidirector.registry.ModSounds;
import com.igneko.aidirector.world.saferoom.SafeZoneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Cóctel molotov: una botella con un trapo ardiendo. Al romperse crea una zona de fuego persistente
 * ({@link FirePatchEntity}) que abrasa a los infectados comunes, prende al Coloso y quema a los
 * supervivientes que la pisen. En el agua se apaga sin arder.
 */
public class MolotovEntity extends ThrowableProjectile {

    public MolotovEntity(EntityType<? extends MolotovEntity> type, Level level) {
        super(type, level);
    }

    public MolotovEntity(Level level, LivingEntity owner) {
        super(ModEntities.MOLOTOV.get(), owner, level);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected float getGravity() {
        return 0.05f;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY() + 0.15, getZ(), 0, 0.01, 0);
            if (tickCount % 2 == 0) level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 0, 0.02, 0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target != getOwner();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide()) return;
        shatter(result.getLocation());
        discard();
    }

    private void shatter(Vec3 at) {
        ServerLevel server = (ServerLevel) level();
        server.playSound(null, at.x, at.y, at.z, ModSounds.MOLOTOV_SHATTER.get(), SoundSource.NEUTRAL, 1.5f, 1f);
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GLASS_BOTTLE)), at.x, at.y, at.z, 12, 0.2, 0.2, 0.2, 0.15);
        if (isInWater() || server.getFluidState(BlockPos.containing(at)).isSource()) {
            server.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.2, at.z, 10, 0.3, 0.1, 0.3, 0.02);
            return;
        }

        // El fuego se derrama por el suelo bajo el impacto.
        BlockPos ground = BlockPos.containing(at);
        for (int i = 0; i < 6 && server.getBlockState(ground.below()).getCollisionShape(server, ground.below()).isEmpty(); i++) {
            ground = ground.below();
        }
        AIDirectorConfig.ItemSettings cfg = AIDirectorConfig.items();
        FirePatchEntity patch = FirePatchEntity.create(server, new Vec3(at.x, ground.getY(), at.z), cfg.molotovRadius(),
                cfg.molotovSeconds() * 20, getOwner());
        server.addFreshEntity(patch);
        server.sendParticles(ParticleTypes.FLAME, at.x, ground.getY() + 0.3, at.z, 60, cfg.molotovRadius() * 0.4, 0.2,
                cfg.molotovRadius() * 0.4, 0.05);
        server.playSound(null, at.x, at.y, at.z, ModSounds.MOLOTOV_IGNITE.get(), SoundSource.NEUTRAL, 2.0f, 1f);

        if (cfg.molotovIgnitesBlocks()) igniteBlocks(server, ground, cfg.molotovRadius());
    }

    private static void igniteBlocks(ServerLevel level, BlockPos center, float radius) {
        int r = (int) Math.ceil(radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > radius * radius || level.random.nextFloat() < 0.4f) continue;
                BlockPos pos = center.offset(dx, 0, dz);
                if (SafeZoneManager.zoneAt(level, pos) != null || !level.getBlockState(pos).isAir()) continue;
                if (BaseFireBlock.canBePlacedAt(level, pos, net.minecraft.core.Direction.UP)) {
                    level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
                }
            }
        }
    }
}
