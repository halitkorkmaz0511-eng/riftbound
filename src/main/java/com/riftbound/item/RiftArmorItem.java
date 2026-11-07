package com.riftbound.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;

public class RiftArmorItem extends Item {
    public static final ResourceKey<EquipmentAsset> RIFT_ARMOR_ASSET = ResourceKey.create(
        EquipmentAssets.ROOT_ID,
        ResourceLocation.fromNamespaceAndPath("riftbound", "rift")
    );

    private final EquipmentSlot slot;

    public RiftArmorItem(EquipmentSlot slot, double armor, double toughness, String slotName, Properties properties) {
        super(properties
            .attributes(createArmorAttributes(slot, armor, toughness, slotName))
            .component(DataComponents.EQUIPPABLE, Equippable.builder(slot)
                .setEquipSound(SoundEvents.ARMOR_EQUIP_NETHERITE)
                .setAsset(RIFT_ARMOR_ASSET)
                .build()
            )
        );
        this.slot = slot;
    }

    private static ItemAttributeModifiers createArmorAttributes(EquipmentSlot slot, double armor, double toughness, String slotName) {
        EquipmentSlotGroup group = switch (slot) {
            case HEAD -> EquipmentSlotGroup.HEAD;
            case CHEST -> EquipmentSlotGroup.CHEST;
            case LEGS -> EquipmentSlotGroup.LEGS;
            case FEET -> EquipmentSlotGroup.FEET;
            default -> EquipmentSlotGroup.ARMOR;
        };
        return ItemAttributeModifiers.builder()
            .add(Attributes.ARMOR, new AttributeModifier(ResourceLocation.fromNamespaceAndPath("riftbound", "armor_" + slotName), armor, AttributeModifier.Operation.ADD_VALUE), group)
            .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(ResourceLocation.fromNamespaceAndPath("riftbound", "toughness_" + slotName), toughness, AttributeModifier.Operation.ADD_VALUE), group)
            .add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(ResourceLocation.fromNamespaceAndPath("riftbound", "kb_" + slotName), 0.15, AttributeModifier.Operation.ADD_VALUE), group)
            .build();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        ItemStack equipped = player.getItemBySlot(this.slot);
        if (equipped.isEmpty()) {
            player.setItemSlot(this.slot, held.copy());
            held.setCount(0);
            player.playSound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }
}