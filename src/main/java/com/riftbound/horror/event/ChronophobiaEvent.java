package com.riftbound.horror.event;

import com.riftbound.horror.HorrorState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public class ChronophobiaEvent extends HorrorEvent {
    public ChronophobiaEvent() {
        super("riftbound:chronophobia", 1800); // 90 Saniye sürer
    }

    @Override
    public boolean canTrigger(ServerLevel level, ServerPlayer player) { return true; }

    @Override
    public void cleanup(ServerLevel level, ServerPlayer player) {}

    @Override
    public boolean tick(ServerLevel level, ServerPlayer player, HorrorState state, long age) {
        // Zamanı sonsuz gece yarısına (18000) kilitle
        level.setDayTime(18000);

        // Oyuncu yatağa yatmaya çalışırsa anında kaldır ve korkunç mesajı ver
        if (player.isSleeping()) {
            player.stopSleeping();
            player.sendSystemMessage(Component.literal("§c§lŞu an uyuyamazsın, odada yalnız değilsin."));
        }
        return false;
    }
}