package com.wok.infantry.client.screen;

import com.wok.infantry.formation.FormationSupportPolicy;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationDetailView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.vote.FormationVotePhase;

import java.util.List;
import java.util.Map;

/**
 * Vote-page fixtures shaped like the preview's {@code data/mock.js}: 学院军 (academy, 18/40) with
 * 常规编制 / 千禧年研讨会机动部队 / 研讨会骑兵军团 and 凯撒 (caesar, 21/40) with 234机械化作战单元.
 */
final class FormationVoteFixtures {
    static final String ACADEMY = "academy";
    static final String CAESAR = "caesar";
    static final String DEFAULT = "default";
    static final String MOBILE = "millennium_seminar_mobile";
    static final String CAVALRY = "millennium_seminar_cavalry_corps";
    static final String CAESAR_234 = "caesar_234_mechanized";

    private FormationVoteFixtures() {
    }

    static FormationSelectionView formation(String id, String name, String category,
                                            int capacity, boolean available, String reason) {
        FormationDetailView detail = new FormationDetailView(
                List.of(new FormationDetailView.ClassQuota("突击兵", 8),
                        new FormationDetailView.ClassQuota("支援兵", 2)),
                List.of(new FormationDetailView.Vehicle("M1296 龙骑兵", 3,
                        FormationDetailView.Vehicle.NEVER)),
                List.of(new FormationDetailView.Squad("阿尔法小队", 8)), 0, 1,
                FormationDetailView.INHERIT_RESPAWN, List.of(),
                FormationSupportPolicy.Mode.ALLOW_LIST,
                List.of("wok_commander_support:recon_satellite"));
        return new FormationSelectionView(id, name, name + "的说明", "", category,
                category, 0, capacity, available, reason, List.of(), List.of(), List.of(),
                List.of(), detail);
    }

    static FactionSelectionView academy(int population, int capacity, boolean available,
                                        FormationVotePhase phase, String locked,
                                        int cavalryCapacity) {
        return new FactionSelectionView(ACADEMY, "学院军", "学院军战场阵营", population, capacity,
                available, List.of(
                formation(DEFAULT, "常规编制", "infantry", 40, true, ""),
                formation(MOBILE, "千禧年研讨会机动部队", "mechanized", 40, true, ""),
                formation(CAVALRY, "研讨会骑兵军团", "armored", cavalryCapacity, true, "")),
                phase, locked);
    }

    static FactionSelectionView caesar(int population, boolean available) {
        return new FactionSelectionView(CAESAR, "凯撒", "凯撒战场阵营", population, 40, available,
                List.of(formation(CAESAR_234, "234机械化作战单元", "mechanized", 40, true, "")),
                FormationVotePhase.NOT_STARTED, "");
    }

    /** Not joined; both ballots not started. */
    static FormationSelectionSnapshot unjoined() {
        return new FormationSelectionSnapshot(3L, true, "", "", FormationVotePhase.NOT_STARTED,
                false, "", "", Map.of(), List.of(
                academy(18, 40, true, FormationVotePhase.NOT_STARTED, "", 40),
                caesar(21, true)));
    }

    /** Joined academy without a formation, ballot in {@code phase}. */
    static FormationSelectionSnapshot joined(FormationVotePhase phase, boolean change, String own,
                                             Map<String, Integer> tally) {
        return new FormationSelectionSnapshot(3L, true, ACADEMY, "", phase, change, own, "",
                tally, List.of(academy(18, 40, true, phase, "", 40), caesar(21, true)));
    }

    static FormationSelectionSnapshot withFactions(FormationSelectionSnapshot base,
                                                   FactionSelectionView... factions) {
        return new FormationSelectionSnapshot(base.generation(), base.selectionRequired(),
                base.selectedFactionId(), base.selectedFormationId(), base.votePhase(),
                base.voteChangeAllowed(), base.ownVoteFormationId(), base.lockedFormationId(),
                base.voteTally(), List.of(factions));
    }

    static Map<String, Integer> tally(int def, int mobile, int cavalry) {
        return Map.of(DEFAULT, def, MOBILE, mobile, CAVALRY, cavalry);
    }
}
