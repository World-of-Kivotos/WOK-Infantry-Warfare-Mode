package com.wok.infantry.support;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * map-render-01: the support view of one faction member must never throw because a mission of
 * the faction belongs to a support the member's own formation does not open.
 */
class SupportServiceViewMergeTest {
    private static final ResourceLocation OVERWORLD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final long NOW = 1_000L;
    // Formation A (the commander's) opens the JDAM run; formation B (the teammate's) only recon.
    private static final SupportDefinition JDAM = SupportDefinitionAndTargetTest.definition(
            "wok_commander_support", "f15ex_jdam", SupportTargetMode.DIRECTIONAL);
    private static final SupportDefinition RECON = SupportDefinitionAndTargetTest.definition(
            "wok_commander_support", "recon_satellite", SupportTargetMode.POINT);

    @Test
    void adminAssignedTeammateInAnotherFormationSeesTheInboundStrikeAsReadOnly() {
        SupportOptionView recon = new SupportOptionView(RECON, true, "", 0L, false);
        SupportService.InFlightMission jdam = inbound(JDAM, 3_400L);

        // Before the fix viewFor built exactly this view and the constructor threw, which
        // aborted the snapshot broadcast for every player after this one.
        assertThrows(IllegalArgumentException.class, () -> new SupportView(List.of(recon),
                List.of(jdam.view()), NOW, 1L, true, ""));

        SupportView view = SupportService.mergeMissionOptions(NOW, List.of(recon),
                List.of(jdam));

        assertTrue(view.serviceAvailable());
        assertEquals(List.of(jdam.view()), view.activeMissions(),
                "the teammate must still receive the friendly danger area");
        assertEquals(2, view.options().size());
        assertSame(recon, view.options().get(0), "callable options keep their order and state");
        SupportOptionView readOnly = view.options().get(1);
        assertEquals(JDAM.id(), readOnly.id());
        assertTrue(readOnly.readOnlyMission());
        assertTrue(readOnly.active());
        assertFalse(readOnly.providerAvailable());
        assertFalse(readOnly.ready(NOW + 1_000_000L), "a read-only option is never callable");
        assertEquals(SupportService.FORMATION_CLOSED_REASON, readOnly.availabilityReason());
        assertEquals(3_400L, readOnly.readyAtGameTick());
        assertEquals(JDAM.radius(), readOnly.radius());
        assertTrue(readOnly.directional(), "geometry follows the definition the call used");
        assertFalse(recon.readOnlyMission());
    }

    @Test
    void memberWithoutAnyOpenSupportNoLongerLosesTheFactionMissions() {
        SupportService.InFlightMission jdam = inbound(JDAM, 0L);

        SupportView view = SupportService.mergeMissionOptions(NOW, List.of(), List.of(jdam));

        assertEquals(List.of(jdam.view()), view.activeMissions());
        assertEquals(1, view.options().size());
        assertTrue(view.options().get(0).readOnlyMission());
        assertEquals("", view.serviceMessage());
    }

    @Test
    void callableOptionThatLostItsProviderMidFlightStaysAnOrdinaryActiveOption() {
        SupportOptionView dropped = new SupportOptionView(JDAM, false, "适配器已熔断",
                2_000L, true);
        SupportService.InFlightMission jdam = inbound(JDAM, 2_000L);

        SupportView view = SupportService.mergeMissionOptions(NOW, List.of(dropped),
                List.of(jdam));

        SupportOptionView option = view.options().get(0);
        assertTrue(option.active());
        assertTrue(option.providerAvailable());
        assertFalse(option.readOnlyMission(),
                "only a formation-closed mission may use the read-only state");
        assertEquals("", option.availabilityReason());
    }

    @Test
    void activeFlagAlwaysMatchesTheFactionMissions() {
        SupportOptionView staleActive = new SupportOptionView(RECON, true, "", 0L, true);
        SupportOptionView staleIdle = new SupportOptionView(JDAM, true, "", 0L, false);
        SupportService.InFlightMission jdam = inbound(JDAM, 0L);

        SupportView view = SupportService.mergeMissionOptions(NOW,
                List.of(staleActive, staleIdle), List.of(jdam));

        assertFalse(view.options().get(0).active());
        assertTrue(view.options().get(0).ready(NOW));
        assertTrue(view.options().get(1).active());
        assertFalse(view.options().get(1).readOnlyMission());
    }

    @Test
    void duplicateMissionForOneSupportIsDroppedInsteadOfBreakingTheView() {
        SupportService.InFlightMission first = inbound(JDAM, 0L);
        SupportService.InFlightMission second = inbound(JDAM, 0L);

        SupportView view = SupportService.mergeMissionOptions(NOW, List.of(),
                List.of(first, second));

        assertEquals(List.of(first.view()), view.activeMissions());
        assertEquals(1, view.options().size());
    }

    @Test
    void noOptionsAndNoMissionsStillReportAnEmptyCatalog() {
        SupportView view = SupportService.mergeMissionOptions(NOW, List.of(), List.of());

        assertTrue(view.serviceAvailable());
        assertTrue(view.options().isEmpty());
        assertTrue(view.activeMissions().isEmpty());
        assertEquals(SupportView.empty(NOW, 0L).serviceMessage(), view.serviceMessage());
    }

    @Test
    void inFlightMissionMustMatchItsDefinition() {
        SupportMissionView reconView = new SupportMissionView(UUID.randomUUID(), RECON.id(),
                OVERWORLD, 0.0D, 0.0D, 0.0D, 0.0D, NOW, 1);
        assertThrows(IllegalArgumentException.class,
                () -> new SupportService.InFlightMission(JDAM, reconView, 0L));
    }

    private static SupportService.InFlightMission inbound(SupportDefinition definition,
                                                          long readyAt) {
        double endX = definition.directional() ? 140.0D : 20.0D;
        SupportMissionView view = new SupportMissionView(UUID.randomUUID(), definition.id(),
                OVERWORLD, 20.0D, -40.0D, endX, -40.0D, NOW + 200L, definition.stepCount());
        return new SupportService.InFlightMission(definition, view, readyAt);
    }
}
