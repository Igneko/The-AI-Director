package com.igneko.aidirector.client.render;

import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.registry.ModEntities;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registro de capas de modelo y renderers de los Infectados Especiales. */
@Mod.EventBusSubscriber(modid = TheAIDirector.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientRenderers {


    private ClientRenderers() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(new ResourceLocation(TheAIDirector.MOD_ID, name), "main");
    }

    /** El icono de la caja de suministros depende del tipo guardado en el objeto (BlockStateTag). */
    @SubscribeEvent
    public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(
                com.igneko.aidirector.registry.ModBlocks.SUPPLY_CRATE_ITEM.get(),
                new ResourceLocation(TheAIDirector.MOD_ID, "crate"), (stack, level, entity, seed) -> {
                    net.minecraft.nbt.CompoundTag tag = stack.getTagElement("BlockStateTag");
                    if (tag == null) return 0f;
                    int kind = switch (tag.getString("kind")) {
                        case "armory" -> 1;
                        case "throwables" -> 2;
                        default -> 0;
                    };
                    return (kind + ("true".equals(tag.getString("locked")) ? 0.5f : 0f)) / 4f;
                }));
    }

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ThrowableRenderer.PIPE_BOMB, ThrowableRenderer::pipeBombLayer);
        event.registerLayerDefinition(ThrowableRenderer.BILE_JAR, ThrowableRenderer::bileJarLayer);
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LURKER.get(), c -> new GeoInfectedRenderer<>(c, "lurker", 0.95f, 0.5f, true));
        event.registerEntityRenderer(ModEntities.STRANGLER.get(), StranglerRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOATER.get(), c -> new GeoInfectedRenderer<>(c, "bloater", 1.0f, 0.8f, false));
        event.registerEntityRenderer(ModEntities.CORRODER.get(), c -> new GeoInfectedRenderer<>(c, "corroder", 0.95f, 0.5f, true));
        event.registerEntityRenderer(ModEntities.LEECH.get(), c -> new GeoInfectedRenderer<>(c, "leech", 0.62f, 0.35f, true));
        event.registerEntityRenderer(ModEntities.RAMMER.get(), c -> new GeoInfectedRenderer<>(c, "rammer", 1.1f, 0.8f, true));
        event.registerEntityRenderer(ModEntities.WAILER.get(), c -> new GeoInfectedRenderer<>(c, "wailer", 0.95f, 0.5f, true));
        event.registerEntityRenderer(ModEntities.BEHEMOTH.get(), c -> new GeoInfectedRenderer<>(c, "behemoth", 1.6f, 1.4f, true));
        event.registerEntityRenderer(ModEntities.ACID_BALL.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.ACID_PUDDLE.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.BEHEMOTH_ROCK.get(), RockRenderer::new);
        event.registerEntityRenderer(ModEntities.PIPE_BOMB.get(), c -> new ThrowableRenderer<>(c, ThrowableRenderer.PIPE_BOMB, "pipe_bomb", com.igneko.aidirector.registry.ModWeapons.PIPE_BOMB));
        event.registerEntityRenderer(ModEntities.BILE_JAR.get(), c -> new ThrowableRenderer<>(c, ThrowableRenderer.BILE_JAR, "bile_jar", com.igneko.aidirector.registry.ModWeapons.BILE_JAR));
        event.registerEntityRenderer(ModEntities.MOLOTOV.get(), c -> new ThrowableRenderer<>(c, ThrowableRenderer.BILE_JAR, "bile_jar", com.igneko.aidirector.registry.ModWeapons.MOLOTOV));
        event.registerEntityRenderer(ModEntities.FIRE_PATCH.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.SURVIVOR_BODY.get(), SurvivorBodyRenderer::new);
    }
}
