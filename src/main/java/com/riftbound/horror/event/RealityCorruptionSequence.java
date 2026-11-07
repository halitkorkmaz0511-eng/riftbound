package com.riftbound.horror.event;

import com.riftbound.horror.HorrorState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public class RealityCorruptionSequence extends HorrorEvent {
    
    private LivingEntity spawnedStalker = null;

    public RealityCorruptionSequence() { super("riftbound:reality_corruption", 5); }

    @Override
    public boolean canTrigger(ServerLevel level, ServerPlayer player) { return true; }

    @Override
    public void cleanup(ServerLevel level, ServerPlayer player) {
        player.removeEffect(MobEffects.DARKNESS);
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.LEVITATION);
        player.removeEffect(MobEffects.CONFUSION); 
        if (spawnedStalker != null && spawnedStalker.isAlive()) {
            spawnedStalker.discard();
            spawnedStalker = null;
        }
    }

    @Override
    public boolean tick(ServerLevel level, ServerPlayer player, HorrorState state, long age) {
        long seed = state.sequenceStartTick.getOrDefault(player.getUUID(), 0L);
        int variation = state.forcedVariation.containsKey(player.getUUID()) ? state.forcedVariation.get(player.getUUID()) : (int) (Math.abs(seed) % 4);

        if (age == 1) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 400, 0, false, false));
            playSafeSound(level, player, "ambient.cave", 2.0f, 0.1f);
        }

        if (age > 1 && age < 300 && age % 10 == 0) {
            level.sendParticles(ParticleTypes.ASH, player.getX(), player.getY() + 3, player.getZ(), 80, 8.0, 4.0, 8.0, 0.05);
        }

        // ==========================================
        // VARYASYON 0: AGRESİF JUMPSCARE (12 Saniyelik Hızlı Şok)
        // ==========================================
        if (variation == 0) {
            if (age > 20 && age < 160 && age % 20 == 0) playSafeSound(level, player, "entity.warden.heartbeat", 2.0f, 0.5f);
            if (age == 160) {
                spawnedStalker = spawnStalker(level, player, 2.0, 0.0, "rift_aggressor");
                if (spawnedStalker != null) playSafeSound(level, player, "entity.warden.agitated", 3.0f, 0.8f);
            }
            if (age == 170) {
                if (player.isAlive()) {
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
                    playSafeSound(level, player, "entity.enderman.scream", 3.0f, 0.1f);
                    if (spawnedStalker != null) { spawnedStalker.discard(); spawnedStalker = null; }
                }
            }
            if (age >= 220) { cleanup(level, player); return true; }
        }

        // ==========================================
        // VARYASYON 1: SİNSİ İZLEYİCİ
        // ==========================================
        else if (variation == 1) {
            if (age > 20 && age < 160 && age % 25 == 0) playSafeSound(level, player, "entity.warden.heartbeat", 1.5f, 0.5f);
            if (age == 160) {
                double dist = 10.0 + level.random.nextDouble() * 5.0; 
                double angle = (level.random.nextBoolean() ? 1 : -1) * 0.7;
                spawnedStalker = spawnStalker(level, player, dist, angle, "rift_watcher");
                playSafeSound(level, player, "ambient.cave", 2.0f, 0.1f);
            }
            if (age > 160 && age < 260) {
                if (spawnedStalker != null && spawnedStalker.isAlive()) {
                    Vec3 toStalker = spawnedStalker.position().subtract(player.position()).normalize();
                    if (player.getLookAngle().dot(toStalker) > 0.85 || age == 250) {
                        level.sendParticles(ParticleTypes.LARGE_SMOKE, spawnedStalker.getX(), spawnedStalker.getY() + 1.5, spawnedStalker.getZ(), 30, 0.5, 1.0, 0.5, 0.05);
                        if (age != 250) playSafeSound(level, player, "entity.phantom.flap", 2.0f, 0.5f);
                        spawnedStalker.discard();
                        spawnedStalker = null;
                    }
                }
            }
            if (age >= 300) { cleanup(level, player); return true; }
        }

        // ==========================================
        // VARYASYON 2: POLTERGEIST / PARANOYA
        // ==========================================
        else if (variation == 2) {
            if (age == 40) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 1, false, false));
            if (age > 60 && age < 240) {
                if (age % 30 == 0) playSafeSound(level, player, "entity.warden.heartbeat", 1.0f, 0.5f);
                if (age % 45 == 0) playSafeSound(level, player, "entity.player.step", 1.5f, 0.8f); 
                if (age % 80 == 0) playSafeSound(level, player, "entity.player.breath", 2.0f, 0.1f);
            }
            if (age == 260) {
                Vec3 back = player.getLookAngle().scale(-1.5);
                BlockPos markPos = BlockPos.containing(player.getX() + back.x, player.getY(), player.getZ() + back.z);
                if (level.isEmptyBlock(markPos)) {
                    Block groundBlock = level.getBlockState(markPos.below()).getBlock();
                    if (groundBlock == Blocks.GRASS_BLOCK || groundBlock == Blocks.DIRT || groundBlock == Blocks.STONE || groundBlock == Blocks.DEEPSLATE || groundBlock == Blocks.SAND) {
                        level.setBlock(markPos, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
                        level.sendParticles(ParticleTypes.REVERSE_PORTAL, markPos.getX()+0.5, markPos.getY()+1, markPos.getZ()+0.5, 60, 0.5, 0.5, 0.5, 0.05);
                        playSafeSound(level, player, "block.respawn_anchor.set_spawn", 1.0f, 0.5f);
                    }
                }
            }
            if (age >= 400) { cleanup(level, player); return true; }
        }

        // ==========================================
        // VARYASYON 3: ANOMALİ & 4. DUVAR (Uzun Psikolojik Çöküş)
        // ==========================================
        else if (variation == 3) {
            // Çıldırmış, giderek hızlanan kalp atışları
            if (age == 40 || age == 80 || age == 110 || age == 130 || age == 145 || age == 155) {
                playSafeSound(level, player, "entity.warden.heartbeat", 2.0f, 0.5f);
            }
            
            // 8. Saniye (160. Tick) - Yükseliş ve Gerçekliğin Kopuşu
            if (age == 160) {
                // 5 saniye (100 tick) boyunca yavaşça havaya kalkar
                player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 0, false, false));
                // 10 saniye (200 tick) mide bulantısı
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, false)); 
                // 7 saniye (140 tick) kör edici karanlık
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0, false, false));
                
                playSafeSound(level, player, "ambient.cave", 3.0f, 0.1f); // Çok derin bir uğultu
                playSafeSound(level, player, "entity.illusioner.prepare_blindness", 1.5f, 0.5f);
            }

            // Havada asılıyken etrafında hiçlik hissi veren partiküller ve yankılı, yavaş sesler
            if (age > 160 && age < 260 && age % 20 == 0) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 40, 1.0, 1.0, 1.0, 0.01);
                playSafeSound(level, player, "block.amethyst_block.chime", 2.0f, 0.1f); // Çarpık, yavaş zil sesi
            }

            // 11. Saniye (220. Tick) - Zihne Kazınan Mesaj
            if (age == 220) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false)); // Mesajla birlikte tam körlük
                player.displayClientMessage(Component.literal("§4§k|||§r §c... uyan ...§r §4§k|||§r"), true);
                playSafeSound(level, player, "entity.enderman.stare", 1.0f, 0.5f); // Rahatsız edici statik ses
            }

            // 13. Saniye (260. Tick) - Düşüş ve İllüzyonun Bitmesi
            if (age == 260) { 
                player.removeEffect(MobEffects.LEVITATION); 
                playSafeSound(level, player, "entity.player.breath", 2.0f, 1.5f); // Panikle uyanmış gibi derin bir nefes
            }

            // Yere düşme hissiyatı
            if (age == 275) {
                playSafeSound(level, player, "entity.warden.heartbeat", 3.0f, 0.1f); 
            }

            if (age >= 360) { cleanup(level, player); return true; }
        }

        return false;
    }

    private LivingEntity spawnStalker(ServerLevel level, ServerPlayer player, double distance, double angleOffsetRadians, String tagLabel) {
        Vec3 look = player.getLookAngle();
        double cos = Math.cos(angleOffsetRadians);
        double sin = Math.sin(angleOffsetRadians);
        
        double rx = look.x * cos - look.z * sin;
        double rz = look.x * sin + look.z * cos;
        
        Vec3 offsetVec = new Vec3(rx, 0, rz).normalize().scale(distance);
        BlockPos spawnPos = BlockPos.containing(player.getX() + offsetVec.x, player.getY(), player.getZ() + offsetVec.z);

        int tries = 0;
        while (!level.isEmptyBlock(spawnPos) && tries < 5) { spawnPos = spawnPos.above(); tries++; }
        tries = 0;
        while (level.isEmptyBlock(spawnPos.below()) && tries < 5) { spawnPos = spawnPos.below(); tries++; }

        if (!level.isEmptyBlock(spawnPos)) {
            if (distance > 5.0) {
                return null; 
            } else {
                spawnPos = BlockPos.containing(player.getX() + offsetVec.x, player.getY() + 1, player.getZ() + offsetVec.z);
            }
        }

        Optional<EntityType<?>> optionalType = BuiltInRegistries.ENTITY_TYPE.getOptional(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("riftbound", "rift_sentinel"));
        if (optionalType.isPresent()) {
            net.minecraft.world.entity.Entity stalker = optionalType.get().create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (stalker instanceof LivingEntity living) {
                living.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                
                double dX = player.getX() - living.getX();
                double dZ = player.getZ() - living.getZ();
                float yaw = (float)(Math.atan2(dZ, dX) * (180D / Math.PI)) - 90.0F;
                living.setYRot(yaw);
                living.yHeadRot = yaw;
                living.yBodyRot = yaw;

                CompoundTag tag = new CompoundTag();
                living.saveWithoutId(tag);
                tag.putBoolean("NoAI", true);
                tag.putBoolean("Silent", true);
                tag.putBoolean("Invulnerable", true);
                living.load(tag);
                
                living.addTag(tagLabel);
                level.addFreshEntity(living);
                return living;
            }
        }
        return null;
    }
}