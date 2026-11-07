package com.riftbound.entity.boss;

import com.riftbound.ability.AbilityManager;
import com.riftbound.entity.RiftBoltEntity;
import com.riftbound.item.ModItems;
import com.riftbound.network.RiftboundPayloads;
import com.riftbound.progression.PlayerRiftData;
import com.riftbound.progression.RiftProgressionState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RiftSentinelEntity extends Monster {
    public static final int STATE_IDLE = 0;
    public static final int STATE_STRIKE_WINDUP = 1;
    public static final int STATE_STRIKE_HIT = 2;
    public static final int STATE_STRIKE_RECOVERY = 3;
    public static final int STATE_VOID_WAVE_CHARGE = 4;
    public static final int STATE_RIFT_BOLTS = 5;
    public static final int STATE_PHASE_SHIFT = 6;
    public static final int STATE_DEATH_SEQUENCE = 7;

    private static final EntityDataAccessor<Integer> ATTACK_STATE =
            SynchedEntityData.defineId(RiftSentinelEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ENRAGED =
            SynchedEntityData.defineId(RiftSentinelEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.literal("§5§lRift Sentinel §7- §dDimensional Guardian"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10
    );

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState strikeAnimationState = new AnimationState();
    public final AnimationState voidWaveAnimationState = new AnimationState();
    public final AnimationState phaseShiftAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();

    private int stateTimer = 0;
    private int globalCooldown = 40;
    private int strikeCooldown = 0;
    private int waveCooldown = 100;
    private int boltsCooldown = 60;
    private int shiftCooldown = 140;
    private int customDeathTicks = 0;

    public RiftSentinelEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 250;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACK_STATE, STATE_IDLE);
        builder.define(ENRAGED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public int getAttackState() {
        return this.entityData.get(ATTACK_STATE);
    }

    public void setAttackState(int state) {
        this.entityData.set(ATTACK_STATE, state);
        this.stateTimer = 0;
    }

    public boolean isEnraged() {
        return this.entityData.get(ENRAGED);
    }

    public void setEnraged(boolean enraged) {
        this.entityData.set(ENRAGED, enraged);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (this.level().isClientSide && ATTACK_STATE.equals(key)) {
            int state = getAttackState();
            strikeAnimationState.stop();
            voidWaveAnimationState.stop();
            phaseShiftAnimationState.stop();
            if (state == STATE_STRIKE_WINDUP) {
                strikeAnimationState.start(this.tickCount);
            } else if (state == STATE_VOID_WAVE_CHARGE) {
                voidWaveAnimationState.start(this.tickCount);
            } else if (state == STATE_PHASE_SHIFT) {
                phaseShiftAnimationState.start(this.tickCount);
            } else if (state == STATE_DEATH_SEQUENCE) {
                deathAnimationState.start(this.tickCount);
            }
        }
    }

    public void triggerSpawnSequence() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 60, 0.8, 1.5, 0.8, 0.15);
            serverLevel.sendParticles(AbilityManager.RIFT_CYAN, getX(), getY() + 1.5, getZ(), 40, 0.8, 1.5, 0.8, 0.05);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            this.idleAnimationState.startIfStopped(this.tickCount);
            spawnClientAmbientParticles();
            return;
        }

        ServerLevel serverLevel = (ServerLevel) this.level();
        this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());

        if (!isEnraged() && this.getHealth() > 0 && this.getHealth() <= this.getMaxHealth() * 0.45f) {
            triggerEnragedPhase(serverLevel);
        }

        if (this.getHealth() <= 0 || getAttackState() == STATE_DEATH_SEQUENCE) {
            return;
        }

        int cdSpeed = isEnraged() ? 2 : 1;
        if (globalCooldown > 0) globalCooldown -= cdSpeed;
        if (strikeCooldown > 0) strikeCooldown -= cdSpeed;
        if (waveCooldown > 0) waveCooldown -= cdSpeed;
        if (boltsCooldown > 0) boltsCooldown -= cdSpeed;
        if (shiftCooldown > 0) shiftCooldown -= cdSpeed;

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            if (getAttackState() != STATE_IDLE) setAttackState(STATE_IDLE);
            return;
        }

        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distSq = this.distanceToSqr(target);

        if (getAttackState() == STATE_IDLE) {
            if (distSq > 9.0) {
                this.getNavigation().moveTo(target, isEnraged() ? 1.35D : 1.05D);
            } else {
                this.getNavigation().stop();
            }

            if (globalCooldown <= 0) {
                selectNextAttack(target, distSq);
            }
        } else {
            this.getNavigation().stop();
            tickActiveAttackState(serverLevel, target);
        }
    }

    private void spawnClientAmbientParticles() {
        if (this.random.nextInt(2) == 0) {
            double ox = (this.random.nextDouble() - 0.5) * 1.6;
            double oy = this.random.nextDouble() * 3.0;
            double oz = (this.random.nextDouble() - 0.5) * 1.6;
            this.level().addParticle(isEnraged() ? AbilityManager.RIFT_CYAN : AbilityManager.RIFT_PURPLE,
                    this.getX() + ox, this.getY() + oy, this.getZ() + oz, 0, 0.03, 0);
        }
    }

    private void triggerEnragedPhase(ServerLevel serverLevel) {
        setEnraged(true);
        this.bossBar.setColor(BossEvent.BossBarColor.BLUE);
        this.bossBar.setName(Component.literal("§b§lRift Sentinel §7- §3§lOVERLOADED RIFT PHASE"));
        serverLevel.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.5f, 0.85f);
        serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 1.8, getZ(), 2, 0.2, 0.2, 0.2, 0.0);
        serverLevel.sendParticles(AbilityManager.RIFT_CYAN, getX(), getY() + 1.5, getZ(), 80, 1.5, 1.5, 1.5, 0.12);

        for (ServerPlayer player : bossBar.getPlayers()) {
            player.displayClientMessage(Component.literal("§b§l⚡ The Rift Sentinel destabilizes and enters its Enraged Phase!"), false);
        }
    }

    private void selectNextAttack(LivingEntity target, double distSq) {
        if (distSq <= 16.0 && strikeCooldown <= 0 && this.hasLineOfSight(target)) {
            setAttackState(STATE_STRIKE_WINDUP);
            this.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 1.0f, 0.6f);
        } else if (distSq <= 49.0 && waveCooldown <= 0) {
            setAttackState(STATE_VOID_WAVE_CHARGE);
            this.playSound(SoundEvents.BEACON_POWER_SELECT, 1.4f, 0.6f);
        } else if (distSq > 36.0 && shiftCooldown <= 0) {
            setAttackState(STATE_PHASE_SHIFT);
            this.playSound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.2f, 0.8f);
        } else if (boltsCooldown <= 0 && this.hasLineOfSight(target)) {
            setAttackState(STATE_RIFT_BOLTS);
            this.playSound(SoundEvents.BLAZE_SHOOT, 1.2f, 0.6f);
        }
    }

    private void tickActiveAttackState(ServerLevel level, LivingEntity target) {
        stateTimer++;
        int state = getAttackState();

        switch (state) {
            case STATE_STRIKE_WINDUP -> {
                Vec3 forward = this.getLookAngle().multiply(1, 0, 1).normalize().scale(2.3);
                Vec3 hitCenter = this.position().add(forward);
                drawWarningCircle(level, hitCenter, 2.2, AbilityManager.RIFT_PURPLE);

                if (stateTimer >= (isEnraged() ? 12 : 16)) {
                    setAttackState(STATE_STRIKE_HIT);
                }
            }
            case STATE_STRIKE_HIT -> {
                if (stateTimer == 1) {
                    Vec3 forward = this.getLookAngle().multiply(1, 0, 1).normalize().scale(2.3);
                    Vec3 hitCenter = this.position().add(forward);
                    level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.1f, 1.2f);
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, hitCenter.x, hitCenter.y + 1.0, hitCenter.z, 6, 0.8, 0.3, 0.8, 0.0);
                    level.sendParticles(AbilityManager.RIFT_CYAN, hitCenter.x, hitCenter.y + 0.3, hitCenter.z, 35, 1.2, 0.2, 1.2, 0.08);

                    AABB hitBox = new AABB(hitCenter.x - 2.3, hitCenter.y - 0.5, hitCenter.z - 2.3,
                            hitCenter.x + 2.3, hitCenter.y + 2.8, hitCenter.z + 2.3);
                    for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != this && this.hasLineOfSight(e))) {
                        victim.hurtServer(level, this.damageSources().mobAttack(this), isEnraged() ? 18.0f : 14.0f);
                        Vec3 kb = victim.position().subtract(this.position()).normalize().scale(1.1);
                        victim.push(kb.x, 0.35, kb.z);
                    }
                }
                if (stateTimer >= 6) {
                    setAttackState(STATE_STRIKE_RECOVERY);
                }
            }
            case STATE_STRIKE_RECOVERY -> {
                if (stateTimer >= (isEnraged() ? 12 : 20)) {
                    strikeCooldown = 50;
                    globalCooldown = 20;
                    setAttackState(STATE_IDLE);
                }
            }
            case STATE_VOID_WAVE_CHARGE -> {
                double radius = 6.5;
                drawWarningCircle(level, this.position(), radius, AbilityManager.RIFT_CYAN);
                drawWarningCircle(level, this.position(), (stateTimer / 32.0) * radius, AbilityManager.RIFT_PURPLE);

                if (stateTimer == 10 || stateTimer == 20) {
                    level.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.5f, 1.4f);
                }

                if (stateTimer >= (isEnraged() ? 24 : 32)) {
                    level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.4f, 0.75f);
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 0.5, getZ(), 2, 0.5, 0.2, 0.5, 0.0);
                    drawWarningCircle(level, this.position(), radius, AbilityManager.RIFT_CYAN);

                    AABB waveBox = this.getBoundingBox().inflate(radius, 2.0, radius);
                    for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, waveBox, e -> e != this && this.hasLineOfSight(e))) {
                        if (victim.distanceToSqr(this) <= radius * radius) {
                            victim.hurtServer(level, this.damageSources().magic(), isEnraged() ? 20.0f : 15.0f);
                            Vec3 launch = victim.position().subtract(this.position()).normalize().scale(1.6);
                            victim.push(launch.x, 0.65, launch.z);
                        }
                    }
                    waveCooldown = 160;
                    globalCooldown = 35;
                    setAttackState(STATE_IDLE);
                }
            }
            case STATE_RIFT_BOLTS -> {
                if (stateTimer == 8 || stateTimer == 16 || stateTimer == 24 || (isEnraged() && stateTimer == 32)) {
                    if (this.hasLineOfSight(target)) {
                        RiftBoltEntity bolt = new RiftBoltEntity(level, this);
                        Vec3 dir = target.getEyePosition().subtract(bolt.position()).normalize();
                        bolt.shoot(dir.x, dir.y, dir.z, 1.25f, 1.5f);
                        level.addFreshEntity(bolt);
                        level.playSound(null, blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.HOSTILE, 1.2f, 1.3f);
                    }
                }
                if (stateTimer >= 36) {
                    boltsCooldown = 110;
                    globalCooldown = 25;
                    setAttackState(STATE_IDLE);
                }
            }
            case STATE_PHASE_SHIFT -> {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 12, 0.4, 0.9, 0.4, 0.08);
                if (stateTimer >= 12) {
                    performSafeTeleport(level, target);
                    shiftCooldown = 180;
                    globalCooldown = 20;
                    setAttackState(STATE_IDLE);
                }
            }
        }
    }

    private void performSafeTeleport(ServerLevel level, LivingEntity target) {
        Vec3 oldPos = this.position();
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double dist = 4.0 + this.random.nextDouble() * 4.0;
            double tx = target.getX() + Math.cos(angle) * dist;
            double tz = target.getZ() + Math.sin(angle) * dist;
            BlockPos candidate = BlockPos.containing(tx, target.getY(), tz);

            for (int dy = 2; dy >= -3; dy--) {
                BlockPos check = candidate.offset(0, dy, 0);
                if (level.getBlockState(check.below()).isSolid() && level.isEmptyBlock(check) && level.isEmptyBlock(check.above())) {
                    level.sendParticles(AbilityManager.RIFT_PURPLE, oldPos.x, oldPos.y + 1.5, oldPos.z, 40, 0.4, 1.0, 0.4, 0.1);
                    level.playSound(null, BlockPos.containing(oldPos), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.3f, 0.8f);

                    this.teleportTo(check.getX() + 0.5, check.getY(), check.getZ() + 0.5);

                    level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.5, getZ(), 1, 0, 0, 0, 0);
                    level.sendParticles(AbilityManager.RIFT_CYAN, getX(), getY() + 1.5, getZ(), 40, 0.4, 1.0, 0.4, 0.1);
                    level.playSound(null, check, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.3f, 1.2f);
                    return;
                }
            }
        }
    }

    private void drawWarningCircle(ServerLevel level, Vec3 center, double radius, net.minecraft.core.particles.ParticleOptions particle) {
        int points = Math.max(16, (int) (radius * 10));
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double px = center.x + Math.cos(angle) * radius;
            double pz = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, px, center.y + 0.15, pz, 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected void tickDeath() {
        if (getAttackState() != STATE_DEATH_SEQUENCE) {
            setAttackState(STATE_DEATH_SEQUENCE);
        }
        customDeathTicks++;

        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            double progress = customDeathTicks / 60.0;
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5 + progress, getZ(),
                    8, 0.6 * (1.0 - progress), 0.8, 0.6 * (1.0 - progress), 0.1);
            serverLevel.sendParticles(AbilityManager.RIFT_CYAN, getX(), getY() + 1.5, getZ(), 5, 0.5, 0.8, 0.5, 0.05);

            if (customDeathTicks % 15 == 0) {
                serverLevel.playSound(null, blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.HOSTILE, 1.2f, 0.6f + (float) progress);
            }

            if (customDeathTicks >= 60) {
                serverLevel.playSound(null, blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 1.5f, 0.9f);
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1.5, getZ(), 2, 0.2, 0.2, 0.2, 0);
                serverLevel.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 80, 0.8, 0.8, 0.8, 0.25);

                this.spawnAtLocation(serverLevel, new ItemStack(ModItems.RIFT_CORE, 1));

                RiftProgressionState state = RiftProgressionState.getServerState(serverLevel.getServer());
                for (ServerPlayer player : bossBar.getPlayers()) {
                    PlayerRiftData data = state.getPlayerData(player.getUUID());
                    data.sentinelKills++;
                    if (data.unlockedTier == 0) {
                        data.unlockedTier = 1;
                        player.sendSystemMessage(Component.literal("§5✦ [RIFTBOUND] §dThe Rift Sentinel collapses! §bRift Dash [R] §dhas been automatically unlocked, and a §5Rift Core §ddropped to unlock your next ability!"));
                    } else {
                        player.sendSystemMessage(Component.literal("§5✦ [RIFTBOUND] §dThe Rift Sentinel collapses! Use the dropped §5Rift Core §dto unlock your next ability tier."));
                    }
                    RiftboundPayloads.syncToClient(player, data);
                }
                state.setDirty();
                this.remove(RemovalReason.KILLED);
            }
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Enraged", isEnraged());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setEnraged(tag.getBoolean("Enraged"));
        if (this.hasCustomName()) {
            this.bossBar.setName(this.getDisplayName());
        }
    }
}
