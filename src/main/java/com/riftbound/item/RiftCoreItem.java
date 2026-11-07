package com.riftbound.item;

import com.riftbound.ability.AbilityManager;
import com.riftbound.network.RiftboundPayloads;
import com.riftbound.progression.PlayerRiftData;
import com.riftbound.progression.RiftProgressionState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RiftCoreItem extends Item {
    public RiftCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            RiftProgressionState state = RiftProgressionState.getServerState(serverPlayer.server);
            PlayerRiftData data = state.getPlayerData(serverPlayer.getUUID());

            if (data.unlockedTier >= 3) {
                serverPlayer.displayClientMessage(Component.literal("§dYou have already mastered all 3 Riftbound abilities!"), true);
                return InteractionResult.FAIL;
            }

            data.unlockedTier++;
            state.setDirty();
            RiftboundPayloads.syncToClient(serverPlayer, data);

            String unlockedName = switch (data.unlockedTier) {
                case 1 -> "§bRift Dash §7(Press [R])";
                case 2 -> "§dVoid Pulse §7(Press [G])";
                case 3 -> "§5Rift Shield §7(Press [V])";
                default -> "Unknown";
            };

            serverPlayer.sendSystemMessage(Component.literal("§5✦ [RIFT AWAKENED] §fYou absorbed the Rift Core and unlocked: " + unlockedName));
            ServerLevel sLevel = serverPlayer.serverLevel();
            sLevel.playSound(null, serverPlayer.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.1f);
            sLevel.sendParticles(ParticleTypes.END_ROD, serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 40, 0.5, 0.8, 0.5, 0.15);
            sLevel.sendParticles(AbilityManager.RIFT_CYAN, serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 30, 0.5, 0.8, 0.5, 0.05);

            stack.consume(1, serverPlayer);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }
}
