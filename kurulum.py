import os
import shutil
import urllib.request
import zipfile

print("[1/3] Resmi Fabric Gradle araclari indiriliyor...")
url = "https://github.com/FabricMC/fabric-example-mod/archive/refs/heads/1.21.zip"
urllib.request.urlretrieve(url, "template.zip")

with zipfile.ZipFile("template.zip", "r") as z:
  z.extractall("temp_tpl")

tpl_dir = os.path.join("temp_tpl", os.listdir("temp_tpl")[0])
for item in ["gradlew", "gradlew.bat", "gradle"]:
  src = os.path.join(tpl_dir, item)
  if os.path.isdir(src):
    if os.path.exists(item):
      shutil.rmtree(item)
    shutil.copytree(src, item)
  elif os.path.isfile(src):
    shutil.copy2(src, item)

shutil.rmtree("temp_tpl")
os.remove("template.zip")
if os.path.exists("src"):
  shutil.rmtree("src")

print(
    "[2/3] RIFTBOUND modunun TUM 11 Java sinifi ve kaynak dosyalari"
    " yaziliyor..."
)

FILES = {
    "gradle.properties": """org.gradle.jvmargs=-Xmx2G
minecraft_version=1.21.4
loader_version=0.16.10
fabric_version=0.114.0+1.21.4
loom_version=1.9-SNAPSHOT
mod_version=1.0.0
maven_group=com.riftbound
archives_base_name=riftbound
""",
    "settings.gradle": """pluginManagement {
    repositories {
        maven {
            name = 'Fabric'
            url = 'https://maven.fabricmc.net/'
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
rootProject.name = 'riftbound'
""",
    "build.gradle": """plugins {
    id 'fabric-loom' version "${loom_version}"
    id 'maven-publish'
}

version = project.mod_version
group = project.maven_group

base {
    archivesName = project.archives_base_name
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}

processResources {
    inputs.property "version", project.version
    filesMatching("fabric.mod.json") {
        expand "version": inputs.properties.version
    }
}

tasks.withType(JavaCompile).configureEach {
    it.options.release = 21
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
""",
    "src/main/resources/fabric.mod.json": """{
  "schemaVersion": 1,
  "id": "riftbound",
  "version": "${version}",
  "name": "RIFTBOUND",
  "description": "Dark-fantasy dimensional bosses, synchronized animations, and unlockable rift combat abilities.",
  "authors": ["Riftbound Team"],
  "license": "MIT",
  "environment": "*",
  "entrypoints": {
    "main": ["com.riftbound.RiftboundMod"],
    "client": ["com.riftbound.RiftboundClient"]
  },
  "depends": {
    "fabricloader": ">=0.16.10",
    "minecraft": "~1.21.4",
    "java": ">=21",
    "fabric-api": "*"
  }
}
""",
    "src/main/resources/assets/riftbound/lang/en_us.json": """{
  "entity.riftbound.rift_sentinel": "Rift Sentinel",
  "item.riftbound.rift_core": "Dimensional Rift Core",
  "item.riftbound.rift_caller": "Sigil of the Rift Caller",
  "category.riftbound.abilities": "RIFTBOUND Abilities",
  "key.riftbound.rift_dash": "Activate Rift Dash",
  "key.riftbound.void_pulse": "Activate Void Pulse",
  "key.riftbound.rift_shield": "Activate Rift Shield"
}
""",
    "src/main/java/com/riftbound/RiftboundMod.java": """package com.riftbound;

import com.riftbound.ability.AbilityManager;
import com.riftbound.entity.ModEntities;
import com.riftbound.entity.boss.RiftSentinelEntity;
import com.riftbound.item.ModItems;
import com.riftbound.network.RiftboundPayloads;
import com.riftbound.progression.PlayerRiftData;
import com.riftbound.progression.RiftProgressionState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RiftboundMod implements ModInitializer {
    public static final String MOD_ID = "riftbound";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        RiftboundPayloads.registerPayloads();
        ModItems.register();
        ModEntities.register();

        ServerPlayNetworking.registerGlobalReceiver(RiftboundPayloads.UseAbilityPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> AbilityManager.tryActivateAbility(player, payload.abilityIndex()));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            RiftProgressionState state = RiftProgressionState.getServerState(server);
            PlayerRiftData data = state.getPlayerData(player.getUUID());
            RiftboundPayloads.syncToClient(player, data);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            RiftProgressionState state = RiftProgressionState.getServerState(server);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                AbilityManager.tickPlayer(player, state.getPlayerData(player.getUUID()));
            }
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player) {
                RiftProgressionState state = RiftProgressionState.getServerState(player.server);
                PlayerRiftData data = state.getPlayerData(player.getUUID());
                if (data.shieldTicksRemaining > 0) {
                    player.serverLevel().playSound(null, player.blockPosition(),
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.7f);
                    AbilityManager.spawnShieldHitParticles(player.serverLevel(), player);
                    player.hurtServer(player.serverLevel(), source, amount * 0.25f);
                    return false;
                }
            }
            return true;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("riftbound")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("summon_sentinel")
                    .executes(ctx -> {
                        ServerLevel level = ctx.getSource().getLevel();
                        RiftSentinelEntity sentinel = new RiftSentinelEntity(ModEntities.RIFT_SENTINEL, level);
                        sentinel.moveTo(ctx.getSource().getPosition());
                        sentinel.triggerSpawnSequence();
                        level.addFreshEntity(sentinel);
                        ctx.getSource().sendSuccess(() -> Component.literal("§5[Riftbound] §dSummoned the Rift Sentinel!"), true);
                        return 1;
                    }))
                .then(Commands.literal("unlock_all")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        RiftProgressionState state = RiftProgressionState.getServerState(player.server);
                        PlayerRiftData data = state.getPlayerData(player.getUUID());
                        data.unlockedTier = 3;
                        state.setDirty();
                        RiftboundPayloads.syncToClient(player, data);
                        ctx.getSource().sendSuccess(() -> Component.literal("§b[Riftbound] Unlocked all 3 Rift abilities!"), false);
                        return 1;
                    }))
                .then(Commands.literal("status")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        RiftProgressionState state = RiftProgressionState.getServerState(player.server);
                        PlayerRiftData data = state.getPlayerData(player.getUUID());
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "§5[Riftbound] §7Unlocked Tier: §b" + data.unlockedTier + "/3 §7| Boss Kills: §d" + data.sentinelKills), false);
                        return 1;
                    }))
            );
        });
        LOGGER.info("RIFTBOUND initialized successfully.");
    }
}
""",
    "src/main/java/com/riftbound/network/RiftboundPayloads.java": """package com.riftbound.network;

import com.riftbound.RiftboundMod;
import com.riftbound.progression.PlayerRiftData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class RiftboundPayloads {

    public record UseAbilityPayload(int abilityIndex) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<UseAbilityPayload> TYPE =
                new CustomPacketPayload.Type<>(RiftboundMod.id("use_ability"));

        public static final StreamCodec<FriendlyByteBuf, UseAbilityPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, UseAbilityPayload::abilityIndex,
                UseAbilityPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SyncProgressionPayload(
            int unlockedTier,
            int dashCooldown,
            int pulseCooldown,
            int shieldCooldown,
            int shieldActiveTicks
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SyncProgressionPayload> TYPE =
                new CustomPacketPayload.Type<>(RiftboundMod.id("sync_progression"));

        public static final StreamCodec<FriendlyByteBuf, SyncProgressionPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SyncProgressionPayload::unlockedTier,
                ByteBufCodecs.VAR_INT, SyncProgressionPayload::dashCooldown,
                ByteBufCodecs.VAR_INT, SyncProgressionPayload::pulseCooldown,
                ByteBufCodecs.VAR_INT, SyncProgressionPayload::shieldCooldown,
                ByteBufCodecs.VAR_INT, SyncProgressionPayload::shieldActiveTicks,
                SyncProgressionPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(UseAbilityPayload.TYPE, UseAbilityPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncProgressionPayload.TYPE, SyncProgressionPayload.CODEC);
    }

    public static void syncToClient(ServerPlayer player, PlayerRiftData data) {
        ServerPlayNetworking.send(player, new SyncProgressionPayload(
                data.unlockedTier,
                data.dashCooldown,
                data.pulseCooldown,
                data.shieldCooldown,
                data.shieldTicksRemaining
        ));
    }
}
""",
    "src/main/java/com/riftbound/progression/PlayerRiftData.java": """package com.riftbound.progression;

import net.minecraft.nbt.CompoundTag;

public class PlayerRiftData {
    public int unlockedTier = 0;
    public int sentinelKills = 0;
    public int dashCooldown = 0;
    public int pulseCooldown = 0;
    public int shieldCooldown = 0;
    public int shieldTicksRemaining = 0;

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("UnlockedTier", unlockedTier);
        tag.putInt("SentinelKills", sentinelKills);
        return tag;
    }

    public static PlayerRiftData fromNbt(CompoundTag tag) {
        PlayerRiftData data = new PlayerRiftData();
        data.unlockedTier = tag.getInt("UnlockedTier");
        data.sentinelKills = tag.getInt("SentinelKills");
        return data;
    }
}
""",
    "src/main/java/com/riftbound/progression/RiftProgressionState.java": """package com.riftbound.progression;

import com.riftbound.RiftboundMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RiftProgressionState extends SavedData {
    private final Map<UUID, PlayerRiftData> players = new HashMap<>();

    public PlayerRiftData getPlayerData(UUID uuid) {
        return players.computeIfAbsent(uuid, k -> new PlayerRiftData());
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
        CompoundTag playersTag = new CompoundTag();
        players.forEach((uuid, data) -> playersTag.put(uuid.toString(), data.toNbt()));
        nbt.put("Players", playersTag);
        return nbt;
    }

    public static RiftProgressionState createFromNbt(CompoundTag tag, HolderLookup.Provider provider) {
        RiftProgressionState state = new RiftProgressionState();
        CompoundTag playersTag = tag.getCompound("Players");
        for (String key : playersTag.getAllKeys()) {
            UUID uuid = UUID.fromString(key);
            state.players.put(uuid, PlayerRiftData.fromNbt(playersTag.getCompound(key)));
        }
        return state;
    }

    private static final Factory<RiftProgressionState> FACTORY = new Factory<>(
            RiftProgressionState::new,
            RiftProgressionState::createFromNbt,
            null
    );

    public static RiftProgressionState getServerState(MinecraftServer server) {
        DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        RiftProgressionState state = storage.computeIfAbsent(FACTORY, RiftboundMod.MOD_ID + "_progression");
        state.setDirty();
        return state;
    }
}
""",
    "src/main/java/com/riftbound/ability/AbilityManager.java": """package com.riftbound.ability;

import com.riftbound.network.RiftboundPayloads;
import com.riftbound.progression.PlayerRiftData;
import com.riftbound.progression.RiftProgressionState;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class AbilityManager {
    public static final int DASH_COOLDOWN_MAX = 80;
    public static final int PULSE_COOLDOWN_MAX = 200;
    public static final int SHIELD_COOLDOWN_MAX = 320;
    public static final int SHIELD_DURATION = 100;

    public static final DustParticleOptions RIFT_PURPLE = new DustParticleOptions(0x9D4EDD, 1.5f);
    public static final DustParticleOptions RIFT_CYAN = new DustParticleOptions(0x00F5D4, 1.4f);

    public static void tickPlayer(ServerPlayer player, PlayerRiftData data) {
        boolean changed = false;
        if (data.dashCooldown > 0) {
            data.dashCooldown--;
            if (data.dashCooldown % 5 == 0 || data.dashCooldown == 0) changed = true;
        }
        if (data.pulseCooldown > 0) {
            data.pulseCooldown--;
            if (data.pulseCooldown % 5 == 0 || data.pulseCooldown == 0) changed = true;
        }
        if (data.shieldCooldown > 0) {
            data.shieldCooldown--;
            if (data.shieldCooldown % 5 == 0 || data.shieldCooldown == 0) changed = true;
        }
        if (data.shieldTicksRemaining > 0) {
            data.shieldTicksRemaining--;
            spawnShieldAura(player.serverLevel(), player);
            if (data.shieldTicksRemaining == 0) {
                player.serverLevel().playSound(null, player.blockPosition(),
                        SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
                changed = true;
            } else if (data.shieldTicksRemaining % 5 == 0) {
                changed = true;
            }
        }
        if (changed) {
            RiftboundPayloads.syncToClient(player, data);
        }
    }

    public static void tryActivateAbility(ServerPlayer player, int abilityIndex) {
        RiftProgressionState state = RiftProgressionState.getServerState(player.server);
        PlayerRiftData data = state.getPlayerData(player.getUUID());

        if (data.unlockedTier < abilityIndex) {
            player.displayClientMessage(Component.literal("§cAbility locked! Consume a Rift Core dropped by the Rift Sentinel."), true);
            return;
        }

        switch (abilityIndex) {
            case 1 -> executeRiftDash(player, data);
            case 2 -> executeVoidPulse(player, data);
            case 3 -> executeRiftShield(player, data);
        }
        RiftboundPayloads.syncToClient(player, data);
    }

    private static void executeRiftDash(ServerPlayer player, PlayerRiftData data) {
        if (data.dashCooldown > 0) {
            player.displayClientMessage(Component.literal("§7Rift Dash on cooldown (" + (data.dashCooldown / 20.0f) + "s)"), true);
            return;
        }
        data.dashCooldown = DASH_COOLDOWN_MAX;
        ServerLevel level = player.serverLevel();

        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.3, 0.5, 0.3, 0.08);
        level.sendParticles(RIFT_CYAN, player.getX(), player.getY() + 1.0, player.getZ(), 18, 0.3, 0.5, 0.3, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.35f);

        Vec3 look = player.getLookAngle().normalize();
        Vec3 dashVec = new Vec3(look.x * 1.85, Math.max(0.25, look.y * 0.75), look.z * 1.85);
        player.setDeltaMovement(dashVec);
        player.hurtMarked = true;
        player.fallDistance = 0.0f;

        for (int i = 1; i <= 6; i++) {
            Vec3 step = player.position().add(look.scale(i * 0.6));
            level.sendParticles(RIFT_PURPLE, step.x, step.y + 0.9, step.z, 6, 0.15, 0.15, 0.15, 0.01);
            level.sendParticles(ParticleTypes.PORTAL, step.x, step.y + 0.9, step.z, 8, 0.1, 0.1, 0.1, 0.05);
        }
    }

    private static void executeVoidPulse(ServerPlayer player, PlayerRiftData data) {
        if (data.pulseCooldown > 0) {
            player.displayClientMessage(Component.literal("§7Void Pulse on cooldown (" + (data.pulseCooldown / 20.0f) + "s)"), true);
            return;
        }
        data.pulseCooldown = PULSE_COOLDOWN_MAX;
        ServerLevel level = player.serverLevel();
        double radius = 5.5;

        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.9f, 1.25f);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 0.5, player.getZ(), 1, 0, 0, 0, 0);

        for (double r = 1.0; r <= radius; r += 0.75) {
            int points = (int) (r * 12);
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI * i) / points;
                double px = player.getX() + Math.cos(angle) * r;
                double pz = player.getZ() + Math.sin(angle) * r;
                level.sendParticles(r % 1.5 == 0 ? RIFT_CYAN : RIFT_PURPLE, px, player.getY() + 0.2, pz, 1, 0, 0.05, 0, 0);
            }
        }

        AABB box = player.getBoundingBox().inflate(radius, 2.5, radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && player.hasLineOfSight(e));

        for (LivingEntity target : targets) {
            target.hurtServer(level, player.damageSources().playerAttack(player), 12.0f);
            Vec3 knock = target.position().subtract(player.position()).normalize().scale(1.35);
            target.push(knock.x, 0.45, knock.z);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.25, 0.4, 0.25, 0.04);
        }
    }

    private static void executeRiftShield(ServerPlayer player, PlayerRiftData data) {
        if (data.shieldCooldown > 0) {
            player.displayClientMessage(Component.literal("§7Rift Shield on cooldown (" + (data.shieldCooldown / 20.0f) + "s)"), true);
            return;
        }
        data.shieldCooldown = SHIELD_COOLDOWN_MAX;
        data.shieldTicksRemaining = SHIELD_DURATION;

        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.4f);
        spawnShieldAura(level, player);
    }

    public static void spawnShieldAura(ServerLevel level, ServerPlayer player) {
        double time = level.getGameTime() * 0.2;
        for (int i = 0; i < 4; i++) {
            double angle = time + (i * Math.PI / 2);
            double px = player.getX() + Math.cos(angle) * 1.1;
            double pz = player.getZ() + Math.sin(angle) * 1.1;
            level.sendParticles(RIFT_CYAN, px, player.getY() + 1.0 + Math.sin(time) * 0.3, pz, 1, 0, 0, 0, 0);
        }
    }

    public static void spawnShieldHitParticles(ServerLevel level, ServerPlayer player) {
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.4, 0.5, 0.4, 0.12);
        level.sendParticles(RIFT_PURPLE, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.4, 0.5, 0.4, 0.04);
    }
}
""",
    "src/main/java/com/riftbound/item/ModItems.java": """package com.riftbound.item;

import com.riftbound.RiftboundMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Function;

public class ModItems {
    public static final Item RIFT_CORE = register("rift_core", RiftCoreItem::new,
            new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant());

    public static final Item RIFT_CALLER = register("rift_caller", RiftCallerItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, RiftboundMod.id(name));
        Item item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.accept(RIFT_CORE);
            entries.accept(RIFT_CALLER);
        });
    }
}
""",
    "src/main/java/com/riftbound/item/RiftCoreItem.java": """package com.riftbound.item;

import com.riftbound.ability.AbilityManager;
import com.riftbound.network.RiftboundPayloads;
import com.riftbound.progression.PlayerRiftData;
import com.riftbound.progression.RiftProgressionState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RiftCoreItem extends Item {
    public RiftCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            RiftProgressionState state = RiftProgressionState.getServerState(serverPlayer.server);
            PlayerRiftData data = state.getPlayerData(serverPlayer.getUUID());

            if (data.unlockedTier >= 3) {
                serverPlayer.displayClientMessage(Component.literal("§dYou have already mastered all 3 Riftbound abilities!"), true);
                return InteractionResult.FAIL;
            }

            data.unlockedTier++;
            state.setDirty();
            RiftboundPayloads.syncToClient(serverPlayer, data);

            String unlockedName = switch (data.unlockedTier) {
                case 1 -> "§bRift Dash §7(Press [R])";
                case 2 -> "§dVoid Pulse §7(Press [G])";
                case 3 -> "§5Rift Shield §7(Press [V])";
                default -> "Unknown";
            };

            serverPlayer.sendSystemMessage(Component.literal("§5✦ [RIFT AWAKENED] §fYou absorbed the Rift Core and unlocked: " + unlockedName));
            ServerLevel sLevel = serverPlayer.serverLevel();
            sLevel.playSound(null, serverPlayer.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.1f);
            sLevel.sendParticles(ParticleTypes.END_ROD, serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 40, 0.5, 0.8, 0.5, 0.15);
            sLevel.sendParticles(AbilityManager.RIFT_CYAN, serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 30, 0.5, 0.8, 0.5, 0.05);

            stack.consume(1, serverPlayer);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }
}
""",
    "src/main/java/com/riftbound/item/RiftCallerItem.java": """package com.riftbound.item;

import com.riftbound.entity.ModEntities;
import com.riftbound.entity.boss.RiftSentinelEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class RiftCallerItem extends Item {
    public RiftCallerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            BlockPos pos = context.getClickedPos().above();
            RiftSentinelEntity sentinel = new RiftSentinelEntity(ModEntities.RIFT_SENTINEL, serverLevel);
            sentinel.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, context.getRotation(), 0.0f);
            sentinel.triggerSpawnSequence();
            serverLevel.addFreshEntity(sentinel);

            serverLevel.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.2f, 0.75f);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.literal("§5The dimensional boundary shatters... The Rift Sentinel awakens!"), false);
                context.getItemInHand().consume(1, context.getPlayer());
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }
}
""",
    "src/main/java/com/riftbound/entity/ModEntities.java": """package com.riftbound.entity;

import com.riftbound.RiftboundMod;
import com.riftbound.entity.boss.RiftSentinelEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    public static final ResourceKey<EntityType<?>> RIFT_SENTINEL_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, RiftboundMod.id("rift_sentinel"));

    public static final EntityType<RiftSentinelEntity> RIFT_SENTINEL = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            RIFT_SENTINEL_KEY,
            EntityType.Builder.of(RiftSentinelEntity::new, MobCategory.MONSTER)
                    .sized(1.4f, 3.2f)
                    .clientTrackingRange(10)
                    .build(RIFT_SENTINEL_KEY)
    );

    public static final ResourceKey<EntityType<?>> RIFT_BOLT_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, RiftboundMod.id("rift_bolt"));

    public static final EntityType<RiftBoltEntity> RIFT_BOLT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            RIFT_BOLT_KEY,
            EntityType.Builder.<RiftBoltEntity>of(RiftBoltEntity::new, MobCategory.MISC)
                    .sized(0.45f, 0.45f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build(RIFT_BOLT_KEY)
    );

    public static void register() {
        FabricDefaultAttributeRegistry.register(RIFT_SENTINEL, RiftSentinelEntity.createAttributes());
    }
}
""",
    "src/main/java/com/riftbound/entity/RiftBoltEntity.java": """package com.riftbound.entity;

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
""",
    "src/main/java/com/riftbound/entity/boss/RiftSentinelEntity.java": """package com.riftbound.entity.boss;

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
""",
    "src/main/java/com/riftbound/client/render/RiftSentinelAnimations.java": """package com.riftbound.client.render;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

public class RiftSentinelAnimations {
    public static final AnimationDefinition IDLE = AnimationDefinition.Builder.withLength(2.0f).looping()
            .addAnimation("core", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.0f, KeyframeAnimations.posVec(0.0f, 3.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("core", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(2.0f, KeyframeAnimations.degreeVec(0.0f, 180.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)))
            .build();

    public static final AnimationDefinition RIFT_STRIKE = AnimationDefinition.Builder.withLength(1.6f)
            .addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.65f, KeyframeAnimations.degreeVec(-145.0f, -20.0f, 15.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.85f, KeyframeAnimations.degreeVec(55.0f, 15.0f, -10.0f), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.25f, KeyframeAnimations.degreeVec(45.0f, 10.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.6f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("left_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.65f, KeyframeAnimations.degreeVec(-130.0f, 20.0f, -15.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.85f, KeyframeAnimations.degreeVec(50.0f, -15.0f, 10.0f), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.6f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM)))
            .build();

    public static final AnimationDefinition VOID_WAVE = AnimationDefinition.Builder.withLength(1.6f)
            .addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.1f, KeyframeAnimations.degreeVec(-90.0f, 0.0f, 75.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.4f, KeyframeAnimations.degreeVec(20.0f, 0.0f, -30.0f), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.6f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM)))
            .addAnimation("left_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.1f, KeyframeAnimations.degreeVec(-90.0f, 0.0f, -75.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.4f, KeyframeAnimations.degreeVec(20.0f, 0.0f, 30.0f), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.6f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM)))
            .build();

    public static final AnimationDefinition DEATH = AnimationDefinition.Builder.withLength(3.0f)
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0f, KeyframeAnimations.posVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.0f, KeyframeAnimations.posVec(0.0f, 10.0f, 0.0f), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.0f, KeyframeAnimations.posVec(0.0f, -12.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0f, KeyframeAnimations.degreeVec(0.0f, 0.0f, 0.0f), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(3.0f, KeyframeAnimations.degreeVec(25.0f, 540.0f, 0.0f), AnimationChannel.Interpolations.LINEAR)))
            .build();
}
""",
    "src/main/java/com/riftbound/client/render/RiftSentinelModel.java": """package com.riftbound.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class RiftSentinelModel extends EntityModel<RiftSentinelRenderer.SentinelRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart core;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public RiftSentinelModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.core = this.body.getChild("core");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-7.0F, -24.0F, -4.0F, 14.0F, 14.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 38).addBox(-4.5F, -10.0F, -2.5F, 9.0F, 10.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 9.0F, 9.0F)
                        .texOffs(36, 0).addBox(-7.5F, -13.0F, -1.0F, 3.0F, 8.0F, 3.0F)
                        .texOffs(36, 0).addBox(4.5F, -13.0F, -1.0F, 3.0F, 8.0F, 3.0F),
                PartPose.offset(0.0F, -25.0F, 0.0F));

        body.addOrReplaceChild("core",
                CubeListBuilder.create()
                        .texOffs(36, 16).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.25F)),
                PartPose.offset(0.0F, -17.0F, 0.0F));

        body.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(44, 28).addBox(-5.0F, -2.0F, -3.0F, 5.0F, 22.0F, 6.0F),
                PartPose.offset(-8.0F, -22.0F, 0.0F));

        body.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(44, 28).mirror().addBox(0.0F, -2.0F, -3.0F, 5.0F, 22.0F, 6.0F),
                PartPose.offset(8.0F, -22.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(RiftSentinelRenderer.SentinelRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * ((float) Math.PI / 180F);
        this.head.xRot = state.xRot * ((float) Math.PI / 180F);

        this.rightArm.xRot += Mth.cos(state.walkAnimationPos * 0.6662F) * 0.8F * state.walkAnimationSpeed;
        this.leftArm.xRot += Mth.cos(state.walkAnimationPos * 0.6662F + (float) Math.PI) * 0.8F * state.walkAnimationSpeed;

        this.animate(state.idleAnimationState, RiftSentinelAnimations.IDLE, state.ageInTicks, 1.0F);
        this.animate(state.strikeAnimationState, RiftSentinelAnimations.RIFT_STRIKE, state.ageInTicks, 1.0F);
        this.animate(state.voidWaveAnimationState, RiftSentinelAnimations.VOID_WAVE, state.ageInTicks, 1.0F);
        this.animate(state.deathAnimationState, RiftSentinelAnimations.DEATH, state.ageInTicks, 1.0F);
    }
}
""",
    "src/main/java/com/riftbound/client/render/RiftSentinelRenderer.java": """package com.riftbound.client.render;

import com.riftbound.RiftboundMod;
import com.riftbound.entity.boss.RiftSentinelEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AnimationState;

public class RiftSentinelRenderer extends MobRenderer<RiftSentinelEntity, RiftSentinelRenderer.SentinelRenderState, RiftSentinelModel> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(RiftboundMod.id("rift_sentinel"), "main");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/enderman/enderman.png");

    public RiftSentinelRenderer(EntityRendererProvider.Context context) {
        super(context, new RiftSentinelModel(context.bakeLayer(LAYER_LOCATION)), 0.85F);
    }

    @Override
    public SentinelRenderState createRenderState() {
        return new SentinelRenderState();
    }

    @Override
    public void extractRenderState(RiftSentinelEntity entity, SentinelRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.enraged = entity.isEnraged();
        state.idleAnimationState.copyFrom(entity.idleAnimationState);
        state.strikeAnimationState.copyFrom(entity.strikeAnimationState);
        state.voidWaveAnimationState.copyFrom(entity.voidWaveAnimationState);
        state.deathAnimationState.copyFrom(entity.deathAnimationState);
    }

    @Override
    public ResourceLocation getTextureLocation(SentinelRenderState state) {
        return TEXTURE;
    }

    public static class SentinelRenderState extends LivingEntityRenderState {
        public boolean enraged;
        public final AnimationState idleAnimationState = new AnimationState();
        public final AnimationState strikeAnimationState = new AnimationState();
        public final AnimationState voidWaveAnimationState = new AnimationState();
        public final AnimationState deathAnimationState = new AnimationState();
    }
}
""",
    "src/main/java/com/riftbound/RiftboundClient.java": """package com.riftbound;

import com.mojang.blaze3d.platform.InputConstants;
import com.riftbound.client.render.RiftSentinelModel;
import com.riftbound.client.render.RiftSentinelRenderer;
import com.riftbound.entity.ModEntities;
import com.riftbound.network.RiftboundPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class RiftboundClient implements ClientModInitializer {
    private static KeyMapping dashKey;
    private static KeyMapping pulseKey;
    private static KeyMapping shieldKey;

    public static int clientUnlockedTier = 0;
    public static int clientDashCd = 0;
    public static int clientPulseCd = 0;
    public static int clientShieldCd = 0;
    public static int clientShieldActive = 0;

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(RiftSentinelRenderer.LAYER_LOCATION, RiftSentinelModel::createBodyLayer);
        EntityRendererRegistry.register(ModEntities.RIFT_SENTINEL, RiftSentinelRenderer::new);
        EntityRendererRegistry.register(ModEntities.RIFT_BOLT, NoopRenderer::new);

        dashKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.riftbound.rift_dash", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.riftbound.abilities"));
        pulseKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.riftbound.void_pulse", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.riftbound.abilities"));
        shieldKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.riftbound.rift_shield", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.riftbound.abilities"));

        ClientPlayNetworking.registerGlobalReceiver(RiftboundPayloads.SyncProgressionPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                clientUnlockedTier = payload.unlockedTier();
                clientDashCd = payload.dashCooldown();
                clientPulseCd = payload.pulseCooldown();
                clientShieldCd = payload.shieldCooldown();
                clientShieldActive = payload.shieldActiveTicks();
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (dashKey.consumeClick()) {
                ClientPlayNetworking.send(new RiftboundPayloads.UseAbilityPayload(1));
            }
            while (pulseKey.consumeClick()) {
                ClientPlayNetworking.send(new RiftboundPayloads.UseAbilityPayload(2));
            }
            while (shieldKey.consumeClick()) {
                ClientPlayNetworking.send(new RiftboundPayloads.UseAbilityPayload(3));
            }
        });

        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui || clientUnlockedTier <= 0) return;

            int x = 10;
            int y = guiGraphics.guiHeight() - 42;

            String dashText = formatAbilityStatus("[R] Dash", 1, clientDashCd, 0);
            guiGraphics.drawString(mc.font, Component.literal(dashText), x, y, 0xFFFFFF, true);

            if (clientUnlockedTier >= 2) {
                String pulseText = formatAbilityStatus("[G] Void Pulse", 2, clientPulseCd, 0);
                guiGraphics.drawString(mc.font, Component.literal(pulseText), x, y + 12, 0xFFFFFF, true);
            }
            if (clientUnlockedTier >= 3) {
                String shieldText = formatAbilityStatus("[V] Rift Shield", 3, clientShieldCd, clientShieldActive);
                guiGraphics.drawString(mc.font, Component.literal(shieldText), x, y + 24, 0xFFFFFF, true);
            }
        });
    }

    private static String formatAbilityStatus(String label, int tier, int cdTicks, int activeTicks) {
        if (clientUnlockedTier < tier) return "§8" + label + ": LOCKED";
        if (activeTicks > 0) return "§b" + label + ": ACTIVE (" + String.format("%.1fs", activeTicks / 20.0f) + ")";
        if (cdTicks > 0) return "§7" + label + ": " + String.format("%.1fs", cdTicks / 20.0f);
        return "§d" + label + ": §aREADY";
    }
}
""",
}

for path, content in FILES.items():
  os.makedirs(
      os.path.dirname(path) if os.path.dirname(path) else ".", exist_ok=True
  )
  with open(path, "w", encoding="utf-8") as f:
    f.write(content)

print("[3/3] TUM DOSYALAR HAZIR! Simdi .\\gradlew build calistirabilirsin.")