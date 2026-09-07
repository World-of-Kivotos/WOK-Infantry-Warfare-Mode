package com.wok.infantry.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleCommandsRegistrationTest {
    @Test
    void registersSingleSupportCooldownClearCommand() {
        CommandNode<CommandSourceStack> node = BattleCommands.supportAdminCommand().build();
        for (String segment : new String[] {
                "cooldown", "clear", "faction", "supportId"
        }) {
            node = node.getChild(segment);
            assertNotNull(node, "Missing command node: " + segment);
        }
        assertNotNull(node.getCommand(), "supportId node must execute the clear action");
    }

    @Test
    void parsesNamespacedSupportIdWithoutTrailingInput() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(BattleCommands.supportAdminCommand());

        ParseResults<CommandSourceStack> parsed = dispatcher.parse(
                "support cooldown clear blue "
                        + "wok_commander_support:millennium_f15ex_jdam_1000lb",
                null);

        assertTrue(parsed.getReader().getRemaining().isEmpty(),
                "Namespaced support ID must be consumed through its colon");
        assertNotNull(parsed.getContext().build(parsed.getReader().getString()).getCommand(),
                "Fully parsed command must resolve to the cooldown clear action");
    }
}
