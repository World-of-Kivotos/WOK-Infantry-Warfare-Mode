package com.wok.infantry.client.hud;

import com.wok.infantry.client.screen.TacticalScreen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plan 4.9: while a WOK步战 terminal is open, the D2 device floats on the dimmed world, so the
 * core's own HUD overlays stand aside; F1 still hides everything, and add-on slots follow F1 only.
 */
class HudFrameTerminalTest {
    private static final class Terminal extends TacticalScreen {
        Terminal() {
            super(Component.literal("terminal"));
        }

        @Override
        protected void initTactical() {
        }
    }

    @Test
    void coreOverlaysStandAsideForF1AndForAnOpenTerminal() {
        assertFalse(HudFrame.coreHidden(false, false));
        assertTrue(HudFrame.coreHidden(true, false), "F1");
        assertTrue(HudFrame.coreHidden(false, true), "terminal open");
        assertTrue(HudFrame.coreHidden(true, true));
    }

    @Test
    void onlyTabletTerminalsCountAsOpenTerminals() {
        assertTrue(HudFrame.terminalOpen(new Terminal()));
        assertFalse(HudFrame.terminalOpen(null), "no screen: the plain HUD");
        assertFalse(HudFrame.terminalOpen(new ChatScreen("")),
                "the chat keeps the HUD (its roster shrinks instead)");
    }

    @Test
    void anOpenTerminalHidesTheCoreButNotTheAddOnSlots() {
        WokHudLayout.Layout layout = WokHudLayout.compute(WokHudLayout.Input.screen(320, 240, 1)
                .withRoster(6, false).withStrip(true));
        HudFrame frame = new HudFrame(1L, 320, 240, 1, false, true, false, false, false, false,
                false, WokHudLayout.RosterPresence.FULL, null, null, null, null, List.of(), null,
                null, false, layout);
        assertTrue(frame.coreHidden(), "roster, strip, ballot and stamina stand aside");
        assertFalse(frame.hidden(), "InfantryHudApi still hands out its slots (add-ons stay put)");
        assertTrue(InfantryHudApi.slot(frame.layout(), InfantryHudApi.TOP_CENTER_NEXT) != null);
    }
}
