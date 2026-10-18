package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.TacticalBoardChrome.KeyHint;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Device hooks of {@link TacticalScreen}: palette defaults and the bezel key registration. */
class TacticalScreenHooksTest {
    private static final class HookScreen extends TacticalScreen {
        boolean registerKeys = true;
        int refreshes;

        HookScreen() {
            super(Component.literal("hooks"));
        }

        void simulateInit() {
            init();
        }

        @Override
        protected void initTactical() {
            if (registerKeys) {
                setBezelKeys(KeyHint.back(), KeyHint.literal("R", Component.literal("刷新")),
                        () -> refreshes++);
            }
        }
    }

    @Test
    void paletteDefaultsFollowTheViewerOnTheBoardScope() {
        HookScreen screen = new HookScreen();

        assertEquals(TacticalLivery.current(), screen.livery());
        assertEquals(TacticalLivery.Scope.BOARD, screen.paletteScope());
    }

    @Test
    void bezelKeysAreRegisteredPerInitWithEscFirst() {
        HookScreen screen = new HookScreen();
        screen.simulateInit();

        List<KeyHint> hints = screen.bezelHints();
        assertEquals(2, hints.size());
        assertEquals("Esc", hints.get(0).key().getString());
        assertEquals("R", hints.get(1).key().getString());
        assertNotNull(screen.bezelRefresh());
        screen.bezelRefresh().run();
        assertEquals(1, screen.refreshes);

        screen.registerKeys = false;
        screen.simulateInit();
        assertTrue(screen.bezelHints().isEmpty(), "a rebuild without keys clears them");
        assertNull(screen.bezelRefresh());
    }
}
