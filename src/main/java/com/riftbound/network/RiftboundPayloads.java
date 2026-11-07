package com.riftbound.network;

import com.riftbound.ability.AbilityManager;
import com.riftbound.progression.PlayerRiftData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class RiftboundPayloads {
    public static void register() {
        PayloadTypeRegistry.playS2C().register(SyncProgressionPayload.TYPE, SyncProgressionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UseAbilityPayload.TYPE, UseAbilityPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(UseAbilityPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                AbilityManager.useAbility(player, payload.abilityId());
            });
        });
    }

    public static void syncToClient(ServerPlayer player, PlayerRiftData data) {
        ServerPlayNetworking.send(player, new SyncProgressionPayload(
            data.getUnlockedTier() > 0,
            data.getUnlockedTier(),
            data.getDashCooldown(),
            data.getPulseCooldown(),
            data.getShieldCooldown(),
            data.getShieldActiveTicks()
        ));
    }

    public record SyncProgressionPayload(boolean unlocked, int unlockedTier, int dashCooldown, int pulseCooldown, int shieldCooldown, int shieldActiveTicks) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SyncProgressionPayload> TYPE = 
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("riftbound", "sync_progression"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncProgressionPayload> CODEC = 
            StreamCodec.of((buf, value) -> {
                buf.writeBoolean(value.unlocked);
                buf.writeInt(value.unlockedTier);
                buf.writeInt(value.dashCooldown);
                buf.writeInt(value.pulseCooldown);
                buf.writeInt(value.shieldCooldown);
                buf.writeInt(value.shieldActiveTicks);
            }, buf -> new SyncProgressionPayload(
                buf.readBoolean(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()
            ));

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record UseAbilityPayload(int abilityId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<UseAbilityPayload> TYPE = 
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("riftbound", "use_ability"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UseAbilityPayload> CODEC = 
            StreamCodec.of((buf, value) -> buf.writeInt(value.abilityId), buf -> new UseAbilityPayload(buf.readInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}