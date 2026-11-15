package com.igneko.aidirector.core.combat;

import com.igneko.aidirector.api.event.ShoveEvent;
import com.igneko.aidirector.compat.tacz.TaczGuns;
import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.survivor.PinService;
import com.igneko.aidirector.core.survivor.ReviveManager;
import com.igneko.aidirector.core.survivor.SurvivorCapability;
import com.igneko.aidirector.core.survivor.SurvivorHealthData;
import com.igneko.aidirector.core.survivor.SurvivorService;
import com.igneko.aidirector.network.ModNetwork;
import com.igneko.aidirector.network.ShoveCooldownPacket;
import com.igneko.aidirector.registry.ModRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ejecución autoritativa del empujón (solo servidor): valida la fatiga, busca objetivos en un cono
 * frontal y aplica retroceso + STAGGERED, con empuje en cadena para los infectados comunes.
 */
public final class ShoveService {

    private static final Map<UUID, ShoveFatigue> FATIGUE = new HashMap<>();

    private ShoveService() {}

    /** Punto de entrada (tecla de empujón o culatazo de TaCZ). Devuelve true si se ejecutó. */
    public static boolean tryShove(ServerPlayer player) {
        if (!SurvivorService.isManaged(player) || !player.isAlive()) return false;
        SurvivorHealthData data = SurvivorCapability.get(player).orElse(null);
        if (data == null) return false;
        if (data.isPinned()) return PinService.tryStruggle(player);
        if (!data.isAlive() || ReviveManager.isReviving(player)) return false;

        AIDirectorConfig.CombatSettings cfg = AIDirectorConfig.combat();
        ShoveFatigue fatigue = FATIGUE.computeIfAbsent(player.getUUID(), id -> new ShoveFatigue());
        if (!fatigue.tryShove(cfg.fatigue(), player.level().getGameTime())) return false;

        TaczGuns.cancelReload(player);
        player.swing(InteractionHand.MAIN_HAND, true);
        player.resetAttackStrengthTicker();
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.PLAYERS, 0.9f, 0.9f + player.getRandom().nextFloat() * 0.2f);

        Set<LivingEntity> hit = new HashSet<>();
        for (LivingEntity target : findTargets(player, cfg)) {
            if (shove(player, target, cfg, cfg.knockback(), hit)) chain(player, target, cfg, hit);
        }

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ShoveCooldownPacket(fatigue.lastCooldown(), fatigue.isFatigued(cfg.fatigue())));
        return true;
    }

    private static List<LivingEntity> findTargets(ServerPlayer player, AIDirectorConfig.CombatSettings cfg) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double range = cfg.shoveRange();
        AABB box = player.getBoundingBox().inflate(range + 1.0);
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            // Punto más cercano de la hitbox al ojo: así los mobs grandes y los agachados cuentan bien.
            Vec3 closest = closestPoint(e.getBoundingBox(), eye);
            Vec3 toTarget = closest.subtract(eye);
            double dist = toTarget.length();
            if (dist > range) continue;
            Vec3 toCenter = e.getBoundingBox().getCenter().subtract(eye).normalize();
            boolean inside = dist < 1e-3;
            if (!inside && Math.max(toTarget.normalize().dot(look), toCenter.dot(look)) < cfg.shoveHalfAngleCos()) continue;
            if (!player.hasLineOfSight(e)) continue;
            result.add(e);
        }
        return result;
    }

    private static Vec3 closestPoint(AABB box, Vec3 p) {
        return new Vec3(Math.max(box.minX, Math.min(p.x, box.maxX)),
                Math.max(box.minY, Math.min(p.y, box.maxY)),
                Math.max(box.minZ, Math.min(p.z, box.maxZ)));
    }

    /** Aplica el empujón a una entidad. Devuelve true si quedó aturdida (para propagar la cadena). */
    private static boolean shove(ServerPlayer shover, LivingEntity target, AIDirectorConfig.CombatSettings cfg,
                                 double strength, Set<LivingEntity> hit) {
        if (!hit.add(target)) return false;
        boolean immune = target.getType().is(ModRegistries.SHOVE_IMMUNE);
        boolean isPlayer = target instanceof Player;
        ShoveEvent event = new ShoveEvent(shover, target, !immune && !isPlayer);
        if (MinecraftForge.EVENT_BUS.post(event) || immune) return false;

        Vec3 push = target.position().subtract(shover.position()).multiply(1, 0, 1);
        if (push.lengthSqr() < 1e-6) push = shover.getLookAngle().multiply(1, 0, 1);
        push = push.normalize();

        if (isPlayer) {
            // Empujar a un compañero lo desplaza ligeramente e interrumpe su reanimación.
            target.push(push.x * 0.4, 0.1, push.z * 0.4);
            target.hurtMarked = true;
            if (target instanceof ServerPlayer sp) ReviveManager.cancel(sp.server, sp.getUUID());
            return false;
        }

        target.knockback(strength, -push.x, -push.z);
        target.hurtMarked = true;
        if (event.shouldStagger()) {
            target.addEffect(new MobEffectInstance(ModRegistries.STAGGERED.get(), cfg.staggerTicks(), 0, false, false, true));
            return true;
        }
        return false;
    }

    /** Empuje en cadena: los infectados justo detrás del empujado también tambalean (un nivel). */
    private static void chain(ServerPlayer shover, LivingEntity first, AIDirectorConfig.CombatSettings cfg,
                              Set<LivingEntity> hit) {
        if (cfg.chainRadius() <= 0) return;
        Vec3 dir = first.position().subtract(shover.position()).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1e-6) return;
        dir = dir.normalize();
        Vec3 behind = first.position().add(dir.scale(cfg.chainRadius() * 0.5));
        AABB box = new AABB(behind, behind).inflate(cfg.chainRadius(), 1.0, cfg.chainRadius());
        for (LivingEntity e : first.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != shover && e.isAlive() && !(e instanceof Player) && !hit.contains(e))) {
            Vec3 rel = e.position().subtract(first.position()).multiply(1, 0, 1);
            if (rel.lengthSqr() > 1e-6 && rel.normalize().dot(dir) < 0.3) continue; // solo los que están detrás
            shove(shover, e, cfg, cfg.knockback() * 0.6, hit);
        }
    }

    public static void forget(UUID player) {
        FATIGUE.remove(player);
    }
}
