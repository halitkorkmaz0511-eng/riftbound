package com.riftbound.ability;

import com.riftbound.network.RiftboundPayloads;
import com.riftbound.progression.PlayerRiftData;
import com.riftbound.progression.RiftProgressionState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class AbilityManager {
    public static final DustParticleOptions RIFT_CYAN = new DustParticleOptions(0x00E5FF, 1.2f);
    public static final DustParticleOptions RIFT_PURPLE = new DustParticleOptions(0xB219FF, 1.2f);

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            RiftProgressionState state = RiftProgressionState.getServerState(server);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PlayerRiftData data = state.getPlayerData(player.getUUID());
                boolean changed = false;

                if (data.getDashCooldown() > 0) { data.setDashCooldown(data.getDashCooldown() - 1); changed = true; }
                if (data.getPulseCooldown() > 0) { data.setPulseCooldown(data.getPulseCooldown() - 1); changed = true; }
                if (data.getShieldCooldown() > 0) { data.setShieldCooldown(data.getShieldCooldown() - 1); changed = true; }
                if (data.getShieldActiveTicks() > 0) {
                    data.setShieldActiveTicks(data.getShieldActiveTicks() - 1);
                    if (player.level() instanceof ServerLevel sLevel && server.getTickCount() % 4 == 0) {
                        sLevel.sendParticles(RIFT_CYAN, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.5, 0.6, 0.5, 0.02);
                    }
                    changed = true;
                }

                if (changed && server.getTickCount() % 5 == 0) {
                    RiftboundPayloads.syncToClient(player, data);
                }
            }
        });
    }

    public static void useAbility(ServerPlayer player, int abilityId) {
        if (!(player.level() instanceof ServerLevel sLevel)) return;
        RiftProgressionState state = RiftProgressionState.getServerState(player.server);
        PlayerRiftData data = state.getPlayerData(player.getUUID());

        if (data.getUnlockedTier() < abilityId) return;

        if (abilityId == 1 && data.getDashCooldown() <= 0) {
            // Yetenek 1: Rift Dash (İleri Atılma)
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(look.x * 1.8, Math.max(look.y * 1.2, 0.35), look.z * 1.8);
            player.hurtMarked = true;
            sLevel.sendParticles(RIFT_CYAN, player.getX(), player.getY() + 1.0, player.getZ(), 35, 0.4, 0.5, 0.4, 0.08);
            sLevel.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.2f);
            data.setDashCooldown(60); // 3 saniye
            RiftboundPayloads.syncToClient(player, data);
        } 
        else if (abilityId == 2 && data.getPulseCooldown() <= 0) {
            // Yetenek 2: Rift Pulse (Alan Hasarı ve Savurma)
            sLevel.sendParticles(RIFT_PURPLE, player.getX(), player.getY() + 1.0, player.getZ(), 60, 2.0, 0.5, 2.0, 0.1);
            sLevel.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.4f);
            AABB box = player.getBoundingBox().inflate(5.0);
            List<LivingEntity> targets = sLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player);
            for (LivingEntity target : targets) {
                target.hurt(sLevel.damageSources().playerAttack(player), 10.0f);
                Vec3 push = target.position().subtract(player.position()).normalize().scale(1.4);
                target.setDeltaMovement(push.x, 0.45, push.z);
                target.hurtMarked = true;
            }
            data.setPulseCooldown(120); // 6 saniye
            RiftboundPayloads.syncToClient(player, data);
        } 
        else if (abilityId == 3 && data.getShieldCooldown() <= 0) {
            // Yetenek 3: Rift Shield (Koruma Kalkanı)
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 2));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            sLevel.sendParticles(RIFT_CYAN, player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.7, 0.9, 0.7, 0.05);
            sLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.3f);
            data.setShieldActiveTicks(100); // 5 saniye aktif
            data.setShieldCooldown(240); // 12 saniye bekleme
            RiftboundPayloads.syncToClient(player, data);
        }
    }
}