package com.riftbound.horror;
import com.riftbound.horror.event.RealityCorruptionSequence;
import com.riftbound.horror.event.FalseMorningEvent;
import com.riftbound.horror.event.TheDoppelgangerEvent;
import com.riftbound.horror.event.ChronophobiaEvent;
import com.riftbound.horror.event.SpatialDistortionEvent;
import com.riftbound.horror.event.HorrorEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;

public class HorrorDirector {
    private static int tickCounter = 0;

    public static void init() {
        HorrorEventRegistry.register(new RealityCorruptionSequence());
        HorrorEventRegistry.register(new FalseMorningEvent());
        HorrorEventRegistry.register(new TheDoppelgangerEvent());
        HorrorEventRegistry.register(new ChronophobiaEvent());
        HorrorEventRegistry.register(new SpatialDistortionEvent());
        ServerTickEvents.END_SERVER_TICK.register(HorrorDirector::onServerTick);
        HorrorCommand.register();
    }

    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        HorrorState state = overworld.getDataStorage().computeIfAbsent(HorrorState.factory(), "riftbound_horror_state");
        long currentTick = overworld.getGameTime();

        for (ServerLevel sLevel : server.getAllLevels()) {
            for (ServerPlayer player : sLevel.players()) {
                if (player.isSpectator() || player.isCreative()) continue;
                UUID uuid = player.getUUID();

                if (state.activeSequenceId.containsKey(uuid)) {
                    String activeSeqId = state.activeSequenceId.get(uuid);
                    HorrorEvent activeEvent = HorrorEventRegistry.getEvent(activeSeqId);
                    if (activeEvent != null) {
                        long startTick = state.sequenceStartTick.getOrDefault(uuid, currentTick);
                        if (activeEvent.tick(sLevel, player, state, currentTick - startTick) || player.isDeadOrDying()) {
                            activeEvent.cleanup(sLevel, player);
                            state.endSequence(uuid, currentTick);
                        }
                    } else state.endSequence(uuid, currentTick);
                    continue;
                }

                if (tickCounter % 20 == 0) state.analyzePlayer(player);
                if (tickCounter % 40 != 0) continue; 

                long lastGlobal = state.lastGlobalEventTick.getOrDefault(uuid, 0L);
                if (currentTick - lastGlobal < 6000) continue;

                float stress = state.undergroundStress.getOrDefault(uuid, 0f);
                int chance = 1000;
                if (stress > 50) chance -= 300;
                if (stress > 75) chance -= 400; 

                if (sLevel.random.nextInt(Math.max(50, chance)) == 0) {
                    List<HorrorEvent> eligible = new ArrayList<>();
                    int totalWeight = 0;

                    for (HorrorEvent ev : HorrorEventRegistry.getAll()) {
                        if (ev.canTrigger(sLevel, player)) {
                            int dynamicWeight = ev.getWeight();

                            Map<String, Long> lastSeenMap = state.eventLastSeen.get(uuid);
                            if (lastSeenMap != null && lastSeenMap.containsKey(ev.getId())) {
                                long timeSinceLast = currentTick - lastSeenMap.get(ev.getId());
                                if (timeSinceLast < 18000) continue;
                                if (timeSinceLast > 48000) dynamicWeight *= 2;
                            }

                            if (stress > 75 && ev.getId().equals("riftbound:false_morning")) dynamicWeight += 100;

                            eligible.add(ev);
                            totalWeight += dynamicWeight;
                        }
                    }

                    if (!eligible.isEmpty()) {
                        int r = sLevel.random.nextInt(totalWeight);
                        int sum = 0;
                        for (HorrorEvent ev : eligible) {
                            Map<String, Long> lastSeenMap = state.eventLastSeen.get(uuid);
                            int dynamicWeight = ev.getWeight();
                            if (lastSeenMap != null && lastSeenMap.containsKey(ev.getId())) {
                                if (currentTick - lastSeenMap.get(ev.getId()) > 48000) dynamicWeight *= 2;
                            }
                            if (stress > 75 && ev.getId().equals("riftbound:false_morning")) dynamicWeight += 100;

                            sum += dynamicWeight;
                            if (r < sum) {
                                if (ev.getId().equals("riftbound:reality_corruption")) {
                                    if (stress > 75) state.forcedVariation.put(uuid, sLevel.random.nextBoolean() ? 2 : 3);
                                    else if (stress > 50) state.forcedVariation.put(uuid, 1);
                                }
                                state.startSequence(uuid, ev.getId(), currentTick);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}