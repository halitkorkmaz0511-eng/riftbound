package com.riftbound.horror.event;

import com.riftbound.horror.HorrorState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class TheUninvitedEvent extends HorrorEvent {
    
    private WitherSkeleton stalker = null;
    private BlockPos targetPos = null;
    private List<BlockPos> fireBlocks = new ArrayList<>();
    
    private boolean wasLooking = true;
    private boolean isJumpscaring = false;
    private long jumpscareTick = 0;
    
    // MÜCADELE (QTE) DEĞİŞKENLERİ
    private int catchCount = 0;
    private boolean isStruggling = false;
    private long struggleStartTick = 0;
    private boolean prevSneaking = false;
    private int struggleProgress = 0;

    private final String[] whispers = {
        "§8§o... neden kaçıyorsun? ...",
        "§8§o... arkana bakma ...",
        "§8§o... çok yavaşsın ..."
    };

    public TheUninvitedEvent() { 
        super("riftbound:the_uninvited", 120); 
    }

    @Override
    public boolean canTrigger(ServerLevel level, ServerPlayer player) { return true; }

    @Override
    public void cleanup(ServerLevel level, ServerPlayer player) {
        if (stalker != null && stalker.isAlive()) { stalker.discard(); stalker = null; }
        for (BlockPos pos : fireBlocks) {
            if (level.getBlockState(pos).is(Blocks.SOUL_FIRE)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        fireBlocks.clear();
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
    }

    @Override
    public boolean tick(ServerLevel level, ServerPlayer player, HorrorState state, long age) {
        if (age == 1) {
            stalker = null; targetPos = null; fireBlocks.clear();
            wasLooking = true; isJumpscaring = false;
            catchCount = 0; isStruggling = false; struggleProgress = 0;
            
            player.sendSystemMessage(Component.literal("§7§o[Sistem] Kusursuz Kovalamaca Modu Aktif..."));
            
            Vec3 look = player.getLookAngle();
            for(int d = 15; d <= 30; d+=3) {
                double angleOffset = (level.random.nextDouble() - 0.5) * 0.8; 
                double rx = look.x * Math.cos(angleOffset) - look.z * Math.sin(angleOffset);
                double rz = look.x * Math.sin(angleOffset) + look.z * Math.cos(angleOffset);
                BlockPos testPos = findGround(level, BlockPos.containing(player.getX() + rx * d, player.getY(), player.getZ() + rz * d));
                if (testPos != null && isUnderTree(level, testPos)) { targetPos = testPos; break; }
            }
            if (targetPos == null) targetPos = findGround(level, BlockPos.containing(player.getX() + look.x * 22, player.getY(), player.getZ() + look.z * 22));

            if (targetPos != null) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, false));
                level.playSound(null, player.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 3.0f, 0.1f);
            } else return true; 
        }

        if (age > 1 && age < 60 && age % 20 == 0) level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 2.0f, 0.5f + (age / 60.0f));

        if (age == 60 && targetPos != null) {
            player.removeEffect(MobEffects.BLINDNESS);
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (lightning != null) {
                lightning.moveTo(Vec3.atBottomCenterOf(targetPos));
                lightning.setVisualOnly(true); level.addFreshEntity(lightning);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.AMBIENT, 5.0f, 0.8f);
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.AMBIENT, 3.0f, 0.5f);

            stalker = EntityType.WITHER_SKELETON.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (stalker != null) {
                stalker.setPos(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
                stalker.setSilent(true); stalker.setNoAi(true); stalker.setInvulnerable(true);
                stalker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                level.addFreshEntity(stalker);
                
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (level.random.nextBoolean()) {
                            BlockPos firePos = targetPos.offset(dx, 0, dz);
                            if (level.isEmptyBlock(firePos) && !level.isEmptyBlock(firePos.below())) {
                                level.setBlock(firePos, Blocks.SOUL_FIRE.defaultBlockState(), 3);
                                fireBlocks.add(firePos); 
                            }
                        }
                    }
                }
                level.playSound(null, targetPos, SoundEvents.WARDEN_NEARBY_CLOSE, SoundSource.AMBIENT, 4.0f, 0.5f);
                player.sendSystemMessage(Component.literal("§8§o... gölgelerde yerin hazır ..."));
            }
        }

        if (age == 160 && stalker != null) {
            for (BlockPos pos : fireBlocks) {
                if (level.getBlockState(pos).is(Blocks.SOUL_FIRE)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX(), pos.getY(), pos.getZ(), 10, 0.2, 0.2, 0.2, 0.01);
                }
            }
            fireBlocks.clear();
            level.playSound(null, stalker.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.AMBIENT, 1.0f, 0.5f);
        }

        // --- ÖLÜM (FİNAL JUMPSCARE) ANİMASYONU ---
        if (isJumpscaring) {
            if (age > jumpscareTick + 20) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, stalker.getX(), stalker.getY() + 1.0, stalker.getZ(), 100, 0.5, 1.5, 0.5, 0.01);
                cleanup(level, player);
                return true;
            }
            return false;
        }

        // --- MÜCADELE (QTE) DURUMU ---
        if (isStruggling && stalker != null && stalker.isAlive()) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 4, false, false, false)); // Yürümeni aşırı yavaşlatır
            
            if (age % 5 == 0) {
                player.displayClientMessage(Component.literal("§c§l>>> KURTULMAK İÇİN 'SHIFT' (EĞİLME) TUŞUNA ARD ARDA BAS! <<<"), true);
            }

            boolean isSneaking = player.isCrouching();
            if (isSneaking && !prevSneaking) {
                struggleProgress++;
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            prevSneaking = isSneaking;

            if (struggleProgress >= 3) {
                // BAŞARILI KAÇIŞ!
                isStruggling = false;
                player.displayClientMessage(Component.literal("§a§l>>> İTTİN! KAÇ! <<<"), true);
                level.playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 1.0f, 0.5f);
                
                // Yaratığı 15 blok geriye fırlat (Kovalamaca devam etsin)
                Vec3 away = player.getLookAngle().scale(-15.0);
                BlockPos backPos = findGround(level, BlockPos.containing(player.getX() + away.x, player.getY() + 1, player.getZ() + away.z));
                if (backPos != null) stalker.teleportTo(backPos.getX(), backPos.getY(), backPos.getZ());
                else stalker.teleportTo(player.getX() + away.x, player.getY(), player.getZ() + away.z);
                
            } else if (age > struggleStartTick + 60) {
                // 3 Saniye içinde basamadı = BAŞARISIZ (ÖLÜM)
                isStruggling = false;
                catchCount = 3; // Direkt final infazına zorla
            }
            
            return false; // Mücadele bitene kadar alt kısımdaki kovalama mantığını durdur!
        }

        // --- KOVALAMA MANTIGI ---
        if (age > 160 && stalker != null && stalker.isAlive()) {
            Vec3 toPlayer = player.position().subtract(stalker.position());
            double dist = toPlayer.length();
            Vec3 look = player.getLookAngle();
            Vec3 toStalker = stalker.position().subtract(player.position()).normalize();
            boolean isLooking = look.dot(toStalker) > 0.25;

            // YAKALANMA ŞARTI (Yaklaşırsa VEYA Mücadeleyi Kaybederse tetiklenir! Artık bakıp bakmaması umrumuzda değil)
            if (dist < 2.5 || catchCount >= 3) {
                
                if (catchCount < 2) {
                    // MİNİ-JUMPSCARE VE MÜCADELE BAŞLANGICI
                    catchCount++;
                    isStruggling = true;
                    struggleStartTick = age;
                    struggleProgress = 0;

                    // 1.5 Blok önüne ışınla ve yüzüne baktır
                    Vec3 flatLook = new Vec3(look.x, 0, look.z).normalize();
                    Vec3 facePos = player.position().add(flatLook.scale(1.5));
                    BlockPos groundPos = findGround(level, BlockPos.containing(facePos.x, player.getY() + 1, facePos.z));
                    if (groundPos != null) stalker.teleportTo(facePos.x, groundPos.getY(), facePos.z);
                    else stalker.teleportTo(facePos.x, player.getY(), facePos.z);
                    
                    float yaw = (float)(Math.atan2(-flatLook.z, -flatLook.x) * (180D / Math.PI)) - 90.0F;
                    stalker.setYRot(yaw);
                    stalker.yHeadRot = yaw;
                    stalker.yBodyRot = yaw;

                    level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SKELETON_HURT, SoundSource.HOSTILE, 3.0f, 0.5f);
                    player.hurtServer(level, player.damageSources().generic(), 2.0f); // 1 Kalp hasar
                    
                } else {
                    // 3. YAKALANMA (VEYA SHIFT'E BASAMAMA) = ÖLÜMCÜL İNFAZ
                    isJumpscaring = true;
                    jumpscareTick = age;
                    
                    // Tam gözünün içine (0.5 blok) girer
                    Vec3 flatLook = new Vec3(look.x, 0, look.z).normalize();
                    Vec3 facePos = player.position().add(flatLook.scale(0.8));
                    stalker.teleportTo(facePos.x, player.getY(), facePos.z);
                    
                    float yaw = (float)(Math.atan2(-flatLook.z, -flatLook.x) * (180D / Math.PI)) - 90.0F;
                    stalker.setYRot(yaw);
                    stalker.yHeadRot = yaw;
                    
                    level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 5.0f, 0.5f);
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, false));
                    player.hurtServer(level, player.damageSources().generic(), 40.0f); // 20 Kalp (Tek atar)
                }
            }
            // OYUNCU KAÇIYOR - YAKLAŞ
            else if (!isLooking) {
                Vec3 dir = toPlayer.normalize();
                double speed = 0.38; // Senden daha hızlı
                if (dist < 10.0) speed = 0.20; // Yakınlaşınca seninle oynar
                if (dist > 25.0) speed = 0.55; // Uzaklaşırsan depar atar
                
                stalker.teleportTo(stalker.getX() + dir.x * speed, stalker.getY(), stalker.getZ() + dir.z * speed);
                
                float yaw = (float)(Math.atan2(dir.z, dir.x) * (180D / Math.PI)) - 90.0F;
                stalker.setYRot(yaw);
                stalker.yHeadRot = yaw;
                stalker.yBodyRot = yaw; // Vücudunu da düzgün çevir

                if (age % 8 == 0) level.playSound(null, stalker.blockPosition(), SoundEvents.WITHER_SKELETON_STEP, SoundSource.HOSTILE, 2.5f, 0.6f);
            } 
            // OYUNCU BAKIYOR - DON
            else {
                if (!wasLooking) {
                    level.playSound(null, stalker.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 3.0f, 1.5f);
                    if (level.random.nextInt(100) < 30) player.sendSystemMessage(Component.literal(whispers[level.random.nextInt(whispers.length)]));
                }
            }
            wasLooking = isLooking;
        }

        if (age > 2000) { // Olay en fazla 100 saniye sürebilir
            level.sendParticles(ParticleTypes.LARGE_SMOKE, stalker.getX(), stalker.getY() + 1.0, stalker.getZ(), 100, 0.5, 1.5, 0.5, 0.01);
            cleanup(level, player);
            return true;
        }

        return false;
    }

    private BlockPos findGround(ServerLevel level, BlockPos pos) {
        BlockPos current = pos;
        int tries = 0;
        while (level.isEmptyBlock(current.below()) && tries < 20) { current = current.below(); tries++; }
        tries = 0;
        while (!level.isEmptyBlock(current) && tries < 20) { current = current.above(); tries++; }
        if (level.isEmptyBlock(current) && !level.isEmptyBlock(current.below())) return current;
        return null;
    }

    private boolean isUnderTree(ServerLevel level, BlockPos pos) {
        for (int y = 2; y < 15; y++) {
            if (level.getBlockState(pos.above(y)).is(BlockTags.LEAVES) || level.getBlockState(pos.above(y)).is(BlockTags.LOGS)) return true;
        }
        return false;
    }
}