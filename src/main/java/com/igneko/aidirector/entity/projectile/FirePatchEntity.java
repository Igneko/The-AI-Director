package com.igneko.aidirector.entity.projectile;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.entity.infected.BehemothEntity;
import com.igneko.aidirector.entity.infected.SpecialInfectedEntity;
import com.igneko.aidirector.registry.ModEntities;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Charco de fuego del molotov: arde durante 15 s en un círculo de unos 3 bloques. Los infectados
 * comunes mueren casi al instante (el daño ignora la resistencia al fuego de la horda), los
 * especiales sufren y el Coloso queda en llamas. Los supervivientes reciben un poco de fuego amigo.
 */
public class FirePatchEntity extends Entity {

    public static final ResourceKey<DamageType> MOLOTOV = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(TheAIDirector.MOD_ID, "molotov"));

    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(FirePatchEntity.class, EntityDataSerializers.FLOAT);
    private static final int DAMAGE_INTERVAL = 10;

    private float maxRadius = 3.2f;
    private int lifetime = 300;
    @Nullable private UUID ownerId;

    public FirePatchEntity(EntityType<? extends FirePatchEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static FirePatchEntity create(Level level, Vec3 pos, float radius, int lifetime, @Nullable Entity owner) {
        FirePatchEntity patch = new FirePatchEntity(ModEntities.FIRE_PATCH.get(), level);
        patch.setPos(pos.x, pos.y, pos.z);
        patch.maxRadius = radius;
        patch.lifetime = lifetime;
        patch.ownerId = owner == null ? null : owner.getUUID();
        patch.entityData.set(RADIUS, radius * 0.6f);
        return patch;
    }

    public float getRadius() {
        return entityData.get(RADIUS);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(RADIUS, 1f);
    }

    @Override
    public void tick() {
        super.tick();
        float r = getRadius();
        if (level().isClientSide()) {
            int n = Math.max(3, (int) (r * r * 2.2));
            for (int i = 0; i < n; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double d = Math.sqrt(random.nextDouble()) * r;
                double x = getX() + Math.cos(a) * d, z = getZ() + Math.sin(a) * d;
                level().addParticle(random.nextInt(4) == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME,
                        x, getY() + 0.05 + random.nextDouble() * 0.3, z, 0, 0.02 + random.nextDouble() * 0.04, 0);
            }
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + (random.nextDouble() - 0.5) * r, getY() + 0.8,
                        getZ() + (random.nextDouble() - 0.5) * r, 0, 0.05, 0);
            }
            if (random.nextInt(8) == 0) {
                level().addParticle(ParticleTypes.LAVA, getX() + (random.nextDouble() - 0.5) * r, getY() + 0.1,
                        getZ() + (random.nextDouble() - 0.5) * r, 0, 0, 0);
            }
            return;
        }

        // Se extiende durante el primer segundo y se consume en el último.
        float grow = Math.min(1f, 0.6f + tickCount / 50f);
        float shrink = tickCount > lifetime - 30 ? Math.max(0.2f, (lifetime - tickCount) / 30f) : 1f;
        entityData.set(RADIUS, maxRadius * grow * shrink);
        if (tickCount >= lifetime || isInWaterOrRain() && tickCount > 40 && random.nextInt(40) == 0) {
            discard();
            return;
        }
        if (tickCount % 30 == 0) level().playSound(null, getX(), getY(), getZ(), ModSounds.FIRE_CRACKLE.get(), SoundSource.NEUTRAL, 1.2f, 1f);
        if (tickCount % DAMAGE_INTERVAL == 0) burn(r);
    }

    private void burn(float r) {
        AIDirectorConfig.ItemSettings cfg = AIDirectorConfig.items();
        AABB box = new AABB(getX() - r, getY() - 0.5, getZ() - r, getX() + r, getY() + 1.5, getZ() + r);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            if (dx * dx + dz * dz > r * r) continue;
            if (e instanceof Player p) {
                if (p.isCreative() || p.isSpectator() || cfg.molotovFriendlyDamage() <= 0) continue;
                p.hurt(fire(), cfg.molotovFriendlyDamage());
                p.setSecondsOnFire(2);
                continue;
            }
            e.invulnerableTime = 0;
            if (e instanceof BehemothEntity) {
                // El Coloso no muere en el charco: se prende y arde (pierde vida y acelera).
                e.hurt(fire(), cfg.molotovDamage() * 0.25f);
                e.setSecondsOnFire(30);
            } else if (e instanceof SpecialInfectedEntity) {
                e.hurt(fire(), cfg.molotovDamage() * 0.6f);
                e.setSecondsOnFire(6);
            } else {
                e.hurt(fire(), cfg.molotovDamage());
                e.setSecondsOnFire(6);
            }
        }
    }

    private DamageSource fire() {
        Entity owner = ownerId == null || !(level() instanceof net.minecraft.server.level.ServerLevel s) ? null : s.getEntity(ownerId);
        return new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(MOLOTOV), this, owner);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        maxRadius = tag.getFloat("MaxRadius");
        lifetime = tag.getInt("Lifetime");
        tickCount = tag.getInt("Age");
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("MaxRadius", maxRadius);
        tag.putInt("Lifetime", lifetime);
        tag.putInt("Age", tickCount);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
