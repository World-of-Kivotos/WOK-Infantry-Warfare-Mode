package com.wok.infantry.server;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatalogTransferCommandsTest {
    @Test void parsesExportPreviewConfirmAndPagedFileListingIncludingQuotedChineseNames() {
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        dispatcher.register(CatalogTransferCommands.attach(Commands.literal("loadoutadmin")));
        for (String command : new String[]{"loadoutadmin export \"学院军.json\"",
                "loadoutadmin import test", "loadoutadmin import test confirm 1234-abcd",
                "loadoutadmin files", "loadoutadmin files 2"}) {
            var result = dispatcher.parse(command, null);
            assertFalse(result.getReader().canRead(), command);
            assertNotNull(result.getContext().build(command).getCommand(), command);
        }
    }
}
