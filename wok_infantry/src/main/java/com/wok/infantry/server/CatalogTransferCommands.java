package com.wok.infantry.server;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.wok.infantry.configtransfer.CatalogTransferAction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

final class CatalogTransferCommands {
    private CatalogTransferCommands() {}

    static LiteralArgumentBuilder<CommandSourceStack> attach(LiteralArgumentBuilder<CommandSourceStack> parent) {
        return parent.then(Commands.literal("export")
                .then(Commands.argument("file", StringArgumentType.string())
                        .executes(context -> execute(context.getSource(), CatalogTransferAction.EXPORT,
                                StringArgumentType.getString(context, "file"), "", 0))))
                .then(Commands.literal("import")
                        .then(Commands.argument("file", StringArgumentType.string())
                                .executes(context -> execute(context.getSource(), CatalogTransferAction.PREVIEW,
                                        StringArgumentType.getString(context, "file"), "", 0))
                                .then(Commands.literal("confirm")
                                        .then(Commands.argument("token", StringArgumentType.word())
                                                .executes(context -> execute(context.getSource(), CatalogTransferAction.IMPORT,
                                                        StringArgumentType.getString(context, "file"),
                                                        StringArgumentType.getString(context, "token"), 0))))))
                .then(Commands.literal("files")
                        .executes(context -> execute(context.getSource(), CatalogTransferAction.LIST, "", "", 0))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(context -> execute(context.getSource(), CatalogTransferAction.LIST,
                                        "", "", IntegerArgumentType.getInteger(context, "page") - 1))));
    }

    private static int execute(CommandSourceStack source, CatalogTransferAction action,
                               String name, String token, int page) {
        LoadoutService service = LoadoutService.get(source.getServer()).orElse(null);
        if (service == null) {
            source.sendFailure(Component.literal("配装服务尚未启动"));
            return 0;
        }
        var result = service.catalogTransfers().execute(source, action, name, token, page);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), false);
        if (result.pages() > 0) source.sendSuccess(() -> Component.literal(
                "第 " + (result.page() + 1) + "/" + result.pages() + " 页：" + String.join(", ", result.files())), false);
        if (!result.token().isEmpty()) {
            String command = "/loadoutadmin import \"" + com.wok.infantry.configtransfer.CatalogFiles.fileName(name)
                    + "\" confirm " + result.token();
            source.sendSuccess(() -> Component.literal("确认整体替换：" + command).withStyle(style ->
                    style.withColor(net.minecraft.ChatFormatting.GOLD).withClickEvent(
                            new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))), false);
        }
        return 1;
    }
}
