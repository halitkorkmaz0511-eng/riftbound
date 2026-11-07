package com.riftbound.horror.event;

import com.riftbound.horror.HorrorState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.level.GameRules;

public class FalseMorningEvent extends HorrorEvent {
    
    public FalseMorningEvent() { 
        super("riftbound:false_morning", 4); 
    }

    @Override
    public boolean canTrigger(ServerLevel level, ServerPlayer player) {
        // Yalnızca gece vakti ve yüzeyde/yüzeye yakınken (Y > 50) tetiklenir
        return level.isNight() && player.getY() > 50; 
    }

    @Override
    public void cleanup(ServerLevel level, ServerPlayer player) {
        // İllüzyon bittiğinde veya oyuncu çıktığında GERÇEK zamanı geri yükle
        player.connection.send(new ClientboundSetTimePacket(
            level.getGameTime(), 
            level.getDayTime(), 
            level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)
        ));
    }

    @Override
    public boolean tick(ServerLevel level, ServerPlayer player, HorrorState state, long age) {
        // 10 Saniyelik (200 Tick) Sahte Sabah İllüzyonu
        if (age == 1) {
            // Sabah hissi vermek için sahte papağan/kuş sesleri
            playSafeSound(level, player, "entity.parrot.ambient", 1.0f, 1.0f); 
        }

        if (age > 1 && age < 200) {
            // Her tick client'a "Saat şu an sabah (1000L)" diye sahte paket gönderiyoruz.
            // Bunu her tick yapıyoruz ki Minecraft'ın kendi senkronizasyonu illüzyonu bozmasın.
            player.connection.send(new ClientboundSetTimePacket(level.getGameTime(), 1000L, false));
            
            // Arada bir kuş sesleri
            if (age % 50 == 0) {
                playSafeSound(level, player, "entity.parrot.ambient", 0.6f, (float)(0.8 + level.random.nextDouble() * 0.4));
            }
        }

        if (age == 200) {
            // İllüzyon Kırılır - Geceye aniden geri dönüş
            cleanup(level, player); // Gerçek karanlık paketi yollanır
            
            // Kırılma şoku (Büyük, boğuk bir patlama ve ışınlanma sesi)
            playSafeSound(level, player, "ambient.cave", 3.0f, 0.1f);
            playSafeSound(level, player, "entity.enderman.teleport", 2.0f, 0.5f);
        }

        if (age >= 240) {
            return true;
        }

        return false;
    }
}