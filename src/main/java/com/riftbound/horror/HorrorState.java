package com.riftbound.horror;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HorrorState extends SavedData {
    public Map<UUID, Long> lastGlobalEventTick = new HashMap<>();
    public Map<UUID, String> activeSequenceId = new HashMap<>();
    public Map<UUID, Long> sequenceStartTick = new HashMap<>();
    public Map<UUID, Integer> forcedVariation = new HashMap<>();
    
    // FEAR HISTORY (Korku Geçmişi)
    public Map<UUID, Map<String, Integer>> eventPlayCounts = new HashMap<>();
    public Map<UUID, Map<String, Long>> eventLastSeen = new HashMap<>();
    
    // UNDERGROUND STRESS (0-100 Stres Sistemi)
    public Map<UUID, Float> undergroundStress = new HashMap<>();

    public HorrorState() {}

    public static SavedData.Factory<HorrorState> factory() { return new SavedData.Factory<>(HorrorState::new, HorrorState::load, null); }
    public static HorrorState load(CompoundTag tag, HolderLookup.Provider provider) { return new HorrorState(); }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) { return tag; }

    public void startSequence(UUID uuid, String eventId, long currentTick) {
        activeSequenceId.put(uuid, eventId);
        sequenceStartTick.put(uuid, currentTick);
        lastGlobalEventTick.put(uuid, currentTick);
        
        // Fear History Güncellemesi
        eventPlayCounts.computeIfAbsent(uuid, k -> new HashMap<>()).merge(eventId, 1, Integer::sum);
        eventLastSeen.computeIfAbsent(uuid, k -> new HashMap<>()).put(eventId, currentTick);
        setDirty();
    }

    public void endSequence(UUID uuid, long currentTick) {
        activeSequenceId.remove(uuid);
        sequenceStartTick.remove(uuid);
        forcedVariation.remove(uuid);
        setDirty();
    }
    
    public void analyzePlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        
        // STRES HESAPLAMA MOTORU (Her saniye çalışır)
        float currentStress = undergroundStress.getOrDefault(uuid, 0f);
        
        if (player.getY() < 50) {
            float stressGain = 0.2f; // Temel artış
            
            // Işık seviyesi düşükse stres çok daha hızlı artar
            int light = player.serverLevel().getMaxLocalRawBrightness(player.blockPosition());
            if (light < 5) stressGain += 0.5f; 
            
            // Derinlik 0'ın altındaysa (Deepslate katmanı) ekstra stres
            if (player.getY() < 0) stressGain += 0.3f;
            
            undergroundStress.put(uuid, Math.min(100f, currentStress + stressGain));
        } else {
            // Yüzeye çıkınca stres yavaşça azalır, huzur gelir
            undergroundStress.put(uuid, Math.max(0f, currentStress - 1.0f));
        }
    }
}