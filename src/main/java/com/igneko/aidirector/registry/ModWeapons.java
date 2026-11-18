package com.igneko.aidirector.registry;

import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.item.BileJarItem;
import com.igneko.aidirector.item.MeleeWeaponItem;
import com.igneko.aidirector.item.MolotovItem;
import com.igneko.aidirector.item.MeleeWeaponItem.Stats;
import com.igneko.aidirector.item.PipeBombItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** Armas cuerpo a cuerpo y arrojables del superviviente, con su pestaña creativa. */
public final class ModWeapons {

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TheAIDirector.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TheAIDirector.MOD_ID);

    private static final List<RegistryObject<Item>> ALL = new ArrayList<>();
    private static final List<RegistryObject<Item>> MELEE = new ArrayList<>();

    //                                                         daño  vel.   alcance dur.  barre
    public static final RegistryObject<Item> BASEBALL_BAT    = melee("baseball_bat",    new Stats(8f,  -2.8f, 0.5f,  400, false));
    public static final RegistryObject<Item> CRICKET_BAT     = melee("cricket_bat",     new Stats(8f,  -2.8f, 0.5f,  380, false));
    public static final RegistryObject<Item> CROWBAR         = melee("crowbar",         new Stats(7f,  -2.4f, 0.3f,  650, false));
    public static final RegistryObject<Item> ELECTRIC_GUITAR = melee("electric_guitar", new Stats(9f,  -3.0f, 0.6f,  220, true));
    public static final RegistryObject<Item> FIRE_AXE        = melee("fire_axe",        new Stats(11f, -3.1f, 0.5f,  500, true));
    public static final RegistryObject<Item> FRYING_PAN      = melee("frying_pan",      new Stats(7f,  -2.6f, 0.2f, 1500, false));
    public static final RegistryObject<Item> GOLF_CLUB       = melee("golf_club",       new Stats(7f,  -2.5f, 0.9f,  350, false));
    public static final RegistryObject<Item> KATANA          = melee("katana",          new Stats(10f, -2.2f, 0.6f,  480, true));
    public static final RegistryObject<Item> MACHETE         = melee("machete",         new Stats(9f,  -2.3f, 0.4f,  520, true));
    public static final RegistryObject<Item> NIGHTSTICK      = melee("nightstick",      new Stats(6f,  -2.0f, 0.3f,  800, false));
    public static final RegistryObject<Item> COMBAT_KNIFE    = melee("combat_knife",    new Stats(6f,  -1.6f, 0.0f,  900, false));
    public static final RegistryObject<Item> PITCHFORK       = melee("pitchfork",       new Stats(8f,  -2.8f, 1.3f,  360, false));
    public static final RegistryObject<Item> SHOVEL          = melee("shovel",          new Stats(8f,  -2.8f, 0.8f,  450, false));

    public static final RegistryObject<Item> PIPE_BOMB = add(ITEMS.register("pipe_bomb", () -> new PipeBombItem(new Item.Properties().stacksTo(1))));
    public static final RegistryObject<Item> BILE_JAR = add(ITEMS.register("bile_jar", () -> new BileJarItem(new Item.Properties().stacksTo(1))));
    public static final RegistryObject<Item> MOLOTOV = add(ITEMS.register("molotov", () -> new MolotovItem(new Item.Properties().stacksTo(1))));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("weapons", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.aidirector.weapons"))
            .icon(() -> new ItemStack(FIRE_AXE.get()))
            .displayItems((params, out) -> ALL.forEach(item -> out.accept(item.get())))
            .build());

    private ModWeapons() {}

    private static RegistryObject<Item> melee(String name, Stats stats) {
        RegistryObject<Item> item = add(ITEMS.register(name, () -> new MeleeWeaponItem(stats)));
        MELEE.add(item);
        return item;
    }

    private static RegistryObject<Item> add(RegistryObject<Item> item) {
        ALL.add(item);
        return item;
    }

    public static List<RegistryObject<Item>> melee() {
        return MELEE;
    }

    public static List<RegistryObject<Item>> all() {
        return ALL;
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
