package com.wok.infantry.client.screen;

import com.wok.infantry.client.ui.probe.UiLayoutFrame;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A hardware key at an end of the device's bottom bezel: Esc on the left (acts like pressing
 * Esc) and R on the right (the screen's refresh action), registered with
 * {@link TacticalScreen#setBezelKeys} and placed by {@link TacticalBezelPlan}. Probe ids
 * {@link TacticalBoardChrome#ESC_KEY_UI_ID} and {@link TacticalBoardChrome#REFRESH_KEY_UI_ID}.
 *
 * <p>Key caps belong to the device, so they are painted in its {@link DeviceSkin}, not in the
 * screen palette (preview {@code keyCap} in {@code 17-device.js}): {@code LINE} outline with
 * clipped corners, {@code KEY} face with a {@code KEY_HI} upper and a {@code KEY_LO} lower lip, the
 * key name in {@code KEY_TEXT} and the action beside it in {@code KEY_SUB}. Hover lights the face
 * to {@code KEY_HI}; a disabled key writes both in {@code KEY_SUB} under the disabled hatch and
 * explains itself on hover; the keyboard focus ring uses {@code KEY_TEXT} (the white
 * {@code FOCUS} ring would vanish on the pale Neutral case). The page keys of a
 * {@link TacticalTabStrip.Skin#BEZEL} strip use the same cap; the current page's key is pressed
 * ({@code KEY_DOWN} face, {@code KEY_LO} upper lip, label 1px lower in {@code SELECT_B}) under a
 * lit LED.
 */
public final class BezelKey extends AbstractWidget {
    /** Which end of the bezel the key sits on. */
    public enum Role {
        ESC,
        REFRESH;

        /** Layout-probe uiId of the key. */
        public String uiId() {
            return this == ESC ? TacticalBoardChrome.ESC_KEY_UI_ID
                    : TacticalBoardChrome.REFRESH_KEY_UI_ID;
        }

        int tabOrderGroup() {
            return this == ESC ? TacticalBezelPlan.TAB_ORDER_ESC
                    : TacticalBezelPlan.TAB_ORDER_REFRESH;
        }
    }

    /** How a key cap is drawn. */
    public enum CapState {
        RAISED,
        HOVER,
        /** Pressed: the current page. */
        DOWN,
        DISABLED;

        /** State name for the uiTest probe ({@code NORMAL}, {@code HOVER}, ...). */
        public String probeState() {
            return switch (this) {
                case RAISED -> "NORMAL";
                case HOVER -> "HOVER";
                case DOWN -> "CURRENT";
                case DISABLED -> "DISABLED";
            };
        }
    }

    /**
     * Colours of one key cap. {@code bottomLip} 0 draws no lower lip; {@code textDrop} moves the
     * label down (a pressed key).
     */
    public record Cap(int edge, int face, int topLip, int bottomLip, int text, int sub,
                      boolean hatch, int textDrop) {
    }

    /** Where an Esc / R label goes: key name at {@code keyX}, action at {@code actionX}. */
    public record Label(int keyX, int actionX, boolean fits) {
    }

    /**
     * Unlit page LED (preview {@code DT.LED_OFF}; a device part, the same for every livery and
     * the same as the case's unlit LEDs, {@link DeviceArt#LED_OFF}).
     */
    public static final int LED_OFF = DeviceArt.LED_OFF;
    /**
     * Prefix of the probe note a drawn page LED leaves in a recorded frame ({@link #ledNote}).
     * uiTest only; nothing without a probe.
     */
    public static final String LED_NOTE = "bezel.led ";
    /** Alpha of the glow around the lit LED. */
    static final int LED_GLOW_ALPHA = 0x50;
    /** Gap between the key name and its action label. */
    static final int LABEL_GAP = 4;

    private final Role role;
    private final TacticalBoardChrome.KeyHint hint;
    private final BooleanSupplier action;
    private final Supplier<Component> disabledReason;
    private final Supplier<DeviceSkin> skin;
    private Tooltip reasonTooltip;
    private String reasonText;
    private Tooltip labelTooltip;

    /**
     * @param action         runs the key; returns whether it did anything
     * @param disabledReason asked on every frame and press: non-null disables the key and is
     *                       shown on hover ({@code null} supplier: always enabled)
     * @param skin           device paint ({@code null}: the paint of the screen on display)
     */
    BezelKey(Role role, TacticalBoardChrome.KeyHint hint, BooleanSupplier action,
             Supplier<Component> disabledReason, Supplier<DeviceSkin> skin) {
        super(0, 0, 0, 0, message(Objects.requireNonNull(hint, "hint")));
        this.role = Objects.requireNonNull(role, "role");
        this.hint = hint;
        this.action = action == null ? () -> false : action;
        this.disabledReason = disabledReason == null ? () -> null : disabledReason;
        this.skin = skin == null ? BezelKey::currentSkin : skin;
        setTabOrderGroup(role.tabOrderGroup());
        UiLayoutProbe.tag(this, role.uiId());
        syncEnabled();
    }

    private static Component message(TacticalBoardChrome.KeyHint hint) {
        if (hint.action().getString().isEmpty()) {
            return hint.key();
        }
        return Component.empty().append(hint.key()).append(" ").append(hint.action());
    }

    public Role role() {
        return role;
    }

    public TacticalBoardChrome.KeyHint hint() {
        return hint;
    }

    public void setBounds(UiRect bounds) {
        UiRect safe = bounds == null ? UiRect.EMPTY : bounds;
        setX(safe.left());
        setY(safe.top());
        setWidth(safe.width());
        setHeight(safe.height());
    }

    // ---- input ----------------------------------------------------------------------------------

    /** Why the key is disabled right now, or {@code null}; also updates {@link #active}. */
    Component syncEnabled() {
        Component reason = disabledReason.get();
        active = reason == null;
        return reason;
    }

    /** Presses the key (click, Enter or Space); returns whether its action ran. */
    boolean press() {
        return syncEnabled() == null && action.getAsBoolean();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !isValidClickButton(button) || !clicked(mouseX, mouseY)
                || syncEnabled() != null) {
            return false;
        }
        clickSound();
        press();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !CommonInputs.selected(keyCode) || syncEnabled() != null) {
            return false;
        }
        clickSound();
        press();
        return true;
    }

    private void clickSound() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            playDownSound(minecraft.getSoundManager());
        }
    }

    // ---- drawing --------------------------------------------------------------------------------

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Component reason = syncEnabled();
        if (width <= 0 || height <= 0) {
            setTooltip(null);
            return;
        }
        Font font = Minecraft.getInstance().font;
        DeviceSkin paint = skin.get();
        CapState state = state(reason == null, false, isHovered());
        Cap cap = cap(paint, state);
        UiRect key = UiRect.ofSize(getX(), getY(), width, height);
        drawCap(graphics, key, cap);
        boolean focusRing = TacticalButtonStyle.keyboardFocused(this);
        if (focusRing) {
            focusRing(graphics, key, paint);
        }
        boolean truncated = drawLabel(graphics, font, key, cap);
        setTooltip(reason != null ? reasonTooltip(reason) : truncated ? labelTooltip() : null);
        if (UiLayoutProbe.recording()) {
            UiLayoutProbe.widget(graphics, this, "key", state.probeState(), truncated, focusRing);
        }
    }

    /** Draws key name and action; returns whether only the name fitted. */
    private boolean drawLabel(GuiGraphics graphics, Font font, UiRect key, Cap cap) {
        int textY = textY(key, cap);
        boolean hasAction = !hint.action().getString().isEmpty();
        Label label = label(key, font.width(hint.key()), hasAction ? font.width(hint.action()) : 0);
        if (!label.fits()) {
            TextFit.draw(graphics, font, hint.key(), key.left() + 3, textY,
                    Math.max(0, key.width() - 6), cap.text(), TextFit.Align.CENTER);
            return hasAction;
        }
        graphics.drawString(font, hint.key(), label.keyX(), textY, cap.text(), false);
        UiLayoutProbe.rawText(graphics, font, hint.key(), label.keyX(), textY);
        if (hasAction) {
            graphics.drawString(font, hint.action(), label.actionX(), textY, cap.sub(), false);
            UiLayoutProbe.rawText(graphics, font, hint.action(), label.actionX(), textY);
        }
        return false;
    }

    private Tooltip reasonTooltip(Component reason) {
        String text = reason.getString();
        if (reasonTooltip == null || !text.equals(reasonText)) {
            reasonTooltip = Tooltip.create(Component.empty().append(getMessage()).append("\n")
                    .append(reason));
            reasonText = text;
        }
        return reasonTooltip;
    }

    private Tooltip labelTooltip() {
        if (labelTooltip == null) {
            labelTooltip = Tooltip.create(getMessage());
        }
        return labelTooltip;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    // ---- shared key cap (also the page keys of a BEZEL tab strip) ----------------------------------

    /** Pure state rule: disabled wins, then pressed, then hover. */
    public static CapState state(boolean enabled, boolean pressed, boolean hovered) {
        if (!enabled) {
            return CapState.DISABLED;
        }
        if (pressed) {
            return CapState.DOWN;
        }
        return hovered ? CapState.HOVER : CapState.RAISED;
    }

    /**
     * Colours of a key cap in {@code state}: the device paint, except the pressed key's label,
     * which is the screen palette's {@code SELECT_B} (preview {@code pageKeys}).
     */
    public static Cap cap(DeviceSkin skin, CapState state) {
        DeviceSkin paint = Objects.requireNonNull(skin, "skin");
        return switch (state == null ? CapState.RAISED : state) {
            case RAISED -> new Cap(paint.line(), paint.key(), paint.keyHi(), paint.keyLo(),
                    paint.keyText(), paint.keySub(), false, 0);
            case HOVER -> new Cap(paint.line(), paint.keyHi(), paint.keyHi(), paint.keyLo(),
                    paint.keyText(), paint.keySub(), false, 0);
            case DOWN -> new Cap(paint.line(), paint.keyDown(), paint.keyLo(), 0,
                    TacticalBoardTheme.SELECT_B, paint.keySub(), false, 1);
            case DISABLED -> new Cap(paint.line(), paint.key(), paint.keyHi(), paint.keyLo(),
                    paint.keySub(), paint.keySub(), true, 0);
        };
    }

    /**
     * Draws a key cap: outline with clipped corners, face, lips and (disabled) the hatch; at most
     * five fills and one hatch blit.
     */
    static void drawCap(GuiGraphics graphics, UiRect key, Cap cap) {
        if (key.width() < 3 || key.height() < 3) {
            return;
        }
        int left = key.left();
        int top = key.top();
        int right = key.right();
        int bottom = key.bottom();
        graphics.fill(left + 1, top, right - 1, bottom, cap.edge());
        graphics.fill(left, top + 1, right, bottom - 1, cap.edge());
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, cap.face());
        graphics.fill(left + 1, top + 1, right - 1, top + 2, cap.topLip());
        if (cap.bottomLip() != 0) {
            graphics.fill(left + 1, bottom - 2, right - 1, bottom - 1, cap.bottomLip());
        }
        if (cap.hatch()) {
            TacticalButtonStyle.hatch(graphics, left + 1, top + 1, right - 1, bottom - 1);
        }
    }

    /** Keyboard focus ring one pixel outside the cap, in {@code KEY_TEXT}. */
    static void focusRing(GuiGraphics graphics, UiRect key, DeviceSkin skin) {
        BattleUiTheme.outline(graphics, key.left() - 1, key.top() - 1, key.right() + 1,
                key.bottom() + 1, skin.keyText());
    }

    /** Baseline of a key label: the 8px glyphs centred, 1px lower on a pressed key. */
    static int textY(UiRect key, Cap cap) {
        return TacticalTabStrip.labelTextY(TacticalTabStrip.Skin.BEZEL, key) + cap.textDrop();
    }

    /**
     * Pure label placement of an Esc / R key: key name, 4px, action, centred as one group. It
     * fits when it leaves the 3px side margins of a centred key label free.
     */
    public static Label label(UiRect key, int keyTextWidth, int actionWidth) {
        int keyWidth = Math.max(0, keyTextWidth);
        int total = keyWidth + (actionWidth > 0 ? LABEL_GAP + actionWidth : 0);
        int x = key.left() + Math.floorDiv(key.width() - total, 2);
        return new Label(x, x + keyWidth + LABEL_GAP, total <= key.width() - 6);
    }

    /** Draws the LED above {@code key} (lit: the skin's LED with its glow; else unlit). */
    static void drawLed(GuiGraphics graphics, TacticalBezelPlan plan, UiRect key, boolean lit,
                        DeviceSkin skin) {
        UiRect led = plan.led(key);
        if (led.isEmpty()) {
            return;
        }
        if (UiLayoutProbe.recording()) {
            UiLayoutProbe.note(ledNote(lit, UiLayoutFrame.transform(graphics.pose().last().pose(),
                    key.left(), key.top(), key.right(), key.bottom())));
        }
        if (lit) {
            UiRect glow = plan.ledGlow(key);
            graphics.fill(glow.left(), glow.top(), glow.right(), glow.bottom(), ledGlow(skin));
        }
        graphics.fill(led.left(), led.top(), led.right(), led.bottom(), ledColor(skin, lit));
    }

    /**
     * {@code bezel.led lit=true key=[l,t,r,b]}: the probe note of the LED above the page key at
     * {@code key} (GUI coordinates, the same rectangle the key reports as its control), so the
     * uiTest can tell that the current page's key sits under a lit LED.
     */
    public static String ledNote(boolean lit, UiLayoutFrame.Rect key) {
        return LED_NOTE + "lit=" + lit + " key=" + key;
    }

    /**
     * Colour of a page LED: the livery's own {@code LED} when lit (a page palette never recolours
     * the hardware), otherwise the shared unlit grey.
     */
    public static int ledColor(DeviceSkin skin, boolean lit) {
        return lit ? skin.led() : LED_OFF;
    }

    /** Glow under a lit LED: its colour at alpha {@code 0x50}. */
    public static int ledGlow(DeviceSkin skin) {
        return (LED_GLOW_ALPHA << 24) | (skin.led() & 0x00FFFFFF);
    }

    /**
     * Paint of the device on display: the open {@link TacticalScreen}'s livery, otherwise the
     * viewer's.
     */
    static DeviceSkin currentSkin() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.screen instanceof TacticalScreen screen) {
            return screen.deviceSkin();
        }
        return DeviceSkin.forLivery(TacticalLivery.current());
    }
}
