package com.igneko.aidirector.client.render;

import com.igneko.aidirector.core.TheAIDirector;
import com.igneko.aidirector.entity.projectile.PipeBombEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Arrojables en 3D: tubo con tapas, cinta y mecha (bomba casera) o frasco con tapa y etiqueta. */
@OnlyIn(Dist.CLIENT)
public class ThrowableRenderer<T extends ThrowableProjectile> extends EntityRenderer<T> {

    public static final ModelLayerLocation PIPE_BOMB = new ModelLayerLocation(new ResourceLocation(TheAIDirector.MOD_ID, "pipe_bomb"), "main");
    public static final ModelLayerLocation BILE_JAR = new ModelLayerLocation(new ResourceLocation(TheAIDirector.MOD_ID, "bile_jar"), "main");

    private final SimpleModel model;
    private final ResourceLocation texture;
    private final java.util.function.Supplier<net.minecraft.world.item.Item> item;

    public ThrowableRenderer(EntityRendererProvider.Context ctx, ModelLayerLocation layer, String textureName,
                             java.util.function.Supplier<net.minecraft.world.item.Item> item) {
        super(ctx);
        this.item = item;
        this.model = new SimpleModel(ctx.bakeLayer(layer));
        this.texture = new ResourceLocation(TheAIDirector.MOD_ID, "textures/entity/" + textureName + ".png");
        this.shadowRadius = 0.15f;
    }

    /** Tubo metálico de 3×8×3 con tapas, cinta adhesiva y una mecha con luz. 32×32. */
    public static LayerDefinition pipeBombLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5f, -4f, -1.5f, 3, 8, 3)
                .texOffs(12, 0).addBox(-2f, -5f, -2f, 4, 1, 4)
                .texOffs(12, 5).addBox(-2f, 4f, -2f, 4, 1, 4)
                .texOffs(0, 11).addBox(-1.75f, -1f, -1.75f, 3.5f, 2, 3.5f)
                .texOffs(16, 10).addBox(-0.5f, -7f, -0.5f, 1, 2, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    /** Frasco de cristal verdoso con tapa metálica y etiqueta. 32×32. */
    public static LayerDefinition bileJarLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5f, -3f, -2.5f, 5, 6, 5)
                .texOffs(0, 11).addBox(-1.5f, -4f, -1.5f, 3, 1, 3)
                .texOffs(12, 11).addBox(-2f, -5f, -2f, 4, 1, 4), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, 0.15, 0);
        boolean resting = entity instanceof PipeBombEntity bomb && bomb.isResting();
        if (resting) {
            pose.mulPose(Axis.ZP.rotationDegrees(90f));
        } else {
            float spin = (entity.tickCount + partialTick) * 25f;
            pose.mulPose(Axis.YP.rotationDegrees(spin * 0.7f));
            pose.mulPose(Axis.XP.rotationDegrees(spin));
        }
        // Modelo 3D del ítem hecho en Blockbench (vista "fixed"); el modelo por código queda de respaldo.
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item.get());
        net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(stack,
                net.minecraft.world.item.ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers,
                entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    /** Modelo de una sola pieza. */
    private static final class SimpleModel extends Model {
        private final ModelPart root;

        SimpleModel(ModelPart root) {
            super(RenderType::entityCutoutNoCull);
            this.root = root;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
            root.render(pose, vc, light, overlay, r, g, b, a);
        }
    }
}
