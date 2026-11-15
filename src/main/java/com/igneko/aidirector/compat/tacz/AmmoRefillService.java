package com.igneko.aidirector.compat.tacz;

import com.igneko.aidirector.config.AIDirectorConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Lógica de los montones de munición L4D2 sobre armas de TaCZ (solo servidor). */
public final class AmmoRefillService {

    public enum Result { REFILLED, ALREADY_FULL, NOT_A_GUN, NOT_ALLOWED, NO_SPACE }

    private AmmoRefillService() {}

    public static Result refill(ServerPlayer player) {
        ItemStack gun = player.getMainHandItem();
        if (!TaczGuns.isGun(gun)) return report(player, Result.NOT_A_GUN);

        AIDirectorConfig.TaczSettings cfg = AIDirectorConfig.tacz();
        String type = TaczGuns.gunType(gun);
        String id = TaczGuns.gunId(gun).map(ResourceLocation::toString).orElse("");
        if (cfg.pileExcludedTypes().contains(type) || cfg.pileExcludedGuns().contains(id)) {
            return report(player, Result.NOT_ALLOWED);
        }

        if (TaczGuns.usesDummyAmmo(gun)) {
            return report(player, TaczGuns.refillDummyAmmo(gun) ? Result.REFILLED : Result.ALREADY_FULL);
        }

        Optional<ResourceLocation> ammoId = TaczGuns.ammoId(gun);
        int mag = TaczGuns.magazineSize(gun);
        if (ammoId.isEmpty() || mag <= 0) return report(player, Result.NOT_ALLOWED);

        int target = mag * cfg.reserveMagazines();
        int missing = target - TaczGuns.countAmmo(player, ammoId.get());
        if (missing <= 0) return report(player, Result.ALREADY_FULL);

        int added = 0;
        while (missing > 0) {
            ItemStack probe = TaczGuns.ammoStack(ammoId.get(), 1);
            if (probe.isEmpty()) break;
            int batch = Math.min(missing, probe.getMaxStackSize());
            ItemStack stack = TaczGuns.ammoStack(ammoId.get(), batch);
            player.getInventory().add(stack);
            int inserted = batch - stack.getCount();
            if (inserted <= 0) break;
            added += inserted;
            missing -= inserted;
        }
        if (added == 0) return report(player, Result.NO_SPACE);
        player.containerMenu.broadcastChanges();
        return report(player, Result.REFILLED);
    }

    private static Result report(ServerPlayer player, Result result) {
        String key = switch (result) {
            case REFILLED -> "message.aidirector.ammo.refilled";
            case ALREADY_FULL -> "message.aidirector.ammo.full";
            case NOT_A_GUN -> "message.aidirector.ammo.no_gun";
            case NOT_ALLOWED -> "message.aidirector.ammo.not_allowed";
            case NO_SPACE -> "message.aidirector.ammo.no_space";
        };
        player.displayClientMessage(Component.translatable(key), true);
        if (result == Result.REFILLED) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.0f, 0.8f);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 0.7f);
        }
        return result;
    }
}
