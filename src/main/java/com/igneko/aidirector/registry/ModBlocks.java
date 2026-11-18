package com.igneko.aidirector.registry;

import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.world.saferoom.CrescendoAlarmBlock;
import com.igneko.aidirector.world.saferoom.CrescendoAlarmBlockEntity;
import com.igneko.aidirector.world.saferoom.SafeRoomDoorBlock;
import com.igneko.aidirector.world.saferoom.SafeRoomRadioBlock;
import com.igneko.aidirector.world.saferoom.SafeRoomRadioBlockEntity;
import com.igneko.aidirector.world.saferoom.SupplyCrateBlock;
import com.igneko.aidirector.world.saferoom.SupplyCrateBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Bloques de los refugios: puerta, radio, cajas de suministros y alarma Crescendo, con su pestaña creativa. */
public final class ModBlocks {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TheAIDirector.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TheAIDirector.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TheAIDirector.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TheAIDirector.MOD_ID);

    public static final RegistryObject<Block> SAFE_ROOM_DOOR = BLOCKS.register("safe_room_door", () -> new SafeRoomDoorBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(10.0f, 1200.0f).requiresCorrectToolForDrops()
                    .noOcclusion().sound(SoundType.METAL).pushReaction(PushReaction.BLOCK)));
    public static final RegistryObject<Item> SAFE_ROOM_DOOR_ITEM = ITEMS.register("safe_room_door",
            () -> new DoubleHighBlockItem(SAFE_ROOM_DOOR.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** La radio es indestructible en supervivencia: si desapareciera, desaparecería el refugio. */
    public static final RegistryObject<Block> SAFE_ROOM_RADIO = BLOCKS.register("safe_room_radio", () -> new SafeRoomRadioBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(-1.0f, 3600000.0f).noLootTable()
                    .noOcclusion().sound(SoundType.METAL).lightLevel(s -> 5).pushReaction(PushReaction.BLOCK)));
    public static final RegistryObject<Item> SAFE_ROOM_RADIO_ITEM = ITEMS.register("safe_room_radio",
            () -> new BlockItem(SAFE_ROOM_RADIO.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Block> SUPPLY_CRATE = BLOCKS.register("supply_crate", () -> new SupplyCrateBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f, 6.0f).noLootTable()
                    .noOcclusion().sound(SoundType.WOOD)));
    public static final RegistryObject<Item> SUPPLY_CRATE_ITEM = ITEMS.register("supply_crate",
            () -> new BlockItem(SUPPLY_CRATE.get(), new Item.Properties().rarity(Rarity.UNCOMMON)) {
                @Override
                public Component getName(ItemStack stack) {
                    return SupplyCrateBlock.nameOf(stack);
                }
            });

    public static final RegistryObject<Block> CRESCENDO_ALARM = BLOCKS.register("crescendo_alarm", () -> new CrescendoAlarmBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(5.0f, 1200.0f).requiresCorrectToolForDrops()
                    .noOcclusion().sound(SoundType.METAL).lightLevel(s -> s.getValue(CrescendoAlarmBlock.ACTIVE) ? 13 : 0)));
    public static final RegistryObject<Item> CRESCENDO_ALARM_ITEM = ITEMS.register("crescendo_alarm",
            () -> new BlockItem(CRESCENDO_ALARM.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<SafeRoomRadioBlockEntity>> SAFE_ROOM_RADIO_BE = BLOCK_ENTITIES.register("safe_room_radio",
            () -> BlockEntityType.Builder.of(SafeRoomRadioBlockEntity::new, SAFE_ROOM_RADIO.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<SupplyCrateBlockEntity>> SUPPLY_CRATE_BE = BLOCK_ENTITIES.register("supply_crate",
            () -> BlockEntityType.Builder.of(SupplyCrateBlockEntity::new, SUPPLY_CRATE.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<CrescendoAlarmBlockEntity>> CRESCENDO_ALARM_BE = BLOCK_ENTITIES.register("crescendo_alarm",
            () -> BlockEntityType.Builder.of(CrescendoAlarmBlockEntity::new, CRESCENDO_ALARM.get()).build(null));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("safe_rooms", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.aidirector.safe_rooms"))
            .icon(() -> new ItemStack(SAFE_ROOM_DOOR_ITEM.get()))
            .displayItems((params, out) -> {
                out.accept(SAFE_ROOM_DOOR_ITEM.get());
                out.accept(SAFE_ROOM_RADIO_ITEM.get());
                for (SupplyCrateBlock.Kind kind : SupplyCrateBlock.Kind.values()) {
                    out.accept(SupplyCrateBlock.withKind(new ItemStack(SUPPLY_CRATE_ITEM.get()), kind, false));
                }
                out.accept(SupplyCrateBlock.withKind(new ItemStack(SUPPLY_CRATE_ITEM.get()), SupplyCrateBlock.Kind.ARMORY, true));
                out.accept(CRESCENDO_ALARM_ITEM.get());
                out.accept(ModRegistries.AMMO_PILE_ITEM.get());
                out.accept(ModRegistries.infiniteAmmoPile());
            })
            .build());

    private ModBlocks() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        TABS.register(modBus);
    }
}
