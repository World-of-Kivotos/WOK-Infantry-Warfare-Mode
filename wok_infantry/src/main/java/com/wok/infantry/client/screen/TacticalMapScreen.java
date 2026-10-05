package com.wok.infantry.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;
import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.battle.MemberPosition;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.SquadView;
import com.wok.infantry.battle.TacticalMarker;
import com.wok.infantry.battle.TacticalMarkerType;
import com.wok.infantry.client.BattleClientActions;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.map.TacticalMapIcons;
import com.wok.infantry.client.map.TacticalMapPinPlanner;
import com.wok.infantry.client.map.TacticalMapSegmentClipper;
import com.wok.infantry.client.map.TacticalMapAreaOverlay;
import com.wok.infantry.client.map.TacticalMapAreaOverlayRegistry;
import com.wok.infantry.client.map.TacticalMapTerrainRegistry;
import com.wok.infantry.client.map.TacticalMapTerrainRequest;
import com.wok.infantry.client.map.TacticalSupportMapPresentation;
import com.wok.infantry.client.map.TacticalSupportMapPresentationRegistry;
import com.wok.infantry.config.InfantryClientConfig;
import com.wok.infantry.deployment.DeploymentPoint;
import com.wok.infantry.deployment.DeploymentPointKind;
import com.wok.infantry.deployment.DeploymentPhase;
import com.wok.infantry.support.SupportMissionView;
import com.wok.infantry.support.SupportOptionView;
import com.wok.infantry.support.SupportTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.lang.ref.WeakReference;

/** Full-screen tactical map backed by an optional JourneyMap or Xaero terrain engine. */
public final class TacticalMapScreen extends Screen {
    static final int COMPASS_TOP_OFFSET = 36;
    static final int MARKER_DELETE_HEIGHT = 20;
    /** Physical pixels a marker-tool icon keeps clear above and below it inside its key. */
    static final int TOOL_ICON_MARGIN_PHYSICAL = 2;
    /** Hit tolerance around an attack-order line, in physical pixels. */
    static final double ATTACK_LINE_HIT_PHYSICAL = 10.0D;
    static final int ALLIED_PLAYER_MARKER_RADIUS = 5;
    static final int ALLIED_LEADER_MARKER_RADIUS = 6;
    static final int ALLIED_COMMANDER_MARKER_RADIUS = 7;
    static final int ALLIED_PLAYER_DIRECTION_LENGTH = 12;
    static final int ALLIED_PLAYER_HOVER_RADIUS = 13;
    static final double MIN_INTEL_MARKER_SCALE =
            InfantryClientConfig.MIN_MAP_MARKER_SCALE;
    static final double MAX_INTEL_MARKER_SCALE =
            InfantryClientConfig.MAX_MAP_MARKER_SCALE;
    static final double INTEL_MARKER_SCALE_STEP = 0.05D;
    /** Leader of a deployment pin that stepped aside: friendly blue on the dark symbol casing. */
    private static final int PIN_LEADER_COLOR = TacticalBoardTheme.MAP_ICON_FRIENDLY;
    private static final int PIN_LEADER_CASING = TacticalBoardTheme.MAP_ICON_OUTLINE;
    private static final int SUPPORT_AREA_ALPHA = 0x38;
    private static final int SUPPORT_ACTIVE_AREA_ALPHA = 0x48;
    private static final float MAP_TEXT_PHYSICAL_SCALE = 2.0F;
    private static final int OFFENSIVE_SUPPORT_FILL_ALPHA = 0x50;
    private static final int OFFENSIVE_SUPPORT_HATCH_ALPHA = 0x80;
    static final int SUPPORT_RADIUS_MAX_PIXELS = 192;
    static final int SUPPORT_GUIDANCE_MIN_RING_PIXELS = 3;
    static final int SUPPORT_GUIDANCE_MIN_DASHES = 8;
    static final int SUPPORT_GUIDANCE_MAX_DASHES = 256;
    // Designation zones are partition lines, not danger boundaries: thin amber dashes on a dark
    // casing stay distinct from the red impact outline and its hatching on any terrain.
    private static final int SUPPORT_GUIDANCE_COLOR = BattleUiTheme.ACCENT;
    private static final int SUPPORT_GUIDANCE_CASING = 0xB0141B1D;
    private static final int SUPPORT_GUIDANCE_LINE_PHYSICAL = 2;
    private static final int SUPPORT_GUIDANCE_CASING_PHYSICAL = 4;
    private static final int SUPPORT_GUIDANCE_DASH_PHYSICAL = 9;
    private static final int SUPPORT_GUIDANCE_GAP_PHYSICAL = 6;
    private static final int SUPPORT_PAGE_SIZE = 3;
    private static final double MIN_ZOOM = 0.02D;
    private static final double MAX_ZOOM = 4.0D;
    // Keeps a 1080p/1440p tactical overview useful while staying inside the terrain tile budget.
    // Wider strategic views intentionally fall back to the tactical coordinate grid.
    private static final double DEFAULT_ZOOM = 0.45D;
    private static final long TERRAIN_DEBOUNCE_NANOS = 250_000_000L;
    private static final long TERRAIN_MIN_REQUEST_INTERVAL_NANOS = 100_000_000L;
    private static final long TERRAIN_REQUEST_TIMEOUT_NANOS = 10_000_000_000L;
    private static final long TERRAIN_RETRY_NANOS = 5_000_000_000L;
    private static final int TERRAIN_CHUNK_MARGIN = 4;
    // JourneyMap's public API caps responses at 512 px. Current 6.0.x terrain data is one pixel
    // per block even when a higher API zoom is requested, so 32 chunks is the largest safe side.
    private static final int TERRAIN_TILE_CHUNK_SPAN = TacticalMapTerrainRequest.MAX_CHUNK_SPAN;
    private static final int TERRAIN_MAX_IN_FLIGHT = 4;
    private static final int TERRAIN_MAX_HOST_PENDING = 8;
    private static final int TERRAIN_REQUEST_BURST = 4;
    // Each tile is a separate JourneyMap request, texture upload and render binding. A strict
    // viewport budget prevents normal large displays from creating hundreds of dynamic textures.
    private static final int TERRAIN_MAX_VIEWPORT_TILES = 96;
    private static final int TERRAIN_MAX_CACHED_TILES = 128;
    private static final long TERRAIN_MAX_CACHED_PIXELS = 8L * 1024L * 1024L;
    private final Screen previous;
    private double intelMarkerScale;
    private long observedGeneration = -1L;
    private boolean requestedSnapshot;
    private boolean centerInitialized;
    private ResourceLocation centeredDimension;
    private double centerWorldX;
    private double centerWorldZ;
    private double zoom = DEFAULT_ZOOM;
    private int mapLeft;
    private int mapTop;
    private int mapRight;
    private int mapBottom;
    private int sidebarLeft;
    private boolean compactTools;
    private boolean richBoard;
    private int topBarControlsRight;
    private TacticalMapLayout.Layout boardLayout;

    private BattleClientActions.MarkerTool selectedTool;
    private WorldPoint attackStart;
    private ResourceLocation selectedSupport;
    private WorldPoint supportStart;
    private int supportPage;
    private SidebarToolMode sidebarToolMode = SidebarToolMode.MARKERS;
    private UUID selectedMarkerId;
    private boolean showPlayers = true;
    private boolean showInfantry = true;
    private boolean showVehicles = true;
    private boolean showOrders = true;
    private boolean panningMap;

    private Set<TacticalMapTerrainRequest> observedTerrainRequests = Set.of();
    private List<TacticalMapTerrainRequest> desiredTerrainRequests = List.of();
    private final LinkedHashMap<TacticalMapTerrainRequest, TerrainTexture> terrainTextures =
            new LinkedHashMap<>(32, 0.75F, true);
    private final Map<TacticalMapTerrainRequest, InFlightTerrainTile> inFlightTerrainRequests =
            new HashMap<>();
    private final Map<TacticalMapTerrainRequest, Long> terrainRetryAfterNanos = new HashMap<>();
    private final Set<TacticalMapTerrainRequest> failedTerrainRequests = new HashSet<>();
    private final Map<TacticalMapTerrainRequest, InFlightTerrainTile> timedOutTerrainRequests =
            new HashMap<>();
    private final List<MarkerToolIconSlot> markerToolIconSlots = new ArrayList<>();
    /**
     * Label panels drawn so far this frame (support cards and tags, area notes): a deployment pin
     * that has to step aside avoids landing on them.
     */
    private final List<TacticalMapLayout.Rect> frameLabels = new ArrayList<>();
    private final Map<ResourceLocation, TacticalBoardButton> supportButtons =
            new LinkedHashMap<>();
    private long terrainRequestDueNanos;
    private long terrainLastRequestNanos;
    private long terrainRequestEpoch;
    private long terrainNextRequestSerial;
    private long terrainCachedPixels;
    private long terrainProviderGeneration = -1L;
    private boolean terrainProviderReady;
    private boolean terrainScreenActive;
    private boolean terrainViewportTooLarge;
    private String terrainProviderId = "grid";

    public TacticalMapScreen() {
        this(null);
    }

    public TacticalMapScreen(Screen previous) {
        super(Component.translatable("screen.wok_infantry.tactical_map"));
        this.previous = previous;
        this.intelMarkerScale = clampIntelMarkerScale(
                InfantryClientConfig.markerScale());
    }

    @Override
    protected void init() {
        terrainScreenActive = true;
        markerToolIconSlots.clear();
        supportButtons.clear();
        observedGeneration = ClientBattleState.generation();
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        if (snapshot == null && !requestedSnapshot) {
            requestedSnapshot = true;
            BattleClientActions.requestSnapshot();
        }

        boardLayout = TacticalMapLayout.compute(width, height);
        TacticalMapLayout.Rect viewport = boardLayout.mapViewport();
        mapLeft = viewport.left();
        mapTop = viewport.top();
        mapRight = viewport.right();
        mapBottom = viewport.bottom();
        sidebarLeft = boardLayout.sidebar().left();
        compactTools = boardLayout.compact();
        richBoard = boardLayout.rich();

        initTopBar();
        initIconScaleSlider();
        initLayerButtons();
        initSidebarModeSwitcher();
        if (snapshot != null && snapshot.permissions().canCreateMarkers()) {
            initMarkerTools();
        } else {
            selectedTool = null;
            attackStart = null;
        }
        if (snapshot != null && selectedSupport != null
                && !supportUsable(snapshot, selectedSupport)) {
            selectedSupport = null;
            supportStart = null;
        }
        if (snapshot != null) {
            initSupportTools(snapshot);
        }
        initMarkerDelete(snapshot);
        initializeCenter(snapshot);
    }

    private void initTopBar() {
        int y = boardLayout.topBarY();
        int buttonHeight = richBoard ? 22 : 20;
        int tabWidth = width < 420 ? 62 : richBoard ? 104 : 84;
        int centerButtonWidth = width < 420 ? 54 : richBoard ? 88 : 74;
        int x = boardLayout.header().left() + 4;
        Component squadsLabel = fittedButtonLabel("screen.wok_infantry.tab.squads",
                "screen.wok_infantry.tab.squads_short", tabWidth);
        TacticalBoardButton squadsButton = boardButton(x, y, tabWidth, buttonHeight,
                squadsLabel, ignored -> {
                    Screen target = previous instanceof SquadScreen ? previous : new SquadScreen(this);
                    Minecraft.getInstance().setScreen(target);
                }, TacticalBoardButton.Kind.NAVIGATION, false, TacticalBoardTheme.FRIENDLY);
        squadsButton.setTooltip(Tooltip.create(
                Component.translatable("screen.wok_infantry.tab.squads")));
        addRenderableWidget(squadsButton);
        x += tabWidth + 3;
        Component mapLabel = fittedButtonLabel("screen.wok_infantry.tab.map",
                "screen.wok_infantry.tab.map_short", tabWidth);
        TacticalBoardButton active = boardButton(x, y, tabWidth, buttonHeight, mapLabel,
                ignored -> {
                }, TacticalBoardButton.Kind.NAVIGATION, true,
                TacticalBoardTheme.SELECTED);
        active.setTooltip(Tooltip.create(Component.translatable("screen.wok_infantry.tab.map")));
        active.active = false;
        addRenderableWidget(active);
        x += tabWidth + 6;
        addRenderableWidget(boardButton(x, y, 22, buttonHeight, Component.literal("−"),
                ignored -> zoomAtCenter(1.0D / 1.25D),
                TacticalBoardButton.Kind.CONTROL, false, TacticalBoardTheme.ACCENT));
        x += 25;
        addRenderableWidget(boardButton(x, y, 22, buttonHeight, Component.literal("+"),
                ignored -> zoomAtCenter(1.25D), TacticalBoardButton.Kind.CONTROL,
                false, TacticalBoardTheme.ACCENT));
        x += 25;
        Component centerLabel = fittedButtonLabel("gui.wok_infantry.map.center",
                "gui.wok_infantry.map.center_short", centerButtonWidth);
        TacticalBoardButton centerButton = boardButton(x, y, centerButtonWidth, buttonHeight,
                centerLabel, ignored -> {
            centerInitialized = false;
            initializeCenter(ClientBattleState.snapshot());
        }, TacticalBoardButton.Kind.CONTROL, false, TacticalBoardTheme.ACCENT);
        centerButton.setTooltip(Tooltip.create(
                Component.translatable("gui.wok_infantry.map.center")));
        addRenderableWidget(centerButton);
        x += centerButtonWidth + 3;
        TacticalBoardButton refresh = boardButton(x, y, 22, buttonHeight,
                Component.literal("R"), ignored -> BattleClientActions.requestSnapshot(),
                TacticalBoardButton.Kind.CONTROL, false, TacticalBoardTheme.ACCENT);
        refresh.setTooltip(Tooltip.create(Component.translatable("gui.wok_infantry.refresh")));
        addRenderableWidget(refresh);
        topBarControlsRight = x + 22;
    }

    private static TacticalBoardButton boardButton(int x, int y, int width, int height,
                                                    Component label, Button.OnPress onPress,
                                                    TacticalBoardButton.Kind kind,
                                                    boolean engaged, int accentColor) {
        return new TacticalBoardButton(x, y, width, height, label, onPress,
                kind, engaged, accentColor);
    }

    private void initLayerButtons() {
        if (selectionReplacesSidebar()) {
            return;
        }
        TacticalMapLayout.Rect sidebar = boardLayout.sidebar();
        int x = sidebar.left() + 6;
        int availableWidth = sidebar.right() - x - 6;
        int gap = richBoard ? 4 : 0;
        int buttonWidth = richBoard ? (availableWidth - gap) / 2 : availableWidth;
        int buttonHeight = richBoard ? 24 : 20;
        int rowGap = richBoard ? 4 : 3;
        int y = boardLayout.layerButtonsTop();
        addRenderableWidget(layerButton("screen.wok_infantry.map.layer.players", showPlayers,
                x, y, buttonWidth, buttonHeight, value -> showPlayers = value));
        addRenderableWidget(layerButton("screen.wok_infantry.map.layer.infantry", showInfantry,
                richBoard ? x + buttonWidth + gap : x,
                richBoard ? y : y + buttonHeight + rowGap,
                buttonWidth, buttonHeight, value -> showInfantry = value));
        addRenderableWidget(layerButton("screen.wok_infantry.map.layer.vehicles", showVehicles,
                x, y + (richBoard ? buttonHeight + rowGap : (buttonHeight + rowGap) * 2),
                buttonWidth, buttonHeight, value -> showVehicles = value));
        addRenderableWidget(layerButton("screen.wok_infantry.map.layer.orders", showOrders,
                richBoard ? x + buttonWidth + gap : x,
                y + (richBoard ? buttonHeight + rowGap : (buttonHeight + rowGap) * 3),
                buttonWidth, buttonHeight, value -> showOrders = value));
    }

    private Button layerButton(String key, boolean value, int x, int y, int buttonWidth,
                               int buttonHeight, BooleanSetter setter) {
        return boardButton(x, y, buttonWidth, buttonHeight, layerLabel(key, value), button -> {
            boolean replacement = !value;
            setter.set(replacement);
            rebuildWidgets();
        }, TacticalBoardButton.Kind.TOGGLE, value, TacticalBoardTheme.SELECTED);
    }

    private void initIconScaleSlider() {
        TacticalMapLayout.Rect bounds = TacticalMapLayout.iconScaleSlider(
                boardLayout, topBarControlsRight);
        if (bounds.width() == 0) {
            return;
        }
        Component label = richBoard
                ? Component.translatable("screen.wok_infantry.map.icon_scale")
                : Component.empty();
        TacticalBoardSlider slider = new TacticalBoardSlider(
                bounds.left(), bounds.top(), bounds.width(), bounds.height(), label,
                MIN_INTEL_MARKER_SCALE, MAX_INTEL_MARKER_SCALE,
                INTEL_MARKER_SCALE_STEP, intelMarkerScale,
                value -> {
                    intelMarkerScale = clampIntelMarkerScale(value);
                    InfantryClientConfig.setMarkerScale(intelMarkerScale);
                });
        slider.setTooltip(Tooltip.create(Component.translatable(
                "screen.wok_infantry.map.icon_scale.tooltip")));
        addRenderableWidget(slider);
    }

    private void initSidebarModeSwitcher() {
        if (richBoard || selectionReplacesSidebar()) {
            return;
        }
        TacticalMapLayout.Rect bounds = TacticalMapLayout.sidebarModeSwitcher(boardLayout);
        int gap = 3;
        int markerWidth = Math.max(20, (bounds.width() - gap) / 2);
        int supportWidth = Math.max(20, bounds.width() - markerWidth - gap);
        Component markerLabel = Component.translatable(
                "screen.wok_infantry.map.mode.markers");
        Component supportLabel = Component.translatable(
                "screen.wok_infantry.map.mode.support");
        TacticalBoardButton markers = boardButton(bounds.left(), bounds.top(),
                markerWidth, bounds.height(), markerLabel, ignored -> {
                    sidebarToolMode = SidebarToolMode.MARKERS;
                    selectedSupport = null;
                    supportStart = null;
                    rebuildWidgets();
                }, TacticalBoardButton.Kind.NAVIGATION,
                sidebarToolMode == SidebarToolMode.MARKERS,
                TacticalBoardTheme.SELECTED);
        markers.setTooltip(Tooltip.create(Component.translatable(
                "screen.wok_infantry.map.marker_tools")));
        addRenderableWidget(markers);

        TacticalBoardButton support = boardButton(bounds.left() + markerWidth + gap,
                bounds.top(), supportWidth, bounds.height(), supportLabel, ignored -> {
                    sidebarToolMode = SidebarToolMode.SUPPORT;
                    selectedTool = null;
                    attackStart = null;
                    selectedMarkerId = null;
                    rebuildWidgets();
                }, TacticalBoardButton.Kind.NAVIGATION,
                sidebarToolMode == SidebarToolMode.SUPPORT,
                TacticalBoardTheme.ACCENT);
        support.setTooltip(Tooltip.create(Component.translatable(
                "screen.wok_infantry.map.support.module")));
        addRenderableWidget(support);
    }

    private void initMarkerTools() {
        if (!richBoard && sidebarToolMode != SidebarToolMode.MARKERS) {
            return;
        }
        if (selectionReplacesSidebar() && !compactTools) {
            return;
        }
        TacticalMapLayout.Rect sidebar = boardLayout.sidebar();
        TacticalMapLayout.Rect toolRegion = TacticalMapLayout.markerToolRegion(boardLayout);
        int baseX = compactTools ? mapLeft + 5 : sidebar.left() + 6;
        int gap = compactTools ? 2 : richBoard ? 4 : 0;
        int availableWidth = compactTools
                ? mapRight - mapLeft - 10
                : sidebar.right() - baseX - 6;
        int buttonWidth = compactTools
                ? Math.max(20, (availableWidth - gap * 5) / 6)
                : richBoard ? (availableWidth - gap) / 2 : availableWidth;
        int buttonHeight = richBoard ? 24 : 20;
        int rowGap = richBoard ? 4 : 2;
        int index = 0;
        for (BattleClientActions.MarkerTool tool : BattleClientActions.MarkerTool.values()) {
            int column = richBoard ? index % 2 : 0;
            int row = richBoard ? index / 2 : index;
            int x = compactTools
                    ? baseX + index * (buttonWidth + gap)
                    : baseX + column * (buttonWidth + gap);
            int y = compactTools
                    ? toolRegion.top()
                    : toolRegion.top() + row * (buttonHeight + rowGap);
            Component fullName = markerToolName(tool);
            Button.OnPress onPress = ignored -> {
                if (selectedTool == tool) {
                    selectedTool = null;
                    attackStart = null;
                } else {
                    selectedTool = tool;
                    attackStart = null;
                    selectedSupport = null;
                    supportStart = null;
                    selectedMarkerId = null;
                }
                rebuildWidgets();
            };
            Button button;
            if (compactTools) {
                button = boardButton(x, y, buttonWidth, buttonHeight, Component.empty(),
                        onPress, TacticalBoardButton.Kind.TOOL, selectedTool == tool,
                        markerColor(markerType(tool)));
                button.setTooltip(Tooltip.create(fullName));
            } else {
                Component label = richBoard
                        ? Component.literal("   ").append(fullName) : fullName;
                button = boardButton(x, y, buttonWidth, buttonHeight, label, onPress,
                        TacticalBoardButton.Kind.TOOL, selectedTool == tool,
                        markerColor(markerType(tool)));
                button.setTooltip(Tooltip.create(fullName));
            }
            addRenderableWidget(button);
            if (compactTools || richBoard) {
                markerToolIconSlots.add(new MarkerToolIconSlot(tool,
                        compactTools ? x + buttonWidth / 2 : x + 14,
                        y + buttonHeight / 2, buttonHeight));
            }
            index++;
        }
    }

    private void initSupportTools(BattleSnapshot snapshot) {
        if (selectionReplacesSidebar()
                || !richBoard && sidebarToolMode != SidebarToolMode.SUPPORT) {
            return;
        }
        List<SupportOptionView> options = snapshot.support().options();
        int pageCount = supportPageCount(options.size());
        SupportPagingState pagingState = supportPagingState(supportPage, supportPage,
                pageCount, selectedSupport, supportStart != null);
        if (pagingState.changed()) {
            supportPage = pagingState.page();
            selectedSupport = pagingState.selectedSupport();
            if (!pagingState.supportStartSet()) {
                supportStart = null;
            }
        }
        int fromIndex = supportPage * SUPPORT_PAGE_SIZE;
        int visibleCount = Math.max(0,
                Math.min(SUPPORT_PAGE_SIZE, options.size() - fromIndex));
        for (int slot = 0; slot < visibleCount; slot++) {
            SupportOptionView option = options.get(fromIndex + slot);
            ResourceLocation supportId = option.id();
            TacticalMapLayout.Rect bounds = TacticalMapLayout.supportButton(
                    boardLayout, slot, visibleCount);
            if (bounds.width() <= 0 || bounds.height() <= 0) {
                continue;
            }
            TacticalBoardButton button = boardButton(bounds.left(), bounds.top(),
                    bounds.width(), bounds.height(), Component.empty(), ignored -> {
                        BattleSnapshot current = ClientBattleState.snapshot();
                        if (current == null || !supportUsable(current, supportId)) {
                            return;
                        }
                        if (supportId.equals(selectedSupport)) {
                            selectedSupport = null;
                            supportStart = null;
                        } else {
                            selectedSupport = supportId;
                            supportStart = null;
                            selectedTool = null;
                            attackStart = null;
                            selectedMarkerId = null;
                        }
                        rebuildWidgets();
                    }, TacticalBoardButton.Kind.TOOL, supportId.equals(selectedSupport),
                    supportColor(option));
            supportButtons.put(supportId, button);
            addRenderableWidget(button);
        }
        initSupportPager(pageCount);
        refreshSupportButtons(snapshot);
    }

    private void initSupportPager(int pageCount) {
        if (pageCount <= 1 || compactTools) {
            return;
        }
        TacticalMapLayout.Rect previous = TacticalMapLayout.supportPagerButton(
                boardLayout, false);
        TacticalMapLayout.Rect next = TacticalMapLayout.supportPagerButton(boardLayout, true);
        if (previous.width() > 0) {
            TacticalBoardButton button = boardButton(previous.left(), previous.top(),
                    previous.width(), previous.height(), Component.literal("‹"), ignored -> {
                        changeSupportPage(supportPage - 1, pageCount);
                    }, TacticalBoardButton.Kind.NAVIGATION, false,
                    TacticalBoardTheme.ACCENT);
            button.active = supportPage > 0;
            addRenderableWidget(button);
        }
        if (next.width() > 0) {
            TacticalBoardButton button = boardButton(next.left(), next.top(),
                    next.width(), next.height(), Component.literal("›"), ignored -> {
                        changeSupportPage(supportPage + 1, pageCount);
                    }, TacticalBoardButton.Kind.NAVIGATION, false,
                    TacticalBoardTheme.ACCENT);
            button.active = supportPage + 1 < pageCount;
            addRenderableWidget(button);
        }
    }

    private void changeSupportPage(int requestedPage, int pageCount) {
        SupportPagingState pagingState = supportPagingState(supportPage, requestedPage,
                pageCount, selectedSupport, supportStart != null);
        if (!pagingState.changed()) {
            return;
        }
        supportPage = pagingState.page();
        selectedSupport = pagingState.selectedSupport();
        if (!pagingState.supportStartSet()) {
            supportStart = null;
        }
        rebuildWidgets();
    }

    static SupportPagingState supportPagingState(int currentPage, int requestedPage,
                                                  int pageCount,
                                                  ResourceLocation selectedSupport,
                                                  boolean supportStartSet) {
        int page = Math.max(0, Math.min(Math.max(0, pageCount - 1), requestedPage));
        if (page == currentPage) {
            return new SupportPagingState(page, selectedSupport, supportStartSet, false);
        }
        return new SupportPagingState(page, null, false, true);
    }

    private void refreshSupportButtons(BattleSnapshot snapshot) {
        long now = ClientBattleState.estimatedSupportGameTick();
        supportButtons.forEach((supportId, button) -> {
            SupportOptionView option = supportOption(snapshot, supportId);
            SupportUiStatus status = supportStatus(snapshot, option, now);
            button.setMessage(option == null ? Component.literal(supportId.toString())
                    : supportButtonLabel(option, status, button.getWidth()));
            button.active = supportUsable(snapshot, option, now);
            if (option == null) {
                return;
            }
            MutableComponent tooltip = supportName(option).copy().append(" · ")
                    .append(status.label()).append("\n")
                    .append(Component.translatable(option.directional()
                            ? "screen.wok_infantry.map.support.target.directional"
                            : "screen.wok_infantry.map.support.target.point"));
            if (option != null && !option.providerAvailable() && !option.readOnlyMission()
                    && !option.availabilityReason().isBlank()) {
                tooltip.append("\n").append(Component.literal(option.availabilityReason()));
            }
            button.setTooltip(Tooltip.create(tooltip));
        });
    }

    private boolean supportUsable(BattleSnapshot snapshot, ResourceLocation supportId) {
        return supportUsable(snapshot, supportOption(snapshot, supportId),
                ClientBattleState.estimatedSupportGameTick());
    }

    private static boolean supportUsable(BattleSnapshot snapshot,
                                         SupportOptionView option, long now) {
        return snapshot.commander()
                && snapshot.deployment().phase() == DeploymentPhase.ACTIVE
                && option != null
                && option.ready(now);
    }

    private static SupportOptionView supportOption(BattleSnapshot snapshot,
                                                   ResourceLocation supportId) {
        return snapshot.support().options().stream()
                .filter(option -> option.id().equals(supportId))
                .findFirst().orElse(null);
    }

    private static SupportUiStatus supportStatus(BattleSnapshot snapshot,
                                                 SupportOptionView option, long now) {
        if (option != null && option.readOnlyMission()) {
            return new SupportUiStatus(
                    Component.translatable(
                            "screen.wok_infantry.map.support.status.formation_closed"),
                    Component.translatable(
                            "screen.wok_infantry.map.support.status.formation_closed_short"));
        }
        if (!snapshot.commander()) {
            return new SupportUiStatus(
                    Component.translatable("screen.wok_infantry.map.support.status.locked"),
                    Component.translatable(
                            "screen.wok_infantry.map.support.status.locked_short"));
        }
        if (snapshot.deployment().phase() != DeploymentPhase.ACTIVE) {
            return new SupportUiStatus(
                    Component.translatable(
                            "screen.wok_infantry.map.support.status.waiting_deployment"),
                    Component.translatable(
                            "screen.wok_infantry.map.support.status.waiting_deployment_short"));
        }
        if (option == null || !option.providerAvailable()) {
            return new SupportUiStatus(
                    Component.translatable(
                            "screen.wok_infantry.map.support.status.provider_missing"),
                    Component.translatable(
                            "screen.wok_infantry.map.support.status.provider_missing_short"));
        }
        if (option.active()) {
            return new SupportUiStatus(
                    Component.translatable("screen.wok_infantry.map.support.status.active"),
                    Component.translatable(
                            "screen.wok_infantry.map.support.status.active_short"));
        }
        long remainingTicks = option.cooldownRemainingTicks(now);
        if (remainingTicks > 0L) {
            Component cooldown = Component.literal(Long.toString((remainingTicks + 19L) / 20L))
                    .copy().append(Component.translatable(
                            "screen.wok_infantry.map.support.status.seconds_suffix"));
            return new SupportUiStatus(cooldown, cooldown);
        }
        return new SupportUiStatus(
                Component.translatable("screen.wok_infantry.map.support.status.ready"),
                Component.translatable("screen.wok_infantry.map.support.status.ready_short"));
    }

    private Component supportButtonLabel(SupportOptionView option, SupportUiStatus status,
                                         int buttonWidth) {
        Component full = supportName(option).copy().append("  ").append(status.label());
        if (font.width(full) <= Math.max(1, buttonWidth - 6)) {
            return full;
        }
        return supportShortName(option).copy().append(" ").append(status.shortLabel());
    }

    private static Component supportName(SupportOptionView option) {
        return Component.translatableWithFallback(option.translationKey(),
                option.fallbackName());
    }

    private static Component supportShortName(SupportOptionView option) {
        return Component.literal(option.shortName());
    }

    private static int supportColor(SupportOptionView option) {
        return switch (supportPresentation(option)) {
            case OFFENSIVE -> TacticalBoardTheme.DANGER;
            case INTELLIGENCE -> TacticalBoardTheme.SELECTED;
            case UTILITY -> TacticalBoardTheme.ACCENT;
        };
    }

    private static TacticalSupportMapPresentation supportPresentation(
            SupportOptionView option) {
        return TacticalSupportMapPresentationRegistry.presentation(option.id());
    }

    private static int supportPageCount(int optionCount) {
        return optionCount <= 0 ? 0
                : (optionCount + SUPPORT_PAGE_SIZE - 1) / SUPPORT_PAGE_SIZE;
    }

    private void initMarkerDelete(BattleSnapshot snapshot) {
        TacticalMarker selected = selectedMarker();
        if (snapshot == null || selected == null) {
            return;
        }
        boolean ownMarker = selected.creatorId().equals(snapshot.viewerId());
        boolean sameSquadLeader = snapshot.squadLeader()
                && snapshot.ownSquad() != null
                && snapshot.ownSquad() == selected.creatorSquad();
        boolean canRemove = snapshot.permissions().canRemoveAnyMarker()
                || ownMarker || sameSquadLeader;
        if (!canRemove) {
            return;
        }
        int buttonX = boardLayout.sidebar().left() + 7;
        int availableWidth = boardLayout.sidebar().right() - buttonX - 7;
        int buttonWidth = Math.max(20, availableWidth);
        int buttonY = Math.min(selectedDetailTop() + 68,
                boardLayout.sidebar().bottom() - MARKER_DELETE_HEIGHT - 7);
        TacticalBoardButton deleteButton = boardButton(buttonX, buttonY,
                buttonWidth, MARKER_DELETE_HEIGHT,
                Component.translatable("gui.wok_infantry.map.remove_marker"), ignored -> {
            BattleClientActions.removeMarker(selected.id());
            selectedMarkerId = null;
            rebuildWidgets();
        }, TacticalBoardButton.Kind.DANGER, false, TacticalBoardTheme.DANGER);
        addRenderableWidget(deleteButton);
    }

    private boolean selectionReplacesSidebar() {
        return selectedMarker() != null && !richBoard
                && (compactTools || mapBottom - markerToolsBottom() < 96);
    }

    private int markerToolsBottom() {
        if (compactTools) {
            return boardLayout.compactDrawer().bottom();
        }
        int rows = richBoard ? 3 : BattleClientActions.MarkerTool.values().length;
        int buttonHeight = richBoard ? 24 : 20;
        int rowGap = richBoard ? 4 : 2;
        return TacticalMapLayout.markerToolRegion(boardLayout).top()
                + rows * buttonHeight + (rows - 1) * rowGap;
    }

    private int selectedDetailTop() {
        if (selectionReplacesSidebar()) {
            return mapTop + 4;
        }
        return richBoard ? boardLayout.detailTop() : markerToolsBottom() + 8;
    }

    @Override
    public void tick() {
        super.tick();
        if (!currentDimension().equals(centeredDimension)) {
            invalidateTerrainRequests();
            releaseTerrainTextures();
            attackStart = null;
            selectedSupport = null;
            supportStart = null;
            selectedMarkerId = null;
            panningMap = false;
            centerInitialized = false;
            initializeCenter(ClientBattleState.snapshot());
            rebuildWidgets();
            return;
        }
        if (observedGeneration != ClientBattleState.generation()) {
            rebuildWidgets();
        }
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        if (snapshot != null) {
            refreshSupportButtons(snapshot);
        }
        tickTerrain();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTerminalShell(graphics);
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        renderBoardHeader(graphics, snapshot);

        if (snapshot == null) {
            BattleUiTheme.drawCenteredText(graphics, font,
                    Component.translatable("screen.wok_infantry.waiting_snapshot"),
                    width / 2, height / 2, TacticalBoardTheme.MUTED_TEXT);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        renderMap(graphics, snapshot, mouseX, mouseY);
        if (compactTools) {
            TacticalMapLayout.Rect drawer = boardLayout.compactDrawer();
            TacticalBoardTheme.raisedPanel(graphics, drawer.left(), drawer.top(),
                    drawer.right(), drawer.bottom(), TacticalBoardTheme.BOARD_ALT);
        }
        renderSidebar(graphics, snapshot);
        renderFooter(graphics, snapshot, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderCompactMarkerToolIcons(graphics);
        TacticalMapLayout.Rect footer = boardLayout.footer();
        BattleUiTheme.feedback(graphics, font, footer.left(), footer.right(),
                footer.top() + 5);
    }

    private void renderTerminalShell(GuiGraphics graphics) {
        graphics.fill(0, 0, width, height, TacticalBoardTheme.WORLD_SHADE);
        graphics.fill(2, 3, width, height, TacticalBoardTheme.DEVICE_SHADOW);
        graphics.fill(0, 0, width, height, TacticalBoardTheme.DEVICE_FRAME);
        BattleUiTheme.outline(graphics, 1, 1, width - 1, height - 1,
                TacticalBoardTheme.DEVICE_EDGE);
        graphics.fill(4, 4, width - 4, height - 4, TacticalBoardTheme.DEVICE_MID);
        TacticalMapLayout.Rect header = boardLayout.header();
        TacticalMapLayout.Rect footer = boardLayout.footer();
        graphics.fill(header.left(), header.top(), footer.right(), footer.bottom(),
                TacticalBoardTheme.BOARD);
        TacticalBoardTheme.raisedPanel(graphics, header.left(), header.top(),
                header.right(), header.bottom(), TacticalBoardTheme.DEVICE_FRAME);
        TacticalBoardTheme.raisedPanel(graphics, footer.left(), footer.top(),
                footer.right(), footer.bottom(), TacticalBoardTheme.BOARD_ALT);
        TacticalBoardTheme.rivet(graphics, 6, 6);
        TacticalBoardTheme.rivet(graphics, width - 7, 6);
        TacticalBoardTheme.rivet(graphics, 6, height - 7);
        TacticalBoardTheme.rivet(graphics, width - 7, height - 7);
    }

    private void renderBoardHeader(GuiGraphics graphics, BattleSnapshot snapshot) {
        TacticalMapLayout.Rect header = boardLayout.header();
        Component headerTitle = Component.translatable("screen.wok_infantry.map.board_title");
        Component identity = snapshot == null
                ? Component.translatable("screen.wok_infantry.map.link_connecting")
                : boardIdentity(snapshot);
        int identityWidth = Math.min(header.width() / 2, font.width(identity));
        int titleWidth = Math.max(32, header.width() - identityWidth - 32);
        int titleY = header.top() + 4;
        graphics.fill(header.left() + 5, titleY + 1,
                header.left() + 8, titleY + 8,
                snapshot == null ? TacticalBoardTheme.ACCENT : TacticalBoardTheme.SUCCESS);
        graphics.drawString(font, fittedText(headerTitle, titleWidth),
                header.left() + 12, titleY, TacticalBoardTheme.LIGHT_TEXT, false);
        graphics.drawString(font, fittedText(identity, header.width() / 2),
                header.right() - identityWidth - 8, titleY,
                snapshot == null ? TacticalBoardTheme.ACCENT : 0xFF9DD8F4, false);
    }

    private Component boardIdentity(BattleSnapshot snapshot) {
        Component faction = snapshot.faction() == null
                ? Component.translatable("faction.wok_infantry.unassigned")
                : Component.translatable("faction.wok_infantry." + snapshot.faction().id());
        MutableComponent identity = faction.copy();
        if (snapshot.ownSquad() != null) {
            identity.append(" · ").append(SquadScreen.callsign(snapshot.ownSquad()));
        }
        if (snapshot.commander()) {
            identity.append(" · ").append(Component.translatable(
                    "screen.wok_infantry.map.authority.commander"));
        } else if (snapshot.squadLeader()) {
            identity.append(" · ").append(Component.translatable(
                    "screen.wok_infantry.map.authority.squad_leader"));
        }
        return identity;
    }

    private void renderMap(GuiGraphics graphics, BattleSnapshot snapshot, int mouseX, int mouseY) {
        TacticalMapLayout.Rect frame = boardLayout.mapFrame();
        TacticalBoardTheme.insetPanel(graphics, frame.left(), frame.top(),
                frame.right(), frame.bottom(), TacticalBoardTheme.INSET);
        graphics.fill(mapLeft, mapTop, mapRight, mapBottom, 0xFF8D9996);
        graphics.enableScissor(mapLeft, mapTop, mapRight, mapBottom);

        // Map layers are intentionally ordered from world background to interaction chrome.
        // Keep translucent world areas below symbols, and reserve the final pass for labels,
        // hover feedback and the cursor so add-ons cannot accidentally cover core controls.
        renderTerrain(graphics);
        renderGrid(graphics);
        ResourceLocation dimension = currentDimension();
        List<TacticalMapAreaOverlay> addonAreas = TacticalMapAreaOverlayRegistry.overlays();
        // One marker list per frame: the server already merged and capped it (reserved intel and
        // manual quotas), so the map only filters by dimension and layer, never trims or reorders.
        List<TacticalMarker> markers = visibleMarkers(dimension);
        int artPx = mapIconArtPx();
        frameLabels.clear();
        UUID hovered = panningMap ? null : hoveredMarkerId(markers, mouseX, mouseY, artPx);

        // Area / order geometry.
        renderAddonAreaGeometry(graphics, dimension, addonAreas);
        renderSupportMissions(graphics, snapshot, dimension);
        renderAttackDirectionLines(graphics, markers);

        // World annotations are below discrete tactical symbols. Deployment pins that found a
        // clear spot go below the marker icons; a pin that could not get clear of every icon is
        // drawn above them so its head is never hidden.
        renderAddonAreaAnnotations(graphics, dimension, addonAreas);
        List<TacticalMapPinPlanner.Plan> pins = planDeploymentPins(snapshot, dimension, markers,
                hovered, artPx);
        renderDeploymentPins(graphics, pins, false, artPx);
        renderMarkerIcons(graphics, markers, hovered, artPx);
        renderDeploymentPins(graphics, pins, true, artPx);

        // Live units and active previews take precedence over static map content.
        if (showPlayers) {
            renderPlayers(graphics, snapshot, dimension, mouseX, mouseY);
        }
        renderAttackPreview(graphics, dimension, mouseX, mouseY, artPx);
        renderSupportPreview(graphics, dimension, mouseX, mouseY);

        // Map chrome always remains readable regardless of add-on content beneath it.
        renderCompass(graphics);
        renderMapCursor(graphics, mouseX, mouseY);
        graphics.disableScissor();
        renderMapScale(graphics);
        BattleUiTheme.outline(graphics, mapLeft, mapTop, mapRight, mapBottom,
                TacticalBoardTheme.BORDER);
        TacticalBoardTheme.cornerBrackets(graphics, mapLeft, mapTop,
                mapRight, mapBottom, TacticalBoardTheme.BORDER_BRIGHT);
        TacticalBoardTheme.rivet(graphics, frame.left() + 2, frame.top() + 2);
        TacticalBoardTheme.rivet(graphics, frame.right() - 3, frame.top() + 2);
        TacticalBoardTheme.rivet(graphics, frame.left() + 2, frame.bottom() - 3);
        TacticalBoardTheme.rivet(graphics, frame.right() - 3, frame.bottom() - 3);
    }

    private void renderMapCursor(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!insideMap(mouseX, mouseY)) {
            return;
        }
        int color = 0xA0F4F7F3;
        graphics.pose().pushPose();
        graphics.pose().translate(mouseX, mouseY, 0.0D);
        graphics.pose().scale(mapInverseGuiScale(), mapInverseGuiScale(), 1.0F);
        graphics.fill(-9, 0, -3, 1, color);
        graphics.fill(4, 0, 10, 1, color);
        graphics.fill(0, -9, 1, -3, color);
        graphics.fill(0, 4, 1, 10, color);
        BattleUiTheme.outline(graphics, -2, -2, 3, 3, color);
        graphics.pose().popPose();
    }

    private void renderMapScale(GuiGraphics graphics) {
        MapScaleBar bar = mapScaleBar();
        TacticalMapLayout.Rect panel = bar.panel();
        graphics.fill(panel.left(), panel.top(), panel.right(), panel.bottom(), 0xA8D7DDDA);
        BattleUiTheme.outline(graphics, panel.left(), panel.top(),
                panel.right(), panel.bottom(), TacticalBoardTheme.BORDER);
        int left = bar.left();
        int right = bar.right();
        int bottom = bar.bottom();
        graphics.fill(left, bottom, right + 1, bottom + 2, TacticalBoardTheme.TEXT);
        graphics.fill(left, bottom - 3, left + 2, bottom + 3, TacticalBoardTheme.TEXT);
        graphics.fill(right - 1, bottom - 3, right + 1, bottom + 3, TacticalBoardTheme.TEXT);
        drawMapCenteredString(graphics, bar.label(), (left + right) / 2,
                panel.top() + mapTextLogicalHeight() / 2 + mapPhysicalToLogical(2),
                TacticalBoardTheme.TEXT);
    }

    private MapScaleBar mapScaleBar() {
        double targetWorldDistance = 80.0D / Math.max(zoom, 0.001D);
        double magnitude = Math.pow(10.0D, Math.floor(Math.log10(targetWorldDistance)));
        double normalized = targetWorldDistance / magnitude;
        double step = normalized >= 5.0D ? 5.0D : normalized >= 2.0D ? 2.0D : 1.0D;
        int worldDistance = Math.max(1, (int) Math.round(step * magnitude));
        int pixelWidth = Math.max(20, (int) Math.round(worldDistance * zoom));
        int left = mapLeft + 12;
        int bottom = mapBottom - 9;
        int right = Math.min(mapRight - 12, left + pixelWidth);
        String label = worldDistance + " m";
        int labelWidth = mapTextLogicalWidth(label);
        int labelHeight = mapTextLogicalHeight();
        int centerX = (left + right) / 2;
        int panelLeft = Math.min(left - 5,
                centerX - labelWidth / 2 - mapPhysicalToLogical(4));
        int panelRight = Math.max(right + 6,
                centerX + (labelWidth + 1) / 2 + mapPhysicalToLogical(4));
        int panelTop = bottom - labelHeight - mapPhysicalToLogical(8);
        return new MapScaleBar(left, right, bottom, label,
                new TacticalMapLayout.Rect(panelLeft, panelTop, panelRight, bottom + 5));
    }

    private void renderGrid(GuiGraphics graphics) {
        double worldStep = gridWorldStep();
        double minX = screenToWorldX(mapLeft);
        double maxX = screenToWorldX(mapRight);
        double minZ = screenToWorldZ(mapTop);
        double maxZ = screenToWorldZ(mapBottom);
        double firstX = Math.floor(minX / worldStep) * worldStep;
        double firstZ = Math.floor(minZ / worldStep) * worldStep;

        int index = 0;
        for (double worldX = firstX; worldX <= maxX && index++ < 256; worldX += worldStep) {
            int x = worldToScreenX(worldX);
            boolean major = Math.round(worldX / worldStep) % 4L == 0L;
            graphics.fill(x, mapTop, x + 1, mapBottom,
                    major ? TacticalBoardTheme.GRID_MAJOR : TacticalBoardTheme.GRID_MINOR);
            if (major && x > mapLeft + 3 && x < mapRight - 58) {
                String label = axisGridLabel('E', 'W', worldX);
                int labelWidth = mapTextLogicalWidth(label);
                int labelHeight = mapTextLogicalHeight();
                graphics.fill(x + 2, mapTop + 2, x + labelWidth + 6,
                        mapTop + labelHeight + 5, 0xB8D7DDDA);
                drawMapString(graphics, label, x + 4, mapTop + 3,
                        TacticalBoardTheme.TEXT);
            }
        }
        index = 0;
        for (double worldZ = firstZ; worldZ <= maxZ && index++ < 256; worldZ += worldStep) {
            int y = worldToScreenY(worldZ);
            boolean major = Math.round(worldZ / worldStep) % 4L == 0L;
            graphics.fill(mapLeft, y, mapRight, y + 1,
                    major ? TacticalBoardTheme.GRID_MAJOR : TacticalBoardTheme.GRID_MINOR);
            if (major && y > mapTop + 14 && y < mapBottom - 28) {
                String label = axisGridLabel('S', 'N', worldZ);
                int labelWidth = mapTextLogicalWidth(label);
                int labelHeight = mapTextLogicalHeight();
                graphics.fill(mapLeft + 2, y + 2, mapLeft + labelWidth + 6,
                        y + labelHeight + 5, 0xB8D7DDDA);
                drawMapString(graphics, label, mapLeft + 4, y + 3,
                        TacticalBoardTheme.TEXT);
            }
        }
    }

    private static String axisGridLabel(char positive, char negative, double coordinate) {
        char direction = coordinate >= 0.0D ? positive : negative;
        long value = Math.abs(Math.round(coordinate));
        return direction + String.format(Locale.ROOT, "%04d", value);
    }

    private void renderCompass(GuiGraphics graphics) {
        int x = mapRight - 18;
        // Keep the compass below the top feedback banner.
        int y = mapTop + COMPASS_TOP_OFFSET;
        String north = Component.translatable("screen.wok_infantry.map.north").getString();
        int labelHeight = mapTextLogicalHeight();
        TacticalMapLayout.Rect panel = mapCompassPanel();
        int bottom = panel.bottom();
        graphics.fill(panel.left(), panel.top(), panel.right(), bottom, 0xA8D7DDDA);
        BattleUiTheme.outline(graphics, panel.left(), panel.top(), panel.right(), bottom,
                TacticalBoardTheme.BORDER);
        drawMapCenteredString(graphics, north, x, y, TacticalBoardTheme.TEXT);
        int arrowTop = y + labelHeight / 2 + mapPhysicalToLogical(3);
        int arrowBottom = bottom - mapPhysicalToLogical(4);
        int arrowWing = mapPhysicalToLogical(5);
        drawLine(graphics, x, arrowTop, x, arrowBottom, TacticalBoardTheme.ACCENT);
        drawLine(graphics, x, arrowTop, x - arrowWing,
                arrowTop + arrowWing, TacticalBoardTheme.ACCENT);
        drawLine(graphics, x, arrowTop, x + arrowWing,
                arrowTop + arrowWing, TacticalBoardTheme.ACCENT);
    }

    private TacticalMapLayout.Rect mapCompassPanel() {
        int x = mapRight - 18;
        int y = mapTop + COMPASS_TOP_OFFSET;
        String north = Component.translatable("screen.wok_infantry.map.north").getString();
        int labelHeight = mapTextLogicalHeight();
        int halfWidth = Math.max(9, mapTextLogicalWidth(north) / 2 + mapPhysicalToLogical(3));
        int top = y - labelHeight / 2 - mapPhysicalToLogical(3);
        int bottom = y + labelHeight / 2 + mapPhysicalToLogical(20);
        return new TacticalMapLayout.Rect(x - halfWidth, top, x + halfWidth + 1, bottom);
    }

    /** Scale bar and compass are drawn above every map layer, so labels must keep clear. */
    private List<TacticalMapLayout.Rect> mapChrome() {
        return List.of(mapScaleBar().panel(), mapCompassPanel());
    }

    private void renderPlayers(GuiGraphics graphics, BattleSnapshot snapshot,
                               ResourceLocation dimension, int mouseX, int mouseY) {
        List<MemberPosition> positions = snapshot.alliedPositions().stream()
                .filter(position -> position.dimension().equals(dimension))
                .sorted(Comparator.comparing(position ->
                        !ClientBattleState.isSameSquad(position.playerId())))
                .toList();
        MemberView hovered = null;
        int hoveredX = 0;
        int hoveredY = 0;
        double hoverRadius = ALLIED_PLAYER_HOVER_RADIUS / mapGuiScale();
        double hoveredDistance = hoverRadius * hoverRadius;
        for (MemberPosition position : positions) {
            int x = worldToScreenX(position.x());
            int y = worldToScreenY(position.z());
            if (!insideMap(x, y)) {
                continue;
            }
            boolean self = position.playerId().equals(snapshot.viewerId());
            boolean squadMate = ClientBattleState.isSameSquad(position.playerId());
            MemberView member = ClientBattleState.member(position.playerId());
            SquadCallsign squad = member == null ? null : member.squad();
            int color = self ? BattleUiTheme.ACCENT : squadColor(squad, squadMate);
            int radius = alliedPlayerMarkerRadius(self,
                    member != null && member.commander(),
                    member != null && member.leader());
            drawMapDiamond(graphics, x, y, radius, color);
            if (member != null && member.commander()) {
                drawMapLocalLine(graphics, x, y,
                        -radius, 0, radius, 0, 0xFFFFFFFF, 2);
                drawMapLocalLine(graphics, x, y,
                        0, -radius, 0, radius, 0xFFFFFFFF, 2);
            }
            double radians = Math.toRadians(position.yaw());
            int directionX = -(int) Math.round(Math.sin(radians)
                    * ALLIED_PLAYER_DIRECTION_LENGTH);
            int directionY = (int) Math.round(Math.cos(radians)
                    * ALLIED_PLAYER_DIRECTION_LENGTH);
            drawMapLocalLine(graphics, x, y,
                    0, 0, directionX, directionY, color, 2);
            if (member != null && (squadMate || member.leader() || member.commander())) {
                String label = squadMate ? Integer.toString(memberNumber(snapshot, member))
                        : squadLetter(squad);
                int labelOffset = radius + 2;
                drawMapString(graphics, label, x, y,
                        labelOffset, -labelOffset, color);
            }
            double dx = mouseX - x;
            double dy = mouseY - y;
            double distance = dx * dx + dy * dy;
            if (member != null && distance < hoveredDistance) {
                hoveredDistance = distance;
                hovered = member;
                hoveredX = x;
                hoveredY = y;
            }
        }
        if (hovered != null) {
            Component hoveredSquad = hovered.squad() == null
                    ? Component.translatable("screen.wok_infantry.status.none")
                    : SquadScreen.callsign(hovered.squad());
            Component tooltip = hoveredSquad.copy()
                    .append(" · ").append(Component.literal(hovered.name()))
                    .append(" · ").append(SquadScreen.className(snapshot, hovered.classId()));
            String visibleTooltip = fittedMapText(tooltip.getString(), mapRight - mapLeft - 10);
            int tooltipWidth = mapTextLogicalWidth(visibleTooltip);
            int tooltipHeight = mapTextLogicalHeight();
            int tooltipX = Math.max(mapLeft + 3,
                    Math.min(mapRight - tooltipWidth - 5, hoveredX + 8));
            int tooltipY = Math.max(mapTop + 3,
                    hoveredY - tooltipHeight - mapPhysicalToLogical(4));
            graphics.fill(tooltipX - 2, tooltipY - 2,
                    tooltipX + tooltipWidth + 2,
                    tooltipY + tooltipHeight + 2, 0xD9081014);
            drawMapString(graphics, visibleTooltip, tooltipX, tooltipY, 0xFFF2F6F7);
        }
    }

    /**
     * Deployment points as friendly pins (main base, field beacon, squad rally pack) with the tip
     * on the point. A pin whose spot is taken by a marker icon, an attack order or an earlier pin
     * steps aside and draws a leader back ({@link TacticalMapPinPlanner}); the spawn point the
     * player picked carries a green check badge, the yellow ring stays "selected on this map".
     */
    private List<TacticalMapPinPlanner.Plan> planDeploymentPins(BattleSnapshot snapshot,
                                                                ResourceLocation dimension,
                                                                List<TacticalMarker> markers,
                                                                UUID hovered, int artPx) {
        double scale = mapGuiScale();
        UUID spawn = snapshot.deployment().selectedPointId();
        List<TacticalMapPinPlanner.Pin> pins = new ArrayList<>();
        for (DeploymentPoint point : snapshot.deployment().points()) {
            if (!point.dimension().equals(dimension)) {
                continue;
            }
            double x = screenX(point.position().getX() + 0.5D);
            double y = screenY(point.position().getZ() + 0.5D);
            if (!insideMap(x, y)) {
                continue;
            }
            pins.add(new TacticalMapPinPlanner.Pin(TacticalMapIcons.MapIcon.of(point.kind()),
                    Math.round(x * scale), Math.round(y * scale), point.id().equals(spawn)));
        }
        if (pins.isEmpty()) {
            return List.of();
        }
        List<TacticalMapPinPlanner.Box> avoid = new ArrayList<>();
        List<TacticalMapLayout.Rect> labels = new ArrayList<>(frameLabels);
        labels.addAll(mapChrome());
        for (TacticalMapLayout.Rect label : labels) {
            avoid.add(new TacticalMapPinPlanner.Box(label.left() * scale, label.top() * scale,
                    label.right() * scale, label.bottom() * scale, 1.0D, false));
        }
        return TacticalMapPinPlanner.plan(pins, pinObstacles(markers, hovered, artPx), avoid,
                artPx, physicalViewport(1), 6.0D * scale, symbolPhysical(3), spawnBadgeHalf());
    }

    /**
     * What a deployment pin must not sit on: every drawn marker icon (with its ring when selected
     * or hovered), the start icon of an attack order being placed, the attack orders' heads and,
     * at a quarter weight, their shafts.
     */
    private List<TacticalMapPinPlanner.Box> pinObstacles(List<TacticalMarker> markers,
                                                         UUID hovered, int artPx) {
        double scale = mapGuiScale();
        TacticalMapPinPlanner.Box viewport = physicalViewport(0);
        List<TacticalMapPinPlanner.Box> obstacles = new ArrayList<>();
        for (TacticalMarker marker : markers) {
            boolean ring = marker.id().equals(selectedMarkerId) || marker.id().equals(hovered);
            int pad = ring ? 3 : 1;
            double x = screenX(marker.x());
            double y = screenY(marker.z());
            if (marker.type() == TacticalMarkerType.ATTACK_DIRECTION) {
                double endX = screenX(marker.endX());
                double endY = screenY(marker.endZ());
                TacticalMapSegmentClipper.ClippedSegment clipped = TacticalMapSegmentClipper.clip(
                        x, y, endX, endY, mapLeft, mapTop, mapRight - 1.0D, mapBottom - 1.0D);
                if (clipped == null) {
                    continue;
                }
                double x0 = clipped.startX() * scale;
                double y0 = clipped.startY() * scale;
                double x1 = clipped.endX() * scale;
                double y1 = clipped.endY() * scale;
                obstacles.addAll(TacticalMapPinPlanner.lineBoxes(x0, y0, x1, y1, 6.0D * scale,
                        symbolPhysical(ring ? 7 : 4), TacticalMapPinPlanner.SHAFT_WEIGHT,
                        viewport));
                double length = Math.max(1.0E-3D, Math.hypot(x1 - x0, y1 - y0));
                double back = symbolPhysical(7);
                obstacles.add(TacticalMapPinPlanner.Box.around(
                        x1 - (x1 - x0) / length * back, y1 - (y1 - y0) / length * back,
                        symbolPhysical(10), 1.0D, false));
            }
            if (insideMap(x, y)) {
                obstacles.add(TacticalMapPinPlanner.iconBox(
                        TacticalMapIcons.MapIcon.of(marker.type()), x * scale, y * scale, artPx,
                        pad, 1.0D, true));
            }
        }
        if (selectedTool == BattleClientActions.MarkerTool.ATTACK_DIRECTION && attackStart != null
                && attackStart.dimension().equals(currentDimension())) {
            double x = screenX(attackStart.x());
            double y = screenY(attackStart.z());
            if (insideMap(x, y)) {
                obstacles.add(TacticalMapPinPlanner.iconBox(
                        TacticalMapIcons.MapIcon.ATTACK_DIRECTION, x * scale, y * scale, artPx, 1,
                        1.0D, true));
            }
        }
        return obstacles;
    }

    /**
     * Pins of one layer: the ones clear of the marker icons ({@code over} false, drawn below the
     * icons) or the ones that could not get clear ({@code over} true, drawn above them). A moved pin
     * first draws its leader from the true spot (friendly blue on a dark casing, a small diamond on
     * the spot) to its tip.
     */
    private void renderDeploymentPins(GuiGraphics graphics,
                                      List<TacticalMapPinPlanner.Plan> pins, boolean over,
                                      int artPx) {
        double scale = mapGuiScale();
        for (TacticalMapPinPlanner.Plan plan : pins) {
            if (plan.over() != over) {
                continue;
            }
            double x = plan.pin().x();
            double y = plan.pin().y();
            if (plan.moved()) {
                drawPhysicalLine(graphics, x, y, plan.tipX(), plan.tipY(), PIN_LEADER_CASING,
                        symbolPhysical(4));
                drawPhysicalLine(graphics, x, y, plan.tipX(), plan.tipY(), PIN_LEADER_COLOR,
                        symbolPhysical(2));
                drawPhysicalDiamond(graphics, x, y, symbolPhysical(3), PIN_LEADER_CASING);
                drawPhysicalDiamond(graphics, x, y, symbolPhysical(2), PIN_LEADER_COLOR);
            }
            TacticalMapIcons.draw(graphics, plan.pin().icon(), (float) (plan.tipX() / scale),
                    (float) (plan.tipY() / scale), artPx, TacticalMapIcons.IconState.NORMAL);
            if (plan.badge() != null) {
                renderSpawnBadge(graphics, plan.badge());
            }
        }
    }

    /** Green check badge (preview {@code spawnBadge}): "this is the spawn point you picked". */
    private void renderSpawnBadge(GuiGraphics graphics, TacticalMapPinPlanner.Badge badge) {
        int iconPx = spawnBadgeIconPx();
        int half = badge.half();
        int centerX = (int) Math.round(badge.x());
        int centerY = (int) Math.round(badge.y());
        int iconSize = TacticalIcon.SIZE * iconPx;
        int iconLeft = centerX - half + (2 * half + 1 - iconSize) / 2;
        int iconTop = centerY - half + (2 * half + 1 - iconSize) / 2;
        graphics.pose().pushPose();
        graphics.pose().scale(mapInverseGuiScale(), mapInverseGuiScale(), 1.0F);
        graphics.fill(centerX - half - 1, centerY - half - 1, centerX + half + 2,
                centerY + half + 2, TacticalBoardTheme.MAP_ICON_OUTLINE);
        graphics.fill(centerX - half, centerY - half, centerX + half + 1, centerY + half + 1,
                TacticalBoardTheme.SUCCESS_B);
        graphics.pose().translate(iconLeft, iconTop, 0.0F);
        graphics.pose().scale(iconPx, iconPx, 1.0F);
        TacticalIcon.CHECK.draw(graphics, 0, 0, TacticalBoardTheme.WELL);
        graphics.pose().popPose();
    }

    private void renderAddonAreaGeometry(GuiGraphics graphics, ResourceLocation dimension,
                                         List<TacticalMapAreaOverlay> areas) {
        for (TacticalMapAreaOverlay area : areas) {
            if (!area.dimension().equals(dimension)) {
                continue;
            }
            int left = Math.min(worldToScreenX(area.minX()), worldToScreenX(area.maxX()));
            int right = Math.max(worldToScreenX(area.minX()), worldToScreenX(area.maxX()));
            int top = Math.min(worldToScreenY(area.minZ()), worldToScreenY(area.maxZ()));
            int bottom = Math.max(worldToScreenY(area.minZ()), worldToScreenY(area.maxZ()));
            if (right < mapLeft || left > mapRight || bottom < mapTop || top > mapBottom) {
                continue;
            }
            int color = area.locked() ? TacticalBoardTheme.MUTED_TEXT : area.color();
            int fill = (color & 0x00FFFFFF) | 0x28000000;
            graphics.fill(left, top, right + 1, bottom + 1, fill);
            BattleUiTheme.outline(graphics, left, top, right + 1, bottom + 1, color);
        }
    }

    private void renderAddonAreaAnnotations(GuiGraphics graphics, ResourceLocation dimension,
                                            List<TacticalMapAreaOverlay> areas) {
        for (TacticalMapAreaOverlay area : areas) {
            if (!area.dimension().equals(dimension)) {
                continue;
            }
            int left = Math.min(worldToScreenX(area.minX()), worldToScreenX(area.maxX()));
            int right = Math.max(worldToScreenX(area.minX()), worldToScreenX(area.maxX()));
            int top = Math.min(worldToScreenY(area.minZ()), worldToScreenY(area.maxZ()));
            int bottom = Math.max(worldToScreenY(area.minZ()), worldToScreenY(area.maxZ()));
            if (right < mapLeft || left > mapRight || bottom < mapTop || top > mapBottom) {
                continue;
            }
            int color = area.locked() ? TacticalBoardTheme.MUTED_TEXT : area.color();
            int centerX = (left + right) / 2;
            int centerY = (top + bottom) / 2;
            int availableLabelWidth = Math.max(mapPhysicalToLogical(18),
                    right - left - mapPhysicalToLogical(8));
            String label = fittedMapText(area.label().getString(), availableLabelWidth);
            int labelWidth = mapTextLogicalWidth(label);
            int labelHeight = mapTextLogicalHeight();
            int horizontalPadding = mapPhysicalToLogical(4);
            int verticalPadding = mapPhysicalToLogical(3);
            int labelLeft = centerX - labelWidth / 2;
            int labelTop = centerY - labelHeight / 2;
            graphics.fill(labelLeft - horizontalPadding, labelTop - verticalPadding,
                    labelLeft + labelWidth + horizontalPadding,
                    labelTop + labelHeight + verticalPadding, 0xE4141C1E);
            drawMapString(graphics, label, labelLeft, labelTop, color);

            int minimumBarWidth = mapPhysicalToLogical(24);
            int maximumBarWidth = mapPhysicalToLogical(84);
            int barWidth = Math.max(minimumBarWidth,
                    Math.min(maximumBarWidth,
                            Math.max(minimumBarWidth, right - left - mapPhysicalToLogical(12))));
            int barLeft = centerX - barWidth / 2;
            int barTop = labelTop + labelHeight + verticalPadding + mapPhysicalToLogical(3);
            int barHeight = mapPhysicalToLogical(5);
            graphics.fill(barLeft, barTop, barLeft + barWidth,
                    barTop + barHeight, 0xD920292B);
            graphics.fill(barLeft, barTop,
                    barLeft + (int) Math.round(barWidth * area.progress()),
                    barTop + barHeight, color);
            frameLabels.add(new TacticalMapLayout.Rect(
                    Math.min(labelLeft - horizontalPadding, barLeft),
                    labelTop - verticalPadding,
                    Math.max(labelLeft + labelWidth + horizontalPadding, barLeft + barWidth),
                    barTop + barHeight));
        }
    }

    /**
     * Markers of this map, in the server's order: on the current dimension and on a visible layer.
     * The snapshot is already merged and capped (reserved intel and manual quotas), so the map never
     * trims, reorders or de-duplicates it.
     */
    private List<TacticalMarker> visibleMarkers(ResourceLocation dimension) {
        List<TacticalMarker> visible = new ArrayList<>();
        for (TacticalMarker marker : ClientBattleState.activeMarkers()) {
            if (marker.dimension().equals(dimension) && layerVisible(marker.type())) {
                visible.add(marker);
            }
        }
        return visible;
    }

    /** Attack-order lines; their start icons are drawn with the other marker icons, above them. */
    private void renderAttackDirectionLines(GuiGraphics graphics, List<TacticalMarker> markers) {
        for (TacticalMarker marker : markers) {
            if (marker.type() != TacticalMarkerType.ATTACK_DIRECTION) {
                continue;
            }
            renderAttackArrow(graphics,
                    worldToScreenX(marker.x()), worldToScreenY(marker.z()),
                    worldToScreenX(marker.endX()), worldToScreenY(marker.endZ()),
                    markerColor(marker.type()), marker.id().equals(selectedMarkerId));
        }
    }

    /**
     * Squad-style marker icons ({@link TacticalMapIcons}): enemy circles and order squares on their
     * spot, an attack order's icon on its start (only while the start is on the map). The hovered
     * and the selected marker are drawn last so their rings stay on top; a marker about to expire is
     * faded.
     */
    private void renderMarkerIcons(GuiGraphics graphics, List<TacticalMarker> markers,
                                   UUID hovered, int artPx) {
        long now = ClientBattleState.estimatedServerTimeMillis();
        TacticalMarker hoveredMarker = null;
        TacticalMarker selected = null;
        for (TacticalMarker marker : markers) {
            if (marker.id().equals(selectedMarkerId)) {
                selected = marker;
            } else if (marker.id().equals(hovered)) {
                hoveredMarker = marker;
            } else {
                renderMarkerIcon(graphics, marker, TacticalMapIcons.IconState.NORMAL, artPx, now);
            }
        }
        if (hoveredMarker != null) {
            renderMarkerIcon(graphics, hoveredMarker, TacticalMapIcons.IconState.HOVER, artPx, now);
        }
        if (selected != null) {
            renderMarkerIcon(graphics, selected, TacticalMapIcons.IconState.SELECTED, artPx, now);
        }
    }

    private void renderMarkerIcon(GuiGraphics graphics, TacticalMarker marker,
                                  TacticalMapIcons.IconState state, int artPx, long now) {
        double x = screenX(marker.x());
        double y = screenY(marker.z());
        if (!insideMap(x, y)) {
            return;
        }
        TacticalMapIcons.draw(graphics, TacticalMapIcons.MapIcon.of(marker.type()),
                (float) x, (float) y, artPx, state,
                TacticalMapIcons.alpha(marker.type(), marker.createdAtMillis(),
                        marker.expiresAtMillis(), now));
    }

    /**
     * Marker-tool keys show their marker's map icon: 2 physical pixels per art pixel, as on the
     * map, when the key keeps {@link #TOOL_ICON_MARGIN_PHYSICAL} above and below the plate, else 1.
     */
    private void renderCompactMarkerToolIcons(GuiGraphics graphics) {
        if (markerToolIconSlots.isEmpty()) {
            return;
        }
        double scale = mapGuiScale();
        for (MarkerToolIconSlot slot : markerToolIconSlots) {
            TacticalMapIcons.draw(graphics,
                    TacticalMapIcons.MapIcon.of(markerType(slot.tool())),
                    slot.centerX(), slot.centerY(), toolIconArtPx(slot.keyHeight(), scale),
                    TacticalMapIcons.IconState.NORMAL);
        }
    }

    /** Physical pixels per art pixel of a marker-tool icon in a key {@code keyHeight} GUI tall. */
    static int toolIconArtPx(int keyHeight, double guiScale) {
        double physical = keyHeight * (Double.isFinite(guiScale) && guiScale > 0.0D
                ? guiScale : 1.0D);
        int mapSize = TacticalMapIcons.Plate.CIRCLE.height()
                * TacticalMapIcons.BASE_PHYSICAL_PER_ART;
        return physical >= mapSize + 2 * TOOL_ICON_MARGIN_PHYSICAL
                ? TacticalMapIcons.BASE_PHYSICAL_PER_ART : 1;
    }

    /**
     * Attack order being placed: the recorded start wears the attack-direction icon (also while
     * the cursor is off the map), the preview line runs from it to the cursor.
     */
    private void renderAttackPreview(GuiGraphics graphics, ResourceLocation dimension,
                                     int mouseX, int mouseY, int artPx) {
        if (selectedTool != BattleClientActions.MarkerTool.ATTACK_DIRECTION
                || attackStart == null || !attackStart.dimension().equals(dimension)) {
            return;
        }
        if (insideMap(mouseX, mouseY)) {
            renderAttackArrow(graphics,
                    worldToScreenX(attackStart.x()), worldToScreenY(attackStart.z()),
                    mouseX, mouseY, BattleUiTheme.ACCENT, false);
        }
        double x = screenX(attackStart.x());
        double y = screenY(attackStart.z());
        if (insideMap(x, y)) {
            TacticalMapIcons.draw(graphics, TacticalMapIcons.MapIcon.ATTACK_DIRECTION,
                    (float) x, (float) y, artPx, TacticalMapIcons.IconState.NORMAL);
        }
    }

    private void renderSupportMissions(GuiGraphics graphics, BattleSnapshot snapshot,
                                       ResourceLocation dimension) {
        long now = ClientBattleState.estimatedSupportGameTick();
        List<SupportMissionView> missions = snapshot.support().activeMissions().stream()
                .filter(mission -> mission.dimension().equals(dimension))
                .filter(mission -> supportOption(snapshot, mission.supportId()) != null)
                .toList();
        // Every footprint first, then every label, so a later mission's area or dashed zone is
        // never drawn across an earlier mission's card.
        for (SupportMissionView mission : missions) {
            SupportOptionView option = supportOption(snapshot, mission.supportId());
            int accent = supportColor(option);
            renderSupportGeometry(graphics, option,
                    mission.startX(), mission.startZ(), mission.endX(), mission.endZ(),
                    withAlpha(accent, SUPPORT_ACTIVE_AREA_ALPHA), accent,
                    supportPresentation(option), supportGuidanceZone(option));
        }
        for (SupportMissionView mission : missions) {
            SupportOptionView option = supportOption(snapshot, mission.supportId());
            TacticalSupportMapPresentation presentation = supportPresentation(option);
            SupportGuidanceZone guidance = supportGuidanceZone(option);
            int accent = supportColor(option);
            long remainingTicks = Math.max(0L, mission.executeAtGameTick() - now);
            Component state = remainingTicks > 0L
                    ? Component.translatable(
                            "screen.wok_infantry.map.support.mission.inbound").copy()
                    .append(" " + ((remainingTicks + 19L) / 20L))
                    .append(Component.translatable(
                            "screen.wok_infantry.map.support.status.seconds_suffix"))
                    : Component.translatable(
                            "screen.wok_infantry.map.support.mission.active");
            renderSupportMissionStatus(graphics, option, mission, presentation, guidance,
                    state.getString(), accent);
        }
    }

    private void renderSupportMissionStatus(GuiGraphics graphics, SupportOptionView option,
                                            SupportMissionView mission,
                                            TacticalSupportMapPresentation presentation,
                                            SupportGuidanceZone guidance,
                                            String state, int accent) {
        StringBuilder detail = new StringBuilder(state);
        if (presentation == TacticalSupportMapPresentation.OFFENSIVE) {
            detail.append(" · ").append(Component.translatable(
                    "screen.wok_infantry.map.support.impact_radius",
                    displayRadius(option.radius())).getString());
        } else if (presentation == TacticalSupportMapPresentation.INTELLIGENCE) {
            detail.append(" · ").append(Component.translatable(
                    "screen.wok_infantry.map.support.scan_radius",
                    displayRadius(option.radius())).getString());
        }
        if (guidance != null && !guidance.separateLabel()) {
            detail.append(" · ").append(guidance.label());
        }
        renderSupportGuidanceLabel(graphics, worldToScreenX(mission.startX()),
                worldToScreenY(mission.startZ()), guidance);
        int centerX = worldToScreenX((mission.startX() + mission.endX()) * 0.5D);
        int centerY = worldToScreenY((mission.startZ() + mission.endZ()) * 0.5D);
        renderMapStatusCard(graphics, supportShortName(option).getString(),
                detail.toString(), centerX,
                centerY - supportLabelOffset(option, guidance, (mapStatusCardHeight() + 1) / 2),
                accent);
    }

    private void renderSupportPreview(GuiGraphics graphics, ResourceLocation dimension,
                                      int mouseX, int mouseY) {
        if (selectedSupport == null || !insideMap(mouseX, mouseY)) {
            return;
        }
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        SupportOptionView option = snapshot == null
                ? null : supportOption(snapshot, selectedSupport);
        if (option == null) {
            return;
        }
        TacticalSupportMapPresentation presentation = supportPresentation(option);
        WorldPoint cursor = screenToWorld(mouseX, mouseY);
        if (!cursor.dimension().equals(dimension)) {
            return;
        }
        int accent = supportColor(option);
        SupportGuidanceZone guidance = supportGuidanceZone(option);
        int cursorX = worldToScreenX(cursor.x());
        int cursorY = worldToScreenY(cursor.z());
        if (!option.directional()) {
            renderSupportGeometry(graphics, option,
                    cursor.x(), cursor.z(), cursor.x(), cursor.z(),
                    withAlpha(accent, SUPPORT_AREA_ALPHA), accent, presentation, guidance);
            renderSupportGuidanceLabel(graphics, cursorX, cursorY, guidance);
            renderSupportPreviewRange(graphics, option, presentation, guidance,
                    mouseX, mouseY, accent);
            return;
        }
        if (supportStart == null || !supportStart.dimension().equals(dimension)) {
            renderSupportPointArea(graphics, cursorX, cursorY, supportRadiusPixels(option),
                    withAlpha(accent, SUPPORT_AREA_ALPHA), accent, presentation);
            renderSupportGuidanceRing(graphics, cursorX, cursorY, guidance);
            drawMapDiamond(graphics, cursorX, cursorY, 3, accent);
            renderSupportGuidanceLabel(graphics, cursorX, cursorY, guidance);
            renderSupportPreviewRange(graphics, option, presentation, guidance,
                    mouseX, mouseY, accent);
            return;
        }
        boolean valid = isValidSupportDirectionGeometry(supportStart.x(), supportStart.z(),
                cursor.x(), cursor.z());
        int previewColor = valid ? accent : TacticalBoardTheme.DANGER;
        renderSupportGeometry(graphics, option,
                supportStart.x(), supportStart.z(), cursor.x(), cursor.z(),
                withAlpha(previewColor, SUPPORT_AREA_ALPHA), previewColor, presentation,
                guidance);
        renderSupportGuidanceLabel(graphics, worldToScreenX(supportStart.x()),
                worldToScreenY(supportStart.z()), guidance);
        renderSupportPreviewRange(graphics, option, presentation, guidance,
                (worldToScreenX(supportStart.x()) + mouseX) / 2,
                (worldToScreenY(supportStart.z()) + mouseY) / 2, previewColor);
    }

    private void renderSupportPreviewRange(GuiGraphics graphics, SupportOptionView option,
                                           TacticalSupportMapPresentation presentation,
                                           SupportGuidanceZone guidance,
                                           int centerX, int centerY, int accent) {
        // A support area too small for a separate zone tag folds the zone label into this one
        // so zooming out never drops the authorization radius from the preview.
        String guidanceLabel = guidance != null && !guidance.separateLabel()
                ? guidance.label() : null;
        String label;
        if (presentation == TacticalSupportMapPresentation.OFFENSIVE) {
            label = Component.translatable(
                    "screen.wok_infantry.map.support.impact_radius",
                    displayRadius(option.radius())).getString();
            if (guidanceLabel != null) {
                label = label + " · " + guidanceLabel;
            }
        } else if (guidanceLabel != null) {
            label = guidanceLabel;
        } else {
            return;
        }
        renderMapStatusTag(graphics, label, centerX,
                centerY - supportLabelOffset(option, guidance, (mapStatusTagHeight() + 1) / 2),
                accent);
    }

    private void renderSupportGeometry(GuiGraphics graphics, SupportOptionView option,
                                       double startX, double startZ,
                                       double endX, double endZ,
                                       int fillColor, int outlineColor,
                                       TacticalSupportMapPresentation presentation,
                                       SupportGuidanceZone guidance) {
        renderSupportFootprint(graphics, option, startX, startZ, endX, endZ,
                fillColor, outlineColor, presentation);
        // Designations are validated around the support's target anchor, which is the start
        // point for both point and directional calls.
        renderSupportGuidanceRing(graphics, worldToScreenX(startX), worldToScreenY(startZ),
                guidance);
    }

    private void renderSupportFootprint(GuiGraphics graphics, SupportOptionView option,
                                        double startX, double startZ,
                                        double endX, double endZ,
                                        int fillColor, int outlineColor,
                                        TacticalSupportMapPresentation presentation) {
        int screenStartX = worldToScreenX(startX);
        int screenStartY = worldToScreenY(startZ);
        int radius = supportRadiusPixels(option);
        if (!option.directional()) {
            renderSupportPointArea(graphics, screenStartX, screenStartY,
                    radius, fillColor, outlineColor, presentation);
            if (presentation == TacticalSupportMapPresentation.OFFENSIVE) {
                renderImpactCrosshair(graphics, screenStartX, screenStartY, outlineColor);
            } else {
                drawMapDiamond(graphics, screenStartX, screenStartY, 4, outlineColor);
            }
            return;
        }
        int screenEndX = worldToScreenX(endX);
        int screenEndY = worldToScreenY(endZ);
        if (presentation == TacticalSupportMapPresentation.OFFENSIVE) {
            renderOffensiveCorridor(graphics, screenStartX, screenStartY,
                    screenEndX, screenEndY, radius, outlineColor);
            return;
        }
        drawLine(graphics, screenStartX, screenStartY, screenEndX, screenEndY,
                fillColor, Math.max(3, radius * 2 + 1));
        drawLine(graphics, screenStartX, screenStartY, screenEndX, screenEndY,
                outlineColor, 2);
        drawMapDiamond(graphics, screenStartX, screenStartY, 3, outlineColor);
        drawMapDiamond(graphics, screenEndX, screenEndY, 4, outlineColor);
        drawMapArrow(graphics, screenStartX, screenStartY,
                screenEndX, screenEndY, outlineColor, 2);
    }

    private void renderSupportPointArea(GuiGraphics graphics, int centerX, int centerY,
                                        int radius, int fillColor, int outlineColor,
                                        TacticalSupportMapPresentation presentation) {
        if (presentation == TacticalSupportMapPresentation.OFFENSIVE) {
            renderOffensiveImpactArea(graphics, centerX, centerY, radius, outlineColor);
            return;
        }
        renderSupportDisc(graphics, centerX, centerY, radius, fillColor, outlineColor);
    }

    private void renderSupportDisc(GuiGraphics graphics, int centerX, int centerY,
                                   int radius, int fillColor, int outlineColor) {
        int safeRadius = Math.max(2, radius);
        long radiusSquared = (long) safeRadius * safeRadius;
        for (int offsetY = -safeRadius; offsetY <= safeRadius; offsetY++) {
            int halfWidth = (int) Math.floor(Math.sqrt(Math.max(0L,
                    radiusSquared - (long) offsetY * offsetY)));
            graphics.fill(centerX - halfWidth, centerY + offsetY,
                    centerX + halfWidth + 1, centerY + offsetY + 1, fillColor);
            graphics.fill(centerX - halfWidth, centerY + offsetY,
                    centerX - halfWidth + 1, centerY + offsetY + 1, outlineColor);
            graphics.fill(centerX + halfWidth, centerY + offsetY,
                    centerX + halfWidth + 1, centerY + offsetY + 1, outlineColor);
        }
    }

    private void renderOffensiveImpactArea(GuiGraphics graphics, int centerX, int centerY,
                                           int radius, int outlineColor) {
        int safeRadius = Math.max(3, radius);
        renderSupportDisc(graphics, centerX, centerY, safeRadius,
                withAlpha(outlineColor, OFFENSIVE_SUPPORT_FILL_ALPHA), outlineColor);
        renderImpactHatching(graphics, centerX, centerY, safeRadius,
                withAlpha(outlineColor, OFFENSIVE_SUPPORT_HATCH_ALPHA));
        renderImpactCrosshair(graphics, centerX, centerY, outlineColor);
    }

    private void renderImpactHatching(GuiGraphics graphics, int centerX, int centerY,
                                      int radius, int color) {
        int spacing = Math.max(3, mapPhysicalToLogical(10));
        int diagonalLimit = (int) Math.floor(radius * Math.sqrt(2.0D));
        for (int diagonal = -diagonalLimit; diagonal <= diagonalLimit; diagonal += spacing) {
            double discriminant = 2.0D * radius * radius - (double) diagonal * diagonal;
            if (discriminant < 0.0D) {
                continue;
            }
            double root = Math.sqrt(discriminant);
            int startX = (int) Math.ceil((-diagonal - root) * 0.5D);
            int endX = (int) Math.floor((-diagonal + root) * 0.5D);
            drawLine(graphics, centerX + startX, centerY + startX + diagonal,
                    centerX + endX, centerY + endX + diagonal, color, 1);
        }
    }

    private void renderOffensiveCorridor(GuiGraphics graphics,
                                         int startX, int startY, int endX, int endY,
                                         int radius, int outlineColor) {
        int safeRadius = Math.max(3, radius);
        int fill = withAlpha(outlineColor, OFFENSIVE_SUPPORT_FILL_ALPHA);
        drawLine(graphics, startX, startY, endX, endY,
                fill, safeRadius * 2 + 1);
        renderSupportDisc(graphics, startX, startY, safeRadius, fill, outlineColor);
        renderSupportDisc(graphics, endX, endY, safeRadius, fill, outlineColor);

        double dx = endX - startX;
        double dy = endY - startY;
        double length = Math.hypot(dx, dy);
        if (length > 0.001D) {
            int normalX = (int) Math.round(-dy / length * safeRadius);
            int normalY = (int) Math.round(dx / length * safeRadius);
            drawLine(graphics, startX + normalX, startY + normalY,
                    endX + normalX, endY + normalY, outlineColor, 1);
            drawLine(graphics, startX - normalX, startY - normalY,
                    endX - normalX, endY - normalY, outlineColor, 1);
            int stripeSpacing = Math.max(4, mapPhysicalToLogical(14));
            int stripes = Math.max(1, (int) Math.floor(length / stripeSpacing));
            int hatch = withAlpha(outlineColor, OFFENSIVE_SUPPORT_HATCH_ALPHA);
            for (int index = 0; index <= stripes; index++) {
                double progress = (double) index / stripes;
                int centerX = (int) Math.round(startX + dx * progress);
                int centerY = (int) Math.round(startY + dy * progress);
                drawLine(graphics, centerX - normalX, centerY - normalY,
                        centerX + normalX, centerY + normalY, hatch, 1);
            }
        }
        renderImpactCrosshair(graphics, endX, endY, outlineColor);
        drawMapArrow(graphics, startX, startY, endX, endY, outlineColor, 2);
    }

    private void renderImpactCrosshair(GuiGraphics graphics, int centerX, int centerY,
                                       int color) {
        int arm = impactCrosshairArm();
        int gap = Math.max(1, mapPhysicalToLogical(3));
        drawLine(graphics, centerX - arm, centerY, centerX - gap, centerY, color, 1);
        drawLine(graphics, centerX + gap, centerY, centerX + arm, centerY, color, 1);
        drawLine(graphics, centerX, centerY - arm, centerX, centerY - gap, color, 1);
        drawLine(graphics, centerX, centerY + gap, centerX, centerY + arm, color, 1);
        drawMapDiamond(graphics, centerX, centerY, 2, color);
    }

    private int impactCrosshairArm() {
        return Math.max(4, mapPhysicalToLogical(9));
    }

    private SupportGuidanceZone supportGuidanceZone(SupportOptionView option) {
        OptionalDouble radius = TacticalSupportMapPresentationRegistry.guidanceRadius(
                option.id());
        if (radius.isEmpty()) {
            return null;
        }
        int areaPixels = supportRadiusPixels(option);
        int ringPixels = supportGuidanceRadiusPixels(radius.getAsDouble(), option.radius(),
                areaPixels, zoom);
        String label = Component.translatable(
                "screen.wok_infantry.map.support.guidance_radius",
                displayRadius(radius.getAsDouble())).getString();
        // The zone tag sits wholly below both rings, so its opaque panel never covers the dashed
        // inner ring or the outer outline.
        int labelHalfHeight = (mapStatusTagHeight() + 1) / 2;
        int labelOffset = supportGuidanceLabelOffset(areaPixels, ringPixels,
                mapPhysicalToLogical(SUPPORT_GUIDANCE_CASING_PHYSICAL / 2),
                mapPhysicalToLogical(4), labelHalfHeight);
        int centerClearance = impactCrosshairArm() + mapPhysicalToLogical(4);
        return new SupportGuidanceZone(ringPixels, labelOffset, label,
                supportGuidanceLabelClearsTarget(labelOffset, labelHalfHeight,
                        centerClearance));
    }

    /**
     * Centre offset above the target for a mission card or range tag: like the zone tag below,
     * the panel sits wholly outside the area outline and the dashed zone, never across them.
     */
    private int supportLabelOffset(SupportOptionView option, SupportGuidanceZone guidance,
                                   int labelHalfHeight) {
        return supportGuidanceLabelOffset(supportRadiusPixels(option),
                guidance == null ? 0 : guidance.ringPixels(),
                mapPhysicalToLogical(SUPPORT_GUIDANCE_CASING_PHYSICAL / 2),
                mapPhysicalToLogical(4), labelHalfHeight);
    }

    private void renderSupportGuidanceRing(GuiGraphics graphics, int centerX, int centerY,
                                           SupportGuidanceZone guidance) {
        // At strategic zoom a ring inside the target crosshair is noise; the zone label still
        // reports the radius through its own tag or the folded range text.
        if (guidance == null || guidance.ringPixels() <= impactCrosshairArm()) {
            return;
        }
        int dash = Math.max(2, mapPhysicalToLogical(SUPPORT_GUIDANCE_DASH_PHYSICAL));
        int gap = Math.max(1, mapPhysicalToLogical(SUPPORT_GUIDANCE_GAP_PHYSICAL));
        int dashes = supportGuidanceDashCount(guidance.ringPixels(), dash + gap);
        if (dashes <= 0) {
            return;
        }
        double step = Math.PI * 2.0D / dashes;
        double sweep = step * dash / (dash + gap);
        // Casing first for every dash, so neighbouring casings never cover an amber dash.
        renderSupportGuidanceDashes(graphics, centerX, centerY, guidance.ringPixels(),
                dashes, step, sweep, SUPPORT_GUIDANCE_CASING,
                SUPPORT_GUIDANCE_CASING_PHYSICAL);
        renderSupportGuidanceDashes(graphics, centerX, centerY, guidance.ringPixels(),
                dashes, step, sweep, SUPPORT_GUIDANCE_COLOR,
                SUPPORT_GUIDANCE_LINE_PHYSICAL);
    }

    private void renderSupportGuidanceDashes(GuiGraphics graphics, int centerX, int centerY,
                                             int radius, int dashes,
                                             double step, double sweep,
                                             int color, int physicalThickness) {
        for (int index = 0; index < dashes; index++) {
            double from = index * step - Math.PI * 0.5D;
            double to = from + sweep;
            drawMapWorldLine(graphics,
                    centerX + (int) Math.round(Math.cos(from) * radius),
                    centerY + (int) Math.round(Math.sin(from) * radius),
                    centerX + (int) Math.round(Math.cos(to) * radius),
                    centerY + (int) Math.round(Math.sin(to) * radius),
                    color, physicalThickness);
        }
    }

    private void renderSupportGuidanceLabel(GuiGraphics graphics, int centerX, int centerY,
                                            SupportGuidanceZone guidance) {
        if (guidance == null || !guidance.separateLabel()) {
            return;
        }
        renderMapStatusTag(graphics, guidance.label(), centerX,
                centerY + guidance.labelOffset(), SUPPORT_GUIDANCE_COLOR);
    }

    /**
     * Converts a guidance radius to map pixels in proportion to the drawn outer area, so the
     * inner zone keeps its true ratio even when the outer area is clamped at extreme zoom.
     */
    static int supportGuidanceRadiusPixels(double guidanceRadius, double supportRadius,
                                           int supportRadiusPixels, double mapZoom) {
        if (!Double.isFinite(guidanceRadius) || guidanceRadius <= 0.0D) {
            return 0;
        }
        double pixels;
        if (Double.isFinite(supportRadius) && supportRadius > 0.0D
                && supportRadiusPixels > 0) {
            pixels = supportRadiusPixels * guidanceRadius / supportRadius;
        } else if (Double.isFinite(mapZoom) && mapZoom > 0.0D) {
            pixels = guidanceRadius * mapZoom;
        } else {
            return 0;
        }
        return (int) Math.max(0L, Math.min(SUPPORT_RADIUS_MAX_PIXELS, Math.round(pixels)));
    }

    /**
     * Centre offset from the target for a label panel outside the support area: the panel's near
     * edge keeps {@code gap} beyond whichever reaches further of the outer area outline and the
     * dashed inner ring including its casing half-width, so the label never covers either ring.
     * The zone tag uses it below the target; mission cards and range tags above it.
     */
    static int supportGuidanceLabelOffset(int areaPixels, int ringPixels, int ringHalfWidth,
                                          int gap, int labelHalfHeight) {
        int ringEdge = Math.max(0, ringPixels) + Math.max(0, ringHalfWidth);
        return Math.max(Math.max(0, areaPixels), ringEdge) + Math.max(0, gap)
                + Math.max(0, labelHalfHeight);
    }

    /**
     * A separate zone tag centred {@code labelOffset} below the target must not cover the
     * target crosshair; otherwise its label is folded into the range tag or mission card.
     */
    static boolean supportGuidanceLabelClearsTarget(int labelOffset, int labelHalfHeight,
                                                    int centerClearance) {
        return labelOffset - Math.max(0, labelHalfHeight) >= Math.max(0, centerClearance);
    }

    static int supportGuidanceDashCount(int ringPixels, int dashPeriodPixels) {
        if (ringPixels < SUPPORT_GUIDANCE_MIN_RING_PIXELS) {
            return 0;
        }
        long dashes = Math.round(2.0D * Math.PI * ringPixels / Math.max(2, dashPeriodPixels));
        return (int) Math.max(SUPPORT_GUIDANCE_MIN_DASHES,
                Math.min(SUPPORT_GUIDANCE_MAX_DASHES, dashes));
    }

    private int supportRadiusPixels(SupportOptionView option) {
        return Math.max(3, Math.min(SUPPORT_RADIUS_MAX_PIXELS,
                (int) Math.round(option.radius() * zoom)));
    }

    private static int displayRadius(double radius) {
        return Math.max(0, (int) Math.round(radius));
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | color & 0x00FFFFFF;
    }

    private void renderAttackArrow(GuiGraphics graphics, int startX, int startY,
                                   int endX, int endY, int color, boolean selected) {
        TacticalMapSegmentClipper.ClippedSegment clipped = TacticalMapSegmentClipper.clip(
                startX, startY, endX, endY,
                mapLeft, mapTop, mapRight - 1.0D, mapBottom - 1.0D);
        if (clipped == null) {
            return;
        }
        int visibleStartX = (int) Math.round(clipped.startX());
        int visibleStartY = (int) Math.round(clipped.startY());
        int visibleEndX = (int) Math.round(clipped.endX());
        int visibleEndY = (int) Math.round(clipped.endY());
        if (selected) {
            // The same yellow as the selection ring on the order's start icon.
            int highlight = TacticalMapIcons.SELECTED_RING;
            drawMapArrow(graphics, visibleStartX, visibleStartY,
                    visibleEndX, visibleEndY, highlight, 4);
            drawMapDiamond(graphics, visibleStartX, visibleStartY, 3, highlight);
            drawMapDiamond(graphics, visibleEndX, visibleEndY, 3, highlight);
        }
        drawMapArrow(graphics, visibleStartX, visibleStartY,
                visibleEndX, visibleEndY, color, 2);
    }

    private void renderSidebar(GuiGraphics graphics, BattleSnapshot snapshot) {
        TacticalMapLayout.Rect sidebar = boardLayout.sidebar();
        TacticalBoardTheme.raisedPanel(graphics, sidebar.left(), sidebar.top(),
                sidebar.right(), sidebar.bottom(), TacticalBoardTheme.BOARD_ALT);
        int innerLeft = sidebar.left() + 6;
        int innerRight = sidebar.right() - 6;
        TacticalMarker selected = selectedMarker();

        if (selectionReplacesSidebar()) {
            renderSelectedMarkerCard(graphics, selected, innerLeft, innerRight,
                    selectedDetailTop(), sidebar.bottom() - 6);
            return;
        }

        if (richBoard) {
            TacticalBoardTheme.sectionHeader(graphics, font,
                    Component.translatable("screen.wok_infantry.map.operation_status").getString(),
                    innerLeft, mapTop, innerRight, TacticalBoardTheme.FRIENDLY);
            renderOperationSummary(graphics, snapshot, innerLeft, innerRight,
                    mapTop + 17, boardLayout.layerTitleY() - 6);
        }

        TacticalBoardTheme.sectionHeader(graphics, font,
                Component.translatable("screen.wok_infantry.map.layers").getString(),
                innerLeft, boardLayout.layerTitleY(), innerRight,
                TacticalBoardTheme.SELECTED);
        if (richBoard && snapshot.permissions().canCreateMarkers()) {
            TacticalBoardTheme.sectionHeader(graphics, font,
                    Component.translatable(
                            "screen.wok_infantry.map.marker_tools").getString(),
                    innerLeft, boardLayout.markerTitleY(), innerRight,
                    TacticalBoardTheme.ACCENT);
            if (compactTools) {
                drawSidebarHint(graphics,
                        Component.translatable("screen.wok_infantry.map.tools_below"),
                        boardLayout.markerTitleY() + 17,
                        TacticalBoardTheme.MUTED_TEXT);
            }
        } else if (richBoard) {
            drawSidebarHint(graphics,
                    Component.translatable("screen.wok_infantry.map.read_only"),
                    boardLayout.markerTitleY() + 17,
                    TacticalBoardTheme.MUTED_TEXT);
        }

        if (richBoard) {
            TacticalMapLayout.Rect target = TacticalMapLayout.selectedTargetRegion(boardLayout);
            renderSelectedMarkerCard(graphics, selected, target.left(), target.right(),
                    target.top(), target.bottom());
            renderSupportPanel(graphics, snapshot);
        } else if (sidebarToolMode == SidebarToolMode.SUPPORT) {
            renderSupportPanel(graphics, snapshot);
        } else if (selected != null) {
            int detailTop = selectedDetailTop();
            if (detailTop + 48 < sidebar.bottom() - 6) {
                renderSelectedMarkerCard(graphics, selected, innerLeft, innerRight,
                        detailTop, sidebar.bottom() - 6);
            }
        }
    }

    private void renderSupportPanel(GuiGraphics graphics, BattleSnapshot snapshot) {
        if (compactTools) {
            if (snapshot.support().options().isEmpty()) {
                TacticalMapLayout.Rect body = TacticalMapLayout.supportBody(boardLayout);
                renderSupportEmptyState(graphics, snapshot, body, true);
            }
            return;
        }
        TacticalMapLayout.Rect region = TacticalMapLayout.supportRegion(boardLayout);
        TacticalBoardTheme.sectionHeader(graphics, font,
                Component.translatable("screen.wok_infantry.map.support.module"),
                region.left(), region.top(), region.right(), TacticalBoardTheme.ACCENT);
        TacticalMapLayout.Rect body = TacticalMapLayout.supportBody(boardLayout);
        if (body.height() <= 0) {
            return;
        }
        TacticalBoardTheme.insetPanel(graphics, body.left(), body.top(),
                body.right(), body.bottom(), TacticalBoardTheme.CARD);
        for (int y = body.top() + 18; y < body.bottom() - 3; y += 18) {
            graphics.fill(body.left() + 5, y, body.right() - 5, y + 1, 0x3052605E);
        }

        List<SupportOptionView> options = snapshot.support().options();
        if (options.isEmpty()) {
            renderSupportEmptyState(graphics, snapshot, body, false);
            return;
        }

        int fromIndex = Math.min(options.size(), supportPage * SUPPORT_PAGE_SIZE);
        int visibleCount = Math.min(SUPPORT_PAGE_SIZE, options.size() - fromIndex);
        TacticalMapLayout.Rect summaryBounds = TacticalMapLayout.supportSummary(
                boardLayout, visibleCount);
        if (summaryBounds.width() <= 0 || summaryBounds.height() <= 0) {
            return;
        }
        Component summary;
        int color;
        boolean activeSummary = false;
        if (!snapshot.commander()) {
            summary = Component.translatable(
                    "screen.wok_infantry.map.support.commander_locked");
            color = TacticalBoardTheme.MUTED_TEXT;
        } else if (snapshot.deployment().phase() != DeploymentPhase.ACTIVE) {
            summary = Component.translatable(
                    "screen.wok_infantry.map.support.status.waiting_deployment");
            color = TacticalBoardTheme.MUTED_TEXT;
        } else if (!snapshot.support().activeMissions().isEmpty()) {
            summary = Component.translatable(
                    "screen.wok_infantry.map.support.mission.active").copy()
                    .append("  " + snapshot.support().activeMissions().size());
            activeSummary = true;
            color = snapshot.support().activeMissions().stream()
                    .map(mission -> supportOption(snapshot, mission.supportId()))
                    .filter(Objects::nonNull)
                    .anyMatch(option -> supportPresentation(option)
                            == TacticalSupportMapPresentation.OFFENSIVE)
                    ? TacticalBoardTheme.DANGER : TacticalBoardTheme.ACCENT;
        } else {
            summary = Component.translatable(
                    "screen.wok_infantry.map.support.select_hint");
            color = TacticalBoardTheme.MUTED_TEXT;
        }
        if (activeSummary) {
            renderActiveSupportSummary(graphics, summary, summaryBounds, color);
        } else {
            int summaryY = summaryBounds.top()
                    + Math.max(1, (summaryBounds.height() - font.lineHeight) / 2);
            graphics.drawString(font, fittedText(summary,
                            Math.max(20, summaryBounds.width())),
                    summaryBounds.left(), summaryY, color, false);
        }

        int pageCount = supportPageCount(options.size());
        if (pageCount > 1) {
            TacticalMapLayout.Rect pager = TacticalMapLayout.supportPagerButton(
                    boardLayout, false);
            if (pager.width() > 0 && pager.height() > 0) {
                Component page = Component.literal((supportPage + 1) + " / " + pageCount);
                int pageX = body.left() + (body.width() - font.width(page)) / 2;
                int pageY = pager.top() + Math.max(1,
                        (pager.height() - font.lineHeight) / 2);
                graphics.drawString(font, page, pageX, pageY,
                        TacticalBoardTheme.MUTED_TEXT, false);
            }
        }
    }

    private void renderActiveSupportSummary(GuiGraphics graphics, Component summary,
                                            TacticalMapLayout.Rect bounds, int accent) {
        int left = bounds.left();
        int top = bounds.top();
        int right = bounds.right();
        int bottom = bounds.bottom();
        graphics.fill(left, top, right, bottom, 0xE51A2021);
        BattleUiTheme.outline(graphics, left, top, right, bottom, accent);
        graphics.fill(left + 1, top + 1, left + 4, bottom - 1, accent);

        float scale = 1.25F;
        int availableWidth = Math.max(1, right - left - 14);
        String visible = fittedText(summary,
                Math.max(1, (int) Math.floor(availableWidth / scale)));
        float renderedWidth = font.width(visible) * scale;
        float renderedHeight = font.lineHeight * scale;
        float textX = left + 6 + Math.max(0.0F,
                (availableWidth - renderedWidth) * 0.5F);
        float textY = top + Math.max(0.0F,
                (bottom - top - renderedHeight) * 0.5F);
        graphics.pose().pushPose();
        graphics.pose().translate(textX, textY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, visible, 0, 0, accent, false);
        graphics.pose().popPose();
    }

    private void renderSupportEmptyState(GuiGraphics graphics, BattleSnapshot snapshot,
                                         TacticalMapLayout.Rect body, boolean centeredCompact) {
        if (body.width() <= 0 || body.height() <= 0) {
            return;
        }
        boolean serviceAvailable = snapshot.support().serviceAvailable();
        Component heading;
        Component detail;
        Component hint;
        int headingColor;
        if (serviceAvailable) {
            heading = Component.translatable(
                    "screen.wok_infantry.map.support.framework_ready");
            detail = Component.translatable("screen.wok_infantry.map.support.empty");
            hint = Component.translatable("screen.wok_infantry.map.support.empty_hint");
            headingColor = TacticalBoardTheme.ACCENT;
        } else {
            heading = Component.translatable(
                    "screen.wok_infantry.map.support.status.provider_missing");
            detail = snapshot.support().serviceMessage().isBlank()
                    ? heading : Component.literal(snapshot.support().serviceMessage());
            hint = Component.empty();
            headingColor = TacticalBoardTheme.DANGER;
        }

        int textWidth = Math.max(20, body.width() - 12);
        if (centeredCompact) {
            BattleUiTheme.drawCenteredText(graphics, font, fittedText(detail, textWidth),
                    body.left() + body.width() / 2,
                    body.top() + Math.max(2, (body.height() - font.lineHeight) / 2),
                    headingColor);
            return;
        }

        int lineStep = font.lineHeight + 8;
        int firstLineY = body.top() + 6;
        int verticalBudget = body.bottom() - 4 - firstLineY - font.lineHeight;
        int lineCapacity = verticalBudget < 0 ? 0 : 1 + verticalBudget / lineStep;
        if (lineCapacity <= 0) {
            return;
        }
        List<Component> lines;
        if (lineCapacity == 1) {
            lines = List.of(detail);
        } else if (lineCapacity == 2 || hint.getString().isBlank()) {
            lines = List.of(heading, detail);
        } else {
            lines = List.of(heading, detail, hint);
        }
        for (int index = 0; index < lines.size(); index++) {
            Component line = lines.get(index);
            int lineColor = index == 0 && lines.size() > 1
                    ? headingColor : TacticalBoardTheme.MUTED_TEXT;
            graphics.drawString(font, fittedText(line, textWidth),
                    body.left() + 6, firstLineY + index * lineStep, lineColor, false);
        }
    }

    private void renderOperationSummary(GuiGraphics graphics, BattleSnapshot snapshot,
                                        int left, int right, int top, int bottom) {
        TacticalBoardTheme.insetPanel(graphics, left, top, right, bottom,
                TacticalBoardTheme.CARD);
        SquadView ownSquad = snapshot.ownSquad() == null ? null
                : snapshot.squads().stream()
                .filter(squad -> squad.callsign() == snapshot.ownSquad())
                .findFirst().orElse(null);
        Component forceValue = Component.literal(snapshot.factionMemberCount()
                + " / " + snapshot.factionCapacity());
        Component squadValue = ownSquad == null
                ? Component.translatable("screen.wok_infantry.status.none")
                : SquadScreen.callsign(ownSquad.callsign()).copy().append("  ")
                .append(Component.literal(ownSquad.members().size() + "/" + ownSquad.capacity()));
        Component authorityValue = Component.translatable(snapshot.commander()
                ? "screen.wok_infantry.map.authority.commander"
                : snapshot.squadLeader()
                ? "screen.wok_infantry.map.authority.squad_leader"
                : "screen.wok_infantry.map.authority.member");
        int y = top + 5;
        drawBoardStatRow(graphics,
                Component.translatable("screen.wok_infantry.map.force_strength"),
                forceValue, left + 6, right - 6, y);
        y += 15;
        drawBoardStatRow(graphics,
                Component.translatable("screen.wok_infantry.map.squad_status"),
                squadValue, left + 6, right - 6, y);
        y += 15;
        drawBoardStatRow(graphics,
                Component.translatable("screen.wok_infantry.map.authority"),
                authorityValue, left + 6, right - 6, y);
        if (y + 15 < bottom - 2) {
            drawBoardStatRow(graphics,
                    Component.translatable("screen.wok_infantry.map.active_markers"),
                    Component.literal(Integer.toString(ClientBattleState.activeMarkers().size())),
                    left + 6, right - 6, y + 15);
        }
    }

    private void drawBoardStatRow(GuiGraphics graphics, Component label, Component value,
                                  int left, int right, int y) {
        graphics.drawString(font, fittedText(label, Math.max(20, (right - left) / 2)),
                left, y, TacticalBoardTheme.MUTED_TEXT, false);
        String visibleValue = fittedText(value, Math.max(20, (right - left) / 2));
        graphics.drawString(font, visibleValue,
                Math.max(left, right - font.width(visibleValue)), y,
                TacticalBoardTheme.TEXT, false);
        graphics.fill(left, y + 11, right, y + 12, 0x4052605E);
    }

    private void renderSelectedMarkerCard(GuiGraphics graphics, TacticalMarker selected,
                                          int left, int right, int top, int bottom) {
        if (bottom - top < 44) {
            return;
        }
        TacticalBoardTheme.sectionHeader(graphics, font,
                Component.translatable("screen.wok_infantry.map.selected_target").getString(),
                left, top, right, selected == null
                        ? TacticalBoardTheme.INSET : markerColor(selected.type()));
        int cardTop = top + 17;
        TacticalBoardTheme.insetPanel(graphics, left, cardTop, right, bottom,
                TacticalBoardTheme.CARD);
        for (int y = cardTop + 44; y < bottom - 3; y += 18) {
            graphics.fill(left + 5, y, right - 5, y + 1, 0x3052605E);
        }
        int textWidth = Math.max(20, right - left - 12);
        if (selected == null) {
            BattleUiTheme.drawCenteredText(graphics, font, "+", (left + right) / 2,
                    cardTop + 9, TacticalBoardTheme.SELECTED);
            List<FormattedCharSequence> lines = font.split(Component.translatable(
                    "screen.wok_infantry.map.no_selection"), textWidth);
            int y = cardTop + 24;
            for (int index = 0; index < Math.min(3, lines.size()); index++) {
                BattleUiTheme.drawCenteredText(graphics, font, lines.get(index),
                        (left + right) / 2, y + index * 10,
                        TacticalBoardTheme.MUTED_TEXT);
            }
            return;
        }

        graphics.drawString(font, fittedText(markerName(selected.type()), textWidth),
                left + 6, cardTop + 5, markerColor(selected.type()), false);
        Component coordinates = Component.literal("X " + (int) selected.x()
                + "   Z " + (int) selected.z());
        graphics.drawString(font, fittedText(coordinates, textWidth),
                left + 6, cardTop + 18, TacticalBoardTheme.TEXT, false);
        Component source = selected.creatorSquad() == null
                ? Component.literal("—") : SquadScreen.callsign(selected.creatorSquad());
        long remainingSeconds = Math.max(0L,
                (selected.expiresAtMillis()
                        - ClientBattleState.estimatedServerTimeMillis() + 999L) / 1_000L);
        Component sourceAndTtl = Component.translatable(
                "screen.wok_infantry.map.marker_source_ttl", source, remainingSeconds);
        graphics.drawString(font, fittedText(sourceAndTtl, textWidth),
                left + 6, cardTop + 31, TacticalBoardTheme.MUTED_TEXT, false);
    }

    private void renderFooter(GuiGraphics graphics, BattleSnapshot snapshot, int mouseX, int mouseY) {
        WorldPoint point = screenToWorld(mouseX, mouseY);
        String coordinateText = insideMap(mouseX, mouseY)
                ? String.format("X %.0f  Z %.0f", point.x(), point.z())
                : String.format("X %.0f  Z %.0f", centerWorldX, centerWorldZ);
        boolean terrainReady = TacticalMapTerrainRegistry.isReady();
        String terrainProvider = TacticalMapTerrainRegistry.activeProviderId();
        Component terrainName = terrainReady
                ? Component.translatableWithFallback(
                        "screen.wok_infantry.map.terrain." + terrainProvider, terrainProvider)
                : Component.translatableWithFallback(
                        "screen.wok_infantry.map.terrain.initializing",
                        "Terrain provider initializing");
        MutableComponent status;
        if (width < 420) {
            Component compactTerrainName = Component.translatableWithFallback(
                    terrainReady
                            ? "screen.wok_infantry.map.terrain." + terrainProvider + "_short"
                            : "screen.wok_infantry.map.terrain.initializing_short",
                    terrainProvider);
            status = Component.literal(coordinateText)
                    .append(" · ").append(String.format("%.2fx", zoom / DEFAULT_ZOOM))
                    .append(" · ").append(Component.translatable(
                            "screen.wok_infantry.map.footer_compact"))
                    .append(" · ").append(compactTerrainName);
        } else {
            status = Component.translatable("screen.wok_infantry.map.footer",
                    coordinateText, String.format("%.2fx", zoom / DEFAULT_ZOOM)).copy()
                    .append(" · ").append(terrainName);
        }
        if (terrainReady) {
            TerrainProgress progress = terrainProgress();
            if (terrainViewportTooLarge) {
                status.append(" · ").append(Component.translatableWithFallback(
                        "screen.wok_infantry.map.terrain.zoom_in",
                        "terrain overview uses grid; zoom in"));
            } else if (progress.total() > 0 && progress.loaded() < progress.total()) {
                String progressKey = progress.failed() > 0
                        ? "screen.wok_infantry.map.terrain.failed"
                        : "screen.wok_infantry.map.terrain.loading";
                String fallback = progress.failed() > 0
                        ? "retrying %s/%s"
                        : "loading %s/%s";
                status.append(" · ").append(Component.translatableWithFallback(
                        progressKey, fallback, progress.loaded(), progress.total()));
            }
        }
        Component footer = status;
        int footerColor = TacticalBoardTheme.MUTED_TEXT;
        if (selectedSupport != null) {
            BattleSnapshot currentSnapshot = ClientBattleState.snapshot();
            SupportOptionView option = currentSnapshot == null
                    ? null : supportOption(currentSnapshot, selectedSupport);
            if (option == null) {
                selectedSupport = null;
                supportStart = null;
            } else if (!option.directional()) {
                footer = Component.translatable(
                        "screen.wok_infantry.map.support.point_hint");
            } else if (supportStart == null) {
                footer = Component.translatable(
                        "screen.wok_infantry.map.support.direction_start_hint");
            } else {
                footer = Component.translatable(
                        "screen.wok_infantry.map.support.direction_finish_hint");
            }
            if (option != null) {
                footerColor = supportColor(option);
            }
        } else if (attackStart != null) {
            footer = Component.translatable("screen.wok_infantry.map.attack_finish_hint");
            footerColor = TacticalBoardTheme.ACCENT;
        }
        TacticalMapLayout.Rect footerBounds = boardLayout.footer();
        Component dimension = Component.literal(currentDimension().toString());
        int dimensionReserve = richBoard ? Math.min(footerBounds.width() / 3,
                font.width(dimension) + 18) : 0;
        int textWidth = Math.max(20, footerBounds.width() - dimensionReserve - 24);
        String visibleFooter = fittedText(footer, textWidth);
        int baseline = footerBounds.top() + Math.max(4, (footerBounds.height() - 8) / 2);
        graphics.fill(footerBounds.left() + 5, baseline + 1,
                footerBounds.left() + 8, baseline + 7,
                TacticalMapTerrainRegistry.isReady()
                        ? TacticalBoardTheme.SUCCESS : TacticalBoardTheme.ACCENT);
        graphics.drawString(font, visibleFooter, footerBounds.left() + 12,
                baseline, footerColor, false);
        if (richBoard) {
            String visibleDimension = fittedText(dimension, dimensionReserve - 8);
            graphics.drawString(font, visibleDimension,
                    footerBounds.right() - font.width(visibleDimension) - 8,
                    baseline, TacticalBoardTheme.TEXT, false);
        }
    }

    private String fittedText(Component text, int maxWidth) {
        int safeWidth = Math.max(1, maxWidth);
        String value = text.getString();
        if (font.width(value) <= safeWidth) {
            return value;
        }
        String ellipsis = "…";
        int bodyWidth = Math.max(1, safeWidth - font.width(ellipsis));
        return font.plainSubstrByWidth(value, bodyWidth) + ellipsis;
    }

    private Component fittedButtonLabel(String regularKey, String shortKey, int buttonWidth) {
        Component regular = Component.translatable(regularKey);
        return font.width(regular) <= Math.max(1, buttonWidth - 6)
                ? regular : Component.translatable(shortKey);
    }

    private void drawSidebarHint(GuiGraphics graphics, Component text, int y, int color) {
        int maxWidth = Math.max(1, width - sidebarLeft - 14);
        int maxLines = Math.max(1, (mapBottom - y - 4) / 10);
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        for (int index = 0; index < Math.min(maxLines, lines.size()); index++) {
            graphics.drawString(font, lines.get(index), sidebarLeft + 7, y + index * 10,
                    color, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        panningMap = false;
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                && (selectedTool != null || attackStart != null
                || selectedSupport != null || supportStart != null)) {
            selectedTool = null;
            attackStart = null;
            selectedSupport = null;
            supportStart = null;
            rebuildWidgets();
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && insideMap(mouseX, mouseY)) {
            panningMap = true;
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !insideMap(mouseX, mouseY)) {
            return false;
        }

        BattleSnapshot snapshot = ClientBattleState.snapshot();
        if (snapshot == null) {
            return false;
        }
        WorldPoint point = screenToWorld(mouseX, mouseY);
        if (selectedSupport != null) {
            ResourceLocation requestedId = selectedSupport;
            SupportOptionView requestedOption = supportOption(snapshot, requestedId);
            if (!supportUsable(snapshot, requestedId) || requestedOption == null) {
                ClientBattleState.showFeedback(false, Component.translatable(
                        "message.wok_infantry.support_unavailable").getString());
                selectedSupport = null;
                supportStart = null;
                rebuildWidgets();
                return true;
            }
            if (requestedOption.directional()) {
                if (supportStart == null
                        || !supportStart.dimension().equals(point.dimension())) {
                    supportStart = point;
                } else {
                    if (!isValidSupportDirectionGeometry(supportStart.x(), supportStart.z(),
                            point.x(), point.z())) {
                        ClientBattleState.showFeedback(false, Component.translatable(
                                "message.wok_infantry.support_direction_length").getString());
                        return true;
                    }
                    BattleClientActions.requestSupport(new BattleClientActions.SupportDraft(
                            requestedId, point.dimension(), supportStart.x(), supportStart.z(),
                            point.x(), point.z()));
                    selectedSupport = null;
                    supportStart = null;
                    rebuildWidgets();
                }
            } else {
                BattleClientActions.requestSupport(BattleClientActions.SupportDraft.point(
                        requestedId, point.dimension(), point.x(), point.z()));
                selectedSupport = null;
                supportStart = null;
                rebuildWidgets();
            }
            return true;
        }
        if (selectedTool != null && snapshot.permissions().canCreateMarkers()) {
            if (selectedTool.directional()) {
                if (attackStart == null
                        || !attackStart.dimension().equals(point.dimension())) {
                    attackStart = point;
                } else {
                    if (!TacticalMarker.isValidAttackGeometry(
                            attackStart.x(), attackStart.z(), point.x(), point.z())) {
                        ClientBattleState.showFeedback(false, Component.translatable(
                                "message.wok_infantry.attack_direction_length",
                                (int) BattleRules.MIN_ATTACK_DIRECTION_LENGTH_BLOCKS,
                                (int) BattleRules.MAX_ATTACK_DIRECTION_LENGTH_BLOCKS).getString());
                        return true;
                    }
                    BattleClientActions.createMarker(new BattleClientActions.MarkerDraft(
                            selectedTool, point.dimension(), attackStart.x(), attackStart.z(),
                            point.x(), point.z()));
                    attackStart = null;
                    rebuildWidgets();
                }
            } else {
                BattleClientActions.createMarker(BattleClientActions.MarkerDraft.point(
                        selectedTool, point.dimension(), point.x(), point.z()));
                rebuildWidgets();
            }
            return true;
        }

        TacticalMarker nearest = markerAt(mouseX, mouseY);
        selectedMarkerId = nearest == null ? null : nearest.id();
        if (nearest != null && !richBoard) {
            sidebarToolMode = SidebarToolMode.MARKERS;
        }
        panningMap = true;
        rebuildWidgets();
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (panningMap) {
            centerWorldX -= dragX / zoom;
            centerWorldZ -= dragY / zoom;
            clampCenter();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean wasPanning = panningMap;
        panningMap = false;
        return super.mouseReleased(mouseX, mouseY, button) || wasPanning;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        boolean supportPanelVisible = !selectionReplacesSidebar()
                && (richBoard || sidebarToolMode == SidebarToolMode.SUPPORT);
        if (delta != 0.0D && snapshot != null && supportPanelVisible
                && TacticalMapLayout.supportRegion(boardLayout).contains(mouseX, mouseY)) {
            int pageCount = supportPageCount(snapshot.support().options().size());
            if (pageCount > 1) {
                int requestedPage = supportPage + (delta < 0.0D ? 1 : -1);
                int previousPage = supportPage;
                changeSupportPage(requestedPage, pageCount);
                return supportPage != previousPage;
            }
        }
        if (!insideMap(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        if (children().stream().anyMatch(child -> child.isMouseOver(mouseX, mouseY))) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        WorldPoint before = screenToWorld(mouseX, mouseY);
        zoom = clamp(zoom * Math.pow(1.25D, delta), MIN_ZOOM, MAX_ZOOM);
        WorldPoint after = screenToWorld(mouseX, mouseY);
        centerWorldX += before.x() - after.x();
        centerWorldZ += before.z() - after.z();
        clampCenter();
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(previous);
    }

    @Override
    public void removed() {
        super.removed();
        terrainScreenActive = false;
        invalidateTerrainRequests();
        releaseTerrainTextures();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void initializeCenter(BattleSnapshot snapshot) {
        if (centerInitialized) {
            return;
        }
        ResourceLocation dimension = currentDimension();
        MemberPosition viewer = snapshot == null
                ? null : ClientBattleState.position(snapshot.viewerId());
        if (viewer != null && viewer.dimension().equals(dimension)) {
            centerWorldX = viewer.x();
            centerWorldZ = viewer.z();
        } else if (snapshot != null) {
            DeploymentPoint point = snapshot.deployment().points().stream()
                    .filter(candidate -> candidate.dimension().equals(dimension))
                    .filter(candidate -> candidate.id().equals(
                            snapshot.deployment().selectedPointId()))
                    .findFirst()
                    .orElseGet(() -> snapshot.deployment().points().stream()
                            .filter(candidate -> candidate.dimension().equals(dimension))
                            .findFirst().orElse(null));
            if (point != null) {
                centerWorldX = point.position().getX() + 0.5D;
                centerWorldZ = point.position().getZ() + 0.5D;
            } else if (minecraft != null && minecraft.player != null) {
                centerWorldX = minecraft.player.getX();
                centerWorldZ = minecraft.player.getZ();
            }
        } else if (minecraft != null && minecraft.player != null) {
            centerWorldX = minecraft.player.getX();
            centerWorldZ = minecraft.player.getZ();
        }
        clampCenter();
        centeredDimension = dimension;
        centerInitialized = true;
    }

    private void zoomAtCenter(double factor) {
        zoom = clamp(zoom * factor, MIN_ZOOM, MAX_ZOOM);
    }

    private void tickTerrain() {
        long now = System.nanoTime();
        boolean providerReady = TacticalMapTerrainRegistry.isReady();
        long activeProviderGeneration = TacticalMapTerrainRegistry.activeProviderGeneration();
        String activeProviderId = providerReady
                ? TacticalMapTerrainRegistry.activeProviderId() : "grid";
        if (!providerReady) {
            if (terrainProviderReady
                    || terrainProviderGeneration != activeProviderGeneration) {
                terrainProviderReady = false;
                terrainProviderId = "grid";
                terrainProviderGeneration = activeProviderGeneration;
                invalidateTerrainRequests();
                releaseTerrainTextures();
            }
            return;
        }
        if (!terrainProviderReady || !activeProviderId.equals(terrainProviderId)
                || terrainProviderGeneration != activeProviderGeneration) {
            terrainProviderReady = true;
            terrainProviderId = activeProviderId;
            terrainProviderGeneration = activeProviderGeneration;
            invalidateTerrainRequests();
            releaseTerrainTextures();
        }

        List<TacticalMapTerrainRequest> requests = terrainRequestsForViewport();
        Set<TacticalMapTerrainRequest> requestSet = Set.copyOf(requests);
        desiredTerrainRequests = requests;
        if (!requestSet.equals(observedTerrainRequests)) {
            observedTerrainRequests = requestSet;
            terrainRequestDueNanos = now + TERRAIN_DEBOUNCE_NANOS;
            terrainRetryAfterNanos.keySet().retainAll(requestSet);
            failedTerrainRequests.retainAll(requestSet);
        }

        expireTerrainRequests(now);
        if (terrainViewportTooLarge || requests.isEmpty() || panningMap
                || now < terrainRequestDueNanos
                || now - terrainLastRequestNanos < TERRAIN_MIN_REQUEST_INTERVAL_NANOS) {
            return;
        }

        int available = Math.max(0, Math.min(
                TERRAIN_MAX_IN_FLIGHT - inFlightTerrainRequests.size(),
                TERRAIN_MAX_HOST_PENDING - inFlightTerrainRequests.size()
                        - timedOutTerrainRequests.size()));
        int requestBudget = Math.min(TERRAIN_REQUEST_BURST, available);
        int requested = 0;
        for (TacticalMapTerrainRequest request : requests) {
            if (requested >= requestBudget) {
                break;
            }
            if (terrainTextures.containsKey(request)
                    || inFlightTerrainRequests.containsKey(request)
                    || timedOutTerrainRequests.containsKey(request)) {
                continue;
            }
            long retryAfter = terrainRetryAfterNanos.getOrDefault(request, 0L);
            if (now < retryAfter) {
                continue;
            }
            requestTerrain(request, now);
            requested++;
        }
        if (requested > 0) {
            terrainLastRequestNanos = now;
        }
    }

    private void requestTerrain(TacticalMapTerrainRequest request, long now) {
        long serial = ++terrainNextRequestSerial;
        long epoch = terrainRequestEpoch;
        long providerGeneration = TacticalMapTerrainRegistry.activeProviderGeneration();
        inFlightTerrainRequests.put(request,
                new InFlightTerrainTile(serial, epoch, providerGeneration, now));
        WeakReference<TacticalMapScreen> screenReference = new WeakReference<>(this);
        TacticalMapTerrainRegistry.requestTile(request, image -> {
            TacticalMapScreen screen = screenReference.get();
            if (screen == null) {
                if (image != null) {
                    image.close();
                }
                return;
            }
            Minecraft client = Minecraft.getInstance();
            try {
                client.execute(() -> screen.acceptTerrain(
                        serial, epoch, providerGeneration, request, image));
            } catch (RuntimeException exception) {
                if (image != null) {
                    image.close();
                }
                WokInfantryMod.LOGGER.warn("Could not schedule tactical terrain tile upload",
                        exception);
            }
        });
    }

    private void acceptTerrain(long serial, long epoch, long providerGeneration,
                               TacticalMapTerrainRequest request, NativeImage image) {
        InFlightTerrainTile inFlight = inFlightTerrainRequests.get(request);
        if (inFlight == null) {
            inFlight = timedOutTerrainRequests.remove(request);
        }
        if (!terrainScreenActive || epoch != terrainRequestEpoch || inFlight == null
                || serial != inFlight.serial() || epoch != inFlight.epoch()
                || providerGeneration != inFlight.providerGeneration()
                || providerGeneration != TacticalMapTerrainRegistry.activeProviderGeneration()) {
            if (image != null) {
                image.close();
            }
            return;
        }
        inFlightTerrainRequests.remove(request);
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            if (image != null) {
                image.close();
            }
            markTerrainFailure(request, System.nanoTime());
            return;
        }

        NativeImage textureImage;
        try {
            textureImage = resizeTerrainImage(request, image);
        } catch (RuntimeException exception) {
            image.close();
            markTerrainFailure(request, System.nanoTime());
            WokInfantryMod.LOGGER.warn("Could not resize tactical terrain texture", exception);
            return;
        }
        int imageWidth = textureImage.getWidth();
        int imageHeight = textureImage.getHeight();
        DynamicTexture dynamicTexture;
        try {
            dynamicTexture = new DynamicTexture(textureImage);
            dynamicTexture.setFilter(true, false);
        } catch (RuntimeException exception) {
            textureImage.close();
            markTerrainFailure(request, System.nanoTime());
            WokInfantryMod.LOGGER.warn("Could not create tactical terrain texture", exception);
            return;
        }

        ResourceLocation textureLocation;
        try {
            textureLocation = Minecraft.getInstance().getTextureManager()
                    .register("wok_infantry_tactical_terrain", dynamicTexture);
        } catch (RuntimeException exception) {
            dynamicTexture.close();
            markTerrainFailure(request, System.nanoTime());
            WokInfantryMod.LOGGER.warn("Could not register tactical terrain texture", exception);
            return;
        }

        failedTerrainRequests.remove(request);
        terrainRetryAfterNanos.remove(request);
        cacheTerrainTexture(new TerrainTexture(request, textureLocation,
                imageWidth, imageHeight));
    }

    private void renderTerrain(GuiGraphics graphics) {
        double visibleMinX = screenToWorldX(mapLeft);
        double visibleMaxX = screenToWorldX(mapRight);
        double visibleMinZ = screenToWorldZ(mapTop);
        double visibleMaxZ = screenToWorldZ(mapBottom);
        ResourceLocation dimension = currentDimension();
        boolean rendered = false;
        for (TacticalMapTerrainRequest request : desiredTerrainRequests) {
            TerrainTexture texture = terrainTextures.get(request);
            if (texture == null) {
                texture = nearestCachedTerrainFor(request);
            }
            if (texture == null || !request.dimension().equals(dimension)
                    || request.maxBlockX() <= visibleMinX || request.minBlockX() >= visibleMaxX
                    || request.maxBlockZ() <= visibleMinZ || request.minBlockZ() >= visibleMaxZ) {
                continue;
            }

            int left = worldToScreenX(request.minBlockX());
            int top = worldToScreenY(request.minBlockZ());
            int right = worldToScreenX(request.maxBlockX());
            int bottom = worldToScreenY(request.maxBlockZ());
            int drawWidth = right - left;
            int drawHeight = bottom - top;
            if (drawWidth <= 0 || drawHeight <= 0) {
                continue;
            }
            graphics.blit(texture.location(), left, top, drawWidth, drawHeight,
                    0.0F, 0.0F, texture.imageWidth(), texture.imageHeight(),
                    texture.imageWidth(), texture.imageHeight());
            rendered = true;
        }
        if (rendered) {
            graphics.fill(mapLeft, mapTop, mapRight, mapBottom,
                    TacticalBoardTheme.MAP_WASH);
        }
    }

    /** Keeps the last terrain visible while a provider supplies a newly requested zoom level. */
    private TerrainTexture nearestCachedTerrainFor(TacticalMapTerrainRequest desired) {
        TerrainTexture nearest = null;
        int nearestZoomDistance = Integer.MAX_VALUE;
        for (TerrainTexture candidate : terrainTextures.values()) {
            TacticalMapTerrainRequest cached = candidate.request();
            if (!sameTerrainRegion(cached, desired)) {
                continue;
            }
            int zoomDistance = Math.abs(cached.providerZoom() - desired.providerZoom());
            if (zoomDistance < nearestZoomDistance) {
                nearest = candidate;
                nearestZoomDistance = zoomDistance;
            }
        }
        return nearest;
    }

    private static boolean sameTerrainRegion(TacticalMapTerrainRequest first,
                                             TacticalMapTerrainRequest second) {
        return first.dimension().equals(second.dimension())
                && first.style() == second.style()
                && first.startChunkX() == second.startChunkX()
                && first.startChunkZ() == second.startChunkZ()
                && first.endChunkX() == second.endChunkX()
                && first.endChunkZ() == second.endChunkZ();
    }

    private List<TacticalMapTerrainRequest> terrainRequestsForViewport() {
        int rawStartX = (int) Math.floor(screenToWorldX(mapLeft) / 16.0D)
                - TERRAIN_CHUNK_MARGIN;
        int rawStartZ = (int) Math.floor(screenToWorldZ(mapTop) / 16.0D)
                - TERRAIN_CHUNK_MARGIN;
        int rawEndX = (int) Math.floor(screenToWorldX(mapRight) / 16.0D)
                + 1 + TERRAIN_CHUNK_MARGIN;
        int rawEndZ = (int) Math.floor(screenToWorldZ(mapBottom) / 16.0D)
                + 1 + TERRAIN_CHUNK_MARGIN;
        int startX = quantizeDown(rawStartX, TERRAIN_TILE_CHUNK_SPAN);
        int startZ = quantizeDown(rawStartZ, TERRAIN_TILE_CHUNK_SPAN);
        int endX = quantizeUp(rawEndX, TERRAIN_TILE_CHUNK_SPAN);
        int endZ = quantizeUp(rawEndZ, TERRAIN_TILE_CHUNK_SPAN);
        long columns = Math.max(0L, ((long) endX - startX) / TERRAIN_TILE_CHUNK_SPAN);
        long rows = Math.max(0L, ((long) endZ - startZ) / TERRAIN_TILE_CHUNK_SPAN);
        if (columns == 0L || rows == 0L || columns * rows > TERRAIN_MAX_VIEWPORT_TILES) {
            terrainViewportTooLarge = columns * rows > TERRAIN_MAX_VIEWPORT_TILES;
            return List.of();
        }

        terrainViewportTooLarge = false;
        ResourceLocation dimension = currentDimension();
        TacticalMapTerrainRequest.Style style = TacticalMapTerrainRequest.Style.DAY;
        int providerZoom = terrainProviderZoom();
        List<TacticalMapTerrainRequest> requests = new ArrayList<>((int) (columns * rows));
        for (int chunkZ = startZ; chunkZ < endZ; chunkZ += TERRAIN_TILE_CHUNK_SPAN) {
            for (int chunkX = startX; chunkX < endX; chunkX += TERRAIN_TILE_CHUNK_SPAN) {
                requests.add(new TacticalMapTerrainRequest(dimension, style,
                        chunkX, chunkZ,
                        chunkX + TERRAIN_TILE_CHUNK_SPAN,
                        chunkZ + TERRAIN_TILE_CHUNK_SPAN,
                        providerZoom, false));
            }
        }
        requests.sort(Comparator
                .comparingDouble(this::terrainDistanceFromCenterSquared)
                .thenComparingInt(TacticalMapTerrainRequest::startChunkZ)
                .thenComparingInt(TacticalMapTerrainRequest::startChunkX));
        return List.copyOf(requests);
    }

    private int terrainProviderZoom() {
        double requested = Math.log(32.0D / zoom) / Math.log(2.0D);
        return (int) clamp(Math.round(requested), 0.0D, 8.0D);
    }

    private static int quantizeDown(int value, int quantum) {
        return Math.floorDiv(value, quantum) * quantum;
    }

    private static int quantizeUp(int value, int quantum) {
        return -Math.floorDiv(-value, quantum) * quantum;
    }

    private void invalidateTerrainRequests() {
        terrainRequestEpoch++;
        inFlightTerrainRequests.clear();
        terrainRetryAfterNanos.clear();
        failedTerrainRequests.clear();
        timedOutTerrainRequests.clear();
        desiredTerrainRequests = List.of();
        observedTerrainRequests = Set.of();
        terrainViewportTooLarge = false;
        terrainRequestDueNanos = 0L;
    }

    private void releaseTerrainTextures() {
        for (TerrainTexture texture : terrainTextures.values()) {
            Minecraft.getInstance().getTextureManager().release(texture.location());
        }
        terrainTextures.clear();
        terrainCachedPixels = 0L;
    }

    private void expireTerrainRequests(long now) {
        Iterator<Map.Entry<TacticalMapTerrainRequest, InFlightTerrainTile>> iterator =
                inFlightTerrainRequests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<TacticalMapTerrainRequest, InFlightTerrainTile> entry = iterator.next();
            if (now - entry.getValue().startedNanos() < TERRAIN_REQUEST_TIMEOUT_NANOS) {
                continue;
            }
            iterator.remove();
            // The public map API has no cancellation handle. Do not resend this tile while the
            // old host task may still be running; a matching late result can still be consumed.
            timedOutTerrainRequests.put(entry.getKey(), entry.getValue());
            markTerrainFailure(entry.getKey(), now);
        }
    }

    private void markTerrainFailure(TacticalMapTerrainRequest request, long now) {
        if (!observedTerrainRequests.contains(request)) {
            return;
        }
        failedTerrainRequests.add(request);
        terrainRetryAfterNanos.put(request, now + TERRAIN_RETRY_NANOS);
    }

    private NativeImage resizeTerrainImage(TacticalMapTerrainRequest request,
                                           NativeImage image) {
        int targetWidth = targetTerrainTextureDimension(image.getWidth(),
                request.endChunkX() - request.startChunkX(), request.providerZoom());
        int targetHeight = targetTerrainTextureDimension(image.getHeight(),
                request.endChunkZ() - request.startChunkZ(), request.providerZoom());
        if (targetWidth == image.getWidth() && targetHeight == image.getHeight()) {
            return image;
        }

        NativeImage resized = new NativeImage(image.format(), targetWidth, targetHeight, true);
        try {
            image.resizeSubRectTo(0, 0, image.getWidth(), image.getHeight(), resized);
        } catch (RuntimeException exception) {
            resized.close();
            throw exception;
        }
        image.close();
        return resized;
    }

    private static int targetTerrainTextureDimension(int sourcePixels, int chunkSpan,
                                                     int providerZoom) {
        double pixelsPerBlock = Math.min(1.0D,
                32.0D / (1 << providerZoom));
        int desired = Math.max(1,
                (int) Math.ceil(chunkSpan * 16.0D * pixelsPerBlock));
        int powerOfTwo = 1;
        while (powerOfTwo < desired && powerOfTwo < sourcePixels) {
            powerOfTwo <<= 1;
        }
        return Math.max(1, Math.min(sourcePixels, powerOfTwo));
    }

    private void cacheTerrainTexture(TerrainTexture replacement) {
        TerrainTexture previous = terrainTextures.put(replacement.request(), replacement);
        if (previous != null) {
            terrainCachedPixels -= previous.pixelCount();
            Minecraft.getInstance().getTextureManager().release(previous.location());
        }
        terrainCachedPixels += replacement.pixelCount();
        trimTerrainCache();
    }

    private void trimTerrainCache() {
        while (terrainTextures.size() > TERRAIN_MAX_CACHED_TILES
                || terrainCachedPixels > TERRAIN_MAX_CACHED_PIXELS) {
            TacticalMapTerrainRequest eviction = null;
            for (TacticalMapTerrainRequest request : terrainTextures.keySet()) {
                if (!observedTerrainRequests.contains(request)) {
                    eviction = request;
                    break;
                }
            }
            if (eviction == null) {
                Iterator<TacticalMapTerrainRequest> iterator = terrainTextures.keySet().iterator();
                if (!iterator.hasNext()) {
                    return;
                }
                eviction = iterator.next();
            }
            releaseTerrainTexture(eviction);
        }
    }

    private void releaseTerrainTexture(TacticalMapTerrainRequest request) {
        TerrainTexture removed = terrainTextures.remove(request);
        if (removed == null) {
            return;
        }
        terrainCachedPixels -= removed.pixelCount();
        Minecraft.getInstance().getTextureManager().release(removed.location());
    }

    private double terrainDistanceFromCenterSquared(TacticalMapTerrainRequest request) {
        double tileCenterX = (request.startChunkX() + request.endChunkX()) * 8.0D;
        double tileCenterZ = (request.startChunkZ() + request.endChunkZ()) * 8.0D;
        double dx = tileCenterX - centerWorldX;
        double dz = tileCenterZ - centerWorldZ;
        return dx * dx + dz * dz;
    }

    private TerrainProgress terrainProgress() {
        int loaded = 0;
        int failed = 0;
        for (TacticalMapTerrainRequest request : desiredTerrainRequests) {
            if (terrainTextures.containsKey(request)) {
                loaded++;
            }
            if (failedTerrainRequests.contains(request)) {
                failed++;
            }
        }
        return new TerrainProgress(loaded, desiredTerrainRequests.size(), failed);
    }

    /**
     * Marker under the cursor for a click: never a drone or satellite contact (they cannot be
     * selected or removed). Clicking repeatedly over overlapping markers cycles through them.
     */
    private TacticalMarker markerAt(double mouseX, double mouseY) {
        List<MarkerHit> hits = markerHits(visibleMarkers(currentDimension()), mouseX, mouseY,
                mapIconArtPx(), false);
        if (hits.isEmpty()) {
            return null;
        }
        if (selectedMarkerId != null && hits.size() > 1) {
            for (int index = 0; index < hits.size(); index++) {
                if (hits.get(index).marker().id().equals(selectedMarkerId)) {
                    return hits.get((index + 1) % hits.size()).marker();
                }
            }
        }
        return hits.get(0).marker();
    }

    /** Marker under the cursor for the white hover ring; contacts can be hovered too. */
    private UUID hoveredMarkerId(List<TacticalMarker> markers, int mouseX, int mouseY,
                                 int artPx) {
        if (!insideMap(mouseX, mouseY)) {
            return null;
        }
        List<MarkerHit> hits = markerHits(markers, mouseX, mouseY, artPx, true);
        return hits.isEmpty() ? null : hits.get(0).marker().id();
    }

    /**
     * Markers under the cursor, nearest first: a marker icon hits on its plate plus one art pixel
     * all round ({@link TacticalMapIcons.Placement#hit}); an attack order also hits within
     * {@link #ATTACK_LINE_HIT_PHYSICAL} of its visible line.
     */
    private List<MarkerHit> markerHits(List<TacticalMarker> markers, double mouseX,
                                       double mouseY, int artPx, boolean includeContacts) {
        double scale = mapGuiScale();
        List<MarkerHit> hits = new ArrayList<>();
        for (TacticalMarker marker : markers) {
            if (marker.type() == TacticalMarkerType.RECON_CONTACT && !includeContacts) {
                continue;
            }
            double x = screenX(marker.x());
            double y = screenY(marker.z());
            double dx = (x - mouseX) * scale;
            double dy = (y - mouseY) * scale;
            double distance = dx * dx + dy * dy;
            boolean onIcon = insideMap(x, y) && TacticalMapIcons.hitGui(
                    TacticalMapIcons.MapIcon.of(marker.type()), x, y, artPx, scale,
                    mouseX, mouseY);
            if (marker.type() == TacticalMarkerType.ATTACK_DIRECTION) {
                double endX = screenX(marker.endX());
                double endY = screenY(marker.endZ());
                if (segmentIntersectsMap(x, y, endX, endY)) {
                    double line = distanceToSegmentSquared(mouseX, mouseY, x, y, endX, endY)
                            * scale * scale;
                    if (line < ATTACK_LINE_HIT_PHYSICAL * ATTACK_LINE_HIT_PHYSICAL) {
                        hits.add(new MarkerHit(marker, onIcon ? Math.min(line, distance) : line));
                        continue;
                    }
                }
            }
            if (onIcon) {
                hits.add(new MarkerHit(marker, distance));
            }
        }
        hits.sort(Comparator.comparingDouble(MarkerHit::distanceSquared)
                .thenComparing(hit -> hit.marker().id()));
        return hits;
    }

    private TacticalMarker selectedMarker() {
        if (selectedMarkerId == null) {
            return null;
        }
        return ClientBattleState.activeMarkers().stream()
                .filter(marker -> marker.id().equals(selectedMarkerId))
                .filter(marker -> marker.dimension().equals(currentDimension()))
                .filter(marker -> layerVisible(marker.type()))
                .findFirst().orElse(null);
    }

    private boolean layerVisible(TacticalMarkerType type) {
        return switch (type) {
            case RECON_CONTACT -> showInfantry || showVehicles;
            case INFANTRY -> showInfantry;
            case TANK, IFV -> showVehicles;
            case ATTACK_DIRECTION, DEFEND, RALLY -> showOrders;
        };
    }

    /** Unrounded screen x of a world x (marker icons round to whole physical pixels instead). */
    private double screenX(double x) {
        return (mapLeft + mapRight) * 0.5D + (x - centerWorldX) * zoom;
    }

    private double screenY(double z) {
        return (mapTop + mapBottom) * 0.5D + (z - centerWorldZ) * zoom;
    }

    private int worldToScreenX(double x) {
        return (int) Math.round(screenX(x));
    }

    private int worldToScreenY(double z) {
        return (int) Math.round(screenY(z));
    }

    private double screenToWorldX(double x) {
        return centerWorldX + (x - (mapLeft + mapRight) * 0.5D) / zoom;
    }

    private double screenToWorldZ(double y) {
        return centerWorldZ + (y - (mapTop + mapBottom) * 0.5D) / zoom;
    }

    private WorldPoint screenToWorld(double x, double y) {
        return new WorldPoint(currentDimension(),
                clamp(screenToWorldX(x), -BattleRules.MAX_COORDINATE,
                        BattleRules.MAX_COORDINATE),
                clamp(screenToWorldZ(y), -BattleRules.MAX_COORDINATE,
                        BattleRules.MAX_COORDINATE));
    }

    private void clampCenter() {
        centerWorldX = clamp(centerWorldX, -BattleRules.MAX_COORDINATE,
                BattleRules.MAX_COORDINATE);
        centerWorldZ = clamp(centerWorldZ, -BattleRules.MAX_COORDINATE,
                BattleRules.MAX_COORDINATE);
    }

    private boolean insideMap(double x, double y) {
        return x >= mapLeft && x < mapRight && y >= mapTop && y < mapBottom;
    }

    private double gridWorldStep() {
        double step = 16.0D;
        while (step * zoom < 28.0D && step < 8192.0D) {
            step *= 2.0D;
        }
        return step;
    }

    private ResourceLocation currentDimension() {
        BattleSnapshot snapshot = ClientBattleState.snapshot();
        if (snapshot != null && snapshot.deployment().phase() != DeploymentPhase.ACTIVE
                && !snapshot.deployment().points().isEmpty()) {
            UUID selectedId = snapshot.deployment().selectedPointId();
            return snapshot.deployment().points().stream()
                    .filter(point -> point.id().equals(selectedId))
                    .map(DeploymentPoint::dimension)
                    .findFirst()
                    .orElse(snapshot.deployment().points().get(0).dimension());
        }
        if (minecraft != null && minecraft.level != null) {
            return minecraft.level.dimension().location();
        }
        return Level.OVERWORLD.location();
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        init();
    }

    private static Component layerLabel(String key, boolean enabled) {
        return Component.translatable(key).copy().append("  ")
                .append(Component.translatable(enabled
                        ? "gui.wok_infantry.layer.on" : "gui.wok_infantry.layer.off"));
    }

    private static Component markerToolName(BattleClientActions.MarkerTool tool) {
        return Component.translatable("marker.wok_infantry."
                + tool.name().toLowerCase(Locale.ROOT));
    }

    private static TacticalMarkerType markerType(BattleClientActions.MarkerTool tool) {
        return switch (tool) {
            case INFANTRY -> TacticalMarkerType.INFANTRY;
            case TANK -> TacticalMarkerType.TANK;
            case IFV -> TacticalMarkerType.IFV;
            case DEFEND -> TacticalMarkerType.DEFEND;
            case RALLY -> TacticalMarkerType.RALLY;
            case ATTACK_DIRECTION -> TacticalMarkerType.ATTACK_DIRECTION;
        };
    }

    /**
     * Physical pixels per art pixel of the map markers: size scheme B times the icon-size knob
     * ({@link TacticalMapIcons#mapArtPx}); the map zoom never changes it.
     */
    private int mapIconArtPx() {
        return TacticalMapIcons.mapArtPx(intelMarkerScale, mapGuiScale());
    }

    /** A symbol dimension in physical pixels under size scheme B (the preview's {@code KP}). */
    private int symbolPhysical(int physical) {
        return Math.max(1, (int) Math.round(
                physical * TacticalMapIcons.symbolScale(mapGuiScale())));
    }

    /** The map viewport in physical pixels, inset by {@code insetGui} GUI pixels. */
    private TacticalMapPinPlanner.Box physicalViewport(int insetGui) {
        double scale = mapGuiScale();
        return new TacticalMapPinPlanner.Box((mapLeft + insetGui) * scale,
                (mapTop + insetGui) * scale, (mapRight - insetGui) * scale,
                (mapBottom - insetGui) * scale, 0.0D, false);
    }

    /** Physical pixels per texel of the spawn badge's check mark. */
    private int spawnBadgeIconPx() {
        return Math.max(1, (int) Math.round(TacticalMapIcons.symbolScale(mapGuiScale())));
    }

    /** Half size of the spawn badge's green square: the check mark plus 2 px all round. */
    private int spawnBadgeHalf() {
        return (TacticalIcon.SIZE * spawnBadgeIconPx() + 1) / 2 + 2;
    }

    private double mapGuiScale() {
        double guiScale = minecraft == null || minecraft.getWindow() == null
                ? 1.0D : minecraft.getWindow().getGuiScale();
        return Double.isFinite(guiScale) && guiScale >= 1.0D ? guiScale : 1.0D;
    }

    private float mapInverseGuiScale() {
        return (float) (1.0D / mapGuiScale());
    }

    private float mapTextLocalScale() {
        return (float) (MAP_TEXT_PHYSICAL_SCALE / mapGuiScale());
    }

    private int mapPhysicalToLogical(int physicalPixels) {
        return Math.max(1, (int) Math.ceil(Math.max(1, physicalPixels) / mapGuiScale()));
    }

    private int mapTextLogicalWidth(String text) {
        return Math.max(1, (int) Math.ceil(font.width(text) * mapTextLocalScale()));
    }

    private int mapTextLogicalHeight() {
        return Math.max(1, (int) Math.ceil(font.lineHeight * mapTextLocalScale()));
    }

    private String fittedMapText(String text, int logicalWidth) {
        int nativeWidth = Math.max(1,
                (int) Math.floor(Math.max(1, logicalWidth) / mapTextLocalScale()));
        if (font.width(text) <= nativeWidth) {
            return text;
        }
        String ellipsis = "…";
        int bodyWidth = Math.max(1, nativeWidth - font.width(ellipsis));
        return font.plainSubstrByWidth(text, bodyWidth) + ellipsis;
    }

    private double snapMapCoordinate(double logicalCoordinate) {
        double guiScale = mapGuiScale();
        return Math.round(logicalCoordinate * guiScale) / guiScale;
    }

    private void drawMapString(GuiGraphics graphics, String text,
                               int x, int y, int color) {
        // Two physical font pixels per authored glyph pixel keeps the map readable at 960x720,
        // while dividing by the GUI scale prevents the same label from tripling at 320x240.
        // The net framebuffer scale is the integer 2x, avoiding fractional glyph sampling.
        graphics.pose().pushPose();
        graphics.pose().translate(snapMapCoordinate(x), snapMapCoordinate(y), 0.0D);
        graphics.pose().scale(mapTextLocalScale(), mapTextLocalScale(), 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private void drawMapString(GuiGraphics graphics, String text,
                               int anchorX, int anchorY,
                               int offsetX, int offsetY, int color) {
        drawMapString(graphics, text,
                anchorX + (int) Math.round(offsetX / mapGuiScale()),
                anchorY + (int) Math.round(offsetY / mapGuiScale()), color);
    }

    private void drawMapCenteredString(GuiGraphics graphics, String text,
                                       int centerX, int centerY, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(snapMapCoordinate(centerX),
                snapMapCoordinate(centerY), 0.0D);
        graphics.pose().scale(mapTextLocalScale(), mapTextLocalScale(), 1.0F);
        graphics.drawString(font, text, -font.width(text) / 2,
                -font.lineHeight / 2, color, false);
        graphics.pose().popPose();
    }

    private void renderMapStatusTag(GuiGraphics graphics, String text,
                                    int centerX, int centerY, int accent) {
        int horizontalPadding = mapPhysicalToLogical(6);
        int verticalPadding = mapPhysicalToLogical(4);
        int accentWidth = mapPhysicalToLogical(3);
        int availableWidth = Math.max(12,
                mapRight - mapLeft - horizontalPadding * 2 - accentWidth - 4);
        String visibleText = fittedMapText(text, availableWidth);
        int textWidth = mapTextLogicalWidth(visibleText);
        int panelWidth = textWidth + horizontalPadding * 2 + accentWidth;
        int panelHeight = mapStatusTagHeight();
        int left = mapLabelLeft(centerX, panelWidth);
        int top = (int) clamp(centerY - panelHeight / 2.0D,
                mapTop + 2, Math.max(mapTop + 2, mapBottom - panelHeight - 2));
        left = clearOfMapChrome(new TacticalMapLayout.Rect(left, top,
                        left + panelWidth, top + panelHeight),
                mapLeft + 2, mapLabelMaxLeft(panelWidth), mapPhysicalToLogical(4), mapChrome());
        int right = Math.min(mapRight - 2, left + panelWidth);
        int bottom = Math.min(mapBottom - 2, top + panelHeight);
        frameLabels.add(new TacticalMapLayout.Rect(left, top, right, bottom));
        graphics.fill(left, top, right, bottom, 0xE4141B1D);
        BattleUiTheme.outline(graphics, left, top, right, bottom, accent);
        graphics.fill(left + 1, top + 1, left + accentWidth, bottom - 1, accent);
        drawMapString(graphics, visibleText,
                left + accentWidth + horizontalPadding,
                top + verticalPadding, 0xFFF3F6F4);
    }

    private void renderMapStatusCard(GuiGraphics graphics, String title, String detail,
                                     int centerX, int centerY, int accent) {
        int horizontalPadding = mapPhysicalToLogical(6);
        int verticalPadding = mapPhysicalToLogical(4);
        int lineGap = mapPhysicalToLogical(3);
        int accentWidth = mapPhysicalToLogical(3);
        int availableWidth = Math.max(12,
                mapRight - mapLeft - horizontalPadding * 2 - accentWidth - 4);
        String visibleTitle = fittedMapText(title, availableWidth);
        String visibleDetail = fittedMapText(detail, availableWidth);
        int textWidth = Math.max(mapTextLogicalWidth(visibleTitle),
                mapTextLogicalWidth(visibleDetail));
        int textHeight = mapTextLogicalHeight();
        int panelWidth = textWidth + horizontalPadding * 2 + accentWidth;
        int panelHeight = mapStatusCardHeight();
        int left = mapLabelLeft(centerX, panelWidth);
        int top = (int) clamp(centerY - panelHeight / 2.0D,
                mapTop + 2, Math.max(mapTop + 2, mapBottom - panelHeight - 2));
        left = clearOfMapChrome(new TacticalMapLayout.Rect(left, top,
                        left + panelWidth, top + panelHeight),
                mapLeft + 2, mapLabelMaxLeft(panelWidth), mapPhysicalToLogical(4), mapChrome());
        int right = Math.min(mapRight - 2, left + panelWidth);
        int bottom = Math.min(mapBottom - 2, top + panelHeight);
        frameLabels.add(new TacticalMapLayout.Rect(left, top, right, bottom));
        graphics.fill(left, top, right, bottom, 0xE8141B1D);
        BattleUiTheme.outline(graphics, left, top, right, bottom, accent);
        graphics.fill(left + 1, top + 1, left + accentWidth, bottom - 1, accent);
        int textX = left + accentWidth + horizontalPadding;
        int titleY = top + verticalPadding;
        drawMapString(graphics, visibleTitle, textX, titleY, 0xFFF3F6F4);
        drawMapString(graphics, visibleDetail, textX,
                titleY + textHeight + lineGap, accent);
    }

    private int mapStatusTagHeight() {
        return mapTextLogicalHeight() + mapPhysicalToLogical(4) * 2;
    }

    private int mapStatusCardHeight() {
        return mapTextLogicalHeight() * 2 + mapPhysicalToLogical(3)
                + mapPhysicalToLogical(4) * 2;
    }

    private int mapLabelLeft(int centerX, int panelWidth) {
        return (int) clamp(centerX - panelWidth / 2.0D, mapLeft + 2,
                mapLabelMaxLeft(panelWidth));
    }

    private int mapLabelMaxLeft(int panelWidth) {
        return Math.max(mapLeft + 2, mapRight - panelWidth - 2);
    }

    /**
     * Slides a map label sideways along its row until it no longer sits under map chrome, which is
     * drawn above every map layer. Keeping the row preserves the label's clearance from the area
     * it describes. The nearest clear position inside {@code [minLeft, maxLeft]} wins; when there
     * is none the label keeps its place.
     */
    static int clearOfMapChrome(TacticalMapLayout.Rect label, int minLeft, int maxLeft, int gap,
                                List<TacticalMapLayout.Rect> chrome) {
        if (chrome.stream().noneMatch(label::intersects)) {
            return label.left();
        }
        int width = label.width();
        int best = label.left();
        int bestDistance = Integer.MAX_VALUE;
        for (TacticalMapLayout.Rect blocker : chrome) {
            for (int candidate : new int[]{blocker.right() + gap, blocker.left() - gap - width}) {
                if (candidate < minLeft || candidate > maxLeft) {
                    continue;
                }
                TacticalMapLayout.Rect moved = new TacticalMapLayout.Rect(candidate, label.top(),
                        candidate + width, label.bottom());
                int distance = Math.abs(candidate - label.left());
                if (distance < bestDistance && chrome.stream().noneMatch(moved::intersects)) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    static int alliedPlayerMarkerRadius(boolean self, boolean commander,
                                        boolean leader) {
        if (self || commander) {
            return ALLIED_COMMANDER_MARKER_RADIUS;
        }
        return leader ? ALLIED_LEADER_MARKER_RADIUS : ALLIED_PLAYER_MARKER_RADIUS;
    }

    private static Component markerName(TacticalMarkerType type) {
        return Component.translatable("marker.wok_infantry." + type.id());
    }

    /**
     * Colour of a marker kind: its map icon's plate colour, so the attack-order line, the selected
     * marker card and the tool keys match the icon on the map.
     */
    static int markerColor(TacticalMarkerType type) {
        return TacticalMapIcons.MapIcon.of(type).color();
    }

    static double clampIntelMarkerScale(double value) {
        if (!Double.isFinite(value)) {
            return 1.0D;
        }
        return clamp(value, MIN_INTEL_MARKER_SCALE, MAX_INTEL_MARKER_SCALE);
    }

    private static int squadColor(SquadCallsign squad, boolean sameSquad) {
        if (sameSquad) {
            return 0xFF7FE8FF;
        }
        if (squad == null) {
            return 0xFF8EA0A8;
        }
        return switch (squad) {
            case ALPHA -> 0xFF55C7F3;
            case BRAVO -> 0xFF68D391;
            case CHARLIE -> 0xFFFFC857;
            case DELTA -> 0xFFC792EA;
            case ECHO -> 0xFFFF7EB6;
        };
    }

    private static String squadLetter(SquadCallsign squad) {
        return squad == null ? "?" : squad.id().substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private static int memberNumber(BattleSnapshot snapshot, MemberView member) {
        if (member.squad() == null) {
            return 0;
        }
        SquadView squad = snapshot.squads().stream()
                .filter(candidate -> candidate.callsign() == member.squad())
                .findFirst().orElse(null);
        if (squad == null) {
            return 0;
        }
        for (int index = 0; index < squad.members().size(); index++) {
            if (squad.members().get(index).playerId().equals(member.playerId())) {
                return index + 1;
            }
        }
        return 0;
    }

    private static void drawDiamond(GuiGraphics graphics, int centerX, int centerY,
                                    int radius, int color) {
        for (int offset = -radius; offset <= radius; offset++) {
            int half = radius - Math.abs(offset);
            graphics.fill(centerX - half, centerY + offset,
                    centerX + half + 1, centerY + offset + 1, color);
        }
    }

    private void drawMapDiamond(GuiGraphics graphics, int centerX, int centerY,
                                int physicalRadius, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0D);
        graphics.pose().scale(mapInverseGuiScale(), mapInverseGuiScale(), 1.0F);
        drawDiamond(graphics, 0, 0, physicalRadius, color);
        graphics.pose().popPose();
    }

    /** A line between two points given in physical pixels, {@code thickness} physical pixels. */
    private void drawPhysicalLine(GuiGraphics graphics, double startX, double startY,
                                  double endX, double endY, int color, int thickness) {
        graphics.pose().pushPose();
        graphics.pose().scale(mapInverseGuiScale(), mapInverseGuiScale(), 1.0F);
        drawLine(graphics, (int) Math.round(startX), (int) Math.round(startY),
                (int) Math.round(endX), (int) Math.round(endY), color, thickness);
        graphics.pose().popPose();
    }

    /** A diamond centred on a point given in physical pixels. */
    private void drawPhysicalDiamond(GuiGraphics graphics, double centerX, double centerY,
                                     int physicalRadius, int color) {
        graphics.pose().pushPose();
        graphics.pose().scale(mapInverseGuiScale(), mapInverseGuiScale(), 1.0F);
        drawDiamond(graphics, (int) Math.round(centerX), (int) Math.round(centerY),
                physicalRadius, color);
        graphics.pose().popPose();
    }

    private void drawMapArrow(GuiGraphics graphics, int startX, int startY,
                              int endX, int endY, int color,
                              int physicalThickness) {
        drawMapWorldLine(graphics, startX, startY, endX, endY,
                color, physicalThickness);
        double angle = Math.atan2(endY - startY, endX - startX);
        int leftX = -(int) Math.round(Math.cos(angle - 0.55D) * 7.0D);
        int leftY = -(int) Math.round(Math.sin(angle - 0.55D) * 7.0D);
        int rightX = -(int) Math.round(Math.cos(angle + 0.55D) * 7.0D);
        int rightY = -(int) Math.round(Math.sin(angle + 0.55D) * 7.0D);
        drawMapLocalLine(graphics, endX, endY,
                0, 0, leftX, leftY, color, physicalThickness);
        drawMapLocalLine(graphics, endX, endY,
                0, 0, rightX, rightY, color, physicalThickness);
    }

    private void drawMapLocalLine(GuiGraphics graphics, int anchorX, int anchorY,
                                  int startX, int startY, int endX, int endY,
                                  int color, int physicalThickness) {
        graphics.pose().pushPose();
        graphics.pose().translate(anchorX, anchorY, 0.0D);
        graphics.pose().scale(mapInverseGuiScale(), mapInverseGuiScale(), 1.0F);
        drawLine(graphics, startX, startY, endX, endY,
                color, physicalThickness);
        graphics.pose().popPose();
    }

    private void drawMapWorldLine(GuiGraphics graphics, int startX, int startY,
                                  int endX, int endY, int color,
                                  int physicalThickness) {
        int dx = endX - startX;
        int dy = endY - startY;
        int safeThickness = Math.max(1, physicalThickness);
        if (dx == 0 && dy == 0) {
            drawMapLocalLine(graphics, startX, startY,
                    0, 0, 0, 0, color, safeThickness);
            return;
        }
        int length = Math.max(1, (int) Math.ceil(Math.hypot(dx, dy)));
        graphics.pose().pushPose();
        graphics.pose().translate(startX, startY, 0.0D);
        graphics.pose().mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
        graphics.pose().scale(1.0F, mapInverseGuiScale(), 1.0F);
        int top = -safeThickness / 2;
        graphics.fill(0, top, length, top + safeThickness, color);
        graphics.pose().popPose();
    }

    private static void drawLine(GuiGraphics graphics, int startX, int startY,
                                 int endX, int endY, int color) {
        drawLine(graphics, startX, startY, endX, endY, color, 2);
    }

    private static void drawLine(GuiGraphics graphics, int startX, int startY,
                                 int endX, int endY, int color, int thickness) {
        int dx = endX - startX;
        int dy = endY - startY;
        int safeThickness = Math.max(1, thickness);
        if (dx == 0 && dy == 0) {
            graphics.fill(startX, startY, startX + safeThickness,
                    startY + safeThickness, color);
            return;
        }
        int length = Math.max(1, (int) Math.ceil(Math.hypot(dx, dy)));
        graphics.pose().pushPose();
        graphics.pose().translate(startX, startY, 0.0D);
        graphics.pose().mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
        // GuiGraphics.fill emits one quad regardless of the projected world-space distance.
        int top = -safeThickness / 2;
        graphics.fill(0, top, length + 1, top + safeThickness, color);
        graphics.pose().popPose();
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    static boolean isValidSupportDirectionGeometry(double startX, double startZ,
                                                   double endX, double endZ) {
        return SupportTarget.isValidDirection(startX, startZ, endX, endZ);
    }

    private static double distanceToSegmentSquared(double pointX, double pointY,
                                                   double startX, double startY,
                                                   double endX, double endY) {
        double segmentX = endX - startX;
        double segmentY = endY - startY;
        double lengthSquared = segmentX * segmentX + segmentY * segmentY;
        if (lengthSquared <= 1.0E-9D) {
            double dx = pointX - startX;
            double dy = pointY - startY;
            return dx * dx + dy * dy;
        }
        double progress = ((pointX - startX) * segmentX
                + (pointY - startY) * segmentY) / lengthSquared;
        progress = clamp(progress, 0.0D, 1.0D);
        double nearestX = startX + progress * segmentX;
        double nearestY = startY + progress * segmentY;
        double dx = pointX - nearestX;
        double dy = pointY - nearestY;
        return dx * dx + dy * dy;
    }

    private boolean segmentIntersectsMap(double startX, double startY,
                                         double endX, double endY) {
        return TacticalMapSegmentClipper.clip(startX, startY, endX, endY,
                mapLeft, mapTop, mapRight - 1.0D, mapBottom - 1.0D) != null;
    }

    private record WorldPoint(ResourceLocation dimension, double x, double z) {
    }

    private record MarkerHit(TacticalMarker marker, double distanceSquared) {
    }

    private record MarkerToolIconSlot(BattleClientActions.MarkerTool tool,
                                      int centerX, int centerY, int keyHeight) {
    }

    private record SupportUiStatus(Component label, Component shortLabel) {
    }

    /**
     * Client-registered inner designation zone of one support at the current zoom. When the
     * outer area is too small for a separate tag below it, {@code separateLabel} is false and
     * the label is folded into the support's range tag or mission card instead.
     */
    private record SupportGuidanceZone(int ringPixels, int labelOffset, String label,
                                       boolean separateLabel) {
    }

    /** Scale bar line ends, label and the chrome panel behind them. */
    private record MapScaleBar(int left, int right, int bottom, String label,
                               TacticalMapLayout.Rect panel) {
    }

    record SupportPagingState(int page, ResourceLocation selectedSupport,
                              boolean supportStartSet, boolean changed) {
    }

    private enum SidebarToolMode {
        MARKERS,
        SUPPORT
    }

    private record InFlightTerrainTile(long serial, long epoch, long providerGeneration,
                                       long startedNanos) {
    }

    private record TerrainProgress(int loaded, int total, int failed) {
    }

    private record TerrainTexture(TacticalMapTerrainRequest request,
                                  ResourceLocation location,
                                  int imageWidth,
                                  int imageHeight) {
        private long pixelCount() {
            return (long) imageWidth * imageHeight;
        }
    }

    @FunctionalInterface
    private interface BooleanSetter {
        void set(boolean value);
    }
}
