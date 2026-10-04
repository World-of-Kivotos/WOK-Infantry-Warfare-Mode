package com.wok.infantry.client.screen;

import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

/**
 * Text field of the tactical-tablet style (preview {@code UI.textField}): dark well, 1px outline
 * ({@link TacticalBoardTheme#INPUT_EDGE}; {@link TacticalBoardTheme#SELECT_B} while typing,
 * {@link TacticalBoardTheme#DANGER_B} with an error), light text, a faint placeholder and no text
 * shadow anywhere.
 *
 * <p>It stays a vanilla {@link EditBox} (typing, selection, clipboard, cursor, narration, focus),
 * with vanilla's bordered geometry: the text sits 4px in and vertically centred. Only for this
 * class {@code EditBoxShadowMixin} skips vanilla's black box and border and draws its texts
 * without a shadow; every other {@link EditBox} is untouched. Should the mixin ever not apply
 * (another Minecraft build), the field still works and only looks like a vanilla box again.
 */
public class TacticalTextField extends EditBox {
    private boolean editable = true;
    private Component error;
    private Tooltip ownTooltip;
    private Tooltip errorTooltip;

    public TacticalTextField(Font font, int x, int y, int width, int height, Component name) {
        super(font, x, y, width, height, name == null ? Component.empty() : name);
        setTextColor(TacticalBoardTheme.LIGHT);
        setTextColorUneditable(TacticalBoardTheme.FAINT);
    }

    /** Placeholder shown while the field is empty and not focused, in the faint colour. */
    public TacticalTextField placeholder(Component text) {
        setHint(text == null ? Component.empty() : text.copy().withStyle(style ->
                style.withColor(TextColor.fromRgb(TacticalBoardTheme.FAINT & 0xFFFFFF))));
        return this;
    }

    /**
     * Marks the field as wrong ({@code null} clears it): red outline, and the reason becomes the
     * tooltip and is narrated. The screen's own tooltip comes back when the error is cleared.
     */
    public void setError(Component reason) {
        error = reason == null || reason.getString().isEmpty() ? null : reason;
        errorTooltip = error == null ? null : Tooltip.create(error);
        super.setTooltip(errorTooltip != null ? errorTooltip : ownTooltip);
    }

    public boolean hasError() {
        return error != null;
    }

    /** The current error reason, or {@code null}. */
    public Component error() {
        return error;
    }

    public boolean isEditableField() {
        return editable;
    }

    @Override
    public void setEditable(boolean value) {
        super.setEditable(value);
        this.editable = value;
    }

    @Override
    public void setTooltip(Tooltip tooltip) {
        ownTooltip = tooltip;
        super.setTooltip(errorTooltip != null ? errorTooltip : tooltip);
    }

    /** Outline colour for the current state (see {@link TacticalDraw#inputEdge}). */
    public int edgeColor() {
        return TacticalDraw.inputEdge(isFocused(), error != null, editable);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!isVisible()) {
            return;
        }
        TacticalDraw.inputFrame(graphics, UiRect.ofSize(getX(), getY(), width, height),
                edgeColor());
        // Text, cursor, selection and placeholder: vanilla, without its frame and shadow.
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        if (UiLayoutProbe.recording()) {
            UiLayoutProbe.widget(graphics, this, "text-field", error != null ? "ERROR"
                    : !editable ? "READONLY" : isFocused() ? "FOCUSED" : "NORMAL", false,
                    isFocused());
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        super.updateWidgetNarration(output);
        if (error != null) {
            output.add(NarratedElementType.HINT, error);
        }
    }
}
