package com.wok.infantry.client.hud;

import com.wok.infantry.client.ClientFormationState.LockTransition;
import com.wok.infantry.client.hud.FormationVoteHudModel.Plate;
import com.wok.infantry.client.hud.FormationVoteHudModel.State;
import com.wok.infantry.client.hud.FormationVoteHudModel.VoteView;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.TacticalIcon;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.vote.FormationVotePhase;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationVoteHudModelTest {
    private static final Map<String, String> ZH = HudTestSupport.bundle(HudTestSupport.ZH_CN);
    private static final Map<String, String> EN = HudTestSupport.bundle(HudTestSupport.EN_US);
    private static final Component KEY = Component.literal("`");
    private static final FormationSelectionView MOBILE = formation("millennium_seminar_mobile",
            "千禧年研讨会机动部队", "wok_infantry:textures/gui/formation/mobile.png");
    private static final FormationSelectionView REGULAR = formation("default", "常规编制", "");
    private static final FactionSelectionView FACTION = new FactionSelectionView("millennium",
            "学院军", "", 18, 40, true, List.of(MOBILE, REGULAR));
    private static final long NOW = 50_000_000_000L;

    @Test
    void noPlateWithoutAFactionOrOnceTheFormationIsSet() {
        assertNull(FormationVoteHudModel.view(null, Optional.empty(), NOW));
        assertNull(FormationVoteHudModel.view(snapshot(true, "", FormationVotePhase.OPEN, "",
                Map.of()), Optional.empty(), NOW), "no faction yet: the formation page handles it");
        assertNull(FormationVoteHudModel.view(snapshot(false, "millennium",
                FormationVotePhase.LOCKED, "", Map.of()), Optional.empty(), NOW),
                "formation already set");
        assertNull(FormationVoteHudModel.view(snapshot(true, "millennium",
                FormationVotePhase.LOCKED, "", Map.of()), Optional.empty(), NOW),
                "locked without a fresh lock notice");
    }

    @Test
    void ballotStatesFollowTheSnapshot() {
        VoteView waiting = FormationVoteHudModel.view(snapshot(true, "millennium",
                FormationVotePhase.NOT_STARTED, "", Map.of()), Optional.empty(), NOW);
        assertEquals(State.WAITING, waiting.state());
        assertEquals("学院军", waiting.factionName());
        assertEquals(18, waiting.population());

        Map<String, Integer> tally = Map.of("millennium_seminar_mobile", 4, "default", 4);
        VoteView open = FormationVoteHudModel.view(snapshot(true, "millennium",
                FormationVotePhase.OPEN, "", tally), Optional.empty(), NOW);
        assertEquals(State.OPEN, open.state());
        assertEquals(8, open.voted());

        VoteView voted = FormationVoteHudModel.view(snapshot(true, "millennium",
                FormationVotePhase.OPEN, "millennium_seminar_mobile",
                Map.of("millennium_seminar_mobile", 5, "default", 4)), Optional.empty(), NOW);
        assertEquals(State.VOTED, voted.state());
        assertEquals(9, voted.voted());
        assertEquals("千禧年研讨会机动部队", voted.formation());
        assertTrue(voted.changeAllowed());
    }

    @Test
    void lockNoticeLastsThreeSecondsAndRunsOut() {
        FormationSelectionSnapshot locked = snapshot(false, "millennium",
                FormationVotePhase.LOCKED, "", Map.of());
        LockTransition lock = new LockTransition("millennium", "millennium_seminar_mobile",
                "千禧年研讨会机动部队", NOW - 1_000_000_000L);
        VoteView view = FormationVoteHudModel.view(locked, Optional.of(lock), NOW);
        assertEquals(State.LOCKED, view.state());
        assertEquals("学院军", view.factionName());
        assertEquals("wok_infantry:textures/gui/formation/mobile.png", view.iconId());
        assertEquals(2.0F / 3.0F, view.remain(), 1.0E-4F);
        assertNull(FormationVoteHudModel.view(locked, Optional.of(lock), NOW + 2_000_000_000L),
                "gone after three seconds");
        assertNull(FormationVoteHudModel.view(locked, Optional.of(new LockTransition(
                "millennium", "x", "", NOW + 5)), NOW), "a lock from the future is ignored");

        Plate plate = FormationVoteHudModel.plate(view, false, KEY);
        assertEquals(State.LOCKED, plate.state());
        assertTrue(plate.solid());
        assertEquals(TacticalBoardTheme.SUCCESS_B, plate.accent());
        assertTrue(plate.hasEmblem());
        assertEquals(22, plate.leadWidth(), "16px emblem on its backing");
        assertEquals("学院军编制已锁定：千禧年研讨会机动部队", line(plate.firstLine(), ZH));
        assertEquals("全阵营共用这一个编制 · 按 [`] 选择小队并部署", line(plate.secondLine(), ZH));
        assertEquals("编制已锁定：千禧年研讨会机动部队",
                line(FormationVoteHudModel.plate(view, true, KEY).firstLine(), ZH));
        assertEquals("全阵营共用 · 按 [`] 部署",
                line(FormationVoteHudModel.plate(view, true, KEY).secondLine(), ZH));
    }

    @Test
    void formationIdsResolveWithinTheViewersFaction() {
        // formation ids are unique per faction only: another faction listed first has "default" too
        FactionSelectionView other = new FactionSelectionView("abydos", "对策委员会", "", 5, 40,
                true, List.of(formation("default", "对策委员会常规", "wok_infantry:textures/gui/a.png")));
        FormationSelectionSnapshot locked = new FormationSelectionSnapshot(3L, false, "millennium",
                "default", FormationVotePhase.LOCKED, true, "", "default", Map.of(),
                List.of(other, FACTION));
        VoteView notice = FormationVoteHudModel.view(locked, Optional.of(new LockTransition(
                "millennium", "default", "", NOW - 1L)), NOW);
        assertEquals("常规编制", notice.formation());
        assertEquals("", notice.iconId(), "never the other faction's emblem");

        FormationSelectionSnapshot open = new FormationSelectionSnapshot(3L, true, "millennium", "",
                FormationVotePhase.OPEN, true, "default", "", Map.of("default", 1),
                List.of(other, FACTION));
        assertEquals("常规编制", FormationVoteHudModel.view(open, Optional.empty(), NOW).formation());
    }

    @Test
    void lockNoticeWithoutEmblemShowsTheFlag() {
        VoteView view = new VoteView(State.LOCKED, "", 0, 0, "常规编制", "", false, 0.5F);
        Plate plate = FormationVoteHudModel.plate(view, false, KEY);
        assertFalse(plate.hasEmblem());
        assertEquals(TacticalIcon.FLAG, plate.icon());
        assertEquals(12, plate.leadWidth());
        assertEquals("编制已锁定：常规编制", line(plate.firstLine(), ZH),
                "no faction name: the short label");
        assertEquals(0.5F, plate.remain());
    }

    @Test
    void waitingWordingAndKeyCap() {
        VoteView view = new VoteView(State.WAITING, "学院军", 18, 0, "", "", false, 0.0F);
        Plate wide = FormationVoteHudModel.plate(view, false, KEY);
        assertEquals(TacticalBoardTheme.NEUTRAL_B, wide.accent(), "waiting is neutral, never orange");
        assertEquals(TacticalIcon.CLOCK, wide.icon());
        assertFalse(wide.meter());
        assertEquals("等待管理员开启编制投票", line(wide.firstLine(), ZH));
        assertEquals("学院军 18 人 · 开启后 [`] → 编制页", line(wide.secondLine(), ZH));
        Plate tight = FormationVoteHudModel.plate(view, true, KEY);
        assertEquals("等待开启编制投票", line(tight.firstLine(), ZH));
        assertEquals("开启后 [`] → 编制页", line(tight.secondLine(), ZH));
    }

    @Test
    void unboundTerminalKeyLeavesTheKeyCapOut() {
        VoteView view = new VoteView(State.WAITING, "学院军", 18, 0, "", "", false, 0.0F);
        Plate plate = FormationVoteHudModel.plate(view, false, null);
        assertTrue(plate.secondLine().stream().noneMatch(TacticalHud.Segment::key));
        assertEquals("学院军 18 人 · 开启后在编制页投票", line(plate.secondLine(), ZH));
        Plate blank = FormationVoteHudModel.plate(view, true, Component.empty());
        assertTrue(blank.secondLine().stream().noneMatch(TacticalHud.Segment::key));
    }

    @Test
    void openAndVotedWordingWithTheTallyMeterOnWideScreens() {
        VoteView open = new VoteView(State.OPEN, "学院军", 18, 8, "", "", true, 0.0F);
        Plate wide = FormationVoteHudModel.plate(open, false, KEY);
        assertEquals("编制投票进行中 · 你尚未投票", line(wide.firstLine(), ZH));
        assertEquals("学院军已投 8/18 · 投票 [`] → 编制页", line(wide.secondLine(), ZH));
        assertTrue(wide.meter());
        assertEquals(8.0F / 18.0F, wide.meterRatio(), 1.0E-6F);
        Plate tight = FormationVoteHudModel.plate(open, true, KEY);
        assertFalse(tight.meter(), "no tally meter on tight screens");
        assertEquals("编制投票中 · 你未投票", line(tight.firstLine(), ZH));
        assertEquals("已投 8/18 · [`] → 编制页", line(tight.secondLine(), ZH));

        VoteView voted = new VoteView(State.VOTED, "学院军", 18, 9, "千禧年研讨会机动部队", "",
                true, 0.0F);
        Plate change = FormationVoteHudModel.plate(voted, false, KEY);
        assertEquals(TacticalIcon.CHECK, change.icon());
        assertEquals(TacticalBoardTheme.SUCCESS_B, change.iconColor());
        assertEquals("编制投票中 · 已投：千禧年研讨会机动部队", line(change.firstLine(), ZH));
        assertEquals("学院军已投 9/18 · 等待管理员锁定 · [`] → 编制页改票",
                line(change.secondLine(), ZH));
        assertEquals("9/18 · 等待锁定 · [`] 改票",
                line(FormationVoteHudModel.plate(voted, true, KEY).secondLine(), ZH));
        VoteView noChange = new VoteView(State.VOTED, "学院军", 18, 9, "千禧年研讨会机动部队", "",
                false, 0.0F);
        assertEquals("学院军已投 9/18 · 等待管理员锁定 · [`] → 编制页查看计票",
                line(FormationVoteHudModel.plate(noChange, false, KEY).secondLine(), ZH));
        assertEquals("学院军已投 9/18 · 等待管理员锁定 · 编制页可查看计票",
                line(FormationVoteHudModel.plate(noChange, false, null).secondLine(), ZH));
    }

    @Test
    void everyVariantResolvesInBothLanguages() {
        Set<String> keys = new TreeSet<>();
        for (State state : State.values()) {
            for (boolean tight : new boolean[]{false, true}) {
                for (Component key : new Component[]{KEY, null}) {
                    for (boolean change : new boolean[]{false, true}) {
                        VoteView view = new VoteView(state, "Faction", 18, 9, "Formation",
                                "", change, 0.5F);
                        Plate plate = FormationVoteHudModel.plate(view, tight, key);
                        for (List<TacticalHud.Segment> segments : List.of(plate.firstLine(),
                                plate.secondLine())) {
                            for (TacticalHud.Segment segment : segments) {
                                keys.addAll(HudTestSupport.keys(segment.text()));
                            }
                            String zh = line(segments, ZH);
                            String en = line(segments, EN);
                            assertFalse(zh.contains("!hud") || en.contains("!hud"),
                                    state + " tight=" + tight + ": " + zh + " / " + en);
                        }
                    }
                }
            }
        }
        assertTrue(keys.size() >= 30, "all ballot wording comes from language keys: " + keys);
        for (String key : keys) {
            assertTrue(ZH.containsKey(key) && EN.containsKey(key), key);
        }
    }

    @Test
    void contentWidthAddsLeadAndPadding() {
        VoteView view = new VoteView(State.WAITING, "学院军", 18, 0, "", "", false, 0.0F);
        Plate plate = FormationVoteHudModel.plate(view, false, KEY);
        int width = plate.contentWidth(segments -> segments.size() * 10);
        assertEquals(Math.max(plate.firstLine().size(), plate.secondLine().size()) * 10 + 12 + 12,
                width);
        assertEquals(27, FormationVoteHudModel.height(plate));
        assertEquals(32, FormationVoteHudModel.height(FormationVoteHudModel.plate(
                new VoteView(State.OPEN, "", 2, 1, "", "", true, 0.0F), false, KEY)));
    }

    private static String line(List<TacticalHud.Segment> segments, Map<String, String> bundle) {
        StringBuilder out = new StringBuilder();
        for (TacticalHud.Segment segment : segments) {
            String text = HudTestSupport.render(segment.text(), bundle);
            out.append(segment.key() ? "[" + text + "]" : text);
        }
        return out.toString();
    }

    private static FormationSelectionSnapshot snapshot(boolean required, String faction,
                                                       FormationVotePhase phase, String own,
                                                       Map<String, Integer> tally) {
        return new FormationSelectionSnapshot(3L, required, faction,
                required ? "" : "millennium_seminar_mobile", phase, true, own,
                phase == FormationVotePhase.LOCKED ? "millennium_seminar_mobile" : "", tally,
                List.of(FACTION));
    }

    private static FormationSelectionView formation(String id, String name, String icon) {
        return new FormationSelectionView(id, name, "", icon, "infantry", "步兵营", 0, 40, true,
                "", List.of(), List.of(), List.of(), List.of());
    }
}
