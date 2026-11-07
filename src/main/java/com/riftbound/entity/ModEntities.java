package com.riftbound.entity;

import com.riftbound.RiftboundMod;
import com.riftbound.entity.boss.RiftSentinelEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    public static final ResourceKey<EntityType<?>> RIFT_SENTINEL_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, RiftboundMod.id("rift_sentinel"));

    public static final EntityType<RiftSentinelEntity> RIFT_SENTINEL = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            RIFT_SENTINEL_KEY,
            EntityType.Builder.of(RiftSentinelEntity::new, MobCategory.MONSTER)
                    .sized(1.4f, 3.2f)
                    .clientTrackingRange(10)
                    .build(RIFT_SENTINEL_KEY)
    );

    public static final ResourceKey<EntityType<?>> RIFT_BOLT_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, RiftboundMod.id("rift_bolt"));

    public static final EntityType<RiftBoltEntity> RIFT_BOLT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            RIFT_BOLT_KEY,
            EntityType.Builder.<RiftBoltEntity>of(RiftBoltEntity::new, MobCategory.MISC)
                    .sized(0.45f, 0.45f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build(RIFT_BOLT_KEY)
    );

    public static void register() {
        FabricDefaultAttributeRegistry.register(RIFT_SENTINEL, RiftSentinelEntity.createAttributes());
    }
}
