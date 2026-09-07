package com.wok.infantry.uitest;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.TacticalMarkerType;
import com.wok.infantry.client.BattleClientActions;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.client.map.TacticalMapTerrainRequest;
import com.wok.infantry.client.map.TacticalMapTerrainRegistry;
import com.wok.infantry.client.map.TacticalSupportMapPresentation;
import com.wok.infantry.client.map.TacticalSupportMapPresentationRegistry;
import com.wok.infantry.client.screen.AdminLoadoutScreen;
import com.wok.infantry.client.screen.FormationSelectionScreen;
import com.wok.infantry.client.screen.PlayerLoadoutScreen;
import com.wok.infantry.client.screen.SquadScreen;
import com.wok.infantry.client.screen.TacticalMapScreen;
import com.wok.infantry.integration.journeymap.JourneyMapUiPolicy;
import com.wok.infantry.deployment.DeploymentPoint;
import com.wok.infantry.deployment.DeploymentPhase;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.deployment.KitProvenance;
import com.wok.infantry.formation.FormationConfigData;
import com.wok.infantry.formation.FormationClassEditAction;
import com.wok.infantry.formation.FormationClassEditor;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.loadout.LoadoutConfigData;
import com.wok.infantry.loadout.LoadoutClassDefinition;
import com.wok.infantry.loadout.LoadoutInventoryTarget;
import com.wok.infantry.loadout.LoadoutSlotDefinition;
import com.wok.infantry.loadout.LoadoutSnapshot;
import com.wok.infantry.loadout.PlayerLoadoutData;
import com.wok.infantry.loadout.LoadoutSlot;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.battle.BattleOpenTarget;
import com.wok.infantry.network.formation.FormationNetwork;
import com.wok.infantry.registry.InfantryItems;
import com.wok.infantry.server.FormationService;
import com.wok.infantry.support.adapter.SupportIntelContact;
import com.wok.infantry.support.SupportMissionView;
import com.wok.infantry.support.SupportOptionView;
import com.wok.infantry.support.SupportTargetMode;
import com.wok.infantry.support.SupportView;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Development-only, real-client acceptance harness for the tactical UI.
 *
 * <p>The class lives in {@code src/uiTest}; default builds and the production JAR cannot see it.
 * It enters an integrated world, drives the same server-bound intent bridge as K/M, captures the
 * rendered framebuffer, writes a semantic result file and exits the client.</p>
 */
@Mod.EventBusSubscriber(modid = WokInfantryMod.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class UiRuntimeAcceptanceHarness {
    private static final String[] SCREENSHOTS = {
            "wok_ui_01_deployment_320x240.png",
            "wok_ui_02_squads_320x240.png",
            "wok_ui_03_classes_320x240.png",
            "wok_ui_04_map_320x240.png",
            "wok_ui_05_squads_960x720.png",
            "wok_ui_06_map_960x720.png",
            "wok_ui_07_commander_squads_320x240.png",
            "wok_ui_08_loadout_320x240.png",
            "wok_ui_09_loadout_960x720.png",
            "wok_ui_10_formation_vote_320x240.png",
            "wok_ui_11_formation_vote_960x720.png",
            "wok_ui_12_admin_loadout_320x240.png",
            "wok_ui_13_admin_loadout_960x720.png",
            "wok_ui_14_admin_class_settings_320x240.png"
    };
    private static final List<BattleClientActions.MarkerTool> REQUIRED_MARKERS = List.of(
            BattleClientActions.MarkerTool.INFANTRY,
            BattleClientActions.MarkerTool.TANK,
            BattleClientActions.MarkerTool.IFV,
            BattleClientActions.MarkerTool.DEFEND,
            BattleClientActions.MarkerTool.RALLY,
            BattleClientActions.MarkerTool.ATTACK_DIRECTION);
    private static final int GLOBAL_TIMEOUT_TICKS = 2_400;
    private static final int PHASE_TIMEOUT_TICKS = 400;
    private static final int SCREEN_SETTLE_TICKS = 8;
    private static final String EXPECTED_TERRAIN_PROVIDER = System.getProperty(
            "wok.ui.expectedTerrainProvider", "journeymap").trim()
            .toLowerCase(java.util.Locale.ROOT);
    private static final String EXPECTED_JOURNEYMAP_LOADED_RAW = System.getProperty(
            "wok.ui.expectedJourneyMapLoaded", "true").trim().toLowerCase(
            java.util.Locale.ROOT);
    private static final boolean EXPECTED_JOURNEYMAP_LOADED =
            EXPECTED_JOURNEYMAP_LOADED_RAW.equals("true");

    private static final Map<String, Long> screenshotBaselines = new LinkedHashMap<>();
    private static final Map<String, String> screenshotCallbacks = new ConcurrentHashMap<>();
    private static final List<String> observations = new ArrayList<>();

    private static Phase phase = Phase.WAIT_FOR_LOGIN;
    private static int totalTicks;
    private static int phaseTicks;
    private static PendingCapture pendingCapture;
    private static PendingCapture renderedCapture;
    private static String waitingForCapture;
    private static boolean initialized;
    private static boolean worldOpenRequested;
    private static Screen handledWorldConfirmation;
    private static boolean autoDeploymentObserved;
    private static int stableDeploymentScreenTicks;
    private static boolean formationSelectionSent;
    private static volatile String formationFixtureResult;
    private static boolean compactSquadKeySent;
    private static boolean compactMapKeySent;
    private static boolean compactSupportModeSelected;
    private static boolean classTabClicked;
    private static int compactLoadoutRequestTick = -1;
    private static int largeLoadoutRequestTick = -1;
    private static boolean leaveSent;
    private static boolean createSent;
    private static int squadActionSentTick = -1;
    private static boolean commanderClaimSent;
    private static boolean commanderSnapshotRequested;
    private static int commanderActionSentTick = -1;
    private static boolean baseSetupQueued;
    private static volatile String baseSetupResult;
    private static volatile java.util.UUID baseFixturePointId;
    private static boolean deploymentSelectionSent;
    private static boolean deploymentPreflightQueued;
    private static boolean deploymentPreflightObserved;
    private static volatile String deploymentPreflightResult;
    private static boolean deploymentInventorySeedQueued;
    private static volatile String deploymentInventorySeedResult;
    private static boolean deploymentInventoryCheckQueued;
    private static volatile String deploymentInventoryCheckResult;
    private static boolean deploySent;
    private static boolean administratorToolSetupQueued;
    private static boolean administratorToolFinalizeQueued;
    private static volatile boolean administratorToolTemporaryOperator;
    private static volatile String administratorToolResult;
    private static boolean formationAdministratorSetupQueued;
    private static volatile boolean formationAdministratorReady;
    private static Boolean formationScreenCompact;
    private static int nextMarkerIndex;
    private static BattleClientActions.MarkerTool pendingMarker;
    private static int pendingMarkerSentTick = -1;
    private static int nextMarkerAllowedTick;
    private static boolean reconContactQueued;
    private static volatile String reconContactResult;
    private static volatile boolean terrainProbePending;
    private static volatile boolean terrainProbeSucceeded;
    private static volatile int terrainProbeNextAttemptTick;
    private static volatile String terrainProbeResult;
    private static volatile String terrainProbeLastFailure;
    private static int terrainProbeAttempts;
    private static Field terrainTexturesField;
    private static Field desiredTerrainRequestsField;
    private static boolean journeyMapFullscreenRedirectAttempted;
    private static boolean journeyMapFullscreenRedirectVerified;
    private static final java.util.Set<String> recordedTerrainCoverage = new java.util.HashSet<>();
    private static boolean cleanupQueued;
    private static volatile boolean cleanupFinished;
    private static boolean resultWritten;
    private static String failureReason;

    private UiRuntimeAcceptanceHarness() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || phase == Phase.STOPPED) {
            return;
        }
        try {
            tick(Minecraft.getInstance());
        } catch (Throwable throwable) {
            fail("Unhandled harness exception: " + throwable);
            WokInfantryMod.LOGGER.error("[UI ACCEPTANCE] Harness failed", throwable);
        }
    }

    @SubscribeEvent
    public static void onScreenRendered(ScreenEvent.Render.Post event) {
        PendingCapture capture = pendingCapture;
        if (capture == null || event.getScreen() != capture.screen()) {
            return;
        }
        // ScreenEvent.Render.Post fires before GuiGraphics.flush() in 1.20.1. Defer the actual
        // framebuffer read until RenderTick END, after GameRenderer has flushed every UI batch.
        renderedCapture = capture;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        PendingCapture capture = renderedCapture;
        if (capture == null || capture != pendingCapture) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != capture.screen()) {
            renderedCapture = null;
            return;
        }
        renderedCapture = null;
        pendingCapture = null;
        WokInfantryMod.LOGGER.info("[UI ACCEPTANCE] Capturing {} at logical {}x{}",
                capture.fileName(), capture.screen().width, capture.screen().height);
        Screenshot.grab(minecraft.gameDirectory, capture.fileName(),
                minecraft.getMainRenderTarget(), component ->
                        screenshotCallbacks.put(capture.fileName(), component.getString()));
    }

    private static void tick(Minecraft minecraft) throws IOException {
        totalTicks++;
        phaseTicks++;
        if (!initialized) {
            initializeArtifacts(minecraft);
        }
        if (phase == Phase.OPEN_COMPACT_MAP || phase == Phase.CAPTURE_COMPACT_MAP
                || phase == Phase.OPEN_LARGE_MAP || phase == Phase.CAPTURE_LARGE_MAP) {
            installCommanderSupportFixture(minecraft);
        }
        if (totalTicks > GLOBAL_TIMEOUT_TICKS && phase != Phase.FINISH
                && phase != Phase.FAIL) {
            fail("Global timeout in phase " + phase);
        }
        if (phaseTicks > PHASE_TIMEOUT_TICKS && phase != Phase.WAIT_FOR_LOGIN
                && phase != Phase.WAIT_FOR_FILES && phase != Phase.FINISH
                && phase != Phase.FAIL) {
            fail("Phase timeout in " + phase + feedbackSuffix());
        }

        switch (phase) {
            case WAIT_FOR_LOGIN -> waitForLogin(minecraft);
            case CAPTURE_DEPLOYMENT -> captureScreen(minecraft, SquadScreen.class,
                    SCREENSHOTS[0], Phase.OPEN_COMPACT_SQUADS);
            case OPEN_COMPACT_SQUADS -> openCompactSquads(minecraft);
            case CAPTURE_COMPACT_SQUADS -> captureScreen(minecraft, SquadScreen.class,
                    SCREENSHOTS[1], Phase.OPEN_COMPACT_CLASSES);
            case OPEN_COMPACT_CLASSES -> openCompactClasses(minecraft);
            case CAPTURE_COMPACT_CLASSES -> captureScreen(minecraft, SquadScreen.class,
                    SCREENSHOTS[2], Phase.OPEN_COMPACT_LOADOUT);
            case OPEN_COMPACT_LOADOUT -> openLoadout(minecraft, true);
            case CAPTURE_COMPACT_LOADOUT -> captureScreen(minecraft,
                    PlayerLoadoutScreen.class, SCREENSHOTS[7], Phase.ENSURE_LEADER);
            case ENSURE_LEADER -> ensureLeader();
            case CLAIM_COMMANDER -> claimCommander();
            case WAIT_FOR_COMMANDER_FEEDBACK_CLEAR -> waitForCommanderFeedbackClear();
            case OPEN_COMPACT_COMMANDER_SQUADS -> openCompactCommanderSquads(minecraft);
            case CAPTURE_COMPACT_COMMANDER_SQUADS -> captureScreen(minecraft,
                    SquadScreen.class, SCREENSHOTS[6], Phase.SET_BASE);
            case SET_BASE -> setBase(minecraft);
            case SELECT_DEPLOYMENT -> selectDeploymentPoint();
            case DEPLOY -> deploy(minecraft);
            case WAIT_FOR_ACTIVE -> waitForActiveDeployment(minecraft);
            case VERIFY_ADMINISTRATOR_TOOLS -> verifyAdministratorTools(minecraft);
            case CREATE_MARKERS -> createMarkers();
            case OPEN_COMPACT_MAP -> openCompactMap(minecraft);
            case CAPTURE_COMPACT_MAP -> captureScreen(minecraft, TacticalMapScreen.class,
                    SCREENSHOTS[3], Phase.WAIT_FOR_FEEDBACK_CLEAR);
            case WAIT_FOR_FEEDBACK_CLEAR -> waitForFeedbackClear();
            case OPEN_LARGE_SQUADS -> openLargeSquads(minecraft);
            case CAPTURE_LARGE_SQUADS -> captureScreen(minecraft, SquadScreen.class,
                    SCREENSHOTS[4], Phase.OPEN_LARGE_LOADOUT);
            case OPEN_LARGE_LOADOUT -> openLoadout(minecraft, false);
            case CAPTURE_LARGE_LOADOUT -> captureScreen(minecraft,
                    PlayerLoadoutScreen.class, SCREENSHOTS[8], Phase.OPEN_LARGE_MAP);
            case OPEN_LARGE_MAP -> openLargeMap(minecraft);
            case CAPTURE_LARGE_MAP -> captureScreen(minecraft, TacticalMapScreen.class,
                    SCREENSHOTS[5], Phase.OPEN_COMPACT_FORMATION);
            case OPEN_COMPACT_FORMATION -> openFormationVote(minecraft, true);
            case CAPTURE_COMPACT_FORMATION -> captureScreen(minecraft,
                    FormationSelectionScreen.class, SCREENSHOTS[9],
                    Phase.OPEN_LARGE_FORMATION);
            case OPEN_LARGE_FORMATION -> openFormationVote(minecraft, false);
            case CAPTURE_LARGE_FORMATION -> captureScreen(minecraft,
                    FormationSelectionScreen.class, SCREENSHOTS[10],
                    Phase.OPEN_COMPACT_ADMIN_LOADOUT);
            case OPEN_COMPACT_ADMIN_LOADOUT -> openAdminLoadout(minecraft, true);
            case CAPTURE_COMPACT_ADMIN_LOADOUT -> captureScreen(minecraft,
                    AdminLoadoutScreen.class, SCREENSHOTS[11],
                    Phase.OPEN_COMPACT_ADMIN_CLASS_SETTINGS);
            case OPEN_COMPACT_ADMIN_CLASS_SETTINGS -> openCompactAdminClassSettings(minecraft);
            case CAPTURE_COMPACT_ADMIN_CLASS_SETTINGS -> captureScreen(minecraft,
                    AdminLoadoutScreen.class, SCREENSHOTS[13],
                    Phase.OPEN_LARGE_ADMIN_LOADOUT);
            case OPEN_LARGE_ADMIN_LOADOUT -> openAdminLoadout(minecraft, false);
            case CAPTURE_LARGE_ADMIN_LOADOUT -> captureScreen(minecraft,
                    AdminLoadoutScreen.class, SCREENSHOTS[12], Phase.WAIT_FOR_FILES);
            case WAIT_FOR_FILES -> waitForFiles(minecraft);
            case FINISH -> finish(minecraft, true);
            case FAIL -> finish(minecraft, false);
            case STOPPED -> {
            }
        }
    }

    private static void initializeArtifacts(Minecraft minecraft) throws IOException {
        initialized = true;
        // Automated windows are intentionally not focused. Keep the integrated server ticking
        // after deployment/respawn packets close or replace screens, matching the dedicated
        // network harness and a real multiplayer server.
        minecraft.options.pauseOnLostFocus = false;
        Path screenshots = minecraft.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(screenshots);
        for (String fileName : SCREENSHOTS) {
            Path file = screenshots.resolve(fileName);
            screenshotBaselines.put(fileName, Files.isRegularFile(file)
                    ? Files.getLastModifiedTime(file).toMillis() : -1L);
        }
        observations.add("harness=src/uiTest (excluded from production JAR)");
        boolean journeyMapLoaded = ModList.get().isLoaded("journeymap");
        observations.add("journeymapLoaded=" + journeyMapLoaded
                + " expected=" + EXPECTED_JOURNEYMAP_LOADED);
        observations.add("expectedTerrainProvider=" + EXPECTED_TERRAIN_PROVIDER);
        if (!EXPECTED_TERRAIN_PROVIDER.equals("journeymap")) {
            fail("JourneyMap 6 is mandatory; unsupported terrain provider expectation: "
                    + EXPECTED_TERRAIN_PROVIDER);
        } else if (!EXPECTED_JOURNEYMAP_LOADED_RAW.equals("true")
                && !EXPECTED_JOURNEYMAP_LOADED_RAW.equals("false")) {
            fail("Expected JourneyMap loaded state must be true or false, got: "
                    + EXPECTED_JOURNEYMAP_LOADED_RAW);
        } else if (journeyMapLoaded != EXPECTED_JOURNEYMAP_LOADED) {
            fail("JourneyMap loaded state mismatch: expected "
                    + EXPECTED_JOURNEYMAP_LOADED + ", got " + journeyMapLoaded);
        }
        WokInfantryMod.LOGGER.info("[UI ACCEPTANCE] Harness armed; waiting for integrated login");
    }

    private static void waitForLogin(Minecraft minecraft) {
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        selectFormationFixtureWhenRequired(minecraft);
        if (formationFixtureResult != null && formationFixtureResult.startsWith("ERROR:")) {
            fail(formationFixtureResult);
            return;
        }
        if (minecraft.player == null || minecraft.level == null
                || minecraft.getSingleplayerServer() == null || snapshot == null
                || !BattleClientActions.available()) {
            if (minecraft.screen instanceof ConfirmScreen confirmScreen
                    && handledWorldConfirmation != confirmScreen && phaseTicks >= 2) {
                Button proceed = confirmScreen.children().stream()
                        .filter(Button.class::isInstance)
                        .map(Button.class::cast)
                        .filter(button -> button.active && button.visible)
                        .min(java.util.Comparator.comparingInt(Button::getX))
                        .orElse(null);
                if (proceed != null) {
                    handledWorldConfirmation = confirmScreen;
                    observations.add("worldConfirmation=" + proceed.getMessage().getString());
                    WokInfantryMod.LOGGER.info(
                            "[UI ACCEPTANCE] Confirming isolated fixture prompt with '{}'",
                            proceed.getMessage().getString());
                    proceed.onPress();
                    return;
                }
            }
            if (!worldOpenRequested && phaseTicks >= 10
                    && minecraft.getSingleplayerServer() == null) {
                worldOpenRequested = true;
                Screen returnScreen = minecraft.screen == null
                        ? new TitleScreen() : minecraft.screen;
                observations.add("quickPlayFallback=WorldOpenFlows.loadLevel");
                observations.add("fallbackParentScreen="
                        + returnScreen.getClass().getSimpleName());
                WokInfantryMod.LOGGER.info(
                        "[UI ACCEPTANCE] Quick Play did not enter the fixture; loading it explicitly from {}",
                        returnScreen.getClass().getSimpleName());
                minecraft.createWorldOpenFlows().loadLevel(returnScreen, "wok_ui_test");
            }
            if (phaseTicks > PHASE_TIMEOUT_TICKS * 2) {
                fail("Integrated login/snapshot timeout" + feedbackSuffix());
            }
            return;
        }
        if (!(minecraft.screen instanceof SquadScreen)) {
            stableDeploymentScreenTicks = 0;
            if (formationFixtureResult != null
                    && formationFixtureResult.startsWith("OK:")
                    && minecraft.screen == null && phaseTicks >= 20) {
                // The synthetic integrated-server vote can complete while the client is still on
                // ReceivingLevelScreen. That loading screen replaces the deployment screen sent by
                // the normal S2C snapshot, so reopen the already-authorized state once the world is
                // visibly stable.
                minecraft.setScreen(new SquadScreen(null));
                observations.add("fixtureDeploymentScreenReopenedAfterWorldSync=true");
                return;
            }
            if (phaseTicks > 250) {
                fail("Login snapshot arrived but the deployment screen did not auto-open");
            }
            return;
        }
        stableDeploymentScreenTicks++;
        if (stableDeploymentScreenTicks < 20) {
            return;
        }
        autoDeploymentObserved = true;
        minecraft.getTutorial().setStep(TutorialSteps.NONE);
        minecraft.getToasts().clear();
        minecraft.getWindow().setWindowed(960, 720);
        minecraft.resizeDisplay();
        setGuiScale(minecraft, 3);
        observations.add("autoDeploymentScreen=true");
        observations.add("compactLogicalSize=" + logicalSize(minecraft));
        transition(Phase.CAPTURE_DEPLOYMENT);
    }

    private static void selectFormationFixtureWhenRequired(Minecraft minecraft) {
        if (formationSelectionSent || minecraft.player == null || minecraft.level == null
                || minecraft.getSingleplayerServer() == null) {
            return;
        }
        var selection = ClientFormationState.snapshot();
        if (selection == null || !selection.selectionRequired()) {
            return;
        }
        for (var faction : selection.factions()) {
            if (!faction.available()) {
                continue;
            }
            for (var formation : faction.formations()) {
                if (!formation.available()) {
                    continue;
                }
                formationSelectionSent = true;
                observations.add("formationSelection=" + faction.id() + "/"
                        + formation.id());
                WokInfantryMod.LOGGER.info(
                        "[UI ACCEPTANCE] Opening, voting and locking fixture formation {}/{}",
                        faction.id(), formation.id());
                MinecraftServer server = minecraft.getSingleplayerServer();
                java.util.UUID playerId = minecraft.player.getUUID();
                long generation = selection.generation();
                server.execute(() -> configureFormationVoteFixture(server, playerId,
                        generation, faction.id(), formation.id()));
                return;
            }
        }
    }

    private static void configureFormationVoteFixture(MinecraftServer server,
                                                      java.util.UUID playerId,
                                                      long generation,
                                                      String factionId,
                                                      String formationId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        FormationService formations = player == null
                ? null : FormationService.get(player).orElse(null);
        if (player == null || formations == null) {
            formationFixtureResult = "ERROR: Integrated formation service is unavailable";
            return;
        }
        boolean temporaryOperator = !server.getPlayerList().isOp(player.getGameProfile());
        if (temporaryOperator) {
            server.getPlayerList().op(player.getGameProfile());
        }
        try {
            ActionResult opened = formations.openVote(player, factionId, true);
            ActionResult joined = opened.success()
                    ? formations.selectFaction(player, generation, factionId) : opened;
            ActionResult voted = joined.success()
                    ? formations.castVote(player, generation, formationId) : joined;
            ActionResult locked = voted.success()
                    ? formations.lockVote(player, factionId, formationId) : voted;
            if (!locked.success()) {
                formationFixtureResult = "ERROR: Formation vote fixture failed: "
                        + locked.code() + " / " + locked.message();
                return;
            }
            FormationNetwork.sendSnapshotToPlayer(player, false);
            BattleService.get(player).ifPresent(battle -> BattleNetwork.sendSnapshotToPlayer(
                    battle, player, BattleOpenTarget.DEPLOYMENT));
            formationFixtureResult = "OK:" + factionId + "/" + formationId;
        } finally {
            if (temporaryOperator) {
                server.getPlayerList().deop(player.getGameProfile());
            }
        }
    }

    private static void openCompactSquads(Minecraft minecraft) {
        if (phaseTicks == 1) {
            minecraft.setScreen(null);
        }
        if (!compactSquadKeySent && phaseTicks >= 2 && minecraft.screen == null) {
            clickBoundKey(GLFW.GLFW_KEY_K);
            compactSquadKeySent = true;
        }
        if (minecraft.screen instanceof SquadScreen && phaseTicks >= SCREEN_SETTLE_TICKS) {
            observations.add("KKeyNetworkOpen=true");
            transition(Phase.CAPTURE_COMPACT_SQUADS);
        }
    }

    private static void openCompactClasses(Minecraft minecraft) {
        if (!(minecraft.screen instanceof SquadScreen squadScreen)) {
            fail("Squad screen closed before the class-tab interaction");
            return;
        }
        if (!classTabClicked && phaseTicks >= 2) {
            int utilityWidth = 22;
            int tabCount = 5;
            int tabGap = 3;
            int available = Math.max(170, squadScreen.width - 16 - utilityWidth - 6);
            int tabWidth = Math.max(34, Math.min(96,
                    (available - tabGap * (tabCount - 1)) / tabCount));
            double classTabX = 8 + tabWidth + 3 + tabWidth / 2.0D;
            boolean clicked = squadScreen.mouseClicked(classTabX, 34.0D,
                    GLFW.GLFW_MOUSE_BUTTON_LEFT);
            if (!clicked) {
                fail("The class tab did not accept a real mouse click");
                return;
            }
            classTabClicked = true;
            observations.add("classTabMouseClick=true");
        }
        if (classTabClicked && phaseTicks >= SCREEN_SETTLE_TICKS) {
            transition(Phase.CAPTURE_COMPACT_CLASSES);
        }
    }

    private static void openLoadout(Minecraft minecraft, boolean compact) {
        if (minecraft.screen instanceof PlayerLoadoutScreen) {
            String sizeName = compact ? "compact" : "large";
            observations.add(sizeName + "LoadoutNetworkOpen=true");
            observations.add(sizeName + "LoadoutLogicalSize=" + logicalSize(minecraft));
            transition(compact ? Phase.CAPTURE_COMPACT_LOADOUT
                    : Phase.CAPTURE_LARGE_LOADOUT);
            return;
        }
        if (!(minecraft.screen instanceof SquadScreen)) {
            fail("Squad screen closed before opening the "
                    + (compact ? "compact" : "large") + " loadout screen");
            return;
        }

        int previousRequestTick = compact
                ? compactLoadoutRequestTick : largeLoadoutRequestTick;
        // OPEN_UI is rate-limited to one request per second. Wait a full 25 client ticks before
        // the first attempt and between retries so a preceding K/network open cannot make the
        // acceptance run flaky on a fast machine.
        if (phaseTicks >= 25
                && (previousRequestTick < 0 || totalTicks - previousRequestTick >= 25)) {
            BattleClientActions.openLoadout();
            if (compact) {
                compactLoadoutRequestTick = totalTicks;
            } else {
                largeLoadoutRequestTick = totalTicks;
            }
        }
    }

    private static void ensureLeader() {
        BattleSnapshot snapshot = requireSnapshot();
        if (snapshot == null) {
            return;
        }
        if (snapshot.squadLeader() && snapshot.ownSquad() != null
                && snapshot.permissions().canCreateMarkers()) {
            observations.add("leaderSquad=" + snapshot.ownSquad().id());
            transition(Phase.CLAIM_COMMANDER);
            return;
        }

        if (snapshot.ownSquad() != null) {
            if (!leaveSent) {
                BattleClientActions.leaveSquad();
                leaveSent = true;
                squadActionSentTick = totalTicks;
            } else if (totalTicks - squadActionSentTick > 120) {
                fail("Server did not confirm leaving the previous squad" + feedbackSuffix());
            }
            return;
        }

        if (!createSent) {
            SquadCallsign available = snapshot.squads().stream()
                    .filter(squad -> !squad.active())
                    .map(squad -> squad.callsign())
                    .findFirst().orElse(null);
            if (available == null) {
                fail("No inactive squad callsign is available to create a leader state");
                return;
            }
            BattleClientActions.createSquad(available);
            createSent = true;
            squadActionSentTick = totalTicks;
        } else if (totalTicks - squadActionSentTick > 120) {
            fail("Server did not confirm creating the UI test squad" + feedbackSuffix());
        }
    }

    private static void claimCommander() {
        BattleSnapshot snapshot = requireSnapshot();
        if (snapshot == null) {
            return;
        }
        if (snapshot.commander()) {
            observations.add("commanderClaimNetwork=true");
            transition(Phase.WAIT_FOR_COMMANDER_FEEDBACK_CLEAR);
            return;
        }
        if (!snapshot.permissions().canClaimCommander()) {
            fail("Leader fixture cannot claim the vacant commander role" + feedbackSuffix());
            return;
        }
        // CREATE and CLAIM share the production squad-action limiter. Model a deliberate second
        // click instead of firing both intents on adjacent client ticks.
        if (!commanderClaimSent && phaseTicks >= 10) {
            BattleClientActions.claimCommander();
            commanderClaimSent = true;
            commanderActionSentTick = totalTicks;
        } else if (commanderClaimSent && !commanderSnapshotRequested
                && totalTicks - commanderActionSentTick >= 25) {
            BattleClientActions.requestSnapshot();
            commanderSnapshotRequested = true;
        } else if (commanderClaimSent && totalTicks - commanderActionSentTick > 160) {
            fail("Server did not confirm the commander claim" + feedbackSuffix());
        }
    }

    private static void waitForCommanderFeedbackClear() {
        if (ClientBattleState.feedback() == null) {
            transition(Phase.OPEN_COMPACT_COMMANDER_SQUADS);
        }
    }

    private static void openCompactCommanderSquads(Minecraft minecraft) {
        if (phaseTicks == 1) {
            setGuiScale(minecraft, 3);
            minecraft.setScreen(new SquadScreen(null));
        }
        if (minecraft.screen instanceof SquadScreen && phaseTicks >= SCREEN_SETTLE_TICKS) {
            observations.add("commanderCompactLogicalSize=" + logicalSize(minecraft));
            transition(Phase.CAPTURE_COMPACT_COMMANDER_SQUADS);
        }
    }

    private static void setBase(Minecraft minecraft) {
        BattleSnapshot snapshot = requireSnapshot();
        if (snapshot == null) {
            return;
        }
        if (baseSetupResult != null && baseSetupResult.startsWith("ERROR:")) {
            fail(baseSetupResult);
            return;
        }
        if (!baseSetupQueued) {
            MinecraftServer server = minecraft.getSingleplayerServer();
            if (server == null || minecraft.player == null) {
                return;
            }
            baseSetupQueued = true;
            Faction expectedFaction = snapshot.faction();
            java.util.UUID playerId = minecraft.player.getUUID();
            server.execute(() -> configureBaseOnServer(server, playerId, expectedFaction));
            return;
        }
        if (baseSetupResult == null || !baseSetupResult.startsWith("OK:")) {
            return;
        }
        DeploymentPoint point = fixtureDeploymentPoint(snapshot);
        if (point != null) {
            observations.add("deploymentBase=" + point.dimension() + "@"
                    + point.position().toShortString());
            transition(Phase.SELECT_DEPLOYMENT);
        }
    }

    private static void selectDeploymentPoint() {
        BattleSnapshot snapshot = requireSnapshot();
        if (snapshot == null || snapshot.deployment().points().isEmpty()) {
            return;
        }
        DeploymentPoint point = fixtureDeploymentPoint(snapshot);
        if (point == null) {
            return;
        }
        if (point.id().equals(snapshot.deployment().selectedPointId())) {
            observations.add("deploymentPointSelected=" + point.kind() + "@"
                    + point.dimension() + ":" + point.position().toShortString());
            transition(Phase.DEPLOY);
            return;
        }
        if (!deploymentSelectionSent) {
            deploymentSelectionSent = true;
            BattleClientActions.selectDeploymentPoint(point.id());
        }
    }

    private static void deploy(Minecraft minecraft) {
        BattleSnapshot snapshot = requireSnapshot();
        if (snapshot == null) {
            return;
        }
        if (snapshot.deployment().phase() == DeploymentPhase.ACTIVE) {
            transition(Phase.WAIT_FOR_ACTIVE);
            return;
        }
        if (snapshot.deployment().phase() == DeploymentPhase.WAITING
                || !snapshot.deployment().canDeploy()) {
            return;
        }
        if (deploymentPreflightResult == null) {
            if (!deploymentPreflightQueued) {
                MinecraftServer server = minecraft.getSingleplayerServer();
                DeploymentPoint point = snapshot.deployment().points().stream()
                        .filter(candidate -> candidate.id().equals(
                                snapshot.deployment().selectedPointId()))
                        .findFirst().orElse(null);
                if (server == null || point == null) {
                    fail("Could not inspect the selected deployment point before deploy");
                    return;
                }
                deploymentPreflightQueued = true;
                server.execute(() -> deploymentPreflightResult =
                        describeDeploymentFixture(server, point));
            }
            return;
        }
        if (!deploymentPreflightObserved) {
            deploymentPreflightObserved = true;
            observations.add("deploymentPreflight=" + deploymentPreflightResult);
        }
        if (deploymentInventorySeedResult == null) {
            if (!deploymentInventorySeedQueued) {
                MinecraftServer server = minecraft.getSingleplayerServer();
                if (server == null || minecraft.player == null) {
                    fail("Could not seed the destructive-inventory deployment fixture");
                    return;
                }
                deploymentInventorySeedQueued = true;
                java.util.UUID playerId = minecraft.player.getUUID();
                server.execute(() -> deploymentInventorySeedResult =
                        seedDeploymentInventory(server, playerId));
            }
            return;
        }
        if (!deploymentInventorySeedResult.startsWith("OK:")) {
            fail(deploymentInventorySeedResult);
            return;
        }
        if (!deploySent) {
            deploySent = true;
            BattleClientActions.deploy();
            transition(Phase.WAIT_FOR_ACTIVE);
        }
    }

    private static void waitForActiveDeployment(Minecraft minecraft) {
        BattleSnapshot snapshot = requireSnapshot();
        if (snapshot == null || minecraft.player == null || minecraft.gameMode == null) {
            return;
        }
        if (snapshot.deployment().phase() != DeploymentPhase.ACTIVE) {
            return;
        }
        if (minecraft.gameMode.getPlayerMode() != GameType.SURVIVAL) {
            fail("Deployment reached ACTIVE without client survival mode");
            return;
        }
        for (LoadoutSlot slot : LoadoutSlot.values()) {
            ItemStack stack = minecraft.player.getInventory().getItem(slot.hotbarIndex());
            if (stack.isEmpty() || !KitProvenance.isIssued(stack)) {
                fail("ACTIVE deployment did not deliver a provenance-tagged six-slot kit at "
                        + slot.id());
                return;
            }
        }
        if (deploymentInventoryCheckResult == null) {
            if (!deploymentInventoryCheckQueued) {
                MinecraftServer server = minecraft.getSingleplayerServer();
                deploymentInventoryCheckQueued = true;
                java.util.UUID playerId = minecraft.player.getUUID();
                server.execute(() -> deploymentInventoryCheckResult =
                        verifyDeploymentInventoryPolicy(server, playerId));
            }
            return;
        }
        if (!deploymentInventoryCheckResult.startsWith("OK:")) {
            fail(deploymentInventoryCheckResult);
            return;
        }
        observations.add("deploymentActive=true");
        observations.add("deploymentInventoryPolicy=" + deploymentInventoryCheckResult);
        observations.add("deploymentKitSlots=" + LoadoutSlot.values().length);
        observations.add("deploymentDimension="
                + minecraft.player.level().dimension().location());
        transition(Phase.VERIFY_ADMINISTRATOR_TOOLS);
    }

    private static void verifyAdministratorTools(Minecraft minecraft) {
        BattleSnapshot snapshot = requireSnapshot();
        if (snapshot == null || snapshot.deployment().phase() != DeploymentPhase.ACTIVE) {
            fail("ACTIVE deployment ended during administrator beacon-tool verification"
                    + feedbackSuffix());
            return;
        }
        if (administratorToolResult != null && administratorToolResult.startsWith("ERROR:")) {
            fail(administratorToolResult);
            return;
        }
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null || minecraft.player == null) {
            return;
        }
        if (!administratorToolSetupQueued) {
            administratorToolSetupQueued = true;
            java.util.UUID playerId = minecraft.player.getUUID();
            server.execute(() -> installAdministratorTools(server, playerId));
            return;
        }
        if ("INSTALLED".equals(administratorToolResult) && phaseTicks >= 30
                && !administratorToolFinalizeQueued) {
            administratorToolFinalizeQueued = true;
            java.util.UUID playerId = minecraft.player.getUUID();
            server.execute(() -> finishAdministratorToolCheck(server, playerId));
            return;
        }
        if ("PASS".equals(administratorToolResult)) {
            observations.add("administratorBeaconToolsSurvivedActiveTicks=true");
            BattleClientActions.requestSnapshot();
            transition(Phase.CREATE_MARKERS);
        }
    }

    private static void installAdministratorTools(MinecraftServer server,
                                                  java.util.UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            administratorToolResult = "ERROR: Integrated player disappeared before tool check";
            return;
        }
        administratorToolTemporaryOperator = !server.getPlayerList().isOp(
                player.getGameProfile());
        if (administratorToolTemporaryOperator) {
            server.getPlayerList().op(player.getGameProfile());
        }
        player.getInventory().setItem(6,
                new ItemStack(InfantryItems.DEPLOYMENT_BEACON.get()));
        player.getInventory().setItem(7, new ItemStack(Items.BLUE_DYE));
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        administratorToolResult = "INSTALLED";
    }

    private static void finishAdministratorToolCheck(MinecraftServer server,
                                                     java.util.UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            administratorToolResult = "ERROR: Integrated player disappeared during tool check";
            return;
        }
        DeploymentService deployment = DeploymentService.get(player).orElse(null);
        boolean active = deployment != null
                && deployment.viewFor(player).phase() == DeploymentPhase.ACTIVE;
        boolean toolsPresent = player.getInventory().getItem(6)
                .is(InfantryItems.DEPLOYMENT_BEACON.get())
                && player.getInventory().getItem(7).is(Items.BLUE_DYE);
        player.getInventory().setItem(6, ItemStack.EMPTY);
        player.getInventory().setItem(7, ItemStack.EMPTY);
        if (administratorToolTemporaryOperator) {
            server.getPlayerList().deop(player.getGameProfile());
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        if (!active || !toolsPresent) {
            administratorToolResult = "ERROR: ACTIVE inventory enforcement removed beacon tools "
                    + "or terminated deployment";
            return;
        }
        BattleService.get(player).ifPresent(battle ->
                BattleNetwork.sendSnapshotToPlayer(battle, player, BattleOpenTarget.NONE));
        administratorToolResult = "PASS";
    }

    private static void configureBaseOnServer(MinecraftServer server,
                                              java.util.UUID playerId,
                                              Faction expectedFaction) {
        try {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            BattleService battle = player == null ? null : BattleService.get(player).orElse(null);
            DeploymentService deployment = player == null
                    ? null : DeploymentService.get(player).orElse(null);
            Faction faction = battle == null ? null
                    : battle.factionOf(playerId).orElse(null);
            if (player == null || battle == null || deployment == null || faction == null
                    || faction != expectedFaction) {
                baseSetupResult = "ERROR: Integrated server battle state is unavailable";
                return;
            }
            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld == null) {
                baseSetupResult = "ERROR: Integrated overworld is unavailable";
                return;
            }
            BlockPos spawn = prepareSafeBaseFixture(overworld);
            boolean temporaryOperator = !server.getPlayerList().isOp(player.getGameProfile());
            ActionResult result;
            try {
                // The fixture is isolated. Grant only the exact administrator state required by
                // /battle deployment setbase, then revoke it before the authoritative UI snapshot.
                if (temporaryOperator) {
                    server.getPlayerList().op(player.getGameProfile());
                }
                result = deployment.setMainBaseAt(player, faction, overworld, spawn, 0.0F);
            } finally {
                if (temporaryOperator) {
                    server.getPlayerList().deop(player.getGameProfile());
                }
            }
            if (!result.success()) {
                baseSetupResult = "ERROR: Could not set UI test deployment base: "
                        + result.message();
                return;
            }
            BattleNetwork.sendSnapshotToPlayer(battle, player, BattleOpenTarget.NONE);
            DeploymentPoint configured = deployment.mainBase(faction).orElse(null);
            if (configured == null) {
                baseSetupResult = "ERROR: Configured base was not persisted";
                return;
            }
            baseFixturePointId = configured.id();
            baseSetupResult = "OK:" + result.message();
        } catch (Throwable throwable) {
            baseSetupResult = "ERROR: Base setup exception: " + throwable;
            WokInfantryMod.LOGGER.error("[UI ACCEPTANCE] Base setup failed", throwable);
        }
    }

    /**
     * The UI seed is copied from the latest GameTest world and therefore must not assume that the
     * shared spawn still has solid terrain. Build a tiny deterministic platform in the isolated
     * copy so this acceptance run validates the real safe-spawn check instead of depending on
     * stale fixture geometry.
     */
    private static BlockPos prepareSafeBaseFixture(ServerLevel level) {
        BlockPos sharedSpawn = level.getSharedSpawnPos();
        int minimumFeetY = level.getMinBuildHeight() + 2;
        int maximumFeetY = level.getMaxBuildHeight() - 2;
        // The copied GameTest seed can place shared spawn below sea level. Clearing only two
        // water blocks creates a momentary air pocket that refills before the deploy packet is
        // processed. Put the deterministic fixture above sea level so the second safety scan
        // validates stable terrain rather than a transient fluid update.
        int requestedFeetY = Math.max(sharedSpawn.getY(), level.getSeaLevel() + 4);
        int feetY = Math.max(minimumFeetY, Math.min(maximumFeetY, requestedFeetY));
        BlockPos feet = new BlockPos(sharedSpawn.getX(), feetY, sharedSpawn.getZ());
        level.getChunkAt(feet);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos column = feet.offset(x, 0, z);
                level.setBlockAndUpdate(column.below(), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(column, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(column.above(), Blocks.AIR.defaultBlockState());
            }
        }
        return feet;
    }

    private static String describeDeploymentFixture(MinecraftServer server,
                                                     DeploymentPoint point) {
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null || !Level.OVERWORLD.location().equals(point.dimension())) {
            return "ERROR:unexpectedDimension=" + point.dimension();
        }
        BlockPos feet = point.position();
        BlockPos floor = feet.below();
        return "floor=" + level.getBlockState(floor)
                + ",feet=" + level.getBlockState(feet)
                + ",head=" + level.getBlockState(feet.above())
                + ",floorFluid=" + level.getFluidState(floor)
                + ",feetFluid=" + level.getFluidState(feet)
                + ",headFluid=" + level.getFluidState(feet.above());
    }

    private static String seedDeploymentInventory(MinecraftServer server,
                                                  java.util.UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return "ERROR: deployment inventory fixture player is offline";
        }
        player.getInventory().setItem(20, new ItemStack(Items.DIRT, 7));
        player.getInventory().setItem(36, new ItemStack(Items.IRON_BOOTS));
        player.getInventory().setItem(40, new ItemStack(Items.STICK, 3));
        player.getEnderChestInventory().clearContent();
        player.getEnderChestInventory().setItem(0, new ItemStack(Items.DIAMOND, 2));
        player.getInventory().setChanged();
        player.getEnderChestInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return "OK: seeded body items and an isolated Ender Chest sentinel";
    }

    private static String verifyDeploymentInventoryPolicy(MinecraftServer server,
                                                          java.util.UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return "ERROR: deployment inventory verification player is offline";
        }
        for (int slot : new int[]{20, 36, 40}) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && !KitProvenance.isIssued(stack)) {
                return "ERROR: personal body item survived deployment at inventory slot " + slot;
            }
        }
        ItemStack sentinel = player.getEnderChestInventory().getItem(0);
        if (!sentinel.is(Items.DIAMOND) || sentinel.getCount() != 2) {
            return "ERROR: deployment changed the Ender Chest sentinel";
        }
        for (int slot = 1; slot < player.getEnderChestInventory().getContainerSize(); slot++) {
            if (!player.getEnderChestInventory().getItem(slot).isEmpty()) {
                return "ERROR: deployment inserted a body item into Ender Chest slot " + slot;
            }
        }
        return "OK: body items cleared and Ender Chest remained untouched";
    }

    private static void createMarkers() {
        BattleSnapshot snapshot = requireSnapshot();
        if (snapshot == null || snapshot.deployment().points().isEmpty()) {
            return;
        }
        EnumSet<TacticalMarkerType> present = EnumSet.noneOf(TacticalMarkerType.class);
        ClientBattleState.activeMarkers().forEach(marker -> present.add(marker.type()));
        if (pendingMarker != null) {
            TacticalMarkerType pendingType = TacticalMarkerType.valueOf(pendingMarker.name());
            if (present.contains(pendingType)) {
                nextMarkerIndex++;
                pendingMarker = null;
                pendingMarkerSentTick = -1;
                // MAP_MARKER is deliberately rate-limited at the packet boundary. Mirror a real
                // leader's deliberate clicks instead of sending the next intent in the same tick.
                boolean filledRateWindow = nextMarkerIndex
                        % BattleRules.MAX_MARKERS_PER_RATE_WINDOW == 0
                        && nextMarkerIndex < REQUIRED_MARKERS.size();
                int delayTicks = filledRateWindow
                        ? (int) Math.ceil(BattleRules.MARKER_RATE_WINDOW_MILLIS / 50.0D) + 5
                        : 25;
                nextMarkerAllowedTick = totalTicks + delayTicks;
                return;
            } else if (totalTicks - pendingMarkerSentTick > 120) {
                fail("Server did not confirm marker " + pendingMarker + feedbackSuffix());
                return;
            } else {
                return;
            }
        }
        while (nextMarkerIndex < REQUIRED_MARKERS.size()
                && present.contains(TacticalMarkerType.valueOf(
                REQUIRED_MARKERS.get(nextMarkerIndex).name()))) {
            nextMarkerIndex++;
        }
        if (nextMarkerIndex >= REQUIRED_MARKERS.size()) {
            if (!present.contains(TacticalMarkerType.RECON_CONTACT)) {
                if (reconContactResult != null && reconContactResult.startsWith("ERROR:")) {
                    fail(reconContactResult);
                    return;
                }
                if (!reconContactQueued) {
                    queueReconContactFixture(snapshot);
                }
                return;
            }
            observations.add("activeMarkerCount=" + ClientBattleState.activeMarkers().size());
            observations.add("markerTypes=" + present);
            observations.add("reconContactRedDot=true");
            transition(Phase.OPEN_COMPACT_MAP);
            return;
        }
        if (totalTicks < nextMarkerAllowedTick) {
            return;
        }

        DeploymentPoint point = fixtureDeploymentPoint(snapshot);
        if (point == null) {
            return;
        }
        BattleClientActions.MarkerTool tool = REQUIRED_MARKERS.get(nextMarkerIndex);
        double x = point.position().getX() + 0.5D;
        double z = point.position().getZ() + 0.5D;
        BattleClientActions.MarkerDraft marker = switch (tool) {
            case INFANTRY -> BattleClientActions.MarkerDraft.point(tool, point.dimension(),
                    x + 96.0D, z);
            case TANK -> BattleClientActions.MarkerDraft.point(tool, point.dimension(),
                    x, z + 96.0D);
            case IFV -> BattleClientActions.MarkerDraft.point(tool, point.dimension(),
                    x - 96.0D, z);
            case DEFEND -> BattleClientActions.MarkerDraft.point(tool, point.dimension(),
                    x + 64.0D, z - 96.0D);
            case RALLY -> BattleClientActions.MarkerDraft.point(tool, point.dimension(),
                    x - 64.0D, z - 96.0D);
            case ATTACK_DIRECTION -> new BattleClientActions.MarkerDraft(tool,
                    point.dimension(), x - 72.0D, z - 72.0D, x + 72.0D, z + 72.0D);
            default -> throw new IllegalStateException("Unexpected required marker " + tool);
        };
        BattleClientActions.createMarker(marker);
        pendingMarker = tool;
        pendingMarkerSentTick = totalTicks;
    }

    private static void queueReconContactFixture(BattleSnapshot snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null || minecraft.player == null || snapshot.faction() == null) {
            return;
        }
        reconContactQueued = true;
        java.util.UUID playerId = minecraft.player.getUUID();
        Faction faction = snapshot.faction();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            BattleService battle = player == null ? null
                    : BattleService.get(player).orElse(null);
            if (player == null || battle == null) {
                reconContactResult = "ERROR: UI fixture could not resolve the commander";
                return;
            }
            SupportIntelContact contact = new SupportIntelContact(
                    java.util.UUID.nameUUIDFromBytes("ui-recon-contact".getBytes(
                            StandardCharsets.UTF_8)),
                    player.getX() + 36.0D, player.getY(), player.getZ() + 24.0D);
            ActionResult result = battle.publishSupportIntel(player,
                    java.util.UUID.nameUUIDFromBytes("ui-recon-call".getBytes(
                            StandardCharsets.UTF_8)), faction,
                    player.serverLevel().dimension(), List.of(contact), 20 * 60 * 10);
            reconContactResult = result.success()
                    ? "OK: tactical map received a satellite red-dot fixture"
                    : "ERROR: " + result.message();
        });
    }

    private static void openCompactMap(Minecraft minecraft) {
        if (phaseTicks == 1) {
            minecraft.setScreen(null);
        }
        if (!compactMapKeySent && phaseTicks >= 2 && minecraft.screen == null) {
            clickBoundKey(GLFW.GLFW_KEY_M);
            compactMapKeySent = true;
        }
        if (!(minecraft.screen instanceof TacticalMapScreen tacticalMapScreen)
                || phaseTicks < SCREEN_SETTLE_TICKS) {
            return;
        }

        String actualProvider = TacticalMapTerrainRegistry.isReady()
                ? TacticalMapTerrainRegistry.activeProviderId() : "grid";
        if (!EXPECTED_TERRAIN_PROVIDER.equals(actualProvider)) {
            if (phaseTicks >= 250) {
                fail("Terrain provider mismatch: expected " + EXPECTED_TERRAIN_PROVIDER
                        + ", got " + actualProvider + " (registered="
                        + TacticalMapTerrainRegistry.activeProviderId() + ")");
            }
            return;
        }

        if (!JourneyMapUiPolicy.nativeMinimapHidden()) {
            if (phaseTicks >= 250) {
                fail("JourneyMap 6 terrain provider is ready, but its native minimap is visible");
            }
            return;
        }
        if (!journeyMapFullscreenRedirectAttempted) {
            journeyMapFullscreenRedirectAttempted = true;
            try {
                Object nativeFullscreen = Class.forName(
                                "journeymap.client.ui.fullscreen.Fullscreen")
                        .getConstructor().newInstance();
                if (!(nativeFullscreen instanceof Screen screen)) {
                    fail("JourneyMap Fullscreen is not a Minecraft Screen");
                    return;
                }
                minecraft.setScreen(screen);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                fail("Could not exercise JourneyMap fullscreen redirect: " + exception);
            }
            return;
        }
        if (!(minecraft.screen instanceof TacticalMapScreen)) {
            fail("JourneyMap native fullscreen was not redirected to the WOK tactical map");
            return;
        }
        if (!journeyMapFullscreenRedirectVerified) {
            journeyMapFullscreenRedirectVerified = true;
            observations.add("journeyMapNativeMinimapHidden=true");
            observations.add("journeyMapFullscreenRedirectedToWok=true");
        }

        if (!terrainProbeSucceeded && !terrainProbePending
                && totalTicks >= terrainProbeNextAttemptTick) {
            requestTerrainProbe(minecraft);
        }
        if (!terrainProbeSucceeded) {
            if (phaseTicks >= 350) {
                fail("JourneyMap terrain probe did not return a usable image after "
                        + terrainProbeAttempts + " attempt(s); pending="
                        + terrainProbePending + "; last=" + terrainProbeLastFailure);
            }
            return;
        }

        if (!compactSupportModeSelected) {
            BattleSnapshot snapshot = ClientBattleState.snapshot();
            if (snapshot == null || snapshot.support().options().size() != 3) {
                fail("Compact commander-support acceptance requires three support options");
                return;
            }
            String supportLabel = Component.translatable(
                    "screen.wok_infantry.map.mode.support").getString();
            Button supportButton = tacticalMapScreen.children().stream()
                    .filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .filter(button -> button.getMessage().getString().equals(supportLabel))
                    .findFirst().orElse(null);
            if (supportButton == null) {
                fail("Compact tactical map has no support-mode button");
                return;
            }
            supportButton.onPress();
            if (!supportButtonPresent(tacticalMapScreen,
                    new ResourceLocation("wok_commander_support",
                            "millennium_f15ex_jdam_1000lb"))) {
                fail("Compact tactical map did not render the F-15EX JDAM support button");
                return;
            }
            if (!supportButtonPresent(tacticalMapScreen,
                    new ResourceLocation("wok_commander_support",
                            "f16c_gbu12_paveway_500lb"))) {
                fail("Compact tactical map did not render the F-16C Paveway support button");
                return;
            }
            compactSupportModeSelected = true;
            observations.add("compactSupportOptions=3");
            observations.add("compactJdamSupportVisible=true");
            observations.add("compactPavewaySupportVisible=true");
            return;
        }

        observations.add("MKeyNetworkOpen=true");
        observations.add("compactMapLogicalSize=" + logicalSize(minecraft));
        observations.add("terrainProvider=" + actualProvider);
        if (terrainProbeResult != null) {
            observations.add("terrainProbe=" + terrainProbeResult);
        }
        transition(Phase.CAPTURE_COMPACT_MAP);
    }

    private static void requestTerrainProbe(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        DeploymentPoint terrainAnchor = tacticalTerrainAnchor();
        if (terrainAnchor == null) {
            return;
        }
        terrainProbePending = true;
        terrainProbeAttempts++;
        // Mirror the production screen's region-aligned request shape. JourneyMap 6.0.2's
        // public merger cannot safely crop an arbitrary sub-region, while its documented
        // maximum 32x32-chunk tile is exactly one 512x512 region and is our real code path.
        int anchorChunkX = Math.floorDiv(terrainAnchor.position().getX(), 16);
        int anchorChunkZ = Math.floorDiv(terrainAnchor.position().getZ(), 16);
        int chunkX = Math.floorDiv(anchorChunkX,
                TacticalMapTerrainRequest.MAX_CHUNK_SPAN)
                * TacticalMapTerrainRequest.MAX_CHUNK_SPAN;
        int chunkZ = Math.floorDiv(anchorChunkZ,
                TacticalMapTerrainRequest.MAX_CHUNK_SPAN)
                * TacticalMapTerrainRequest.MAX_CHUNK_SPAN;
        TacticalMapTerrainRequest request = new TacticalMapTerrainRequest(
                terrainAnchor.dimension(),
                TacticalMapTerrainRequest.Style.TOPOGRAPHY,
                chunkX, chunkZ,
                chunkX + TacticalMapTerrainRequest.MAX_CHUNK_SPAN,
                chunkZ + TacticalMapTerrainRequest.MAX_CHUNK_SPAN,
                4, false);
        observations.add("terrainProbeRequest=" + request.dimension() + "@"
                + chunkX + "," + chunkZ + " style=" + request.style());
        TacticalMapTerrainRegistry.requestTile(request,
                UiRuntimeAcceptanceHarness::completeTerrainProbe);
    }

    private static void completeTerrainProbe(NativeImage image) {
        String result = null;
        String failure = null;
        try {
            if (image == null) {
                failure = "JourneyMap terrain probe completed without an image";
            } else if (image.getWidth() <= 0 || image.getHeight() <= 0
                    || image.getWidth() > 512 || image.getHeight() > 512) {
                failure = "JourneyMap terrain probe returned invalid dimensions: "
                        + image.getWidth() + "x" + image.getHeight();
            } else {
                long nonZeroPixels = 0L;
                long fingerprint = 0xcbf29ce484222325L;
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int pixel = image.getPixelRGBA(x, y);
                        if (pixel != 0) {
                            nonZeroPixels++;
                        }
                        fingerprint ^= Integer.toUnsignedLong(pixel);
                        fingerprint *= 0x100000001b3L;
                    }
                }
                if (nonZeroPixels == 0L) {
                    failure = "JourneyMap terrain probe returned an all-zero image";
                } else {
                    result = image.getWidth() + "x" + image.getHeight()
                            + " nonZeroPixels=" + nonZeroPixels
                            + " fingerprint=0x" + Long.toUnsignedString(fingerprint, 16);
                }
            }
        } catch (RuntimeException exception) {
            failure = "JourneyMap terrain probe inspection failed: " + exception;
        } finally {
            if (image != null) {
                image.close();
            }
        }
        if (result != null) {
            terrainProbeResult = result;
            terrainProbeSucceeded = true;
        } else {
            terrainProbeLastFailure = failure;
            terrainProbeNextAttemptTick = totalTicks + 30;
        }
        terrainProbePending = false;
    }

    private static TerrainCoverage terrainCoverage(TacticalMapScreen screen,
                                                   ResourceLocation expectedDimension) {
        try {
            if (terrainTexturesField == null) {
                terrainTexturesField = TacticalMapScreen.class.getDeclaredField(
                        "terrainTextures");
                terrainTexturesField.setAccessible(true);
            }
            if (desiredTerrainRequestsField == null) {
                desiredTerrainRequestsField = TacticalMapScreen.class.getDeclaredField(
                        "desiredTerrainRequests");
                desiredTerrainRequestsField.setAccessible(true);
            }
            Object textureValue = terrainTexturesField.get(screen);
            Object desiredValue = desiredTerrainRequestsField.get(screen);
            if (!(textureValue instanceof Map<?, ?> textures)
                    || !(desiredValue instanceof List<?> desired)) {
                return new TerrainCoverage(-1, -1);
            }
            int loaded = 0;
            int total = 0;
            for (Object value : desired) {
                if (value instanceof TacticalMapTerrainRequest request) {
                    if (!request.dimension().equals(expectedDimension)) {
                        fail("TacticalMapScreen uploaded terrain for " + request.dimension()
                                + " while the deployment map targets " + expectedDimension);
                        return new TerrainCoverage(-1, -1);
                    }
                    total++;
                    if (textures.containsKey(request)) {
                        loaded++;
                    }
                }
            }
            return new TerrainCoverage(loaded, total);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            fail("Could not inspect TacticalMapScreen terrain uploads: " + exception);
            return new TerrainCoverage(-1, -1);
        }
    }

    private static boolean fullTerrainCoverageReady(TacticalMapScreen screen,
                                                    String screenshotName) {
        DeploymentPoint terrainAnchor = tacticalTerrainAnchor();
        if (terrainAnchor == null) {
            fail("JourneyMap acceptance has no authoritative deployment-map anchor");
            return false;
        }
        TerrainCoverage coverage = terrainCoverage(screen, terrainAnchor.dimension());
        if (coverage.loaded() < 0 || coverage.desired() < 0) {
            return false;
        }
        if (coverage.desired() == 0 || coverage.loaded() < coverage.desired()) {
            return false;
        }
        if (recordedTerrainCoverage.add(screenshotName)) {
            observations.add("screenTerrainCoverage[" + screenshotName + "]="
                    + coverage.loaded() + "/" + coverage.desired());
            if (SCREENSHOTS[3].equals(screenshotName)) {
                observations.add("screenTerrainTiles=" + coverage.loaded());
            }
        }
        return true;
    }

    private static DeploymentPoint tacticalTerrainAnchor() {
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        if (snapshot == null || snapshot.deployment().points().isEmpty()) {
            return null;
        }
        java.util.UUID selectedId = snapshot.deployment().selectedPointId();
        return snapshot.deployment().points().stream()
                .filter(point -> point.id().equals(selectedId))
                .findFirst()
                .orElse(snapshot.deployment().points().get(0));
    }

    private static void openLargeSquads(Minecraft minecraft) {
        if (phaseTicks == 1) {
            setGuiScale(minecraft, 1);
            minecraft.setScreen(new SquadScreen(null));
        }
        if (minecraft.screen instanceof SquadScreen && phaseTicks >= SCREEN_SETTLE_TICKS) {
            observations.add("largeLogicalSize=" + logicalSize(minecraft));
            transition(Phase.CAPTURE_LARGE_SQUADS);
        }
    }

    private static void waitForFeedbackClear() {
        if (ClientBattleState.feedback() == null) {
            transition(Phase.OPEN_LARGE_SQUADS);
        }
    }

    private static void openLargeMap(Minecraft minecraft) {
        if (phaseTicks == 1) {
            minecraft.setScreen(new TacticalMapScreen(null));
        }
        if (minecraft.screen instanceof TacticalMapScreen tacticalMapScreen
                && phaseTicks >= SCREEN_SETTLE_TICKS
                && fullTerrainCoverageReady(tacticalMapScreen, SCREENSHOTS[5])) {
            if (!supportButtonPresent(tacticalMapScreen,
                    new ResourceLocation("wok_commander_support",
                            "millennium_f15ex_jdam_1000lb"))) {
                fail("Large tactical map did not render the F-15EX JDAM support button");
                return;
            }
            if (!supportButtonPresent(tacticalMapScreen,
                    new ResourceLocation("wok_commander_support",
                            "f16c_gbu12_paveway_500lb"))) {
                fail("Large tactical map did not render the F-16C Paveway support button");
                return;
            }
            observations.add("largeJdamSupportVisible=true");
            observations.add("largePavewaySupportVisible=true");
            transition(Phase.CAPTURE_LARGE_MAP);
        }
    }

    /**
     * Keeps the production tactical map fed with a deterministic Millennium commander catalog.
     * This uiTest-only fixture is required because the optional commander-support MOD is not part
     * of the core client's isolated acceptance launch.
     */
    private static void installCommanderSupportFixture(Minecraft minecraft) {
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        if (snapshot == null) {
            return;
        }
        ResourceLocation jdamId = new ResourceLocation("wok_commander_support",
                "millennium_f15ex_jdam_1000lb");
        ResourceLocation reconId = new ResourceLocation(
                "wok_commander_support", "recon_satellite");
        ResourceLocation pavewayId = new ResourceLocation(
                "wok_commander_support", "f16c_gbu12_paveway_500lb");
        TacticalSupportMapPresentationRegistry.register(reconId,
                TacticalSupportMapPresentation.INTELLIGENCE);
        TacticalSupportMapPresentationRegistry.register(jdamId,
                TacticalSupportMapPresentation.OFFENSIVE);
        TacticalSupportMapPresentationRegistry.register(pavewayId,
                TacticalSupportMapPresentation.OFFENSIVE);
        if (snapshot.support().options().stream().anyMatch(option -> option.id().equals(jdamId))) {
            return;
        }
        List<SupportOptionView> options = List.of(
                new SupportOptionView(
                        reconId,
                        "support.wok_commander_support.recon_satellite",
                        "侦察卫星", "侦察卫星", SupportTargetMode.POINT, 150.0D,
                        true, "", 0L, false),
                new SupportOptionView(
                        jdamId,
                        "support.wok_commander_support.millennium_f15ex_jdam_1000lb",
                        "千禧年 F-15EX 杰达姆 1000磅空袭", "F-15EX JDAM空袭",
                        SupportTargetMode.POINT, 32.0D,
                        true, "", 0L, true),
                new SupportOptionView(
                        pavewayId,
                        "support.wok_commander_support.f16c_gbu12_paveway_500lb",
                        "F-16C GBU-12 宝石路 II 500磅精准空袭", "F-16C 宝石路空袭",
                        SupportTargetMode.POINT, 64.0D,
                        true, "", 0L, true));
        SupportMissionView activeJdam = new SupportMissionView(
                UUID.fromString("a9185f44-0dbd-4b0f-8ba7-d62f4397a14c"),
                jdamId, Level.OVERWORLD.location(), 80.0D, 80.0D,
                80.0D, 80.0D, snapshot.support().serverGameTick() + 80L, 5);
        SupportView support = new SupportView(options, List.of(activeJdam),
                snapshot.support().serverGameTick(),
                snapshot.support().structuralRevision() + 1L, true, "");
        ClientBattleState.update(snapshot.withSupport(support));
        if (minecraft.screen instanceof TacticalMapScreen tacticalMapScreen) {
            tacticalMapScreen.resize(minecraft, tacticalMapScreen.width,
                    tacticalMapScreen.height);
        }
    }

    private static boolean supportButtonPresent(TacticalMapScreen screen,
                                                ResourceLocation supportId) {
        try {
            Field field = TacticalMapScreen.class.getDeclaredField("supportButtons");
            field.setAccessible(true);
            Object value = field.get(screen);
            if (!(value instanceof Map<?, ?> buttons)) {
                fail("Tactical map support button registry has an unexpected type");
                return false;
            }
            Object button = buttons.get(supportId);
            if (!(button instanceof Button renderedButton)
                    || !renderedButton.visible
                    || renderedButton.getWidth() <= 0
                    || renderedButton.getHeight() <= 0) {
                return false;
            }
            observations.add("supportButton[" + supportId.getPath() + "]="
                    + renderedButton.getMessage().getString() + "@"
                    + renderedButton.getWidth() + "x" + renderedButton.getHeight());
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            fail("Could not inspect tactical map support buttons: " + exception);
            return false;
        }
    }

    private static void openFormationVote(Minecraft minecraft, boolean compact) {
        if (!formationAdministratorReady) {
            MinecraftServer server = minecraft.getSingleplayerServer();
            if (!formationAdministratorSetupQueued && server != null
                    && minecraft.player != null) {
                formationAdministratorSetupQueued = true;
                java.util.UUID playerId = minecraft.player.getUUID();
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                    if (player != null) {
                        server.getPlayerList().op(player.getGameProfile());
                        formationAdministratorReady = true;
                    }
                });
            }
            return;
        }
        if (minecraft.player == null || !minecraft.player.hasPermissions(
                BattleRules.ADMIN_PERMISSION_LEVEL)) {
            return;
        }
        if (!Objects.equals(formationScreenCompact, compact)) {
            formationScreenCompact = compact;
            setGuiScale(minecraft, compact ? 3 : 1);
            minecraft.setScreen(new FormationSelectionScreen(
                    formationVoteVisualFixture(compact), null));
            return;
        }
        if (minecraft.screen instanceof FormationSelectionScreen formationScreen
                && phaseTicks >= SCREEN_SETTLE_TICKS) {
            boolean administratorVoteActionVisible = formationScreen.children().stream()
                    .filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .anyMatch(button -> (compact ? "管理员开启投票" : "管理员锁定").equals(
                            button.getMessage().getString()) && button.visible);
            if (!administratorVoteActionVisible) {
                fail("Formation vote screen omitted the administrator "
                        + (compact ? "open" : "lock") + " control");
                return;
            }
            observations.add((compact ? "compact" : "large")
                    + "FormationLogicalSize=" + logicalSize(minecraft));
            observations.add((compact ? "compact" : "large")
                    + "FormationAdministratorLockVisible=true");
            transition(compact ? Phase.CAPTURE_COMPACT_FORMATION
                    : Phase.CAPTURE_LARGE_FORMATION);
        }
    }

    private static void openAdminLoadout(Minecraft minecraft, boolean compact) {
        if (phaseTicks == 1) {
            setGuiScale(minecraft, compact ? 3 : 1);
            minecraft.setScreen(new AdminLoadoutScreen(adminLoadoutVisualFixture()));
        }
        if (minecraft.screen instanceof AdminLoadoutScreen
                && phaseTicks >= SCREEN_SETTLE_TICKS) {
            AdminLoadoutScreen screen = (AdminLoadoutScreen) minecraft.screen;
            boolean addSlot = screen.children().stream().filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .anyMatch(button -> "+槽位".equals(button.getMessage().getString()));
            boolean editSlot = screen.children().stream().filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .anyMatch(button -> "设置".equals(button.getMessage().getString()));
            if (!addSlot || !editSlot) {
                fail("Administrator loadout omitted dynamic slot controls");
                return;
            }
            observations.add((compact ? "compact" : "large")
                    + "AdminLoadoutLogicalSize=" + logicalSize(minecraft));
            observations.add((compact ? "compact" : "large")
                    + "AdminDynamicSlotControls=true");
            transition(compact ? Phase.CAPTURE_COMPACT_ADMIN_LOADOUT
                    : Phase.CAPTURE_LARGE_ADMIN_LOADOUT);
        }
    }

    private static void openCompactAdminClassSettings(Minecraft minecraft) {
        if (phaseTicks == 1 && minecraft.screen instanceof AdminLoadoutScreen screen) {
            Button manage = screen.children().stream().filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .filter(button -> "职业管理".equals(button.getMessage().getString()))
                    .findFirst().orElse(null);
            if (manage == null) {
                fail("Compact administrator screen has no profession-management button");
                return;
            }
            manage.onPress();
        }
        if (minecraft.screen instanceof AdminLoadoutScreen
                && phaseTicks >= SCREEN_SETTLE_TICKS) {
            observations.add("compactAdminProfessionSettings=true");
            transition(Phase.CAPTURE_COMPACT_ADMIN_CLASS_SETTINGS);
        }
    }

    private static LoadoutSnapshot adminLoadoutVisualFixture() {
        LoadoutConfigData loadouts = LoadoutConfigData.defaultConfig();
        loadouts.findClass("assault").orElseThrow().addSlot(new LoadoutSlotDefinition(
                "helmet", "防弹头盔", LoadoutInventoryTarget.ARMOR_HEAD, false));
        loadouts.classes().add(new LoadoutClassDefinition(
                "custom_visual_medic", "战斗医疗员", true, 1));
        FormationConfigData formations = FormationClassEditor.apply(
                FormationConfigData.defaultConfig(), "academy", "default",
                "assault", "突破手", 8, FormationClassEditAction.UPDATE);
        formations = FormationClassEditor.apply(formations, "academy", "default",
                "custom_visual_medic", "战斗医疗员", 1,
                FormationClassEditAction.CREATE);
        return new LoadoutSnapshot(loadouts, new PlayerLoadoutData(), true, formations);
    }

    /** Display-only fixture exercising all five fixed categories without inventing game values. */
    private static FormationSelectionSnapshot formationVoteVisualFixture(
            boolean awaitingAdministratorOpen) {
        List<FormationSelectionView> academy = List.of(
                new FormationSelectionView("millennium_seminar_mobile",
                        "千禧年研讨会机动部队",
                        "由千禧年研讨会统一组建的先头力量，承担快速部署与快速反应任务。"
                                + "她们以多型斯特赖克（Stryker）轮式战车为核心，在航空兵支援下可于战斗初期"
                                + "投入大量轻型装甲载具；但后劲不足、单兵装备较为平庸，不擅长长时间消耗战。",
                        "wok_infantry:textures/gui/formations/"
                                + "millennium_seminar_mobile.png",
                        "mechanized", "机械化步兵营", 0, 40, true, "",
                        List.of("突击兵", "支援兵", "工程兵", "侦察兵"),
                        List.of("Alpha–Echo，每队 8 人"),
                        List.of("M1296 龙骑兵 ×3（不可再生）",
                                "M1128 MGS ×2（不可再生）",
                                "悍马 M2 ×2（5分钟）", "小鸟 机枪版（10分钟）"),
                        List.of("支援：无")),
                visualFormation("academy_light_infantry", "学院轻步兵营", "轻步兵框架",
                        "infantry", "步兵营", List.of("多兵站能力：待具体编制配置",
                                "队包冷却：待具体编制配置", "支援：未配置")),
                visualFormation("academy_armored", "学院装甲营", "装甲框架",
                        "armored", "装甲营", List.of("兵站上限：待具体编制配置",
                                "坦克补充时间：待配置", "支援：未配置")),
                visualFormation("academy_motorized", "学院摩步营", "摩托化框架",
                        "motorized", "摩步营", List.of("固定兵站：待配置",
                                "载具运输增援", "移动重生载具：未配置")),
                visualFormation("academy_special", "学院特种编制", "特种编制框架",
                        "special", "特种编制", List.of("能力：待具体编制配置",
                                "支援：未配置")));
        List<FormationSelectionView> caesar = List.of(
                visualFormation("caesar_infantry", "凯撒步兵营", "步兵框架",
                        "infantry", "步兵营", List.of("兵站：待配置", "支援：未配置")),
                visualFormation("caesar_armored", "凯撒装甲营", "装甲框架",
                        "armored", "装甲营", List.of("坦克数量：待配置", "支援：未配置")),
                visualFormation("caesar_motorized", "凯撒摩步营", "摩托化框架",
                        "motorized", "摩步营", List.of("载具：待配置")),
                new FormationSelectionView("caesar_234_mechanized",
                        "234机械化作战单元",
                        "凯撒重工是凯撒公司旗下的重型军事生产企业，战争前几乎包揽了整个基沃托斯的"
                                + "军火与军用车辆生产，234机械化作战单元则是其核心作战力量之一。"
                                + "该单元以CV90步兵战车伴随推进，并配备略显过时的豹2A4主战坦克，"
                                + "作战职能偏向机动轻步兵。其装备水平尚可，擅长在复杂战线中进行混战缠斗；"
                                + "但重型载具数量有限、战损补充缓慢，一旦脱离步兵协同或分散投入，"
                                + "便容易失去进攻节奏。",
                        "mechanized", "机械化步兵营", 0, 40, true, "",
                        List.of("突击兵", "支援兵", "工程兵", "侦察兵"),
                        List.of("Alpha–Echo，每队 8 人"),
                        List.of("豹2A4 ×1（15分钟）", "CV90 ×2（15分钟）",
                                "装甲无武装 HMMWV ×2（5分钟）"),
                        List.of("支援：通用编制支援")),
                visualFormation("caesar_special", "凯撒特种编制", "特种编制框架",
                        "special", "特种编制", List.of("能力：待配置")));
        return new FormationSelectionSnapshot(1L, true, "academy", "",
                awaitingAdministratorOpen ? FormationVotePhase.NOT_STARTED
                        : FormationVotePhase.OPEN,
                true, awaitingAdministratorOpen ? "" : "millennium_seminar_mobile", "",
                awaitingAdministratorOpen ? Map.of()
                        : Map.of("millennium_seminar_mobile", 5,
                        "academy_light_infantry", 4, "academy_armored", 3,
                        "academy_motorized", 2, "academy_special", 1),
                List.of(new FactionSelectionView("academy", "学院军", "蓝队", 15, 40,
                                true, academy),
                        new FactionSelectionView("caesar", "凯撒军", "红队", 14, 40,
                                true, caesar)));
    }

    private static FormationSelectionView visualFormation(String id, String name,
                                                           String description,
                                                           String categoryId,
                                                           String categoryName,
                                                           List<String> capabilities) {
        return new FormationSelectionView(id, name, description, categoryId, categoryName,
                0, 40, true, "", List.of("待配置"), List.of("待配置"),
                List.of("待配置"), capabilities);
    }

    private static void captureScreen(Minecraft minecraft, Class<? extends Screen> expected,
                                      String fileName, Phase next) throws IOException {
        // A dimension sync may briefly replace the visible screen after the render callback has
        // already captured the expected framebuffer. Let the asynchronous PNG write finish before
        // evaluating the next live screen.
        if (fileName.equals(waitingForCapture)) {
            if (fileUpdated(minecraft, fileName)) {
                observations.add(fileName + "=" + screenshotCallbacks.getOrDefault(fileName,
                        "written"));
                waitingForCapture = null;
                transition(next);
            }
            return;
        }
        if (!expected.isInstance(minecraft.screen)) {
            fail("Expected " + expected.getSimpleName() + " while capturing " + fileName
                    + ", got " + (minecraft.screen == null ? "world"
                    : minecraft.screen.getClass().getSimpleName()));
            return;
        }
        if (minecraft.screen instanceof TacticalMapScreen tacticalMapScreen
                && !fullTerrainCoverageReady(tacticalMapScreen, fileName)) {
            return;
        }
        if (!buttonsFit(minecraft, minecraft.screen)) {
            return;
        }
        if (waitingForCapture == null && pendingCapture == null
                && phaseTicks >= SCREEN_SETTLE_TICKS) {
            waitingForCapture = fileName;
            pendingCapture = new PendingCapture(fileName, minecraft.screen);
            return;
        }
    }

    private static boolean fileUpdated(Minecraft minecraft, String fileName) throws IOException {
        Path file = minecraft.gameDirectory.toPath().resolve("screenshots").resolve(fileName);
        if (!Files.isRegularFile(file) || Files.size(file) <= 0L) {
            return false;
        }
        long baseline = screenshotBaselines.getOrDefault(fileName, -1L);
        return Files.getLastModifiedTime(file).toMillis() > baseline;
    }

    private static boolean buttonsFit(Minecraft minecraft, Screen screen) {
        for (Button button : screen.children().stream()
                .filter(Button.class::isInstance).map(Button.class::cast).toList()) {
            if (!button.visible) {
                continue;
            }
            // TacticalBoardButton deliberately clips its displayed label to the exact inner
            // width. Its full narration component can therefore be wider without visual overflow.
            if (button.getClass().getSimpleName().equals("TacticalBoardButton")) {
                continue;
            }
            int available = Math.max(1, button.getWidth() - 6);
            int textWidth = minecraft.font.width(button.getMessage());
            if (textWidth > available) {
                fail("Visible button text overflows: '" + button.getMessage().getString()
                        + "' is " + textWidth + "px for " + available + "px");
                return false;
            }
        }
        return true;
    }

    private static void waitForFiles(Minecraft minecraft) throws IOException {
        for (String fileName : SCREENSHOTS) {
            if (!fileUpdated(minecraft, fileName)) {
                return;
            }
        }
        BattleSnapshot snapshot = requireSnapshot();
        if (snapshot == null) {
            return;
        }
        observations.add("faction=" + snapshot.faction());
        observations.add("ownSquad=" + snapshot.ownSquad());
        observations.add("squadLeader=" + snapshot.squadLeader());
        observations.add("commander=" + snapshot.commander());
        observations.add("finalActiveMarkers=" + ClientBattleState.activeMarkers().size());
        observations.add("framebuffer=" + minecraft.getWindow().getWidth() + "x"
                + minecraft.getWindow().getHeight());
        transition(Phase.FINISH);
    }

    private static void finish(Minecraft minecraft, boolean success) throws IOException {
        if (!cleanupFinished) {
            queueFixtureCleanup(minecraft);
            return;
        }
        if (!resultWritten) {
            writeResult(minecraft, success);
            resultWritten = true;
            WokInfantryMod.LOGGER.info("[UI ACCEPTANCE] {}: {}",
                    success ? "PASS" : "FAIL", success ? "all captures written" : failureReason);
            phaseTicks = 0;
            return;
        }
        if (phaseTicks >= 20) {
            phase = Phase.STOPPED;
            minecraft.stop();
        }
    }

    private static void queueFixtureCleanup(Minecraft minecraft) {
        if (cleanupQueued) {
            return;
        }
        cleanupQueued = true;
        MinecraftServer server = minecraft.getSingleplayerServer();
        java.util.UUID playerId = minecraft.player == null ? null : minecraft.player.getUUID();
        if (server == null || playerId == null) {
            cleanupFinished = true;
            return;
        }
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                if (player.getInventory().getItem(6)
                        .is(InfantryItems.DEPLOYMENT_BEACON.get())) {
                    player.getInventory().setItem(6, ItemStack.EMPTY);
                }
                if (player.getInventory().getItem(7).is(Items.BLUE_DYE)
                        || player.getInventory().getItem(7).is(Items.RED_DYE)) {
                    player.getInventory().setItem(7, ItemStack.EMPTY);
                }
                if (administratorToolTemporaryOperator
                        && server.getPlayerList().isOp(player.getGameProfile())) {
                    server.getPlayerList().deop(player.getGameProfile());
                }
                player.getInventory().setChanged();
                player.inventoryMenu.broadcastChanges();
            }
            administratorToolTemporaryOperator = false;
            cleanupFinished = true;
        });
    }

    private static void writeResult(Minecraft minecraft, boolean success) throws IOException {
        Path resultDirectory = minecraft.gameDirectory.toPath().resolve("ui-test-results");
        Files.createDirectories(resultDirectory);
        Path result = resultDirectory.resolve("wok_ui_acceptance.txt");
        List<String> lines = new ArrayList<>();
        lines.add("status=" + (success ? "PASS" : "FAIL"));
        lines.add("autoDeploymentObserved=" + autoDeploymentObserved);
        lines.add("totalTicks=" + totalTicks);
        if (!success) {
            lines.add("failure=" + Objects.requireNonNullElse(failureReason, "unknown"));
        }
        lines.addAll(observations);
        Files.write(result, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private static BattleSnapshot requireSnapshot() {
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        if (snapshot == null) {
            fail("Authoritative battle snapshot disappeared" + feedbackSuffix());
        }
        return snapshot;
    }

    private static DeploymentPoint fixtureDeploymentPoint(BattleSnapshot snapshot) {
        java.util.UUID expectedId = baseFixturePointId;
        if (snapshot == null || expectedId == null) {
            return null;
        }
        return snapshot.deployment().points().stream()
                .filter(point -> expectedId.equals(point.id()))
                .findFirst().orElse(null);
    }

    private static void setGuiScale(Minecraft minecraft, int scale) {
        minecraft.options.guiScale().set(scale);
        minecraft.resizeDisplay();
    }

    private static void clickBoundKey(int glfwKey) {
        KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(glfwKey));
    }

    private static String logicalSize(Minecraft minecraft) {
        return minecraft.getWindow().getGuiScaledWidth() + "x"
                + minecraft.getWindow().getGuiScaledHeight();
    }

    private static String feedbackSuffix() {
        ClientBattleState.BattleFeedback feedback = ClientBattleState.feedback();
        return feedback == null ? "" : "; server feedback=" + feedback.message();
    }

    private static void transition(Phase next) {
        WokInfantryMod.LOGGER.info("[UI ACCEPTANCE] {} -> {}", phase, next);
        phase = next;
        phaseTicks = 0;
        pendingCapture = null;
        renderedCapture = null;
        waitingForCapture = null;
    }

    private static void fail(String reason) {
        if (phase == Phase.FAIL || phase == Phase.STOPPED) {
            return;
        }
        failureReason = reason;
        WokInfantryMod.LOGGER.error("[UI ACCEPTANCE] FAIL in {}: {}", phase, reason);
        phase = Phase.FAIL;
        phaseTicks = 0;
        pendingCapture = null;
        renderedCapture = null;
        waitingForCapture = null;
    }

    private record PendingCapture(String fileName, Screen screen) {
    }

    private record TerrainCoverage(int loaded, int desired) {
    }

    private enum Phase {
        WAIT_FOR_LOGIN,
        CAPTURE_DEPLOYMENT,
        OPEN_COMPACT_SQUADS,
        CAPTURE_COMPACT_SQUADS,
        OPEN_COMPACT_CLASSES,
        CAPTURE_COMPACT_CLASSES,
        OPEN_COMPACT_LOADOUT,
        CAPTURE_COMPACT_LOADOUT,
        ENSURE_LEADER,
        CLAIM_COMMANDER,
        WAIT_FOR_COMMANDER_FEEDBACK_CLEAR,
        OPEN_COMPACT_COMMANDER_SQUADS,
        CAPTURE_COMPACT_COMMANDER_SQUADS,
        SET_BASE,
        SELECT_DEPLOYMENT,
        DEPLOY,
        WAIT_FOR_ACTIVE,
        VERIFY_ADMINISTRATOR_TOOLS,
        CREATE_MARKERS,
        OPEN_COMPACT_MAP,
        CAPTURE_COMPACT_MAP,
        WAIT_FOR_FEEDBACK_CLEAR,
        OPEN_LARGE_SQUADS,
        CAPTURE_LARGE_SQUADS,
        OPEN_LARGE_LOADOUT,
        CAPTURE_LARGE_LOADOUT,
        OPEN_LARGE_MAP,
        CAPTURE_LARGE_MAP,
        OPEN_COMPACT_FORMATION,
        CAPTURE_COMPACT_FORMATION,
        OPEN_LARGE_FORMATION,
        CAPTURE_LARGE_FORMATION,
        OPEN_COMPACT_ADMIN_LOADOUT,
        CAPTURE_COMPACT_ADMIN_LOADOUT,
        OPEN_COMPACT_ADMIN_CLASS_SETTINGS,
        CAPTURE_COMPACT_ADMIN_CLASS_SETTINGS,
        OPEN_LARGE_ADMIN_LOADOUT,
        CAPTURE_LARGE_ADMIN_LOADOUT,
        WAIT_FOR_FILES,
        FINISH,
        FAIL,
        STOPPED
    }
}
