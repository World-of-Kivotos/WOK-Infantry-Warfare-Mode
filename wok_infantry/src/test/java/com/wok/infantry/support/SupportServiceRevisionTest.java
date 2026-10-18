package com.wok.infantry.support;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SupportServiceRevisionTest {
    private static final ResourceLocation OVERWORLD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    @Test
    void revisionDependsOnlyOnTheFactionVisibleView() {
        SupportDefinition definition = SupportDefinitionAndTargetTest.definition(
                "test", "support", SupportTargetMode.POINT);
        SupportOptionView blueOption = new SupportOptionView(
                definition, true, "", 500L, false);
        SupportOptionView redOption = new SupportOptionView(
                definition, true, "", 900L, false);
        long blueBefore = SupportService.visibleRevision(
                List.of(blueOption), List.of(), true, "");
        long redBefore = SupportService.visibleRevision(
                List.of(redOption), List.of(), true, "");

        SupportMissionView blueMission = new SupportMissionView(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                definition.id(), OVERWORLD, 10.0D, 20.0D, 10.0D, 20.0D,
                400L, 1);
        long blueAfter = SupportService.visibleRevision(
                List.of(blueOption), List.of(blueMission), true, "");
        long redAfterEnemyMutation = SupportService.visibleRevision(
                List.of(redOption), List.of(), true, "");

        assertNotEquals(blueBefore, blueAfter);
        assertEquals(redBefore, redAfterEnemyMutation);
    }
}
