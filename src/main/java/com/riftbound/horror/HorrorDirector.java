package com.riftbound.horror;
import com.riftbound.horror.event.RealityCorruptionSequence;
import com.riftbound.horror.event.FalseMorningEvent;
import com.riftbound.horror.event.TheUninvitedEvent;
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
        HorrorEventRegistry.register(new TheUninvitedEvent());
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
                    // Aktif event kodlarÄ± (kÄ±saltÄ±ldÄ±)
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
                if (tickCounter % 40 != 0) continue; // 2 saniyede bir karar ver

                // 1. GENEL COOLDOWN (Son olaydan beri en az 5 dakika geÃ§miÅŸ olmalÄ±)
                long lastGlobal = state.lastGlobalEventTick.getOrDefault(uuid, 0L);
                if (currentTick - lastGlobal < 6000) continue; 

                float stress = state.undergroundStress.getOrDefault(uuid, 0f);
                int chance = 1000; 
                if (stress > 50) chance -= 300; 
                if (stress > 75) chance -= 400; // YÃ¼ksek streste olay ihtimali fÄ±rlar
                
                if (sLevel.random.nextInt(Math.max(50, chance)) == 0) {
                    List<HorrorEvent> eligible = new ArrayList<>();
                    int totalWeight = 0;
                    
                    for (HorrorEvent ev : HorrorEventRegistry.getAll()) {
                        if (ev.canTrigger(sLevel, player)) {
                            int dynamicWeight = ev.getWeight();
                            
                            Map<String, Long> lastSeenMap = state.eventLastSeen.get(uuid);
                            if (lastSeenMap != null && lastSeenMap.containsKey(ev.getId())) {
                                long timeSinceLast = currentTick - lastSeenMap.get(ev.getId());
                                
                                // 2. OLAY BAZLI COOLDOWN: AynÄ± olay 15 dakika (18000 tick) iÃ§inde tekrar edemez!
                                if (timeSinceLast < 18000) continue; 
                                
                                // Olay Ã§ok uzun sÃ¼redir gÃ¶rÃ¼nmediyse (Ã¶rneÄŸin 40 dakika) aÄŸÄ±rlÄ±ÄŸÄ±nÄ± (ÅŸansÄ±nÄ±) ikiye katla
                                if (timeSinceLast > 48000) dynamicWeight *= 2; 
                            }
                            
                            // STRES ETKÄ°SÄ°: YÃ¼ksek streste saldÄ±rgan varyasyonlar yerine manipÃ¼lasyon seÃ§ilir (VaryasyonlarÄ± RealityCorruption iÃ§inden yÃ¶netmek iÃ§in ÅŸimdilik Event aÄŸÄ±rlÄ±ÄŸÄ±na yansÄ±tÄ±yoruz)
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
                                // YÃ¼ksek streste Reality Corruption seÃ§ilirse Jumpscare (0) yerine Paranoya(2) veya 4.Duvar(3) zorlanabilir
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