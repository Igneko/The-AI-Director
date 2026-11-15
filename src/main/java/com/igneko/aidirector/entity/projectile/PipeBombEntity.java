package com.igneko.aidirector.entity.projectile;

import com.igneko.aidirector.api.event.AcousticPulseEvent;
import com.igneko.aidirector.compat.enhancedhordes.EnhancedHordesCompat;
import com.igneko.aidirector.entity.infected.SpecialInfectedEntity;
import com.igneko.aidirector.core.director.BileService;
import com.igneko.aidirector.registry.ModEntities;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Vector3f;

/**
 * Bomba casera: rebota al caer, pita cada vez más rápido y deja una estela de luz roja. Durante su
 * mecha de 5 s atrae a todos los infectados comunes (no a los especiales) en 32 bloques y después
 * estalla, dañando a los infectados en un área de 5×5 sobre ella.
 */
public class PipeBombEntity extends ThrowableProjectile {

    public static final int FUSE_TICKS = 100;
    private static final double LURE_RADIUS = 32;
    private static final float BLAST_HALF = 2.5f;
    private static final DustParticleOptions RED_LIGHT = new DustParticleOptions(new Vector3f(1.0f, 0.08f, 0.05f), 1.1f);

    private boolean resting;
    private int nextBeep;

    public PipeBombEntity(EntityType<? extends PipeBombEntity> type, Level level) {
        super(type, level);
    }

    public PipeBombEntity(Level level, LivingEntity owner) {
        super(ModEntities.PIPE_BOMB.get(), owner, level);
    }

    @Override
    protected void defineSynchedData() {}

    public boolean isResting() {
        return resting;
    }

    @Override
    public void tick() {
        if (resting) {
            baseTick();
            setDeltaMovement(Vec3.ZERO);
        } else {
            super.tick();
        }

        if (level().isClientSide()) {
            // Estela de luz roja y parpadeo.
            level().addParticle(RED_LIGHT, getX(), getY() + 0.15, getZ(), 0, 0, 0);
            if (tickCount % Math.max(3, 12 - tickCount / 10) == 0) {
                level().addParticle(new DustParticleOptions(new Vector3f(1f, 0.1f, 0.1f), 2.2f), getX(), getY() + 0.3, getZ(), 0, 0, 0);
            }
            return;
        }

        if (tickCount >= nextBeep) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.PIPEBOMB_BEEP.get(), SoundSource.NEUTRAL, 1.4f, 1f);
            nextBeep = tickCount + Math.max(3, 16 - tickCount / 7);
        }
        if (tickCount % 10 == 0) lure();
        if (tickCount >= FUSE_TICKS) explode();
    }

    /** Atrae a los infectados comunes; los especiales no caen en la trampa. */
    private void lure() {
        for (Monster mob : level().getEntitiesOfClass(Monster.class, getBoundingBox().inflate(LURE_RADIUS),
                m -> m.isAlive() && !(m instanceof SpecialInfectedEntity) && m.getType().is(EnhancedHordesCompat.HORDE_ENTITIES))) {
            if (mob.getTarget() != null && BileService.isBiled(mob.getTarget())) continue;
            mob.setTarget(null);
            mob.getNavigation().moveTo(getX(), getY(), getZ(), 1.25);
        }
    }

    private void explode() {
        ServerLevel server = (ServerLevel) level();
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 0.5, getZ(), 1, 0, 0, 0, 0);
        server.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 12, 0.8, 0.3, 0.8, 0.1);
        server.playSound(null, getX(), getY(), getZ(), ModSounds.PIPEBOMB_EXPLODE.get(), SoundSource.NEUTRAL, 3.0f, 1f);
        AABB blast = new AABB(getX() - BLAST_HALF, getY() - 1, getZ() - BLAST_HALF, getX() + BLAST_HALF, getY() + 3, getZ() + BLAST_HALF);
        LivingEntity owner = getOwner() instanceof LivingEntity le ? le : null;
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, blast, e -> e.isAlive() && !(e instanceof Player))) {
            e.hurt(damageSources().explosion(this, owner), 40f);
            Vec3 push = e.position().subtract(position()).multiply(1, 0, 1);
            if (push.lengthSqr() > 1e-4) push = push.normalize();
            e.push(push.x * 0.8, 0.5, push.z * 0.8);
        }
        MinecraftForge.EVENT_BUS.post(new AcousticPulseEvent(server, position(), 40, AcousticPulseEvent.Source.EXPLOSION, this));
        discard();
    }

    /** Rebota en el suelo y las paredes perdiendo energía hasta quedarse quieta. */
    @Override
    protected void onHitBlock(BlockHitResult hit) {
        Vec3 v = getDeltaMovement();
        Direction face = hit.getDirection();
        Vec3 bounced = switch (face.getAxis()) {
            case X -> new Vec3(-v.x * 0.45, v.y * 0.6, v.z * 0.6);
            case Y -> new Vec3(v.x * 0.55, -v.y * 0.4, v.z * 0.55);
            case Z -> new Vec3(v.x * 0.6, v.y * 0.6, -v.z * 0.45);
        };
        Vec3 at = hit.getLocation().add(face.getStepX() * 0.05, face.getStepY() * 0.05, face.getStepZ() * 0.05);
        setPos(at.x, at.y, at.z);
        if (face == Direction.UP && bounced.lengthSqr() < 0.01) {
            resting = true;
            setDeltaMovement(Vec3.ZERO);
        } else {
            setDeltaMovement(bounced);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        // Choca y cae al suelo junto a la entidad.
        setDeltaMovement(getDeltaMovement().multiply(-0.2, 0.2, -0.2));
    }

    @Override
    protected float getGravity() {
        return 0.05f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Resting", resting);
        tag.putInt("Age", tickCount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        resting = tag.getBoolean("Resting");
        tickCount = tag.getInt("Age");
    }
}
