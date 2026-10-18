package com.wok.infantry.networktest;

import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.MemberPosition;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.PlayerRecord;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.SquadView;
import com.wok.infantry.battle.TacticalMarker;
import com.wok.infantry.battle.TacticalMarkerType;
import com.wok.infantry.client.BattleClientActions;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.deployment.DeploymentPoint;
import com.wok.infantry.deployment.DeploymentService;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.network.battle.BattleNetwork;
import com.wok.infantry.network.formation.client.FormationClientNetworkBridge;
import com.wok.infantry.server.FormationService;
import io.netty.channel.ChannelFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConnectionListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Development-only three-process Forge loopback acceptance harness.
 *
 * <p>This source lives in {@code src/networkTest}; neither the normal runtime classpath nor the
 * production JAR contains it. The host opens an IPv4 loopback listener directly and deliberately
 * does not call {@code IntegratedServer.publishServer}, which would bind to a wildcard address and
 * advertise the offline fixture on the LAN.</p>
 */
@Mod.EventBusSubscriber(modid = WokInfantryMod.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NetworkRuntimeAcceptanceHarness {
    private static final Pattern SAFE_RUN_ID =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    private static final byte[] IPV4_LOOPBACK_BYTES = {127, 0, 0, 1};
    private static final String WORLD_NAME = "wok_network_test";
    private static final String EXPECTED_BATTLE_PROTOCOL = "14";
    private static final String ACADEMY_FACTION_ID = "academy";
    private static final String CAESAR_FACTION_ID = "caesar";
    private static final String DEFAULT_FORMATION_ID = "default";
    private static final long GLOBAL_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(5);
    private static final int ACTION_TIMEOUT_TICKS = 240;
    private static final int ACTION_SPACING_TICKS = 25;
    private static final int STOP_DELAY_TICKS = 20;
    private static final long HOST_SERVER_STALL_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final long HOST_SERVER_PAUSE_NANOS = TimeUnit.SECONDS.toNanos(30);

    private static volatile Config config;
    private static volatile Phase phase = Phase.BOOT;
    private static volatile String failureReason;
    /** True after this role has published its successful acceptance evidence. */
    private static volatile boolean resultWritten;
    /** Kept separate so a failure after PASS is still published and wins orchestration. */
    private static volatile boolean failureWritten;
    private static volatile boolean hostSetupComplete;
    private static volatile String hostEndpoint;
    private static volatile UUID hostPlayerId;
    private static volatile int hostMemoryConnections;
    private static volatile int hostTcpConnections;

    private static long startedNanos;
    private static int totalClientTicks;
    private static int phaseTicks;
    private static int stopTicks;
    private static boolean worldOpenRequested;
    private static Screen handledWorldConfirmation;
    private static boolean guestEverConnected;
    private static boolean actionSent;
    private static long initialGeneration;
    private static BattleSnapshot beforeRequestedSnapshot;
    private static long requestedSnapshotAtNanos;
    private static String guestRemoteAddress;
    private static UUID guestPlayerId;
    private static Faction guestFaction;
    private static String guestPublicFactionId;
    private static String guestFormationId;
    private static SquadCallsign guestCallsign;
    private static TacticalMarkerType guestMarkerType;
    private static long guestInitialServerTime;
    private static long guestFinalServerTime;
    private static int lastObservedHostServerTick = -1;
    private static long lastHostServerProgressNanos;
    private static long hostPauseStartedNanos;
    private static String hostPauseDescription;
    private static volatile int hostServerTickAdvances;
    private static volatile boolean pauseOnLostFocusForced;
    private static boolean pausePolicyLogged;

    private NetworkRuntimeAcceptanceHarness() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || phase == Phase.STOPPED) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        try {
            // Every role must keep ticking while the orchestrator launches hidden windows.
            // Forge's registry handshake also needs guest client/render ticks before login.
            minecraft.options.pauseOnLostFocus = false;
            pauseOnLostFocusForced = true;
            initialize();
            if (!pausePolicyLogged) {
                pausePolicyLogged = true;
                WokInfantryMod.LOGGER.info(
                        "[NETWORK ACCEPTANCE] role={} pauseOnLostFocus forced false",
                        config.role().id);
            }
            totalClientTicks++;
            phaseTicks++;
            if (System.nanoTime() - startedNanos > GLOBAL_TIMEOUT_NANOS
                    && phase != Phase.FAIL && phase != Phase.DONE) {
                fail("Global five-minute timeout in phase " + phase);
            }
            if (config.role() == Role.HOST) {
                tickHostClient(minecraft);
            } else {
                tickGuest(minecraft);
            }
            tickTermination(minecraft);
        } catch (Throwable throwable) {
            WokInfantryMod.LOGGER.error("[NETWORK ACCEPTANCE] Client harness exception", throwable);
            fail("Unhandled client harness exception: " + summarize(throwable));
            tickTermination(minecraft);
        }
    }

    /** All listener mutation and authoritative host verification occur on the server thread. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || config == null
                || config.role() != Role.HOST || phase == Phase.FAIL
                || phase == Phase.DONE || phase == Phase.STOPPED) {
            return;
        }
        try {
            tickHostServer(event.getServer());
        } catch (Throwable throwable) {
            WokInfantryMod.LOGGER.error("[NETWORK ACCEPTANCE] Host server exception", throwable);
            fail("Unhandled host server exception: " + summarize(throwable));
        }
    }

    private static synchronized void initialize() throws IOException {
        if (config != null) {
            return;
        }
        Config loaded = Config.load();
        Files.createDirectories(loaded.resultsDir());
        config = loaded;
        startedNanos = System.nanoTime();
        requireProtocolEight();
        phase = loaded.role() == Role.HOST ? Phase.HOST_WAIT_LOGIN : Phase.GUEST_WAIT_LOGIN;
        WokInfantryMod.LOGGER.info(
                "[NETWORK ACCEPTANCE] Armed role={} runId={} port={} results={}",
                loaded.role().id, loaded.runId(), loaded.port(), loaded.resultsDir());
    }

    private static void tickHostClient(Minecraft minecraft) {
        monitorIntegratedServerProgress(minecraft);
        if (phase == Phase.HOST_WAIT_LOGIN) {
            openFixtureIfNeeded(minecraft);
            if (minecraft.player != null && minecraft.level != null
                    && minecraft.getSingleplayerServer() != null) {
                transition(Phase.HOST_SETUP);
            }
        }
    }

    private static void monitorIntegratedServerProgress(Minecraft minecraft) {
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            lastObservedHostServerTick = -1;
            lastHostServerProgressNanos = 0L;
            hostPauseStartedNanos = 0L;
            hostPauseDescription = null;
            return;
        }
        if (minecraft.screen instanceof PauseScreen) {
            WokInfantryMod.LOGGER.info(
                    "[NETWORK ACCEPTANCE] Closing explicit PauseScreen on unpublished loopback host");
            minecraft.setScreen(null);
        }
        int serverTick = server.getTickCount();
        long now = System.nanoTime();
        if (minecraft.isPaused()) {
            String description = minecraft.screen == null
                    ? "screen=<none>"
                    : "screen=" + minecraft.screen.getClass().getName();
            if (hostPauseStartedNanos == 0L) {
                hostPauseStartedNanos = now;
                hostPauseDescription = description;
                WokInfantryMod.LOGGER.info(
                        "[NETWORK ACCEPTANCE] Integrated server temporarily paused ({})",
                        description);
            }
            lastHostServerProgressNanos = now;
            if (now - hostPauseStartedNanos > HOST_SERVER_PAUSE_NANOS) {
                fail("Integrated server remained paused for more than thirty seconds; "
                        + hostPauseDescription);
            }
            return;
        }
        if (hostPauseStartedNanos != 0L) {
            WokInfantryMod.LOGGER.info(
                    "[NETWORK ACCEPTANCE] Integrated server pause ended ({})",
                    hostPauseDescription);
            hostPauseStartedNanos = 0L;
            hostPauseDescription = null;
        }
        if (lastObservedHostServerTick != serverTick) {
            if (lastObservedHostServerTick >= 0) {
                hostServerTickAdvances++;
            }
            lastObservedHostServerTick = serverTick;
            lastHostServerProgressNanos = now;
            return;
        }
        if ((phase == Phase.HOST_SETUP || phase == Phase.HOST_WAIT_GUESTS)
                && lastHostServerProgressNanos > 0L
                && now - lastHostServerProgressNanos > HOST_SERVER_STALL_NANOS) {
            fail("Integrated server tick count stalled for more than five seconds at "
                    + serverTick + "; pauseOnLostFocus="
                    + minecraft.options.pauseOnLostFocus);
        }
    }

    private static void openFixtureIfNeeded(Minecraft minecraft) {
        if (minecraft.screen instanceof ConfirmScreen confirmation
                && handledWorldConfirmation != confirmation && phaseTicks >= 2) {
            Button proceed = confirmation.children().stream()
                    .filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .filter(button -> button.active && button.visible)
                    .min(Comparator.comparingInt(Button::getX))
                    .orElse(null);
            if (proceed != null) {
                handledWorldConfirmation = confirmation;
                WokInfantryMod.LOGGER.info(
                        "[NETWORK ACCEPTANCE] Confirming isolated fixture prompt with '{}'",
                        proceed.getMessage().getString());
                proceed.onPress();
                return;
            }
        }
        if (!worldOpenRequested && phaseTicks >= 10
                && minecraft.getSingleplayerServer() == null) {
            worldOpenRequested = true;
            Screen parent = minecraft.screen == null ? new TitleScreen() : minecraft.screen;
            WokInfantryMod.LOGGER.info(
                    "[NETWORK ACCEPTANCE] Quick Play did not enter {}; loading it explicitly",
                    WORLD_NAME);
            minecraft.createWorldOpenFlows().loadLevel(parent, WORLD_NAME);
        }
    }

    private static void tickHostServer(MinecraftServer server) throws Exception {
        if (phase == Phase.HOST_SETUP) {
            setupHost(server);
            return;
        }
        if (phase != Phase.HOST_WAIT_GUESTS) {
            return;
        }

        ConnectionCounts counts = countPlayerConnections(server);
        hostMemoryConnections = counts.memory();
        hostTcpConnections = counts.tcp();
        if (counts.players() > 3 || counts.memory() > 1 || counts.tcp() > 2) {
            fail("Unexpected connection cardinality: players=" + counts.players()
                    + " memory=" + counts.memory() + " tcp=" + counts.tcp());
            return;
        }
        if (counts.players() < 3 || counts.memory() != 1 || counts.tcp() != 2) {
            return;
        }
        requireExactLoopbackPlayers(server);
        if (!validPassSignal(Role.GUEST_A) || !validPassSignal(Role.GUEST_B)) {
            return;
        }

        HostEvidence evidence = verifyGuestBattleState(server);
        Properties pass = baseProperties("PASS", Role.HOST,
                "Three-player loopback Forge/C2S/S2C acceptance completed");
        pass.setProperty("listener", Objects.requireNonNullElse(hostEndpoint, ""));
        pass.setProperty("hostPlayerId", Objects.toString(hostPlayerId, ""));
        pass.setProperty("onlinePlayers", Integer.toString(counts.players()));
        pass.setProperty("memoryConnections", Integer.toString(counts.memory()));
        pass.setProperty("tcpConnections", Integer.toString(counts.tcp()));
        pass.setProperty("hostServerTickAdvances",
                Integer.toString(hostServerTickAdvances));
        pass.setProperty("guestIds", evidence.guestIds());
        pass.setProperty("guestSquads", evidence.guestSquads());
        pass.setProperty("guestMarkerTypes", evidence.markerTypes());
        pass.setProperty("guestPublicFactions", evidence.publicFactions());
        pass.setProperty("guestFormations", evidence.formations());
        addSupportEvidence(pass, evidence.support());
        writeSignal(Role.HOST, "pass", pass);
        resultWritten = true;
        transition(Phase.DONE);
    }

    private static void setupHost(MinecraftServer server) throws Exception {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.size() != 1 || players.get(0).connection == null
                || !players.get(0).connection.connection.isMemoryConnection()) {
            return;
        }
        ServerPlayer host = players.get(0);
        hostPlayerId = host.getUUID();

        // Offline authentication is restricted to the exact loopback listener below.
        server.setUsesAuthentication(false);
        InetAddress loopback = InetAddress.getByAddress(IPV4_LOOPBACK_BYTES);
        server.getConnection().startTcpServerListener(loopback, config.port());
        hostEndpoint = verifyListenerAddress(server.getConnection(), config.port());

        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Integrated overworld is unavailable");
        }
        BlockPos sharedSpawn = overworld.getSharedSpawnPos();
        int minimumFeetY = overworld.getMinBuildHeight() + 2;
        int maximumFeetY = overworld.getMaxBuildHeight() - 2;
        int requestedFeetY = Math.max(sharedSpawn.getY(), overworld.getSeaLevel() + 4);
        int feetY = Math.max(minimumFeetY, Math.min(maximumFeetY, requestedFeetY));
        BlockPos blueBase = new BlockPos(sharedSpawn.getX(), feetY, sharedSpawn.getZ());
        BlockPos redBase = blueBase.offset(32, 0, 0);
        prepareSafeBaseFixture(overworld, blueBase);
        prepareSafeBaseFixture(overworld, redBase);
        boolean temporaryOperator = !server.getPlayerList().isOp(host.getGameProfile());
        try {
            if (temporaryOperator) {
                server.getPlayerList().op(host.getGameProfile());
            }
            BattleService battle = BattleService.get(server)
                    .orElseThrow(() -> new IllegalStateException("Battle service is unavailable"));
            ActionResult reset = battle.resetBattle(host);
            requireSuccess(reset, "resetBattle");
            DeploymentService deployment = DeploymentService.get(server)
                    .orElseThrow(() -> new IllegalStateException(
                            "Deployment service is unavailable"));
            requireSuccess(deployment.setMainBaseAt(host, Faction.BLUE, overworld,
                    blueBase, 0.0F),
                    "set BLUE main base");
            requireSuccess(deployment.setMainBaseAt(host, Faction.RED, overworld,
                    redBase, 180.0F), "set RED main base");
        } finally {
            if (temporaryOperator) {
                server.getPlayerList().deop(host.getGameProfile());
            }
        }

        ConnectionCounts counts = countPlayerConnections(server);
        if (counts.players() != 1 || counts.memory() != 1 || counts.tcp() != 0) {
            throw new IllegalStateException("Host-ready cardinality mismatch: players="
                    + counts.players() + " memory=" + counts.memory() + " tcp=" + counts.tcp());
        }
        Properties ready = baseProperties("READY", Role.HOST,
                "IPv4 loopback listener verified and battle fixture reset");
        ready.setProperty("listener", hostEndpoint);
        ready.setProperty("hostPlayerId", host.getUUID().toString());
        ready.setProperty("usesAuthentication", Boolean.toString(server.usesAuthentication()));
        ready.setProperty("memoryConnections", "1");
        ready.setProperty("tcpConnections", "0");
        writeSignal(Role.HOST, "ready", ready);
        hostSetupComplete = true;
        transition(Phase.HOST_WAIT_GUESTS);
    }

    private static void prepareSafeBaseFixture(ServerLevel level, BlockPos feet) {
        level.getChunkAt(feet);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos column = feet.offset(x, 0, z);
                level.setBlockAndUpdate(column.below(), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(column, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(column.above(), Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static void tickGuest(Minecraft minecraft) throws IOException {
        if (phase == Phase.GUEST_WAIT_LOGIN) {
            waitForGuestLogin(minecraft);
            return;
        }
        if (phase == Phase.GUEST_SELECT_FORMATION) {
            selectGuestFormation();
            return;
        }
        if (phase == Phase.GUEST_CREATE_SQUAD) {
            createGuestSquad();
            return;
        }
        if (phase == Phase.GUEST_SELECT_CLASS) {
            selectGuestClass();
            return;
        }
        if (phase == Phase.GUEST_CREATE_MARKER) {
            createGuestMarker();
            return;
        }
        if (phase == Phase.GUEST_REQUEST_SNAPSHOT) {
            requestFreshSnapshot();
            return;
        }
        if (phase == Phase.GUEST_WAIT_HOST_PASS) {
            if (Files.isRegularFile(signalPath(Role.HOST, "fail"))) {
                fail("Host reported failure after guest acceptance");
            } else if (validPassSignal(Role.HOST)) {
                transition(Phase.DONE);
            } else {
                requireGuestStillConnected(minecraft);
            }
        }
    }

    private static void waitForGuestLogin(Minecraft minecraft) {
        ClientPacketListener packetListener = minecraft.getConnection();
        if (minecraft.player == null || minecraft.level == null || packetListener == null
                || !BattleClientActions.available()) {
            if (guestEverConnected && packetListener == null) {
                fail("Guest connection closed before the acceptance flow completed");
            }
            return;
        }
        guestEverConnected = true;
        Connection connection = packetListener.getConnection();
        if (connection.isMemoryConnection()) {
            fail("Guest unexpectedly uses an in-memory connection");
            return;
        }
        SocketAddress remote = connection.getRemoteAddress();
        if (!(remote instanceof InetSocketAddress inet)
                || !isExactIpv4Loopback(inet.getAddress()) || inet.getPort() != config.port()) {
            fail("Guest remote endpoint is not the requested IPv4 loopback listener: " + remote);
            return;
        }
        FormationSelectionSnapshot selection = ClientFormationState.snapshot();
        if (selection == null) {
            if (!actionSent && phaseTicks >= ACTION_SPACING_TICKS) {
                FormationClientNetworkBridge.requestCatalog();
                actionSent = true;
            }
            enforceActionTimeout("receive the mandatory public faction/formation catalog");
            return;
        }
        guestRemoteAddress = remote.toString();
        guestPlayerId = minecraft.player.getUUID();
        if (config.role() == Role.GUEST_A) {
            guestFaction = Faction.RED;
            guestPublicFactionId = CAESAR_FACTION_ID;
            guestCallsign = SquadCallsign.ALPHA;
            guestMarkerType = TacticalMarkerType.INFANTRY;
        } else {
            guestFaction = Faction.BLUE;
            guestPublicFactionId = ACADEMY_FACTION_ID;
            guestCallsign = SquadCallsign.BRAVO;
            guestMarkerType = TacticalMarkerType.IFV;
        }
        guestFormationId = DEFAULT_FORMATION_ID;
        requireDesiredFormationAvailable(selection);
        transition(Phase.GUEST_SELECT_FORMATION);
    }

    private static void selectGuestFormation() {
        FormationSelectionSnapshot selection = ClientFormationState.snapshot();
        if (selection == null) {
            fail("Mandatory public faction/formation catalog disappeared");
            return;
        }
        requireDesiredFormationAvailable(selection);
        if (!selection.selectionRequired()) {
            if (!guestPublicFactionId.equals(selection.selectedFactionId())
                    || !guestFormationId.equals(selection.selectedFormationId())) {
                fail("Guest selected unexpected public formation: faction="
                        + selection.selectedFactionId() + " formation="
                        + selection.selectedFormationId());
                return;
            }
            BattleSnapshot battle = ClientBattleState.snapshot();
            if (battle == null) {
                enforceActionTimeout("receive the first post-selection battle snapshot");
                return;
            }
            if (!battle.viewerId().equals(guestPlayerId)) {
                fail("Viewer-specific snapshot id mismatch: player=" + guestPlayerId
                        + " snapshot=" + battle.viewerId());
                return;
            }
            if (battle.faction() != guestFaction) {
                fail("Public faction " + guestPublicFactionId + " resolved to unexpected battle "
                        + "side " + battle.faction() + " instead of " + guestFaction);
                return;
            }
            requireEmptySupport(battle, "initial guest battle snapshot");
            guestInitialServerTime = battle.serverTimeMillis();
            initialGeneration = ClientBattleState.generation();
            transition(Phase.GUEST_CREATE_SQUAD);
            return;
        }
        if (!actionSent && phaseTicks >= ACTION_SPACING_TICKS) {
            FormationClientNetworkBridge.select(selection.generation(), guestPublicFactionId,
                    guestFormationId);
            actionSent = true;
            return;
        }
        if (actionSent) {
            String feedback = ClientFormationState.feedback();
            if (!feedback.isBlank() && !ClientFormationState.feedbackSuccess()) {
                fail("Server rejected mandatory formation selection: " + feedback);
                return;
            }
        }
        enforceActionTimeout("select public faction " + guestPublicFactionId
                + " and formation " + guestFormationId);
    }

    private static void createGuestSquad() {
        BattleSnapshot snapshot = requireGuestSnapshot();
        if (snapshot == null) {
            return;
        }
        if (snapshot.ownSquad() == guestCallsign && snapshot.squadLeader()) {
            actionSent = false;
            transition(Phase.GUEST_SELECT_CLASS);
            return;
        }
        if (snapshot.ownSquad() != null && snapshot.ownSquad() != guestCallsign) {
            fail("Guest was assigned to unexpected squad " + snapshot.ownSquad());
            return;
        }
        if (!actionSent) {
            BattleClientActions.createSquad(guestCallsign);
            actionSent = true;
        } else {
            failOnNegativeFeedback();
            enforceActionTimeout("create squad " + guestCallsign);
        }
    }

    private static void selectGuestClass() {
        MemberView viewer = ClientBattleState.viewer();
        if (viewer != null && "support".equals(viewer.classId())) {
            actionSent = false;
            transition(Phase.GUEST_CREATE_MARKER);
            return;
        }
        if (!actionSent && phaseTicks >= ACTION_SPACING_TICKS) {
            BattleClientActions.selectClass("support");
            actionSent = true;
        } else if (actionSent) {
            failOnNegativeFeedback();
            enforceActionTimeout("select support class");
        }
    }

    private static void createGuestMarker() {
        BattleSnapshot snapshot = requireGuestSnapshot();
        if (snapshot == null) {
            return;
        }
        TacticalMarker marker = snapshot.markers().stream()
                .filter(candidate -> candidate.creatorId().equals(guestPlayerId)
                        && candidate.type() == guestMarkerType)
                .findFirst().orElse(null);
        if (marker != null) {
            actionSent = false;
            transition(Phase.GUEST_REQUEST_SNAPSHOT);
            return;
        }
        if (!actionSent && phaseTicks >= ACTION_SPACING_TICKS) {
            if (snapshot.deployment().points().isEmpty()) {
                enforceActionTimeout("wait for authoritative deployment point");
                return;
            }
            DeploymentPoint base = snapshot.deployment().points().get(0);
            double offset = config.role() == Role.GUEST_A ? 12.0D : 24.0D;
            BattleClientActions.MarkerTool tool = BattleClientActions.MarkerTool.valueOf(
                    guestMarkerType.name());
            BattleClientActions.createMarker(BattleClientActions.MarkerDraft.point(tool,
                    base.dimension(), base.position().getX() + 0.5D + offset,
                    base.position().getZ() + 0.5D + offset));
            actionSent = true;
        } else if (actionSent) {
            failOnNegativeFeedback();
            enforceActionTimeout("create " + guestMarkerType + " marker");
        }
    }

    private static void requestFreshSnapshot() throws IOException {
        BattleSnapshot snapshot = requireGuestSnapshot();
        if (snapshot == null) {
            return;
        }
        if (!actionSent && phaseTicks >= ACTION_SPACING_TICKS) {
            beforeRequestedSnapshot = snapshot;
            requestedSnapshotAtNanos = System.nanoTime();
            BattleClientActions.requestSnapshot();
            actionSent = true;
            return;
        }
        if (!actionSent) {
            return;
        }
        BattleSnapshot updated = ClientBattleState.snapshot();
        if (updated != null && updated != beforeRequestedSnapshot
                && updated.viewerId().equals(guestPlayerId)
                && updated.serverTimeMillis() >= beforeRequestedSnapshot.serverTimeMillis()) {
            verifyGuestSnapshotContents(updated);
            guestFinalServerTime = updated.serverTimeMillis();
            Properties pass = baseProperties("PASS", config.role(),
                    "Real TCP C2S intents and viewer-specific S2C snapshots verified");
            pass.setProperty("playerId", guestPlayerId.toString());
            pass.setProperty("faction", guestFaction.id());
            pass.setProperty("publicFactionId", guestPublicFactionId);
            pass.setProperty("formationId", guestFormationId);
            pass.setProperty("callsign", guestCallsign.id());
            pass.setProperty("classId", "support");
            pass.setProperty("markerType", guestMarkerType.id());
            pass.setProperty("remoteAddress", guestRemoteAddress);
            pass.setProperty("initialGeneration", Long.toString(initialGeneration));
            pass.setProperty("finalGeneration", Long.toString(ClientBattleState.generation()));
            pass.setProperty("initialServerTime", Long.toString(guestInitialServerTime));
            pass.setProperty("finalServerTime", Long.toString(guestFinalServerTime));
            pass.setProperty("snapshotRequestRoundTripNanos",
                    Long.toString(System.nanoTime() - requestedSnapshotAtNanos));
            addSupportEvidence(pass, requireEmptySupport(updated,
                    "post-request guest battle snapshot"));
            writeSignal(config.role(), "pass", pass);
            resultWritten = true;
            transition(Phase.GUEST_WAIT_HOST_PASS);
            return;
        }
        enforceActionTimeout("receive a post-request viewer snapshot");
    }

    private static void verifyGuestSnapshotContents(BattleSnapshot snapshot) {
        if (snapshot.ownSquad() != guestCallsign || !snapshot.squadLeader()) {
            throw new IllegalStateException("Post-request snapshot lost squad leadership");
        }
        MemberView viewer = snapshot.squads().stream()
                .flatMap(squad -> squad.members().stream())
                .filter(member -> member.playerId().equals(guestPlayerId))
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "Post-request snapshot omits the viewer's squad member record"));
        if (!"support".equals(viewer.classId())) {
            throw new IllegalStateException("Post-request snapshot class is " + viewer.classId());
        }
        boolean markerPresent = snapshot.markers().stream().anyMatch(marker ->
                marker.creatorId().equals(guestPlayerId) && marker.type() == guestMarkerType);
        if (!markerPresent) {
            throw new IllegalStateException("Post-request snapshot omits the guest marker");
        }
        requireEmptySupport(snapshot, "post-request guest battle snapshot");
    }

    private static void requireDesiredFormationAvailable(FormationSelectionSnapshot selection) {
        FactionSelectionView faction = selection.factions().stream()
                .filter(candidate -> guestPublicFactionId.equals(candidate.id()))
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "Public faction catalog omits " + guestPublicFactionId));
        if (!faction.available()) {
            throw new IllegalStateException("Public faction is unavailable: "
                    + guestPublicFactionId);
        }
        FormationSelectionView formation = faction.formations().stream()
                .filter(candidate -> guestFormationId.equals(candidate.id()))
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "Public faction " + guestPublicFactionId + " omits formation "
                                + guestFormationId));
        if (!formation.available()) {
            throw new IllegalStateException("Formation is unavailable: "
                    + guestPublicFactionId + "/" + guestFormationId + " reason="
                    + formation.unavailableReason());
        }
    }

    private static void requireProtocolEight() {
        if (!EXPECTED_BATTLE_PROTOCOL.equals(BattleNetwork.PROTOCOL_VERSION)) {
            throw new IllegalStateException("Network acceptance requires battle protocol "
                    + EXPECTED_BATTLE_PROTOCOL + ", found " + BattleNetwork.PROTOCOL_VERSION);
        }
    }

    private static SupportEvidence requireEmptySupport(BattleSnapshot snapshot, String context) {
        requireProtocolEight();
        if (snapshot == null || snapshot.support() == null) {
            throw new IllegalStateException(context + " has no support DTO");
        }
        boolean available = snapshot.support().serviceAvailable();
        int options = snapshot.support().options().size();
        int activeMissions = snapshot.support().activeMissions().size();
        if (!available || options != 0 || activeMissions != 0) {
            throw new IllegalStateException(context + " violates the empty support framework "
                    + "contract: serviceAvailable=" + available + " options=" + options
                    + " activeMissions=" + activeMissions);
        }
        return new SupportEvidence(available, options, activeMissions);
    }

    private static void addSupportEvidence(Properties properties, SupportEvidence evidence) {
        properties.setProperty("supportServiceAvailable",
                Boolean.toString(evidence.serviceAvailable()));
        properties.setProperty("supportOptions", Integer.toString(evidence.options()));
        properties.setProperty("supportActiveMissions",
                Integer.toString(evidence.activeMissions()));
    }

    private static BattleSnapshot requireGuestSnapshot() {
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        if (snapshot == null) {
            fail("Authoritative guest snapshot disappeared");
            return null;
        }
        if (!snapshot.viewerId().equals(guestPlayerId)) {
            fail("Guest received a snapshot for another viewer: " + snapshot.viewerId());
            return null;
        }
        return snapshot;
    }

    private static void requireGuestStillConnected(Minecraft minecraft) {
        ClientPacketListener packetListener = minecraft.getConnection();
        if (packetListener == null || packetListener.getConnection().isMemoryConnection()
                || !packetListener.getConnection().isConnected()) {
            fail("Guest TCP connection closed while waiting for host verification");
        }
    }

    private static void failOnNegativeFeedback() {
        ClientBattleState.BattleFeedback feedback = ClientBattleState.feedback();
        if (feedback != null && !feedback.success()) {
            fail("Server rejected guest action: " + feedback.message());
        }
    }

    private static void enforceActionTimeout(String action) {
        if (phaseTicks > ACTION_TIMEOUT_TICKS) {
            fail("Timed out while waiting to " + action);
        }
    }

    private static String verifyListenerAddress(ServerConnectionListener listener, int port)
            throws ReflectiveOperationException {
        Field channelsField = ServerConnectionListener.class.getDeclaredField("channels");
        channelsField.setAccessible(true);
        Object value = channelsField.get(listener);
        if (!(value instanceof List<?> channels)) {
            throw new IllegalStateException("Listener channel field is not a list");
        }
        List<InetSocketAddress> tcpListeners = new ArrayList<>();
        for (Object candidate : channels) {
            if (!(candidate instanceof ChannelFuture future)) {
                continue;
            }
            SocketAddress address = future.channel().localAddress();
            if (address instanceof InetSocketAddress inet) {
                if (!future.channel().isActive()) {
                    throw new IllegalStateException("TCP listener channel is inactive: " + inet);
                }
                tcpListeners.add(inet);
            }
        }
        if (tcpListeners.size() != 1) {
            throw new IllegalStateException("Expected one TCP listener, found " + tcpListeners);
        }
        InetSocketAddress bound = tcpListeners.get(0);
        if (bound.getAddress() == null || bound.getAddress().isAnyLocalAddress()
                || !isExactIpv4Loopback(bound.getAddress()) || bound.getPort() != port) {
            throw new IllegalStateException("Unsafe or unexpected listener address: " + bound);
        }
        return bound.getAddress().getHostAddress() + ":" + bound.getPort();
    }

    private static boolean isExactIpv4Loopback(InetAddress address) {
        return address instanceof Inet4Address
                && java.util.Arrays.equals(address.getAddress(), IPV4_LOOPBACK_BYTES);
    }

    private static ConnectionCounts countPlayerConnections(MinecraftServer server) {
        int memory = 0;
        int tcp = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.connection == null || player.connection.connection == null) {
                continue;
            }
            if (player.connection.connection.isMemoryConnection()) {
                memory++;
            } else {
                tcp++;
            }
        }
        return new ConnectionCounts(server.getPlayerList().getPlayers().size(), memory, tcp);
    }

    private static void requireExactLoopbackPlayers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Connection connection = player.connection.connection;
            if (connection.isMemoryConnection()) {
                continue;
            }
            SocketAddress remote = connection.getRemoteAddress();
            if (!(remote instanceof InetSocketAddress inet)
                    || !isExactIpv4Loopback(inet.getAddress())) {
                throw new IllegalStateException("Non-loopback guest connection: " + remote);
            }
        }
    }

    private static HostEvidence verifyGuestBattleState(MinecraftServer server) {
        BattleService service = BattleService.get(server)
                .orElseThrow(() -> new IllegalStateException("Battle service disappeared"));
        FormationService formations = FormationService.get(server)
                .orElseThrow(() -> new IllegalStateException("Formation service disappeared"));
        List<ServerPlayer> guests = server.getPlayerList().getPlayers().stream()
                .filter(player -> !player.connection.connection.isMemoryConnection())
                .toList();
        if (guests.size() != 2) {
            throw new IllegalStateException("Expected two TCP guests, got " + guests.size());
        }
        Set<SquadCallsign> observedSquads = new HashSet<>();
        Set<TacticalMarkerType> observedMarkers = new HashSet<>();
        Set<String> observedPublicFactions = new HashSet<>();
        Set<String> observedFormations = new HashSet<>();
        List<String> ids = new ArrayList<>();
        SupportEvidence observedSupport = null;
        for (ServerPlayer guest : guests) {
            PlayerRecord record = service.playerRecord(guest.getUUID())
                    .orElseThrow(() -> new IllegalStateException(
                            "Missing guest battle record " + guest.getUUID()));
            if (!"support".equals(record.assignedClassId())) {
                throw new IllegalStateException("Guest " + guest.getUUID()
                        + " class is " + record.assignedClassId());
            }
            SquadCallsign squad = record.squad();
            if (squad == null || !observedSquads.add(squad)
                    || service.squadLeader(record.faction(), squad)
                    .filter(guest.getUUID()::equals).isEmpty()) {
                throw new IllegalStateException("Guest " + guest.getUUID()
                        + " is not unique squad leader; squad=" + squad);
            }
            TacticalMarkerType expected = squad == SquadCallsign.ALPHA
                    ? TacticalMarkerType.INFANTRY
                    : squad == SquadCallsign.BRAVO ? TacticalMarkerType.IFV : null;
            if (expected == null) {
                throw new IllegalStateException("Unexpected guest squad " + squad);
            }
            Faction expectedSide = squad == SquadCallsign.ALPHA ? Faction.RED : Faction.BLUE;
            String expectedPublicFaction = squad == SquadCallsign.ALPHA
                    ? CAESAR_FACTION_ID : ACADEMY_FACTION_ID;
            if (record.faction() != expectedSide
                    || !DEFAULT_FORMATION_ID.equals(record.formationId())) {
                throw new IllegalStateException("Guest " + guest.getUUID()
                        + " authoritative formation mismatch: side=" + record.faction()
                        + " formation=" + record.formationId());
            }
            String selectedPublicFaction = formations.selectedFaction(guest.getUUID())
                    .map(faction -> faction.id())
                    .orElseThrow(() -> new IllegalStateException(
                            "Formation service has no selected public faction for "
                                    + guest.getUUID()));
            String selectedFormation = formations.selectedFormation(guest.getUUID())
                    .map(formation -> formation.id())
                    .orElseThrow(() -> new IllegalStateException(
                            "Formation service has no selected formation for "
                                    + guest.getUUID()));
            if (!expectedPublicFaction.equals(selectedPublicFaction)
                    || !DEFAULT_FORMATION_ID.equals(selectedFormation)) {
                throw new IllegalStateException("Guest " + guest.getUUID()
                        + " public formation mismatch: faction=" + selectedPublicFaction
                        + " formation=" + selectedFormation);
            }
            BattleSnapshot guestSnapshot = service.snapshotFor(guest);
            SupportEvidence support = requireEmptySupport(guestSnapshot,
                    "host-authored snapshot for " + guest.getUUID());
            if (observedSupport != null && !observedSupport.equals(support)) {
                throw new IllegalStateException("Guest support evidence differs across viewers");
            }
            observedSupport = support;
            boolean markerPresent = guestSnapshot.markers().stream()
                    .filter(marker -> marker.creatorId().equals(guest.getUUID()))
                    .anyMatch(marker -> marker.type() == expected);
            if (!markerPresent) {
                throw new IllegalStateException("Guest " + guest.getUUID()
                        + " has no authoritative " + expected + " marker");
            }
            observedMarkers.add(expected);
            observedPublicFactions.add(selectedPublicFaction);
            observedFormations.add(selectedFormation);
            verifyEnemyIdentityIsolation(service, guest,
                    server.getPlayerList().getPlayers());
            ids.add(guest.getUUID().toString());
        }
        if (!observedSquads.equals(Set.of(SquadCallsign.ALPHA, SquadCallsign.BRAVO))
                || !observedMarkers.equals(Set.of(TacticalMarkerType.INFANTRY,
                TacticalMarkerType.IFV))
                || !observedPublicFactions.equals(Set.of(ACADEMY_FACTION_ID,
                CAESAR_FACTION_ID))
                || !observedFormations.equals(Set.of(DEFAULT_FORMATION_ID))) {
            throw new IllegalStateException("Guest evidence mismatch: squads=" + observedSquads
                    + " markers=" + observedMarkers + " publicFactions="
                    + observedPublicFactions + " formations=" + observedFormations);
        }
        if (observedSupport == null) {
            throw new IllegalStateException("Host collected no support framework evidence");
        }
        return new HostEvidence(String.join(",", ids), observedSquads.toString(),
                observedMarkers.toString(), observedPublicFactions.toString(),
                observedFormations.toString(), observedSupport);
    }

    private static void verifyEnemyIdentityIsolation(BattleService service, ServerPlayer viewer,
                                                     List<ServerPlayer> onlinePlayers) {
        Faction viewerFaction = service.factionOf(viewer.getUUID())
                .orElseThrow(() -> new IllegalStateException("Viewer has no faction"));
        Set<UUID> enemyIds = new HashSet<>();
        for (ServerPlayer online : onlinePlayers) {
            if (service.factionOf(online.getUUID()).filter(faction -> faction != viewerFaction)
                    .isPresent()) {
                enemyIds.add(online.getUUID());
            }
        }
        BattleSnapshot snapshot = service.snapshotFor(viewer);
        Set<UUID> exposed = new HashSet<>();
        for (SquadView squad : snapshot.squads()) {
            if (squad.leaderId() != null) {
                exposed.add(squad.leaderId());
            }
            squad.members().stream().map(MemberView::playerId).forEach(exposed::add);
        }
        snapshot.alliedPositions().stream().map(MemberPosition::playerId).forEach(exposed::add);
        snapshot.markers().stream().map(TacticalMarker::creatorId).forEach(exposed::add);
        exposed.retainAll(enemyIds);
        if (!exposed.isEmpty()) {
            throw new IllegalStateException("Viewer snapshot exposed enemy UUIDs " + exposed);
        }
    }

    private static boolean validPassSignal(Role role) throws IOException {
        Path file = signalPath(role, "pass");
        if (!Files.isRegularFile(file)) {
            return false;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        return "PASS".equals(properties.getProperty("status"))
                && role.id.equals(properties.getProperty("role"))
                && config.runId().equals(properties.getProperty("runId"))
                && EXPECTED_BATTLE_PROTOCOL.equals(properties.getProperty("battleProtocol"))
                && "true".equals(properties.getProperty("supportServiceAvailable"))
                && "0".equals(properties.getProperty("supportOptions"))
                && "0".equals(properties.getProperty("supportActiveMissions"));
    }

    private static Properties baseProperties(String status, Role role, String detail) {
        Properties properties = new Properties();
        properties.setProperty("status", status);
        properties.setProperty("role", role.id);
        properties.setProperty("runId", config.runId());
        properties.setProperty("port", Integer.toString(config.port()));
        properties.setProperty("detail", Objects.requireNonNullElse(detail, ""));
        properties.setProperty("battleProtocol", BattleNetwork.PROTOCOL_VERSION);
        properties.setProperty("pauseOnLostFocusForced",
                Boolean.toString(pauseOnLostFocusForced));
        properties.setProperty("totalClientTicks", Integer.toString(totalClientTicks));
        properties.setProperty("timestampMillis", Long.toString(System.currentTimeMillis()));
        return properties;
    }

    private static synchronized void writeSignal(Role role, String kind, Properties properties)
            throws IOException {
        Path target = signalPath(role, kind);
        Files.createDirectories(target.getParent());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        properties.store(output, "WOK Infantry loopback network acceptance");
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp-"
                + UUID.randomUUID());
        try {
            Files.write(temporary, output.toByteArray(), StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Path signalPath(Role role, String kind) {
        return config.resultsDir().resolve(config.runId() + "-" + role.id + "-" + kind
                + ".properties");
    }

    private static void tickTermination(Minecraft minecraft) {
        if (phase != Phase.FAIL && phase != Phase.DONE) {
            return;
        }
        if (phase == Phase.FAIL && !failureWritten) {
            try {
                String reason = Objects.requireNonNullElse(failureReason, "unknown failure");
                Properties failure = baseProperties("FAIL", config.role(), reason);
                failure.setProperty("phase", phase.name());
                failure.setProperty("failure", reason);
                writeSignal(config.role(), "fail", failure);
                failureWritten = true;
            } catch (Throwable writeFailure) {
                WokInfantryMod.LOGGER.error(
                        "[NETWORK ACCEPTANCE] Could not write failure result", writeFailure);
                failureWritten = true;
            }
        }
        if (++stopTicks >= STOP_DELAY_TICKS) {
            phase = Phase.STOPPED;
            minecraft.stop();
        }
    }

    private static void requireSuccess(ActionResult result, String operation) {
        if (result == null || !result.success()) {
            throw new IllegalStateException(operation + " failed: "
                    + (result == null ? "null result" : result.code() + " " + result.message()));
        }
    }

    private static synchronized void transition(Phase next) {
        WokInfantryMod.LOGGER.info("[NETWORK ACCEPTANCE] {} -> {}", phase, next);
        phase = next;
        phaseTicks = 0;
        actionSent = false;
    }

    private static synchronized void fail(String reason) {
        if (phase == Phase.FAIL || phase == Phase.DONE || phase == Phase.STOPPED) {
            return;
        }
        failureReason = Objects.requireNonNullElse(reason, "unknown failure");
        WokInfantryMod.LOGGER.error("[NETWORK ACCEPTANCE] FAIL in {}: {}", phase,
                failureReason);
        phase = Phase.FAIL;
        phaseTicks = 0;
    }

    private static String summarize(Throwable throwable) {
        String message = throwable.getMessage();
        return throwable.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private record Config(Role role, int port, String runId, Path resultsDir) {
        private static Config load() {
            Role role = Role.parse(requiredProperty("wok.net.role"));
            String portText = requiredProperty("wok.net.port");
            int port;
            try {
                port = Integer.parseInt(portText);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("wok.net.port is not an integer: " + portText,
                        exception);
            }
            if (port < 1_024 || port > 65_535) {
                throw new IllegalArgumentException("wok.net.port must be 1024..65535: " + port);
            }
            String runId = requiredProperty("wok.net.runId");
            if (!SAFE_RUN_ID.matcher(runId).matches()) {
                throw new IllegalArgumentException(
                        "wok.net.runId must match " + SAFE_RUN_ID.pattern());
            }
            Path results = Path.of(requiredProperty("wok.net.resultsDir"))
                    .toAbsolutePath().normalize();
            return new Config(role, port, runId, results);
        }

        private static String requiredProperty(String name) {
            String value = System.getProperty(name);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Missing required system property " + name);
            }
            return value.trim();
        }
    }

    private enum Role {
        HOST("host"),
        GUEST_A("guest-a"),
        GUEST_B("guest-b");

        private final String id;

        Role(String id) {
            this.id = id;
        }

        private static Role parse(String value) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (Role role : values()) {
                if (role.id.equals(normalized)) {
                    return role;
                }
            }
            throw new IllegalArgumentException("Unsupported wok.net.role: " + value);
        }
    }

    private enum Phase {
        BOOT,
        HOST_WAIT_LOGIN,
        HOST_SETUP,
        HOST_WAIT_GUESTS,
        GUEST_WAIT_LOGIN,
        GUEST_SELECT_FORMATION,
        GUEST_CREATE_SQUAD,
        GUEST_SELECT_CLASS,
        GUEST_CREATE_MARKER,
        GUEST_REQUEST_SNAPSHOT,
        GUEST_WAIT_HOST_PASS,
        FAIL,
        DONE,
        STOPPED
    }

    private record ConnectionCounts(int players, int memory, int tcp) {
    }

    private record SupportEvidence(boolean serviceAvailable, int options, int activeMissions) {
    }

    private record HostEvidence(String guestIds, String guestSquads, String markerTypes,
                                String publicFactions, String formations,
                                SupportEvidence support) {
    }
}
