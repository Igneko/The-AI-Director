package com.igneko.aidirector.registry;

import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.entity.infected.BehemothEntity;
import com.igneko.aidirector.entity.infected.BloaterEntity;
import com.igneko.aidirector.entity.infected.CorroderEntity;
import com.igneko.aidirector.entity.infected.LeechEntity;
import com.igneko.aidirector.entity.infected.LurkerEntity;
import com.igneko.aidirector.entity.infected.RammerEntity;
import com.igneko.aidirector.entity.infected.StranglerEntity;
import com.igneko.aidirector.entity.infected.WailerEntity;
import com.igneko.aidirector.entity.projectile.AcidBallEntity;
import com.igneko.aidirector.entity.projectile.AcidPuddleEntity;
import com.igneko.aidirector.entity.projectile.BehemothRockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** Tipos de entidad de los Infectados Especiales, sus proyectiles y sus huevos generadores. */
public final class ModEntities {

    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TheAIDirector.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TheAIDirector.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TheAIDirector.MOD_ID);

    private static final List<RegistryObject<Item>> EGGS = new ArrayList<>();

    public static final RegistryObject<EntityType<LurkerEntity>> LURKER = monster("lurker", LurkerEntity::new, 0.7f, 1.45f, 0x6E5A2C, 0x8B9478);
    public static final RegistryObject<EntityType<StranglerEntity>> STRANGLER = monster("strangler", StranglerEntity::new, 0.6f, 2.1f, 0x5A2328, 0x9C4E6A);
    public static final RegistryObject<EntityType<BloaterEntity>> BLOATER = monster("bloater", BloaterEntity::new, 1.0f, 1.95f, 0x9AA38A, 0xC8C45A);
    public static final RegistryObject<EntityType<CorroderEntity>> CORRODER = monster("corroder", CorroderEntity::new, 0.6f, 2.0f, 0x2F5E5A, 0xB7FF3C);
    public static final RegistryObject<EntityType<LeechEntity>> LEECH = monster("leech", LeechEntity::new, 0.55f, 1.15f, 0xA58F8F, 0x55603A);
    public static final RegistryObject<EntityType<RammerEntity>> RAMMER = monster("rammer", RammerEntity::new, 0.95f, 2.25f, 0xD4762A, 0x7F7A72);
    public static final RegistryObject<EntityType<WailerEntity>> WAILER = monster("wailer", WailerEntity::new, 0.6f, 1.9f, 0xB9C3C9, 0xFF3B2F);
    public static final RegistryObject<EntityType<BehemothEntity>> BEHEMOTH = monster("behemoth", BehemothEntity::new, 1.5f, 2.9f, 0x8B8580, 0xC2502B);

    public static final RegistryObject<EntityType<AcidBallEntity>> ACID_BALL = ENTITIES.register("acid_ball",
            () -> EntityType.Builder.<AcidBallEntity>of(AcidBallEntity::new, MobCategory.MISC).sized(0.4f, 0.4f)
                    .clientTrackingRange(8).updateInterval(2).build("acid_ball"));
    public static final RegistryObject<EntityType<AcidPuddleEntity>> ACID_PUDDLE = ENTITIES.register("acid_puddle",
            () -> EntityType.Builder.<AcidPuddleEntity>of(AcidPuddleEntity::new, MobCategory.MISC).sized(0.5f, 0.1f)
                    .fireImmune().clientTrackingRange(8).updateInterval(10).build("acid_puddle"));
    public static final RegistryObject<EntityType<BehemothRockEntity>> BEHEMOTH_ROCK = ENTITIES.register("behemoth_rock",
            () -> EntityType.Builder.<BehemothRockEntity>of(BehemothRockEntity::new, MobCategory.MISC).sized(1.0f, 1.0f)
                    .clientTrackingRange(10).updateInterval(2).build("behemoth_rock"));

    public static final RegistryObject<EntityType<com.igneko.aidirector.entity.projectile.PipeBombEntity>> PIPE_BOMB = ENTITIES.register("pipe_bomb",
            () -> EntityType.Builder.<com.igneko.aidirector.entity.projectile.PipeBombEntity>of(com.igneko.aidirector.entity.projectile.PipeBombEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f).clientTrackingRange(8).updateInterval(2).build("pipe_bomb"));
    public static final RegistryObject<EntityType<com.igneko.aidirector.entity.projectile.BileJarEntity>> BILE_JAR = ENTITIES.register("bile_jar",
            () -> EntityType.Builder.<com.igneko.aidirector.entity.projectile.BileJarEntity>of(com.igneko.aidirector.entity.projectile.BileJarEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f).clientTrackingRange(8).updateInterval(2).build("bile_jar"));

    public static final RegistryObject<EntityType<com.igneko.aidirector.entity.projectile.MolotovEntity>> MOLOTOV = ENTITIES.register("molotov",
            () -> EntityType.Builder.<com.igneko.aidirector.entity.projectile.MolotovEntity>of(com.igneko.aidirector.entity.projectile.MolotovEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f).clientTrackingRange(8).updateInterval(2).build("molotov"));
    public static final RegistryObject<EntityType<com.igneko.aidirector.entity.projectile.FirePatchEntity>> FIRE_PATCH = ENTITIES.register("fire_patch",
            () -> EntityType.Builder.<com.igneko.aidirector.entity.projectile.FirePatchEntity>of(com.igneko.aidirector.entity.projectile.FirePatchEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.1f).fireImmune().clientTrackingRange(8).updateInterval(10).build("fire_patch"));
    public static final RegistryObject<EntityType<com.igneko.aidirector.entity.SurvivorBodyEntity>> SURVIVOR_BODY = ENTITIES.register("survivor_body",
            () -> EntityType.Builder.<com.igneko.aidirector.entity.SurvivorBodyEntity>of(com.igneko.aidirector.entity.SurvivorBodyEntity::new, MobCategory.MISC)
                    .sized(1.0f, 0.4f).fireImmune().clientTrackingRange(10).updateInterval(20).build("survivor_body"));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("special_infected", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.aidirector.special_infected"))
            .icon(() -> new ItemStack(EGGS.get(EGGS.size() - 1).get()))
            .displayItems((params, out) -> EGGS.forEach(egg -> out.accept(egg.get())))
            .build());

    private ModEntities() {}

    public static List<RegistryObject<Item>> eggs() {
        return EGGS;
    }

    private static <T extends Mob> RegistryObject<EntityType<T>> monster(String name, EntityType.EntityFactory<T> factory,
                                                                          float width, float height, int bg, int fg) {
        RegistryObject<EntityType<T>> type = ENTITIES.register(name, () -> EntityType.Builder.of(factory, MobCategory.MONSTER)
                .sized(width, height).clientTrackingRange(10).build(name));
        EGGS.add(ITEMS.register(name + "_spawn_egg", () -> new ForgeSpawnEggItem(type, bg, fg, new Item.Properties())));
        return type;
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(ModEntities::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(LURKER.get(), LurkerEntity.createAttributes().build());
        event.put(STRANGLER.get(), StranglerEntity.createAttributes().build());
        event.put(BLOATER.get(), BloaterEntity.createAttributes().build());
        event.put(CORRODER.get(), CorroderEntity.createAttributes().build());
        event.put(LEECH.get(), LeechEntity.createAttributes().build());
        event.put(RAMMER.get(), RammerEntity.createAttributes().build());
        event.put(WAILER.get(), WailerEntity.createAttributes().build());
        event.put(BEHEMOTH.get(), BehemothEntity.createAttributes().build());
    }
}
