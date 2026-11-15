package com.igneko.aidirector.entity.infected;

import software.bernie.geckolib.core.animation.RawAnimation;
import com.igneko.aidirector.core.director.BileService;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Hinchado: lento y frágil (50 HP L4D2). Se acerca a escondidas y vomita bilis en un cono de
 * 7 bloques: los alcanzados quedan cubiertos (ceguera parcial y una horda va a por ellos).
 * Al morir revienta, empujando a todos y salpicando bilis a menos de 3,5 bloques.
 */
public class BloaterEntity extends SpecialInfectedEntity {

    public static final int STATE_IDLE = 0, STATE_VOMIT = 1;

    private static final double VOMIT_RANGE = 7;
    private static final double VOMIT_CONE_COS = Math.cos(Math.toRadians(35));
    private static final int VOMIT_COOLDOWN = 600; // 30 s
    private static final DustParticleOptions BILE = new DustParticleOptions(new Vector3f(0.45f, 0.62f, 0.12f), 1.6f);

    private int cooldown = 20;
    private int vomitTicks;
    private boolean burst;

    public BloaterEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, new Sounds(ModSounds.BLOATER_AMBIENT, ModSounds.BLOATER_HURT, ModSounds.BLOATER_EXPLODE, ModSounds.BLOATER_SPAWN));
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return baseAttributes(10, 0.18, l4d(5));
    }

    @Override
    protected void registerBehaviourGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (cooldown > 0) cooldown--;
        if (getState() == STATE_VOMIT) {
            tickVomit();
            return;
        }
        LivingEntity target = getTarget();
        if (cooldown == 0 && target != null && isSurvivor(target) && distanceTo(target) <= VOMIT_RANGE - 1.5
                && hasLineOfSight(target)) {
            setState(STATE_VOMIT);
            vomitTicks = 0;
            getNavigation().stop();
            level().playSound(null, getX(), getY(), getZ(), ModSounds.BLOATER_VOMIT.get(), SoundSource.HOSTILE, 1.5f, 1f);
        }
    }

    private void tickVomit() {
        vomitTicks++;
        getNavigation().stop();
        LivingEntity target = getTarget();
        if (target != null) getLookControl().setLookAt(target, 60f, 60f);
        Vec3 look = getLookAngle();
        Vec3 mouth = getEyePosition().add(look.scale(0.5));

        // Chorro de bilis: partículas a lo largo del cono.
        for (int i = 0; i < 6; i++) {
            double d = 0.5 + random.nextDouble() * VOMIT_RANGE;
            Vec3 p = mouth.add(look.scale(d)).add((random.nextDouble() - 0.5) * d * 0.5,
                    (random.nextDouble() - 0.6) * d * 0.3, (random.nextDouble() - 0.5) * d * 0.5);
            serverLevel().sendParticles(BILE, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }

        if (vomitTicks == 4 || vomitTicks == 10) {
            for (ServerPlayer player : serverLevel().players()) {
                if (!isSurvivor(player)) continue;
                Vec3 to = player.getEyePosition().subtract(mouth);
                double dist = to.length();
                if (dist <= VOMIT_RANGE && to.normalize().dot(look) >= VOMIT_CONE_COS && hasLineOfSight(player)) {
                    BileService.cover(player);
                }
            }
        }
        if (vomitTicks >= 16) {
            setState(STATE_IDLE);
            cooldown = VOMIT_COOLDOWN;
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide() && !burst) burst();
        super.die(source);
    }

    /** Revienta: empuja a todo lo cercano y salpica bilis a corta distancia. */
    private void burst() {
        burst = true;
        Vec3 c = position().add(0, 1, 0);
        serverLevel().sendParticles(BILE, c.x, c.y, c.z, 80, 1.6, 0.8, 1.6, 0.05);
        serverLevel().sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SLIME_BALL)),
                c.x, c.y, c.z, 40, 1.0, 0.6, 1.0, 0.2);
        serverLevel().sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.5),
                e -> e != this && e.isAlive())) {
            Vec3 push = e.position().subtract(position()).multiply(1, 0, 1);
            double dist = push.length();
            if (dist > 4.5) continue;
            push = dist < 1e-3 ? new Vec3(1, 0, 0) : push.normalize();
            double strength = 1.2 * (1 - dist / 5.0) + 0.3;
            e.push(push.x * strength, 0.35, push.z * strength);
            e.hurtMarked = true;
            if (dist <= 3.5 && e instanceof ServerPlayer p) BileService.cover(p);
        }
    }

    @Override
    public float getVoicePitch() {
        return 0.8f + random.nextFloat() * 0.1f;
    }

    // ================================================================== animación y efectos

    @Override
    public boolean shouldTrackHead() {
        return getState() != STATE_VOMIT;
    }

    @Override
    protected RawAnimation mainAnimation(boolean moving) {
        if (getState() == STATE_VOMIT) return loop("vomit");
        return moving ? loop("walk") : loop("idle");
    }

    @Override
    protected void clientEffects() {
        // Babea bilis y lo rodea un enjambre de moscas.
        if (random.nextInt(6) == 0) {
            Vec3 mouth = getEyePosition().add(getLookAngle().scale(0.5));
            level().addParticle(DRIP, mouth.x, mouth.y - 0.35, mouth.z, 0, -0.05, 0);
        }
        if (random.nextInt(3) == 0) {
            double a = (tickCount * 0.3 + random.nextDouble()) % (Math.PI * 2);
            level().addParticle(FLY, getX() + Math.cos(a) * 0.9, getY() + 1.2 + random.nextDouble() * 0.6, getZ() + Math.sin(a) * 0.9, 0, 0, 0);
        }
    }

    private static final DustParticleOptions DRIP = new DustParticleOptions(new Vector3f(0.5f, 0.64f, 0.14f), 1.0f);
    private static final DustParticleOptions FLY = new DustParticleOptions(new Vector3f(0.05f, 0.05f, 0.05f), 0.4f);
}
