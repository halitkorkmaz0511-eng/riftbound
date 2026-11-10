package com.riftbound;

import com.riftbound.ability.AbilityManager;
import com.riftbound.entity.ModEntities;
import com.riftbound.item.ModItems;
import com.riftbound.network.RiftboundPayloads;
import com.riftbound.world.structure.ModStructures;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RiftboundMod implements ModInitializer {
    public static final String MOD_ID = "riftbound";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        com.riftbound.horror.GuideBookManager.register();
        com.riftbound.horror.HorrorDirector.init();
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            handler.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("\u00A7d[Riftbound] \u00A7bMod rehberini acmak icin Z tusuna basiniz."));
        });
        com.riftbound.block.ModBlocks.register();
        com.riftbound.block.ModBlockEntities.register();
        com.riftbound.command.RiftCommands.register();
        LOGGER.info("Initializing Riftbound Mod");
        ModItems.register();
        ModEntities.register();
        RiftboundPayloads.register();
        AbilityManager.register();
        ModStructures.register();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}