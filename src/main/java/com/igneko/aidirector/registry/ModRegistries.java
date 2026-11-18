package com.igneko.aidirector.registry;

import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.core.combat.StaggeredEffect;
import com.igneko.aidirector.core.survivor.MedicalService;
import com.igneko.aidirector.item.FirstAidKitItem;
import com.igneko.aidirector.item.TempHealthItem;
import com.igneko.aidirector.world.supply.AmmoPileBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registros diferidos del mod (efectos, bloques, ítems) y etiquetas propias. */
public final class ModRegistries {

    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TheAIDirector.MOD_ID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TheAIDirector.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TheAIDirector.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TheAIDirector.MOD_ID);

    public static final RegistryObject<MobEffect> STAGGERED = EFFECTS.register("staggered", StaggeredEffect::new);
    /** Cubierto de bilis: atrae a la horda y emborrona la visión. */
    public static final RegistryObject<MobEffect> BILED = EFFECTS.register("biled",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x7FA324) {});

    public static final RegistryObject<Block> AMMO_PILE = BLOCKS.register("ammo_pile", () -> new AmmoPileBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.0f, 6.0f)
                    .sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Item> AMMO_PILE_ITEM = ITEMS.register("ammo_pile",
            () -> new BlockItem(AMMO_PILE.get(), new Item.Properties()) {
                @Override
                public Component getName(ItemStack stack) {
                    net.minecraft.nbt.CompoundTag tag = stack.getTagElement("BlockStateTag");
                    return tag != null && "true".equals(tag.getString("infinite"))
                            ? Component.translatable("block.aidirector.ammo_pile.infinite") : super.getName(stack);
                }
            });

    // Suministros médicos (una unidad por ranura, como en L4D2)
    public static final RegistryObject<Item> FIRST_AID_KIT = ITEMS.register("first_aid_kit",
            () -> new FirstAidKitItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> PAIN_PILLS = ITEMS.register("pain_pills",
            () -> new TempHealthItem(new Item.Properties().stacksTo(1), MedicalService::applyPainPills,
                    () -> SoundEvents.BUNDLE_DROP_CONTENTS, 1.4f, "tooltip.aidirector.pain_pills"));
    public static final RegistryObject<Item> ADRENALINE = ITEMS.register("adrenaline",
            () -> new TempHealthItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), MedicalService::applyAdrenaline,
                    () -> SoundEvents.CROSSBOW_LOADING_END, 1.5f, "tooltip.aidirector.adrenaline"));

    public static final RegistryObject<Item> DEFIBRILLATOR = ITEMS.register("defibrillator",
            () -> new com.igneko.aidirector.item.DefibrillatorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> ALMANAC = ITEMS.register("almanac",
            () -> new com.igneko.aidirector.item.AlmanacBookItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.aidirector"))
            .icon(() -> new ItemStack(FIRST_AID_KIT.get()))
            .displayItems((params, out) -> {
                out.accept(ALMANAC.get());
                out.accept(FIRST_AID_KIT.get());
                out.accept(PAIN_PILLS.get());
                out.accept(ADRENALINE.get());
                out.accept(DEFIBRILLATOR.get());
                out.accept(AMMO_PILE_ITEM.get());
            })
            .build());

    /** Entidades que ignoran el empujón (Charger, Tank, Witch en la Fase 5). */
    public static final TagKey<EntityType<?>> SHOVE_IMMUNE = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation(TheAIDirector.MOD_ID, "shove_immune"));

    /** Montón de munición inagotable (el de los refugios). */
    public static ItemStack infiniteAmmoPile() {
        ItemStack stack = new ItemStack(AMMO_PILE_ITEM.get());
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("infinite", "true");
        stack.getOrCreateTag().put("BlockStateTag", tag);
        return stack;
    }

    private ModRegistries() {}

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
