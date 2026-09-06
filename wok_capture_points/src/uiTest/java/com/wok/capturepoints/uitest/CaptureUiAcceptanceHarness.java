package com.wok.capturepoints.uitest;

import com.wok.capturepoints.WokCapturePointsMod;
import com.wok.capturepoints.capture.CapturePointView;
import com.wok.capturepoints.capture.CaptureTeam;
import com.wok.capturepoints.client.ClientCaptureState;
import com.wok.capturepoints.network.CaptureSnapshotPacket;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.map.TacticalMapAreaOverlayRegistry;
import com.wok.infantry.client.screen.TacticalMapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Development-only real-client HUD and tactical-map visual acceptance. */
@Mod.EventBusSubscriber(modid = WokCapturePointsMod.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CaptureUiAcceptanceHarness {
    private static final String[] SCREENSHOTS = {
            "wok_capture_01_hud_320x240.png",
            "wok_capture_02_map_320x240.png",
            "wok_capture_03_hud_960x720.png",
            "wok_capture_04_map_960x720.png"
    };
    private static final int GLOBAL_TIMEOUT_TICKS = 1_600;
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
            case CAPTURE_LARGE_MAP -> capture(minecraft, SCREENSHOTS[3], Phase.FINISH);
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
        transition(Phase.PREPARE_COMPACT_HUD);
    }

    private static void prepareHud(Minecraft minecraft, int guiScale, int expectedWidth,
                                   int expectedHeight, Phase next) {
        setSize(minecraft, guiScale);
        minecraft.setScreen(null);
        if (phaseTicks < 12) return;
        if (!logicalSize(minecraft).equals(expectedWidth + "x" + expectedHeight)) {
            fail("HUD logical size mismatch: expected " + expectedWidth + 'x' + expectedHeight
                    + ", got " + logicalSize(minecraft));
            return;
        }
        OBSERVATIONS.add("hudLogicalSize=" + logicalSize(minecraft));
        transition(next);
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
        FINISH,
        FAIL,
        STOPPED
    }
}
