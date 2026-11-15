package com.igneko.aidirector.core.survivor;

import com.igneko.aidirector.config.AIDirectorConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Puente de eventos Forge → dominio del superviviente. Toda mutación ocurre en el servidor;
 * las comprobaciones de bloqueo (salto, ataque) se ejecutan en ambos lados para evitar saltos
 * visuales, pero el servidor siempre tiene la última palabra.
 */
public final class SurvivorEventHandler {

    private static final int SYNC_INTERVAL_TICKS = 10;

    // ------------------------------------------------------------------ ciclo de vida del dato

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            SurvivorCapability.Provider provider = new SurvivorCapability.Provider();
            event.addCapability(SurvivorCapability.ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        SurvivorCapability.get(original).ifPresent(old -> SurvivorCapability.get(event.getEntity()).ifPresent(now -> {
            if (event.isWasDeath()) now.reset(AIDirectorConfig.rules());
            else now.copyFrom(old); // cambio de dimensión / regreso del End
        }));
        original.invalidateCaps();
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        refresh(event.getEntity());
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        refresh(event.getEntity());
    }

    @SubscribeEvent
    public void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        refresh(event.getEntity());
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ReviveManager.cancelAllInvolving(sp);
            PinService.release(sp);
        }
        com.igneko.aidirector.core.combat.ShoveService.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer receiver && event.getTarget() instanceof Player subject) {
            SurvivorCapability.get(subject).ifPresent(d -> SurvivorService.syncTo(receiver, subject, d));
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        ReviveManager.clear();
        PinService.clear();
    }

    private static void refresh(Player player) {
        if (player instanceof ServerPlayer sp) {
            SurvivorCapability.get(sp).ifPresent(d -> {
                SurvivorService.applyEffects(sp, d);
                SurvivorService.sync(sp, d);
            });
        }
    }

    // ------------------------------------------------------------------ daño, muerte y curación

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SurvivorService.isManaged(player)) return;
        SurvivorHealthData data = SurvivorCapability.get(player).orElse(null);
        if (data == null || event.getAmount() <= 0f) return;

        // Muerte ya decidida por el dominio, o daño que ignora la invulnerabilidad (vacío, /kill).
        if (data.isDead()) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            data.forceDead();
            ReviveManager.cancelAllInvolving(player);
            return;
        }

        SurvivorHealthData.Outcome outcome = data.applyDamage(AIDirectorConfig.rules(),
                SurvivorService.toSurvivorPoints(player, event.getAmount()));

        if (outcome == SurvivorHealthData.Outcome.KILLED) {
            event.setAmount(Float.MAX_VALUE);
            ReviveManager.cancelAllInvolving(player);
            PinService.release(player);
            return;
        }

        // El daño real lo gestiona el dominio: vanilla solo refleja el resultado.
        event.setAmount(0f);
        SurvivorService.onOutcome(player, data, outcome);
    }

    /** Red de seguridad: cualquier muerte no decidida por el dominio se convierte en incapacitación. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SurvivorService.isManaged(player)) return;
        SurvivorHealthData data = SurvivorCapability.get(player).orElse(null);
        if (data == null || data.isDead()) return;

        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) || data.isIncapacitated()) {
            data.forceDead();
            ReviveManager.cancelAllInvolving(player);
            return;
        }

        SurvivorHealthData.Outcome outcome = data.forceIncapacitate(AIDirectorConfig.rules());
        if (outcome == SurvivorHealthData.Outcome.INCAPACITATED) {
            event.setCanceled(true);
            player.setHealth(1f);
            SurvivorService.onOutcome(player, data, outcome);
        }
    }

    @SubscribeEvent
    public void onHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SurvivorService.isManaged(player)
                && AIDirectorConfig.blockVanillaHealing()) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ tick

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        SurvivorHealthData data = SurvivorCapability.get(player).orElse(null);
        if (data == null) return;

        if (!SurvivorService.isManaged(player) || !player.isAlive()) {
            SurvivorService.applyEffects(player, data);
            return;
        }

        SurvivorHealthData.Outcome outcome = data.tick(AIDirectorConfig.rules());
        if (outcome == SurvivorHealthData.Outcome.KILLED) {
            SurvivorService.killForReal(player);
            return;
        }

        SurvivorService.applyEffects(player, data);
        if (data.isIncapacitated()) player.setDeltaMovement(new Vec3(0, Math.min(0, player.getDeltaMovement().y), 0));

        if (data.consumeDirty() || player.tickCount % SYNC_INTERVAL_TICKS == 0) {
            SurvivorService.sync(player, data);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) ReviveManager.tick(event.getServer());
    }

    // ------------------------------------------------------------------ restricciones e interacción

    private static boolean isDowned(Player player) {
        return SurvivorService.isManaged(player)
                && SurvivorCapability.get(player).map(SurvivorHealthData::isIncapacitated).orElse(false);
    }

    @SubscribeEvent
    public void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player && isDowned(player)) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0, 1));
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (isDowned(player)) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getTarget() instanceof Player target) || !isDowned(target)) return;

        // Siempre consumimos la interacción para que el objeto en mano no se use.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (player instanceof ServerPlayer reviver && target instanceof ServerPlayer downed) {
            ReviveManager.onInteract(reviver, downed);
        }
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (isDowned(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isDowned(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isDowned(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (isDowned(event.getEntity())) event.setCanceled(true);
    }
}
