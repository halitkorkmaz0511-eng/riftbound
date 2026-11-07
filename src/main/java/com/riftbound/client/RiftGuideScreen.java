package com.riftbound.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RiftGuideScreen extends Screen {
    private int currentPage = 0;
    private final int totalPages = 3;

    public RiftGuideScreen() {
        super(Component.literal("Riftbound Rehberi"));
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Geri Butonu
        this.addRenderableWidget(Button.builder(Component.literal("< Geri"), button -> {
            if (currentPage > 0) {
                currentPage--;
                this.clearWidgets();
                this.init();
            }
        }).bounds(centerX - 140, centerY + 85, 60, 20).build());

        // Ileri Butonu
        this.addRenderableWidget(Button.builder(Component.literal("Ileri >"), button -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                this.clearWidgets();
                this.init();
            }
        }).bounds(centerX + 80, centerY + 85, 60, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Yari seffaf arka plan
        graphics.fill(0, 0, this.width, this.height, 0xB3000000);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Ana Cerceve (Koyu Mor)
        graphics.fill(centerX - 150, centerY - 110, centerX + 150, centerY + 110, 0xFF12081C);
        
        // Cerceve Kenarliklari (Camgobegi)
        graphics.fill(centerX - 151, centerY - 111, centerX + 151, centerY - 110, 0xFF00DCFF);
        graphics.fill(centerX - 151, centerY + 110, centerX + 151, centerY + 111, 0xFF00DCFF);
        graphics.fill(centerX - 151, centerY - 111, centerX - 150, centerY + 111, 0xFF00DCFF);
        graphics.fill(centerX + 150, centerY - 111, centerX + 151, centerY + 111, 0xFF00DCFF);

        // Butonlarin cizilmesi
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, "§d§l=== RIFTBOUND REHBERI (" + (currentPage + 1) + "/" + totalPages + ") ===", centerX, centerY - 95, 0xFFFFFF);

        if (currentPage == 0) {
            graphics.drawString(this.font, "§b[ 1. Boyut Monoliti ]", centerX - 135, centerY - 70, 0xFFFFFF);
            graphics.drawString(this.font, "§7Dunyada tapinagi bul veya komutla insa et.", centerX - 135, centerY - 55, 0xAAAAAA);
            graphics.drawString(this.font, "§7Merkezdeki Altar'a sag tiklayip yaratik", centerX - 135, centerY - 45, 0xAAAAAA);
            graphics.drawString(this.font, "§7dalgasini (Rift Walker) temizle.", centerX - 135, centerY - 35, 0xAAAAAA);
            
            graphics.drawString(this.font, "§b[ 2. Boyut Cekirdegi (Rift Core) ]", centerX - 135, centerY - 10, 0xFFFFFF);
            graphics.drawString(this.font, "§7Sunagi savundugunda bu cekirdek duser.", centerX - 135, centerY + 5, 0xAAAAAA);
            graphics.drawString(this.font, "§7Eline alip sag tiklayarak kendi gucunu", centerX - 135, centerY + 15, 0xAAAAAA);
            graphics.drawString(this.font, "§7artir (Atilma, Sok Dalgasi, Gecici Kalkan).", centerX - 135, centerY + 25, 0xAAAAAA);
        } else if (currentPage == 1) {
            graphics.drawString(this.font, "§b[ 3. Ozel Silahlar ]", centerX - 135, centerY - 70, 0xFFFFFF);
            graphics.drawString(this.font, "§cBoyut Kilici:§7 Sag tikla baktigin yere isinlan.", centerX - 135, centerY - 55, 0xAAAAAA);
            graphics.drawString(this.font, "§cBoyut Tirpani:§7 Vuruslar can calar ve alan", centerX - 135, centerY - 45, 0xAAAAAA);
            graphics.drawString(this.font, "§7hasari verir. Sag tikla girdap yaratip cek.", centerX - 135, centerY - 35, 0xAAAAAA);
            graphics.drawString(this.font, "§cBoyut Asasi:§7 Sag tikla delici lazer atesle.", centerX - 135, centerY - 25, 0xAAAAAA);
            
            graphics.drawString(this.font, "§b[ 4. Rift Zirh Seti ]", centerX - 135, centerY, 0xFFFFFF);
            graphics.drawString(this.font, "§7Ozel zirhlarin 4 parcasini da ayni anda", centerX - 135, centerY + 15, 0xAAAAAA);
            graphics.drawString(this.font, "§7giydiginde set bonusu aktiflesir:", centerX - 135, centerY + 25, 0xAAAAAA);
            graphics.drawString(this.font, "§eHiz, Direnc ve Gece Gorusu§7 kazanirsin.", centerX - 135, centerY + 35, 0xAAAAAA);
        } else if (currentPage == 2) {
            graphics.drawString(this.font, "§b[ 5. Rift Sentinel (Nihai Boss) ]", centerX - 135, centerY - 70, 0xFFFFFF);
            graphics.drawString(this.font, "§7Silah ve zirhlarini urettikten sonra,", centerX - 135, centerY - 55, 0xAAAAAA);
            graphics.drawString(this.font, "§7elinde §dBoyut Cekirdegi§7 varken", centerX - 135, centerY - 45, 0xAAAAAA);
            graphics.drawString(this.font, "§7Altar'a sag tiklayarak asil ritueli baslat.", centerX - 135, centerY - 35, 0xAAAAAA);
            
            graphics.drawString(this.font, "§7Bu eylem elindeki cekirdegi tuketir", centerX - 135, centerY - 15, 0xAAAAAA);
            graphics.drawString(this.font, "§7ve gokyuzunden §cRift Sentinel§7'i indirir.", centerX - 135, centerY - 5, 0xAAAAAA);
            graphics.drawString(this.font, "§8(Not: Rituel sistemini test ettikten", centerX - 135, centerY + 15, 0xAAAAAA);
            graphics.drawString(this.font, "§8hemen sonra uretime [crafting] gececegiz)", centerX - 135, centerY + 25, 0xAAAAAA);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Ekran acikken oyun arkada akmaya devam etsin
    }
}