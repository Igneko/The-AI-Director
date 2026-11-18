package com.igneko.aidirector.core.director;

import com.igneko.aidirector.compat.tacz.TaczGuns;
import com.igneko.aidirector.config.AIDirectorConfig;
import com.igneko.aidirector.core.survivor.SurvivorCapability;
import com.igneko.aidirector.core.survivor.SurvivorHealthData;
import com.igneko.aidirector.core.survivor.SurvivorRules;
import com.igneko.aidirector.core.survivor.SurvivorService;
import com.igneko.aidirector.registry.ModRegistries;
import com.igneko.aidirector.registry.ModWeapons;
import com.igneko.aidirector.world.saferoom.SupplyCrateBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Generación dinámica de suministros (especificación §4.3): el contenido de una caja depende del
 * estado del equipo en el momento de abrirla.
 * <ul>
 *   <li>Salud media del equipo H &lt; 50 %: botiquines (o un desfibrilador si hay algún caído).</li>
 *   <li>H ≥ 50 %: píldoras, adrenalina y arrojables.</li>
 * </ul>
 */
public final class SupplyDirector {

    private SupplyDirector() {}

    /** Salud media del equipo del nivel, 0..1 (los incapacitados cuentan como 0). */
    public static float teamHealth(ServerLevel level) {
        SurvivorRules rules = AIDirectorConfig.rules();
        float sum = 0;
        int n = 0;
        for (ServerPlayer p : level.players()) {
            if (!SurvivorService.isManaged(p)) continue;
            SurvivorHealthData d = SurvivorCapability.get(p).orElse(null);
            if (d == null) continue;
            sum += d.isAlive() ? d.totalHealth() / rules.maxHealth() : 0f;
            n++;
        }
        return n == 0 ? 1f : sum / n;
    }

    private static boolean anyoneDead(ServerLevel level) {
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (p.level() == level && p.isDeadOrDying()) return true;
        }
        return false;
    }

    public static List<ItemStack> roll(SupplyCrateBlock.Kind kind, ServerLevel level, ServerPlayer opener, boolean reward) {
        RandomSource r = level.random;
        List<ItemStack> out = new ArrayList<>();
        switch (kind) {
            case MEDICAL -> {
                float health = teamHealth(level);
                if (health < 0.5f) {
                    out.add(stack(anyoneDead(level) && r.nextFloat() < 0.5f ? ModRegistries.DEFIBRILLATOR : ModRegistries.FIRST_AID_KIT));
                    if (r.nextFloat() < 0.5f) out.add(stack(ModRegistries.PAIN_PILLS));
                } else {
                    out.add(stack(r.nextFloat() < 0.65f ? ModRegistries.PAIN_PILLS : ModRegistries.ADRENALINE));
                    if (r.nextFloat() < 0.35f) out.add(stack(ModRegistries.FIRST_AID_KIT));
                    if (r.nextFloat() < 0.3f) out.add(throwable(r));
                }
                if (reward) out.add(stack(ModRegistries.FIRST_AID_KIT));
            }
            case THROWABLES -> {
                out.add(throwable(r));
                if (reward || r.nextFloat() < 0.25f) out.add(throwable(r));
            }
            case ARMORY -> {
                List<String> pool = reward ? AIDirectorConfig.safeRoom().tier2Guns() : AIDirectorConfig.safeRoom().tier1Guns();
                ItemStack gun = randomGun(pool, r);
                if (!gun.isEmpty()) {
                    out.add(gun);
                    TaczGuns.ammoId(gun).ifPresent(ammo -> {
                        int mag = Math.max(8, TaczGuns.magazineSize(gun));
                        int total = mag * (reward ? 4 : 3);
                        while (total > 0) {
                            int n = Math.min(64, total);
                            out.add(TaczGuns.ammoStack(ammo, n));
                            total -= n;
                        }
                    });
                }
                if (gun.isEmpty() || r.nextFloat() < 0.4f) {
                    List<RegistryObject<Item>> melee = ModWeapons.melee();
                    out.add(new ItemStack(melee.get(r.nextInt(melee.size())).get()));
                }
                if (reward) out.add(throwable(r));
            }
        }
        return out;
    }

    private static ItemStack randomGun(List<String> pool, RandomSource r) {
        List<ResourceLocation> valid = new ArrayList<>();
        for (String id : pool) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null && TaczGuns.gunExists(rl)) valid.add(rl);
        }
        return valid.isEmpty() ? ItemStack.EMPTY : TaczGuns.buildGun(valid.get(r.nextInt(valid.size())));
    }

    private static ItemStack throwable(RandomSource r) {
        float roll = r.nextFloat();
        return stack(roll < 0.4f ? ModWeapons.PIPE_BOMB : roll < 0.75f ? ModWeapons.MOLOTOV : ModWeapons.BILE_JAR);
    }

    private static ItemStack stack(RegistryObject<Item> item) {
        return new ItemStack(item.get());
    }
}
