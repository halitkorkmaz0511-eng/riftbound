package com.riftbound.horror.event;

import com.riftbound.horror.HorrorState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class DoppelgangerEvent extends HorrorEvent {
    public DoppelgangerEvent() {
        super("riftbound:doppelganger", 200);
    }
    @Override
    public boolean canTrigger(ServerLevel level, ServerPlayer player) { return true; }
    @Override
    public void cleanup(ServerLevel level, ServerPlayer player) {}
    @Override
    public boolean tick(ServerLevel level, ServerPlayer player, HorrorState state, long age) {
        if (age == 1) {
            player.sendSystemMessage(Component.literal("§e[Bilinmeyen] oyuna katıldı"));
        }
        if (age == 80) {
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.AMBIENT, 1.0f, 1.0f);
        }
        if (age == 160) {
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.AMBIENT, 1.0f, 1.0f);
        }
        return age >= 200;
    }
}