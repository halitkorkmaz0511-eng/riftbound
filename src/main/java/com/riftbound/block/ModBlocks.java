package com.riftbound.block;

import com.riftbound.RiftboundMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.function.Function;

public class ModBlocks {
    // 1.21.4 uyumlu factory tabanlı blok kaydı
    public static final Block RIFT_ALTAR = registerBlock("rift_altar", 
        props -> new RiftAltarBlock(props.strength(5.0f, 1200.0f).lightLevel(s -> 12).requiresCorrectToolForDrops())
    );

    private static Block registerBlock(String name, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("riftbound", name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        
        // KRİTİK DÜZELTME: Block id not set hatasını önlemek için setId(blockKey) uygulanıyor
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().setId(blockKey);
        Block registered = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(props));
        
        Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(registered, new Item.Properties().setId(itemKey)));
        return registered;
    }

    public static void register() {
        RiftboundMod.LOGGER.info("Registering Riftbound Blocks...");
    }
}