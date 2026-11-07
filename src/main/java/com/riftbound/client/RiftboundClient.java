package com.riftbound.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class RiftboundClient implements ClientModInitializer {
    private static KeyMapping guideKeyBinding;

    @Override
    public void onInitializeClient() {
        // 'Z' tusunu kaydet
        guideKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "Riftbound Rehberi Ac/Kapat",
            GLFW.GLFW_KEY_Z,
            "Riftbound Mod"
        ));

        // Tusa basildiginda ekrani ac
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guideKeyBinding.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new RiftGuideScreen());
                }
            }
        });
    }
}