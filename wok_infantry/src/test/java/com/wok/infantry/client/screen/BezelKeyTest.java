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

        void registerStrip(TacticalTabStrip strip) {
            setTabStrip(strip);
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
    void placingTheKeysAlsoMovesARegisteredBezelStripBetweenThem() {
        KeyScreen screen = screen();
        TacticalTabStrip strip = BattleTab.strip(BattleTab.SQUADS, tab -> null, tab -> { });
        screen.registerStrip(strip);
        TacticalBezelPlan plan = TacticalBezelPlan.plan(new UiRect(22, 332, 618, 355),
                TacticalShellLayout.Density.STANDARD, 52, 40);

        screen.placeBezelKeys(plan);

        List<BezelKey> keys = screen.bezelKeys();
        assertEquals(plan.esc(), bounds(keys.get(0)));
        assertEquals(plan.refresh(), bounds(keys.get(1)));
        assertEquals(plan.pages(), bounds(strip), "page keys between Esc and R");
        assertSame(plan, strip.bezelPlan(false));

        // Any other skin stays where its screen put it.
        TacticalTabStrip header = new TacticalTabStrip(TacticalTabStrip.Skin.HEADER,
                BattleTab.tabs(null), 0, index -> { });
        header.setBounds(5, 5, 50, 10);
        screen.registerStrip(header);
        screen.placeBezelKeys(plan);
        assertEquals(new UiRect(5, 5, 55, 15), bounds(header));
    }

    private static UiRect bounds(net.minecraft.client.gui.components.AbstractWidget widget) {
        return UiRect.ofSize(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
    }

    @Test
    void theDeviceIsPaintedInTheLiveryResolvedForTheFrame() {
        KeyScreen screen = screen();
        assertSame(DeviceSkin.NEUTRAL, screen.deviceSkin(), "before the first frame");
        screen.livery = TacticalLivery.Livery.CAESAR;
        assertSame(DeviceSkin.NEUTRAL, screen.deviceSkin(),
                "a livery change mid-frame never repaints the keys of that frame");
        screen.framePalette();
        assertSame(DeviceSkin.CAESAR, screen.deviceSkin(), "the next frame follows the livery");
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
            assertEquals(skin.keyHover(), hover.face(), "hover lights the face toward KEY_HI");
            assertEquals(skin.keyHi(), hover.topLip());
            assertEquals(skin.keyLo(), hover.bottomLip());
            assertEquals(skin.keyText(), hover.text());
            assertEquals(skin.keySub(), hover.sub());

            BezelKey.Cap down = BezelKey.cap(skin, BezelKey.CapState.DOWN);
            assertEquals(skin.keyDown(), down.face());
            assertEquals(skin.keyLo(), down.topLip(), "a pressed key is shaded at the top");
            assertEquals(0, down.bottomLip());
            assertEquals(TacticalBoardTheme.SELECT_B, down.text(), "current page = SELECT_B");
            assertEquals(1, down.textDrop());

            BezelKey.Cap disabled = BezelKey.cap(skin, BezelKey.CapState.DISABLED);
            assertEquals(skin.keyOff(), disabled.text(), "disabled writes in the dimmed keyOff");
            assertEquals(skin.keyOff(), disabled.sub(), "both labels");
            assertTrue(disabled.hatch(), "and is hatched");
            assertEquals(skin.key(), disabled.face());
            assertEquals(0, disabled.topLip(), "flat: no lit upper lip");
            assertEquals(skin.keyLo(), disabled.bottomLip());
            assertEquals(skin.line(), disabled.edge());
        }
    }

    @Test
    void aDisabledKeyReadsButIsPlainlyDimmerThanAnEnabledOneInEveryLivery() {
        // UI rule 6: disabled must read distinct. Before B8 the disabled Caesar key name was
        // 1.21:1 from the enabled one and its action label the same colour as an enabled one.
        for (DeviceSkin skin : List.of(DeviceSkin.ACADEMY, DeviceSkin.CAESAR, DeviceSkin.NEUTRAL)) {
            BezelKey.Cap enabled = BezelKey.cap(skin, BezelKey.CapState.RAISED);
            BezelKey.Cap disabled = BezelKey.cap(skin, BezelKey.CapState.DISABLED);
            String name = skinName(skin);
            assertTrue(contrast(disabled.text(), disabled.face()) >= 3.0,
                    name + ": disabled labels still read at 3:1 on KEY (the disabled-label floor)");
            assertTrue(contrast(disabled.text(), enabled.text()) >= 2.0,
                    name + ": the key name dims by at least 2:1 from KEY_TEXT");
            assertTrue(contrast(disabled.sub(), enabled.sub()) >= 1.4,
                    name + ": the action label dims visibly from KEY_SUB");
            assertTrue(contrast(enabled.text(), enabled.face()) >= 4.5,
                    name + ": an enabled key name is body text");
            assertTrue(contrast(enabled.sub(), enabled.face()) >= 3.0,
                    name + ": an enabled action label is secondary text");
        }
    }

    @Test
    void hoverLightsTheKeyOnlyAsFarAsBothLabelsStillRead() {
        for (DeviceSkin skin : List.of(DeviceSkin.ACADEMY, DeviceSkin.CAESAR, DeviceSkin.NEUTRAL)) {
            BezelKey.Cap hover = BezelKey.cap(skin, BezelKey.CapState.HOVER);
            String name = skinName(skin);
            assertTrue(contrast(hover.text(), hover.face()) >= 4.5,
                    name + ": hovered key name at 4.5:1");
            assertTrue(contrast(hover.sub(), hover.face()) >= 3.0,
                    name + ": hovered action label at 3:1");
            assertTrue(contrast(hover.face(), skin.key()) >= 1.15,
                    name + ": the hover face still differs from the raised one");
        }
        assertEquals(DeviceSkin.CAESAR.keyHi(), DeviceSkin.CAESAR.keyHover(), "preview KEY_HI kept");
        assertEquals(DeviceSkin.NEUTRAL.keyHi(), DeviceSkin.NEUTRAL.keyHover(), "preview KEY_HI kept");
        assertFalse(DeviceSkin.ACADEMY.keyHi() == DeviceSkin.ACADEMY.keyHover(),
                "Academy's KEY_SUB is 2.69:1 on KEY_HI, so its hover face stops short of it");
    }

    private static String skinName(DeviceSkin skin) {
        return skin == DeviceSkin.ACADEMY ? "ACADEMY" : skin == DeviceSkin.CAESAR ? "CAESAR"
                : skin == DeviceSkin.NEUTRAL ? "NEUTRAL" : skin.toString();
    }

    private static double contrast(int ink, int ground) {
        return TacticalPaletteContrastTest.contrast(ink, ground);
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
