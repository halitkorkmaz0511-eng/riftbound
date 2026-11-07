package com.riftbound.item;

import com.riftbound.entity.ModEntities;
import com.riftbound.entity.boss.RiftSentinelEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class RiftCallerItem extends Item {
    public RiftCallerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            BlockPos pos = context.getClickedPos().above();
            RiftSentinelEntity sentinel = new RiftSentinelEntity(ModEntities.RIFT_SENTINEL, serverLevel);
            sentinel.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, context.getRotation(), 0.0f);
            sentinel.triggerSpawnSequence();
            serverLevel.addFreshEntity(sentinel);

            serverLevel.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.2f, 0.75f);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.literal("§5The dimensional boundary shatters... The Rift Sentinel awakens!"), false);
                context.getItemInHand().consume(1, context.getPlayer());
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }
}
