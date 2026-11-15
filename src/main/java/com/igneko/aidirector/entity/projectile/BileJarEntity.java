package com.igneko.aidirector.entity.projectile;

import com.igneko.aidirector.compat.enhancedhordes.EnhancedHordesCompat;
import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.director.BileService;
import com.igneko.aidirector.registry.ModEntities;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Frasco de bilis: al romperse deja una nube verdosa que llama a una horda y atrae a los comunes
 * cercanos. Si impacta directamente en alguien (un infectado especial u otro jugador), queda cubierto
 * de bilis y la horda va a por él.
 */
public class BileJarEntity extends ThrowableProjectile {

    public static final DustParticleOptions BILE = new DustParticleOptions(new Vector3f(0.45f, 0.65f, 0.12f), 1.8f);
    private static final int CLOUD_TICKS = 300;

    @Nullable private Entity directHit;

    public BileJarEntity(EntityType<? extends BileJarEntity> type, Level level) {
        super(type, level);
    }

    public BileJarEntity(Level level, LivingEntity owner) {
        super(ModEntities.BILE_JAR.get(), owner, level);
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
        if (level().isClientSide() && tickCount % 2 == 0) {
            level().addParticle(BILE, getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target != getOwner();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        directHit = hit.getEntity();
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
        server.playSound(null, at.x, at.y, at.z, ModSounds.BILEJAR_SHATTER.get(), SoundSource.NEUTRAL, 1.5f, 1f);
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GLASS_BOTTLE)), at.x, at.y, at.z, 12, 0.2, 0.2, 0.2, 0.15);
        server.sendParticles(BILE, at.x, at.y + 0.3, at.z, 50, 1.2, 0.5, 1.2, 0.02);

        // Quien recibe el frasco en la cabeza queda cubierto de bilis.
        if (directHit instanceof ServerPlayer player) {
            BileService.cover(player);
        } else if (directHit instanceof LivingEntity living && !(directHit instanceof Player)) {
            BileService.coverMob(living);
        }
        // Salpicadura: los infectados alrededor también quedan marcados.
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(2.5),
                e -> e.isAlive() && !(e instanceof Player) && e != directHit)) {
            BileService.coverMob(e);
        }

        // Nube verdosa en el suelo que atrae a los comunes durante 15 s.
        BlockPos ground = BlockPos.containing(at);
        for (int i = 0; i < 6 && server.getBlockState(ground.below()).getCollisionShape(server, ground.below()).isEmpty(); i++) ground = ground.below();
        Vec3 cloudPos = new Vec3(at.x, ground.getY(), at.z);
        AreaEffectCloud cloud = new AreaEffectCloud(server, cloudPos.x, cloudPos.y, cloudPos.z);
        cloud.setRadius(2.5f);
        cloud.setRadiusPerTick(-0.004f);
        cloud.setDuration(CLOUD_TICKS);
        cloud.setWaitTime(0);
        cloud.setParticle(BILE);
        if (getOwner() instanceof LivingEntity owner) cloud.setOwner(owner);
        server.addFreshEntity(cloud);
        BileService.addLurePoint(server, cloudPos, CLOUD_TICKS);

        int count = AIDirectorConfig.infected().bileHordeSize();
        if (count > 0) {
            ServerPlayer focus = directHit instanceof ServerPlayer p ? p : null;
            EnhancedHordesCompat.triggerPanicEvent(server, ground, count, focus);
        }
    }
}
