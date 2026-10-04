package com.wok.infantry.uitest;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.client.screen.TacticalScreen;
import com.wok.infantry.client.screen.UiScale;
import com.wok.infantry.client.ui.probe.UiLayoutFrame;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Captures one rendered frame: the layout probe records exactly the frame whose framebuffer is
 * then saved, so the screenshot and its layout report always match.
 *
 * <p>A request is armed for a screen (or for the HUD with no screen). On the next
 * {@code ScreenEvent.Render.Pre} (or {@code RenderGuiEvent.Pre}) the probe starts recording, on the
 * matching {@code Post} it stops, and at {@code RenderTick END} — after GameRenderer flushed every
 * UI batch — the framebuffer is grabbed (unless the request is probe-only) and the callback gets
 * the recorded frame. If the screen changed in between, the request stays armed for the next frame.
 */
@Mod.EventBusSubscriber(modid = WokInfantryMod.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class UiCapture {
    /** A recorded frame and what was captured with it. */
    public record Result(String fileName, Screen screen, UiLayoutFrame frame, int guiWidth,
                         int guiHeight, double guiScale, int baseScale) {
        /** Layout (logical) size of the captured surface after the minimum 2x. */
        public int layoutWidth() {
            return guiWidth / Math.max(1, baseScale);
        }

        public int layoutHeight() {
            return guiHeight / Math.max(1, baseScale);
        }
    }

    private record Request(String fileName, Screen screen, boolean hud, Consumer<Result> callback) {
    }

    private static final Map<String, String> SAVE_MESSAGES = new ConcurrentHashMap<>();
    private static Request armed;
    private static Request recordingFor;
    private static Result rendered;

    private UiCapture() {
    }

    /** Captures {@code screen} (screenshot {@code fileName}, or probe only when null). */
    public static void arm(String fileName, Screen screen, Consumer<Result> callback) {
        armed = new Request(fileName, screen, screen == null, callback);
        recordingFor = null;
        rendered = null;
    }

    /** Records the next frame of {@code screen} without a screenshot. */
    public static void probe(Screen screen, Consumer<Result> callback) {
        arm(null, screen, callback);
    }

    public static boolean busy() {
        return armed != null;
    }

    public static void cancel() {
        armed = null;
        recordingFor = null;
        rendered = null;
        UiLayoutProbe.endFrame();
    }

    /** Message Minecraft reported when it saved {@code fileName} ("" until then). */
    public static String saveMessage(String fileName) {
        return SAVE_MESSAGES.getOrDefault(fileName, "");
    }

    // ---- events ---------------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        Request request = armed;
        if (request != null && !request.hud() && event.getScreen() == request.screen()
                && recordingFor == null && rendered == null) {
            start(request, event.getScreen());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Request request = recordingFor;
        if (request != null && !request.hud() && event.getScreen() == request.screen()) {
            finish(request);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGuiRenderPre(RenderGuiEvent.Pre event) {
        Request request = armed;
        Minecraft minecraft = Minecraft.getInstance();
        if (request != null && request.hud() && minecraft.screen == null && recordingFor == null
                && rendered == null) {
            start(request, null);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiRenderPost(RenderGuiEvent.Post event) {
        Request request = recordingFor;
        if (request != null && request.hud()) {
            finish(request);
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            // Window events between frames may have moved the mouse a case holds.
            UiInputDriver.applyHeld(Minecraft.getInstance());
            return;
        }
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Result result = rendered;
        Request request = armed;
        if (result == null || request == null) {
            if (recordingFor != null) {
                // The frame never reached its Post event (screen replaced mid-frame): retry.
                UiLayoutProbe.endFrame();
                recordingFor = null;
            }
            return;
        }
        rendered = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != request.screen()) {
            // The screen changed after the probed frame; record the next frame instead.
            return;
        }
        armed = null;
        if (request.fileName() != null) {
            WokInfantryMod.LOGGER.info("[UI ACCEPTANCE] Capturing {} at GUI {}x{} (layout x{})",
                    request.fileName(), result.guiWidth(), result.guiHeight(), result.baseScale());
            Screenshot.grab(minecraft.gameDirectory, request.fileName(),
                    minecraft.getMainRenderTarget(), component ->
                            SAVE_MESSAGES.put(request.fileName(), component.getString()));
        }
        request.callback().accept(result);
    }

    private static void start(Request request, Screen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        int baseScale = screen == null ? UiScale.hudFactor()
                : screen instanceof TacticalScreen tactical ? tactical.uiScale() : 1;
        UiLayoutProbe.beginFrame(new UiLayoutFrame(window.getGuiScaledWidth(),
                window.getGuiScaledHeight(), window.getGuiScale(), baseScale));
        recordingFor = request;
    }

    private static void finish(Request request) {
        UiLayoutFrame frame = UiLayoutProbe.endFrame();
        recordingFor = null;
        if (frame == null) {
            return;
        }
        rendered = new Result(request.fileName(), request.screen(), frame, frame.guiWidth(),
                frame.guiHeight(), frame.guiScale(), frame.baseScale());
    }
}
