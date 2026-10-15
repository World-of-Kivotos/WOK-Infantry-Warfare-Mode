package com.wok.infantry.formation;

import com.wok.infantry.loadout.LoadoutConfigData;
import com.wok.infantry.loadout.LoadoutEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClassLoadoutCopyPlannerTest {
    @Test
    void deepCopyReplacesOnlyOneTargetProfessionLoadout() {
        FormationConfigData formations = FormationLoadoutRuleEditor.apply(
                FormationConfigData.defaultConfig(), "academy", "default",
                "assault", "primary", "empty_primary",
                FormationLoadoutEditAction.EXCLUSIVE);
        LoadoutConfigData loadouts = LoadoutConfigData.defaultConfig();
        var sourceBackingBeforeCopy = loadouts.findClass("assault").orElseThrow();
        LoadoutEntry sourceGun = sourceBackingBeforeCopy.entries("primary").get(0);
        sourceBackingBeforeCopy.entries("primary").set(0, new LoadoutEntry(sourceGun.id(),
                sourceGun.displayName(), sourceGun.itemId(), sourceGun.count(),
                sourceGun.snbt(), 420));
        FormationDefinition originalTarget = formations.findFormation("academy",
                "millennium_seminar_mobile").orElseThrow();
        FormationClassRule originalTargetRule = originalTarget.findClass("support").orElseThrow();
        List<String> originalOrder = originalTarget.classes().stream()
                .map(FormationClassRule::classId).toList();
        int targetVehicleCount = originalTarget.vehicles().size();
        List<Integer> targetLimits = originalTarget.squads().stream()
                .map(squad -> squad.classLimit("support", originalTargetRule.squadLimit()))
                .toList();

        ClassLoadoutCopyPlanner.CopyPlan plan = ClassLoadoutCopyPlanner.plan(
                formations, loadouts, "academy", "default", "assault",
                "academy", "millennium_seminar_mobile", "support",
                ignored -> "copy_support");

        FormationDefinition source = plan.formations().findFormation(
                "academy", "default").orElseThrow();
        FormationDefinition target = plan.formations().findFormation(
                "academy", "millennium_seminar_mobile").orElseThrow();
        assertEquals(List.of("assault", "support", "engineer", "recon"),
                source.classes().stream().map(FormationClassRule::classId).toList());
        List<String> expectedOrder = originalOrder.stream()
                .map(id -> id.equals("support") ? "copy_support" : id).toList();
        assertEquals(expectedOrder,
                target.classes().stream().map(FormationClassRule::classId).toList());
        assertEquals(targetVehicleCount, target.vehicles().size());

        FormationClassRule copiedRule = target.findClass("copy_support").orElseThrow();
        assertEquals(originalTargetRule.displayName(), copiedRule.displayName());
        assertEquals(originalTargetRule.squadLimit(), copiedRule.squadLimit());
        assertEquals(List.of("empty_primary"),
                copiedRule.allowedEntriesFor("primary"));
        assertEquals("copy_support", plan.targetClassId());
        assertEquals("support", plan.replacedClassId());
        for (int index = 0; index < target.squads().size(); index++) {
            FormationSquadDefinition squad = target.squads().get(index);
            assertEquals(targetLimits.get(index),
                    squad.classLimit("copy_support", copiedRule.squadLimit()));
            assertFalse(squad.classLimits().containsKey("support"));
        }

        var sourceBacking = plan.loadouts().findClass("assault").orElseThrow();
        var copiedBacking = plan.loadouts().findClass("copy_support").orElseThrow();
        assertEquals(420, sourceBacking.entries("primary").get(0).ammoReserveLimit());
        assertEquals(420, copiedBacking.entries("primary").get(0).ammoReserveLimit());
        assertNotEquals(sourceBacking.id(), copiedBacking.id());
        assertEquals(sourceBacking.slotDefinitions().stream().map(slot ->
                        slot.id() + ":" + slot.target()).toList(),
                copiedBacking.slotDefinitions().stream().map(slot ->
                        slot.id() + ":" + slot.target()).toList());
        int sourceEntryCount = sourceBacking.entries("primary").size();
        copiedBacking.entries("primary").add(new LoadoutEntry(
                "copy_only", "复制专用", "minecraft:stick", 1, ""));
        assertEquals(sourceEntryCount, sourceBacking.entries("primary").size());
        assertEquals(sourceEntryCount + 1, copiedBacking.entries("primary").size());
    }

    @Test
    void identicalTargetAndDuplicateGeneratedIdFailClosed() {
        FormationConfigData formations = FormationConfigData.defaultConfig();
        LoadoutConfigData loadouts = LoadoutConfigData.defaultConfig();
        assertThrows(IllegalArgumentException.class, () ->
                ClassLoadoutCopyPlanner.plan(formations, loadouts,
                        "academy", "default", "assault",
                        "academy", "default", "assault",
                        ignored -> "copy_assault"));
        assertThrows(IllegalArgumentException.class, () ->
                ClassLoadoutCopyPlanner.plan(formations, loadouts,
                        "academy", "default", "assault",
                        "academy", "default", "support",
                        ignored -> "assault"));
        assertTrue(formations.findFormation("academy", "default").isPresent());
    }
}
