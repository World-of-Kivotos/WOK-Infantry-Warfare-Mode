package com.wok.bodyhealth.hudtest;

import com.wok.bodyhealth.WokBodyHealthMod;
import com.wok.bodyhealth.api.BodyHealthHudApi;
import com.wok.bodyhealth.client.BodyHealthHudLayout;
import com.wok.bodyhealth.client.BodyHealthHudLayout.Layout;
import com.wok.bodyhealth.client.BodyHealthHudLayout.Tier;
import com.wok.bodyhealth.client.BodyHealthOverlay;
import com.wok.bodyhealth.client.ClientBodyHealthState;
import com.wok.bodyhealth.health.BodyHealthData;
import com.wok.bodyhealth.health.BodyHealthService;
import com.wok.bodyhealth.health.BodyPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Real-client visual acceptance for the body-health HUD. It opens an isolated
 * superflat world, applies a fixed injury state, captures the rendered
 * framebuffer at the required logical sizes, checks the live layout against
 * the hotbar, writes a result file and exits. Lives in src/hudTest only.
 */
@Mod.EventBusSubscriber(modid = WokBodyHealthMod.MOD_ID, value = Dist.CLIENT)
public final class HudAcceptanceHarness {
    private static final boolean ENABLED = Boolean.getBoolean("wok.bodyhealth.hudTest");
    private static final boolean EXPECT_CORE = Boolean.getBoolean("wok.bodyhealth.hudTestCore");
    private static final String WORLD = "wok_hud_test";
    private static final int PHASE_TIMEOUT_TICKS = 20 * 90;
    private static final int GLOBAL_TIMEOUT_TICKS = 20 * 60 * 8;
    private static final Map<BodyPart, Float> INJURIES = createInjuries();
    private static final List<Capture> CAPTURES = List.of(
            new Capture("hud_compact_320x240.png", 960, 720, 3, 320, 240, Tier.COMPACT),
            // Minecraft rounds scaled sizes up: 1280/3 -> 427, the same as 2560x1440 at 6x.
            new Capture("hud_short_427x240.png", 1280, 720, 3, 427, 240, Tier.SHORT),
            // 1440x810 at 3x equals 1920x1080 at the automatic 4x: 480x270.
            new Capture("hud_full_480x270.png", 1440, 810, 3, 480, 270, Tier.FULL),
            new Capture("hud_full_960x720.png", 960, 720, 1, 960, 720, Tier.FULL));

    private enum Phase {
        BOOT, WAIT_FOR_WORLD, SETUP, RESIZE, SETTLE, CAPTURE, WAIT_FOR_FILE,
        CREATIVE_ON, CREATIVE_CHECK, FINISH, STOPPED
    }

    private static Phase phase = Phase.BOOT;
    private static int phaseTicks;
    private static int totalTicks;
    private static int captureIndex;
    private static boolean worldRequested;
    private static volatile boolean setupDone;
    private static volatile String setupError;
    private static volatile boolean captureRequested;
    private static long captureStartedAt;
    private static String failure;
    private static boolean resultWritten;
    private static final List<String> observations = new ArrayList<>();
    private static final Set<String> closedScreens = new LinkedHashSet<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || phase == Phase.STOPPED) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        try {
            tick(minecraft);
        } catch (Throwable throwable) {
            WokBodyHealthMod.LOGGER.error("[HUD ACCEPTANCE] Harness failed", throwable);
            fail("Unhandled harness exception: " + throwable);
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || !captureRequested) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null) {
            return;
        }
        // RenderTick END runs after the GUI batch has been flushed to the main target.
        captureRequested = false;
        Capture capture = CAPTURES.get(captureIndex);
        Screenshot.grab(minecraft.gameDirectory, capture.fileName(), minecraft.getMainRenderTarget(),
                message -> WokBodyHealthMod.LOGGER.info("[HUD ACCEPTANCE] {}", message.getString()));
    }

    private static void tick(Minecraft minecraft) throws IOException {
        totalTicks++;
        phaseTicks++;
        if (totalTicks > GLOBAL_TIMEOUT_TICKS && phase != Phase.FINISH) {
            fail("Global timeout in " + phase);
        }
        if (phaseTicks > PHASE_TIMEOUT_TICKS && phase != Phase.FINISH) {
            fail("Phase timeout in " + phase);
        }
        if (phase.ordinal() >= Phase.RESIZE.ordinal() && phase.ordinal() <= Phase.CREATIVE_CHECK.ordinal()) {
            keepViewClear(minecraft);
        }

        switch (phase) {
            case BOOT -> boot(minecraft);
            case WAIT_FOR_WORLD -> waitForWorld(minecraft);
            case SETUP -> waitForSetup(minecraft);
            case RESIZE -> resize(minecraft);
            case SETTLE -> settle(minecraft);
            case CAPTURE -> {
                if (phaseTicks > 40) {
                    fail("Screenshot was never taken for " + CAPTURES.get(captureIndex).fileName());
                }
            }
            case WAIT_FOR_FILE -> waitForFile(minecraft);
            case CREATIVE_ON -> creativeOn(minecraft);
            case CREATIVE_CHECK -> creativeCheck(minecraft);
            case FINISH -> finish(minecraft);
            case STOPPED -> {
            }
        }
    }

    private static void boot(Minecraft minecraft) {
        minecraft.options.pauseOnLostFocus = false;
        minecraft.options.onboardAccessibility = false;
        minecraft.options.hideGui = false;
        if (minecraft.getOverlay() != null || phaseTicks < 40) {
            return;
        }
        if (!worldRequested) {
            worldRequested = true;
            boolean exists = Files.isDirectory(minecraft.gameDirectory.toPath().resolve("saves").resolve(WORLD));
            observations.add("fixtureWorld=" + (exists ? "loaded" : "created") + " " + WORLD);
            if (exists) {
                minecraft.createWorldOpenFlows().loadLevel(new TitleScreen(), WORLD);
            } else {
                GameRules rules = new GameRules();
                rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
                rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                LevelSettings settings = new LevelSettings(WORLD, GameType.SURVIVAL, false,
                        Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
                minecraft.createWorldOpenFlows().createFreshLevel(WORLD, settings,
                        new WorldOptions(20260930L, false, false),
                        registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            }
            transition(Phase.WAIT_FOR_WORLD);
        }
    }

    private static void waitForWorld(Minecraft minecraft) {
        if (minecraft.screen instanceof ConfirmScreen confirm) {
            confirm.children().stream()
                    .filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(button -> button.active && button.visible)
                    .min(java.util.Comparator.comparingInt(Button::getX))
                    .ifPresent(Button::onPress);
            return;
        }
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (minecraft.player == null || minecraft.level == null || server == null
                || ClientBodyHealthState.get() == null || phaseTicks < 60) {
            return;
        }
        boolean coreLoaded = ModList.get().isLoaded("wok_infantry");
        observations.add("coreLoaded=" + coreLoaded + " expected=" + EXPECT_CORE);
        if (coreLoaded != EXPECT_CORE) {
            fail("WOK步战核心 loaded state mismatch: expected " + EXPECT_CORE + ", got " + coreLoaded);
            return;
        }
        UUID playerId = minecraft.player.getUUID();
        server.execute(() -> applyFixture(server, playerId));
        transition(Phase.SETUP);
    }

    private static void applyFixture(MinecraftServer server, UUID playerId) {
        try {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                setupError = "Integrated player is missing";
                return;
            }
            var source = server.createCommandSourceStack().withSuppressedOutput();
            for (String command : List.of("time set noon", "weather clear",
                    "gamerule doDaylightCycle false", "gamerule doWeatherCycle false",
                    "gamerule doMobSpawning false")) {
                server.getCommands().performPrefixedCommand(source, command);
            }
            player.setGameMode(GameType.SURVIVAL);
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            player.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
            player.getInventory().setItem(1, new ItemStack(Items.BOW));
            player.getInventory().setItem(2, new ItemStack(Items.BREAD, 12));
            player.getInventory().setItem(3, new ItemStack(Items.TORCH, 32));
            player.getInventory().selected = 0;
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), 0.0F, 8.0F);
            BodyHealthData data = BodyHealthData.load(player);
            INJURIES.forEach(data::set);
            data.save(player);
            BodyHealthService.sync(player);
            setupDone = true;
        } catch (RuntimeException exception) {
            setupError = "Fixture setup failed: " + exception;
        }
    }

    private static void waitForSetup(Minecraft minecraft) {
        if (setupError != null) {
            fail(setupError);
            return;
        }
        if (!setupDone || phaseTicks < 40) {
            return;
        }
        for (BodyPart part : BodyPart.values()) {
            float expected = INJURIES.get(part);
            float actual = ClientBodyHealthState.get().current()[part.ordinal()];
            if (Math.abs(expected - actual) > 0.01F) {
                fail("Client snapshot for " + part + " is " + actual + ", expected " + expected);
                return;
            }
        }
        observations.add("injuries=" + INJURIES);
        observations.add("fontWidth(85/85)=" + minecraft.font.width("85/85")
                + " fontWidth(85)=" + minecraft.font.width("85"));
        transition(Phase.RESIZE);
    }

    private static void resize(Minecraft minecraft) {
        applyWindowSize(minecraft, CAPTURES.get(captureIndex));
        transition(Phase.SETTLE);
    }

    /** An unfocused fixture window can be minimised by the desktop; restore it before sizing. */
    private static void applyWindowSize(Minecraft minecraft, Capture capture) {
        long handle = minecraft.getWindow().getWindow();
        if (GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE) {
            GLFW.glfwRestoreWindow(handle);
            observations.add("restoredMinimizedWindow before " + capture.fileName());
        }
        minecraft.getWindow().setWindowed(capture.windowWidth(), capture.windowHeight());
        minecraft.resizeDisplay();
        minecraft.options.guiScale().set(capture.guiScale());
        minecraft.resizeDisplay();
    }

    private static void settle(Minecraft minecraft) {
        if (phaseTicks < 20) {
            return;
        }
        Capture capture = CAPTURES.get(captureIndex);
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        if (width != capture.logicalWidth() || height != capture.logicalHeight()) {
            if (phaseTicks < 200) {
                if (phaseTicks % 20 == 0) {
                    applyWindowSize(minecraft, capture);
                }
                return;
            }
            fail(capture.fileName() + ": logical size " + width + "x" + height + ", expected "
                    + capture.logicalWidth() + "x" + capture.logicalHeight()
                    + " (window " + minecraft.getWindow().getWidth() + "x" + minecraft.getWindow().getHeight() + ")");
            return;
        }
        String problem = verifyLayout(minecraft, capture, width, height);
        if (problem != null) {
            fail(capture.fileName() + ": " + problem);
            return;
        }
        minecraft.gui.getChat().clearMessages(false);
        minecraft.getToasts().clear();
        captureStartedAt = System.currentTimeMillis();
        captureRequested = true;
        transition(Phase.CAPTURE);
        phase = Phase.WAIT_FOR_FILE;
    }

    /** Checks the live layout (real font widths) against the hotbar and the companion strip. */
    private static String verifyLayout(Minecraft minecraft, Capture capture, int width, int height) {
        Layout layout = BodyHealthOverlay.currentLayout(minecraft, width, height);
        if (layout == null) {
            return "HUD is hidden";
        }
        if (layout.tier() != capture.expectedTier()) {
            return "tier " + layout.tier() + ", expected " + capture.expectedTier();
        }
        int fullWidth = minecraft.font.width("85/85");
        int shortWidth = minecraft.font.width("85");
        int labelExtent = switch (layout.tier()) {
            case FULL -> Tier.FULL.labelGap() + fullWidth + BodyHealthHudLayout.CHIP_PADDING;
            case SHORT -> Tier.SHORT.labelGap() + shortWidth + BodyHealthHudLayout.CHIP_PADDING;
            case COMPACT -> 0;
        };
        int left = Math.min(layout.figureX() - labelExtent, layout.companionLeft());
        int right = Math.max(layout.figureX() + BodyHealthHudLayout.FIGURE_WIDTH + labelExtent,
                layout.companionLeft() + BodyHealthHudLayout.COMPANION_WIDTH);
        int hotbarLeft = BodyHealthHudLayout.hotbarLeft(width);
        if (left < 2 || right > hotbarLeft - 4) {
            return "HUD spans x " + left + ".." + right + " but the hotbar starts at " + hotbarLeft;
        }
        int[] slot = BodyHealthHudApi.companionSlot(width, height);
        if (slot == null || slot[0] != layout.companionLeft() || slot[1] != height - 13) {
            return "companion slot " + java.util.Arrays.toString(slot) + " does not match the figure";
        }
        observations.add(capture.fileName() + " logical=" + width + "x" + height
                + " tier=" + layout.tier() + " figureX=" + layout.figureX()
                + " hudX=" + left + ".." + right + " hotbarLeft=" + hotbarLeft
                + " staminaSlot=" + slot[0] + "," + slot[1] + "," + slot[2] + "x" + slot[3]
                + staminaState());
        return null;
    }

    private static String staminaState() {
        if (!EXPECT_CORE) {
            return "";
        }
        try {
            Object snapshot = Class.forName("com.wok.infantry.client.ClientStaminaState")
                    .getMethod("snapshot").invoke(null);
            Object enabled = snapshot.getClass().getMethod("enabled").invoke(snapshot);
            return " coreStaminaEnabled=" + enabled;
        } catch (ReflectiveOperationException exception) {
            return " coreStamina=unreadable(" + exception + ")";
        }
    }

    private static void waitForFile(Minecraft minecraft) throws IOException {
        Capture capture = CAPTURES.get(captureIndex);
        Path file = minecraft.gameDirectory.toPath().resolve("screenshots").resolve(capture.fileName());
        if (captureRequested) {
            if (phaseTicks > 60) {
                fail("Screenshot was never taken for " + capture.fileName());
            }
            return;
        }
        if (!Files.isRegularFile(file) || Files.size(file) == 0
                || Files.getLastModifiedTime(file).toMillis() < captureStartedAt - 1000L) {
            return;
        }
        observations.add("captured=" + file.toAbsolutePath());
        captureIndex++;
        transition(captureIndex < CAPTURES.size() ? Phase.RESIZE : Phase.CREATIVE_ON);
    }

    private static void creativeOn(Minecraft minecraft) {
        MinecraftServer server = minecraft.getSingleplayerServer();
        UUID playerId = minecraft.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                player.setGameMode(GameType.CREATIVE);
            }
        });
        transition(Phase.CREATIVE_CHECK);
    }

    private static void creativeCheck(Minecraft minecraft) {
        if (phaseTicks < 30) {
            return;
        }
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        if (BodyHealthOverlay.currentLayout(minecraft, width, height) != null
                || BodyHealthHudApi.companionSlot(width, height) != null) {
            fail("HUD is still shown in creative mode");
            return;
        }
        observations.add("creativeHidden=true");
        transition(Phase.FINISH);
    }

    private static void keepViewClear(Minecraft minecraft) {
        minecraft.getTutorial().setStep(TutorialSteps.NONE);
        if (minecraft.screen != null) {
            closedScreens.add(minecraft.screen.getClass().getName());
            minecraft.setScreen(null);
        }
    }

    private static void finish(Minecraft minecraft) throws IOException {
        if (!resultWritten) {
            resultWritten = true;
            Path directory = minecraft.gameDirectory.toPath().resolve("hud-test-results");
            Files.createDirectories(directory);
            List<String> lines = new ArrayList<>();
            lines.add("status=" + (failure == null ? "PASS" : "FAIL"));
            if (failure != null) {
                lines.add("failure=" + failure);
            }
            lines.add("totalTicks=" + totalTicks);
            lines.add("closedScreens=" + closedScreens);
            lines.addAll(observations);
            Files.write(directory.resolve("result.txt"), lines, StandardCharsets.UTF_8);
            WokBodyHealthMod.LOGGER.info("[HUD ACCEPTANCE] {}", failure == null ? "PASS" : "FAIL: " + failure);
            phaseTicks = 0;
            return;
        }
        if (phaseTicks >= 20) {
            phase = Phase.STOPPED;
            minecraft.stop();
        }
    }

    private static void fail(String reason) {
        if (failure == null) {
            failure = reason;
            WokBodyHealthMod.LOGGER.error("[HUD ACCEPTANCE] FAIL in {}: {}", phase, reason);
        }
        captureRequested = false;
        transition(Phase.FINISH);
    }

    private static void transition(Phase next) {
        phase = next;
        phaseTicks = 0;
    }

    private static Map<BodyPart, Float> createInjuries() {
        EnumMap<BodyPart, Float> injuries = new EnumMap<>(BodyPart.class);
        injuries.put(BodyPart.HEAD, 35.0F);
        injuries.put(BodyPart.CHEST, 51.0F);
        injuries.put(BodyPart.ABDOMEN, 12.0F);
        injuries.put(BodyPart.LEFT_ARM, 0.0F);
        injuries.put(BodyPart.RIGHT_ARM, 60.0F);
        injuries.put(BodyPart.LEFT_LEG, 40.0F);
        injuries.put(BodyPart.RIGHT_LEG, 20.0F);
        return injuries;
    }

    private record Capture(String fileName, int windowWidth, int windowHeight, int guiScale,
                           int logicalWidth, int logicalHeight, Tier expectedTier) {
    }

    private HudAcceptanceHarness() {
    }
}
