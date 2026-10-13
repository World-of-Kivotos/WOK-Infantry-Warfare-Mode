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
    void registersTestModeSwitchAndTestStartBesideTheVehicleTest() {
        CommandNode<CommandSourceStack> test = BattleCommands.testCommand().build();
        CommandNode<CommandSourceStack> mode = test.getChild("mode");
        assertNotNull(mode, "Missing /battle admin test mode");
        for (String state : new String[] {"on", "off", "status"}) {
            assertNotNull(mode.getChild(state), "Missing test mode " + state);
            assertNotNull(mode.getChild(state).getCommand(), state + " must execute");
        }
        CommandNode<CommandSourceStack> start = test.getChild("start");
        assertNotNull(start, "Missing /battle admin test start");
        assertNotNull(start.getCommand(), "test start without arguments must execute");
        CommandNode<CommandSourceStack> node = start;
        for (String segment : new String[] {"faction", "formation", "player"}) {
            node = node.getChild(segment);
            assertNotNull(node, "Missing test start argument: " + segment);
            assertNotNull(node.getCommand(), "test start must execute after " + segment);
        }
        assertNotNull(test.getChild("vehicle"), "the vehicle test mode stays available");
    }

    @Test
    void parsesTestStartWithFactionAndFormation() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(BattleCommands.testCommand());

        ParseResults<CommandSourceStack> parsed = dispatcher.parse(
                "test start academy millennium_seminar_mobile", null);

        assertTrue(parsed.getReader().getRemaining().isEmpty(),
                "faction and formation ids must be consumed as words");
        assertNotNull(parsed.getContext().build(parsed.getReader().getString()).getCommand(),
                "test start with faction and formation must resolve to the start action");
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
