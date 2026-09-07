package com.wok.infantry.deployment;

import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.SquadCallsign;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeploymentSavedDataTest {
    private static final ResourceLocation OVERWORLD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final ResourceLocation NETHER =
            ResourceLocation.fromNamespaceAndPath("minecraft", "the_nether");

    @Test
    void versionOneMainBaseLoadsAndMigratesWithoutInventingFieldPoints() {
        DeploymentPoint legacyBase = new DeploymentPoint(id("legacy-base"), Faction.BLUE,
                OVERWORLD, new BlockPos(12, 65, -7), 37.5F,
                DeploymentPoint.DEFAULT_SUPPLY_RADIUS);
        DeploymentSavedData writer = new DeploymentSavedData();
        writer.setMainBase(legacyBase);
        CompoundTag current = writer.save(new CompoundTag());

        CompoundTag versionOne = new CompoundTag();
        versionOne.putInt("Version", 1);
        versionOne.put("MainBases", current.get("MainBases").copy());

        DeploymentSavedData loaded = DeploymentSavedData.load(versionOne);

        assertEquals(legacyBase, loaded.mainBase(Faction.BLUE).orElseThrow());
        assertTrue(loaded.fieldPoints(Faction.BLUE).isEmpty());
        assertTrue(loaded.fieldPoints(Faction.RED).isEmpty());

        CompoundTag migrated = loaded.save(new CompoundTag());
        assertEquals(DeploymentSavedData.DATA_VERSION, migrated.getInt("Version"));
        assertTrue(migrated.getList("FieldPoints", Tag.TAG_COMPOUND).isEmpty());
    }

    @Test
    void versionTwoRoundTripsMainBaseAndStableFieldPointIdentity() {
        DeploymentSavedData original = new DeploymentSavedData();
        DeploymentPoint mainBase = new DeploymentPoint(id("v2-main"), Faction.BLUE,
                OVERWORLD, new BlockPos(0, 64, 0), 90.0F,
                DeploymentPoint.DEFAULT_SUPPLY_RADIUS);
        FieldDeploymentPoint blueOne = field("blue-one", Faction.BLUE, OVERWORLD,
                new BlockPos(20, 64, 20));
        FieldDeploymentPoint blueTwo = field("blue-two", Faction.BLUE, NETHER,
                new BlockPos(-30, 70, 11));
        FieldDeploymentPoint redOne = field("red-one", Faction.RED, OVERWORLD,
                new BlockPos(80, 64, -40));
        original.setMainBase(mainBase);
        original.putFieldPoint(blueTwo);
        original.putFieldPoint(redOne);
        original.putFieldPoint(blueOne);

        CompoundTag encoded = original.save(new CompoundTag());
        assertEquals(DeploymentSavedData.DATA_VERSION, encoded.getInt("Version"));
        assertEquals(3, encoded.getList("FieldPoints", Tag.TAG_COMPOUND).size());

        DeploymentSavedData decoded = DeploymentSavedData.load(encoded.copy());

        assertEquals(mainBase, decoded.mainBase(Faction.BLUE).orElseThrow());
        assertEquals(original.fieldPoints(Faction.BLUE), decoded.fieldPoints(Faction.BLUE));
        assertEquals(original.fieldPoints(Faction.RED), decoded.fieldPoints(Faction.RED));
        assertEquals(blueOne, decoded.fieldPoint(blueOne.id()).orElseThrow());
        assertEquals(blueTwo, decoded.fieldPointAt(blueTwo.dimension(),
                blueTwo.anchorPosition()).orElseThrow());

        // Saving a decoded value must be canonical and stable, including list ordering.
        assertEquals(encoded, decoded.save(new CompoundTag()));
    }

    @Test
    void duplicateUuidAndAnchorAreRejectedWithoutReplacingFirstValidRecord() {
        UUID sharedId = id("shared-id");
        BlockPos sharedAnchor = new BlockPos(40, 64, 40);
        FieldDeploymentPoint first = new FieldDeploymentPoint(sharedId, Faction.BLUE,
                OVERWORLD, sharedAnchor, sharedAnchor.east().above(), 0.0F);
        FieldDeploymentPoint duplicateId = new FieldDeploymentPoint(sharedId, Faction.RED,
                OVERWORLD, new BlockPos(50, 64, 50), new BlockPos(51, 65, 50), 20.0F);
        FieldDeploymentPoint duplicateAnchor = new FieldDeploymentPoint(id("other-id"),
                Faction.RED, OVERWORLD, sharedAnchor, sharedAnchor.west().above(), 40.0F);
        FieldDeploymentPoint independent = field("independent", Faction.RED, NETHER,
                new BlockPos(70, 70, 70));

        DeploymentSavedData decoded = DeploymentSavedData.load(rootWithFields(List.of(
                serialized(first), serialized(duplicateId), serialized(duplicateAnchor),
                serialized(independent))));

        assertEquals(List.of(first), decoded.fieldPoints(Faction.BLUE));
        assertEquals(List.of(independent), decoded.fieldPoints(Faction.RED));
        assertEquals(first, decoded.fieldPoint(sharedId).orElseThrow());
        assertEquals(first, decoded.fieldPointAt(OVERWORLD, sharedAnchor).orElseThrow());
        assertFalse(decoded.fieldPoint(duplicateAnchor.id()).isPresent());

        CompoundTag canonical = decoded.save(new CompoundTag());
        assertEquals(2, canonical.getList("FieldPoints", Tag.TAG_COMPOUND).size());
    }

    @Test
    void sixteenthPointPerFactionIsRejectedButOtherFactionStillLoads() {
        List<FieldDeploymentPoint> blue = new ArrayList<>();
        List<CompoundTag> serialized = new ArrayList<>();
        for (int index = 0; index < 16; index++) {
            FieldDeploymentPoint point = field("blue-limit-" + index, Faction.BLUE,
                    OVERWORLD, new BlockPos(100 + index * 4, 64, 100));
            blue.add(point);
            serialized.add(serialized(point));
        }
        FieldDeploymentPoint red = field("red-after-blue-limit", Faction.RED, OVERWORLD,
                new BlockPos(-100, 64, -100));
        serialized.add(serialized(red));

        DeploymentSavedData decoded = DeploymentSavedData.load(rootWithFields(serialized));

        assertEquals(15, decoded.fieldPoints(Faction.BLUE).size());
        for (int index = 0; index < 15; index++) {
            assertTrue(decoded.fieldPoint(blue.get(index).id()).isPresent(),
                    "the first fifteen valid BLUE records must survive");
        }
        assertFalse(decoded.fieldPoint(blue.get(15).id()).isPresent(),
                "the sixteenth BLUE record must fail closed");
        assertEquals(List.of(red), decoded.fieldPoints(Faction.RED),
                "one faction reaching its cap must not discard valid enemy records");
        assertEquals(16, decoded.save(new CompoundTag())
                .getList("FieldPoints", Tag.TAG_COMPOUND).size());
    }

    @Test
    void failedPutLeavesExistingFieldPointStateByteForByteUnchanged() {
        DeploymentSavedData data = new DeploymentSavedData();
        DeploymentPoint mainBase = new DeploymentPoint(id("put-main-base"), Faction.BLUE,
                OVERWORLD, new BlockPos(0, 64, 0), 0.0F,
                DeploymentPoint.DEFAULT_SUPPLY_RADIUS);
        data.setMainBase(mainBase);
        FieldDeploymentPoint first = field("put-first", Faction.BLUE, OVERWORLD,
                new BlockPos(200, 64, 200));
        data.putFieldPoint(first);
        CompoundTag beforeDuplicate = data.save(new CompoundTag());

        data.putFieldPoint(new FieldDeploymentPoint(first.id(), Faction.RED, OVERWORLD,
                new BlockPos(210, 64, 210), new BlockPos(211, 65, 210), 0.0F));
        assertEquals(beforeDuplicate, data.save(new CompoundTag()),
                "duplicate UUID must not alter either primary or secondary indexes");

        data.putFieldPoint(new FieldDeploymentPoint(id("put-anchor-collision"), Faction.RED,
                first.dimension(), first.anchorPosition(), first.spawnPosition(), 0.0F));
        assertEquals(beforeDuplicate, data.save(new CompoundTag()),
                "duplicate dimension+anchor must not replace the existing owner");

        for (int index = 1; index < 15; index++) {
            data.putFieldPoint(field("put-limit-" + index, Faction.BLUE, OVERWORLD,
                    new BlockPos(200 + index * 4, 64, 200)));
        }
        CompoundTag atLimit = data.save(new CompoundTag());
        data.putFieldPoint(field("put-over-limit", Faction.BLUE, OVERWORLD,
                new BlockPos(400, 64, 400)));
        assertEquals(atLimit, data.save(new CompoundTag()),
                "a sixteenth field point must not pollute existing state");
        assertEquals(mainBase, data.mainBase(Faction.BLUE).orElseThrow(),
                "the independent main-base slot must coexist with all fifteen field points");
        assertEquals(15, data.fieldPoints(Faction.BLUE).size());
    }

    @Test
    void fieldPointCannotReuseMainBaseUuidDuringLoadOrMutation() {
        UUID shared = id("main-field-shared");
        DeploymentPoint mainBase = new DeploymentPoint(shared, Faction.BLUE, OVERWORLD,
                new BlockPos(5, 64, 5), 0.0F, DeploymentPoint.DEFAULT_SUPPLY_RADIUS);
        FieldDeploymentPoint conflicting = new FieldDeploymentPoint(shared, Faction.RED,
                OVERWORLD, new BlockPos(8, 64, 8), new BlockPos(9, 65, 8), 0.0F);

        DeploymentSavedData mainWriter = new DeploymentSavedData();
        mainWriter.setMainBase(mainBase);
        CompoundTag mainTag = mainWriter.save(new CompoundTag());
        CompoundTag root = rootWithFields(List.of(serialized(conflicting)));
        root.put("MainBases", mainTag.get("MainBases").copy());

        DeploymentSavedData decoded = DeploymentSavedData.load(root);

        assertEquals(mainBase, decoded.mainBase(Faction.BLUE).orElseThrow());
        assertTrue(decoded.fieldPoints(Faction.RED).isEmpty(),
                "a field point must fail closed when its UUID is owned by a main base");

        CompoundTag before = decoded.save(new CompoundTag());
        decoded.putFieldPoint(conflicting);
        assertEquals(before, decoded.save(new CompoundTag()),
                "runtime insertion must enforce the same cross-kind UUID uniqueness");
    }

    @Test
    void fieldSpawnMustRemainInsideTheBeaconSafetyScan() {
        BlockPos anchor = new BlockPos(30, 64, 30);
        assertThrows(IllegalArgumentException.class, () -> new FieldDeploymentPoint(
                id("far-runtime-spawn"), Faction.BLUE, OVERWORLD, anchor,
                anchor.offset(20, 1, 0), 0.0F));

        FieldDeploymentPoint valid = field("far-load-spawn", Faction.BLUE, OVERWORLD, anchor);
        CompoundTag corrupt = serialized(valid);
        corrupt.putInt("SpawnX", anchor.getX() + 20);
        DeploymentSavedData loaded = DeploymentSavedData.load(rootWithFields(List.of(corrupt)));
        assertTrue(loaded.fieldPoints(Faction.BLUE).isEmpty(),
                "损坏存档不得把实体信标投射成远距离出生点");
    }

    @Test
    void versionThreeRoundTripsOneArrowDirectedVehiclePointPerFaction() {
        DeploymentSavedData original = new DeploymentSavedData();
        VehicleDeploymentPoint blue = new VehicleDeploymentPoint(Faction.BLUE,
                OVERWORLD, new BlockPos(300, 64, 300), Direction.EAST);
        VehicleDeploymentPoint red = new VehicleDeploymentPoint(Faction.RED,
                NETHER, new BlockPos(-300, 70, -300), Direction.SOUTH);

        assertTrue(original.bindVehiclePoint(blue));
        assertTrue(original.bindVehiclePoint(red));
        assertFalse(original.bindVehiclePoint(new VehicleDeploymentPoint(Faction.BLUE,
                OVERWORLD, blue.anchorPosition().east(), Direction.WEST)));

        CompoundTag encoded = original.save(new CompoundTag());
        assertEquals(DeploymentSavedData.DATA_VERSION, encoded.getInt("Version"));
        assertEquals(2, encoded.getList("VehiclePoints", Tag.TAG_COMPOUND).size());
        DeploymentSavedData decoded = DeploymentSavedData.load(encoded.copy());

        assertEquals(blue, decoded.vehiclePoint(Faction.BLUE).orElseThrow());
        assertEquals(red, decoded.vehiclePoint(Faction.RED).orElseThrow());
        assertEquals(blue, decoded.vehiclePointAt(OVERWORLD,
                blue.anchorPosition()).orElseThrow());
        assertEquals(270.0F, blue.yaw());
        assertEquals(encoded, decoded.save(new CompoundTag()));
    }

    @Test
    void vehicleAnchorCannotBeSharedAndCorruptLoadKeepsFirstFaction() {
        BlockPos anchor = new BlockPos(410, 64, 410);
        VehicleDeploymentPoint blue = new VehicleDeploymentPoint(Faction.BLUE,
                OVERWORLD, anchor, Direction.NORTH);
        VehicleDeploymentPoint red = new VehicleDeploymentPoint(Faction.RED,
                OVERWORLD, anchor, Direction.WEST);
        CompoundTag blueTag = serialized(blue);
        CompoundTag redTag = serialized(red);
        CompoundTag root = new CompoundTag();
        root.putInt("Version", 3);
        root.put("MainBases", new ListTag());
        root.put("FieldPoints", new ListTag());
        ListTag vehicles = new ListTag();
        vehicles.add(blueTag);
        vehicles.add(redTag);
        root.put("VehiclePoints", vehicles);

        DeploymentSavedData loaded = DeploymentSavedData.load(root);

        assertEquals(blue, loaded.vehiclePoint(Faction.BLUE).orElseThrow());
        assertTrue(loaded.vehiclePoint(Faction.RED).isEmpty());
        assertEquals(1, loaded.save(new CompoundTag())
                .getList("VehiclePoints", Tag.TAG_COMPOUND).size());
    }

    @Test
    void fieldAndVehicleRecordsCannotShareOnePhysicalAnchor() {
        BlockPos anchor = new BlockPos(500, 64, 500);
        DeploymentSavedData vehicleFirst = new DeploymentSavedData();
        assertTrue(vehicleFirst.bindVehiclePoint(new VehicleDeploymentPoint(Faction.BLUE,
                OVERWORLD, anchor, Direction.NORTH)));
        assertFalse(vehicleFirst.putFieldPoint(field("field-after-vehicle", Faction.RED,
                OVERWORLD, anchor)));

        DeploymentSavedData fieldFirst = new DeploymentSavedData();
        assertTrue(fieldFirst.putFieldPoint(field("field-before-vehicle", Faction.BLUE,
                OVERWORLD, anchor)));
        assertFalse(fieldFirst.bindVehiclePoint(new VehicleDeploymentPoint(Faction.RED,
                OVERWORLD, anchor, Direction.SOUTH)));
    }

    @Test
    void versionFiveRoundTripsSquadScopedRallyCooldownAndRejectsSharedAnchor() {
        assertEquals(8L * 60L * 20L, DeploymentService.RALLY_SQUAD_COOLDOWN_TICKS);
        DeploymentSavedData data = new DeploymentSavedData();
        BlockPos anchor = new BlockPos(610, 64, 610);
        RallyDeploymentPoint rally = new RallyDeploymentPoint(id("alpha-rally"),
                Faction.BLUE, "millennium_seminar_mobile", SquadCallsign.ALPHA,
                OVERWORLD, anchor, anchor.above(), 45.0F);

        assertTrue(data.putRally(rally));
        assertFalse(data.putFieldPoint(field("field-on-rally", Faction.BLUE,
                OVERWORLD, anchor)));
        assertEquals(List.of(rally), data.rallies(Faction.BLUE,
                "millennium_seminar_mobile", SquadCallsign.ALPHA));
        assertTrue(data.rallies(Faction.BLUE, "millennium_seminar_mobile",
                SquadCallsign.BRAVO).isEmpty());
        data.startRallyCooldown(Faction.BLUE, "millennium_seminar_mobile",
                SquadCallsign.ALPHA, 12_345L);
        assertEquals(2_345L, data.rallyCooldownRemainingTicks(Faction.BLUE,
                "millennium_seminar_mobile", SquadCallsign.ALPHA, 10_000L));
        assertEquals(0L, data.rallyCooldownRemainingTicks(Faction.BLUE,
                "millennium_seminar_mobile", SquadCallsign.BRAVO, 10_000L));
        assertEquals(0L, data.rallyCooldownRemainingTicks(Faction.BLUE,
                "another_formation", SquadCallsign.ALPHA, 10_000L));
        assertEquals(0L, data.rallyCooldownRemainingTicks(Faction.RED,
                "millennium_seminar_mobile", SquadCallsign.ALPHA, 10_000L));

        CompoundTag encoded = data.save(new CompoundTag());
        assertEquals(DeploymentSavedData.DATA_VERSION, encoded.getInt("Version"));
        assertEquals(1, encoded.getList("Rallies", Tag.TAG_COMPOUND).size());
        assertEquals(1, encoded.getList("RallyCooldowns", Tag.TAG_COMPOUND).size());
        DeploymentSavedData decoded = DeploymentSavedData.load(encoded.copy());

        assertEquals(rally, decoded.rally(rally.id()).orElseThrow());
        assertEquals(rally, decoded.rallyAt(OVERWORLD, anchor).orElseThrow());
        assertEquals(2_345L, decoded.rallyCooldownRemainingTicks(Faction.BLUE,
                "millennium_seminar_mobile", SquadCallsign.ALPHA, 10_000L));
        assertEquals(encoded, decoded.save(new CompoundTag()));
        assertEquals(rally, decoded.removeRally(OVERWORLD, anchor).orElseThrow());
        assertTrue(decoded.rally(rally.id()).isEmpty());
        assertEquals(2_345L, decoded.rallyCooldownRemainingTicks(Faction.BLUE,
                "millennium_seminar_mobile", SquadCallsign.ALPHA, 10_000L),
                "removing the radio must not clear its squad deployment cooldown");
        assertEquals(0L, decoded.rallyCooldownRemainingTicks(Faction.BLUE,
                "millennium_seminar_mobile", SquadCallsign.ALPHA, 12_345L));
        assertEquals(0, decoded.save(new CompoundTag())
                .getList("RallyCooldowns", Tag.TAG_COMPOUND).size());
    }

    private static CompoundTag rootWithFields(List<CompoundTag> fields) {
        CompoundTag root = new CompoundTag();
        root.putInt("Version", 2);
        root.put("MainBases", new ListTag());
        ListTag list = new ListTag();
        fields.forEach(field -> list.add(field.copy()));
        root.put("FieldPoints", list);
        return root;
    }

    private static CompoundTag serialized(FieldDeploymentPoint point) {
        DeploymentSavedData one = new DeploymentSavedData();
        one.putFieldPoint(point);
        ListTag list = one.save(new CompoundTag()).getList("FieldPoints", Tag.TAG_COMPOUND);
        assertEquals(1, list.size(), "test fixture point must be serializable on its own");
        return list.getCompound(0).copy();
    }

    private static CompoundTag serialized(VehicleDeploymentPoint point) {
        DeploymentSavedData one = new DeploymentSavedData();
        assertTrue(one.bindVehiclePoint(point));
        ListTag list = one.save(new CompoundTag())
                .getList("VehiclePoints", Tag.TAG_COMPOUND);
        assertEquals(1, list.size(), "test fixture vehicle point must serialize");
        return list.getCompound(0).copy();
    }

    private static FieldDeploymentPoint field(String seed, Faction faction,
                                               ResourceLocation dimension, BlockPos anchor) {
        return new FieldDeploymentPoint(id(seed), faction, dimension, anchor,
                anchor.east().above(), 15.0F);
    }

    private static UUID id(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
