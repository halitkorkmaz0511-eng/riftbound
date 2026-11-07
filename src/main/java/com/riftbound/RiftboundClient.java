package com.riftbound;

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
