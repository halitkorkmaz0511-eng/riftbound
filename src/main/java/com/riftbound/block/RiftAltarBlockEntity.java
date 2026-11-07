package com.riftbound.block;

import com.riftbound.ability.AbilityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.InteractionHand;
import java.util.List;
import java.util.Optional;

public class RiftAltarBlockEntity extends BlockEntity {
    private int state = 0; // 0=IDLE, 1=WAVE_ACTIVE, 2=COOLDOWN, 3=BOSS_ACTIVE
    private int cooldownTicks = 0;
    private int checkTicks = 0;
    private static final String MOB_TAG = "rift_shrine_mob";
    private static final String BOSS_TAG = "rift_sentinel_boss";

    public RiftAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RIFT_ALTAR_BE, pos, state);
    }

    public boolean activate(Player player) {
        if (state != 0) {
            if (state == 2) player.displayClientMessage(Component.literal("§cAltarin enerjisi tukenmis. Daha sonra tekrar gel."), true);
            else if (state == 3) player.displayClientMessage(Component.literal("§4Rituel zaten basladi! Hayatta kalmaya calis!"), true);
            return false;
        }

        // Oyuncunun elinde Rift Core olup olmadigini kontrol et
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        boolean hasCoreInMain = mainHand.getItem() == com.riftbound.item.ModItems.RIFT_CORE;
        boolean hasCoreInOff = offHand.getItem() == com.riftbound.item.ModItems.RIFT_CORE;

        if (hasCoreInMain || hasCoreInOff) {
            // BOSS RİTÜELİNİ BAŞLAT
            if (hasCoreInMain) mainHand.shrink(1);
            else offHand.shrink(1);

            state = 3; // BOSS_ACTIVE State
            setChanged();

            if (level instanceof ServerLevel sLevel) {
                sLevel.playSound(null, getBlockPos(), SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 1.0f, 0.5f);
                sLevel.playSound(null, getBlockPos(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 1.0f, 1.0f);
                
                // Dev mor ve camgobegi isin / patlama efekti
                sLevel.sendParticles(AbilityManager.RIFT_PURPLE, getBlockPos().getX() + 0.5, getBlockPos().getY() + 3.0, getBlockPos().getZ() + 0.5, 300, 1.0, 5.0, 1.0, 0.5);
                sLevel.sendParticles(AbilityManager.RIFT_CYAN, getBlockPos().getX() + 0.5, getBlockPos().getY() + 3.0, getBlockPos().getZ() + 0.5, 100, 2.0, 2.0, 2.0, 0.2);

                player.displayClientMessage(Component.literal("§4§lBoyut yirtiliyor! RIFT SENTINEL UYANDI!"), true);
                
                // Boss'u Cagir (1.21.4 Optional duzeltmesi)
                Optional<net.minecraft.core.Holder.Reference<EntityType<?>>> optionalType = BuiltInRegistries.ENTITY_TYPE.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("riftbound", "rift_sentinel"));
                if (optionalType.isPresent()) {
                    EntityType<?> sentinelType = optionalType.get().value();
                    net.minecraft.world.entity.Entity boss = sentinelType.create(sLevel, EntitySpawnReason.SPAWNER);
                    if (boss instanceof LivingEntity livingBoss) {
                        livingBoss.setPos(getBlockPos().getX() + 0.5, getBlockPos().getY() + 5.0, getBlockPos().getZ() + 0.5);
                        livingBoss.addTag(BOSS_TAG);
                        sLevel.addFreshEntity(livingBoss);
                    }
                }
            }
            return true;
        } else {
            // NORMAL WAVE BAŞLAT (Eski sistem)
            state = 1;
            checkTicks = 0;
            setChanged();
            if (level instanceof ServerLevel sLevel) {
                sLevel.playSound(null, getBlockPos(), SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0f, 0.8f);
                sLevel.sendParticles(AbilityManager.RIFT_PURPLE, getBlockPos().getX() + 0.5, getBlockPos().getY() + 1.5, getBlockPos().getZ() + 0.5, 100, 1.0, 1.0, 1.0, 0.1);
                spawnWave(sLevel);
                player.displayClientMessage(Component.literal("§dBoyut yirtigi acildi! Gelenleri yok et!"), true);
            }
            return true;
        }
    }

    private void spawnWave(ServerLevel level) {
        BlockPos p = getBlockPos();
        for(int i = 0; i < 4; i++) {
            EnderMan mob = EntityType.ENDERMAN.create(level, EntitySpawnReason.SPAWNER);
            if(mob != null) {
                mob.setPos(p.getX() + (level.random.nextDouble() * 8 - 4), p.getY() + 1, p.getZ() + (level.random.nextDouble() * 8 - 4));
                mob.addTag(MOB_TAG);
                mob.setCustomName(Component.literal("§5Rift Walker"));
                mob.setCustomNameVisible(true);
                level.addFreshEntity(mob);
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RiftAltarBlockEntity be) {
        if (level.isClientSide) return;
        
        if (be.state == 1) { // Wave Takibi
            be.checkTicks++;
            if (be.checkTicks >= 40) { 
                be.checkTicks = 0;
                AABB area = new AABB(pos).inflate(24);
                List<LivingEntity> mobs = level.getEntitiesOfClass(LivingEntity.class, area, e -> e.getTags().contains(MOB_TAG));
                
                if (mobs.isEmpty()) {
                    be.state = 2; // Savas bitti, cooldown
                    be.cooldownTicks = 6000;
                    be.setChanged();
                    level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    if (level instanceof ServerLevel sLevel) {
                        sLevel.sendParticles(AbilityManager.RIFT_CYAN, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 200, 0.5, 1.5, 0.5, 0.1);
                    }
                    ItemEntity reward = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, new ItemStack(com.riftbound.item.ModItems.RIFT_CORE));
                    level.addFreshEntity(reward);
                }
            }
        } else if (be.state == 3) { // Boss Takibi
            be.checkTicks++;
            if (be.checkTicks >= 40) {
                be.checkTicks = 0;
                AABB area = new AABB(pos).inflate(48); // Boss arenasi daha genis taranir
                List<LivingEntity> bosses = level.getEntitiesOfClass(LivingEntity.class, area, e -> e.getTags().contains(BOSS_TAG));
                
                if (bosses.isEmpty()) {
                    be.state = 2; // Boss bitti, Altar dinlenmeye (cooldown) gecer
                    be.cooldownTicks = 12000; // Boss savasi sonrasi 10 dakika cooldown
                    be.setChanged();
                    level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    if (level instanceof ServerLevel sLevel) {
                        sLevel.sendParticles(AbilityManager.RIFT_CYAN, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 500, 2.0, 2.0, 2.0, 0.2);
                    }
                }
            }
        } else if (be.state == 2) { // Cooldown
            be.cooldownTicks--;
            if (be.cooldownTicks <= 0) {
                be.state = 0;
                be.setChanged();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("RiftState", state);
        tag.putInt("RiftCooldown", cooldownTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        state = tag.getInt("RiftState");
        cooldownTicks = tag.getInt("RiftCooldown");
    }
}