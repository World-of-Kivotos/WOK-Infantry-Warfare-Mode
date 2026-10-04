package com.wok.infantry.testmode;

import com.wok.infantry.battle.Faction;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestModeRulesTest {
    private static final List<String> FACTIONS = List.of("academy", "caesar");
    private static final List<String> FORMATIONS = List.of("default",
            "millennium_seminar_mobile", "millennium_seminar_cavalry_corps");

    // ---- switch -------------------------------------------------------------------------------

    @Test
    void switchChangesOnlyWhenTheRequestedStateDiffers() {
        assertEquals(TestModeRules.Transition.ENABLED, TestModeRules.transition(false, true));
        assertEquals(TestModeRules.Transition.ALREADY_ENABLED,
                TestModeRules.transition(true, true));
        assertEquals(TestModeRules.Transition.DISABLED, TestModeRules.transition(true, false));
        assertEquals(TestModeRules.Transition.ALREADY_DISABLED,
                TestModeRules.transition(false, false));
        assertTrue(TestModeRules.Transition.ENABLED.changed());
        assertTrue(TestModeRules.Transition.DISABLED.changed());
        assertFalse(TestModeRules.Transition.ALREADY_ENABLED.changed());
        assertFalse(TestModeRules.Transition.ALREADY_DISABLED.changed());
        assertTrue(TestModeRules.Transition.ENABLED.enabledAfter());
        assertTrue(TestModeRules.Transition.ALREADY_ENABLED.enabledAfter());
        assertFalse(TestModeRules.Transition.DISABLED.enabledAfter());
        assertFalse(TestModeRules.Transition.ALREADY_DISABLED.enabledAfter());
    }

    @Test
    void switchStateSurvivesASaveAndLoad() {
        TestModeSavedData data = new TestModeSavedData();
        assertFalse(data.enabled(), "a new world starts without the test mode");
        data.set(true, 1_759_600_000_000L, "Steve");
        assertTrue(data.isDirty(), "a switch must be written with the world");
        TestModeSavedData loaded = TestModeSavedData.load(data.save(new CompoundTag()));
        assertTrue(loaded.enabled(), "restart keeps the test mode on");
        assertEquals(1_759_600_000_000L, loaded.changedAtMillis());
        assertEquals("Steve", loaded.changedBy());
        loaded.set(false, 1_759_600_100_000L, "控制台");
        TestModeSavedData off = TestModeSavedData.load(loaded.save(new CompoundTag()));
        assertFalse(off.enabled());
        assertEquals("控制台", off.changedBy());
    }

    @Test
    void savedSwitchClipsOversizedNamesAndIgnoresMissingFields() {
        TestModeSavedData empty = TestModeSavedData.load(new CompoundTag());
        assertFalse(empty.enabled(), "a file without fields reads as off");
        assertEquals("", empty.changedBy());
        TestModeSavedData data = new TestModeSavedData();
        data.set(true, -5L, "x".repeat(200));
        assertEquals(64, data.changedBy().length());
        assertEquals(0L, data.changedAtMillis(), "negative times are clamped");
    }

    // ---- faction ------------------------------------------------------------------------------

    @Test
    void factionArgumentWinsThenCurrentThenFirstPublicFaction() {
        TestModeRules.FactionChoice argument = TestModeRules.chooseFaction("Caesar", "academy",
                FACTIONS);
        assertTrue(argument.ok());
        assertEquals("caesar", argument.factionId(), "the argument may switch the faction");
        assertEquals(TestModeRules.FactionSource.ARGUMENT, argument.source());

        TestModeRules.FactionChoice current = TestModeRules.chooseFaction("", "caesar", FACTIONS);
        assertEquals("caesar", current.factionId());
        assertEquals(TestModeRules.FactionSource.CURRENT, current.source());

        TestModeRules.FactionChoice first = TestModeRules.chooseFaction(null, "", FACTIONS);
        assertEquals("academy", first.factionId());
        assertEquals(TestModeRules.FactionSource.FIRST_PUBLIC, first.source());

        TestModeRules.FactionChoice disabledCurrent = TestModeRules.chooseFaction("",
                "gehenna", FACTIONS);
        assertEquals("academy", disabledCurrent.factionId(),
                "a current faction that is no longer enabled falls back to the first");
    }

    @Test
    void unknownFactionArgumentIsAnErrorNotAFallback() {
        TestModeRules.FactionChoice unknown = TestModeRules.chooseFaction("gehenna", "academy",
                FACTIONS);
        assertFalse(unknown.ok());
        assertTrue(unknown.error().contains("gehenna"));
        assertFalse(TestModeRules.chooseFaction("", "", List.of()).ok(),
                "a catalog without enabled factions cannot start a test");
    }

    // ---- formation ----------------------------------------------------------------------------

    @Test
    void formationArgumentIsLockedWhenTheFactionIsNotLockedYet() {
        TestModeRules.FormationChoice choice = TestModeRules.chooseFormation(
                "millennium_seminar_mobile", "", FORMATIONS);
        assertTrue(choice.ok());
        assertEquals("millennium_seminar_mobile", choice.chosenId());
        assertEquals("millennium_seminar_mobile", choice.effectiveId());
        assertEquals(TestModeRules.FormationSource.ARGUMENT, choice.source());
        assertTrue(choice.needsLock());
        assertFalse(choice.lockKept());
    }

    @Test
    void anExistingLockIsKeptEvenWhenTheArgumentNamesAnotherFormation() {
        TestModeRules.FormationChoice other = TestModeRules.chooseFormation(
                "millennium_seminar_mobile", "default", FORMATIONS);
        assertTrue(other.ok());
        assertEquals("default", other.effectiveId(), "the player joins the locked formation");
        assertTrue(other.lockKept(), "the report must say the lock was kept");
        assertFalse(other.needsLock(), "a locked result is never changed");

        TestModeRules.FormationChoice same = TestModeRules.chooseFormation("default", "default",
                FORMATIONS);
        assertFalse(same.lockKept());
        assertFalse(same.needsLock());

        TestModeRules.FormationChoice noArgument = TestModeRules.chooseFormation("",
                "millennium_seminar_cavalry_corps", FORMATIONS);
        assertEquals(TestModeRules.FormationSource.LOCKED, noArgument.source());
        assertEquals("millennium_seminar_cavalry_corps", noArgument.effectiveId());
        assertFalse(noArgument.needsLock());
    }

    @Test
    void withoutArgumentAndLockDefaultComesFirstThenTheFirstCandidate() {
        TestModeRules.FormationChoice preferred = TestModeRules.chooseFormation(null, "",
                List.of("millennium_seminar_mobile", "default"));
        assertEquals("default", preferred.effectiveId(),
                "default wins even when it is not listed first");
        assertEquals(TestModeRules.FormationSource.DEFAULT, preferred.source());
        assertTrue(preferred.needsLock());

        TestModeRules.FormationChoice first = TestModeRules.chooseFormation("", "",
                List.of("caesar_234_mechanized", "caesar_reserve"));
        assertEquals("caesar_234_mechanized", first.effectiveId());
        assertEquals(TestModeRules.FormationSource.FIRST_CANDIDATE, first.source());
    }

    @Test
    void unusableFormationArgumentAndEmptyCatalogAreErrors() {
        assertFalse(TestModeRules.chooseFormation("ghost", "", FORMATIONS).ok());
        assertFalse(TestModeRules.chooseFormation("ghost", "default", FORMATIONS).ok(),
                "an unknown argument is refused even when the faction is locked");
        assertTrue(TestModeRules.chooseFormation("retired", "retired", List.of()).ok(),
                "naming the locked formation itself is accepted; joining reports its state");
        assertFalse(TestModeRules.chooseFormation("", "", List.of()).ok());
    }

    // ---- squad --------------------------------------------------------------------------------

    @Test
    void firstFreeCallsignFollowsTheFormationOrder() {
        List<String> configured = List.of("alpha", "bravo", "charlie");
        assertEquals("alpha", TestModeRules.firstFreeCallsign(configured, Set.of()));
        assertEquals("bravo", TestModeRules.firstFreeCallsign(configured, Set.of("ALPHA")));
        assertEquals("charlie", TestModeRules.firstFreeCallsign(configured,
                List.of("alpha", "bravo")));
        assertEquals("", TestModeRules.firstFreeCallsign(configured,
                List.of("alpha", "bravo", "charlie")), "every squad exists: join one instead");
        assertEquals("", TestModeRules.firstFreeCallsign(null, Set.of()));
    }

    // ---- main bases ---------------------------------------------------------------------------

    @Test
    void blueBaseSitsAtTheSpawnAndRedSixtyFourBlocksEast() {
        List<TestModeRules.BaseColumn> blue = TestModeRules.baseColumns(Faction.BLUE, 100, -20);
        assertEquals(100, blue.get(0).x(), "blue tries the spawn itself first");
        assertEquals(-20, blue.get(0).z());
        List<TestModeRules.BaseColumn> red = TestModeRules.baseColumns(Faction.RED, 100, -20);
        assertEquals(64, TestModeRules.RED_BASE_OFFSET_X);
        assertEquals(100 + 64, red.get(0).x(), "red tries spawn +64 X first");
        assertEquals(-20, red.get(0).z());
        List<TestModeRules.BaseColumn> redCenters = TestModeRules.baseCenters(Faction.RED, 100,
                -20);
        assertEquals(List.of(164, 36), redCenters.stream().map(TestModeRules.BaseColumn::x)
                .toList(), "west of the spawn only after everything east of it");
        for (TestModeRules.BaseColumn column : blue) {
            assertTrue(Math.abs(column.x() - 100) <= 24 && Math.abs(column.z() + 20) <= 24,
                    "blue stays near the spawn: " + column);
        }
        for (TestModeRules.BaseColumn column : red) {
            assertTrue(Math.abs(column.x() - 100) >= 40 && Math.abs(column.z() + 20) <= 24,
                    "red stays apart from blue: " + column);
            for (TestModeRules.BaseColumn blueColumn : blue) {
                assertFalse(column.x() == blueColumn.x() && column.z() == blueColumn.z(),
                        "the two sides never share a column: " + column);
            }
        }
    }

    @Test
    void searchAroundACentreGrowsRingByRingWithoutRepeats() {
        TestModeRules.BaseColumn center = new TestModeRules.BaseColumn(0, 0, "出生点");
        List<TestModeRules.BaseColumn> columns = TestModeRules.searchColumns(center);
        assertEquals(center, columns.get(0), "the centre itself comes first");
        // 1 + 8 + 16 + 24 columns for rings at 8, 16 and 24 blocks.
        assertEquals(49, columns.size());
        assertEquals(49, columns.stream().map(column -> column.x() + "," + column.z())
                .distinct().count(), "no column is tried twice");
        int previousRing = 0;
        for (TestModeRules.BaseColumn column : columns) {
            int ring = Math.max(Math.abs(column.x()), Math.abs(column.z()));
            assertTrue(ring >= previousRing, "rings never shrink: " + column);
            assertEquals(0, column.x() % TestModeRules.BASE_SEARCH_STEP);
            assertEquals(0, column.z() % TestModeRules.BASE_SEARCH_STEP);
            assertTrue(ring <= TestModeRules.BASE_SEARCH_RADIUS);
            previousRing = ring;
        }
        assertTrue(columns.get(1).label().startsWith("出生点 附近"),
                "a searched column says where it lies: " + columns.get(1).label());
    }

    @Test
    void basesFaceEachOther() {
        assertEquals(270.0F, TestModeRules.baseYaw(Faction.BLUE), "blue looks east");
        assertEquals(90.0F, TestModeRules.baseYaw(Faction.RED), "red looks west");
        assertEquals(270.0F, TestModeRules.baseYaw(Faction.BLUE, 0));
        assertEquals(90.0F, TestModeRules.baseYaw(Faction.RED, 64),
                "red east of the spawn looks west towards it");
        assertEquals(270.0F, TestModeRules.baseYaw(Faction.RED, -64),
                "审查修正: red west of the spawn looks east towards it, not away");
    }

    // ---- room (审查修正) ----------------------------------------------------------------------

    @Test
    void roomIsCheckedAgainstTheFactionMaximumAndTheFormationCapacity() {
        assertEquals("", TestModeRules.seatRefusal("学院军", 0, 40, "常规编制", 40),
                "an empty faction takes the player");
        assertEquals("", TestModeRules.seatRefusal("学院军", 39, 40, "常规编制", 40),
                "the last free place is still a place");
        String full = TestModeRules.seatRefusal("学院军", 40, 40, "常规编制", 40);
        assertTrue(full.contains("学院军已满") && full.contains("最多 40 人")
                && full.contains("未做任何改动"), full);
        String small = TestModeRules.seatRefusal("学院军", 12, 40, "机动部队", 12);
        assertTrue(small.contains("最多 12 人") && small.contains("机动部队")
                        && small.contains("容量 12"),
                "a formation smaller than the faction bounds it after the lock: " + small);
        assertEquals("", TestModeRules.seatRefusal("学院军", 11, 40, "机动部队", 12));
        assertFalse(TestModeRules.seatRefusal("学院军", 0, 40, "幽灵编制", 0).isEmpty(),
                "a formation without capacity never takes anyone");
    }

    @Test
    void borderCheckMatchesBlockSemantics() {
        // A border of radius 50 around the origin: blocks -50 … 49 are inside.
        assertTrue(TestModeRules.insideBorder(-50, -50, 50, 50, 0, 0));
        assertTrue(TestModeRules.insideBorder(-50, -50, 50, 50, 49, -50));
        assertFalse(TestModeRules.insideBorder(-50, -50, 50, 50, 50, 0));
        assertFalse(TestModeRules.insideBorder(-50, -50, 50, 50, 0, -51));
        // A spawn near the east edge: the red centre is outside, the west centre is not.
        List<TestModeRules.BaseColumn> red = TestModeRules.baseCenters(Faction.RED, 20, 0);
        assertFalse(TestModeRules.insideBorder(-50, -50, 50, 50, red.get(0).x(), red.get(0).z()));
        assertTrue(TestModeRules.insideBorder(-50, -50, 50, 50, red.get(1).x(), red.get(1).z()));
    }
}
