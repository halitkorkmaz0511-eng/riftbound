package com.riftbound.horror.event;

import com.riftbound.horror.HorrorState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import java.util.Random;

public class TheDoppelgangerEvent extends HorrorEvent {
    private final Random random = new Random();

    public TheDoppelgangerEvent() {
        super("riftbound:doppelganger", 1200); // 60 Saniye sürer
    }

    @Override
    public boolean canTrigger(ServerLevel level, ServerPlayer player) { return true; }

    @Override
    public void cleanup(ServerLevel level, ServerPlayer player) {}

    @Override
    public boolean tick(ServerLevel level, ServerPlayer player, HorrorState state, long age) {
        // 1. Saniyede sahte katılım mesajı
        if (age == 20) {
            player.sendSystemMessage(Component.literal("§e[Bilinmeyen] oyuna katildi"));
        }

        // Rastgele aralıklarla arkadan gelen sahte oyuncu sesleri (Yemek, Kazı, Hasar)
        if (age > 100 && age % 50 == 0) {
            if (random.nextFloat() < 0.3f) {
                playFakeSound(level, player, SoundEvents.GENERIC_EAT);
            } else if (random.nextFloat() < 0.6f) {
                playFakeSound(level, player, SoundEvents.STONE_BREAK);
            } else if (random.nextFloat() < 0.8f) {
                playFakeSound(level, player, SoundEvents.PLAYER_HURT);
            }
        }
        return false;
    }

    private void playFakeSound(ServerLevel level, ServerPlayer player, Object soundObj) {
        Vec3 look = player.getLookAngle();
        Vec3 behind = player.position().subtract(look.scale(3.0));
        BlockPos pos = BlockPos.containing(behind.x, player.getY(), behind.z);
        
        net.minecraft.sounds.SoundEvent sound = null;
        if (soundObj instanceof net.minecraft.core.Holder) {
            sound = (net.minecraft.sounds.SoundEvent) ((net.minecraft.core.Holder<?>) soundObj).value();
        } else if (soundObj instanceof net.minecraft.sounds.SoundEvent) {
            sound = (net.minecraft.sounds.SoundEvent) soundObj;
        }
        
        if (sound != null) {
            level.playSound(null, pos, sound, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }
}