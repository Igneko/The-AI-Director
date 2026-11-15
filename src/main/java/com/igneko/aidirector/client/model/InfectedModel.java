package com.igneko.aidirector.client.model;

import com.igneko.aidirector.entity.infected.BehemothEntity;
import com.igneko.aidirector.entity.infected.BloaterEntity;
import com.igneko.aidirector.entity.infected.CorroderEntity;
import com.igneko.aidirector.entity.infected.LeechEntity;
import com.igneko.aidirector.entity.infected.LurkerEntity;
import com.igneko.aidirector.entity.infected.RammerEntity;
import com.igneko.aidirector.entity.infected.SpecialInfectedEntity;
import com.igneko.aidirector.entity.infected.WailerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Modelo humanoide deformado para los Infectados Especiales. Cada tipo define su malla (proporciones
 * y piezas extra) y su postura/animación. Las texturas se generan con la misma disposición UV.
 */
@OnlyIn(Dist.CLIENT)
public class InfectedModel<T extends SpecialInfectedEntity> extends HumanoidModel<T> {

    public enum Kind {
        //          piernas, torso, hombroX, caídaHombro
        LURKER(12, 12, 5, 2),
        STRANGLER(14, 14, 5, 2),
        BLOATER(12, 12, 6.5f, 2),
        CORRODER(12, 12, 4.5f, 2),
        LEECH(12, 12, 5, 2),
        RAMMER(12, 12, 6, 2),
        WAILER(12, 12, 4.5f, 2),
        BEHEMOTH(10, 14, 12, 3);

        final float legLen, bodyLen, shoulderX, shoulderDrop;

        Kind(float legLen, float bodyLen, float shoulderX, float shoulderDrop) {
            this.legLen = legLen;
            this.bodyLen = bodyLen;
            this.shoulderX = shoulderX;
            this.shoulderDrop = shoulderDrop;
        }
    }

    private final Kind kind;

    public InfectedModel(ModelPart root, Kind kind) {
        super(root);
        this.kind = kind;
    }

    // ================================================================== mallas

    private static MeshDefinition humanoid() {
        return HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
    }

    private static void noHat(PartDefinition root) {
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
    }

    /** Acechador: complexión fibrosa y capucha raída (capa "hat"). 64x64. */
    public static LayerDefinition lurkerLayer() {
        MeshDefinition mesh = humanoid();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-4f, -8f, -4f, 8, 8, 8, new CubeDeformation(0.6f)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Estrangulador: alto y desgarbado, tumores en cara y hombro, lengua colgante. 128x64. */
    public static LayerDefinition stranglerLayer() {
        MeshDefinition mesh = humanoid();
        PartDefinition root = mesh.getRoot();
        noHat(root);
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4f, -8f, -4f, 8, 8, 8), PartPose.ZERO);
        head.addOrReplaceChild("tumor", CubeListBuilder.create().texOffs(64, 0).addBox(1.5f, -5f, -4.5f, 4, 5, 3), PartPose.ZERO);
        head.addOrReplaceChild("tumor2", CubeListBuilder.create().texOffs(64, 8).addBox(2.5f, -1.5f, -3f, 3, 3, 4), PartPose.ZERO);
        head.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(80, 0).addBox(-0.5f, -1.5f, -4.6f, 1, 6, 1), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-4f, 0f, -2f, 8, 14, 4), PartPose.ZERO);
        body.addOrReplaceChild("growth", CubeListBuilder.create().texOffs(64, 16).addBox(-5.5f, -1f, -2.5f, 4, 4, 5), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-2f, -2f, -1.5f, 3, 15, 3), PartPose.offset(-5f, 2f, 0f));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1f, -2f, -1.5f, 3, 15, 3), PartPose.offset(5f, 2f, 0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2f, 0f, -2f, 4, 14, 4), PartPose.offset(-1.9f, 10f, 0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2f, 0f, -2f, 4, 14, 4), PartPose.offset(1.9f, 10f, 0f));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** Hinchado: barriga enorme, papada y extremidades gruesas. 128x64. */
    public static LayerDefinition bloaterLayer() {
        MeshDefinition mesh = humanoid();
        PartDefinition root = mesh.getRoot();
        noHat(root);
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4f, -8f, -4f, 8, 8, 8), PartPose.ZERO);
        head.addOrReplaceChild("jowl", CubeListBuilder.create().texOffs(64, 20).addBox(-4.5f, -2f, -5f, 9, 4, 6), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-4f, 0f, -2f, 8, 12, 4), PartPose.ZERO);
        body.addOrReplaceChild("belly", CubeListBuilder.create().texOffs(64, 0).addBox(-6f, 3f, -6f, 12, 10, 9), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.5f, -2f, -2.5f, 5, 12, 5), PartPose.offset(-6.5f, 2f, 0f));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.5f, -2f, -2.5f, 5, 12, 5), PartPose.offset(6.5f, 2f, 0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 34).addBox(-2.5f, 0f, -2.5f, 5, 12, 5), PartPose.offset(-2.4f, 12f, 0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 34).mirror().addBox(-2.5f, 0f, -2.5f, 5, 12, 5), PartPose.offset(2.4f, 12f, 0f));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** Corrosiva: cuello alargado, mandíbula desencajada y melena lacia. 128x64. */
    public static LayerDefinition corroderLayer() {
        MeshDefinition mesh = humanoid();
        PartDefinition root = mesh.getRoot();
        noHat(root);
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4f, -8f, -4f, 8, 8, 8), PartPose.ZERO);
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(64, 0).addBox(-3f, -1f, -4.5f, 6, 4, 4), PartPose.ZERO);
        head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(64, 8).addBox(-4.5f, -8.5f, 2.5f, 9, 13, 2), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-3.5f, 0f, -2f, 7, 12, 4), PartPose.ZERO);
        body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(64, 24).addBox(-1.5f, -4f, -1.5f, 3, 4, 3), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-2f, -2f, -1.5f, 3, 12, 3), PartPose.offset(-4.5f, 2f, 0f));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1f, -2f, -1.5f, 3, 12, 3), PartPose.offset(4.5f, 2f, 0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.5f, 0f, -1.5f, 3, 12, 3), PartPose.offset(-1.6f, 12f, 0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.5f, 0f, -1.5f, 3, 12, 3), PartPose.offset(1.6f, 12f, 0f));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** Parásito: cabeza desproporcionada sobre un cuerpo encogido. 128x64. */
    public static LayerDefinition leechLayer() {
        MeshDefinition mesh = humanoid();
        PartDefinition root = mesh.getRoot();
        noHat(root);
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(64, 0).addBox(-5f, -10f, -5f, 10, 10, 10), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-2f, -2f, -1.5f, 3, 13, 3), PartPose.offset(-5f, 2f, 0f));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1f, -2f, -1.5f, 3, 13, 3), PartPose.offset(5f, 2f, 0f));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** Embestidor: un brazo descomunal con un callo óseo y el otro atrofiado. 128x64. */
    public static LayerDefinition rammerLayer() {
        MeshDefinition mesh = humanoid();
        PartDefinition root = mesh.getRoot();
        noHat(root);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(64, 40).addBox(-5f, 0f, -3f, 10, 12, 6), PartPose.ZERO);
        PartDefinition big = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(64, 0)
                .addBox(-7f, -3f, -4f, 8, 17, 8), PartPose.offset(-6f, 2f, 0f));
        big.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(64, 25).addBox(-7.5f, -4f, -4.5f, 9, 5, 9), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-1f, -2f, -1.5f, 3, 11, 3), PartPose.offset(6f, 2f, 0f));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** Plañidera: extremidades finísimas, melena larga y garras enormes. 128x64. */
    public static LayerDefinition wailerLayer() {
        MeshDefinition mesh = humanoid();
        PartDefinition root = mesh.getRoot();
        noHat(root);
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4f, -8f, -4f, 8, 8, 8), PartPose.ZERO);
        head.addOrReplaceChild("hair_back", CubeListBuilder.create().texOffs(64, 0).addBox(-4.5f, -8.5f, 3.5f, 9, 14, 1), PartPose.ZERO);
        head.addOrReplaceChild("hair_left", CubeListBuilder.create().texOffs(84, 0).addBox(3.5f, -8.5f, -3.5f, 1, 12, 7), PartPose.ZERO);
        head.addOrReplaceChild("hair_right", CubeListBuilder.create().texOffs(100, 0).addBox(-4.5f, -8.5f, -3.5f, 1, 12, 7), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-3.5f, 0f, -2f, 7, 12, 4), PartPose.ZERO);
        CubeListBuilder rightClaws = CubeListBuilder.create().texOffs(40, 16).addBox(-2f, -2f, -1.5f, 3, 12, 3);
        CubeListBuilder leftClaws = CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1f, -2f, -1.5f, 3, 12, 3);
        PartDefinition ra = root.addOrReplaceChild("right_arm", rightClaws, PartPose.offset(-4.5f, 2f, 0f));
        PartDefinition la = root.addOrReplaceChild("left_arm", leftClaws, PartPose.offset(4.5f, 2f, 0f));
        for (int i = 0; i < 3; i++) {
            float x = -1.8f + i * 1.1f;
            ra.addOrReplaceChild("claw" + i, CubeListBuilder.create().texOffs(64, 20).addBox(x, 10f, -1f, 1, 7, 1), PartPose.ZERO);
            la.addOrReplaceChild("claw" + i, CubeListBuilder.create().texOffs(64, 20).addBox(x + 0.8f, 10f, -1f, 1, 7, 1), PartPose.ZERO);
        }
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.5f, 0f, -1.5f, 3, 12, 3), PartPose.offset(-1.6f, 12f, 0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.5f, 0f, -1.5f, 3, 12, 3), PartPose.offset(1.6f, 12f, 0f));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** Coloso: torso colosal con joroba, cabeza hundida, brazos y puños gigantes. 128x128. */
    public static LayerDefinition behemothLayer() {
        MeshDefinition mesh = humanoid();
        PartDefinition root = mesh.getRoot();
        noHat(root);
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3f, -5f, -4f, 6, 6, 6), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-9f, 0f, -5f, 18, 14, 10), PartPose.ZERO);
        body.addOrReplaceChild("hump", CubeListBuilder.create().texOffs(0, 40).addBox(-8f, -4f, -3f, 16, 6, 9), PartPose.ZERO);
        PartDefinition ra = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(56, 0)
                .addBox(-9f, -3f, -5f, 10, 20, 10), PartPose.offset(-12f, 2f, 0f));
        ra.addOrReplaceChild("fist", CubeListBuilder.create().texOffs(56, 34).addBox(-9.5f, 15f, -5.5f, 11, 7, 11), PartPose.ZERO);
        PartDefinition la = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(56, 0).mirror()
                .addBox(-1f, -3f, -5f, 10, 20, 10), PartPose.offset(12f, 2f, 0f));
        la.addOrReplaceChild("fist", CubeListBuilder.create().texOffs(56, 34).mirror().addBox(-1.5f, 15f, -5.5f, 11, 7, 11), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 56).addBox(-3.5f, 0f, -3.5f, 7, 10, 7), PartPose.offset(-4.5f, 14f, 0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 56).mirror().addBox(-3.5f, 0f, -3.5f, 7, 10, 7), PartPose.offset(4.5f, 14f, 0f));
        return LayerDefinition.create(mesh, 128, 128);
    }

    // ================================================================== animación

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, age, yaw, pitch);
        int state = entity.getState();
        float walk = Mth.cos(limbSwing * 0.6662f) * limbSwingAmount;
        float breathe = Mth.sin(age * 0.08f);

        switch (kind) {
            case LURKER -> {
                boolean leaping = state == LurkerEntity.STATE_LEAP;
                boolean pinning = state == LurkerEntity.STATE_PINNING;
                stance(leaping ? 1.2f : 1.0f, leaping ? 0.2f : 0.8f);
                if (leaping) {
                    arms(-2.4f, -2.4f, 0.15f);
                    legs(0.9f, 0.9f);
                } else if (pinning) {
                    arms(-1.3f + Mth.sin(age * 0.9f) * 0.7f, -1.3f - Mth.sin(age * 0.9f) * 0.7f, 0.2f);
                } else {
                    arms(-0.4f + walk * 0.6f, -0.4f - walk * 0.6f, 0.12f);
                }
            }
            case STRANGLER -> {
                stance(0.22f, 0.06f);
                boolean tongue = entity instanceof com.igneko.aidirector.entity.infected.StranglerEntity s && s.getTongueTarget() != null;
                arms(-0.15f + walk * 0.5f, tongue ? -1.6f : -0.15f - walk * 0.5f, 0.08f);
                if (tongue) head.xRot -= 0.25f;
            }
            case BLOATER -> {
                stance(0.02f, 0f);
                body.y += breathe * 0.4f;
                arms(-0.2f + walk * 0.4f, -0.2f - walk * 0.4f, 0.45f);
                if (state == BloaterEntity.STATE_VOMIT) {
                    head.xRot = -0.35f + Mth.sin(age * 1.2f) * 0.1f;
                    arms(-0.6f, -0.6f, 0.6f);
                }
            }
            case CORRODER -> {
                stance(0.42f, 0.1f);
                neckUp(4f);
                arms(-0.35f + walk * 0.5f, -0.35f - walk * 0.5f, 0.1f);
                if (state == CorroderEntity.STATE_SPIT) {
                    head.xRot = -0.5f;
                    arms(-0.9f, -0.9f, 0.3f);
                }
            }
            case LEECH -> {
                if (riding) {
                    stance(0.2f, 0f);
                    arms(-2.7f, -2.7f, 0.35f);
                } else {
                    stance(0.75f, 0.55f);
                    arms(-0.9f + walk * 0.7f + Mth.sin(age * 0.3f) * 0.15f, -0.9f - walk * 0.7f, 0.25f);
                }
            }
            case RAMMER -> {
                boolean charging = state == RammerEntity.STATE_CHARGE;
                stance(charging ? 0.75f : 0.4f, charging ? 0.25f : 0.12f);
                rightArm.x = -kind.shoulderX - 1f;
                if (charging) {
                    rightArm.xRot = -1.35f;
                    rightArm.zRot = 0.2f;
                } else if (state == RammerEntity.STATE_POUND) {
                    rightArm.xRot = -2.8f + Math.abs(Mth.sin(age * 0.21f)) * 2.6f;
                    rightArm.zRot = 0.1f;
                } else {
                    rightArm.xRot = -0.25f + walk * 0.3f;
                    rightArm.zRot = 0.18f;
                }
                leftArm.xRot = -0.5f - walk * 0.6f;
                leftArm.zRot = -0.1f;
            }
            case WAILER -> {
                if (state == WailerEntity.STATE_CALM) {
                    sitCrying(age);
                } else if (state == WailerEntity.STATE_DISTURBED) {
                    stance(0.3f, 0.05f);
                    arms(-0.5f, -0.5f, 1.0f + Mth.sin(age * 0.5f) * 0.08f);
                } else {
                    stance(0.65f, 0.12f);
                    arms(0.9f + walk * 0.4f, 0.9f - walk * 0.4f, 0.45f);
                }
            }
            case BEHEMOTH -> {
                stance(0.35f, 0.12f);
                if (state == BehemothEntity.STATE_ROCK) {
                    arms(-2.9f, -2.9f, -0.15f);
                } else {
                    arms(-0.3f + walk * 0.8f, -0.3f - walk * 0.8f, 0.12f);
                }
            }
        }

        // Golpe cuerpo a cuerpo: barrido con el brazo derecho.
        if (attackTime > 0f && kind != Kind.WAILER) {
            float swing = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot -= swing * 1.6f;
        } else if (attackTime > 0f) {
            rightArm.xRot -= Mth.sin(attackTime * Mth.PI) * 2.0f;
            leftArm.xRot -= Mth.sin(attackTime * Mth.PI) * 2.0f;
        }
        hat.copyFrom(head);
    }

    /** Coloca piernas, torso, cabeza y hombros para una inclinación y una flexión dadas. */
    private void stance(float lean, float legBend) {
        float hipY = 24f - kind.legLen * Mth.cos(legBend);
        rightLeg.y = hipY;
        leftLeg.y = hipY;
        rightLeg.z = 0f;
        leftLeg.z = 0f;
        rightLeg.xRot -= legBend;
        leftLeg.xRot -= legBend;
        body.xRot = lean;
        body.y = hipY - kind.bodyLen * Mth.cos(lean);
        body.z = -kind.bodyLen * Mth.sin(lean);
        head.y = body.y;
        head.z = body.z;
        rightArm.x = -kind.shoulderX;
        leftArm.x = kind.shoulderX;
        rightArm.y = body.y + kind.shoulderDrop * Mth.cos(lean);
        leftArm.y = rightArm.y;
        rightArm.z = body.z + kind.shoulderDrop * Mth.sin(lean);
        leftArm.z = rightArm.z;
    }

    private void arms(float rightX, float leftX, float spread) {
        rightArm.xRot = rightX;
        leftArm.xRot = leftX;
        rightArm.zRot = spread;
        leftArm.zRot = -spread;
        rightArm.yRot = 0f;
        leftArm.yRot = 0f;
    }

    private void legs(float rightX, float leftX) {
        rightLeg.xRot = rightX;
        leftLeg.xRot = leftX;
    }

    /** Eleva la cabeza sobre el cuello alargado (sigue la inclinación del torso). */
    private void neckUp(float neck) {
        head.y -= neck * Mth.cos(body.xRot);
        head.z -= neck * Mth.sin(body.xRot);
    }

    /** Sentada en el suelo, encogida, con la cara entre las manos y sollozando. */
    private void sitCrying(float age) {
        float sob = Mth.sin(age * 0.35f) * 0.4f;
        rightLeg.xRot = -1.45f;
        leftLeg.xRot = -1.45f;
        rightLeg.yRot = 0.25f;
        leftLeg.yRot = -0.25f;
        rightLeg.y = 22f;
        leftLeg.y = 22f;
        rightLeg.z = 0f;
        leftLeg.z = 0f;
        body.xRot = 0.35f;
        body.y = 22f - 12f * Mth.cos(0.35f) + sob;
        body.z = -12f * Mth.sin(0.35f);
        head.y = body.y;
        head.z = body.z;
        head.xRot = 0.75f;
        head.yRot = 0f;
        rightArm.x = -4.5f;
        leftArm.x = 4.5f;
        rightArm.y = body.y + 2f;
        leftArm.y = body.y + 2f;
        rightArm.z = body.z + 0.7f;
        leftArm.z = body.z + 0.7f;
        rightArm.xRot = -2.1f;
        leftArm.xRot = -2.1f;
        rightArm.yRot = -0.45f;
        leftArm.yRot = 0.45f;
        rightArm.zRot = 0f;
        leftArm.zRot = 0f;
    }
}
