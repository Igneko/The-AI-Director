package com.igneko.aidirector.client.render;

import com.igneko.aidirector.client.model.InfectedModel;
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

    public static final ModelLayerLocation LURKER = layer("lurker");
    public static final ModelLayerLocation STRANGLER = layer("strangler");
    public static final ModelLayerLocation BLOATER = layer("bloater");
    public static final ModelLayerLocation CORRODER = layer("corroder");
    public static final ModelLayerLocation LEECH = layer("leech");
    public static final ModelLayerLocation RAMMER = layer("rammer");
    public static final ModelLayerLocation WAILER = layer("wailer");
    public static final ModelLayerLocation BEHEMOTH = layer("behemoth");

    private ClientRenderers() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(new ResourceLocation(TheAIDirector.MOD_ID, name), "main");
    }

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LURKER, InfectedModel::lurkerLayer);
        event.registerLayerDefinition(STRANGLER, InfectedModel::stranglerLayer);
        event.registerLayerDefinition(BLOATER, InfectedModel::bloaterLayer);
        event.registerLayerDefinition(CORRODER, InfectedModel::corroderLayer);
        event.registerLayerDefinition(LEECH, InfectedModel::leechLayer);
        event.registerLayerDefinition(RAMMER, InfectedModel::rammerLayer);
        event.registerLayerDefinition(WAILER, InfectedModel::wailerLayer);
        event.registerLayerDefinition(BEHEMOTH, InfectedModel::behemothLayer);
        event.registerLayerDefinition(ThrowableRenderer.PIPE_BOMB, ThrowableRenderer::pipeBombLayer);
        event.registerLayerDefinition(ThrowableRenderer.BILE_JAR, ThrowableRenderer::bileJarLayer);
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LURKER.get(), c -> new InfectedRenderer<>(c, LURKER, InfectedModel.Kind.LURKER, "lurker", 0.95f, 0.5f));
        event.registerEntityRenderer(ModEntities.STRANGLER.get(), c -> new StranglerRenderer(c, STRANGLER));
        event.registerEntityRenderer(ModEntities.BLOATER.get(), c -> new InfectedRenderer<>(c, BLOATER, InfectedModel.Kind.BLOATER, "bloater", 1.0f, 0.8f));
        event.registerEntityRenderer(ModEntities.CORRODER.get(), c -> new InfectedRenderer<>(c, CORRODER, InfectedModel.Kind.CORRODER, "corroder", 0.95f, 0.5f));
        event.registerEntityRenderer(ModEntities.LEECH.get(), c -> new InfectedRenderer<>(c, LEECH, InfectedModel.Kind.LEECH, "leech", 0.6f, 0.35f));
        event.registerEntityRenderer(ModEntities.RAMMER.get(), c -> new InfectedRenderer<>(c, RAMMER, InfectedModel.Kind.RAMMER, "rammer", 1.12f, 0.8f));
        event.registerEntityRenderer(ModEntities.WAILER.get(), c -> new InfectedRenderer<>(c, WAILER, InfectedModel.Kind.WAILER, "wailer", 0.95f, 0.5f));
        event.registerEntityRenderer(ModEntities.BEHEMOTH.get(), c -> new InfectedRenderer<>(c, BEHEMOTH, InfectedModel.Kind.BEHEMOTH, "behemoth", 1.6f, 1.4f));
        event.registerEntityRenderer(ModEntities.ACID_BALL.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.ACID_PUDDLE.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.BEHEMOTH_ROCK.get(), RockRenderer::new);
        event.registerEntityRenderer(ModEntities.PIPE_BOMB.get(), c -> new ThrowableRenderer<>(c, ThrowableRenderer.PIPE_BOMB, "pipe_bomb"));
        event.registerEntityRenderer(ModEntities.BILE_JAR.get(), c -> new ThrowableRenderer<>(c, ThrowableRenderer.BILE_JAR, "bile_jar"));
    }
}
