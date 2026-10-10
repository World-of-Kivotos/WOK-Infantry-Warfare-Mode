package com.wok.infantry.client.screen;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.BelowOrAboveWidgetTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Base class of every WOK步战 tablet screen.
 *
 * <p><b>Minimum 2x.</b> When {@link UiScale} picks factor 2 (GUI scale 1, window ≥ 640×480,
 * {@code ui.minimumScale2x} on), {@link #width}/{@link #height} are the halved logical layout
 * (960×720 → 480×360), everything is drawn under a 2x pose and every mouse coordinate is divided
 * by the factor before it reaches the screen or its widgets. With factor 1 every call passes
 * straight through, so the class behaves like a plain {@link Screen}.
 *
 * <p><b>Migration.</b> {@code init()} becomes {@link #initTactical()}, {@code render} becomes
 * {@link #renderTactical} (call {@link #renderWidgets} where {@code super.render} was), and the
 * input overrides become {@code onMouseClicked}, {@code onMouseReleased}, {@code onMouseDragged},
 * {@code onMouseScrolled}, {@code onMouseMoved}, {@code onKeyPressed}, {@code onKeyReleased} and
 * {@code onCharTyped}; the originals are final here. A hook that still calls the old
 * {@code super.mouseClicked(...)}/{@code super.keyPressed(...)} (or {@code super.render(...)} from
 * renderTactical) gets the vanilla behaviour without a second conversion or recursion. Clip with
 * {@link UiScale#enableScissor}, never with {@link GuiGraphics#enableScissor} directly.
 *
 * <p><b>Tooltips.</b> Every widget tooltip ({@code setTooltip(...)}) and every
 * {@code setTooltipForNextRenderPass(...)} call is captured (with vanilla's replace rule) and drawn
 * as a shadowless {@link TacticalTooltip} in layout coordinates after the screen, next to the mouse
 * or, for a keyboard-focused control, below it; existing tooltip calls need no change.
 *
 * <p><b>Modal layer.</b> {@link #openModal} shows a {@link TacticalModal} (for example a
 * {@link TacticalConfirmDialog}) inside this screen: the board is dimmed, hover and tooltips
 * underneath stop and all input goes to the modal (including the drag and release of a press that
 * closed it). Nothing underneath keeps the focus; on close it returns to the control that opened
 * the modal. Closing it does not re-initialise the screen, so drafts and scroll positions stay.
 *
 * <p><b>Tabs.</b> A strip registered with {@link #setTabStrip} receives Ctrl+Tab and
 * Ctrl+Shift+Tab from anywhere on the screen; plain Tab keeps the vanilla focus navigation.
 *
 * <p><b>Device.</b> Each frame runs inside the palette of the screen's {@link #livery()} and
 * {@link #paletteScope()} (the HUD, drawn before the screen, keeps the A scheme). The order is:
 * backdrop, {@link #renderTactical} (shell, page, widgets), modal, {@link #renderGlassOverlay},
 * tooltip. The hardware Esc / R keys of the bottom bezel are registered with
 * {@link #setBezelKeys}.
 */
public abstract class TacticalScreen extends Screen {
    /** Mouse position handed to the board while a modal is open, so nothing underneath hovers. */
    private static final int NO_MOUSE = -32768;
    private static final float MODAL_Z = 300.0F;

    private int uiScale = 1;
    private TacticalModal modal;
    private GuiEventListener focusBeforeModal;
    private TacticalTabStrip tabStrip;
    private List<FormattedCharSequence> pendingTooltip;
    private boolean pendingTooltipKeyboard;
    private boolean modalLaidOut;
    private boolean rendering;

    protected TacticalScreen(Component title) {
        super(title);
    }

    // ---- layout ---------------------------------------------------------------------------------

    /**
     * Final: picks the scale factor, sets the logical {@link #width}/{@link #height} from the
     * window (also on rebuilds and resizes, so the size is never divided twice) and calls
     * {@link #initTactical()}.
     */
    @Override
    protected final void init() {
        uiScale = UiScale.factor(minecraft);
        if (minecraft != null) {
            Window window = minecraft.getWindow();
            width = UiScale.layoutSize(window.getGuiScaledWidth(), uiScale);
            height = UiScale.layoutSize(window.getGuiScaledHeight(), uiScale);
        }
        tabStrip = null;
        resetBezelKeys();
        initTactical();
        layoutModal();
    }

    /** Creates widgets for the logical {@link #width} × {@link #height}. */
    protected abstract void initTactical();

    /** Current factor: 2 while the minimum 2x applies, otherwise 1. */
    public final int uiScale() {
        return uiScale;
    }

    /** Unit-test seam: forces the factor without a client window (the next init recomputes it). */
    final void applyUiScale(int factor) {
        uiScale = Math.max(1, factor);
    }

    /** Physical pixels per logical pixel of this screen. */
    public final double effectiveGuiScale() {
        return minecraft == null ? uiScale : minecraft.getWindow().getGuiScale() * uiScale;
    }

    /** Shell geometry of this screen's logical size. */
    public final TacticalShellLayout shellLayout() {
        return TacticalShellLayout.compute(width, height);
    }

    /**
     * Draws the tablet shell for this screen's logical size (call first in renderTactical), in
     * the livery resolved for this frame ({@link #frameLivery()}), the same one as the palette.
     */
    protected final TacticalBoardChrome.Shell drawShell(GuiGraphics graphics,
                                                        TacticalBoardChrome.ShellSpec spec) {
        return deviceDrawn(TacticalBoardChrome.shell(graphics, font, shellLayout(), frameLivery(),
                spec));
    }

    /**
     * Registers the strip that Ctrl+Tab / Ctrl+Shift+Tab cycles. Call from
     * {@link #initTactical()} after adding it; cleared automatically on every init.
     */
    protected final void setTabStrip(TacticalTabStrip strip) {
        this.tabStrip = strip;
    }

    public final TacticalTabStrip tabStrip() {
        return tabStrip;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- rendering ------------------------------------------------------------------------------

    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (rendering) {
            // renderTactical still calling super.render(...): draw the widgets, do not recurse.
            renderWidgets(graphics, mouseX, mouseY, partialTick);
            return;
        }
        int layoutX = UiScale.toLayout(mouseX, uiScale);
        int layoutY = UiScale.toLayout(mouseY, uiScale);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        rendering = true;
        // The palette covers the page, widgets, modal and tooltip; closing it restores the previous
        // palette before anything else draws, also when rendering throws.
        try (TacticalPalette.Applied palette = pushPalette()) {
            if (uiScale != 1) {
                pose.scale(uiScale, uiScale, 1.0F);
            }
            pendingTooltip = null;
            renderBackdrop(graphics, partialTick);
            boolean modalOpen = modal != null;
            renderTactical(graphics, modalOpen ? NO_MOUSE : layoutX,
                    modalOpen ? NO_MOUSE : layoutY, partialTick);
            if (modal != null) {
                // Nothing under the modal may show a tooltip, not even the keyboard-focused one.
                pendingTooltip = null;
                if (!modalLaidOut) {
                    layoutModal();
                }
                // A dialog key never adopts a page label drawn under it (uiTest probe only).
                UiLayoutProbe.layer();
                pose.pushPose();
                pose.translate(0.0F, 0.0F, MODAL_Z);
                graphics.fill(0, 0, width + 1, height + 1, TacticalBoardTheme.MODAL_DIM);
                modal.render(graphics, font, layoutX, layoutY, partialTick);
                pose.popPose();
            }
            renderGlassOverlay(graphics, partialTick);
            renderPendingTooltip(graphics, layoutX, layoutY);
        } finally {
            rendering = false;
            pendingTooltip = null;
            pose.popPose();
        }
    }

    /**
     * Draws the screen in logical coordinates (already scaled). The default draws only the
     * widgets; subclasses draw the shell and panels and call {@link #renderWidgets}.
     */
    protected void renderTactical(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderWidgets(graphics, mouseX, mouseY, partialTick);
    }

    /** Draws every renderable widget (what {@code super.render} did in a plain screen). */
    protected final void renderWidgets(GuiGraphics graphics, int mouseX, int mouseY,
                                       float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // ---- faction palette ------------------------------------------------------------------------

    /**
     * Livery this screen is painted in, read once per frame by {@link #render}; default: the
     * viewer's livery ({@link TacticalLivery#current()}).
     */
    protected TacticalLivery.Livery livery() {
        return TacticalLivery.current();
    }

    /** Palette scope of the page on screen; default {@link TacticalLivery.Scope#BOARD}. */
    protected TacticalLivery.Scope paletteScope() {
        return TacticalLivery.Scope.BOARD;
    }

    /** Livery of the frame being drawn (the last one drawn between frames). */
    private TacticalLivery.Livery frameLivery = TacticalLivery.Livery.NEUTRAL;

    /**
     * Livery the current frame is painted in, resolved once per frame before anything is drawn:
     * device paint ({@link TacticalLivery.Livery#skin()}) read through it always matches the
     * screen palette, even when a snapshot arrives mid-frame. Neutral before the first frame.
     */
    protected final TacticalLivery.Livery frameLivery() {
        return frameLivery;
    }

    /**
     * Resolves this frame's livery (kept for {@link #frameLivery()}) and returns its palette on
     * the page's scope; a {@code null} livery counts as Neutral.
     */
    final TacticalPalette framePalette() {
        TacticalLivery.Livery resolved = livery();
        frameLivery = resolved != null ? resolved : TacticalLivery.Livery.NEUTRAL;
        return frameLivery.palette(paletteScope());
    }

    /** Applies this frame's palette; {@link #render} closes the handle. */
    private TacticalPalette.Applied pushPalette() {
        return TacticalPalette.push(framePalette());
    }

    // ---- device backdrop and glass --------------------------------------------------------------

    /**
     * Depth of the glass overlay: above an open modal (drawn at z 300) and below the tooltip
     * ({@link TacticalTooltip#Z}), so the glass also covers the dimmed board and its dialog.
     */
    static final float GLASS_Z = 350.0F;

    /** Set by {@link #drawShell} in this frame; the glass is only laid over a drawn device. */
    private boolean deviceDrawn;

    /**
     * Draws what shows around the device, before {@link #renderTactical}: the world dimmed with
     * {@link DeviceArt#WORLD_DIM} and the vignette ({@link DeviceArt#drawBackdrop}); never an
     * opaque background and never the vanilla {@code renderBackground}.
     */
    protected void renderBackdrop(GuiGraphics graphics, float partialTick) {
        DeviceArt.drawBackdrop(graphics, width, height);
    }

    /**
     * Draws the glass over the page, its widgets and an open modal (rim, lip shadow, sheen);
     * tooltips stay above it. Logical coordinates; nothing in a frame without {@link #drawShell}.
     */
    protected void renderGlassOverlay(GuiGraphics graphics, float partialTick) {
        if (!deviceDrawn) {
            return;
        }
        deviceDrawn = false;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, GLASS_Z);
        DeviceArt.drawGlass(graphics, shellLayout(), frameLivery());
        pose.popPose();
    }

    /**
     * {@link #drawShell} finished the device: arms the glass overlay and offers the whole receipt
     * while the mouse rests on a shortened status-bar pill.
     */
    private TacticalBoardChrome.Shell deviceDrawn(TacticalBoardChrome.Shell shell) {
        deviceDrawn = true;
        TacticalBoardChrome.StatusText receipt = shell.status().feedback();
        if (receipt.truncated() && mouseOver(shell.status().pill())) {
            setTooltipForNextRenderPass(Component.literal(receipt.full()));
        }
        return shell;
    }

    /**
     * Whether the mouse (read from the mouse handler like vanilla's render loop, in layout
     * coordinates) is over {@code rect}; never while a modal takes the input.
     */
    private boolean mouseOver(UiRect rect) {
        if (minecraft == null || modal != null || rect == null || rect.isEmpty()) {
            return false;
        }
        Window window = minecraft.getWindow();
        if (window.getScreenWidth() <= 0 || window.getScreenHeight() <= 0) {
            return false;
        }
        double guiX = minecraft.mouseHandler.xpos() * window.getGuiScaledWidth()
                / window.getScreenWidth();
        double guiY = minecraft.mouseHandler.ypos() * window.getGuiScaledHeight()
                / window.getScreenHeight();
        return rect.contains(UiScale.toLayout(guiX, uiScale), UiScale.toLayout(guiY, uiScale));
    }

    // ---- bezel keys -----------------------------------------------------------------------------
    //
    // The Esc and R hardware keys are BezelKey widgets owned by this block: setBezelKeys adds and
    // places them (TacticalBezelPlan), init removes them. Keyboard Esc and R stay with the screen's
    // own onKeyPressed; the keys only add the mouse (and Tab focus) route to the same behaviour.

    private TacticalBoardChrome.KeyHint bezelEscHint;
    private TacticalBoardChrome.KeyHint bezelRefreshHint;
    private Runnable bezelRefresh;
    private java.util.function.Supplier<Component> bezelRefreshDisabled;
    private BezelKey bezelEscKey;
    private BezelKey bezelRefreshKey;

    /**
     * Registers the hardware keys at the ends of the bottom bezel: {@code escHint} for the Esc key
     * (it acts like pressing Esc, so the screen keeps its own meaning of Esc) and
     * {@code refreshHint} with the {@code refresh} action for the R key. A {@code null} hint
     * leaves that key out. Call from {@link #initTactical()}, then place the page keys with
     * {@link TacticalBoardChrome#placeBezel} and {@link #bezelHints()} (a bezel strip already
     * registered with {@link #setTabStrip} is moved between the keys here as well); cleared on
     * every init.
     */
    protected final void setBezelKeys(TacticalBoardChrome.KeyHint escHint,
                                      TacticalBoardChrome.KeyHint refreshHint, Runnable refresh) {
        setBezelKeys(escHint, refreshHint, refresh, null);
    }

    /**
     * {@link #setBezelKeys(TacticalBoardChrome.KeyHint, TacticalBoardChrome.KeyHint, Runnable)}
     * with an R key that can be disabled: {@code refreshDisabledReason} is asked on every frame and
     * press; a non-null reason disables the key (drawn hatched) and is shown on hover, for example
     * a formation page whose R only retries while it waits for the catalog.
     */
    protected final void setBezelKeys(TacticalBoardChrome.KeyHint escHint,
                                      TacticalBoardChrome.KeyHint refreshHint, Runnable refresh,
                                      java.util.function.Supplier<Component> refreshDisabledReason) {
        removeBezelKeys();
        this.bezelEscHint = escHint;
        this.bezelRefreshHint = refreshHint;
        this.bezelRefresh = refresh;
        this.bezelRefreshDisabled = refreshDisabledReason;
        if (escHint != null) {
            bezelEscKey = addRenderableWidget(new BezelKey(BezelKey.Role.ESC, escHint,
                    this::pressBezelEscape, null, this::deviceSkin));
        }
        if (refreshHint != null) {
            bezelRefreshKey = addRenderableWidget(new BezelKey(BezelKey.Role.REFRESH, refreshHint,
                    this::pressBezelRefresh, this::bezelRefreshDisabledReason, this::deviceSkin));
        }
        if (font != null) {
            placeBezelKeys(TacticalBezelPlan.plan(font, shellLayout(), escHint, refreshHint));
        }
    }

    /**
     * Moves the Esc and R keys to {@code plan}, and a registered {@link TacticalTabStrip.Skin#BEZEL}
     * strip between them, so the strip may be registered before or after the keys.
     */
    final void placeBezelKeys(TacticalBezelPlan plan) {
        if (bezelEscKey != null) {
            bezelEscKey.setBounds(plan.esc());
        }
        if (bezelRefreshKey != null) {
            bezelRefreshKey.setBounds(plan.refresh());
        }
        if (tabStrip != null && tabStrip.skin() == TacticalTabStrip.Skin.BEZEL) {
            tabStrip.placeOnBezel(plan);
        }
    }

    /** Hints of the registered bezel keys, Esc first; empty without {@link #setBezelKeys}. */
    public final List<TacticalBoardChrome.KeyHint> bezelHints() {
        List<TacticalBoardChrome.KeyHint> hints = new ArrayList<>(2);
        if (bezelEscHint != null) {
            hints.add(bezelEscHint);
        }
        if (bezelRefreshHint != null) {
            hints.add(bezelRefreshHint);
        }
        return List.copyOf(hints);
    }

    /** The R key's action, or {@code null} without one. */
    protected final Runnable bezelRefresh() {
        return bezelRefresh;
    }

    /** Why the R key is disabled right now, or {@code null} when it can refresh. */
    protected final Component bezelRefreshDisabledReason() {
        java.util.function.Supplier<Component> reason = bezelRefreshDisabled;
        return reason == null ? null : reason.get();
    }

    /**
     * Paint of this screen's device: the {@link DeviceSkin} of the livery resolved for this frame
     * ({@link #frameLivery()}), so the keys never disagree with the case and the palette.
     */
    public final DeviceSkin deviceSkin() {
        return frameLivery().skin();
    }

    /** The registered Esc and R key widgets, in that order (unit-test and probe seam). */
    final List<BezelKey> bezelKeys() {
        List<BezelKey> keys = new ArrayList<>(2);
        if (bezelEscKey != null) {
            keys.add(bezelEscKey);
        }
        if (bezelRefreshKey != null) {
            keys.add(bezelRefreshKey);
        }
        return List.copyOf(keys);
    }

    /**
     * The bezel's Esc key: the same as pressing Esc ({@link #onKeyPressed} with
     * {@code GLFW_KEY_ESCAPE}), so every screen keeps its own meaning of Esc. Nothing while a
     * modal is open (the modal takes every input).
     */
    final boolean pressBezelEscape() {
        if (modal != null) {
            return false;
        }
        // A click already runs inside the input dispatch; a hook that still calls the old
        // super.keyPressed(...) must get the vanilla handling, and the flag is restored after.
        boolean outer = dispatchingInput;
        dispatchingInput = true;
        try {
            return onKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE, 0, 0);
        } finally {
            dispatchingInput = outer;
        }
    }

    /** The bezel's R key: runs the registered refresh unless it is disabled or a modal is open. */
    final boolean pressBezelRefresh() {
        Runnable refresh = bezelRefresh;
        if (modal != null || refresh == null || bezelRefreshDisabledReason() != null) {
            return false;
        }
        refresh.run();
        return true;
    }

    private void removeBezelKeys() {
        if (bezelEscKey != null) {
            removeWidget(bezelEscKey);
        }
        if (bezelRefreshKey != null) {
            removeWidget(bezelRefreshKey);
        }
        bezelEscKey = null;
        bezelRefreshKey = null;
    }

    private void resetBezelKeys() {
        removeBezelKeys();
        bezelEscHint = null;
        bezelRefreshHint = null;
        bezelRefresh = null;
        bezelRefreshDisabled = null;
    }

    private void renderPendingTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        List<FormattedCharSequence> lines = pendingTooltip;
        if (lines == null || lines.isEmpty() || font == null) {
            return;
        }
        UiRect anchor = pendingTooltipKeyboard ? keyboardAnchor() : null;
        if (anchor != null && !anchor.isEmpty()) {
            TacticalTooltip.renderAtAnchor(graphics, font, lines, anchor, width, height);
        } else {
            TacticalTooltip.renderAtMouse(graphics, font, lines, mouseX, mouseY, width, height);
        }
    }

    private UiRect keyboardAnchor() {
        if (modal != null) {
            return modal.focusedBounds();
        }
        GuiEventListener focused = getFocused();
        while (focused instanceof ContainerEventHandler container && container.getFocused() != null) {
            focused = container.getFocused();
        }
        if (focused == null) {
            return null;
        }
        ScreenRectangle rectangle = focused.getRectangle();
        return UiRect.ofSize(rectangle.left(), rectangle.top(), rectangle.width(),
                rectangle.height());
    }

    // ---- tooltip capture ------------------------------------------------------------------------
    //
    // Same rule as vanilla Screen: the plain list/component calls always replace the pending
    // tooltip; the positioner calls (widget tooltips pass isFocused() as "override") replace it
    // only with override, otherwise the first one wins. Vanilla picks a
    // BelowOrAboveWidgetTooltipPositioner exactly for a keyboard-focused, not hovered widget, so
    // only that tooltip is anchored at the focused control; every other one follows the mouse.

    @Override
    public final void setTooltipForNextRenderPass(List<FormattedCharSequence> lines) {
        captureTooltip(lines, true, false);
    }

    @Override
    public final void setTooltipForNextRenderPass(List<FormattedCharSequence> lines,
                                                  ClientTooltipPositioner positioner,
                                                  boolean override) {
        captureTooltip(lines, override, anchorsAtFocus(positioner));
    }

    @Override
    protected final void setTooltipForNextRenderPass(Component text) {
        if (text != null && font != null) {
            captureTooltip(TacticalTooltip.lines(font, text, width), true, false);
        }
    }

    @Override
    public final void setTooltipForNextRenderPass(Tooltip tooltip,
                                                  ClientTooltipPositioner positioner,
                                                  boolean override) {
        if (tooltip != null && minecraft != null) {
            captureTooltip(tooltip.toCharSequence(minecraft), override, anchorsAtFocus(positioner));
        }
    }

    private static boolean anchorsAtFocus(ClientTooltipPositioner positioner) {
        return positioner instanceof BelowOrAboveWidgetTooltipPositioner;
    }

    private void captureTooltip(List<FormattedCharSequence> lines, boolean override,
                                boolean atFocus) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        if (pendingTooltip == null || override) {
            pendingTooltip = List.copyOf(lines);
            pendingTooltipKeyboard = atFocus;
        }
    }

    /** Unit-test seam: the tooltip captured so far in this frame, or {@code null}. */
    final List<FormattedCharSequence> capturedTooltip() {
        return pendingTooltip;
    }

    /** Unit-test seam: whether the captured tooltip is anchored at the focused control. */
    final boolean capturedTooltipAtFocus() {
        return pendingTooltip != null && pendingTooltipKeyboard;
    }

    // ---- modal layer ----------------------------------------------------------------------------

    /**
     * Opens {@code next} above the board (replacing an open modal). Focus of the underlying
     * widgets is parked and restored by {@link #closeModal()}.
     */
    public final void openModal(TacticalModal next) {
        Objects.requireNonNull(next, "modal");
        if (modal == null) {
            focusBeforeModal = getFocused();
            setFocused(null);
        }
        setDragging(false);
        modal = next;
        modalLaidOut = false;
        next.opened(this);
        layoutModal();
        narrate(next.narration());
    }

    /** Closes the open modal, if any, and gives the focus back to the parked widget. */
    public final void closeModal() {
        if (modal == null) {
            return;
        }
        modal = null;
        modalLaidOut = false;
        GuiEventListener restore = focusBeforeModal;
        focusBeforeModal = null;
        if (restore != null && children().contains(restore)) {
            setFocused(restore);
        }
    }

    /** Closes {@code which} only if it is still the open modal (a newer one stays open). */
    public final void closeModal(TacticalModal which) {
        if (which != null && modal == which) {
            closeModal();
        }
    }

    public final boolean hasModal() {
        return modal != null;
    }

    /** The open modal, or {@code null}. */
    public final TacticalModal modal() {
        return modal;
    }

    /**
     * The control that gets the focus back when the open modal closes ({@code null} without a
     * modal or when nothing had the focus).
     */
    protected final GuiEventListener parkedFocus() {
        return modal == null ? null : focusBeforeModal;
    }

    /**
     * Points the parked focus at {@code widget}: a screen that rebuilt its widgets while a modal
     * stayed open hands over the new instance of the control that opened it, so closing the
     * modal still gives the focus back. No-op without a modal.
     */
    protected final void reparkFocus(GuiEventListener widget) {
        if (modal != null) {
            focusBeforeModal = widget;
        }
    }

    private void layoutModal() {
        if (modal != null && font != null) {
            modal.layout(font, width, height);
            modalLaidOut = true;
        }
    }

    private void narrate(Component message) {
        if (minecraft == null || message == null || message.getString().isEmpty()) {
            return;
        }
        GameNarrator narrator = minecraft.getNarrator();
        if (narrator != null) {
            narrator.sayNow(message);
        }
    }

    // ---- input (logical coordinates; the modal and the tab shortcut come first) ------------------
    //
    // While an on* hook runs, a call back into these final methods (a migrated screen that still
    // writes super.mouseClicked(...) or super.keyPressed(...)) gets the plain vanilla behaviour:
    // coordinates are not divided a second time and the hook is not entered again.

    private boolean dispatchingInput;
    /** Mouse buttons whose press went to a modal; their drag and release never reach the board. */
    private int modalPressedButtons;

    private boolean dispatch(BooleanSupplier hook) {
        dispatchingInput = true;
        try {
            return hook.getAsBoolean();
        } finally {
            dispatchingInput = false;
        }
    }

    private static int buttonBit(int button) {
        return button >= 0 && button < Integer.SIZE ? 1 << button : 0;
    }

    /**
     * A board click opened a modal. Vanilla focuses (and starts dragging) the clicked widget only
     * after its handler returned, i.e. after {@link #openModal} parked the focus; park that widget
     * instead, so nothing under the modal is focused and the opener gets the focus back on close.
     */
    private void parkFocusForModal() {
        GuiEventListener focused = getFocused();
        if (focused != null) {
            focusBeforeModal = focused;
            setFocused(null);
        }
        setDragging(false);
    }

    @Override
    public final boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dispatchingInput) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        double x = UiScale.toLayout(mouseX, uiScale);
        double y = UiScale.toLayout(mouseY, uiScale);
        if (modal != null) {
            modalPressedButtons |= buttonBit(button);
            modal.mouseClicked(x, y, button);
            return true;
        }
        // A fresh board press; a release lost while a modal was open must not swallow its drag.
        modalPressedButtons &= ~buttonBit(button);
        boolean handled = dispatch(() -> onMouseClicked(x, y, button));
        if (modal != null) {
            parkFocusForModal();
            modalPressedButtons |= buttonBit(button);
        }
        return handled;
    }

    @Override
    public final boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dispatchingInput) {
            return super.mouseReleased(mouseX, mouseY, button);
        }
        double x = UiScale.toLayout(mouseX, uiScale);
        double y = UiScale.toLayout(mouseY, uiScale);
        boolean pressedOnModal = (modalPressedButtons & buttonBit(button)) != 0;
        modalPressedButtons &= ~buttonBit(button);
        if (modal != null) {
            modal.mouseReleased(x, y, button);
            return true;
        }
        if (pressedOnModal) {
            // The press closed the modal (confirm / cancel): its release is not a board click.
            setDragging(false);
            return true;
        }
        return dispatch(() -> onMouseReleased(x, y, button));
    }

    @Override
    public final boolean mouseDragged(double mouseX, double mouseY, int button,
                                      double dragX, double dragY) {
        if (dispatchingInput) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        double x = UiScale.toLayout(mouseX, uiScale);
        double y = UiScale.toLayout(mouseY, uiScale);
        double dx = UiScale.toLayout(dragX, uiScale);
        double dy = UiScale.toLayout(dragY, uiScale);
        if (modal != null) {
            modal.mouseDragged(x, y, button, dx, dy);
            return true;
        }
        if ((modalPressedButtons & buttonBit(button)) != 0) {
            return true;
        }
        return dispatch(() -> onMouseDragged(x, y, button, dx, dy));
    }

    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (dispatchingInput) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        double x = UiScale.toLayout(mouseX, uiScale);
        double y = UiScale.toLayout(mouseY, uiScale);
        if (modal != null) {
            modal.mouseScrolled(x, y, delta);
            return true;
        }
        return dispatch(() -> onMouseScrolled(x, y, delta));
    }

    @Override
    public final void mouseMoved(double mouseX, double mouseY) {
        if (dispatchingInput) {
            super.mouseMoved(mouseX, mouseY);
            return;
        }
        if (modal == null) {
            double x = UiScale.toLayout(mouseX, uiScale);
            double y = UiScale.toLayout(mouseY, uiScale);
            dispatch(() -> {
                onMouseMoved(x, y);
                return true;
            });
        }
    }

    @Override
    public final boolean isMouseOver(double mouseX, double mouseY) {
        double scale = dispatchingInput ? 1 : uiScale;
        return super.isMouseOver(mouseX / scale, mouseY / scale);
    }

    @Override
    public final boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (dispatchingInput) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (modal != null) {
            modal.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        int tabDelta = TacticalTabStrip.navigationDelta(keyCode, modifiers);
        if (tabDelta != 0 && tabStrip != null && tabStrip.visible && tabStrip.active) {
            tabStrip.cycle(tabDelta);
            return true;
        }
        return dispatch(() -> onKeyPressed(keyCode, scanCode, modifiers));
    }

    @Override
    public final boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (dispatchingInput) {
            return super.keyReleased(keyCode, scanCode, modifiers);
        }
        if (modal != null) {
            return true;
        }
        return dispatch(() -> onKeyReleased(keyCode, scanCode, modifiers));
    }

    @Override
    public final boolean charTyped(char codePoint, int modifiers) {
        if (dispatchingInput) {
            return super.charTyped(codePoint, modifiers);
        }
        if (modal != null) {
            modal.charTyped(codePoint, modifiers);
            return true;
        }
        return dispatch(() -> onCharTyped(codePoint, modifiers));
    }

    /** Mouse press in logical coordinates; default: the vanilla child dispatch. */
    protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected boolean onMouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected boolean onMouseDragged(double mouseX, double mouseY, int button,
                                     double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    protected boolean onMouseScrolled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    protected void onMouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
    }

    /** Key press without a modal and not taken by Ctrl+Tab; default: Esc, focus and widgets. */
    protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected boolean onKeyReleased(int keyCode, int scanCode, int modifiers) {
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    protected boolean onCharTyped(char codePoint, int modifiers) {
        return super.charTyped(codePoint, modifiers);
    }
}
