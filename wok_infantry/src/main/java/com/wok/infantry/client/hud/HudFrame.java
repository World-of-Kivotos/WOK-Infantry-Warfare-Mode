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
 * content of each part and the resulting {@link WokHudLayout.Layout}.
 *
 * <p>Client render thread only. A frame is recomputed when a new GUI frame starts
 * ({@link #onRenderGuiPre}) or the screen size changes; the roster model is kept until the
 * battle snapshot, the layout width class or the language changes.
 *
 * @param factor       core HUD scale (UiScale.hudFactor()); parts draw in layout pixels
 * @param strip        battle strip content, or null when the strip is hidden
 * @param notices      notices under the strip, top first (round result, base supply)
 * @param vote         formation ballot plate, or null (it replaces strip and notices)
 * @param staminaShown standalone stamina plate shown (no body-health companion slot)
 * @param companionSlot body-health companion slot {left, top, width, height} in GUI pixels, or null
 */
public record HudFrame(long frameId, int guiWidth, int guiHeight, int factor, boolean hidden,
                       boolean chatOpen, boolean debugScreen, boolean playerListHeld,
                       boolean spectator, boolean creative,
                       WokHudLayout.RosterPresence rosterPresence, SquadRosterModel.Roster roster,
                       TicketNetwork.Snapshot tickets, BattleStripModel.Sides strip,
                       List<Notice> notices, FormationVoteHudModel.Plate vote,
                       StaminaSnapshot stamina, boolean staminaShown, int[] companionSlot,
                       WokHudLayout.Layout layout) {
    /** One notice under the battle strip. */
    public record Notice(Component text, TacticalHud.Tone tone) {
    }

    private static long frameCounter;
    private static HudFrame cached;
    private static boolean effectIconsDrawn = true;
    private static int preOffsetX;
    private static int preOffsetY;
    private static int effectOffsetX;
    private static int effectOffsetY;
    private static BattleSnapshot rosterSnapshot;
    private static boolean rosterNarrow;
    private static String rosterLanguage;
    private static SquadRosterModel.Roster rosterCache;

    public HudFrame {
        notices = List.copyOf(notices);
    }

    /** Starts a new GUI frame (Forge bus, {@link RenderGuiEvent.Pre}). */
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        frameCounter++;
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
        frame = capture(Minecraft.getInstance(), guiWidth, guiHeight);
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
        boolean playerListHeld = minecraft.options.keyPlayerList.isDown();
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
        BattleStripModel.Sides strip = vote == null && tickets.visible()
                && InfantryClientConfig.showBattleStrip()
                ? BattleStripModel.sides(tickets, viewerFaction) : null;
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
        boolean staminaAllowed = stamina.enabled() && !creative && !spectator;
        int[] companionSlot = staminaAllowed && !hidden
                ? BodyHealthHudBridge.companionSlot(guiWidth, guiHeight) : null;
        boolean staminaShown = staminaAllowed && companionSlot == null;

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
                .withStrip(strip != null)
                .withToasts(noticeWidths)
                .withVote(vote == null ? 0
                                : vote.contentWidth(line -> TacticalHud.segmentsWidth(font, line)),
                        vote != null && vote.meter())
                .withStamina(staminaShown)
                .withHotbarNeighbours(offhandLeft, indicatorLeft)
                .withEffects(beneficial, harmful)
                .withEffectOffset(effectOffsetX, effectOffsetY)
                .withCapturePanel(capture);
        return new HudFrame(frameCounter, guiWidth, guiHeight, factor, hidden, chatOpen,
                debugScreen, playerListHeld, spectator, creative, presence, roster, tickets,
                strip, notices, vote, stamina, staminaShown, companionSlot,
                WokHudLayout.compute(input));
    }

    /** Roster of the viewer's squad, rebuilt only when the snapshot, width class or language changes. */
    private static SquadRosterModel.Roster roster(Minecraft minecraft, BattleSnapshot snapshot,
                                                  boolean narrow) {
        if (snapshot == null || snapshot.ownSquad() == null) {
            return null;
        }
        String language = Objects.requireNonNullElse(minecraft.options.languageCode, "");
        if (snapshot == rosterSnapshot && narrow == rosterNarrow
                && language.equals(rosterLanguage)) {
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
        rosterCache = roster;
        return roster;
    }
}
