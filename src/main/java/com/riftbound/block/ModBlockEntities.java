package com.riftbound.block;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static BlockEntityType<RiftAltarBlockEntity> RIFT_ALTAR_BE;

    public static void register() {
        RIFT_ALTAR_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("riftbound", "rift_altar"),
            FabricBlockEntityTypeBuilder.create(RiftAltarBlockEntity::new, ModBlocks.RIFT_ALTAR).build()
        );
    }
}