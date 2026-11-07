package com.riftbound.item;

import com.riftbound.RiftboundMod;
import com.riftbound.ability.AbilityManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import java.util.function.Function;

public class ModItems {
    // 1. Çekirdek Eşya
    public static final Item RIFT_CORE = register("rift_core", props -> new RiftCoreItem(props.rarity(Rarity.EPIC).fireResistant()));

    // 2. Özel Rift Silahları (Blade, Scythe, Staff)
    public static final Item RIFT_BLADE = register("rift_blade", props -> new RiftWeaponItem(1, 9.0, -2.2, props.rarity(Rarity.EPIC).fireResistant().stacksTo(1)));
    public static final Item RIFT_SCYTHE = register("rift_scythe", props -> new RiftWeaponItem(2, 12.0, -2.8, props.rarity(Rarity.EPIC).fireResistant().stacksTo(1)));
    public static final Item RIFT_STAFF = register("rift_staff", props -> new RiftWeaponItem(3, 6.0, -2.4, props.rarity(Rarity.EPIC).fireResistant().stacksTo(1)));

    // 3. Rift Zırh Seti (Kask, Göğüslük, Pantolon, Bot)
    public static final Item RIFT_HELMET = register("rift_helmet", props -> new RiftArmorItem(EquipmentSlot.HEAD, 4.0, 3.5, "head", props.rarity(Rarity.EPIC).fireResistant().stacksTo(1)));
    public static final Item RIFT_CHESTPLATE = register("rift_chestplate", props -> new RiftArmorItem(EquipmentSlot.CHEST, 9.0, 3.5, "chest", props.rarity(Rarity.EPIC).fireResistant().stacksTo(1)));
    public static final Item RIFT_LEGGINGS = register("rift_leggings", props -> new RiftArmorItem(EquipmentSlot.LEGS, 7.0, 3.5, "legs", props.rarity(Rarity.EPIC).fireResistant().stacksTo(1)));
    public static final Item RIFT_BOOTS = register("rift_boots", props -> new RiftArmorItem(EquipmentSlot.FEET, 4.0, 3.5, "feet", props.rarity(Rarity.EPIC).fireResistant().stacksTo(1)));

    public static Item register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, RiftboundMod.id(name));
        Item.Properties props = new Item.Properties().setId(key);
        Item item = factory.apply(props);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void register() {
        RiftboundMod.LOGGER.info("Registering Riftbound Weapons & Armor Set...");
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.accept(RIFT_CORE);
            entries.accept(RIFT_BLADE);
            entries.accept(RIFT_SCYTHE);
            entries.accept(RIFT_STAFF);
            entries.accept(RIFT_HELMET);
            entries.accept(RIFT_CHESTPLATE);
            entries.accept(RIFT_LEGGINGS);
            entries.accept(RIFT_BOOTS);
        });

        // Tam Zırh Seti Giymiş Oyunculara Pasif Güçlendirme (Set Bonusu)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.getItemBySlot(EquipmentSlot.HEAD).is(RIFT_HELMET) &&
                    player.getItemBySlot(EquipmentSlot.CHEST).is(RIFT_CHESTPLATE) &&
                    player.getItemBySlot(EquipmentSlot.LEGS).is(RIFT_LEGGINGS) &&
                    player.getItemBySlot(EquipmentSlot.FEET).is(RIFT_BOOTS)) {
                    
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false, true));
                    if (player.level() instanceof ServerLevel sLevel) {
                        sLevel.sendParticles(AbilityManager.RIFT_PURPLE, player.getX(), player.getY() + 0.5, player.getZ(), 3, 0.3, 0.4, 0.3, 0.01);
                    }
                }
            }
        });
    }
}