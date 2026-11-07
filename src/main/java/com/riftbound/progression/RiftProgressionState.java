package com.riftbound.progression;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RiftProgressionState {
    private static final RiftProgressionState INSTANCE = new RiftProgressionState();
    private final Map<UUID, PlayerRiftData> playerDataMap = new HashMap<>();

    public static RiftProgressionState getServerState(MinecraftServer server) {
        return INSTANCE;
    }

    public static RiftProgressionState getServerState(ServerLevel level) {
        return INSTANCE;
    }

    public PlayerRiftData getPlayerData(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, k -> new PlayerRiftData());
    }

    public PlayerRiftData getPlayerData(ServerPlayer player) {
        return getPlayerData(player.getUUID());
    }

    public void setDirty() {
        // Güvenli bellek içi kayıt
    }
}