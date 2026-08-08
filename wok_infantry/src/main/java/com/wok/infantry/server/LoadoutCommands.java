package com.wok.infantry.server;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class LoadoutCommands {
    private LoadoutCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("loadout")
                .executes(context -> openPlayer(context.getSource().getPlayerOrException()))
                .then(Commands.literal("apply")
                        .executes(context -> apply(context.getSource().getPlayerOrException()))));

        dispatcher.register(Commands.literal("loadoutadmin")
                .requires(source -> source.hasPermission(LoadoutService.ADMIN_PERMISSION_LEVEL))
                .executes(context -> openAdmin(context.getSource().getPlayerOrException())));
    }

    private static int openPlayer(ServerPlayer player) {
        LoadoutService.get(player).ifPresent(service -> service.openPlayerScreen(player));
        return 1;
    }

    private static int openAdmin(ServerPlayer player) {
        LoadoutService.get(player).ifPresent(service -> service.openAdminScreen(player));
        return 1;
    }

    private static int apply(ServerPlayer player) {
        return LoadoutService.get(player).map(service -> service.applyLoadout(player) ? 1 : 0).orElse(0);
    }
}
