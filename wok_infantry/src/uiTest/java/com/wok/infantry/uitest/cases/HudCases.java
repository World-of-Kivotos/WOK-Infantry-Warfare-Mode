package com.wok.infantry.uitest.cases;

import com.wok.infantry.battle.MemberState;
import com.wok.infantry.client.ClientBootstrap;
import com.wok.infantry.client.KeyBindingDefaults;
import com.wok.infantry.client.hud.BattleStripOverlay;
import com.wok.infantry.client.hud.FormationVoteHudModel;
import com.wok.infantry.client.hud.FormationVoteHudOverlay;
import com.wok.infantry.client.hud.HudFrame;
import com.wok.infantry.client.hud.SquadHudOverlay;
import com.wok.infantry.client.hud.StaminaHudOverlay;
import com.wok.infantry.client.hud.WokHudLayout;
import com.wok.infantry.client.ui.probe.UiLayoutFrame;
import com.wok.infantry.uitest.UiCapture;
import com.wok.infantry.uitest.UiCase;
import com.wok.infantry.uitest.UiCaseContext;
import com.wok.infantry.uitest.UiInputDriver;
import com.wok.infantry.uitest.UiStep;
import com.wok.infantry.uitest.UiTier;
import com.wok.infantry.uitest.fixtures.HudFixtures;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Battle HUD (preview surface {@code 10-hud}), captured with no screen open, or under the vanilla
 * chat for the {@code chat} state. Every state is a migrated surface: the roster, battle strip,
 * notices, ballot plate and vitals report their boxes and texts to the layout probe, so any text
 * outside its plate or two overlapping parts fail the run on a required tier.
 *
 * <p>States: {@code battle} (roster, strip, base-supply notice, vitals), {@code chat} (the roster
 * shrinks to its title on tight screens), {@code downed} (the viewer is down; a stand-in add-on
 * panel takes the core's {@code center_low} slot), {@code votewait} / {@code vote} /
 * {@code voted} (the ballot plate in the strip's slot, no roster before the lock),
 * {@code locked} (the 3-second lock notice) and {@code roster8} (eight members, the viewer leads
 * the squad and the faction). Data are the preview's demo data ({@link HudFixtures}), installed
 * on the client caches only.
 */
public final class HudCases {
    /** Roster bottom at 320×240 for eight members (preview {@code 10-hud} notes). */
    private static final int ROSTER_BOTTOM_320 = 95;

    private HudCases() {
    }

    public static List<UiCase> cases() {
        return List.of(
                hud("battle", HudFixtures.Mode.BATTLE),
                hud("chat", HudFixtures.Mode.CHAT),
                hud("downed", HudFixtures.Mode.DOWNED),
                hud("roster8", HudFixtures.Mode.ROSTER8),
                hud("votewait", HudFixtures.Mode.VOTE_WAIT),
                hud("vote", HudFixtures.Mode.VOTE),
                hud("voted", HudFixtures.Mode.VOTED),
                hud("locked", HudFixtures.Mode.LOCKED));
    }

    private static UiCase hud(String state, HudFixtures.Mode mode) {
        return UiCase.builder("hud", state)
                .tiers(UiTier.ALL)
                .migrated(true)
                .hudCapture(true)
                .open(context -> {
                    HudFixtures.start(mode);
                    chatLines(context, mode);
                    return mode == HudFixtures.Mode.CHAT ? new ChatScreen("") : null;
                })
                .steps(UiStep.action(context -> {
                            if (mode != HudFixtures.Mode.CHAT) {
                                // No screen: the cursor is grabbed and must not stay parked.
                                UiInputDriver.releaseToCentre(context.minecraft());
                            }
                        }),
                        UiStep.until("the HUD fixture", context -> HudFixtures.applied()))
                .check(HudCases::checkCommon)
                .check((context, capture) -> checkState(context, capture, mode))
                .cleanup(context -> HudFixtures.stop())
                .build();
    }

    /**
     * The preview's chat lines (uiTest language keys): two in battle, four with the chat open,
     * none during the ballot. The chat is cleared for every state, so lines of an earlier case
     * (still shown for ten seconds) never reach another state's screenshot.
     */
    private static void chatLines(UiCaseContext context, HudFixtures.Mode mode) {
        ChatComponent chat = context.minecraft().gui.getChat();
        chat.clearMessages(false);
        if (mode == HudFixtures.Mode.VOTE_WAIT || mode == HudFixtures.Mode.VOTE
                || mode == HudFixtures.Mode.VOTED || mode == HudFixtures.Mode.LOCKED) {
            return;
        }
        if (mode == HudFixtures.Mode.CHAT) {
            chat.addMessage(Component.translatable("uitest.wok_infantry.hud.chat.squad"));
            chat.addMessage(Component.translatable("uitest.wok_infantry.hud.chat.faction"));
        }
        chat.addMessage(Component.translatable("uitest.wok_infantry.hud.chat.kit"));
        chat.addMessage(Component.translatable("uitest.wok_infantry.hud.chat.supply"));
    }

    // ---- checks ---------------------------------------------------------------------------------

    /**
     * Every state: the parts stay clear of the vanilla hotbar and status rows, the vitals end at
     * least 4px left of the hotbar, and at GUI 1 the HUD lays out as 480×360.
     */
    private static void checkCommon(UiCaseContext context, UiCapture.Result capture) {
        UiLayoutFrame frame = capture.frame();
        int width = capture.guiWidth();
        int height = capture.guiHeight();
        UiLayoutFrame.Rect hotbar = new UiLayoutFrame.Rect(width / 2.0F - 91.0F, height - 39.0F,
                width / 2.0F + 91.0F, height);
        for (UiLayoutFrame.Box box : frame.boxes()) {
            if (box.id().startsWith("hud.") && box.rect().overlaps(hotbar)) {
                context.fail(box.id() + " " + box.rect() + " covers the hotbar or status rows "
                        + hotbar);
            }
        }
        UiLayoutFrame.Box vitals = frame.box(StaminaHudOverlay.PROBE_BOX);
        context.require(vitals != null, "the vitals (stamina) plate was not drawn");
        float hotbarLeft = width / 2.0F - WokHudLayout.HOTBAR_HALF_WIDTH;
        context.require(vitals.rect().right() <= hotbarLeft - 4.0F + 0.01F,
                "vitals right edge " + vitals.rect().right() + " is not 4px left of the hotbar "
                        + hotbarLeft);
        if (context.tier() == UiTier.T960) {
            context.require(capture.baseScale() == 2 && capture.layoutWidth() == 480
                            && capture.layoutHeight() == 360,
                    "the HUD at 960x720 GUI 1 must lay out as 480x360 at 2x, got "
                            + capture.layoutWidth() + "x" + capture.layoutHeight() + " x"
                            + capture.baseScale());
        }
        context.observe("hudBoxes[" + context.uiCase().stateId() + "@" + context.tier().id()
                + "]=" + frame.boxes().stream().filter(box -> !"hud.plate".equals(box.id()))
                .map(box -> box.id() + box.rect()).toList());
    }

    private static void checkState(UiCaseContext context, UiCapture.Result capture,
                                   HudFixtures.Mode mode) {
        UiLayoutFrame frame = capture.frame();
        HudFrame hud = HudFrame.current(capture.guiWidth(), capture.guiHeight());
        context.require(hud != null, "no HUD frame");
        UiLayoutFrame.Box roster = frame.box(SquadHudOverlay.PROBE_BOX);
        UiLayoutFrame.Box strip = frame.box(BattleStripOverlay.PROBE_BOX);
        UiLayoutFrame.Box vote = frame.box(FormationVoteHudOverlay.PROBE_BOX);
        if (mode == HudFixtures.Mode.VOTE_WAIT || mode == HudFixtures.Mode.VOTE
                || mode == HudFixtures.Mode.VOTED || mode == HudFixtures.Mode.LOCKED) {
            context.require(vote != null, "the ballot plate was not drawn");
            context.require(strip == null, "the battle strip must give its slot to the ballot");
            context.require(mode == HudFixtures.Mode.LOCKED || roster == null,
                    "no roster before the lock (nobody has a squad yet)");
            FormationVoteHudModel.State expected = switch (mode) {
                case VOTE_WAIT -> FormationVoteHudModel.State.WAITING;
                case VOTE -> FormationVoteHudModel.State.OPEN;
                case VOTED -> FormationVoteHudModel.State.VOTED;
                default -> FormationVoteHudModel.State.LOCKED;
            };
            context.require(hud.vote() != null && hud.vote().state() == expected,
                    "ballot plate shows " + (hud.vote() == null ? "nothing" : hud.vote().state())
                            + " instead of " + expected);
            Component key = ClientBootstrap.keyLabel(KeyBindingDefaults.Binding.TERMINAL);
            if (key != null && !key.getString().isEmpty()) {
                context.require(frame.texts().stream().anyMatch(text ->
                                text.text().equals(key.getString())),
                        "the ballot plate does not name the terminal key " + key.getString());
            }
            context.observe("hudBallot[" + context.tier().id() + "]=" + expected);
            return;
        }
        context.require(roster != null, "the squad roster was not drawn");
        context.require(strip != null, "the battle strip was not drawn");
        context.require(vote == null, "no ballot plate after the lock");
        float layoutBottom = roster.rect().bottom() / capture.baseScale();
        boolean collapsed = hud.rosterPresence() == WokHudLayout.RosterPresence.COLLAPSED;
        if (mode == HudFixtures.Mode.CHAT && context.tight()) {
            context.require(collapsed, "with the chat open on a tight screen the roster must"
                    + " shrink to its title row");
        }
        if (context.tier() == UiTier.T320 && !collapsed) {
            context.require(layoutBottom <= ROSTER_BOTTOM_320 + 0.01F,
                    "roster bottom " + layoutBottom + " is below y" + ROSTER_BOTTOM_320
                            + " at 320x240");
        }
        if (mode == HudFixtures.Mode.ROSTER8) {
            context.require(hud.roster() != null && hud.roster().rows().size() == 8,
                    "the roster does not show eight members");
        }
        if (mode == HudFixtures.Mode.DOWNED) {
            context.require(hud.roster() != null && !hud.roster().rows().isEmpty()
                            && hud.roster().rows().get(0).state() == MemberState.DOWNED,
                    "the viewer's roster row does not show the downed state");
            UiLayoutFrame.Box downed = frame.box(HudFixtures.DOWNED_BOX);
            context.require(downed != null, "the add-on panel did not get the center_low slot");
        } else {
            context.require(frame.box(BattleStripOverlay.TOAST_PROBE_BOX) != null,
                    "the base-supply notice was not drawn under the strip");
        }
        context.observe("hudRosterBottom[" + context.uiCase().stateId() + "@"
                + context.tier().id() + "]=" + layoutBottom + (collapsed ? " collapsed" : ""));
    }
}
