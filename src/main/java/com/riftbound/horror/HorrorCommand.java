package com.riftbound.horror;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// EKSİK OLAN KÜTÜPHANELER EKLENDİ
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HorrorCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommand(dispatcher);
        });
    }

    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("riftbound_test")
            .requires(source -> source.hasPermission(2))
            
            // ALT KOMUT: stop
            .then(Commands.literal("stop")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    HorrorState state = player.serverLevel().getDataStorage().computeIfAbsent(HorrorState.factory(), "riftbound_horror_state");
                    state.endSequence(player.getUUID(), player.serverLevel().getGameTime());
                    player.removeAllEffects();
                    context.getSource().sendSuccess(() -> Component.literal("Tum korku sekanslari durduruldu."), false);
                    return 1;
                })
            )

            // ALT KOMUT: var
            .then(Commands.literal("var")
                .then(Commands.argument("variation", IntegerArgumentType.integer(0, 3))
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        int var = IntegerArgumentType.getInteger(context, "variation");
                        
                        HorrorState state = player.serverLevel().getDataStorage().computeIfAbsent(HorrorState.factory(), "riftbound_horror_state");
                        state.forcedVariation.put(player.getUUID(), var);
                        state.startSequence(player.getUUID(), "riftbound:reality_corruption", player.serverLevel().getGameTime());
                        
                        context.getSource().sendSuccess(() -> Component.literal("Reality Corruption (Var: " + var + ") baslatildi."), false);
                        return 1;
                    })
                )
            )
            
            // ALT KOMUT: debug
            .then(Commands.literal("debug")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    HorrorState state = player.serverLevel().getDataStorage().computeIfAbsent(HorrorState.factory(), "riftbound_horror_state");
                    UUID uuid = player.getUUID();
                    float stress = state.undergroundStress.getOrDefault(uuid, 0f);
                    
                    context.getSource().sendSuccess(() -> Component.literal("§6--- RIFTBOUND DIRECTOR DEBUG ---"), false);
                    context.getSource().sendSuccess(() -> Component.literal("§eUnderground Stress: §f" + String.format("%.1f", stress) + "/100"), false);
                    
                    long currentTick = player.serverLevel().getGameTime();
                    long lastGlobal = state.lastGlobalEventTick.getOrDefault(uuid, 0L);
                    long globalCooldownLeft = Math.max(0, 6000 - (currentTick - lastGlobal));
                    context.getSource().sendSuccess(() -> Component.literal("§eGlobal Cooldown: §f" + (globalCooldownLeft/20) + " saniye kaldi."), false);
                    
                    context.getSource().sendSuccess(() -> Component.literal("§6--- FEAR HISTORY ---"), false);
                    Map<String, Integer> counts = state.eventPlayCounts.getOrDefault(uuid, new HashMap<>());
                    Map<String, Long> lastSeen = state.eventLastSeen.getOrDefault(uuid, new HashMap<>());
                    
                    if (counts.isEmpty()) {
                        context.getSource().sendSuccess(() -> Component.literal("§7Henuz bir olay yasanmadi."), false);
                    } else {
                        for (String evId : counts.keySet()) {
                            long ticksAgo = currentTick - lastSeen.getOrDefault(evId, 0L);
                            int minsAgo = (int) (ticksAgo / 1200);
                            context.getSource().sendSuccess(() -> Component.literal("§b" + evId + " §f- Gosterim: " + counts.get(evId) + " | Son Gorulme: " + minsAgo + " dk once"), false);
                        }
                    }
                    return 1;
                })
            )

            // ALT KOMUT: event
            .then(Commands.literal("event")
                .then(Commands.argument("eventId", StringArgumentType.greedyString())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        String eventId = StringArgumentType.getString(context, "eventId");
                        
                        if (HorrorEventRegistry.getEvent(eventId) != null) {
                            HorrorState state = player.serverLevel().getDataStorage().computeIfAbsent(HorrorState.factory(), "riftbound_horror_state");
                            state.startSequence(player.getUUID(), eventId, player.serverLevel().getGameTime());
                            context.getSource().sendSuccess(() -> Component.literal(eventId + " baslatildi."), false);
                            return 1;
                        } else {
                            context.getSource().sendFailure(Component.literal("Bilinmeyen event ID: " + eventId));
                            return 0;
                        }
                    })
                )
            )
        );
    }
}