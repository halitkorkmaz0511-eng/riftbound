package com.riftbound.client.render;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

public class RiftSentinelAnimations {
    public static final AnimationDefinition IDLE = AnimationDefinition.Builder.withLength(2.0f).looping()
            .addAnimation("core", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.0f, KeyframeAnimations.posVec(0.0f, 3.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("core", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(2.0f, KeyframeAnimations.degreeVec(0.0f, 180.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)))
            .build();

    public static final AnimationDefinition RIFT_STRIKE = AnimationDefinition.Builder.withLength(1.6f)
            .addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.65f, KeyframeAnimations.degreeVec(-145.0f, -20.0f, 15.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.85f, KeyframeAnimations.degreeVec(55.0f, 15.0f, -10.0f), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.25f, KeyframeAnimations.degreeVec(45.0f, 10.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.6f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("left_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.65f, KeyframeAnimations.degreeVec(-130.0f, 20.0f, -15.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.85f, KeyframeAnimations.degreeVec(50.0f, -15.0f, 10.0f), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.6f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM)))
            .build();

    public static final AnimationDefinition VOID_WAVE = AnimationDefinition.Builder.withLength(1.6f)
            .addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.1f, KeyframeAnimations.degreeVec(-90.0f, 0.0f, 75.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.4f, KeyframeAnimations.degreeVec(20.0f, 0.0f, -30.0f), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.6f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("left_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.1f, KeyframeAnimations.degreeVec(-90.0f, 0.0f, -75.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.4f, KeyframeAnimations.degreeVec(20.0f, 0.0f, 30.0f), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.6f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM)))
            .build();

    public static final AnimationDefinition DEATH = AnimationDefinition.Builder.withLength(3.0f)
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.0f, KeyframeAnimations.posVec(0.0f, 10.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.0f, KeyframeAnimations.posVec(0.0f, -12.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(3.0f, KeyframeAnimations.degreeVec(25.0f, 540.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)))
            .build();
}
