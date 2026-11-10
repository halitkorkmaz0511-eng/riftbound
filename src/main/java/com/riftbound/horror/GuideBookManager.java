package com.riftbound.horror;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public class GuideBookManager {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            
            // Oyuncu bu gizli etikete sahip değilse (dünyaya ilk kez giriyorsa)
            if (!player.getTags().contains("riftbound_guide_received")) {
                String playerName = player.getScoreboardName();
                
                // Sayfaları hazır yazılı kitabı doğrudan oyuncunun envanterine yolla
                String command = "give " + playerName + " written_book[written_book_content={title:\"Riftbound Rehberi\",author:\"Bilinmeyen\",pages:['{\"text\":\"§4Riftbound\\'a Hoş Geldin.§r\\n\\nBu dünya artık eskisi gibi değil. Gölgelerde seni izleyen bir şeyler var.\\n\\nBu rehber, zihnini koruman için yazıldı. Gördüğün her şeye inanma, duyduğun her sese güvenme.\"}','{\"text\":\"§l1. Gerçeklik Çarpılması:§r\\nYeraltında çok kalırsan zihnin bulanır. Gökten kül yağabilir, havaya yükselebilirsin.\\n\\n§l2. Yalancı Şafak:§r\\nGecenin köründe aniden güneş doğarsa, sevinme. Sadece zaman algınla oynanıyor.\"}','{\"text\":\"§l3. Doppelganger:§r\\nTek başınasın. Ama sohbette başkasının katıldığını görürsen ve ensende kazı sesi duyarsan, arkana bak. Orada kimse olmayacak. Sadece zihnin sana oyun oynuyor.\"}','{\"text\":\"§l4. Ölü Zaman:§r\\nBazen gece yarısı saat durur. Yatağa girip uyumaya çalışırsan, yatak seni reddedecektir. Çünkü o odada yalnız olmadığını hissedeceksin.\"}','{\"text\":\"§l5. Mekansal Çarpılma:§r\\nDümdüz bir koridorda dakikalarca yürüyüp hiçbir yere varamadığını hissedersen dur. Aynı koridoru tekrar tekrar yürütülüyorsun. Duvarlara güvenme.\"}']}]";
                
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
                
                // Kitabı verdikten sonra etiketi yapıştır ki bir daha vermesin
                player.addTag("riftbound_guide_received");
            }
        });
    }
}