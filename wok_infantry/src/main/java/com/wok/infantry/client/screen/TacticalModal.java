package com.wok.infantry.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * A modal layer drawn inside a {@link TacticalScreen} (confirmation dialogs and similar). While
 * it is open the screen dims the board, stops hover and tooltips underneath and sends every
 * mouse and key event here; the underlying screen is never re-initialised, so drafts, scroll
 * positions and selections survive. Coordinates are logical layout pixels.
 */
public interface TacticalModal {
    /** Called once when the modal is opened on {@code host}. */
    void opened(TacticalScreen host);

    /** (Re)computes the layout; called on open and on every screen init or resize. */
    void layout(Font font, int screenWidth, int screenHeight);

    /** Draws the modal above the dimmed board (the host already drew the dim). */
    void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick);

    boolean mouseClicked(double mouseX, double mouseY, int button);

    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return true;
    }

    default boolean mouseDragged(double mouseX, double mouseY, int button,
                                 double dragX, double dragY) {
        return true;
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return true;
    }

    boolean keyPressed(int keyCode, int scanCode, int modifiers);

    default boolean charTyped(char codePoint, int modifiers) {
        return true;
    }

    /** Bounds of the keyboard-focused control, used to anchor its tooltip; {@code null} if none. */
    default UiRect focusedBounds() {
        return null;
    }

    /** Text read by the narrator when the modal opens. */
    default Component narration() {
        return Component.empty();
    }
}
