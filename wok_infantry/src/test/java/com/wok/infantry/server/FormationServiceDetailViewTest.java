package com.wok.infantry.server;

import com.wok.infantry.formation.FormationCapabilityProfile;
import com.wok.infantry.formation.FormationCategory;
import com.wok.infantry.formation.FormationClassRule;
import com.wok.infantry.formation.FormationDefinition;
import com.wok.infantry.formation.FormationSquadDefinition;
import com.wok.infantry.formation.FormationSupportPolicy;
import com.wok.infantry.formation.FormationVehicleDefinition;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationDetailView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.network.formation.FormationSelectionCodec;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Structured catalog detail of formation protocol 5 (player-09: public names, not IDs). */
class FormationServiceDetailViewTest {
    @Test
    void detailShowsProfessionNamesAndGroupsVehicles() {
        FormationDefinition formation = new FormationDefinition("mobile", "机动部队", "",
                FormationCategory.MECHANIZED, true, 40,
                new FormationCapabilityProfile(null, null, null, new FormationSupportPolicy(
                        FormationSupportPolicy.Mode.ALL, List.of())),
                List.of(new FormationClassRule("assault", "突击兵", 8, Map.of()),
                        new FormationClassRule("recon", "侦察兵", 2, Map.of())),
                List.of(new FormationSquadDefinition("alpha", "阿尔法", 8, Map.of())),
                List.of(vehicle("m1296_a", "M1296 龙骑兵", -1),
                        vehicle("m1296_b", "M1296 龙骑兵", -1),
                        vehicle("hmmwv", "悍马 M2", 300)));

        FormationDetailView detail = FormationService.detailView(formation,
                List.of("wok_commander_support:recon_satellite"));

        assertEquals(List.of(new FormationDetailView.ClassQuota("突击兵", 8),
                new FormationDetailView.ClassQuota("侦察兵", 2)), detail.classQuotas(),
                "profession display names, never the internal class IDs");
        assertEquals(List.of(new FormationDetailView.Vehicle("M1296 龙骑兵", 2,
                        FormationDetailView.Vehicle.NEVER),
                new FormationDetailView.Vehicle("悍马 M2", 1, 300)), detail.vehicles());
        assertEquals(List.of(new FormationDetailView.Squad("阿尔法", 8)), detail.squads());
        assertEquals(List.of("wok_commander_support:recon_satellite"), detail.supportIds(),
                "ALL lists every registered support");
    }

    @Test
    void outOfRangeNumbersAreClampedSoTheCatalogStaysEncodable() {
        FormationDefinition formation = new FormationDefinition("big", "大编制", "",
                FormationCategory.INFANTRY, true, 40, null,
                List.of(new FormationClassRule("assault", "突击兵", 500, Map.of())),
                List.of(new FormationSquadDefinition("alpha", "阿尔法", 500, Map.of())),
                List.of());

        FormationDetailView detail = FormationService.detailView(formation, List.of());

        assertEquals(FormationSelectionCodec.MAX_DETAIL_COUNT,
                detail.classQuotas().get(0).squadLimit());
        assertEquals(FormationSelectionCodec.MAX_DETAIL_COUNT, detail.squads().get(0).capacity());
        FormationSelectionSnapshot snapshot = new FormationSelectionSnapshot(1L, true, "", "",
                FormationVotePhase.NOT_STARTED, false, "", "", Map.of(), List.of(
                new FactionSelectionView("academy", "学院军", "", 0, 40, true, List.of(
                        new FormationSelectionView("big", "大编制", "", "", "infantry", "步兵营",
                                0, 40, true, "", List.of(), List.of(), List.of(), List.of(),
                                detail)))));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        FormationSelectionCodec.encode(buffer, snapshot);

        assertEquals(snapshot, FormationSelectionCodec.decode(buffer));
    }

    private static FormationVehicleDefinition vehicle(String id, String name, int cooldown) {
        return new FormationVehicleDefinition(id, name, "superbwarfare:truck", 0.0D, 0.0D, 0.0D,
                0.0F, cooldown);
    }
}
