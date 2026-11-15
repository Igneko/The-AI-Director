package com.igneko.aidirector.compat.tacz;

import com.igneko.aidirector.api.event.AcousticPulseEvent;
import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.combat.ShoveService;
import com.igneko.aidirector.core.survivor.SurvivorCapability;
import com.igneko.aidirector.core.survivor.SurvivorHealthData;
import com.igneko.aidirector.core.survivor.SurvivorService;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.event.common.GunMeleeEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Traduce eventos de TaCZ a conceptos del mod:
 * <ul>
 *   <li>Disparo → {@link AcousticPulseEvent} con radio según calibre y silenciador.</li>
 *   <li>Disparo estando incapacitado → solo se permiten los tipos configurados (pistolas).</li>
 *   <li>Culatazo de TaCZ → empujón (opcional).</li>
 * </ul>
 */
public final class TaczEventBridge {

    private final Map<UUID, Long> lastPulseTick = new HashMap<>();

    /** Restricción de armas estando incapacitado (ambos lados, para no mostrar el disparo en el cliente). */
    @SubscribeEvent
    public void onShoot(GunShootEvent event) {
        if (!(event.getShooter() instanceof Player player)) return;
        boolean downed = SurvivorService.isManaged(player)
                && SurvivorCapability.get(player).map(SurvivorHealthData::isIncapacitated).orElse(false);
        if (downed && !IncapWeaponHandler.isAllowedWhileDowned(event.getGunItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onFire(GunFireEvent event) {
        if (event.getLogicalSide().isClient() || !(event.getShooter() instanceof ServerPlayer shooter)) return;
        ServerLevel level = shooter.serverLevel();
        AIDirectorConfig.TaczSettings cfg = AIDirectorConfig.tacz();

        long now = level.getGameTime();
        Long last = lastPulseTick.get(shooter.getUUID());
        if (last != null && now - last < cfg.noiseCooldownTicks()) return;
        lastPulseTick.put(shooter.getUUID(), now);

        ItemStack gun = event.getGunItemStack();
        boolean suppressed = TaczGuns.isSuppressed(shooter, gun);
        double radius = suppressed ? cfg.noiseSuppressed() : noiseRadius(cfg, TaczGuns.gunType(gun));
        MinecraftForge.EVENT_BUS.post(new AcousticPulseEvent(level, shooter.position(), radius,
                suppressed ? AcousticPulseEvent.Source.SUPPRESSED_GUNSHOT : AcousticPulseEvent.Source.GUNSHOT, shooter));
    }

    @SubscribeEvent
    public void onMelee(GunMeleeEvent event) {
        if (event.getLogicalSide().isClient() || !AIDirectorConfig.combat().taczMeleeShoves()) return;
        if (event.getShooter() instanceof ServerPlayer player) ShoveService.tryShove(player);
    }

    static double noiseRadius(AIDirectorConfig.TaczSettings cfg, String type) {
        if (cfg.lightTypes().contains(type)) return cfg.noiseLight();
        if (cfg.heavyTypes().contains(type)) return cfg.noiseHeavy();
        return cfg.noiseMedium();
    }

    public void forget(UUID player) {
        lastPulseTick.remove(player);
    }
}
