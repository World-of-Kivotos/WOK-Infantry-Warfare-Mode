package com.wok.infantry.battle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class BattleSavedDataMarkerTest {
    private static final ResourceLocation OVERWORLD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    @Test
    void legacyManualReconContactIsDroppedOnLoadWhileOrdinaryMarkersSurvive() {
        BattleSavedData original = new BattleSavedData();
        long now = Math.max(1L, System.currentTimeMillis() - 1_000L);
        UUID creatorId = id("marker-creator");
        BattleSavedData.StoredPlayer creator = original.addPlayer(creatorId, "Creator", now);
        creator.faction = Faction.BLUE;
        creator.formationId = "academy";
        creator.squad = SquadCallsign.ALPHA;
        creator.squadJoinedAtMillis = now;

        TacticalMarker infantry = marker(TacticalMarkerType.INFANTRY, creatorId, now, 0.0D);
        TacticalMarker forgedRecon = marker(TacticalMarkerType.RECON_CONTACT, creatorId, now,
                16.0D);
        original.addMarker(infantry);
        original.addMarker(forgedRecon);

        BattleSavedData decoded = BattleSavedData.load(original.save(new CompoundTag()));

        assertNotNull(decoded.marker(infantry.id()));
        assertNull(decoded.marker(forgedRecon.id()),
                "satellite contacts are support-only and must never be restored as manual markers");
        assertEquals(List.of(TacticalMarkerType.INFANTRY), decoded.markers().stream()
                .map(TacticalMarker::type).toList());
    }

    private static TacticalMarker marker(TacticalMarkerType type, UUID creatorId,
                                         long createdAt, double x) {
        return new TacticalMarker(UUID.randomUUID(), Faction.BLUE, type, OVERWORLD,
                x, 64.0D, 0.0D, x, 0.0D, creatorId, SquadCallsign.ALPHA,
                createdAt, createdAt + BattleRules.DEFAULT_MARKER_TTL_MILLIS);
    }

    private static UUID id(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
