package com.riftbound.horror.event;

import com.riftbound.horror.HorrorState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public class ChronophobiaEvent extends HorrorEvent {
    public ChronophobiaEvent() {
        super("riftbound:chronophobia", 600);
    }
    @Override
    public boolean canTrigger(ServerLevel level, ServerPlayer player) { return true; }
    @Override
    public void cleanup(ServerLevel level, ServerPlayer player) {}
    @Override
    public boolean tick(ServerLevel level, ServerPlayer player, HorrorState state, long age) {
        level.setDayTime(18000);
        if (age == 1) {
             player.sendSystemMessage(Component.literal("§cZaman durdu... Odada yalnız değilsin."));
        }
        return age >= 600;
    }
}