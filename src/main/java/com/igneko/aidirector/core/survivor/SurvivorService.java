package com.igneko.aidirector.core.survivor;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.network.ModNetwork;
import com.igneko.aidirector.network.SyncSurvivorPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Servicio de aplicación del superviviente (solo servidor): traduce las decisiones del dominio
 * a efectos sobre el {@link ServerPlayer} (atributos, pose, salud vanilla espejo, red).
 */
public final class SurvivorService {

    public static final ResourceKey<DamageType> BLEED_OUT = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(TheAIDirector.MOD_ID, "bleed_out"));

    private static final UUID SPEED_MODIFIER_ID = UUID.fromString("6f1d3b2a-4c5e-4d8a-9b7f-2e1a0c9d8b71");
    private static final String SPEED_MODIFIER_NAME = TheAIDirector.MOD_ID + ":limp";

    private SurvivorService() {}

    /** Creativo y espectador quedan fuera del sistema de supervivencia. */
    public static boolean isManaged(Player player) {
        return !player.isCreative() && !player.isSpectator();
    }

    /** Convierte daño vanilla (tras armadura) a puntos L4D2 respetando el atributo de vida máxima. */
    public static float toSurvivorPoints(Player player, float vanillaDamage) {
        return vanillaDamage * AIDirectorConfig.rules().maxHealth() / Math.max(1f, player.getMaxHealth());
    }

    /** Aplica los efectos del estado actual sin bajar la vida vanilla (ver {@link #applyEffects(ServerPlayer, SurvivorHealthData, boolean)}). */
    public static void applyEffects(ServerPlayer player, SurvivorHealthData data) {
        applyEffects(player, data, false);
    }

    /**
     * Aplica a la entidad todos los efectos derivados del estado actual. Idempotente.
     *
     * @param allowHealthDrop si es false, la vida vanilla espejo solo puede subir. Cada bajada de vida
     *                        sincronizada hace que el cliente reproduzca la animación de daño
     *                        ({@code LocalPlayer.hurtTo}), así que el decaimiento temporal y el sangrado
     *                        no deben bajarla: solo los golpes reales.
     */
    public static void applyEffects(ServerPlayer player, SurvivorHealthData data, boolean allowHealthDrop) {
        SurvivorRules rules = AIDirectorConfig.rules();
        boolean managed = isManaged(player);

        // Pose derribada
        Pose wanted = managed && (data.isIncapacitated() || data.pinType().lying()) ? Pose.SWIMMING : null;
        if (player.getForcedPose() != wanted) player.setForcedPose(wanted);
        if (wanted != null && player.getPose() != wanted) player.setPose(wanted);

        // Limp Mode / inmovilización
        float multiplier = managed ? data.speedMultiplier(rules) : 1f;
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            AttributeModifier current = speed.getModifier(SPEED_MODIFIER_ID);
            double amount = multiplier - 1.0;
            if (current == null || Math.abs(current.getAmount() - amount) > 1e-4) {
                if (current != null) speed.removeModifier(SPEED_MODIFIER_ID);
                if (Math.abs(amount) > 1e-4) {
                    speed.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_ID, SPEED_MODIFIER_NAME,
                            amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
        }

        // Vida vanilla como espejo aproximado de la salud L4D2 (la barra vanilla está oculta en el HUD).
        if (managed && !data.isDead()) {
            float mirror = data.vanillaHealth(rules, player.getMaxHealth());
            float current = player.getHealth();
            if (mirror > current + 0.01f || (allowHealthDrop && mirror < current - 0.01f)) player.setHealth(mirror);
        }
    }

    public static void sync(ServerPlayer player, SurvivorHealthData data) {
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                SyncSurvivorPacket.of(player.getId(), data));
    }

    public static void syncTo(ServerPlayer receiver, Player subject, SurvivorHealthData data) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver),
                SyncSurvivorPacket.of(subject.getId(), data));
    }

    /** Reacciones de alto nivel a los resultados del dominio. */
    public static void onOutcome(ServerPlayer player, SurvivorHealthData data, SurvivorHealthData.Outcome outcome) {
        switch (outcome) {
            case INCAPACITATED -> {
                ReviveManager.cancelAllInvolving(player);
                player.stopUsingItem();
                player.displayClientMessage(Component.translatable("message.aidirector.incapacitated"), true);
                com.igneko.aidirector.core.director.IntensityTracker.onAllyIncapacitated(player);
            }
            case KILLED -> {
                ReviveManager.cancelAllInvolving(player);
                PinService.release(player);
                // TODO Fase 3: notificar al AI Director (+0.75 de intensidad a aliados).
            }
            default -> {}
        }
        applyEffects(player, data, outcome == SurvivorHealthData.Outcome.DAMAGED);
        sync(player, data);
    }

    /** Mata al jugador de verdad (sangrado agotado o caída tras Blanco y Negro). */
    public static void killForReal(ServerPlayer player) {
        DamageSource source = new DamageSource(player.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BLEED_OUT));
        player.setForcedPose(null);
        player.hurt(source, Float.MAX_VALUE);
    }
}
