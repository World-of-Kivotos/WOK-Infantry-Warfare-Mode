package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.TacticalBoardChrome.KeyHint;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The hardware Esc / R keys of the bottom bezel and the key cap they share with the page keys. */
class BezelKeyTest {
    private static final Component NOT_WAITING = Component.literal("只在等待编制目录时可重新请求");

    /** A screen that registers both keys and records what reaches its hooks. */
    private static final class KeyScreen extends TacticalScreen {
        final List<String> events = new ArrayList<>();
        boolean registerKeys = true;
        boolean threeArguments;
        Component refreshBlocked;
        TacticalLivery.Livery livery = TacticalLivery.Livery.NEUTRAL;
        int refreshes;

        KeyScreen() {
            super(Component.literal("keys"));
        }

        void simulateInit() {
            init();
        }

        @Override
        protected void initTactical() {
            if (!registerKeys) {
                return;
            }
            KeyHint refresh = KeyHint.literal("R", Component.literal("刷新"));
            if (threeArguments) {
                setBezelKeys(KeyHint.close(), refresh, () -> refreshes++);
            } else {
                setBezelKeys(KeyHint.back(), refresh, () -> refreshes++, () -> refreshBlocked);
            }
        }

        @Override
        protected TacticalLivery.Livery livery() {
            return livery;
        }

        @Override
        protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
            events.add("key " + keyCode);
            return true;
        }
    }

    /** A screen migrated by renaming only: its key hook still calls the old super method. */
    private static final class LegacyScreen extends TacticalScreen {
        int keyHooks;

        LegacyScreen() {
            super(Component.literal("legacy"));
        }

        void simulateInit() {
            init();
        }

        @Override
        protected void initTactical() {
            setBezelKeys(KeyHint.close(), null, null);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return false;
        }

        @Override
        protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
            keyHooks++;
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    private static final class ModalStub implements TacticalModal {
        @Override
        public void opened(TacticalScreen host) {
        }

        @Override
        public void layout(Font font, int screenWidth, int screenHeight) {
        }

        @Override
        public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY,
                           float partialTick) {
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return true;
        }
    }

    private static KeyScreen screen() {
        KeyScreen screen = new KeyScreen();
        screen.simulateInit();
        return screen;
    }

    // ---- registration and input -----------------------------------------------------------------

    @Test
    void setBezelKeysAddsEscAndRefreshWidgetsWithTheirProbeIds() {
        KeyScreen screen = screen();

        List<BezelKey> keys = screen.bezelKeys();
        assertEquals(2, keys.size());
        assertEquals(BezelKey.Role.ESC, keys.get(0).role());
        assertEquals(BezelKey.Role.REFRESH, keys.get(1).role());
        assertTrue(screen.children().containsAll(keys), "the keys are widgets of the screen");
        assertEquals("shell.key.esc", BezelKey.Role.ESC.uiId());
        assertEquals("shell.key.refresh", BezelKey.Role.REFRESH.uiId());
        assertEquals(TacticalBezelPlan.TAB_ORDER_ESC, keys.get(0).getTabOrderGroup());
        assertEquals(TacticalBezelPlan.TAB_ORDER_REFRESH, keys.get(1).getTabOrderGroup());
        assertTrue(keys.get(0).getMessage().getString().startsWith("Esc "),
                "narrated as key and action");
        assertEquals("R 刷新", keys.get(1).getMessage().getString());
    }

    @Test
    void escDispatchesEscapeToTheScreensOwnKeyHook() {
        KeyScreen screen = screen();

        assertTrue(screen.bezelKeys().get(0).press());

        assertEquals(List.of("key " + GLFW.GLFW_KEY_ESCAPE), screen.events,
                "the screen keeps its own meaning of Esc");
    }

    @Test
    void aClickOnTheEscKeyGoesThroughTheScreensInputDispatch() {
        KeyScreen screen = screen();
        BezelKey esc = screen.bezelKeys().get(0);
        esc.setBounds(new UiRect(10, 300, 60, 312));

        assertTrue(screen.mouseClicked(20.0D, 305.0D, 0));
        assertFalse(screen.mouseClicked(20.0D, 305.0D, 1), "only the left button presses");

        assertEquals(List.of("key " + GLFW.GLFW_KEY_ESCAPE), screen.events);
    }

    @Test
    void escFromAHookThatStillCallsSuperGetsVanillaHandlingWithoutRecursion() {
        LegacyScreen screen = new LegacyScreen();
        screen.simulateInit();

        assertFalse(screen.bezelKeys().get(0).press(), "vanilla Esc with closing turned off");
        assertEquals(1, screen.keyHooks, "the hook ran once, no recursion");
        assertTrue(screen.bezelHints().size() == 1 && screen.bezelKeys().size() == 1,
                "a null refresh hint leaves the R key out");
    }

    @Test
    void refreshRunsTheActionOrIsDisabledWithAReason() {
        KeyScreen screen = screen();
        BezelKey refresh = screen.bezelKeys().get(1);

        assertTrue(refresh.press());
        assertEquals(1, screen.refreshes);
        assertTrue(refresh.active);

        screen.refreshBlocked = NOT_WAITING;
        assertSame(NOT_WAITING, screen.bezelRefreshDisabledReason());
        assertFalse(refresh.press());
        assertEquals(1, screen.refreshes, "a disabled R key never refreshes");
        assertFalse(refresh.active, "drawn disabled; the reason is its hover tooltip");

        screen.refreshBlocked = null;
        assertTrue(refresh.press());
        assertEquals(2, screen.refreshes);
        assertTrue(refresh.active);
        assertTrue(screen.events.isEmpty(), "R never reaches the key hook");
    }

    @Test
    void theThreeArgumentFormKeepsRefreshEnabled() {
        KeyScreen screen = new KeyScreen();
        screen.threeArguments = true;
        screen.simulateInit();

        assertNull(screen.bezelRefreshDisabledReason());
        assertTrue(screen.bezelKeys().get(1).press());
        assertEquals(1, screen.refreshes);
        assertEquals("Esc", screen.bezelHints().get(0).key().getString());
    }

    @Test
    void anOpenModalBlocksBothKeys() {
        KeyScreen screen = screen();
        screen.openModal(new ModalStub());

        assertFalse(screen.bezelKeys().get(0).press());
        assertFalse(screen.bezelKeys().get(1).press());
        assertTrue(screen.events.isEmpty());
        assertEquals(0, screen.refreshes);
    }

    @Test
    void initRemovesTheKeysOfThePreviousBuild() {
        KeyScreen screen = screen();
        List<BezelKey> first = screen.bezelKeys();

        screen.registerKeys = false;
        screen.simulateInit();

        assertTrue(screen.bezelKeys().isEmpty());
        assertTrue(screen.bezelHints().isEmpty());
        assertFalse(screen.children().contains(first.get(0)));
        assertFalse(screen.children().contains(first.get(1)));
        assertNull(screen.bezelRefreshDisabledReason());
    }

    @Test
    void theDeviceIsPaintedInTheScreensLivery() {
        KeyScreen screen = screen();
        assertSame(DeviceSkin.NEUTRAL, screen.deviceSkin());
        screen.livery = TacticalLivery.Livery.CAESAR;
        assertSame(DeviceSkin.CAESAR, screen.deviceSkin());
    }

    // ---- key cap ------------------------------------------------------------------------------

    @Test
    void stateRuleDisabledThenPressedThenHover() {
        assertEquals(BezelKey.CapState.DISABLED, BezelKey.state(false, true, true));
        assertEquals(BezelKey.CapState.DOWN, BezelKey.state(true, true, true));
        assertEquals(BezelKey.CapState.HOVER, BezelKey.state(true, false, true));
        assertEquals(BezelKey.CapState.RAISED, BezelKey.state(true, false, false));
        assertEquals("NORMAL", BezelKey.CapState.RAISED.probeState());
        assertEquals("CURRENT", BezelKey.CapState.DOWN.probeState());
        assertEquals("DISABLED", BezelKey.CapState.DISABLED.probeState());
    }

    @Test
    void capColoursComeFromTheDeviceSkin() {
        for (DeviceSkin skin : List.of(DeviceSkin.ACADEMY, DeviceSkin.CAESAR, DeviceSkin.NEUTRAL)) {
            BezelKey.Cap raised = BezelKey.cap(skin, BezelKey.CapState.RAISED);
            assertEquals(new BezelKey.Cap(skin.line(), skin.key(), skin.keyHi(), skin.keyLo(),
                    skin.keyText(), skin.keySub(), false, 0), raised);

            BezelKey.Cap hover = BezelKey.cap(skin, BezelKey.CapState.HOVER);
            assertEquals(skin.keyHi(), hover.face(), "hover lights the face to KEY_HI");
            assertEquals(skin.keyLo(), hover.bottomLip());

            BezelKey.Cap down = BezelKey.cap(skin, BezelKey.CapState.DOWN);
            assertEquals(skin.keyDown(), down.face());
            assertEquals(skin.keyLo(), down.topLip(), "a pressed key is shaded at the top");
            assertEquals(0, down.bottomLip());
            assertEquals(TacticalBoardTheme.SELECT_B, down.text(), "current page = SELECT_B");
            assertEquals(1, down.textDrop());

            BezelKey.Cap disabled = BezelKey.cap(skin, BezelKey.CapState.DISABLED);
            assertEquals(skin.keySub(), disabled.text(), "disabled writes in KEY_SUB");
            assertTrue(disabled.hatch(), "and is hatched");
            assertEquals(skin.line(), disabled.edge());
        }
    }

    @Test
    void pageLedsUseTheLiveryLedAndASharedOffColour() {
        assertEquals(0xFF8AC4F5, BezelKey.ledColor(DeviceSkin.ACADEMY, true));
        assertEquals(0xFFFFE4E8, BezelKey.ledColor(DeviceSkin.CAESAR, true),
                "Caesar's warm white: a red LED would read as link lost");
        assertEquals(0xFF12A3B4, BezelKey.ledColor(DeviceSkin.NEUTRAL, true));
        for (DeviceSkin skin : List.of(DeviceSkin.ACADEMY, DeviceSkin.CAESAR, DeviceSkin.NEUTRAL)) {
            assertEquals(BezelKey.LED_OFF, BezelKey.ledColor(skin, false));
        }
        assertEquals(0x508AC4F5, BezelKey.ledGlow(DeviceSkin.ACADEMY), "glow alpha 0x50");
        assertEquals(0x5012A3B4, BezelKey.ledGlow(DeviceSkin.NEUTRAL));
    }

    @Test
    void escAndRefreshLabelsAreCentredAsOneGroup() {
        // "Esc 返回" on the standard 52px Esc key: 18 + 4 + 18 = 40px, 6px either side.
        BezelKey.Label label = BezelKey.label(new UiRect(34, 339, 86, 352), 18, 18);
        assertEquals(40, label.keyX());
        assertEquals(62, label.actionX());
        assertTrue(label.fits());

        assertFalse(BezelKey.label(new UiRect(0, 0, 44, 12), 18, 18).fits(),
                "too narrow: only the key name, the rest on hover");
        BezelKey.Label nameOnly = BezelKey.label(new UiRect(0, 0, 30, 12), 18, 0);
        assertEquals(6, nameOnly.keyX());
        assertTrue(nameOnly.fits());
    }

    @Test
    void pressedLabelsDropOnePixel() {
        UiRect key = new UiRect(0, 339, 48, 352);
        DeviceSkin skin = DeviceSkin.ACADEMY;

        assertEquals(341, BezelKey.textY(key, BezelKey.cap(skin, BezelKey.CapState.RAISED)));
        assertEquals(342, BezelKey.textY(key, BezelKey.cap(skin, BezelKey.CapState.DOWN)));
    }
}
