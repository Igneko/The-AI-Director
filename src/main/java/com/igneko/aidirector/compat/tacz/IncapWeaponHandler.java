package com.igneko.aidirector.compat.tacz;

import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.core.survivor.SurvivorCapability;
import com.igneko.aidirector.core.survivor.SurvivorHealthData;
import com.igneko.aidirector.core.survivor.SurvivorService;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Restricción de armas estando incapacitado: se bloquea el cambio de ranura y se fuerza a empuñar
 * una pistola de TaCZ. Si el jugador no lleva ninguna, se le entrega una pistola de respaldo temporal
 * con munición infinita que desaparece al ser levantado.
 */
public final class IncapWeaponHandler {

    private static final String BACKUP_TAG = TheAIDirector.MOD_ID + ":backup_pistol";

    /** Ranura bloqueada por jugador caído. */
    private final Map<UUID, Integer> lockedSlots = new HashMap<>();

    public static boolean isAllowedWhileDowned(ItemStack stack) {
        return TaczGuns.isGun(stack) && AIDirectorConfig.tacz().incapTypes().contains(TaczGuns.gunType(stack));
    }

    private static boolean isBackup(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTag() && stack.getTag().getBoolean(BACKUP_TAG);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        boolean downed = SurvivorService.isManaged(player) && player.isAlive()
                && SurvivorCapability.get(player).map(SurvivorHealthData::isIncapacitated).orElse(false);

        Integer locked = lockedSlots.get(player.getUUID());
        if (downed) {
            if (locked == null) {
                locked = chooseSlot(player);
                lockedSlots.put(player.getUUID(), locked);
            }
            if (locked >= 0 && player.getInventory().selected != locked) {
                player.getInventory().selected = locked;
                player.connection.send(new ClientboundSetCarriedItemPacket(locked));
            }
            ItemStack held = player.getMainHandItem();
            if (isBackup(held)) TaczGuns.topUpMagazine(held);
        } else if (locked != null) {
            lockedSlots.remove(player.getUUID());
            removeBackupPistols(player.getInventory());
        }
    }

    /** Elige la pistola del hotbar o entrega la de respaldo. Devuelve -1 si no hay forma de armar al jugador. */
    private int chooseSlot(ServerPlayer player) {
        Inventory inv = player.getInventory();
        if (isAllowedWhileDowned(inv.getItem(inv.selected))) return inv.selected;
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            if (isAllowedWhileDowned(inv.getItem(i))) return i;
        }

        String id = AIDirectorConfig.tacz().backupPistol();
        ResourceLocation gunId = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        if (gunId == null || !TaczGuns.gunExists(gunId)) return inv.selected;

        int slot = freeHotbarSlot(inv);
        if (slot < 0) {
            // Liberamos la última ranura del hotbar moviendo su contenido a la mochila.
            int free = freeMainSlot(inv);
            if (free < 0) {
                player.displayClientMessage(Component.translatable("message.aidirector.backup_pistol.no_space"), true);
                return inv.selected;
            }
            slot = Inventory.getSelectionSize() - 1;
            inv.setItem(free, inv.getItem(slot));
            inv.setItem(slot, ItemStack.EMPTY);
        }
        ItemStack backup = TaczGuns.buildGun(gunId);
        backup.getOrCreateTag().putBoolean(BACKUP_TAG, true);
        inv.setItem(slot, backup);
        return slot;
    }

    private static int freeHotbarSlot(Inventory inv) {
        for (int i = 0; i < Inventory.getSelectionSize(); i++) if (inv.getItem(i).isEmpty()) return i;
        return -1;
    }

    private static int freeMainSlot(Inventory inv) {
        for (int i = Inventory.getSelectionSize(); i < inv.items.size(); i++) if (inv.items.get(i).isEmpty()) return i;
        return -1;
    }

    private static void removeBackupPistols(Inventory inv) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isBackup(inv.getItem(i))) inv.setItem(i, ItemStack.EMPTY);
        }
    }

    @SubscribeEvent
    public void onToss(ItemTossEvent event) {
        if (isBackup(event.getEntity().getItem())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(item -> isBackup(item.getItem()));
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        lockedSlots.remove(event.getEntity().getUUID());
        removeBackupPistols(event.getEntity().getInventory()); // por si keepInventory está activo
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        lockedSlots.remove(event.getEntity().getUUID());
    }
}
