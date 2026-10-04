package com.wok.infantry.client.screen;

import com.wok.infantry.client.screen.TacticalConfirmDialog.KeyAction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.tooltip.BelowOrAboveWidgetTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.MenuTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Modal layer, tab shortcut and coordinate conversion of {@link TacticalScreen}. */
class TacticalScreenModalTest {
    /** Minimal tablet screen that records what reaches it. */
    private static final class TestScreen extends TacticalScreen {
        int initCount;
        String draft = "";
        int scroll;
        final List<String> events = new ArrayList<>();
        final List<Integer> selectedTabs = new ArrayList<>();
        TacticalTabStrip strip;

        TestScreen() {
            super(Component.literal("test"));
        }

        void simulateInit() {
            init();
        }

        @Override
        protected void initTactical() {
            initCount++;
            strip = addRenderableWidget(BattleTab.strip(BattleTab.SQUADS, tab -> null,
                    tab -> selectedTabs.add(tab.ordinal())));
            setTabStrip(strip);
        }

        @Override
        protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
            events.add("click " + mouseX + "," + mouseY);
            return true;
        }

        @Override
        protected boolean onMouseDragged(double mouseX, double mouseY, int button,
                                         double dragX, double dragY) {
            events.add("drag " + mouseX + "," + mouseY + " d " + dragX + "," + dragY);
            return true;
        }

        @Override
        protected boolean onMouseScrolled(double mouseX, double mouseY, double delta) {
            events.add("scroll " + mouseX + "," + mouseY + " " + delta);
            return true;
        }

        @Override
        protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
            events.add("key " + keyCode);
            return true;
        }

        @Override
        protected boolean onCharTyped(char codePoint, int modifiers) {
            draft += codePoint;
            return true;
        }
    }

    /** Records the logical coordinates a modal receives. */
    private static final class RecordingModal implements TacticalModal {
        final List<String> events = new ArrayList<>();

        @Override
        public void opened(TacticalScreen host) {
        }

        @Override
        public void layout(Font font, int screenWidth, int screenHeight) {
        }

        @Override
        public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            events.add("click " + mouseX + "," + mouseY);
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            events.add("key " + keyCode);
            return true;
        }
    }

    private static TestScreen screen() {
        TestScreen screen = new TestScreen();
        screen.simulateInit();
        return screen;
    }

    private static TacticalConfirmDialog dialog(boolean danger, List<String> log) {
        return TacticalConfirmDialog.builder(Component.literal("解散小队"),
                        Component.literal("阿尔法小队的 6 名成员会被移出小队。"))
                .danger(danger)
                .onConfirm(() -> log.add("confirm"))
                .onCancel(() -> log.add("cancel"))
                .build();
    }

    // ---- pure key rule -----------------------------------------------------------------------

    @Test
    void dangerousConfirmationNeverReactsToEnterOrSpace() {
        for (int key : new int[]{GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE}) {
            assertEquals(KeyAction.NONE, TacticalConfirmDialog.keyAction(key, 0, true, true));
            assertEquals(KeyAction.CANCEL, TacticalConfirmDialog.keyAction(key, 0, true, false),
                    "Enter on the focused cancel key still cancels");
            assertEquals(KeyAction.CONFIRM, TacticalConfirmDialog.keyAction(key, 0, false, true));
        }
        assertEquals(KeyAction.CANCEL,
                TacticalConfirmDialog.keyAction(GLFW.GLFW_KEY_ESCAPE, 0, true, true));
        assertEquals(KeyAction.FOCUS_NEXT,
                TacticalConfirmDialog.keyAction(GLFW.GLFW_KEY_TAB, 0, true, false));
        assertEquals(KeyAction.FOCUS_PREVIOUS, TacticalConfirmDialog.keyAction(GLFW.GLFW_KEY_TAB,
                GLFW.GLFW_MOD_SHIFT, true, false));
        assertEquals(KeyAction.NONE, TacticalConfirmDialog.keyAction(GLFW.GLFW_KEY_Y, 0, false, true));
    }

    @Test
    void cardFitsEveryTier() {
        assertEquals(240, TacticalConfirmDialog.cardWidth(320, true));
        assertEquals(300, TacticalConfirmDialog.cardWidth(640, false));
        assertEquals(224, TacticalConfirmDialog.cardWidth(240, true), "16px side margin");
        for (int height : new int[]{240, 270, 336, 360}) {
            int buttonHeight = height < 280 ? 14 : 18;
            int lines = TacticalConfirmDialog.maxBodyLines(height, buttonHeight);
            assertTrue(TacticalConfirmDialog.cardHeight(lines, buttonHeight) <= height - 8,
                    "card fits " + height);
        }
    }

    // ---- modal layer in a screen -------------------------------------------------------------

    @Test
    void dangerousDialogIgnoresEnterAndEscCancelsWithoutReinitialisingTheScreen() {
        TestScreen screen = screen();
        screen.draft = "B 小队集合点";
        screen.scroll = 7;
        List<String> log = new ArrayList<>();
        TacticalConfirmDialog dialog = dialog(true, log);

        screen.openModal(dialog);
        assertFalse(dialog.confirmFocused(), "dangerous dialogs focus cancel first");
        screen.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0);
        assertTrue(dialog.confirmFocused());
        screen.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
        screen.keyPressed(GLFW.GLFW_KEY_KP_ENTER, 0, 0);
        screen.keyPressed(GLFW.GLFW_KEY_SPACE, 0, 0);
        assertTrue(log.isEmpty(), "Enter/Space never confirm a dangerous action");
        assertTrue(screen.hasModal());
        assertTrue(screen.events.isEmpty(), "keys do not reach the screen under the modal");

        screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
        assertEquals(List.of("cancel"), log);
        assertFalse(screen.hasModal(), "Esc closes the modal, not the screen");
        assertEquals(1, screen.initCount, "closing the modal does not re-init the screen");
        assertEquals("B 小队集合点", screen.draft);
        assertEquals(7, screen.scroll);
    }

    @Test
    void normalDialogConfirmsWithEnterAndClosesBeforeTheCallback() {
        TestScreen screen = screen();
        List<String> log = new ArrayList<>();
        TacticalConfirmDialog dialog = TacticalConfirmDialog.builder(Component.literal("加入阵营"),
                        Component.literal("加入后本轮不能更换阵营。"))
                .onConfirm(() -> log.add("confirm, modal open=" + screen.hasModal()))
                .build();

        screen.openModal(dialog);
        assertTrue(dialog.confirmFocused(), "normal dialogs focus confirm first");
        screen.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);

        assertEquals(List.of("confirm, modal open=false"), log);
        assertTrue(dialog.closed());
        dialog.confirm();
        assertEquals(1, log.size(), "a closed dialog never fires twice");
    }

    @Test
    void typingAndTabShortcutAreBlockedWhileAModalIsOpen() {
        TestScreen screen = screen();
        screen.openModal(dialog(true, new ArrayList<>()));

        screen.charTyped('x', 0);
        screen.keyPressed(GLFW.GLFW_KEY_TAB, 0, GLFW.GLFW_MOD_CONTROL);

        assertEquals("", screen.draft);
        assertTrue(screen.selectedTabs.isEmpty(), "Ctrl+Tab goes to the modal, not to the tabs");
        screen.closeModal();
        screen.charTyped('x', 0);
        assertEquals("x", screen.draft);
    }

    @Test
    void ctrlTabCyclesTheRegisteredStripAndOtherKeysReachTheScreen() {
        TestScreen screen = screen();

        assertTrue(screen.keyPressed(GLFW.GLFW_KEY_TAB, 0, GLFW.GLFW_MOD_CONTROL));
        assertTrue(screen.keyPressed(GLFW.GLFW_KEY_TAB, 0,
                GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT));
        screen.keyPressed(GLFW.GLFW_KEY_R, 0, 0);

        assertEquals(List.of(BattleTab.CLASSES.ordinal(), BattleTab.FORMATION.ordinal()),
                screen.selectedTabs);
        assertEquals(List.of("key " + GLFW.GLFW_KEY_R), screen.events);
        assertSame(screen.strip, screen.tabStrip());
    }

    @Test
    void initClearsTheStripRegistrationAndReRegistersIt() {
        TestScreen screen = screen();
        TacticalTabStrip first = screen.strip;

        screen.simulateInit();

        assertEquals(2, screen.initCount);
        assertSame(screen.strip, screen.tabStrip());
        assertFalse(first == screen.tabStrip(), "rebuilt widgets get a fresh strip");
    }

    @Test
    void mouseCoordinatesAreConvertedOnceAtTwoX() {
        TestScreen screen = screen();
        screen.applyUiScale(2);

        screen.mouseClicked(201.0D, 99.0D, 0);
        screen.mouseDragged(40.0D, 60.0D, 0, 4.0D, -2.0D);
        screen.mouseScrolled(10.0D, 20.0D, 1.0D);

        assertEquals(List.of("click 100.5,49.5", "drag 20.0,30.0 d 2.0,-1.0",
                "scroll 5.0,10.0 1.0"), screen.events);
        assertEquals(2, screen.uiScale());
    }

    @Test
    void modalReceivesLogicalCoordinatesToo() {
        TestScreen screen = screen();
        screen.applyUiScale(2);
        RecordingModal modal = new RecordingModal();

        screen.openModal(modal);
        screen.mouseClicked(300.0D, 100.0D, 0);
        screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);

        assertEquals(List.of("click 150.0,50.0", "key " + GLFW.GLFW_KEY_ESCAPE), modal.events);
        assertTrue(screen.events.isEmpty());
        assertTrue(screen.hasModal(), "a custom modal decides itself when to close");
    }

    /** A screen migrated by renaming only: its hooks still call the old super methods. */
    private static final class LegacyHookScreen extends TacticalScreen {
        final List<String> widgetClicks = new ArrayList<>();

        LegacyHookScreen() {
            super(Component.literal("legacy"));
        }

        void simulateInit() {
            init();
        }

        @Override
        protected void initTactical() {
            addRenderableWidget(new AbstractWidget(0, 0, 400, 300, Component.empty()) {
                @Override
                public boolean mouseClicked(double mouseX, double mouseY, int button) {
                    widgetClicks.add(mouseX + "," + mouseY);
                    return true;
                }

                @Override
                protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
                                            float partialTick) {
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput output) {
                }
            });
        }

        @Override
        protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Test
    void hooksCallingTheOldSuperMethodsNeitherConvertTwiceNorRecurse() {
        LegacyHookScreen screen = new LegacyHookScreen();
        screen.simulateInit();
        screen.applyUiScale(2);

        assertTrue(screen.mouseClicked(201.0D, 99.0D, 0));
        assertEquals(List.of("100.5,49.5"), screen.widgetClicks, "divided exactly once");
        assertFalse(screen.keyPressed(GLFW.GLFW_KEY_R, 0, 0), "vanilla handling, no recursion");
    }

    /** A board whose widget opens a modal from its own mouseClicked, like a "解散" key. */
    private static final class OpenerScreen extends TacticalScreen {
        final List<String> boardEvents = new ArrayList<>();
        AbstractWidget opener;
        AbstractWidget other;
        TacticalModal next;

        OpenerScreen() {
            super(Component.literal("opener"));
        }

        void simulateInit() {
            init();
        }

        private AbstractWidget key(int x, boolean opensModal) {
            return new AbstractWidget(x, 10, 80, 20, Component.empty()) {
                @Override
                public boolean mouseClicked(double mouseX, double mouseY, int button) {
                    if (!isMouseOver(mouseX, mouseY)) {
                        return false;
                    }
                    if (opensModal) {
                        openModal(next);
                    }
                    return true;
                }

                @Override
                protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
                                            float partialTick) {
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput output) {
                }
            };
        }

        @Override
        protected void initTactical() {
            opener = addRenderableWidget(key(10, true));
            other = addRenderableWidget(key(200, false));
        }

        @Override
        protected boolean onMouseReleased(double mouseX, double mouseY, int button) {
            boardEvents.add("release");
            return super.onMouseReleased(mouseX, mouseY, button);
        }

        @Override
        protected boolean onMouseDragged(double mouseX, double mouseY, int button,
                                         double dragX, double dragY) {
            boardEvents.add("drag");
            return true;
        }
    }

    /** Modal that closes itself on the press, as the confirm and cancel keys do. */
    private static final class ClosingModal implements TacticalModal {
        TacticalScreen host;

        @Override
        public void opened(TacticalScreen screen) {
            host = screen;
        }

        @Override
        public void layout(Font font, int screenWidth, int screenHeight) {
        }

        @Override
        public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            host.closeModal(this);
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return true;
        }
    }

    @Test
    void aModalOpenedByAClickOwnsTheFocusAndThePressThatClosesIt() {
        OpenerScreen screen = new OpenerScreen();
        screen.simulateInit();
        screen.next = new ClosingModal();

        screen.mouseClicked(20.0D, 15.0D, 0);
        assertTrue(screen.hasModal());
        assertEquals(null, screen.getFocused(), "nothing under the modal keeps the focus");
        assertFalse(screen.isDragging());
        screen.mouseReleased(20.0D, 15.0D, 0);

        // The press on the modal closes it; its drag and release must not reach the board.
        screen.mouseClicked(210.0D, 15.0D, 0);
        assertFalse(screen.hasModal());
        screen.mouseDragged(212.0D, 16.0D, 0, 2.0D, 1.0D);
        screen.mouseReleased(212.0D, 16.0D, 0);
        assertTrue(screen.boardEvents.isEmpty(), "release of the closing press: " + screen.boardEvents);
        assertSame(screen.opener, screen.getFocused(), "focus returns to the key that opened it");

        // The next board click is an ordinary one again.
        screen.mouseClicked(210.0D, 15.0D, 0);
        screen.mouseDragged(212.0D, 16.0D, 0, 2.0D, 1.0D);
        screen.mouseReleased(212.0D, 16.0D, 0);
        assertEquals(List.of("drag", "release"), screen.boardEvents);
        assertSame(screen.other, screen.getFocused());
    }

    @Test
    void tooltipCaptureFollowsTheVanillaReplaceRule() {
        TestScreen screen = screen();
        List<FormattedCharSequence> first = List.of(FormattedCharSequence.forward("first", Style.EMPTY));
        List<FormattedCharSequence> second = List.of(FormattedCharSequence.forward("second", Style.EMPTY));
        List<FormattedCharSequence> focused = List.of(FormattedCharSequence.forward("focused", Style.EMPTY));
        List<FormattedCharSequence> plain = List.of(FormattedCharSequence.forward("plain", Style.EMPTY));

        screen.setTooltipForNextRenderPass(first, new MenuTooltipPositioner(screen.strip), false);
        screen.setTooltipForNextRenderPass(second, new MenuTooltipPositioner(screen.strip), false);
        assertEquals(first, screen.capturedTooltip(), "without override the first tooltip wins");
        assertFalse(screen.capturedTooltipAtFocus(), "a hovered widget's tooltip follows the mouse");

        screen.setTooltipForNextRenderPass(focused, new BelowOrAboveWidgetTooltipPositioner(screen.strip),
                true);
        assertEquals(focused, screen.capturedTooltip(), "a focused widget replaces it");
        assertTrue(screen.capturedTooltipAtFocus(), "keyboard focus anchors below the control");

        screen.setTooltipForNextRenderPass(plain);
        assertEquals(plain, screen.capturedTooltip(), "the plain list call always replaces (vanilla)");
        assertFalse(screen.capturedTooltipAtFocus());
        screen.setTooltipForNextRenderPass(second, DefaultTooltipPositioner.INSTANCE, false);
        assertEquals(plain, screen.capturedTooltip());
    }

    @Test
    void factorOnePassesCoordinatesThrough() {
        TestScreen screen = screen();

        screen.mouseClicked(201.0D, 99.0D, 0);

        assertEquals(List.of("click 201.0,99.0"), screen.events);
        assertEquals(1, screen.uiScale());
    }
}
