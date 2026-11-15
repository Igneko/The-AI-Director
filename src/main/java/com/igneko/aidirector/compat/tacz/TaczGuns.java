package com.igneko.aidirector.compat.tacz;

import com.igneko.aidirector.core.TheAIDirector;
import com.mojang.logging.LogUtils;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.Optional;

/**
 * Fachada única sobre la API de TaCZ. El resto del mod no importa clases de TaCZ directamente
 * (salvo los listeners de eventos de TaCZ), lo que aísla los cambios de versión en este paquete.
 */
public final class TaczGuns {

    private static final Logger LOG = LogUtils.getLogger();

    private TaczGuns() {}

    public static boolean isGun(ItemStack stack) {
        return IGun.getIGunOrNull(stack) != null;
    }

    public static Optional<ResourceLocation> gunId(ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        return gun == null ? Optional.empty() : Optional.ofNullable(gun.getGunId(stack));
    }

    private static Optional<CommonGunIndex> index(ItemStack stack) {
        return gunId(stack).flatMap(TimelessAPI::getCommonGunIndex);
    }

    /** Tipo de arma según el gunpack ("pistol", "rifle", "shotgun", "sniper", "smg", "mg", "rpg"...). */
    public static String gunType(ItemStack stack) {
        return index(stack).map(CommonGunIndex::getType).map(t -> t.toLowerCase(Locale.ROOT)).orElse("");
    }

    public static Optional<ResourceLocation> ammoId(ItemStack stack) {
        return index(stack).map(i -> i.getGunData().getAmmoId());
    }

    public static int magazineSize(ItemStack stack) {
        return index(stack).map(i -> i.getGunData().getAmmoAmount()).orElse(0);
    }

    /**
     * ¿El arma que empuña el tirador está silenciada? Usa la caché de propiedades de TaCZ (que ya tiene
     * en cuenta accesorios y armas con silenciador integrado); si no está disponible, mira el accesorio de bocacha.
     */
    public static boolean isSuppressed(LivingEntity shooter, ItemStack stack) {
        try {
            AttachmentCacheProperty cache = IGunOperator.fromLivingEntity(shooter).getCacheProperty();
            if (cache != null) {
                Pair<Integer, Boolean> silence = cache.getCache(SilenceModifier.ID);
                if (silence != null) return Boolean.TRUE.equals(silence.right());
            }
        } catch (RuntimeException e) {
            LOG.debug("[{}] No se pudo leer la caché de silenciador de TaCZ", TheAIDirector.MOD_ID, e);
        }
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) return false;
        ResourceLocation muzzle = gun.getAttachmentId(stack, AttachmentType.MUZZLE);
        if (muzzle == null || muzzle.getPath().isEmpty() || "empty".equals(muzzle.getPath())) {
            muzzle = gun.getBuiltInAttachmentId(stack, AttachmentType.MUZZLE);
        }
        if (muzzle == null) return false;
        String path = muzzle.getPath();
        return path.contains("silencer") || path.contains("suppressor");
    }

    /** Cancela la recarga en curso (el empujón interrumpe la recarga, como en L4D2). */
    public static void cancelReload(LivingEntity entity) {
        try {
            IGunOperator op = IGunOperator.fromLivingEntity(entity);
            if (op.getSynReloadState().getStateType().isReloading()) op.cancelReload();
        } catch (RuntimeException e) {
            LOG.debug("[{}] No se pudo cancelar la recarga de TaCZ", TheAIDirector.MOD_ID, e);
        }
    }

    // ------------------------------------------------------------------ munición

    /** Cuenta las balas sueltas del calibre indicado en el inventario. */
    public static int countAmmo(Player player, ResourceLocation ammoId) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            IAmmo ammo = IAmmo.getIAmmoOrNull(stack);
            if (ammo != null && ammoId.equals(ammo.getAmmoId(stack))) total += stack.getCount();
        }
        return total;
    }

    /** Crea una pila de munición TaCZ del calibre indicado. */
    public static ItemStack ammoStack(ResourceLocation ammoId, int count) {
        return AmmoItemBuilder.create().setId(ammoId).setCount(count).build();
    }

    public static boolean usesDummyAmmo(ItemStack gunStack) {
        IGun gun = IGun.getIGunOrNull(gunStack);
        return gun != null && gun.useDummyAmmo(gunStack);
    }

    /** Rellena la munición virtual (armas configuradas con "dummy ammo"). Devuelve true si cambió algo. */
    public static boolean refillDummyAmmo(ItemStack gunStack) {
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null || !gun.useDummyAmmo(gunStack) || !gun.hasMaxDummyAmmo(gunStack)) return false;
        int max = gun.getMaxDummyAmmoAmount(gunStack);
        if (gun.getDummyAmmoAmount(gunStack) >= max) return false;
        gun.setDummyAmmoAmount(gunStack, max);
        return true;
    }

    /** Mantiene el cargador lleno (pistola de respaldo con munición infinita). */
    public static void topUpMagazine(ItemStack gunStack) {
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null) return;
        int mag = magazineSize(gunStack);
        if (mag > 0 && gun.getCurrentAmmoCount(gunStack) < mag) gun.setCurrentAmmoCount(gunStack, mag);
    }

    public static ItemStack buildGun(ResourceLocation gunId) {
        int mag = TimelessAPI.getCommonGunIndex(gunId).map(i -> i.getGunData().getAmmoAmount()).orElse(0);
        return GunItemBuilder.create().setId(gunId).setAmmoCount(mag).setAmmoInBarrel(true).build();
    }

    public static boolean gunExists(ResourceLocation gunId) {
        return TimelessAPI.getCommonGunIndex(gunId).isPresent();
    }
}
