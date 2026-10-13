package com.wok.infantry.client.hud;

import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.SquadView;
import com.wok.infantry.battle.tickets.TicketNetwork;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.ClientBootstrap;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.client.ClientStaminaState;
import com.wok.infantry.client.KeyBindingDefaults;
import com.wok.infantry.client.screen.SquadLabels;
import com.wok.infantry.client.screen.UiRect;
import com.wok.infantry.client.screen.UiScale;
import com.wok.infantry.config.InfantryClientConfig;
import com.wok.infantry.stamina.StaminaSnapshot;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Everything the core HUD needs for one rendered frame, captured once and shared by every core
 * overlay and {@link InfantryHudApi}: the screen state (chat, F3, Tab, spectator, F1), the
 * vanilla obstacles (off-hand slot, attack indicator, effect icons), the capture panel, the
 * add-on panels the roster must end above (body-health figure, downed panel, an add-on in the
 * centre-low slot), the content of each part and the resulting {@link WokHudLayout.Layout}.
 *
 * <p>Client render thread only. A frame is recomputed when a new GUI frame starts
 * ({@link #onRenderGuiPre}) or the screen size changes; the roster model is kept until the
 * battle snapshot, the layout width class, the language or the Unicode font option changes.
 *
 * @param factor       core HUD scale (UiScale.hudFactor()); parts draw in layout pixels
 * @param playerListHeld vanilla draws the player list this frame (its key is held and there is
 *                     a list to show, see {@link WokHudLayout#vanillaPlayerListShown})
 * @param strip        manpower content of the battle strip, or null when no manpower is shown
 * @param objective    the WOK步战附属-占点 point the viewer stands in, shown as the strip's
 *                     objective tile (and, on non-tight screens, its second row), or null; with
 *                     an objective the strip is shown even without manpower
 * @param notices      notices under the strip, top first (round result, base supply)
 * @param vote         formation ballot plate, or null (it replaces strip and notices)
 * @param staminaShown the stamina bar is drawn above the hotbar this frame (stamina on, survival
 *                     or adventure, alive, {@code hud.staminaBar} on); it then also replaces the
 *                     vanilla experience and mount jump bars
 */
public record HudFrame(long frameId, int guiWidth, int guiHeight, int factor, boolean hidden,
                       boolean chatOpen, boolean debugScreen, boolean playerListHeld,
                       boolean spectator, boolean creative,
                       WokHudLayout.RosterPresence rosterPresence, SquadRosterModel.Roster roster,
                       TicketNetwork.Snapshot tickets, BattleStripModel.Sides strip,
                       BattleStripModel.Objective objective,
                       List<Notice> notices, FormationVoteHudModel.Plate vote,
                       StaminaSnapshot stamina, boolean staminaShown,
                       WokHudLayout.Layout layout) {
    /** One notice under the battle strip. */
    public record Notice(Component text, TacticalHud.Tone tone) {
    }

    private static long frameCounter;
    private static long centerLowAskedFrame = -1;
    private static HudFrame cached;
    private static boolean capturing;
    private static boolean effectIconsDrawn = true;
    private static int preOffsetX;
    private static int preOffsetY;
    private static int effectOffsetX;
    private static int effectOffsetY;
    private static BattleSnapshot rosterSnapshot;
    private static boolean rosterNarrow;
    private static String rosterLanguage;
    private static boolean rosterUnicode;
    private static SquadRosterModel.Roster rosterCache;
    private static CaptureObjective objectivePoint;
    private static Faction objectiveViewer;
    private static BattleStripModel.Objective objectiveCache;

    public HudFrame {
        notices = List.copyOf(notices);
    }

    /** Starts a new GUI frame (Forge bus, {@link RenderGuiEvent.Pre}). */
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        frameCounter++;
    }

    /**
     * Records that an add-on asked for the centre-low slot this frame
     * ({@link InfantryHudApi#slot}): from the next frame on the roster ends above that slot.
     */
    static void centerLowAsked() {
        centerLowAskedFrame = frameCounter;
    }

    /**
     * Whether an add-on draws in the centre-low slot: it asked for it this frame or the one
     * before (a frame is laid out before the add-ons draw, so this frame's ask may not be in yet).
     */
    static boolean centerLowInUse(long frame, long askedFrame) {
        return askedFrame >= 0 && frame - askedFrame <= 1;
    }

    /**
     * Forge bus, lowest priority, cancelled events included: records whether the vanilla effect
     * icons are drawn and the pose they are drawn with. Other MODs may move them (JourneyMap
     * pushes a translation at its own lowest-priority listener and pops it at the highest-priority
     * post listener) or hide them (cancel); the next frames' layout uses what was measured.
     */
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!isEffectOverlay(event)) {
            return;
        }
        effectIconsDrawn = !event.isCanceled();
        Matrix4f pose = event.getGuiGraphics().pose().last().pose();
        preOffsetX = Math.round(pose.m30());
        preOffsetY = Math.round(pose.m31());
    }

    /**
     * Forge bus, highest priority: the other half of the effect icon measurement. A listener
     * registered before the translating MOD's sees the translation here, one registered after
     * it sees it in {@link #onOverlayPre}; either way one of the two holds it.
     */
    public static void onOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (!isEffectOverlay(event)) {
            return;
        }
        Matrix4f pose = event.getGuiGraphics().pose().last().pose();
        int[] offset = effectOffset(preOffsetX, preOffsetY, Math.round(pose.m30()),
                Math.round(pose.m31()));
        effectOffsetX = offset[0];
        effectOffsetY = offset[1];
    }

    /** The translated one of the two measurements (pre wins), or 0, 0 when neither moved. */
    static int[] effectOffset(int preX, int preY, int postX, int postY) {
        return preX != 0 || preY != 0 ? new int[]{preX, preY} : new int[]{postX, postY};
    }

    private static boolean isEffectOverlay(RenderGuiOverlayEvent event) {
        return event.getOverlay() != null
                && VanillaGuiOverlay.POTION_ICONS.id().equals(event.getOverlay().id());
    }

    /**
     * The frame for a {@code guiWidth}×{@code guiHeight} GUI, or null without a local player.
     * Computed at most once per GUI frame and size.
     */
    public static HudFrame current(int guiWidth, int guiHeight) {
        HudFrame frame = cached;
        if (frame != null && frame.frameId == frameCounter && frame.guiWidth == guiWidth
                && frame.guiHeight == guiHeight) {
            return frame;
        }
        if (capturing) {
            // An add-on called back into InfantryHudApi while this frame is being captured (e.g.
            // a capture panel API that asks for its own slot): answer with the previous frame
            // instead of recursing.
            return frame;
        }
        capturing = true;
        try {
            frame = capture(Minecraft.getInstance(), guiWidth, guiHeight);
        } finally {
            capturing = false;
        }
        cached = frame;
        return frame;
    }

    private static HudFrame capture(Minecraft minecraft, int guiWidth, int guiHeight) {
        LocalPlayer player = minecraft == null ? null : minecraft.player;
        if (player == null) {
            rosterSnapshot = null;
            rosterCache = null;
            return null;
        }
        int factor = Math.max(1, UiScale.hudFactor());
        int width = Math.max(1, guiWidth / factor);
        int height = Math.max(1, guiHeight / factor);
        boolean tight = WokHudLayout.isTight(width, height);
        boolean narrow = width < WokHudLayout.NARROW_WIDTH;
        boolean hidden = minecraft.options.hideGui;
        boolean chatOpen = minecraft.screen instanceof ChatScreen;
        boolean debugScreen = minecraft.options.renderDebug;
        boolean playerListHeld = playerListShown(minecraft, player);
        boolean spectator = player.isSpectator();
        boolean creative = player.isCreative();
        Font font = minecraft.font;

        WokHudLayout.RosterPresence presence = WokHudLayout.rosterPresence(
                InfantryClientConfig.hudRosterMode(), tight, chatOpen, debugScreen,
                playerListHeld, spectator);
        BattleSnapshot battle = ClientBattleState.snapshot();
        SquadRosterModel.Roster roster = presence == WokHudLayout.RosterPresence.HIDDEN ? null
                : roster(minecraft, battle, narrow);

        FormationVoteHudModel.VoteView voteView = FormationVoteHudModel.view(
                ClientFormationState.snapshot(), ClientFormationState.lockTransition(),
                System.nanoTime());
        FormationVoteHudModel.Plate vote = voteView == null ? null
                : FormationVoteHudModel.plate(voteView, tight,
                ClientBootstrap.keyLabel(KeyBindingDefaults.Binding.TERMINAL));

        TicketNetwork.Snapshot tickets = TicketNetwork.snapshot();
        Faction viewerFaction = battle == null ? null : battle.faction();
        boolean stripOn = InfantryClientConfig.showBattleStrip();
        BattleStripModel.Sides strip = vote == null && tickets.visible() && stripOn
                ? BattleStripModel.sides(tickets, viewerFaction) : null;
        // With the strip off, WOK步战附属-占点 draws its own thin strip (rendersCapturePoints).
        BattleStripModel.Objective objective = hidden || !stripOn ? null
                : objective(CaptureHudBridge.objective(), viewerFaction);
        List<Notice> notices = new ArrayList<>(2);
        if (vote == null) {
            if (tickets.visible()) {
                BattleStripModel.Outcome outcome = BattleStripModel.outcome(tickets.blue(),
                        tickets.red(), viewerFaction);
                Component text = BattleStripModel.outcomeText(outcome);
                if (text != null) {
                    notices.add(new Notice(text, BattleStripModel.outcomeTone(outcome)));
                }
            }
            String supply = TicketNetwork.supplyHint();
            if (!supply.isEmpty()) {
                notices.add(new Notice(Component.literal(supply), TacticalHud.Tone.SUCCESS));
            }
        }

        StaminaSnapshot stamina = ClientStaminaState.snapshot();
        boolean staminaShown = stamina.enabled() && !creative && !spectator && player.isAlive()
                && InfantryClientConfig.hudStaminaBar();
        // The core no longer draws into the body-health companion strip; the slot is still asked
        // for because it locates the figure the roster keeps clear of.
        int[] bodyHealthSlot = hidden ? null
                : BodyHealthHudBridge.companionSlot(guiWidth, guiHeight);
        List<UiRect> addonPanels = new ArrayList<>(2);
        UiRect bodyHealth = BodyHealthHudBridge.hudRect(guiWidth, guiHeight, bodyHealthSlot);
        if (bodyHealth != null) {
            addonPanels.add(bodyHealth);
        }
        UiRect downed = hidden ? null : DownedHudBridge.panelRect(player, guiWidth, guiHeight);
        if (downed != null) {
            addonPanels.add(downed);
        }

        boolean mainRight = player.getMainArm() == HumanoidArm.RIGHT;
        boolean offhandLeft = !spectator && mainRight && !player.getOffhandItem().isEmpty();
        boolean indicatorLeft = !spectator && !mainRight
                && minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR;
        int beneficial = 0;
        int harmful = 0;
        for (MobEffectInstance effect : effectIconsDrawn ? player.getActiveEffects()
                : List.<MobEffectInstance>of()) {
            if (!effect.showIcon()
                    || !IClientMobEffectExtensions.of(effect).isVisibleInGui(effect)) {
                continue;
            }
            if (effect.getEffect().isBeneficial()) {
                beneficial++;
            } else {
                harmful++;
            }
        }
        UiRect capture = hidden ? null : CaptureHudBridge.panelRect(guiWidth, guiHeight);

        List<Integer> noticeWidths = new ArrayList<>(notices.size());
        for (Notice notice : notices) {
            noticeWidths.add(font.width(notice.text()));
        }
        WokHudLayout.Input input = WokHudLayout.Input.screen(guiWidth, guiHeight, factor)
                .withRoster(roster == null ? 0 : roster.rows().size(),
                        presence == WokHudLayout.RosterPresence.COLLAPSED)
                .withStrip(strip != null || objective != null, objective != null && !tight)
                .withToasts(noticeWidths)
                .withVote(vote == null ? 0
                                : vote.contentWidth(line -> TacticalHud.segmentsWidth(font, line)),
                        vote != null && vote.meter())
                .withStaminaText(font.width(StaminaBarLayout.NUMBER_SAMPLE),
                        StaminaBarLayout.chatRight(minecraft.gui.getChat().getWidth(),
                                minecraft.gui.getChat().getScale()))
                .withHotbarNeighbours(offhandLeft, indicatorLeft)
                .withEffects(beneficial, harmful)
                .withEffectOffset(effectOffsetX, effectOffsetY)
                .withCapturePanel(capture)
                .withAddonPanels(addonPanels)
                .withCenterLowInUse(!hidden && centerLowInUse(frameCounter, centerLowAskedFrame));
        return new HudFrame(frameCounter, guiWidth, guiHeight, factor, hidden, chatOpen,
                debugScreen, playerListHeld, spectator, creative, presence, roster, tickets,
                strip, objective, notices, vote, stamina, staminaShown,
                WokHudLayout.compute(input));
    }

    /**
     * The strip content of {@code point} for {@code viewer}, rebuilt only when the add-on hands
     * over a new point description or the viewer changes side.
     */
    private static BattleStripModel.Objective objective(CaptureObjective point, Faction viewer) {
        if (point == null) {
            objectivePoint = null;
            objectiveCache = null;
            return null;
        }
        if (point != objectivePoint || viewer != objectiveViewer || objectiveCache == null) {
            objectiveCache = BattleStripModel.objective(point, viewer);
            objectivePoint = point;
            objectiveViewer = viewer;
        }
        return objectiveCache;
    }

    /**
     * Whether vanilla draws the player list this frame: the key alone is not enough, single
     * player without anyone else listed and without a list objective shows nothing.
     */
    private static boolean playerListShown(Minecraft minecraft, LocalPlayer player) {
        if (!minecraft.options.keyPlayerList.isDown()) {
            return false;
        }
        boolean listObjective = minecraft.level != null && minecraft.level.getScoreboard()
                .getDisplayObjective(Scoreboard.DISPLAY_SLOT_LIST) != null;
        int listed = player.connection == null ? 0
                : player.connection.getListedOnlinePlayers().size();
        return WokHudLayout.vanillaPlayerListShown(true, minecraft.isLocalServer(), listed,
                listObjective);
    }

    /**
     * Roster of the viewer's squad, rebuilt only when the snapshot, width class, language or
     * font choice ("Force Unicode Font", which changes every text width) changes.
     */
    private static SquadRosterModel.Roster roster(Minecraft minecraft, BattleSnapshot snapshot,
                                                  boolean narrow) {
        if (snapshot == null || snapshot.ownSquad() == null) {
            return null;
        }
        String language = Objects.requireNonNullElse(minecraft.options.languageCode, "");
        boolean unicode = Boolean.TRUE.equals(minecraft.options.forceUnicodeFont().get());
        if (snapshot == rosterSnapshot && narrow == rosterNarrow
                && language.equals(rosterLanguage) && unicode == rosterUnicode) {
            return rosterCache;
        }
        SquadView squad = null;
        for (SquadView candidate : snapshot.squads()) {
            if (candidate.callsign() == snapshot.ownSquad()) {
                squad = candidate;
                break;
            }
        }
        SquadRosterModel.Roster roster = squad == null || squad.members().isEmpty() ? null
                : SquadRosterModel.build(squad, snapshot.viewerId(), narrow,
                SquadLabels.callsign(squad.callsign()).getString(),
                member -> SquadLabels.className(snapshot, member.classId()).getString(),
                SquadRosterModel.Labels.translated(), minecraft.font::width);
        rosterSnapshot = snapshot;
        rosterNarrow = narrow;
        rosterLanguage = language;
        rosterUnicode = unicode;
        rosterCache = roster;
        return roster;
    }
}
