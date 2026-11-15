package com.igneko.aidirector.entity.projectile;

import com.igneko.aidirector.core.TheAIDirector;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Charco de ácido: se expande durante sus primeros segundos y daña a los supervivientes que lo pisan.
 * El daño empieza bajo y crece exponencialmente con el tiempo seguido dentro.
 */
public class AcidPuddleEntity extends Entity {

    public static final ResourceKey<DamageType> ACID = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(TheAIDirector.MOD_ID, "acid"));

    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(AcidPuddleEntity.class, EntityDataSerializers.FLOAT);
    private static final int DAMAGE_INTERVAL = 10;

    private float maxRadius = 2.8f;
    private int lifetime = 160;
    /** Ticks seguidos que cada superviviente lleva dentro del ácido. */
    private final Map<UUID, Integer> exposure = new HashMap<>();
    private final Map<UUID, Integer> lastSeen = new HashMap<>();

    public AcidPuddleEntity(EntityType<? extends AcidPuddleEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static AcidPuddleEntity create(Level level, Vec3 pos, float radius, int lifetime) {
        AcidPuddleEntity puddle = new AcidPuddleEntity(ModEntities.ACID_PUDDLE.get(), level);
        puddle.setPos(pos.x, pos.y, pos.z);
        puddle.maxRadius = radius;
        puddle.lifetime = lifetime;
        puddle.entityData.set(RADIUS, 0.5f);
        return puddle;
    }

    public float getRadius() {
        return entityData.get(RADIUS);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(RADIUS, 0.5f);
    }

    @Override
    public void tick() {
        super.tick();
        float r = getRadius();
        if (level().isClientSide()) {
            int n = Math.max(2, (int) (r * r * 2.5));
            for (int i = 0; i < n; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double d = Math.sqrt(random.nextDouble()) * r;
                level().addParticle(AcidBallEntity.ACID, getX() + Math.cos(a) * d, getY() + 0.05, getZ() + Math.sin(a) * d, 0, 0.01, 0);
            }
            if (random.nextInt(6) == 0) {
                level().addParticle(ParticleTypes.SMOKE, getX() + (random.nextDouble() - 0.5) * r, getY() + 0.1,
                        getZ() + (random.nextDouble() - 0.5) * r, 0, 0.03, 0);
            }
            return;
        }

        // Crece durante los 2 primeros segundos y se encoge en el último.
        float grow = Math.min(1f, tickCount / 40f);
        float shrink = tickCount > lifetime - 20 ? (lifetime - tickCount) / 20f : 1f;
        entityData.set(RADIUS, Math.max(0.3f, maxRadius * grow * shrink));
        if (tickCount >= lifetime) {
            discard();
            return;
        }
        if (tickCount % 30 == 0) level().playSound(null, getX(), getY(), getZ(), ModSounds.ACID_SIZZLE.get(), SoundSource.HOSTILE, 0.8f, 1f);
        if (tickCount % DAMAGE_INTERVAL == 0) damageInside(r);
    }

    private void damageInside(float r) {
        AABB box = new AABB(getX() - r, getY() - 0.5, getZ() - r, getX() + r, getY() + 1.2, getZ() + r);
        for (Player p : level().getEntitiesOfClass(Player.class, box, SpecialInfectedEntity::isSurvivor)) {
            double dx = p.getX() - getX(), dz = p.getZ() - getZ();
            if (dx * dx + dz * dz > r * r) continue;
            UUID id = p.getUUID();
            int last = lastSeen.getOrDefault(id, -100);
            int inside = tickCount - last <= DAMAGE_INTERVAL ? exposure.getOrDefault(id, 0) + DAMAGE_INTERVAL : 0;
            exposure.put(id, inside);
            lastSeen.put(id, tickCount);
            float seconds = inside / 20f;
            // L4D2: empieza en ~1 punto y escala exponencialmente (tope 6 vanilla por medio segundo).
            float damage = (float) Math.min(6.0, 0.5 * Math.pow(1.55, seconds));
            p.invulnerableTime = 0;
            p.hurt(acidDamage(), damage);
        }
    }

    private DamageSource acidDamage() {
        return new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ACID));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        maxRadius = tag.getFloat("MaxRadius");
        lifetime = tag.getInt("Lifetime");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("MaxRadius", maxRadius);
        tag.putInt("Lifetime", lifetime);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
