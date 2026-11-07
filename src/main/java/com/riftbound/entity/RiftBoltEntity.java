package com.riftbound.entity;

import com.riftbound.ability.AbilityManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class RiftBoltEntity extends ThrowableProjectile {
    public RiftBoltEntity(EntityType<? extends RiftBoltEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public RiftBoltEntity(Level level, LivingEntity shooter) {
        super(ModEntities.RIFT_BOLT, shooter.getX(), shooter.getEyeY() - 0.2, shooter.getZ(), level);
        this.setOwner(shooter);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > 80) {
            this.discard();
            return;
        }
        if (this.level().isClientSide) {
            this.level().addParticle(AbilityManager.RIFT_CYAN, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            this.level().addParticle(ParticleTypes.PORTAL, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            Entity hit = result.getEntity();
            Entity owner = this.getOwner();
            if (hit != owner && hit instanceof LivingEntity living) {
                living.hurtServer(serverLevel, this.damageSources().indirectMagic(this, owner), 9.0f);
                serverLevel.sendParticles(AbilityManager.RIFT_PURPLE, hit.getX(), hit.getY() + 1.0, hit.getZ(), 18, 0.3, 0.3, 0.3, 0.05);
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, hit.getX(), hit.getY() + 1.0, hit.getZ(), 15, 0.2, 0.2, 0.2, 0.1);
                this.playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, 0.8f);
                this.discard();
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(AbilityManager.RIFT_CYAN, this.getX(), this.getY(), this.getZ(), 12, 0.2, 0.2, 0.2, 0.05);
            this.playSound(SoundEvents.AMETHYST_BLOCK_BREAK, 0.8f, 1.2f);
            this.discard();
        }
    }
}
