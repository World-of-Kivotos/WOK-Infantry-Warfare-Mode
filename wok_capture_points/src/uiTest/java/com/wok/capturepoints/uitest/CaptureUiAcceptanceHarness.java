package com.wok.capturepoints.uitest;

import com.wok.capturepoints.WokCapturePointsMod;
import com.wok.capturepoints.api.CaptureHudApi;
import com.wok.capturepoints.capture.CapturePointView;
import com.wok.capturepoints.capture.CaptureTeam;
import com.wok.capturepoints.client.CaptureStripLayout;
import com.wok.capturepoints.client.ClientCaptureState;
import com.wok.capturepoints.network.CaptureSnapshotPacket;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.map.TacticalMapAreaOverlayRegistry;
import com.wok.infantry.client.screen.TacticalMapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Development-only real-client HUD and tactical-map visual acceptance.
 *
 * <p>0.1.0-alpha.4: the HUD captures also check who draws the point. With a core that shows it
 * ({@code InfantryHudApi.rendersCapturePoints()}, core 0.5.0-beta.2+) the first two HUD captures
 * must show the core's objective tile and this add-on must report no panel; the last two turn the
 * core's {@code hud.showBattleStrip} off (in memory, restored at the end) and must show this
 * add-on's thin strip in the core's {@code top_center_next} slot. With an older core every HUD
 * capture shows the thin strip. The core is only read by reflection here, so the same harness
 * runs against either core JAR ({@code -Pinfantry_dev_jar_path}).
 *
 * <p>Review fix: a vanilla boss bar (client overlay only) is shown during every HUD capture, and
 * the thin strip must hold the same place four ticks apart, inside the top half of the screen.
 * Before the fix the strip and core 0.5.0-beta.1's boss bar shift chased each other down and the
 * strip left the screen within a few frames.
 */
@Mod.EventBusSubscriber(modid = WokCapturePointsMod.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CaptureUiAcceptanceHarness {
    private static final String[] SCREENSHOTS = {
            "wok_capture_01_hud_320x240.png",
            "wok_capture_02_map_320x240.png",
            "wok_capture_03_hud_960x720.png",
            "wok_capture_04_map_960x720.png",
            "wok_capture_05_strip_off_320x240.png",
            "wok_capture_06_strip_off_960x720.png"
    };
    private static final String CORE_HUD_API = "com.wok.infantry.client.hud.InfantryHudApi";
    private static final String CORE_HUD_FRAME = "com.wok.infantry.client.hud.HudFrame";
    private static final String CORE_CLIENT_CONFIG = "com.wok.infantry.config.InfantryClientConfig";
    private static final int GLOBAL_TIMEOUT_TICKS = 2_400;
    private static final int PHASE_TIMEOUT_TICKS = 300;
    private static final Map<String, Long> BASELINES = new LinkedHashMap<>();
    private static final Map<String, String> CALLBACKS = new ConcurrentHashMap<>();
    private static final List<String> OBSERVATIONS = new ArrayList<>();

    private static Phase phase = Phase.WAIT_LOGIN;
    private static int totalTicks;
    private static int phaseTicks;
    private static boolean initialized;
    private static String pendingScreenshot;
    private static boolean captureRequested;
    private static String failure;
    private static boolean resultWritten;
    /** The core's original {@code hud.showBattleStrip}, while the harness has it switched off. */
    private static Boolean savedShowBattleStrip;
    /** The add-on's panelRect four ticks before a HUD check (the strip must have settled). */
    private static int[] settlingPanel;

    private CaptureUiAcceptanceHarness() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || phase == Phase.STOPPED) return;
        Minecraft minecraft = Minecraft.getInstance();
        try {
            tick(minecraft);
        } catch (Throwable throwable) {
            WokCapturePointsMod.LOGGER.error("[CAPTURE UI ACCEPTANCE] Unhandled failure", throwable);
            fail("Unhandled exception: " + throwable);
        }
    }

    @SubscribeEvent
    public static void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingScreenshot == null
                || captureRequested) return;
        Minecraft minecraft = Minecraft.getInstance();
        String fileName = pendingScreenshot;
        captureRequested = true;
        Screenshot.grab(minecraft.gameDirectory, fileName, minecraft.getMainRenderTarget(),
                component -> CALLBACKS.put(fileName, component.getString()));
    }

    @SubscribeEvent
    public static void screenRenderPre(ScreenEvent.Render.Pre event) {
        if (event.getScreen() instanceof TacticalMapScreen) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.level != null) installFixture(minecraft);
        }
    }

    private static void tick(Minecraft minecraft) throws IOException {
        totalTicks++;
        phaseTicks++;
        if (!initialized) initialize(minecraft);
        if (minecraft.player != null && minecraft.level != null) installFixture(minecraft);
        if (totalTicks > GLOBAL_TIMEOUT_TICKS && phase != Phase.FINISH && phase != Phase.FAIL) {
            fail("Global timeout in " + phase);
        }
        if (phaseTicks > PHASE_TIMEOUT_TICKS && phase != Phase.WAIT_LOGIN
                && phase != Phase.FINISH && phase != Phase.FAIL) {
            fail("Phase timeout in " + phase);
        }

        switch (phase) {
            case WAIT_LOGIN -> waitLogin(minecraft);
            case PREPARE_COMPACT_HUD -> prepareHud(minecraft, 3, 320, 240,
                    Phase.CAPTURE_COMPACT_HUD);
            case CAPTURE_COMPACT_HUD -> capture(minecraft, SCREENSHOTS[0],
                    Phase.PREPARE_COMPACT_MAP);
            case PREPARE_COMPACT_MAP -> prepareMap(minecraft, 3, 320, 240,
                    Phase.CAPTURE_COMPACT_MAP);
            case CAPTURE_COMPACT_MAP -> capture(minecraft, SCREENSHOTS[1],
                    Phase.PREPARE_LARGE_HUD);
            case PREPARE_LARGE_HUD -> prepareHud(minecraft, 1, 960, 720,
                    Phase.CAPTURE_LARGE_HUD);
            case CAPTURE_LARGE_HUD -> capture(minecraft, SCREENSHOTS[2],
                    Phase.PREPARE_LARGE_MAP);
            case PREPARE_LARGE_MAP -> prepareMap(minecraft, 1, 960, 720,
                    Phase.CAPTURE_LARGE_MAP);
            case CAPTURE_LARGE_MAP -> capture(minecraft, SCREENSHOTS[3],
                    Phase.PREPARE_COMPACT_STRIP);
            case PREPARE_COMPACT_STRIP -> {
                showBattleStrip(false);
                if (savedShowBattleStrip == null) {
                    fail("could not switch the core's hud.showBattleStrip off");
                } else {
                    prepareHud(minecraft, 3, 320, 240, Phase.CAPTURE_COMPACT_STRIP);
                }
            }
            case CAPTURE_COMPACT_STRIP -> capture(minecraft, SCREENSHOTS[4],
                    Phase.PREPARE_LARGE_STRIP);
            case PREPARE_LARGE_STRIP -> prepareHud(minecraft, 1, 960, 720,
                    Phase.CAPTURE_LARGE_STRIP);
            case CAPTURE_LARGE_STRIP -> capture(minecraft, SCREENSHOTS[5], Phase.FINISH);
            case FINISH -> finish(minecraft, true);
            case FAIL -> finish(minecraft, false);
            case STOPPED -> {
            }
        }
    }

    private static void initialize(Minecraft minecraft) throws IOException {
        initialized = true;
        minecraft.options.pauseOnLostFocus = false;
        Path screenshotDir = minecraft.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(screenshotDir);
        for (String screenshot : SCREENSHOTS) {
            Path path = screenshotDir.resolve(screenshot);
            BASELINES.put(screenshot, Files.isRegularFile(path)
                    ? Files.getLastModifiedTime(path).toMillis() : -1L);
        }
        OBSERVATIONS.add("harness=src/uiTest (excluded from production JAR)");
    }

    private static void waitLogin(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null
                || minecraft.getSingleplayerServer() == null
                || ClientBattleState.snapshot() == null) {
            return;
        }
        minecraft.getTutorial().setStep(TutorialSteps.NONE);
        minecraft.getToasts().clear();
        if (TacticalMapAreaOverlayRegistry.overlays().stream()
                .noneMatch(overlay -> overlay.id().equals("wok_capture_points:a"))) {
            return;
        }
        OBSERVATIONS.add("integratedWorld=true");
        OBSERVATIONS.add("mapOverlayRegistered=true");
        minecraft.gui.getBossOverlay().reset();
        minecraft.gui.getBossOverlay().update(ClientboundBossEventPacket.createAddPacket(
                new ServerBossEvent(Component.literal("Boss"), BossEvent.BossBarColor.RED,
                        BossEvent.BossBarOverlay.PROGRESS)));
        OBSERVATIONS.add("bossBar=1 (client overlay only)");
        transition(Phase.PREPARE_COMPACT_HUD);
    }

    private static void prepareHud(Minecraft minecraft, int guiScale, int expectedWidth,
                                   int expectedHeight, Phase next) {
        setSize(minecraft, guiScale);
        minecraft.setScreen(null);
        if (phaseTicks < 12) return;
        if (phaseTicks == 12) {
            settlingPanel = CaptureHudApi.panelRect(minecraft.getWindow().getGuiScaledWidth(),
                    minecraft.getWindow().getGuiScaledHeight());
            return;
        }
        if (phaseTicks < 16) return;
        if (!logicalSize(minecraft).equals(expectedWidth + "x" + expectedHeight)) {
            fail("HUD logical size mismatch: expected " + expectedWidth + 'x' + expectedHeight
                    + ", got " + logicalSize(minecraft));
            return;
        }
        OBSERVATIONS.add("hudLogicalSize=" + logicalSize(minecraft));
        if (!checkWhoDrawsThePoint(minecraft)) {
            return;
        }
        transition(next);
    }

    /**
     * Who shows the point this frame: the core's objective tile (this add-on reports no panel) or
     * this add-on's thin strip (at most 200 × 23 layout pixels, reported through panelRect).
     */
    private static boolean checkWhoDrawsThePoint(Minecraft minecraft) {
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Boolean coreRenders = coreRendersCapturePoints();
        int[] panel = CaptureHudApi.panelRect(width, height);
        Object objective = coreObjective(width, height);
        int factor = CaptureStripLayout.factor(minecraft.getWindow().getGuiScale(), width, height);
        String tag = "@" + width + "x" + height + (savedShowBattleStrip == null ? ""
                : " showBattleStrip=false");
        OBSERVATIONS.add("coreRendersCapturePoints" + tag + "="
                + (coreRenders == null ? "missing" : coreRenders));
        OBSERVATIONS.add("addonPanelRect" + tag + "="
                + (panel == null ? "null" : Arrays.toString(panel)));
        OBSERVATIONS.add("coreObjective" + tag + "=" + (objective == null ? "null"
                : objective.toString().replaceAll("\\s+", " ")));
        if (Boolean.TRUE.equals(coreRenders)) {
            if (panel != null || objective == null) {
                fail("the core shows the point: expected no add-on panel and a core objective, "
                        + "got panel=" + Arrays.toString(panel) + " objective=" + objective);
                return false;
            }
            return true;
        }
        if (panel == null) {
            fail("the core does not show the point (rendersCapturePoints=" + coreRenders
                    + "): the add-on's thin strip is missing");
            return false;
        }
        if (panel[2] > CaptureStripLayout.MAX_WIDTH * factor
                || panel[3] > CaptureStripLayout.HEIGHT_WIDE * factor
                || panel[0] < 0 || panel[0] + panel[2] > width
                || panel[1] < 0 || panel[1] + panel[3] > height / 2) {
            fail("thin strip out of bounds: " + Arrays.toString(panel) + " on " + width + "x"
                    + height + " at " + factor + "x");
            return false;
        }
        if (!Arrays.equals(settlingPanel, panel)) {
            fail("thin strip still moving under the boss bar: " + Arrays.toString(settlingPanel)
                    + " four ticks ago, now " + Arrays.toString(panel));
            return false;
        }
        if (objective != null) {
            fail("both the core objective tile and the thin strip are shown: " + objective);
            return false;
        }
        return true;
    }

    /** {@code InfantryHudApi.rendersCapturePoints()}, or null on a core without it. */
    private static Boolean coreRendersCapturePoints() {
        try {
            return (Boolean) Class.forName(CORE_HUD_API).getMethod("rendersCapturePoints")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    /** {@code HudFrame.current(w, h).objective()}, or null (none, or a core without it). */
    private static Object coreObjective(int width, int height) {
        try {
            Object frame = Class.forName(CORE_HUD_FRAME)
                    .getMethod("current", int.class, int.class).invoke(null, width, height);
            return frame == null ? null : frame.getClass().getMethod("objective").invoke(frame);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    /**
     * Switches the core's {@code hud.showBattleStrip} (in memory, restored when the run ends);
     * the first call remembers the original value. Without the core nothing changes.
     */
    private static void showBattleStrip(boolean shown) {
        try {
            Field field = Class.forName(CORE_CLIENT_CONFIG).getDeclaredField("SHOW_BATTLE_STRIP");
            field.setAccessible(true);
            ForgeConfigSpec.BooleanValue value = (ForgeConfigSpec.BooleanValue) field.get(null);
            if (savedShowBattleStrip == null) {
                savedShowBattleStrip = value.get();
            }
            if (value.get() != shown) {
                value.set(shown);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            OBSERVATIONS.add("showBattleStrip=unavailable:" + exception);
        }
    }

    private static void restoreBattleStrip() {
        if (savedShowBattleStrip != null) {
            boolean original = savedShowBattleStrip;
            showBattleStrip(original);
            OBSERVATIONS.add("showBattleStripRestored=" + original);
            savedShowBattleStrip = null;
        }
    }

    private static void prepareMap(Minecraft minecraft, int guiScale, int expectedWidth,
                                   int expectedHeight, Phase next) {
        setSize(minecraft, guiScale);
        if (!(minecraft.screen instanceof TacticalMapScreen)) {
            minecraft.setScreen(new TacticalMapScreen(null));
            return;
        }
        if (phaseTicks < 16) return;
        if (!logicalSize(minecraft).equals(expectedWidth + "x" + expectedHeight)) {
            fail("Map logical size mismatch: expected " + expectedWidth + 'x' + expectedHeight
                    + ", got " + logicalSize(minecraft));
            return;
        }
        observeMapGeometry((TacticalMapScreen) minecraft.screen);
        OBSERVATIONS.add("mapLogicalSize=" + logicalSize(minecraft));
        transition(next);
    }

    private static void capture(Minecraft minecraft, String fileName, Phase next) {
        if (pendingScreenshot == null && phaseTicks >= 8) {
            pendingScreenshot = fileName;
            captureRequested = false;
            return;
        }
        Path file = minecraft.gameDirectory.toPath().resolve("screenshots").resolve(fileName);
        if (!captureRequested || !CALLBACKS.containsKey(fileName) || !Files.isRegularFile(file)) {
            return;
        }
        try {
            if (Files.getLastModifiedTime(file).toMillis() <= BASELINES.getOrDefault(fileName, -1L)) {
                return;
            }
            OBSERVATIONS.add(fileName + "=" + Files.size(file) + " bytes");
        } catch (IOException exception) {
            fail("Could not inspect screenshot " + fileName + ": " + exception);
            return;
        }
        transition(next);
    }

    private static void installFixture(Minecraft minecraft) {
        BlockPos center = minecraft.player.blockPosition();
        CapturePointView hudPoint = new CapturePointView("a", "A点 · 火车站",
                minecraft.level.dimension().location(), center.offset(-96, -3, -80),
                center.offset(96, 5, 80), 0, 90, true, 0.35D,
                CaptureTeam.NEUTRAL, CaptureTeam.BLUE, 3, 2, 1, true, false);
        CapturePointView mapPoint = new CapturePointView("b", "B点 · 指挥所",
                Level.OVERWORLD.location(), new BlockPos(36, 60, -4),
                new BlockPos(164, 72, 124), 1, 90, true, -0.4D,
                CaptureTeam.NEUTRAL, CaptureTeam.RED, 1, 3, 2, true, true);
        ClientCaptureState.update(new CaptureSnapshotPacket(List.of(mapPoint, hudPoint),
                "a", true, 90));
    }

    private static void setSize(Minecraft minecraft, int guiScale) {
        if (minecraft.getWindow().getWidth() != 960 || minecraft.getWindow().getHeight() != 720) {
            minecraft.getWindow().setWindowed(960, 720);
            minecraft.resizeDisplay();
        }
        if (minecraft.options.guiScale().get() != guiScale) {
            minecraft.options.guiScale().set(guiScale);
            minecraft.resizeDisplay();
        }
    }

    private static String logicalSize(Minecraft minecraft) {
        return minecraft.getWindow().getGuiScaledWidth() + "x"
                + minecraft.getWindow().getGuiScaledHeight();
    }

    private static void observeMapGeometry(TacticalMapScreen screen) {
        try {
            var overlays = TacticalMapAreaOverlayRegistry.overlays();
            if (overlays.isEmpty()) {
                OBSERVATIONS.add("mapGeometry=NO_OVERLAYS");
                return;
            }
            var area = overlays.get(0);
            double centerWorldX = doubleField(screen, "centerWorldX");
            double centerWorldZ = doubleField(screen, "centerWorldZ");
            double zoom = doubleField(screen, "zoom");
            int mapLeft = intField(screen, "mapLeft");
            int mapRight = intField(screen, "mapRight");
            int mapTop = intField(screen, "mapTop");
            int mapBottom = intField(screen, "mapBottom");
            double areaX = (area.minX() + area.maxX()) * 0.5D;
            double areaZ = (area.minZ() + area.maxZ()) * 0.5D;
            int screenX = (int) Math.round((mapLeft + mapRight) * 0.5D
                    + (areaX - centerWorldX) * zoom);
            int screenY = (int) Math.round((mapTop + mapBottom) * 0.5D
                    + (areaZ - centerWorldZ) * zoom);
            OBSERVATIONS.add("mapGeometry=id=" + area.id() + " dim=" + area.dimension()
                    + " center=" + areaX + ',' + areaZ + " mapCenter=" + centerWorldX + ','
                    + centerWorldZ + " zoom=" + zoom + " screen=" + screenX + ',' + screenY
                    + " viewport=" + mapLeft + ',' + mapTop + '-' + mapRight + ',' + mapBottom);
        } catch (ReflectiveOperationException exception) {
            OBSERVATIONS.add("mapGeometry=ERROR:" + exception);
        }
    }

    private static double doubleField(Object target, String name)
            throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getDouble(target);
    }

    private static int intField(Object target, String name)
            throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private static void transition(Phase next) {
        WokCapturePointsMod.LOGGER.info("[CAPTURE UI ACCEPTANCE] {} -> {}", phase, next);
        phase = next;
        phaseTicks = 0;
        pendingScreenshot = null;
        captureRequested = false;
    }

    private static void fail(String reason) {
        if (phase == Phase.FAIL || phase == Phase.STOPPED) return;
        failure = reason;
        WokCapturePointsMod.LOGGER.error("[CAPTURE UI ACCEPTANCE] FAIL: {}", reason);
        transition(Phase.FAIL);
    }

    private static void finish(Minecraft minecraft, boolean success) throws IOException {
        if (!resultWritten) {
            restoreBattleStrip();
            minecraft.gui.getBossOverlay().reset();
            Path result = minecraft.gameDirectory.toPath().resolve("ui-test-results")
                    .resolve("wok_capture_points_ui_acceptance.txt");
            Files.createDirectories(result.getParent());
            List<String> lines = new ArrayList<>();
            lines.add(success ? "PASS" : "FAIL");
            if (!success) lines.add("failure=" + failure);
            lines.addAll(OBSERVATIONS);
            Files.write(result, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            resultWritten = true;
            phaseTicks = 0;
            return;
        }
        if (phaseTicks >= 20) {
            phase = Phase.STOPPED;
            minecraft.stop();
        }
    }

    private enum Phase {
        WAIT_LOGIN,
        PREPARE_COMPACT_HUD,
        CAPTURE_COMPACT_HUD,
        PREPARE_COMPACT_MAP,
        CAPTURE_COMPACT_MAP,
        PREPARE_LARGE_HUD,
        CAPTURE_LARGE_HUD,
        PREPARE_LARGE_MAP,
        CAPTURE_LARGE_MAP,
        PREPARE_COMPACT_STRIP,
        CAPTURE_COMPACT_STRIP,
        PREPARE_LARGE_STRIP,
        CAPTURE_LARGE_STRIP,
        FINISH,
        FAIL,
        STOPPED
    }
}
