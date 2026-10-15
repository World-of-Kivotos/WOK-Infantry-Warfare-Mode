package com.wok.infantry.client.tablet;

import com.wok.infantry.client.screen.PlayerLoadoutScreen;
import com.wok.infantry.client.screen.SquadScreen;
import com.wok.infantry.client.screen.TacticalMapScreen;
import com.wok.infantry.client.screen.TacticalScreen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The tablet family (DESIGN 2.4) and how each member is animated (IMPL_PLAN 4.2). */
class TabletSurfaceKindTest {
    private static final class Gallery extends TacticalScreen {
        Gallery() {
            super(Component.literal("gallery"));
        }

        @Override
        protected void initTactical() {
        }
    }

    @Test
    void everyTacticalScreenIsATerminalThatOnlyEscCloses() {
        TacticalScreen screen = new Gallery();
        assertEquals(TabletScreenKind.TERMINAL, TabletSurface.kindOf(screen));
        assertTrue(TabletScreenKind.TERMINAL.isTerminal());
        assertFalse(TabletScreenKind.TERMINAL.closesOnTerminalKey());
    }

    @Test
    void theSquadScreenAlsoClosesOnTheTerminalKey() throws Exception {
        assertEquals(SquadScreen.class, SquadScreen.class.getDeclaredMethod("tabletKind")
                .getDeclaringClass(), "the squad screen says SQUAD itself");
        assertTrue(TabletScreenKind.SQUAD.closesOnTerminalKey());
        assertTrue(TabletScreenKind.SQUAD.isTerminal());
    }

    @Test
    void theMapAndTheLoadoutPageAreFullScreenTabletPages() {
        for (Class<?> page : new Class<?>[]{TacticalMapScreen.class, PlayerLoadoutScreen.class}) {
            assertTrue(TabletSurface.class.isAssignableFrom(page), page.getSimpleName());
            assertFalse(TacticalScreen.class.isAssignableFrom(page), page.getSimpleName()
                    + " has no device hooks: Render.Pre/Post animates it");
            assertThrows(NoSuchMethodException.class, () -> page.getDeclaredMethod("tabletKind"),
                    page.getSimpleName() + " keeps the default FULLSCREEN");
        }
        assertFalse(TabletScreenKind.FULLSCREEN.isTerminal());
    }

    @Test
    void otherScreensAreNotTablets() {
        assertNull(TabletSurface.kindOf(new ChatScreen("")));
        assertNull(TabletSurface.kindOf(null));
    }
}
