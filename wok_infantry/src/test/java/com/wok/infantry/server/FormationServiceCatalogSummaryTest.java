package com.wok.infantry.server;

import com.wok.infantry.formation.FormationCapabilityProfile;
import com.wok.infantry.formation.FormationCategory;
import com.wok.infantry.formation.FormationClassRule;
import com.wok.infantry.formation.FormationDefinition;
import com.wok.infantry.formation.FormationSupportPolicy;
import com.wok.infantry.network.formation.FormationSelectionCodec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationServiceCatalogSummaryTest {
    /** The five-entry allow list that kicked players on login with "invalid player data". */
    private static final List<String> LONG_ALLOW_LIST = List.of(
            "wok_commander_support:recon_satellite",
            "wok_commander_support:f16c_gbu12_paveway_500lb",
            "wok_commander_support:howitzer_3round_barrage",
            "wok_commander_support:howitzer_105mm_rapid_3round_barrage",
            "wok_commander_support:howitzer_105mm_5round_barrage");

    @Test
    void supportAllowListIsSentOneIdPerSummary() {
        List<String> summaries = FormationService.capabilitySummaries(formation(
                new FormationSupportPolicy(FormationSupportPolicy.Mode.ALLOW_LIST,
                        LONG_ALLOW_LIST)));

        assertTrue(summaries.stream()
                .allMatch(summary -> summary.length() <= FormationSelectionCodec.MAX_SUMMARY));
        int first = summaries.indexOf("支援：白名单 " + LONG_ALLOW_LIST.get(0));
        assertTrue(first >= 0, summaries.toString());
        assertEquals(LONG_ALLOW_LIST.subList(1, LONG_ALLOW_LIST.size()),
                summaries.subList(first + 1, first + LONG_ALLOW_LIST.size()));
        // The client joins summaries with "、", so the rendered line is unchanged.
        assertTrue(String.join("、", summaries)
                .endsWith("支援：白名单 " + String.join("、", LONG_ALLOW_LIST)));
    }

    @Test
    void fixedSupportModesStaySingleSummaries() {
        assertEquals("支援：无", last(FormationService.capabilitySummaries(formation(
                new FormationSupportPolicy(FormationSupportPolicy.Mode.NONE, List.of())))));
        assertEquals("支援：全部已注册项目", last(FormationService.capabilitySummaries(formation(
                new FormationSupportPolicy(FormationSupportPolicy.Mode.ALL, List.of())))));
    }

    private static String last(List<String> values) {
        return values.get(values.size() - 1);
    }

    private static FormationDefinition formation(FormationSupportPolicy support) {
        return new FormationDefinition("default", "Default", "", FormationCategory.INFANTRY,
                true, 40, new FormationCapabilityProfile(null, null, null, support),
                List.of(new FormationClassRule("assault", 40, Map.of())), List.of(), List.of());
    }
}
