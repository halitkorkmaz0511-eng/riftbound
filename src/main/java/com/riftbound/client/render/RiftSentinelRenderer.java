package com.riftbound.client.render;

import com.riftbound.RiftboundMod;
import com.riftbound.entity.boss.RiftSentinelEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AnimationState;

public class RiftSentinelRenderer extends MobRenderer<RiftSentinelEntity, RiftSentinelRenderer.SentinelRenderState, RiftSentinelModel> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(RiftboundMod.id("rift_sentinel"), "main");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/enderman/enderman.png");

    public RiftSentinelRenderer(EntityRendererProvider.Context context) {
        super(context, new RiftSentinelModel(context.bakeLayer(LAYER_LOCATION)), 0.85F);
    }

    @Override
    public SentinelRenderState createRenderState() {
        return new SentinelRenderState();
    }

    @Override
    public void extractRenderState(RiftSentinelEntity entity, SentinelRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.enraged = entity.isEnraged();
        state.idleAnimationState.copyFrom(entity.idleAnimationState);
        state.strikeAnimationState.copyFrom(entity.strikeAnimationState);
        state.voidWaveAnimationState.copyFrom(entity.voidWaveAnimationState);
        state.deathAnimationState.copyFrom(entity.deathAnimationState);
    }

    @Override
    public ResourceLocation getTextureLocation(SentinelRenderState state) {
        return TEXTURE;
    }

    public static class SentinelRenderState extends LivingEntityRenderState {
        public boolean enraged;
        public final AnimationState idleAnimationState = new AnimationState();
        public final AnimationState strikeAnimationState = new AnimationState();
        public final AnimationState voidWaveAnimationState = new AnimationState();
        public final AnimationState deathAnimationState = new AnimationState();
    }
}
