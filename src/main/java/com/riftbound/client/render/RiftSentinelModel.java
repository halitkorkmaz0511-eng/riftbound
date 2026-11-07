package com.riftbound.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class RiftSentinelModel extends EntityModel<RiftSentinelRenderer.SentinelRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart core;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public RiftSentinelModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.core = this.body.getChild("core");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-7.0F, -24.0F, -4.0F, 14.0F, 14.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 38).addBox(-4.5F, -10.0F, -2.5F, 9.0F, 10.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 9.0F, 9.0F)
                        .texOffs(36, 0).addBox(-7.5F, -13.0F, -1.0F, 3.0F, 8.0F, 3.0F)
                        .texOffs(36, 0).addBox(4.5F, -13.0F, -1.0F, 3.0F, 8.0F, 3.0F),
                PartPose.offset(0.0F, -25.0F, 0.0F));

        body.addOrReplaceChild("core",
                CubeListBuilder.create()
                        .texOffs(36, 16).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.25F)),
                PartPose.offset(0.0F, -17.0F, 0.0F));

        body.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(44, 28).addBox(-5.0F, -2.0F, -3.0F, 5.0F, 22.0F, 6.0F),
                PartPose.offset(-8.0F, -22.0F, 0.0F));

        body.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(44, 28).mirror().addBox(0.0F, -2.0F, -3.0F, 5.0F, 22.0F, 6.0F),
                PartPose.offset(8.0F, -22.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(RiftSentinelRenderer.SentinelRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * ((float) Math.PI / 180F);
        this.head.xRot = state.xRot * ((float) Math.PI / 180F);

        this.rightArm.xRot += Mth.cos(state.walkAnimationPos * 0.6662F) * 0.8F * state.walkAnimationSpeed;
        this.leftArm.xRot += Mth.cos(state.walkAnimationPos * 0.6662F + (float) Math.PI) * 0.8F * state.walkAnimationSpeed;

        this.animate(state.idleAnimationState, RiftSentinelAnimations.IDLE, state.ageInTicks, 1.0F);
        this.animate(state.strikeAnimationState, RiftSentinelAnimations.RIFT_STRIKE, state.ageInTicks, 1.0F);
        this.animate(state.voidWaveAnimationState, RiftSentinelAnimations.VOID_WAVE, state.ageInTicks, 1.0F);
        this.animate(state.deathAnimationState, RiftSentinelAnimations.DEATH, state.ageInTicks, 1.0F);
    }
}
