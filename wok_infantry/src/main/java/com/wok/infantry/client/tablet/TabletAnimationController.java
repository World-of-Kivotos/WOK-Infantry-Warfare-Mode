package com.wok.infantry.client.tablet;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.client.ClientBootstrap;
import com.wok.infantry.client.KeyBindingDefaults;
import com.wok.infantry.client.screen.DeviceArt;
import com.wok.infantry.client.screen.TacticalBoardChrome;
import com.wok.infantry.client.screen.TacticalLivery;
import com.wok.infantry.client.screen.TacticalPalette;
import com.wok.infantry.client.screen.TacticalScreen;
import com.wok.infantry.client.screen.UiScale;
import com.wok.infantry.integration.tacz.TaczStaminaAdapter;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The "take out the tablet" animation in the game (0.5.0-beta.4, IMPL_PLAN 2.2 / 3): one
 * {@link TabletMotion} for the whole client, driven by Forge events on the client thread.
 *
 * <ul>
 *   <li><b>Enter</b> ({@code ScreenEvent.Opening}, LOWEST: the final screen after Xaero or
 *   JourneyMap swapped it): world → tablet opens, another screen → tablet shows, tablet → another
 *   screen resets (DESIGN 2.4).</li>
 *   <li><b>Leave and advance</b> ({@code RenderTickEvent} START, HIGHEST): the tablet screen of the
 *   last frame is gone → close; then the progress advances and this frame's picture is worked
 *   out (IMPL_PLAN D13 order).</li>
 *   <li><b>Draw</b>: a {@link TacticalScreen} through its device hooks (backdrop alpha, a scope
 *   around the device and page); the full-screen pages (map, loadout) in {@code Render.Pre/Post};
 *   the put-away device in the HUD layer {@code wok_infantry:tablet_motion}.</li>
 *   <li><b>Input</b> (DESIGN 2.5): keys pass and finish the opening; a press, scroll or drag on a
 *   part of the screen that is not lit yet is swallowed and jumps to the end, one on a lit part is
 *   moved to where the player sees it; nothing is intercepted while closing.</li>
 *   <li><b>HUD</b>: hidden by {@link TabletHudPolicy} while the tablet is out.</li>
 *   <li><b>Tick</b>: no sprinting until the tablet is put away; interrupts (DESIGN 3.4).</li>
 * </ul>
 *
 * <p><b>Scheme A</b> (batch B3, the full setting): the tablet and both hands are drawn in the
 * first-person hand pass ({@code RenderHandEvent} HIGH, popped at LOWEST, {@link TabletHandPass});
 * the screen is not drawn before READ (0.61, {@code Render.Pre} cancelled), then drawn into the
 * frame's rectangle ({@link TabletReadScope}, or the map's {@code Render.Pre/Post}) with the wake
 * effects over it; the close copies the last frame once ({@link TabletFrameCapture}) and the HUD
 * layer replays it shrinking back into the hands. Without the hand pass the animation takes
 * scheme B (predicted when it opens, or seen on its first frame) or ends (later).
 */
public final class TabletAnimationController {
    /** Scheme A's 3D layer is in (batch B3): the full setting plays it unless it must fall back. */
    static final boolean SCHEME_A_READY = true;
    /** Name of the HUD layer that draws the put-away device. */
    public static final String OVERLAY_NAME = "tablet_motion";

    /**
     * UI acceptance seam (IMPL_PLAN 4.5): replaces the client option and the system properties.
     *
     * @param mode    setting, {@code null} = the configured one
     * @param forceB  play scheme B even when scheme A is possible
     * @param hand    held item, {@code null} = the real one
     * @param freezeP hold the animation at this p ({@code NaN} = do not freeze)
     * @param close   freeze the close (otherwise the opening)
     * @param link    link LED of the drawn device, {@code null} = the screen's own
     */
    public record AcceptanceOverride(TabletMode mode, boolean forceB, TabletHand hand,
                                     double freezeP, boolean close,
                                     TacticalBoardChrome.LinkState link) {
        public boolean freezes() {
            return !Double.isNaN(freezeP);
        }
    }

    /**
     * What the animation is doing this frame (uiTest checks, debugging). {@code a}: scheme A's
     * frame; {@code capture}: a C1 copy is ready; {@code handPass}: the hand pass reached the
     * animation in the last finished frame.
     */
    public record DebugSnapshot(TabletMotion.State state, TabletPath path, double p, int dir,
                                TabletMotion.Curve curve, TabletHand hand, TabletScreenKind kind,
                                boolean frozen, String phase, TabletPath2D.TermFrame term,
                                TabletPath2D.MapFrame map, TabletHudPolicy.Visibility hud,
                                TabletFrame a, boolean capture, boolean handPass) {
    }

    /** How the device looked on its last frame: what the put-away device is painted with. */
    record Face(TabletScreenKind kind, TacticalLivery.Livery livery, int linkColor,
                List<DeviceArt.BlankKey> keys) {
    }

    /** Valid GLFW key codes (glfwGetKey reports an error for the gaps). */
    private static final int[][] KEY_RANGES = {
            {GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_SPACE}, {GLFW.GLFW_KEY_APOSTROPHE,
            GLFW.GLFW_KEY_APOSTROPHE}, {GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_9},
            {GLFW.GLFW_KEY_SEMICOLON, GLFW.GLFW_KEY_SEMICOLON}, {GLFW.GLFW_KEY_EQUAL,
            GLFW.GLFW_KEY_EQUAL}, {GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_RIGHT_BRACKET},
            {GLFW.GLFW_KEY_GRAVE_ACCENT, GLFW.GLFW_KEY_GRAVE_ACCENT}, {GLFW.GLFW_KEY_WORLD_1,
            GLFW.GLFW_KEY_WORLD_2}, {GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_END},
            {GLFW.GLFW_KEY_CAPS_LOCK, GLFW.GLFW_KEY_PAUSE}, {GLFW.GLFW_KEY_F1, GLFW.GLFW_KEY_F25},
            {GLFW.GLFW_KEY_KP_0, GLFW.GLFW_KEY_KP_EQUAL}, {GLFW.GLFW_KEY_LEFT_SHIFT,
            GLFW.GLFW_KEY_MENU}};
    private static final int MOUSE_BUTTONS = 8;

    private static TabletMotion motion = new TabletMotion();
    private static final TabletPassMarks<Object> MARKS = new TabletPassMarks<>();
    private static final Set<Integer> HELD_KEYS = new HashSet<>();
    private static final TabletHandPass HANDS = new TabletHandPass();
    private static boolean installed;
    private static TabletMotion.Kind lastKind;
    private static Screen animScreen;
    private static Face face;
    private static Face closeFace;
    private static TabletPath2D.TermFrame termFrame;
    private static TabletPath2D.MapFrame mapFrame;
    /** This frame's scheme-A picture (opening or closing on A), else {@code null}. */
    private static TabletFrame aFrame;
    /** The device {@link #aFrame} paints (livery, link colour, keys). */
    private static Face aFace;
    private static int heldButtons;
    private static TabletEnvironment.Watch watch;
    private static AcceptanceOverride override;
    /** Render frame counter (RenderTick START). */
    private static long frameId;
    /** The first render frame of the current opening. */
    private static long openFrame = Long.MIN_VALUE;
    /** m11 of the last hand pass's projection (the FOV compensation follows it). */
    private static double lastP11 = TabletPose3D.P11_HAND;
    /** Bezel keys of the terminal being animated, read once per screen and layout size. */
    private static Screen keysScreen;
    private static int keysW;
    private static int keysH;
    private static List<DeviceArt.BlankKey> keysCache = List.of();

    private TabletAnimationController() {
    }

    // =========================================================================================
    // Installation
    // =========================================================================================

    /** Hooks the controller into the client once (from {@code ClientBootstrap.register}). */
    public static void install(IEventBus modBus) {
        if (installed) {
            return;
        }
        installed = true;
        TacticalScreen.installDeviceHooks(HOOKS);
        modBus.addListener(TabletAnimationController::registerOverlays);
        modBus.addListener(TabletAnimationController::registerReloadListener);
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(EventPriority.LOWEST, false, ScreenEvent.Opening.class,
                TabletAnimationController::onScreenOpening);
        bus.addListener(EventPriority.HIGHEST, false, TickEvent.RenderTickEvent.class,
                TabletAnimationController::onRenderTick);
        bus.addListener(EventPriority.NORMAL, false, TickEvent.RenderTickEvent.class,
                TabletAnimationController::onRenderTickEnd);
        // HIGH, not HIGHEST: a night-vision mod and Superb Warfare work at HIGHEST (DESIGN 6.4).
        bus.addListener(EventPriority.HIGH, false, RenderHandEvent.class,
                TabletAnimationController::onRenderHand);
        bus.addListener(EventPriority.LOWEST, true, RenderHandEvent.class,
                TabletAnimationController::onRenderHandEnd);
        bus.addListener(EventPriority.NORMAL, false, TickEvent.ClientTickEvent.class,
                TabletAnimationController::onClientTick);
        bus.addListener(EventPriority.NORMAL, false, ClientPlayerNetworkEvent.LoggingOut.class,
                event -> reset());
        bus.addListener(EventPriority.HIGHEST, false, ScreenEvent.KeyPressed.Pre.class,
                TabletAnimationController::onKeyPressed);
        bus.addListener(EventPriority.HIGHEST, true, ScreenEvent.KeyReleased.Pre.class,
                event -> HELD_KEYS.remove(event.getKeyCode()));
        bus.addListener(EventPriority.NORMAL, true, InputEvent.Key.class,
                TabletAnimationController::onWorldKey);
        bus.addListener(EventPriority.HIGHEST, false, ScreenEvent.MouseButtonPressed.Pre.class,
                TabletAnimationController::onMousePressed);
        bus.addListener(EventPriority.HIGHEST, true, ScreenEvent.MouseButtonReleased.Pre.class,
                event -> heldButtons &= ~buttonBit(event.getButton()));
        bus.addListener(EventPriority.HIGHEST, false, ScreenEvent.MouseScrolled.Pre.class,
                TabletAnimationController::onMouseScrolled);
        bus.addListener(EventPriority.HIGHEST, false, ScreenEvent.MouseDragged.Pre.class,
                TabletAnimationController::onMouseDragged);
        bus.addListener(EventPriority.NORMAL, true, InputEvent.MouseButton.Pre.class,
                TabletAnimationController::onWorldMouse);
        bus.addListener(EventPriority.NORMAL, false, InputEvent.MouseScrollingEvent.class,
                TabletAnimationController::onWorldScroll);
        bus.addListener(EventPriority.HIGHEST, false, RenderGuiOverlayEvent.Pre.class,
                TabletAnimationController::onOverlayPre);
        bus.addListener(EventPriority.HIGHEST, false, ScreenEvent.Render.Pre.class,
                TabletAnimationController::onScreenRenderPre);
        bus.addListener(EventPriority.LOWEST, false, ScreenEvent.Render.Post.class,
                TabletAnimationController::onScreenRenderPost);
    }

    private static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll(OVERLAY_NAME, TabletAnimationController::renderOverlay);
    }

    /** A resource reload (F3+T, a pack change) drops the GPU objects; they are rebuilt on demand. */
    private static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> releaseGpu());
    }

    /** Frees the textures and render targets of scheme A (logout, resource reload). */
    private static void releaseGpu() {
        TabletScreenTarget.release();
        TabletFrameCapture.release();
        TabletTextures.clear();
    }

    // =========================================================================================
    // Queries
    // =========================================================================================

    /** What the HUD hides this frame. */
    public static TabletHudPolicy.Visibility hudVisibility() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean tabletOpen = minecraft != null && minecraft.screen instanceof TabletSurface;
        return TabletHudPolicy.of(motion.state(), motion.path(), motion.p(), tabletOpen);
    }

    /** Whether the HUD (everything but chat, titles and subtitles) is hidden this frame. */
    public static boolean hudHidden() {
        return hudVisibility().hudHidden();
    }

    /** Whether the core's own HUD stands aside this frame ({@code HudFrame}). */
    public static boolean coreHudHidden() {
        return hudHidden();
    }

    /** Whether the crosshair is hidden this frame. */
    public static boolean crosshairHidden() {
        return hudVisibility().crosshairHidden();
    }

    /** Whether the tablet is moving (opening or closing). */
    public static boolean isAnimating() {
        return motion.animating();
    }

    /** The animation's state this frame. */
    public static DebugSnapshot debugSnapshot() {
        TabletMotion.Config config = motion.config();
        return new DebugSnapshot(motion.state(), motion.path(), motion.p(), motion.dir(),
                motion.curve(), config.hand(), config.screen(), motion.frozen(),
                motion.phaseName(), termFrame, mapFrame, hudVisibility(), aFrame,
                TabletFrameCapture.ready(), HANDS.ranIn(frameId) || HANDS.ranIn(frameId - 1));
    }

    // =========================================================================================
    // Acceptance seam
    // =========================================================================================

    /**
     * UI acceptance: plays every following animation with {@code value} instead of the client
     * option and system properties, starting from a fresh idle state.
     */
    public static void overrideForAcceptance(AcceptanceOverride value) {
        override = value;
        restart();
    }

    /** Ends {@link #overrideForAcceptance}; the animation starts afresh from idle. */
    public static void clearAcceptanceOverride() {
        override = null;
        restart();
    }

    private static void restart() {
        motion = new TabletMotion();
        termFrame = null;
        mapFrame = null;
        aFrame = null;
        aFace = null;
        closeFace = null;
        MARKS.clear();
        TabletFrameCapture.drop();
    }

    /** Back to idle and forgets everything (logout). */
    public static void reset() {
        motion.reset(now(), false);
        motion.drainEvents();
        if (motion.frozen()) {
            motion.unfreeze(now());
        }
        animScreen = null;
        face = null;
        closeFace = null;
        termFrame = null;
        mapFrame = null;
        aFrame = null;
        aFace = null;
        lastKind = null;
        watch = null;
        heldButtons = 0;
        keysScreen = null;
        keysCache = List.of();
        HELD_KEYS.clear();
        MARKS.clear();
        HANDS.clear();
        TabletMapFrame.clearCache();
        releaseGpu();
    }

    // =========================================================================================
    // Enter, leave, advance
    // =========================================================================================

    private static double now() {
        return System.nanoTime() / 1.0E6D;
    }

    private static TabletMotion.Kind kindOf(Screen screen) {
        if (screen == null) {
            return null;
        }
        return screen instanceof TabletSurface ? TabletMotion.Kind.TABLET : TabletMotion.Kind.OTHER;
    }

    private static void onScreenOpening(ScreenEvent.Opening event) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen to = event.getNewScreen();
        double now = now();
        boolean inWorld = minecraft.player != null && minecraft.level != null;
        TabletMotion.Enter enter = TabletMotion.enterAction(kindOf(event.getCurrentScreen()),
                kindOf(to), inWorld);
        switch (enter) {
            case OPEN -> {
                configure(minecraft, to);
                rememberHeldInput(minecraft);
                boolean reversing = motion.state() == TabletMotion.State.CLOSING;
                motion.open(now);
                if (!reversing) {
                    // The next render frame is this opening's first (D17: switch to B there).
                    openFrame = frameId + 1;
                }
                animScreen = to;
                closeFace = null;
                TabletFrameCapture.drop();
            }
            case SHOW -> {
                configure(minecraft, to);
                motion.show(now);
                animScreen = to;
                closeFace = null;
            }
            case RESET -> {
                if (motion.state() != TabletMotion.State.IDLE) {
                    WokInfantryMod.LOGGER.debug("[TabletAnim] {} ended by screen {}",
                            motion.state(), to == null ? "none" : to.getClass().getSimpleName());
                    motion.reset(now, false);
                }
                animScreen = null;
                closeFace = null;
            }
            case NONE -> {
                if (to instanceof TabletSurface surface) {
                    // Tablet to tablet (page tab, map ↔ squad): the animation carries on.
                    animScreen = to;
                    motion.configure(motion.config().withScreen(surface.tabletKind()));
                }
            }
        }
        drain(now);
    }

    /** The settings an animation into {@code screen} plays with. */
    private static void configure(Minecraft minecraft, Screen screen) {
        TabletScreenKind kind = TabletSurface.kindOf(screen);
        AcceptanceOverride o = override;
        TabletSettings.Freeze freeze = activeFreeze();
        TabletMode mode = o != null && o.mode() != null ? o.mode() : TabletSettings.mode();
        TabletHand hand = o != null && o.hand() != null ? o.hand()
                : freeze != null && freeze.hand() != null ? freeze.hand()
                : TabletEnvironment.hand(minecraft.player);
        boolean forceB = o != null && o.forceB()
                || freeze != null && freeze.path() == TabletPath.B2D;
        boolean forceA = freeze != null && freeze.path() == TabletPath.A3D;
        boolean degrade = !SCHEME_A_READY || forceB
                || !forceA && TabletEnvironment.predictNoHands(minecraft);
        motion.configure(new TabletMotion.Config(mode, degrade, hand,
                TabletAnimationModel.CAPTURE_DEFAULT, kind));
    }

    /** The freeze in effect: the acceptance override's, else {@code wok.ui.tabletFreeze}. */
    private static TabletSettings.Freeze activeFreeze() {
        AcceptanceOverride o = override;
        if (o != null) {
            return o.freezes() ? new TabletSettings.Freeze(o.freezeP(), o.close(), o.hand(),
                    o.forceB() ? TabletPath.B2D : null) : null;
        }
        return TabletSettings.freeze();
    }

    /** Applies the queued events: a state entered may be frozen (sounds come with batch B4). */
    private static void drain(double now) {
        for (TabletMotion.Event event : motion.drainEvents()) {
            if (event.type() == TabletMotion.Event.Type.STATE) {
                applyFreeze(TabletMotion.State.valueOf(event.name()), now);
            }
        }
    }

    private static void applyFreeze(TabletMotion.State entered, double now) {
        TabletSettings.Freeze freeze = activeFreeze();
        boolean wanted = freeze != null
                && (entered == TabletMotion.State.OPENING && !freeze.close()
                || entered == TabletMotion.State.CLOSING && freeze.close());
        if (wanted) {
            motion.freeze(freeze.p());
        } else if (motion.frozen()) {
            motion.unfreeze(now);
        }
    }

    private static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        double now = now();
        frameId++;
        Screen screen = minecraft.screen;
        TabletMotion.Kind current = kindOf(screen);
        if (TabletMotion.leaveAction(lastKind, current)
                && motion.state() != TabletMotion.State.IDLE) {
            closeFace = faceForClose();
            // D13: the main target still holds the last finished frame; copy it before the close.
            captureForClose(minecraft);
            motion.close(now);
        }
        lastKind = current;
        if (current == TabletMotion.Kind.TABLET && motion.state() == TabletMotion.State.IDLE) {
            // A tablet screen that is on display without an animation (an interrupted opening):
            // it is simply shown.
            configure(minecraft, screen);
            motion.show(now);
            animScreen = screen;
        }
        motion.update(now);
        drain(now);
        if (motion.state() == TabletMotion.State.IDLE) {
            closeFace = null;
            TabletFrameCapture.drop();
            if (current != TabletMotion.Kind.TABLET) {
                animScreen = null;
            }
        }
        computeFrames(minecraft, now);
    }

    /**
     * RenderTick END (D17): scheme A animating but the hand pass never reached it this frame (F1,
     * a mod cancelling the hand pass, a camera that is not the player). On the opening's first
     * frame it restarts on scheme B from the time it opened; later an opening is shown and a close
     * ends.
     */
    private static void onRenderTickEnd(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        TabletHandPass.Reaction reaction = TabletHandPass.react(motion.path(), motion.state(),
                HANDS.ranIn(frameId), openFrame == frameId);
        if (reaction == TabletHandPass.Reaction.NONE || motion.frozen()) {
            return;
        }
        double now = now();
        WokInfantryMod.LOGGER.debug("[TabletAnim] no hand pass at p={} ({}): {}", motion.p(),
                motion.state(), reaction);
        if (reaction == TabletHandPass.Reaction.SWITCH_TO_B) {
            motion.switchPath(TabletPath.B2D, now - motion.elapsed(now));
        } else {
            motion.loseHandRender(now);
        }
        drain(now);
        aFrame = null;
    }

    /**
     * The C1 copy of a scheme-A close (D13, DESIGN 3.4): the block the device took on the last
     * frame, copied before the close starts. When it cannot be made this close starts at READ (the
     * old close), only this once.
     */
    private static void captureForClose(Minecraft minecraft) {
        TabletMotion.Config config = motion.config();
        double last = motion.p();
        if (!TabletCapturePlan.wants(motion.path(), config.capture(), last)) {
            return;
        }
        boolean ok = false;
        try {
            Face put = closeFace;
            TabletScreenKind kind = put != null ? put.kind() : config.screen();
            TabletPose3D.Context ctx = contextFor(minecraft, kind);
            if (ctx != null) {
                TabletFrame from = TabletPose3D.frame(last, ctx, new TabletPose3D.FrameQuery(false,
                        motion.curve(), true, last, config.hand(), 0.0D));
                TabletCapturePlan.Region region = TabletCapturePlan.region(from,
                        ctx.term().pxW(), ctx.term().pxH());
                int mask = 0;
                if (!ctx.map() && put != null) {
                    TabletTextures.Face art = faceArt(ctx, put);
                    mask = art == null ? -1 : art.textureId();
                }
                ok = mask >= 0 && TabletFrameCapture.capture(region, mask,
                        from.capture().fromPlanePx(), from.capture().fromSmooth(), last);
            }
        } catch (RuntimeException exception) {
            ok = false;
        }
        if (!ok) {
            motion.configure(config.withCapture(false));
        }
    }

    /**
     * The device the put-away animation draws: a terminal's last frame (with its bezel keys as
     * blank caps), or a full-screen page's frame in the viewer's livery.
     */
    private static Face faceForClose() {
        if (animScreen instanceof TabletSurface surface && !surface.tabletKind().isTerminal()) {
            return mapFace(surface);
        }
        if (animScreen instanceof TacticalScreen) {
            // The screen as its next frame would paint it (also when it never drew: closed before
            // READ on scheme A); keys read once per screen.
            try {
                Face current = currentFace();
                if (current != null) {
                    return current;
                }
            } catch (RuntimeException ignored) {
                // Fall back to the last frame drawn.
            }
        }
        Face last = face;
        TabletScreenKind kind = last != null ? last.kind() : motion.config().screen();
        TacticalLivery.Livery livery = last != null ? last.livery() : TacticalLivery.current();
        int link = last != null ? last.linkColor()
                : TacticalBoardChrome.LinkState.OK.ledColor(livery.skin());
        List<DeviceArt.BlankKey> keys = List.of();
        if (animScreen instanceof TacticalScreen screen && kind.isTerminal()) {
            try {
                keys = screen.faceKeys();
            } catch (RuntimeException ignored) {
                // A screen torn down mid-way: the device falls without its key caps.
            }
        }
        return new Face(kind, livery, link, keys);
    }

    /** This frame's picture: scheme A's frame, or scheme B's / the quick setting's. */
    private static void computeFrames(Minecraft minecraft, double now) {
        termFrame = null;
        mapFrame = null;
        aFrame = null;
        aFace = null;
        TabletMotion.State state = motion.state();
        if (state != TabletMotion.State.OPENING && state != TabletMotion.State.CLOSING) {
            return;
        }
        TabletPath path = motion.path();
        if (path == TabletPath.A3D) {
            computeFrameA(minecraft, now, state == TabletMotion.State.OPENING);
            return;
        }
        if (path != TabletPath.B2D && path != TabletPath.QUICK) {
            return;
        }
        Window window = minecraft.getWindow();
        int guiW = window.getGuiScaledWidth();
        int guiH = window.getGuiScaledHeight();
        int scale = Math.max(1, (int) Math.round(window.getGuiScale()));
        TabletUnits term = TabletUnits.terminal(guiW, guiH, scale, UiScale.factor(minecraft))
                .withPhysical(window.getWidth(), window.getHeight());
        boolean open = state == TabletMotion.State.OPENING;
        TabletScreenKind kind = open ? TabletSurface.kindOf(minecraft.screen)
                : closeFace != null ? closeFace.kind() : null;
        if (kind == null) {
            kind = motion.config().screen();
        }
        boolean quick = path == TabletPath.QUICK;
        if (kind.isTerminal()) {
            termFrame = TabletPath2D.termFrame(motion.p(), term, open, quick);
        } else {
            mapFrame = TabletPath2D.mapFrame(motion.p(),
                    TabletUnits.fullscreen(guiW, guiH, scale), term, open, quick);
        }
    }

    /** Scheme A: the frame of this p (IMPL_PLAN 4.3 step 1) and the device it paints. */
    private static void computeFrameA(Minecraft minecraft, double now, boolean open) {
        Face device = open ? currentFace() : closeFace;
        TabletScreenKind kind = device != null ? device.kind() : motion.config().screen();
        TabletPose3D.Context ctx = contextFor(minecraft, kind);
        if (ctx == null) {
            return;
        }
        TabletMotion.PoseContext pc = motion.poseContext();
        TabletPose3D.FrameQuery query = open
                ? new TabletPose3D.FrameQuery(true, motion.curve(), true, 1.0D,
                motion.config().hand(), motion.carry(now))
                : new TabletPose3D.FrameQuery(false, pc.curve(), pc.capture(), pc.captureFromP(),
                motion.config().hand(), motion.carry(now));
        aFrame = TabletPose3D.frame(motion.p(), ctx, query);
        aFace = device;
        if (open && device != null) {
            // Paint and upload the front face on the opening's first frames (the gun is still
            // going down), not on the frame the tablet first shows.
            faceArt(ctx, device);
        }
    }

    /** The context of an animated screen of {@code kind} in the current window. */
    private static TabletPose3D.Context contextFor(Minecraft minecraft, TabletScreenKind kind) {
        Window window = minecraft.getWindow();
        if (window == null || window.getWidth() <= 0 || window.getHeight() <= 0) {
            return null;
        }
        int guiW = window.getGuiScaledWidth();
        int guiH = window.getGuiScaledHeight();
        int scale = Math.max(1, (int) Math.round(window.getGuiScale()));
        TabletUnits term = TabletUnits.terminal(guiW, guiH, scale, UiScale.factor(minecraft))
                .withPhysical(window.getWidth(), window.getHeight());
        TabletScreenKind k = kind == null ? TabletScreenKind.SQUAD : kind;
        TabletUnits units = k.isTerminal() ? term
                : TabletUnits.fullscreen(guiW, guiH, scale).withPhysical(window.getWidth(),
                window.getHeight());
        boolean invisible = minecraft.player != null && minecraft.player.isInvisible();
        return TabletPose3D.Context.of(term, units, k, lastP11, invisible);
    }

    /**
     * The device of the screen being opened: a terminal paints its own livery, its link LED in
     * its own palette and its bezel keys as blank caps; a full-screen page has no bezel keys
     * (review P3 c).
     */
    private static Face currentFace() {
        Screen screen = animScreen;
        if (screen instanceof TacticalScreen tactical) {
            TacticalLivery.Livery livery = tactical.resolveDeviceLivery();
            int color;
            try (TacticalPalette.Applied ignored = TacticalPalette.push(
                    tactical.devicePalette(livery))) {
                color = linkState(tactical).ledColor(livery.skin());
            }
            return new Face(tactical.tabletKind(), livery, color, keysOf(tactical));
        }
        if (screen instanceof TabletSurface surface) {
            return mapFace(surface);
        }
        return null;
    }

    /** {@link TacticalScreen#faceKeys()} read once per screen and layout size (it measures text). */
    private static List<DeviceArt.BlankKey> keysOf(TacticalScreen screen) {
        if (screen != keysScreen || screen.width != keysW || screen.height != keysH) {
            List<DeviceArt.BlankKey> keys;
            try {
                keys = screen.faceKeys();
            } catch (RuntimeException exception) {
                keys = List.of();
            }
            keysScreen = screen;
            keysW = screen.width;
            keysH = screen.height;
            keysCache = keys;
        }
        return keysCache;
    }

    /** The front-face texture of {@code device} at {@code ctx}'s size ({@code null} on failure). */
    private static TabletTextures.Face faceArt(TabletPose3D.Context ctx, Face device) {
        TabletD2Geometry g = ctx.geom();
        List<DeviceArt.BlankKey> keys = device.kind().isTerminal() ? device.keys() : List.of();
        return TabletTextures.face(new TabletTextures.FaceKey(g.w(), g.h(), g.density(),
                device.livery(), keys, device.linkColor(), ctx.termSmooth()));
    }

    // =========================================================================================
    // Hand pass (scheme A's 3D layer)
    // =========================================================================================

    /** Scheme A is opening or closing with a frame this render frame. */
    private static boolean schemeA() {
        return aFrame != null && motion.path() == TabletPath.A3D && motion.animating();
    }

    /**
     * RenderHandEvent HIGH: the first hand event of a frame draws the tablet and the hands from
     * the identity; every hand event pushes the held item's press-down (p &lt; 0.30, popped at
     * LOWEST) or is cancelled (from 0.30 on).
     */
    private static void onRenderHand(RenderHandEvent event) {
        boolean active = schemeA();
        TabletHandPass.Decision decision = HANDS.onHigh(active, motion.p(), frameId);
        if (decision.action() == TabletHandPass.Action.NONE) {
            return;
        }
        TabletFrame frame = aFrame;
        PoseStack pose = event.getPoseStack();
        if (decision.drawTablet()) {
            double p11 = RenderSystem.getProjectionMatrix().m11();
            if (p11 > 0.0D && Double.isFinite(p11)) {
                lastP11 = p11;
            }
            drawLayer3D(event, frame, pose);
        }
        if (decision.action() == TabletHandPass.Action.CANCEL) {
            event.setCanceled(true);
            return;
        }
        TabletEasing ease = motion.config().hand() == TabletHand.EMPTY
                ? TabletAnimationModel.EASE_EMPTY_ARM : TabletAnimationModel.EASE_GUN;
        pose.pushPose();
        TabletPoseOps.apply(pose, TabletPose3D.gunOps(motion.p(), ease));
        HANDS.pushed(event);
    }

    private static void drawLayer3D(RenderHandEvent event, TabletFrame frame, PoseStack pose) {
        boolean tablet = frame.tablet().draw();
        boolean hands = frame.hands().draw();
        if (!tablet && !hands) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Face device = aFace;
        TabletTextures.Face art = tablet && device != null ? faceArt(frame.ctx(), device) : null;
        try {
            TabletRenderer.draw(pose, event.getMultiBufferSource(), event.getPackedLight(), frame,
                    TabletPose3D.kf(lastP11), art, device == null ? null : device.livery().skin(),
                    minecraft.player, tablet, hands);
        } catch (RuntimeException exception) {
            if (!drawWarned) {
                drawWarned = true;
                WokInfantryMod.LOGGER.warn("[TabletAnim] 3D tablet not drawn", exception);
            }
        }
    }

    /** The 3D layer's failure was logged (once per session). */
    private static boolean drawWarned;

    /** RenderHandEvent LOWEST (cancelled ones too): pops what HIGH pushed for this event. */
    private static void onRenderHandEnd(RenderHandEvent event) {
        if (HANDS.popFor(event)) {
            event.getPoseStack().popPose();
        }
    }

    // =========================================================================================
    // Tick: sprinting and interrupts
    // =========================================================================================

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            if (motion.state() != TabletMotion.State.IDLE) {
                double now = now();
                motion.reset(now, false);
                drain(now);
            }
            watch = null;
            return;
        }
        // DESIGN 3.7: no sprinting until the tablet is put away (both phases, as the stamina
        // controller does: START before movement, END after other tick hooks).
        if (motion.state() != TabletMotion.State.IDLE && player.isSprinting()) {
            player.setSprinting(false);
        }
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        TabletEnvironment.Watch current = TabletEnvironment.Watch.of(minecraft);
        TabletMotion.State state = motion.state();
        if (state == TabletMotion.State.OPENING || state == TabletMotion.State.CLOSING) {
            boolean closing = state == TabletMotion.State.CLOSING;
            boolean world = closing && minecraft.screen == null;
            TabletMotion.Interrupt cause = TabletEnvironment.detect(watch, current, closing,
                    world && minecraft.options.keyAttack.isDown(),
                    world && minecraft.options.keyUse.isDown(), world && aiming(player));
            if (cause != null) {
                double now = now();
                if (motion.interrupt(now, cause)) {
                    WokInfantryMod.LOGGER.debug("[TabletAnim] {} ended by {}", state, cause);
                    drain(now);
                }
            }
        }
        watch = current;
    }

    private static boolean aiming(LocalPlayer player) {
        try {
            return TaczStaminaAdapter.isAiming(player);
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    // =========================================================================================
    // Input (DESIGN 2.5)
    // =========================================================================================

    private static int buttonBit(int button) {
        return button >= 0 && button < Integer.SIZE ? 1 << button : 0;
    }

    /** Keys and mouse buttons held when the tablet comes out: their repeats are no new input. */
    private static void rememberHeldInput(Minecraft minecraft) {
        HELD_KEYS.clear();
        heldButtons = 0;
        if (minecraft.getWindow() == null) {
            return;
        }
        long window = minecraft.getWindow().getWindow();
        for (int[] range : KEY_RANGES) {
            for (int key = range[0]; key <= range[1]; key++) {
                if (InputConstants.isKeyDown(window, key)) {
                    HELD_KEYS.add(key);
                }
            }
        }
        for (int button = 0; button < MOUSE_BUTTONS; button++) {
            if (GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS) {
                heldButtons |= buttonBit(button);
            }
        }
    }

    private static boolean opening(Screen screen) {
        return motion.state() == TabletMotion.State.OPENING
                && screen != null && screen == Minecraft.getInstance().screen;
    }

    private static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        Screen screen = event.getScreen();
        if (!opening(screen)) {
            return;
        }
        int key = event.getKeyCode();
        int scan = event.getScanCode();
        TabletMotion.Input input = TabletInput.classifyKey(key == GLFW.GLFW_KEY_ESCAPE,
                ClientBootstrap.isKey(KeyBindingDefaults.Binding.TERMINAL, key, scan),
                ClientBootstrap.isKey(KeyBindingDefaults.Binding.TACTICAL_MAP, key, scan),
                hotbarKey(Minecraft.getInstance(), key, scan));
        if (TabletInput.finishes(motion.path(), motion.state(), motion.p(), input,
                TabletSurface.kindOf(screen), HELD_KEYS.contains(key))) {
            double now = now();
            motion.finish(now);
            drain(now);
        }
    }

    private static boolean hotbarKey(Minecraft minecraft, int key, int scan) {
        if (minecraft.options == null) {
            return false;
        }
        for (KeyMapping mapping : minecraft.options.keyHotbarSlots) {
            if (mapping.matches(key, scan)) {
                return true;
            }
        }
        return false;
    }

    private static void onWorldKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_RELEASE) {
            HELD_KEYS.remove(event.getKey());
        }
    }

    private static TabletInput.Delivery decide(Screen screen, TabletMotion.Input input,
                                               double x, double y, boolean heldSinceOpen) {
        Minecraft minecraft = Minecraft.getInstance();
        int guiScale = Math.max(1, (int) Math.round(minecraft.getWindow().getGuiScale()));
        return TabletInput.decide(motion.path(), motion.state(), motion.p(), input,
                TabletSurface.kindOf(screen), termFrame, mapFrame,
                motion.path() == TabletPath.A3D ? aFrame : null, x, y,
                UiScale.factor(minecraft), guiScale, heldSinceOpen);
    }

    /** Carries out a delivery: cancel, jump to the end, then hand the event over (moved). */
    private static void apply(TabletInput.Delivery delivery, ScreenEvent event, Runnable deliver) {
        if (delivery.cancel()) {
            event.setCanceled(true);
        }
        if (delivery.jump()) {
            double now = now();
            motion.jumpShown(now);
            drain(now);
            termFrame = null;
            mapFrame = null;
            aFrame = null;
        }
        if (delivery.deliver()) {
            deliver.run();
        }
    }

    private static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        if (!opening(screen)) {
            return;
        }
        TabletInput.Delivery delivery = decide(screen, TabletMotion.Input.MOUSE_DOWN,
                event.getMouseX(), event.getMouseY(), false);
        apply(delivery, event, () -> screen.mouseClicked(delivery.x(), delivery.y(),
                event.getButton()));
    }

    private static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        Screen screen = event.getScreen();
        if (!opening(screen)) {
            return;
        }
        TabletInput.Delivery delivery = decide(screen, TabletMotion.Input.SCROLL,
                event.getMouseX(), event.getMouseY(), false);
        apply(delivery, event, () -> screen.mouseScrolled(delivery.x(), delivery.y(),
                event.getScrollDelta()));
    }

    private static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        Screen screen = event.getScreen();
        if (!opening(screen)) {
            return;
        }
        boolean held = (heldButtons & buttonBit(event.getMouseButton())) != 0;
        TabletInput.Delivery delivery = decide(screen, TabletMotion.Input.DRAG,
                event.getMouseX(), event.getMouseY(), held);
        apply(delivery, event, () -> screen.mouseDragged(delivery.x(), delivery.y(),
                event.getMouseButton(), event.getDragX(), event.getDragY()));
    }

    /** No screen, closing: firing or aiming puts the tablet away at once (DESIGN 3.4). */
    private static void onWorldMouse(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == GLFW.GLFW_RELEASE) {
            heldButtons &= ~buttonBit(event.getButton());
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getAction() != GLFW.GLFW_PRESS || minecraft.screen != null
                || motion.state() != TabletMotion.State.CLOSING || minecraft.options == null) {
            return;
        }
        TabletMotion.Interrupt cause = minecraft.options.keyAttack.matchesMouse(event.getButton())
                ? TabletMotion.Interrupt.FIRE
                : minecraft.options.keyUse.matchesMouse(event.getButton())
                ? TabletMotion.Interrupt.AIM : null;
        if (cause != null) {
            double now = now();
            if (motion.interrupt(now, cause)) {
                drain(now);
            }
        }
    }

    /** No screen, closing: the wheel switches the held item and ends the close. */
    private static void onWorldScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || motion.state() != TabletMotion.State.CLOSING) {
            return;
        }
        double now = now();
        if (motion.interrupt(now, TabletMotion.Interrupt.HOTBAR)) {
            drain(now);
        }
    }

    // =========================================================================================
    // HUD
    // =========================================================================================

    private static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        TabletHudPolicy.Visibility visibility = hudVisibility();
        if (!visibility.any() || event.getOverlay() == null) {
            return;
        }
        String id = event.getOverlay().id().toString();
        if (TabletHudPolicy.cancels(id, visibility) || TabletHudPolicy.VIGNETTE.equals(id)
                && TabletHudPolicy.hidesVignette(motion.state(), motion.path(), motion.p(),
                Minecraft.getInstance().screen instanceof TabletSurface)) {
            event.setCanceled(true);
        }
    }

    // =========================================================================================
    // Drawing
    // =========================================================================================

    /** The terminal frame {@code screen} draws this frame while opening, or {@code null}. */
    private static TabletPath2D.TermFrame openingFrameFor(TacticalScreen screen) {
        return opening(screen) ? termFrame : null;
    }

    /** The scheme-A frame {@code screen} draws this frame while opening, or {@code null}. */
    private static TabletFrame openingFrameA(Screen screen) {
        return opening(screen) && schemeA() ? aFrame : null;
    }

    private static final TacticalScreen.DeviceHooks HOOKS = new TacticalScreen.DeviceHooks() {
        @Override
        public float backdropAlpha(TacticalScreen screen) {
            TabletFrame a = openingFrameA(screen);
            if (a != null) {
                return a.ui().identity() ? 1.0F : (float) a.fx().backdrop().alpha();
            }
            TabletPath2D.TermFrame frame = openingFrameFor(screen);
            return frame == null ? 1.0F : (float) frame.backdrop();
        }

        @Override
        public TacticalScreen.DeviceScope beginDevice(GuiGraphics graphics, TacticalScreen screen) {
            rememberFace(screen);
            TabletFrame a = openingFrameA(screen);
            if (a != null) {
                Minecraft minecraft = Minecraft.getInstance();
                int guiScale = Math.max(1, (int) Math.round(minecraft.getWindow().getGuiScale()));
                int mask = 0;
                if (a.ui().smooth() && aFace != null) {
                    TabletTextures.Face art = faceArt(a.ctx(), aFace);
                    mask = art == null ? 0 : art.textureId();
                }
                return TabletReadScope.of(graphics, a, guiScale, screen.uiScale(), mask);
            }
            return TabletDeviceScope.of(graphics, openingFrameFor(screen));
        }

        @Override
        public float shadowAlpha(TacticalScreen screen) {
            TabletFrame a = openingFrameA(screen);
            return a == null || a.ui().identity() ? 1.0F : (float) a.fx().shadowAlpha();
        }
    };

    /** Called inside the screen's palette scope, so a waiting LED takes the page's amber. */
    private static void rememberFace(TacticalScreen screen) {
        if (screen != Minecraft.getInstance().screen) {
            return;
        }
        TacticalLivery.Livery livery = screen.deviceLivery();
        face = new Face(screen.tabletKind(), livery, linkState(screen).ledColor(livery.skin()),
                List.of());
    }

    private static TacticalBoardChrome.LinkState linkState(TabletSurface surface) {
        AcceptanceOverride o = override;
        if (o != null && o.link() != null) {
            return o.link();
        }
        TacticalBoardChrome.LinkState link = surface.tabletLink();
        return link == null ? TacticalBoardChrome.LinkState.OK : link;
    }

    /**
     * A full-screen page's device (the map frame has no screen palette of its own): the viewer's
     * livery, the link LED in that livery's board palette, so a waiting LED is the same amber the
     * terminal shows.
     */
    private static Face mapFace(TabletSurface surface) {
        TacticalLivery.Livery livery = TacticalLivery.current();
        int color;
        try (TacticalPalette.Applied ignored = TacticalPalette.push(
                livery.palette(TacticalLivery.Scope.BOARD))) {
            color = linkState(surface).ledColor(livery.skin());
        }
        return new Face(surface.tabletKind(), livery, color, List.of());
    }

    private static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        Screen screen = event.getScreen();
        if (screen != Minecraft.getInstance().screen || !(screen instanceof TabletSurface surface)) {
            return;
        }
        TabletFrame a = openingFrameA(screen);
        if (a != null) {
            preA(event, screen, surface, a);
            return;
        }
        if (surface.tabletKind().isTerminal()) {
            return;
        }
        TabletPath2D.MapFrame frame = opening(screen) ? mapFrame : null;
        if (frame == null || frame.identity()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Face current = mapFace(surface);
        face = current;
        TabletMapFrame.drawFrame(graphics, Minecraft.getInstance().font, frame, current.livery(),
                current.linkColor());
        if (frame.sleepAlpha() >= 1.0D) {
            // The glass is still fully dark: the page is not drawn at all.
            event.setCanceled(true);
            MARKS.begin(screen, TabletPassMarks.Mark.CANCELLED, frame);
            return;
        }
        TabletPath2D.Box inner = frame.inner();
        UiScale.enableScissor(graphics, inner.x(), inner.y(), inner.right(), inner.bottom());
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(frame.offsetX(), frame.offsetY(), 0.0F);
        MARKS.begin(screen, TabletPassMarks.Mark.PUSHED, frame);
    }

    /**
     * Scheme A, opening: before READ (p &lt; 0.61) no page at all, the 3D tablet is all there is;
     * a terminal then draws through its device hooks; a full-screen page is laid into its map
     * rectangle here (smooth downscale on every tier, else a whole-pixel transform).
     */
    private static void preA(ScreenEvent.Render.Pre event, Screen screen, TabletSurface surface,
                             TabletFrame a) {
        if (!a.ui().draw()) {
            event.setCanceled(true);
            MARKS.begin(screen, TabletPassMarks.Mark.CANCELLED, null);
            return;
        }
        if (surface.tabletKind().isTerminal() || a.ui().identity() || a.ui().rectPx() == null) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        if (a.ui().smooth() && TabletScreenTarget.begin(graphics)) {
            MARKS.begin(screen, TabletPassMarks.Mark.SMOOTH, a);
            return;
        }
        TabletReadScope.pushPixel(graphics.pose(), a.ui().rectPx(), guiScale(), 1);
        MARKS.begin(screen, TabletPassMarks.Mark.SCALED, a);
    }

    private static int guiScale() {
        return Math.max(1, (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale()));
    }

    private static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        TabletPassMarks.Open<Object> pass = MARKS.end(event.getScreen());
        if (pass.mark() == TabletPassMarks.Mark.NONE) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        switch (pass.mark()) {
            case PUSHED -> {
                graphics.pose().popPose();
                UiScale.disableScissor(graphics);
            }
            case SCALED -> graphics.pose().popPose();
            case SMOOTH -> {
                if (pass.frame() instanceof TabletFrame a) {
                    TabletScreenTarget.end(graphics, a.ui().rectPx(), guiScale(), true, 0, 0.0F, 0.0F);
                } else {
                    TabletScreenTarget.abort();
                }
            }
            default -> {
            }
        }
        if (pass.frame() instanceof TabletPath2D.MapFrame frame) {
            TabletMapFrame.drawEffects(graphics, frame);
        } else if (pass.frame() instanceof TabletFrame a) {
            int scale = guiScale();
            TabletEffects2D.draw(graphics, TabletEffects2D.wakeFills(a.glassPx(), a.fx(), scale),
                    scale, TabletMapFrame.EFFECT_Z);
        }
    }

    /** HUD layer: the device being put away on scheme B or the quick setting (DESIGN 4.1–4.3). */
    private static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTick,
                                      int width, int height) {
        Face put = closeFace;
        if (motion.state() != TabletMotion.State.CLOSING || put == null) {
            return;
        }
        if (motion.path() == TabletPath.A3D) {
            TabletFrame a = aFrame;
            if (a != null && a.capture().draw() && TabletFrameCapture.ready()) {
                drawCaptureAway(graphics, a, put);
            }
            return;
        }
        Font font = Minecraft.getInstance().font;
        if (termFrame != null) {
            drawTerminalAway(graphics, font, termFrame, put);
        } else if (mapFrame != null) {
            TabletMapFrame.drawFrame(graphics, font, mapFrame, put.livery(), put.linkColor());
            TabletMapFrame.drawEffects(graphics, mapFrame);
        }
    }

    /**
     * Scheme A's C1 close (DESIGN 3.4, IMPL_PLAN 4.4 step 3): a terminal's backdrop (alpha 1 → 0
     * with the shrink, review V7) and its drop shadow (fading out with the wake bands), then the
     * copied frame laid into this frame's rectangle, then the wake effects closing over the glass.
     * A full-screen page has neither backdrop nor shadow.
     */
    private static void drawCaptureAway(GuiGraphics graphics, TabletFrame a, Face put) {
        int scale = guiScale();
        TabletPose3D.Context ctx = a.ctx();
        PoseStack pose = graphics.pose();
        if (!ctx.map()) {
            int factor = Math.max(1, ctx.term().factor());
            pose.pushPose();
            if (factor != 1) {
                pose.scale(factor, factor, 1.0F);
            }
            DeviceArt.drawBackdrop(graphics, ctx.term().lpW(), ctx.term().lpH(),
                    (float) a.fx().backdrop().alpha());
            pose.popPose();
            drawShadow(graphics, a, put, scale);
        }
        TabletFrameCapture.replay(graphics, a.capture().fromRectPx(), a.capture().rectPx(),
                a.capture().smooth(), scale);
        TabletEffects2D.draw(graphics, TabletEffects2D.wakeFills(a.glassPx(), a.fx(), scale), scale,
                TabletMapFrame.EFFECT_Z);
    }

    /** The case's drop shadow under the replayed frame, in layout pixels of the moving plane. */
    private static void drawShadow(GuiGraphics graphics, TabletFrame a, Face put, int scale) {
        TabletPose3D.RectPx plane = a.capture().rectPx();
        double alpha = a.fx().shadowAlpha();
        TabletD2Geometry g = a.ctx().geom();
        if (plane == null || !(alpha > 0.0D) || g.w() <= 0) {
            return;
        }
        int faded = (int) Math.round(((DeviceArt.SHADOW >>> 24) & 0xFF) * Math.min(1.0D, alpha));
        if (faded <= 0) {
            return;
        }
        int color = faded << 24 | (DeviceArt.SHADOW & 0xFFFFFF);
        DeviceArt.Plate plate = DeviceArt.plate(g.w(), g.h(), g.density(), put.livery());
        DeviceArt.Runs under = plate.under();
        int runs = plate.shadowRuns();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(plane.x() / scale, plane.y() / scale, 0.0D);
        float k = (float) (plane.w() / g.w() / scale);
        pose.scale(k, k, 1.0F);
        for (int i = 0; i < runs; i++) {
            graphics.fill(under.left(i), under.top(i), under.right(i), under.bottom(i), color);
        }
        pose.popPose();
    }

    /**
     * The terminal falling away (DESIGN 4.1 close): backdrop fading, the sleeping device with its
     * blank key caps and a clear glass well, moved down by whole layout pixels, then the clear
     * glass (GLASS alpha 0.20) over the opening.
     */
    private static void drawTerminalAway(GuiGraphics graphics, Font font,
                                         TabletPath2D.TermFrame frame, Face put) {
        int factor = UiScale.factor();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        if (factor != 1) {
            pose.scale(factor, factor, 1.0F);
        }
        DeviceArt.drawBackdrop(graphics, frame.w(), frame.h(), (float) frame.backdrop());
        if (frame.visible()) {
            DeviceArt.Plate plate = DeviceArt.plate(frame.w(), frame.h(), frame.geom().density(),
                    put.livery());
            pose.pushPose();
            pose.translate(0.0F, frame.dy(), 0.0F);
            DeviceArt.drawSleepingDevice(graphics, font, plate, put.livery(), put.linkColor(), true,
                    put.keys());
            pose.popPose();
            TabletMapFrame.fill(graphics, frame.s(), DeviceArt.GLASS, frame.closeGlassAlpha());
        }
        pose.popPose();
    }
}
