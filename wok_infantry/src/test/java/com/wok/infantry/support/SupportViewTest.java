package com.wok.infantry.support;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportViewTest {
    private static final ResourceLocation OVERWORLD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    @Test
    void emptyAndUnavailableViewsDoNotInventConcreteCapabilities() {
        SupportView empty = SupportView.empty(40L, 6L);
        SupportView unavailable = SupportView.unavailable(45L, 7L, "missing");

        assertTrue(empty.serviceAvailable());
        assertTrue(empty.options().isEmpty());
        assertFalse(unavailable.serviceAvailable());
        assertEquals("missing", unavailable.serviceMessage());
        assertTrue(unavailable.options().isEmpty());
        assertTrue(unavailable.activeMissions().isEmpty());
    }

    @Test
    void cooldownAndActiveHelpersCannotReportReadyPrematurely() {
        SupportDefinition definition = SupportDefinitionAndTargetTest.definition(
                "test", "support", SupportTargetMode.POINT);
        SupportOptionView cooling = new SupportOptionView(
                definition, true, "ignored", 120L, false);
        assertEquals("", cooling.availabilityReason());
        assertEquals(20L, cooling.cooldownRemainingTicks(100L));
        assertFalse(cooling.ready(100L));
        assertTrue(cooling.ready(120L));

        SupportOptionView active = new SupportOptionView(
                definition, true, "", 0L, true);
        assertFalse(active.ready(500L));
    }

    @Test
    void viewRejectsDuplicateOptionsOrMissionsWithoutDefinitions() {
        SupportDefinition definition = SupportDefinitionAndTargetTest.definition(
                "test", "support", SupportTargetMode.DIRECTIONAL);
        SupportOptionView option = new SupportOptionView(
                definition, true, "", 0L, false);
        assertThrows(IllegalArgumentException.class,
                () -> new SupportView(List.of(option, option), List.of(),
                        0L, 0L, true, ""));
        SupportMissionView unknown = new SupportMissionView(UUID.randomUUID(),
                ResourceLocation.fromNamespaceAndPath("unknown", "missing"), OVERWORLD,
                0.0D, 0.0D, 20.0D, 0.0D, 0L, 1);
        assertThrows(IllegalArgumentException.class,
                () -> new SupportView(List.of(option), List.of(unknown),
                        0L, 0L, true, ""));
        assertThrows(IllegalArgumentException.class,
                () -> new SupportMissionView(UUID.randomUUID(), definition.id(), OVERWORLD,
                        0.0D, 0.0D, 20.0D, 0.0D, 0L,
                        SupportDefinition.MAX_STEPS + 1));
    }

    @Test
    void availabilityReasonFitsNetworkLimitWithoutSplittingEmoji() {
        SupportDefinition definition = SupportDefinitionAndTargetTest.definition(
                "test", "support", SupportTargetMode.POINT);
        String longReason = "x".repeat(159) + "😀" + "tail";
        SupportOptionView option = new SupportOptionView(
                definition, false, longReason, 0L, false);

        assertEquals(159, option.availabilityReason().length());
        assertFalse(Character.isHighSurrogate(option.availabilityReason()
                .charAt(option.availabilityReason().length() - 1)));
        assertTrue(option.availabilityReason().length()
                <= SupportOptionView.MAX_AVAILABILITY_REASON_LENGTH);
    }
}
