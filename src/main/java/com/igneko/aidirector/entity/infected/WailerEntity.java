package com.igneko.aidirector.entity.infected;

import com.igneko.aidirector.api.event.AcousticPulseEvent;
import com.igneko.aidirector.compat.tacz.TaczGuns;
import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.survivor.SurvivorCapability;
import com.igneko.aidirector.core.survivor.SurvivorHealthData;
import com.igneko.aidirector.core.survivor.SurvivorService;
import com.igneko.aidirector.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Plañidera: amenaza pasiva. Permanece sentada llorando; acercarse a menos de 10 bloques, mirarla
 * fijamente o disparar cerca llena su medidor de molestia W. Se levanta gruñendo como aviso y, si W
 * llega a 1 o recibe daño, se enfurece: corre al doble de velocidad hacia quien la asustó, lo
 * incapacita de un zarpazo y lo remata en el suelo. Un disparo de escopeta a quemarropa antes de que
 * reaccione la abate de golpe ("coronarla").
 */
public class WailerEntity extends SpecialInfectedEntity {

    public static final int STATE_CALM = 0, STATE_DISTURBED = 1, STATE_RAGE = 2, STATE_FLEE = 3;

    private static final double NOTICE_RADIUS = 10;
    private static final double RAGE_SPEED = 1.65;

    private float harassment;
    private final Map<UUID, Float> blame = new HashMap<>();
    @Nullable private UUID rageTarget;
    private int stateTicks;
    private int cryTimer;
    private int attackCooldown;

    public WailerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, new Sounds(ModSounds.WAILER_CRY, ModSounds.WAILER_HURT, ModSounds.WAILER_DEATH, null));
        this.xpReward = 200;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return baseAttributes(200, 0.25, l4d(100));
    }

    @Override
    protected void registerGoals() {
        // Sin objetivos automáticos: solo ataca a quien la enfurece.
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void registerBehaviourGoals() {}

    public float getHarassment() {
        return harassment;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        stateTicks++;
        if (attackCooldown > 0) attackCooldown--;
        switch (getState()) {
            case STATE_RAGE -> tickRage();
            case STATE_FLEE -> tickFlee();
            default -> tickCalm();
        }
    }

    private void tickCalm() {
        getNavigation().stop();
        boolean anyone = false;
        for (ServerPlayer p : serverLevel().players()) {
            if (!isSurvivor(p)) continue;
            double d = distanceTo(p);
            if (d > NOTICE_RADIUS) continue;
            anyone = true;
            float gain = (float) (0.012 * (1 - d / NOTICE_RADIUS));
            // Mirarla fijamente (o apuntarla con la linterna) también la altera.
            Vec3 toMe = getEyePosition().subtract(p.getEyePosition()).normalize();
            if (p.getLookAngle().dot(toMe) > 0.96 && p.hasLineOfSight(this)) gain += 0.004f;
            addHarassment(p, gain);
        }
        if (!anyone) harassment = Math.max(0f, harassment - 0.004f);

        if (harassment >= 1f) {
            enrage(topBlamed());
            return;
        }
        if (getState() == STATE_CALM && harassment >= 0.35f) {
            setState(STATE_DISTURBED);
            playSound(ModSounds.WAILER_GROWL.get(), 1.4f, 1f);
        } else if (getState() == STATE_DISTURBED && harassment < 0.25f) {
            setState(STATE_CALM);
        }
        if (getState() == STATE_DISTURBED && stateTicks % 30 == 0) playSound(ModSounds.WAILER_GROWL.get(), 1.0f + harassment, 1f);
        if (--cryTimer <= 0) {
            cryTimer = 50 + random.nextInt(40);
            playSound(ModSounds.WAILER_CRY.get(), 1.2f, 0.9f + random.nextFloat() * 0.2f);
        }
    }

    private void addHarassment(Player p, float amount) {
        harassment = Math.min(1f, harassment + amount);
        blame.merge(p.getUUID(), amount, Float::sum);
    }

    @Nullable
    private ServerPlayer topBlamed() {
        UUID best = null;
        float max = -1;
        for (Map.Entry<UUID, Float> e : blame.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                best = e.getKey();
            }
        }
        return best == null ? null : serverLevel().getServer().getPlayerList().getPlayer(best);
    }

    private void enrage(@Nullable LivingEntity culprit) {
        if (getState() == STATE_RAGE) return;
        if (culprit == null || !isSurvivor(culprit)) culprit = topBlamed();
        if (culprit == null) {
            harassment = 0.6f;
            return;
        }
        rageTarget = culprit.getUUID();
        setTarget(culprit);
        harassment = 1f;
        setState(STATE_RAGE);
        stateTicks = 0;
        level().playSound(null, getX(), getY(), getZ(), ModSounds.WAILER_RAGE.get(), SoundSource.HOSTILE, 3.0f, 1f);
    }

    private void tickRage() {
        ServerPlayer target = rageTarget == null ? null : serverLevel().getServer().getPlayerList().getPlayer(rageTarget);
        boolean targetGone = target == null || !target.isAlive() || target.level() != level()
                || SurvivorCapability.get(target).map(SurvivorHealthData::isDead).orElse(true);
        if (targetGone || stateTicks > 1200) {
            setState(STATE_FLEE);
            stateTicks = 0;
            return;
        }
        setTarget(target);
        if (stateTicks % 5 == 0) getNavigation().moveTo(target, RAGE_SPEED);
        getLookControl().setLookAt(target, 60f, 60f);
        if (attackCooldown == 0 && distanceToSqr(target) < 2.6 * 2.6) {
            attackCooldown = 15;
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            slash(target);
        }
    }

    /** Zarpazo: incapacita al instante a un superviviente en pie; en el suelo, lo desgarra. */
    private void slash(ServerPlayer target) {
        SurvivorHealthData data = SurvivorCapability.get(target).orElse(null);
        level().playSound(null, target.getX(), target.getY(), target.getZ(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.HOSTILE, 1.2f, 0.6f);
        if (data != null && data.isAlive() && SurvivorService.isManaged(target)) {
            SurvivorHealthData.Outcome outcome = data.forceIncapacitate(AIDirectorConfig.rules());
            if (outcome == SurvivorHealthData.Outcome.KILLED) SurvivorService.killForReal(target);
            else SurvivorService.onOutcome(target, data, outcome);
        } else {
            hurtPinned(target, 25f);
        }
    }

    private void tickFlee() {
        if (stateTicks % 20 == 1) {
            Player nearest = level().getNearestPlayer(this, 32);
            Vec3 away = nearest == null ? null : DefaultRandomPos.getPosAway(this, 16, 6, nearest.position());
            if (away != null) getNavigation().moveTo(away.x, away.y, away.z, RAGE_SPEED);
        }
        if (stateTicks > 200) discard();
    }

    /** Ruido de disparos cerca: la sobresalta mucho. */
    public void onNoise(@Nullable Player shooter, double distance) {
        if (getState() >= STATE_RAGE || shooter == null || !isSurvivor(shooter)) return;
        addHarassment(shooter, (float) (0.3 * Math.max(0.2, 1 - distance / 20)));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide() && source.getEntity() instanceof Player attacker && getState() < STATE_RAGE
                && distanceTo(attacker) <= 3.0 && "shotgun".equals(TaczGuns.gunType(attacker.getMainHandItem()))) {
            amount *= 12f; // "coronarla": escopeta a quemarropa antes de que reaccione
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide() && isAlive() && getState() < STATE_RAGE) {
            enrage(source.getEntity() instanceof LivingEntity le ? le : null);
        }
        return hurt;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null; // el llanto lo gestiona su propio temporizador
    }

    @Override
    public boolean isPushable() {
        return getState() >= STATE_RAGE;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Harassment", harassment);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        harassment = tag.getFloat("Harassment");
    }

    /** Oyente de pulsos acústicos (disparos) para todas las Plañideras cercanas. */
    public static final class NoiseListener {
        @SubscribeEvent
        public void onPulse(AcousticPulseEvent event) {
            double r = Math.min(20, event.getRadius());
            Vec3 o = event.getOrigin();
            for (WailerEntity w : event.getLevel().getEntitiesOfClass(WailerEntity.class,
                    new net.minecraft.world.phys.AABB(o, o).inflate(r))) {
                double d = w.position().distanceTo(o);
                if (d <= r) w.onNoise(event.getEmitter() instanceof Player p ? p : null, d);
            }
        }
    }
}
