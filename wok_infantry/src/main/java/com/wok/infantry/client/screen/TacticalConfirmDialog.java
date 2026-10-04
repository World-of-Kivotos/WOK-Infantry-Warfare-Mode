package com.wok.infantry.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Two-key confirmation card of the preview's {@code UI.confirm}, opened as a {@link TacticalModal}
 * with {@link TacticalScreen#openModal}. The board is dimmed, the card has a section strip (red
 * for dangerous actions), wrapped body text and two keys: cancel and confirm (green, or armed red
 * for a dangerous action).
 *
 * <p>Keyboard: Esc cancels; Tab / arrow keys move the focus between the two keys; Enter or Space
 * activates the focused key, except the confirm key of a dangerous dialog, which only reacts to a
 * mouse click. A dangerous dialog starts with the focus on cancel, a normal one on confirm.
 *
 * <p>The dialog closes itself before running the callback, so the callback may open another modal
 * or leave the screen. Because the dialog lives inside the screen, the screen is not re-initialised
 * when it closes.
 */
public final class TacticalConfirmDialog implements TacticalModal {
    /** What a key press does in the dialog. */
    public enum KeyAction {
        NONE,
        CANCEL,
        CONFIRM,
        FOCUS_NEXT,
        FOCUS_PREVIOUS
    }

    private static final int SECTION_HEIGHT = 14;
    private static final int LINE_HEIGHT = 10;

    private final Component title;
    private final Component body;
    private final boolean danger;
    private final Runnable onConfirm;
    private final Runnable onCancel;
    private final Button cancelButton;
    private final Button confirmButton;
    private TacticalScreen host;
    private boolean confirmFocused;
    private boolean closed;
    private UiRect card = UiRect.EMPTY;
    private List<String> bodyLines = List.of();

    private TacticalConfirmDialog(Builder builder) {
        this.title = builder.title;
        this.body = builder.body;
        this.danger = builder.danger;
        this.onConfirm = builder.onConfirm;
        this.onCancel = builder.onCancel;
        // Preview UI.confirm: cancel carries "back", confirm "warn" (danger) or "check".
        this.cancelButton = BattleUiButton.builder(builder.cancelLabel, ignored -> cancel())
                .icon(TacticalIcon.BACK)
                .bounds(0, 0, 0, 0).build();
        this.confirmButton = BattleUiButton.builder(builder.confirmLabel, ignored -> confirm())
                .kind(danger ? BattleUiButton.Kind.DANGER : BattleUiButton.Kind.SUCCESS)
                .armed(danger)
                .icon(danger ? TacticalIcon.WARN : TacticalIcon.CHECK)
                .bounds(0, 0, 0, 0).build();
        this.confirmFocused = !danger;
        applyFocus();
    }

    public static Builder builder(Component title, Component body) {
        return new Builder(title, body);
    }

    public Component title() {
        return title;
    }

    public Component body() {
        return body;
    }

    public boolean danger() {
        return danger;
    }

    /** Whether the keyboard focus is on the confirm key (false: on cancel). */
    public boolean confirmFocused() {
        return confirmFocused;
    }

    public boolean closed() {
        return closed;
    }

    /** Card bounds of the last layout ({@link UiRect#EMPTY} before the first layout). */
    public UiRect card() {
        return card;
    }

    /** Closes the dialog and runs the confirm callback (no-op once closed). */
    public void confirm() {
        if (finish()) {
            onConfirm.run();
        }
    }

    /** Closes the dialog and runs the cancel callback (no-op once closed). */
    public void cancel() {
        if (finish()) {
            onCancel.run();
        }
    }

    private boolean finish() {
        if (closed) {
            return false;
        }
        closed = true;
        if (host != null) {
            host.closeModal(this);
        }
        return true;
    }

    /**
     * Pure key rule. Esc cancels; Enter, keypad Enter and Space activate the focused key, but
     * never the confirm key of a dangerous dialog; Tab, Shift+Tab and the arrow keys move focus.
     */
    public static KeyAction keyAction(int keyCode, int modifiers, boolean danger,
                                      boolean confirmFocused) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> KeyAction.CANCEL;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE ->
                    !confirmFocused ? KeyAction.CANCEL : danger ? KeyAction.NONE : KeyAction.CONFIRM;
            case GLFW.GLFW_KEY_TAB -> (modifiers & GLFW.GLFW_MOD_SHIFT) != 0
                    ? KeyAction.FOCUS_PREVIOUS : KeyAction.FOCUS_NEXT;
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_UP -> KeyAction.FOCUS_PREVIOUS;
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_DOWN -> KeyAction.FOCUS_NEXT;
            default -> KeyAction.NONE;
        };
    }

    /** Card height for {@code lineCount} body lines (preview: 22 + lines·10 + 10 + key + 8). */
    static int cardHeight(int lineCount, int buttonHeight) {
        return 22 + lineCount * LINE_HEIGHT + 10 + buttonHeight + 8;
    }

    /** Most body lines that keep the card inside a screen of {@code screenHeight}. */
    static int maxBodyLines(int screenHeight, int buttonHeight) {
        return Math.max(1, (screenHeight - 8 - cardHeight(0, buttonHeight)) / LINE_HEIGHT);
    }

    /** Card width: screen width − 16, at most 240 (compact) or 300. */
    static int cardWidth(int screenWidth, boolean tight) {
        return Math.max(40, Math.min(screenWidth - 16, tight ? 240 : 300));
    }

    // ---- TacticalModal ------------------------------------------------------------------------

    @Override
    public void opened(TacticalScreen screen) {
        this.host = screen;
    }

    @Override
    public void layout(Font font, int screenWidth, int screenHeight) {
        TacticalShellLayout.Metrics metrics = TacticalShellLayout.Metrics.forSize(screenWidth,
                screenHeight);
        int width = cardWidth(screenWidth, metrics.tight());
        int buttonHeight = metrics.buttonHeight();
        String text = body.getString();
        bodyLines = text.isBlank() ? List.of()
                : TextFit.wrap(font, text, width - 16, maxBodyLines(screenHeight, buttonHeight));
        int height = cardHeight(bodyLines.size(), buttonHeight);
        int left = (screenWidth - width) / 2;
        int top = Math.max(0, (screenHeight - height) / 2);
        card = new UiRect(left, top, left + width, top + height);
        UiRect keys = new UiRect(card.left() + 8, card.bottom() - 8 - buttonHeight,
                card.right() - 8, card.bottom() - 8);
        List<UiRect> cells = keys.cols(6, UiRect.Size.STAR, UiRect.Size.STAR);
        place(cancelButton, cells.get(0));
        place(confirmButton, cells.get(1));
    }

    private static void place(AbstractWidget widget, UiRect rect) {
        widget.setX(rect.left());
        widget.setY(rect.top());
        widget.setWidth(rect.width());
        widget.setHeight(rect.height());
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        if (card.isEmpty()) {
            return;
        }
        graphics.fill(card.left(), card.top(), card.right(), card.bottom(), TacticalBoardTheme.BOARD);
        BattleUiTheme.outline(graphics, card.left(), card.top(), card.right(), card.bottom(),
                TacticalBoardTheme.BORDER_DARK);
        TacticalBoardTheme.sectionHeader(graphics, font, title, Component.empty(),
                TacticalBoardTheme.LIGHT_MUTED, card.left() + 1, card.top() + 1, card.right() - 1,
                card.top() + 1 + SECTION_HEIGHT,
                danger ? TacticalBoardTheme.DANGER : TacticalBoardTheme.SECTION);
        int y = card.top() + 21;
        for (String line : bodyLines) {
            graphics.drawString(font, line, card.left() + 8, y, TacticalBoardTheme.TEXT, false);
            y += LINE_HEIGHT;
        }
        cancelButton.render(graphics, mouseX, mouseY, partialTick);
        confirmButton.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!closed && !cancelButton.mouseClicked(mouseX, mouseY, button)) {
            confirmButton.mouseClicked(mouseX, mouseY, button);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (closed) {
            return true;
        }
        switch (keyAction(keyCode, modifiers, danger, confirmFocused)) {
            case CANCEL -> {
                clickSound(cancelButton);
                cancel();
            }
            case CONFIRM -> {
                clickSound(confirmButton);
                confirm();
            }
            case FOCUS_NEXT, FOCUS_PREVIOUS -> {
                confirmFocused = !confirmFocused;
                applyFocus();
            }
            case NONE -> {
            }
        }
        return true;
    }

    private static void clickSound(AbstractWidget widget) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            widget.playDownSound(minecraft.getSoundManager());
        }
    }

    private void applyFocus() {
        cancelButton.setFocused(!confirmFocused);
        confirmButton.setFocused(confirmFocused);
    }

    @Override
    public UiRect focusedBounds() {
        AbstractWidget focused = confirmFocused ? confirmButton : cancelButton;
        return UiRect.ofSize(focused.getX(), focused.getY(), focused.getWidth(), focused.getHeight());
    }

    @Override
    public Component narration() {
        return Component.empty().append(title).append(". ").append(body);
    }

    /** Builds a dialog; labels default to 确认 / 取消. */
    public static final class Builder {
        private final Component title;
        private final Component body;
        private Component confirmLabel = Component.translatable("screen.wok_infantry.confirm.ok");
        private Component cancelLabel = Component.translatable("screen.wok_infantry.confirm.cancel");
        private boolean danger;
        private Runnable onConfirm = () -> { };
        private Runnable onCancel = () -> { };

        private Builder(Component title, Component body) {
            this.title = title == null ? Component.empty() : title;
            this.body = body == null ? Component.empty() : body;
        }

        public Builder confirmLabel(Component label) {
            if (label != null) {
                this.confirmLabel = label;
            }
            return this;
        }

        public Builder cancelLabel(Component label) {
            if (label != null) {
                this.cancelLabel = label;
            }
            return this;
        }

        /** Dangerous action: red strip, armed red confirm key, Enter never confirms. */
        public Builder danger(boolean danger) {
            this.danger = danger;
            return this;
        }

        public Builder onConfirm(Runnable action) {
            this.onConfirm = action == null ? () -> { } : action;
            return this;
        }

        public Builder onCancel(Runnable action) {
            this.onCancel = action == null ? () -> { } : action;
            return this;
        }

        public TacticalConfirmDialog build() {
            return new TacticalConfirmDialog(this);
        }
    }
}
