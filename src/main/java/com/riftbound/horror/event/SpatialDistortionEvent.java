package com.riftbound.horror.event;

import com.riftbound.horror.HorrorState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpatialDistortionEvent extends HorrorEvent {
    private final Map<UUID, Vec3> startPositions = new HashMap<>();

    public SpatialDistortionEvent() {
        super("riftbound:spatial_distortion", 600); // 30 saniye boyunca döngüde kalır
    }

    @Override
    public boolean canTrigger(ServerLevel level, ServerPlayer player) {
        return true; 
    }

    @Override
    public void cleanup(ServerLevel level, ServerPlayer player) {
        startPositions.remove(player.getUUID());
    }

    @Override
    public boolean tick(ServerLevel level, ServerPlayer player, HorrorState state, long age) {
        UUID uuid = player.getUUID();
        
        if (age == 1) {
            startPositions.put(uuid, player.position());
            player.sendSystemMessage(Component.literal("§8§oHava aniden ağırlaştı..."));
            
            // Korkutucu ambiyans sesi
            Object soundObj = SoundEvents.AMBIENT_CAVE;
            net.minecraft.sounds.SoundEvent sound = null;
            if (soundObj instanceof net.minecraft.core.Holder) {
                sound = (net.minecraft.sounds.SoundEvent) ((net.minecraft.core.Holder<?>) soundObj).value();
            } else if (soundObj instanceof net.minecraft.sounds.SoundEvent) {
                sound = (net.minecraft.sounds.SoundEvent) soundObj;
            }
            if (sound != null) {
                level.playSound(null, player.blockPosition(), sound, SoundSource.AMBIENT, 1.0f, 0.5f);
            }
        }

        if (!startPositions.containsKey(uuid)) return true;

        Vec3 startPos = startPositions.get(uuid);
        double distance = player.position().distanceTo(startPos);

        // Oyuncu 10 blok ileri gittiği an hissettirmeden başladığı yere geri ışınla!
        if (distance > 10.0) {
            player.connection.teleport(startPos.x, startPos.y, startPos.z, player.getYRot(), player.getXRot());
            
            // Kafasını karıştırmak için her başa döndüğünde hafif bir fısıltı
            Object whisperObj = SoundEvents.ENDERMAN_STARE; 
            net.minecraft.sounds.SoundEvent whisper = null;
            if (whisperObj instanceof net.minecraft.core.Holder) {
                whisper = (net.minecraft.sounds.SoundEvent) ((net.minecraft.core.Holder<?>) whisperObj).value();
            } else if (whisperObj instanceof net.minecraft.sounds.SoundEvent) {
                whisper = (net.minecraft.sounds.SoundEvent) whisperObj;
            }
            if (whisper != null) {
                level.playSound(null, player.blockPosition(), whisper, SoundSource.AMBIENT, 0.2f, 0.1f);
            }
        }

        return false;
    }
}