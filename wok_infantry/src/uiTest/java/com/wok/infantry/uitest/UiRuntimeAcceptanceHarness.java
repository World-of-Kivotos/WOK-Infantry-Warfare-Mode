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
import com.wok.infantry.client.map.TacticalMapIcons;
import com.wok.infantry.client.map.TacticalMapTerrainRequest;
import com.wok.infantry.client.map.TacticalMapTerrainRegistry;
import com.wok.infantry.client.map.TacticalSupportMapPresentationRegistry;
import com.wok.infantry.client.screen.PlayerLoadoutScreen;
import com.wok.infantry.client.screen.SquadScreen;
import com.wok.infantry.client.screen.TacticalMapScreen;
import com.wok.infantry.client.ui.probe.UiLayoutFrame;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import com.wok.infantry.client.ui.probe.UiLayoutReport;
import com.wok.infantry.config.InfantryClientConfig;
import com.wok.infantry.integration.journeymap.JourneyMapUiPolicy;
import com.wok.infantry.deployment.DeploymentPoint;
import com.wok.infantry.deployment.DeploymentPhase;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.deployment.KitProvenance;
import com.wok.infantry.loadout.LoadoutSlot;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.battle.BattleOpenTarget;
import com.wok.infantry.network.formation.FormationNetwork;
import com.wok.infantry.registry.InfantryItems;
import com.wok.infantry.server.FormationService;
import com.wok.infantry.support.adapter.SupportIntelContact;
import com.wok.infantry.support.SupportMissionView;
import com.wok.infantry.support.SupportView;
import com.wok.infantry.uitest.cases.UiCaseCatalog;
import com.wok.infantry.uitest.fixtures.DeploymentFixtures;
import com.wok.infantry.uitest.fixtures.ServerFixtures;
import com.wok.infantry.uitest.fixtures.SupportFixtures;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
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
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Development-only, real-client acceptance harness for the tactical UI.
 *
 * <p>The class lives in {@code src/uiTest}; default builds and the production JAR cannot see it.
 * It enters an integrated world and runs the live flow (deployment screen after login, the battle
 * terminal and tactical map opened through their real key bindings and network packets, squad and
 * commander actions, deployment, markers, JourneyMap terrain), capturing the 14 baseline
 * screenshots. It then hands over to {@link UiCaseRunner}, which runs the cases of
 * {@link UiCaseCatalog} — the legacy formation and administrator captures first, then every
 * surface case — on each tier, with the layout probe ({@link UiLayoutProbe}) checking every frame.
 * Finally it writes {@code wok_ui_acceptance.txt} (first line {@code status=PASS|FAIL}), a progress
 * file, {@code wok_ui_layout.json}, {@code wok_ui_manifest.json} and {@code index.html}, and exits.
 *
 * <p>System properties (set by {@code build.gradle}): {@code wok.ui.cases} selects cases by group,
 * surface or id (empty: all; without {@code legacy} the live flow is skipped);
 * {@code wok.ui.tiers} limits tiers ({@code 320x240,960x720,…}); {@code wok.ui.layoutStrict=false}
 * reports layout violations of migrated surfaces without failing the run.
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
    /** Budget of the live flow; the cases add their own ({@link UiCaseRunner#budgetTicks}). */
    private static final int LIVE_FLOW_TIMEOUT_TICKS = 2_400;
    private static final int CASES_ONLY_BASE_TICKS = 900;
    private static final int PHASE_TIMEOUT_TICKS = 400;
    private static final int SCREEN_SETTLE_TICKS = UiCaseRunner.SETTLE_TICKS;
    private static final String EXPECTED_TERRAIN_PROVIDER = System.getProperty(
            "wok.ui.expectedTerrainProvider", "journeymap").trim()
            .toLowerCase(Locale.ROOT);
    private static final String EXPECTED_JOURNEYMAP_LOADED_RAW = System.getProperty(
            "wok.ui.expectedJourneyMapLoaded", "true").trim().toLowerCase(Locale.ROOT);
    private static final boolean EXPECTED_JOURNEYMAP_LOADED =
            EXPECTED_JOURNEYMAP_LOADED_RAW.equals("true");
    /** Set by build.gradle from -PuiLang; empty means "do not assert the language". */
    private static final String EXPECTED_LANGUAGE = System.getProperty(
            "wok.ui.expectedLanguage", "").trim().toLowerCase(Locale.ROOT);
    /** -PuiCases: groups, surfaces or case ids (comma separated); empty runs everything. */
    private static final String CASE_FILTER = System.getProperty("wok.ui.cases", "").trim();
    /** -PuiTiers: tier ids such as 320x240,960x720; empty keeps each case's tiers. */
    private static final String TIER_FILTER = System.getProperty("wok.ui.tiers", "").trim();
    /** -PuiLayoutStrict=false reports violations of migrated surfaces without failing. */
    private static final boolean LAYOUT_STRICT = !"false".equalsIgnoreCase(
            System.getProperty("wok.ui.layoutStrict", "true").trim());
    /** Directory of the layout preview's PNGs for index.html, if found by build.gradle. */
    private static final String PREVIEW_SHOTS = System.getProperty("wok.ui.previewShots", "")
            .trim();
    private static final String MAP_KEY_MAPPING = "key.wok_infantry.open_tactical_map";
    private static final String SQUAD_KEY_MAPPING = "key.wok_infantry.open_squad";
    private static final String RESULT_DIRECTORY = "ui-test-results";
    private static final String PROGRESS_FILE = "wok_ui_progress.txt";
    private static final int PROGRESS_HISTORY_LIMIT = 200;

    private static final Map<String, Long> screenshotBaselines = new LinkedHashMap<>();
    private static final List<String> observations = new ArrayList<>();
    private static final List<UiCaseResult> liveCaptures = new ArrayList<>();

    private static Phase phase = Phase.WAIT_FOR_LOGIN;
    private static int totalTicks;
    private static int phaseTicks;
    private static int globalTimeoutTicks = LIVE_FLOW_TIMEOUT_TICKS;
    private static boolean runLiveFlow = true;
    private static List<UiCase> selectedCases = List.of();
    private static UiCaseRunner caseRunner;
    private static String lastRunnerPosition = "";
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
    private static volatile String temporaryOperatorCleanup;
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
    private static final List<String> progressHistory = new ArrayList<>();
    private static boolean progressWriteFailed;

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
        if (totalTicks > globalTimeoutTicks && phase != Phase.FINISH
                && phase != Phase.FAIL) {
            fail("Global timeout in phase " + phase
                    + (caseRunner == null ? "" : " at " + caseRunner.position()));
        }
        if (phaseTicks > PHASE_TIMEOUT_TICKS && phase != Phase.WAIT_FOR_LOGIN
                && phase != Phase.RUN_CASES && phase != Phase.WAIT_FOR_FILES
                && phase != Phase.FINISH && phase != Phase.FAIL) {
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
                    SCREENSHOTS[5], Phase.RUN_CASES);
            case RUN_CASES -> runCases(minecraft);
            case WAIT_FOR_FILES -> waitForFiles(minecraft);
            case FINISH -> finish(minecraft, true);
            case FAIL -> finish(minecraft, false);
            case STOPPED -> {
            }
        }
    }

    private static void initializeArtifacts(Minecraft minecraft) throws IOException {
        initialized = true;
        UiLayoutProbe.enable();
        // Automated windows are intentionally not focused. Keep the integrated server ticking
        // after deployment/respawn packets close or replace screens, matching the dedicated
        // network harness and a real multiplayer server.
        minecraft.options.pauseOnLostFocus = false;
        runLiveFlow = CASE_FILTER.isEmpty() || List.of(CASE_FILTER.split(UiTier.LIST_SEPARATORS))
                .stream().map(String::trim).anyMatch(UiCase.LEGACY_GROUP::equals);
        List<UiCase> cases = new ArrayList<>();
        if (runLiveFlow) {
            cases.addAll(UiCaseCatalog.legacy());
        }
        for (UiCase uiCase : UiCaseCatalog.surfaces()) {
            if (uiCase.selectedBy(CASE_FILTER)) {
                cases.add(uiCase);
            }
        }
        selectedCases = List.copyOf(cases);
        globalTimeoutTicks = (runLiveFlow ? LIVE_FLOW_TIMEOUT_TICKS : CASES_ONLY_BASE_TICKS)
                + UiCaseRunner.budgetTicks(selectedCases, TIER_FILTER);

        Path screenshots = minecraft.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(screenshots);
        List<String> expectedFiles = new ArrayList<>(List.of(SCREENSHOTS));
        for (UiCase uiCase : selectedCases) {
            // Legacy cases ignore the tier filter (see UiCase.selectedTiers): the live flow always
            // waits for all 14 baseline screenshots.
            for (UiTier tier : uiCase.selectedTiers(TIER_FILTER)) {
                expectedFiles.add(uiCase.fileName(tier));
            }
        }
        for (String fileName : expectedFiles) {
            Path file = screenshots.resolve(fileName);
            screenshotBaselines.put(fileName, Files.isRegularFile(file)
                    ? Files.getLastModifiedTime(file).toMillis() : -1L);
        }
        observations.add("harness=src/uiTest (excluded from production JAR)");
        observations.add("uiCases=" + (CASE_FILTER.isEmpty() ? "all" : CASE_FILTER)
                + " liveFlow=" + runLiveFlow + " cases=" + selectedCases.size());
        observations.add("uiTiers=" + (TIER_FILTER.isEmpty() ? "per-case" : TIER_FILTER)
                + " layoutStrict=" + LAYOUT_STRICT);
        observations.add("globalTimeoutTicks=" + globalTimeoutTicks);
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
        // options.txt is read before the first tick, so this is the code prepareUiTestOptions
        // wrote. Whether a resource pack actually provides it is checked after login, once the
        // resource reload has finished (see languageAvailable in waitForLogin).
        String language = minecraft.getLanguageManager().getSelected();
        observations.add("languageCode=" + language + " expected="
                + (EXPECTED_LANGUAGE.isEmpty() ? "any" : EXPECTED_LANGUAGE));
        if (!EXPECTED_LANGUAGE.isEmpty() && !EXPECTED_LANGUAGE.equals(language)) {
            fail("UI acceptance language mismatch: expected " + EXPECTED_LANGUAGE
                    + ", got " + language);
        }
        recordProgress("armed " + phase);
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
                // visibly stable — on its deployment page, as the server opened it.
                minecraft.setScreen(new SquadScreen(null, true));
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
        // Minecraft keeps an unknown lang code selected but silently renders English, so a
        // mistyped -PuiLang would otherwise produce English captures labelled as another language.
        String language = minecraft.getLanguageManager().getSelected();
        if (minecraft.getLanguageManager().getLanguage(language) == null) {
            fail("UI acceptance language " + language
                    + " is not provided by any loaded resource pack");
            return;
        }
        observations.add("languageAvailable=" + language);
        autoDeploymentObserved = true;
        minecraft.getTutorial().setStep(TutorialSteps.NONE);
        minecraft.getToasts().clear();
        minecraft.getWindow().setWindowed(960, 720);
        minecraft.resizeDisplay();
        setGuiScale(minecraft, 3);
        observations.add("autoDeploymentScreen=true");
        observations.add("compactLogicalSize=" + logicalSize(minecraft));
        transition(runLiveFlow ? Phase.CAPTURE_DEPLOYMENT : Phase.RUN_CASES);
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
            if (!pressMappedKey(minecraft, SQUAD_KEY_MAPPING, "squad", true)) {
                return;
            }
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
            // Real mouse click on the class tab of the terminal strip, found by its probe id
            // ("terminal.tabs", tab "classes") instead of computed coordinates.
            AbstractWidget strip = UiFind.widget(squadScreen,
                    com.wok.infantry.client.screen.BattleTab.TERMINAL_TABS_UI_ID).orElse(null);
            if (!(strip instanceof com.wok.infantry.client.screen.TacticalTabStrip tabs)) {
                fail("The squad terminal has no tab strip ("
                        + com.wok.infantry.client.screen.BattleTab.TERMINAL_TABS_UI_ID + ")");
                return;
            }
            com.wok.infantry.client.screen.UiRect tab = tabs.tabBounds(
                    tabs.indexOf(com.wok.infantry.client.screen.BattleTab.CLASSES.id()));
            if (tab == null || tab.isEmpty()) {
                fail("The class tab is not shown in the terminal strip");
                return;
            }
            UiInputDriver.clickLayout(minecraft, tab.left() + tab.width() / 2.0D,
                    tab.top() + tab.height() / 2.0D);
            if (!(minecraft.screen instanceof SquadScreen clickedScreen)
                    || clickedScreen.page() != com.wok.infantry.client.screen.BattleTab.CLASSES) {
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
                        DeploymentFixtures.describe(server, point));
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
                        DeploymentFixtures.seedInventory(server, playerId));
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
                        DeploymentFixtures.verifyInventoryPolicy(server, playerId));
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
            BlockPos spawn = DeploymentFixtures.prepareSafeBase(overworld);
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
            if (!pressMappedKey(minecraft, MAP_KEY_MAPPING, "map", false)) {
                return;
            }
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
            if (!supportButtonPresent(tacticalMapScreen, SupportFixtures.JDAM_ID)) {
                fail("Compact tactical map did not render the F-15EX JDAM support button");
                return;
            }
            if (!supportButtonPresent(tacticalMapScreen, SupportFixtures.PAVEWAY_ID)) {
                fail("Compact tactical map did not render the F-16C Paveway support button");
                return;
            }
            if (!pavewayGuidanceFixtureReady(snapshot)) {
                fail("Compact tactical map has no executing F-16C call with a "
                        + SupportFixtures.PAVEWAY_GUIDANCE_RADIUS + "m designation zone");
                return;
            }
            compactSupportModeSelected = true;
            observations.add("compactSupportOptions=3");
            observations.add("compactJdamSupportVisible=true");
            observations.add("compactPavewaySupportVisible=true");
            observations.add("compactPavewayGuidanceZone=R"
                    + (int) SupportFixtures.PAVEWAY_GUIDANCE_RADIUS + "/R"
                    + (int) SupportFixtures.PAVEWAY_RADIUS);
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
            if (!supportButtonPresent(tacticalMapScreen, SupportFixtures.JDAM_ID)) {
                fail("Large tactical map did not render the F-15EX JDAM support button");
                return;
            }
            if (!supportButtonPresent(tacticalMapScreen, SupportFixtures.PAVEWAY_ID)) {
                fail("Large tactical map did not render the F-16C Paveway support button");
                return;
            }
            if (!pavewayGuidanceFixtureReady(ClientBattleState.snapshot())) {
                fail("Large tactical map has no executing F-16C call with a "
                        + SupportFixtures.PAVEWAY_GUIDANCE_RADIUS + "m designation zone");
                return;
            }
            observations.add("largeJdamSupportVisible=true");
            observations.add("largePavewaySupportVisible=true");
            observations.add("largePavewayGuidanceZone=R"
                    + (int) SupportFixtures.PAVEWAY_GUIDANCE_RADIUS + "/R"
                    + (int) SupportFixtures.PAVEWAY_RADIUS);
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
        SupportFixtures.registerPresentations();
        boolean largeMap = phase == Phase.OPEN_LARGE_MAP || phase == Phase.CAPTURE_LARGE_MAP;
        if (supportFixtureInstalled(snapshot, largeMap)) {
            return;
        }
        FixtureAnchor anchor = fixtureAnchor(minecraft, snapshot);
        if (anchor == null) {
            return;
        }
        // A 320x240 map cannot hold two mission cards beside an R80 area, so the compact capture
        // keeps the JDAM call active on another dimension (its button and the active count are
        // unchanged) and leaves the F-16C zone alone on the map. In each capture the offsets keep
        // the F-16C rings and labels clear of the JDAM card, the fixed test markers, the scale
        // bar and the compass.
        long tick = snapshot.support().serverGameTick();
        SupportMissionView activeJdam = SupportFixtures.jdamMission(
                largeMap ? anchor.dimension() : Level.NETHER.location(),
                anchor.x() - 20.0D, anchor.z() - 80.0D, tick);
        SupportMissionView executingPaveway = SupportFixtures.pavewayMission(anchor.dimension(),
                anchor.x() + (largeMap ? 0.0D : -48.0D),
                anchor.z() + (largeMap ? 320.0D : -24.0D), tick);
        SupportView support = new SupportView(SupportFixtures.options(),
                List.of(activeJdam, executingPaveway), tick,
                snapshot.support().structuralRevision() + 1L, true, "");
        ClientBattleState.update(snapshot.withSupport(support));
        if (minecraft.screen instanceof TacticalMapScreen tacticalMapScreen) {
            tacticalMapScreen.resize(minecraft, tacticalMapScreen.width,
                    tacticalMapScreen.height);
        }
    }

    /**
     * The support fixture matches the current map when the JDAM call sits on the map dimension
     * for the large capture and off it for the compact one; a server snapshot that replaced the
     * support view, or a switch between the two map sizes, installs it again.
     */
    private static boolean supportFixtureInstalled(BattleSnapshot snapshot, boolean largeMap) {
        if (snapshot.support().options().stream()
                .noneMatch(option -> option.id().equals(SupportFixtures.JDAM_ID))) {
            return false;
        }
        return snapshot.support().activeMissions().stream()
                .filter(mission -> mission.supportId().equals(SupportFixtures.JDAM_ID))
                .anyMatch(mission -> mission.dimension().equals(Level.NETHER.location())
                        != largeMap);
    }

    /** Both acceptance maps are centred on the commander standing at the deployment base. */
    private static FixtureAnchor fixtureAnchor(Minecraft minecraft, BattleSnapshot snapshot) {
        DeploymentPoint point = fixtureDeploymentPoint(snapshot);
        if (point != null) {
            return new FixtureAnchor(point.dimension(), point.position().getX() + 0.5D,
                    point.position().getZ() + 0.5D);
        }
        if (minecraft.player != null && minecraft.level != null) {
            return new FixtureAnchor(minecraft.level.dimension().location(),
                    minecraft.player.getX(), minecraft.player.getZ());
        }
        return null;
    }

    private static boolean pavewayGuidanceFixtureReady(BattleSnapshot snapshot) {
        if (snapshot == null || TacticalSupportMapPresentationRegistry.guidanceRadius(
                        SupportFixtures.PAVEWAY_ID).orElse(0.0D)
                != SupportFixtures.PAVEWAY_GUIDANCE_RADIUS) {
            return false;
        }
        boolean outerRadiusMatches = snapshot.support().options().stream()
                .anyMatch(option -> option.id().equals(SupportFixtures.PAVEWAY_ID)
                        && option.radius() == SupportFixtures.PAVEWAY_RADIUS);
        return outerRadiusMatches && snapshot.support().activeMissions().stream()
                .anyMatch(mission -> mission.supportId().equals(SupportFixtures.PAVEWAY_ID));
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

    // ---- cases ----------------------------------------------------------------------------------

    /** Runs the selected cases (legacy formation/administrator captures first, then surfaces). */
    private static void runCases(Minecraft minecraft) {
        if (caseRunner == null) {
            caseRunner = new UiCaseRunner(selectedCases, TIER_FILTER,
                    minecraft.getLanguageManager().getSelected(), LAYOUT_STRICT,
                    observations::add);
            WokInfantryMod.LOGGER.info("[UI ACCEPTANCE] Running {} case(s)", selectedCases.size());
        }
        boolean done = caseRunner.tick(minecraft);
        String position = caseRunner.position();
        if (!position.equals(lastRunnerPosition)) {
            lastRunnerPosition = position;
            recordProgress("case " + position);
        }
        if (done) {
            transition(Phase.WAIT_FOR_FILES);
        }
    }

    // ---- captures -------------------------------------------------------------------------------

    private static void captureScreen(Minecraft minecraft, Class<? extends Screen> expected,
                                      String fileName, Phase next) throws IOException {
        // A dimension sync may briefly replace the visible screen after the render callback has
        // already captured the expected framebuffer. Let the asynchronous PNG write finish before
        // evaluating the next live screen.
        if (fileName.equals(waitingForCapture)) {
            if (fileUpdated(minecraft, fileName)) {
                String saved = UiCapture.saveMessage(fileName);
                observations.add(fileName + "=" + (saved.isEmpty() ? "written" : saved));
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
        if (waitingForCapture == null && !UiCapture.busy()
                && phaseTicks >= SCREEN_SETTLE_TICKS) {
            waitingForCapture = fileName;
            String language = minecraft.getLanguageManager().getSelected();
            UiCapture.arm(fileName, minecraft.screen, result -> {
                if (result.screen() instanceof TacticalMapScreen) {
                    checkMapIcons(fileName, result);
                }
                liveCaptures.add(liveResult(fileName, result, language));
            });
        }
    }

    /**
     * The live map captures must draw the Squad-style marker icons (0.4.0-beta.3): records every
     * kind drawn at the map size of the current icon knob (size scheme B,
     * {@link TacticalMapIcons#mapArtPx}) and fails when none is. Marker-tool keys may draw their
     * icons smaller; they are counted apart.
     */
    private static void checkMapIcons(String fileName, UiCapture.Result capture) {
        int mapArt = TacticalMapIcons.mapArtPx(InfantryClientConfig.markerScale(),
                capture.guiScale());
        Map<String, Integer> mapSized = new TreeMap<>();
        int otherSize = 0;
        for (UiLayoutFrame.Icon icon : capture.frame().icons()) {
            TacticalMapIcons.MapIcon marker = TacticalMapIcons.MapIcon.valueOf(
                    icon.id().toUpperCase(Locale.ROOT));
            if (icon.physicalWidth() == marker.plate().width() * mapArt
                    && icon.physicalHeight() == marker.plate().height() * mapArt) {
                mapSized.merge(icon.id(), 1, Integer::sum);
            } else {
                otherSize++;
            }
        }
        observations.add("mapIcons[" + fileName + "]=" + mapSized + " artPx=" + mapArt
                + " plate=" + 15 * mapArt + "px guiScale=" + capture.guiScale()
                + " otherSize=" + otherSize);
        if (mapSized.isEmpty()) {
            fail("The tactical map capture " + fileName + " drew no Squad-style marker icon at "
                    + mapArt + " physical px per art px");
        }
    }

    /** A live-flow capture: probed and reported, never failing the run on layout. */
    private static UiCaseResult liveResult(String fileName, UiCapture.Result capture,
                                           String language) {
        String stem = fileName.substring(0, fileName.length() - ".png".length());
        String tier = stem.substring(stem.lastIndexOf('_') + 1);
        return new UiCaseResult("legacy." + stem.substring("wok_ui_".length()), "legacy",
                stem.substring("wok_ui_".length()), "legacy", tier, tier, fileName, false, false,
                false, capture.screen().getClass().getSimpleName(), capture.layoutWidth(),
                capture.layoutHeight(), capture.baseScale(), capture.guiScale(), capture.frame(),
                UiLayoutReport.check(capture.frame(), UiLayoutReport.Options.of(language)), null);
    }

    private static boolean fileUpdated(Minecraft minecraft, String fileName) throws IOException {
        Path file = minecraft.gameDirectory.toPath().resolve("screenshots").resolve(fileName);
        if (!Files.isRegularFile(file) || Files.size(file) <= 0L) {
            return false;
        }
        long baseline = screenshotBaselines.getOrDefault(fileName, -1L);
        return Files.getLastModifiedTime(file).toMillis() > baseline;
    }

    /**
     * Vanilla-style buttons of the live screens must fit their label. WOK keys
     * (TacticalBoardButton, BattleUiButton) ellipsize and offer the full label as a tooltip; the
     * layout probe checks those instead (control-truncated-no-tip, control-text-overflow).
     */
    private static boolean buttonsFit(Minecraft minecraft, Screen screen) {
        for (Button button : screen.children().stream()
                .filter(Button.class::isInstance).map(Button.class::cast).toList()) {
            if (!button.visible) {
                continue;
            }
            String type = button.getClass().getSimpleName();
            if (type.equals("TacticalBoardButton") || type.equals("BattleUiButton")) {
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
        if (runLiveFlow) {
            for (String fileName : SCREENSHOTS) {
                if (!fileUpdated(minecraft, fileName)) {
                    return;
                }
            }
        }
        List<UiCaseResult> caseResults = caseRunner == null ? List.of() : caseRunner.results();
        for (UiCaseResult result : caseResults) {
            if (!result.fileName().isEmpty() && !fileUpdated(minecraft, result.fileName())) {
                return;
            }
        }
        for (UiCaseResult result : caseResults) {
            if (!result.fileName().isEmpty()) {
                String saved = UiCapture.saveMessage(result.fileName());
                observations.add(result.fileName() + "=" + (saved.isEmpty() ? "written" : saved));
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
        List<UiCaseResult> failures = caseRunner == null ? List.of() : caseRunner.failures();
        if (!failures.isEmpty()) {
            fail("UI case(s) failed: " + String.join("; ", failures.stream()
                    .map(result -> result.caseId() + "@" + result.tier()
                            + (result.failure() != null ? " " + result.failure()
                            : " " + result.violations().size() + " layout violation(s)"))
                    .toList()));
            return;
        }
        transition(Phase.FINISH);
    }

    private static void finish(Minecraft minecraft, boolean success) throws IOException {
        if (!cleanupFinished) {
            queueFixtureCleanup(minecraft);
            return;
        }
        if (!resultWritten) {
            if (temporaryOperatorCleanup != null) {
                observations.add("temporaryOperatorCleanup=" + temporaryOperatorCleanup);
            }
            writeResult(minecraft, success);
            resultWritten = true;
            recordProgress("result=" + (success ? "PASS" : "FAIL"));
            WokInfantryMod.LOGGER.info("[UI ACCEPTANCE] {}: {}",
                    success ? "PASS" : "FAIL", success ? "all captures written" : failureReason);
            phaseTicks = 0;
            return;
        }
        if (phaseTicks >= 20) {
            phase = Phase.STOPPED;
            UiLayoutProbe.disable();
            minecraft.stop();
        }
    }

    private static void queueFixtureCleanup(Minecraft minecraft) {
        if (cleanupQueued) {
            return;
        }
        cleanupQueued = true;
        UiCapture.cancel();
        MinecraftServer server = minecraft.getSingleplayerServer();
        java.util.UUID playerId = minecraft.player == null ? null : minecraft.player.getUUID();
        if (server == null || playerId == null) {
            cleanupFinished = true;
            return;
        }
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            boolean temporaryOperator = administratorToolTemporaryOperator
                    || ServerFixtures.temporaryOperatorGranted();
            if (player != null) {
                if (player.getInventory().getItem(6)
                        .is(InfantryItems.DEPLOYMENT_BEACON.get())) {
                    player.getInventory().setItem(6, ItemStack.EMPTY);
                }
                if (player.getInventory().getItem(7).is(Items.BLUE_DYE)
                        || player.getInventory().getItem(7).is(Items.RED_DYE)) {
                    player.getInventory().setItem(7, ItemStack.EMPTY);
                }
                // Every OP grant the fixture made (beacon tools, formation administrator view)
                // is revoked here; a player who was already an operator keeps the status.
                boolean revoked = false;
                if (temporaryOperator
                        && server.getPlayerList().isOp(player.getGameProfile())) {
                    server.getPlayerList().deop(player.getGameProfile());
                    revoked = true;
                }
                temporaryOperatorCleanup = !temporaryOperator ? "none"
                        : revoked ? "revoked" : "alreadyRevoked";
                player.getInventory().setChanged();
                player.inventoryMenu.broadcastChanges();
            }
            administratorToolTemporaryOperator = false;
            ServerFixtures.clearTemporaryOperator();
            cleanupFinished = true;
        });
    }

    private static void writeResult(Minecraft minecraft, boolean success) throws IOException {
        Path resultDirectory = minecraft.gameDirectory.toPath().resolve(RESULT_DIRECTORY);
        Files.createDirectories(resultDirectory);
        Path result = resultDirectory.resolve("wok_ui_acceptance.txt");
        List<UiCaseResult> captures = new ArrayList<>(liveCaptures);
        if (caseRunner != null) {
            captures.addAll(caseRunner.results());
        }
        int violations = 0;
        int strictViolations = 0;
        for (UiCaseResult capture : captures) {
            violations += capture.violations().size();
            if (capture.strict()) {
                strictViolations += capture.violations().size();
            }
        }
        List<String> lines = new ArrayList<>();
        lines.add("status=" + (success ? "PASS" : "FAIL"));
        lines.add("autoDeploymentObserved=" + autoDeploymentObserved);
        lines.add("totalTicks=" + totalTicks);
        if (!success) {
            lines.add("failure=" + Objects.requireNonNullElse(failureReason, "unknown"));
        }
        lines.add("captures=" + captures.size() + " layoutViolations=" + violations
                + " strictLayoutViolations=" + strictViolations);
        lines.addAll(observations);
        Files.write(result, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        try {
            UiResultWriter.write(resultDirectory, captures, success ? "PASS" : "FAIL",
                    minecraft.getLanguageManager().getSelected(),
                    PREVIEW_SHOTS.isEmpty() ? null : Path.of(PREVIEW_SHOTS));
        } catch (IOException | RuntimeException exception) {
            WokInfantryMod.LOGGER.warn("[UI ACCEPTANCE] Could not write the layout report",
                    exception);
        }
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

    /**
     * Presses the key a WOK mapping is bound to (see {@link UiInputDriver#clickMappedKey}) and
     * records the mapping, its key and its conflicts; fails the run when it cannot be pressed.
     */
    private static boolean pressMappedKey(Minecraft minecraft, String mappingName, String label,
                                          boolean terminalFallback) {
        UiInputDriver.KeyPress press = UiInputDriver.clickMappedKey(minecraft, mappingName,
                terminalFallback);
        if (!press.ok()) {
            fail(press.failure());
            return false;
        }
        observations.add(label + "KeyMapping=" + press.describe());
        return true;
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
        Phase previous = phase;
        phase = next;
        phaseTicks = 0;
        waitingForCapture = null;
        recordProgress(previous + " -> " + next);
    }

    private static void fail(String reason) {
        if (phase == Phase.FAIL || phase == Phase.STOPPED) {
            return;
        }
        failureReason = reason;
        WokInfantryMod.LOGGER.error("[UI ACCEPTANCE] FAIL in {}: {}", phase, reason);
        Phase failedPhase = phase;
        phase = Phase.FAIL;
        phaseTicks = 0;
        UiCapture.cancel();
        waitingForCapture = null;
        recordProgress("FAIL in " + failedPhase);
    }

    /**
     * Rewrites {@code ui-test-results/wok_ui_progress.txt}. If the client exits before
     * {@link #writeResult} runs, Gradle reports the last phase from this file.
     */
    private static void recordProgress(String event) {
        if (progressHistory.size() >= PROGRESS_HISTORY_LIMIT) {
            progressHistory.remove(0);
        }
        progressHistory.add("t=" + totalTicks + " " + event);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.gameDirectory == null) {
            return;
        }
        List<String> lines = new ArrayList<>();
        lines.add("phase=" + phase);
        lines.add("totalTicks=" + totalTicks);
        lines.add("screen=" + (minecraft.screen == null ? "world"
                : minecraft.screen.getClass().getSimpleName()));
        if (caseRunner != null) {
            lines.add("case=" + caseRunner.position());
        }
        lines.add("updatedAt=" + Instant.now());
        if (failureReason != null) {
            lines.add("failure=" + failureReason);
        }
        lines.add("history:");
        lines.addAll(progressHistory);
        try {
            Path directory = minecraft.gameDirectory.toPath().resolve(RESULT_DIRECTORY);
            Files.createDirectories(directory);
            Files.write(directory.resolve(PROGRESS_FILE), lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException | RuntimeException exception) {
            if (!progressWriteFailed) {
                progressWriteFailed = true;
                WokInfantryMod.LOGGER.warn("[UI ACCEPTANCE] Could not write progress file",
                        exception);
            }
        }
    }

    private record FixtureAnchor(ResourceLocation dimension, double x, double z) {
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
        /** Legacy formation/administrator captures and every surface case. */
        RUN_CASES,
        WAIT_FOR_FILES,
        FINISH,
        FAIL,
        STOPPED
    }
}
