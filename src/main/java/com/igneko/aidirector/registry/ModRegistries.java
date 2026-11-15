package com.igneko.aidirector.registry;

import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.core.combat.StaggeredEffect;
import com.igneko.aidirector.world.supply.AmmoPileBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registros diferidos del mod (efectos, bloques, ítems) y etiquetas propias. */
public final class ModRegistries {

    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TheAIDirector.MOD_ID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TheAIDirector.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TheAIDirector.MOD_ID);

    public static final RegistryObject<MobEffect> STAGGERED = EFFECTS.register("staggered", StaggeredEffect::new);

    public static final RegistryObject<Block> AMMO_PILE = BLOCKS.register("ammo_pile", () -> new AmmoPileBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.0f, 6.0f)
                    .sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Item> AMMO_PILE_ITEM = ITEMS.register("ammo_pile",
            () -> new BlockItem(AMMO_PILE.get(), new Item.Properties()));

    /** Entidades que ignoran el empujón (Charger, Tank, Witch en la Fase 5). */
    public static final TagKey<EntityType<?>> SHOVE_IMMUNE = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation(TheAIDirector.MOD_ID, "shove_immune"));

    private ModRegistries() {}

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(ModRegistries::onBuildCreativeTabs);
    }

    private static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(AMMO_PILE_ITEM);
    }
}
