package com.riftbound.command;

import com.riftbound.world.RiftMonolithGenerator;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import static net.minecraft.commands.Commands.literal;

public class RiftCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("riftbound")
                .then(literal("spawn_monolith").executes(context -> {
                    ServerLevel level = context.getSource().getLevel();
                    BlockPos pos = BlockPos.containing(context.getSource().getPosition());
                    RiftMonolithGenerator.buildMonolith(level, pos);
                    context.getSource().sendSuccess(() -> Component.literal("Rift Monolith basariyla insa edildi!"), false);
                    return 1;
                }))
            );
        });
    }
}