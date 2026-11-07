package com.riftbound.item;

import com.riftbound.ability.AbilityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class RiftWeaponItem extends Item {
    private final int weaponType; // 1: Blade, 2: Scythe, 3: Staff

    public RiftWeaponItem(int weaponType, double attackDamage, double attackSpeed, Properties properties) {
        super(properties.attributes(createAttributes(attackDamage, attackSpeed)));
        this.weaponType = weaponType;
    }

    private static ItemAttributeModifiers createAttributes(double baseDamage, double baseSpeed) {
        return ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(ResourceLocation.fromNamespaceAndPath("riftbound", "weapon_damage"), baseDamage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.ATTACK_SPEED, new AttributeModifier(ResourceLocation.fromNamespaceAndPath("riftbound", "weapon_speed"), baseSpeed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .build();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel sLevel) {
            if (weaponType == 1) {
                // Rift Blade: Hızlı kesik ve ekstra büyü/boyut hasarı + Cyan parçacık
                sLevel.sendParticles(AbilityManager.RIFT_CYAN, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.3, 0.4, 0.3, 0.05);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            } else if (weaponType == 2) {
                // Rift Scythe: Geniş alan (AoE) tırpan darbesi ve Can Çalma (Lifesteal)
                sLevel.sendParticles(AbilityManager.RIFT_PURPLE, target.getX(), target.getY() + 1.0, target.getZ(), 35, 1.2, 0.4, 1.2, 0.05);
                attacker.heal(2.5f);
                AABB area = target.getBoundingBox().inflate(2.5);
                for (LivingEntity nearby : sLevel.getEntitiesOfClass(LivingEntity.class, area, e -> e != attacker && e != target)) {
                    nearby.hurt(sLevel.damageSources().magic(), 6.0f);
                }
            }
        }
        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        if (level instanceof ServerLevel sLevel) {
            if (weaponType == 1) {
                // Rift Blade Sağ Tık: Kısa mesafe gölge adımı (Blink)
                Vec3 look = player.getLookAngle();
                player.setDeltaMovement(look.x * 1.5, 0.3, look.z * 1.5);
                player.hurtMarked = true;
                sLevel.sendParticles(AbilityManager.RIFT_CYAN, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.4, 0.4, 0.4, 0.05);
                sLevel.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.5f);
                player.getCooldowns().addCooldown(stack, 40);
            } else if (weaponType == 2) {
                // Rift Scythe Sağ Tık: Yakındaki düşmanları kendine doğru çeken boyutsal girdap
                sLevel.sendParticles(AbilityManager.RIFT_PURPLE, player.getX(), player.getY() + 1.0, player.getZ(), 50, 2.5, 0.5, 2.5, 0.08);
                sLevel.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.0f, 0.8f);
                AABB pullBox = player.getBoundingBox().inflate(6.0);
                for (LivingEntity e : sLevel.getEntitiesOfClass(LivingEntity.class, pullBox, ent -> ent != player)) {
                    Vec3 pull = player.position().subtract(e.position()).normalize().scale(1.1);
                    e.setDeltaMovement(pull.x, 0.35, pull.z);
                    e.hurtMarked = true;
                }
                player.getCooldowns().addCooldown(stack, 80);
            } else if (weaponType == 3) {
                // Rift Staff Sağ Tık: İleriye doğru 12 blokluk boyutsal ışın (Rift Beam)
                Vec3 start = player.getEyePosition();
                Vec3 look = player.getLookAngle();
                sLevel.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.7f, 1.6f);
                for (int i = 1; i <= 14; i++) {
                    Vec3 point = start.add(look.scale(i));
                    sLevel.sendParticles(AbilityManager.RIFT_CYAN, point.x, point.y, point.z, 4, 0.1, 0.1, 0.1, 0.01);
                    sLevel.sendParticles(AbilityManager.RIFT_PURPLE, point.x, point.y, point.z, 4, 0.15, 0.15, 0.15, 0.02);
                    AABB hitBox = new AABB(point.x - 1.0, point.y - 1.0, point.z - 1.0, point.x + 1.0, point.y + 1.0, point.z + 1.0);
                    List<LivingEntity> hits = sLevel.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != player);
                    for (LivingEntity hit : hits) {
                        hit.hurt(sLevel.damageSources().playerAttack(player), 12.0f);
                    }
                }
                player.getCooldowns().addCooldown(stack, 50);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }
}