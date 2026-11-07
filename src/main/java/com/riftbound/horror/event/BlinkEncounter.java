package com.riftbound.horror.event;
import com.riftbound.horror.HorrorState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class BlinkEncounter extends HorrorEvent {
    public BlinkEncounter() { super("riftbound:blink", 8); }

    @Override
    public boolean canTrigger(ServerLevel level, ServerPlayer player) { return true; }

    @Override
    public boolean tick(ServerLevel level, ServerPlayer player, HorrorState state, long age) {
        if (age == 1) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
            playSafeSound(level, player, "entity.enderman.scream", 1.5f, 0.3f);
        }
        
        if (age >= 40) {
            return true; // Kisa olay 2 saniyede biter
        }
        return false;
    }
}