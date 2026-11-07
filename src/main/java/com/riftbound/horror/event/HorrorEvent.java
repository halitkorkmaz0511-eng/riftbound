package com.riftbound.horror.event;
import com.riftbound.horror.HorrorState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Optional;

public abstract class HorrorEvent {
    private final String id;
    private final int weight;

    public HorrorEvent(String id, int weight) {
        this.id = id;
        this.weight = weight;
    }

    public String getId() { return id; }
    public int getWeight() { return weight; }

    public abstract boolean canTrigger(ServerLevel level, ServerPlayer player);
    public abstract boolean tick(ServerLevel level, ServerPlayer player, HorrorState state, long ageInTicks);
    public void cleanup(ServerLevel level, ServerPlayer player) {}

    protected void playSafeSound(ServerLevel level, ServerPlayer player, String soundId, float vol, float pitch) {
        Optional<SoundEvent> opt = BuiltInRegistries.SOUND_EVENT.getOptional(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", soundId));
        if (opt.isPresent()) {
            SoundEvent se = opt.get();
            level.playSound(null, player.blockPosition(), se, SoundSource.HOSTILE, vol, pitch);
        }
    }
}