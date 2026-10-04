package com.wok.infantry.network.formation;

import com.wok.infantry.formation.FormationSupportPolicy;
import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationDetailView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.selection.FormationSupportLabel;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.network.formation.packet.c2s.SelectFormationPacket;
import com.wok.infantry.network.formation.packet.c2s.SelectFactionPacket;
import com.wok.infantry.network.formation.packet.c2s.CastFormationVotePacket;
import com.wok.infantry.network.formation.packet.s2c.FormationCatalogPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationSelectionCodecTest {
    @Test
    void catalogRoundTripsWithoutLeakingBattleSide() {
        FormationSelectionSnapshot expected = new FormationSelectionSnapshot(7L, true,
                "academy", "", FormationVotePhase.OPEN, true, "armored", "",
                Map.of("armored", 2),
                List.of(new FactionSelectionView("academy", "学院军", "学院联军", 3, 40,
                        true, List.of(new FormationSelectionView("armored", "装甲旅", "重装推进",
                        "wok_infantry:textures/gui/formations/armored.png",
                        "armored", "装甲营", 2, 20, true, "",
                        List.of("assault", "support"), List.of("Alpha (8)"),
                        List.of("M1A2 [superbwarfare:m_1a_2]"),
                        List.of("兵站上限 1", "支援：无"))))));
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        FormationSelectionCodec.encode(encoded, expected);

        FormationSelectionSnapshot decoded = FormationSelectionCodec.decode(encoded);

        assertEquals(expected, decoded);
        assertEquals(0, encoded.readableBytes());
    }

    @Test
    void overlongDisplayTextIsClippedInsteadOfFailingTheSnapshot() {
        String summary = "支援：白名单 " + "wok_commander_support:x、".repeat(12);
        String reason = "原因".repeat(200);
        FormationSelectionSnapshot snapshot = new FormationSelectionSnapshot(1L, true,
                "", "", FormationVotePhase.NOT_STARTED, false, "", "", Map.of(),
                List.of(new FactionSelectionView("academy", "学院军", "", 0, 40, true,
                        List.of(new FormationSelectionView("default", "常规", "", "",
                                "infantry", "步兵", 0, 40, false, reason,
                                List.of("assault"), List.of(), List.of(),
                                List.of(summary))))));
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        FormationSelectionCodec.encode(encoded, snapshot);

        FormationSelectionView decoded = FormationSelectionCodec.decode(encoded)
                .factions().get(0).formations().get(0);

        String clippedSummary = decoded.capabilities().get(0);
        assertEquals(FormationSelectionCodec.MAX_SUMMARY, clippedSummary.length());
        assertEquals(summary.substring(0, FormationSelectionCodec.MAX_SUMMARY - 1) + "…",
                clippedSummary);
        assertEquals(FormationSelectionCodec.MAX_REASON, decoded.unavailableReason().length());
        assertEquals(0, encoded.readableBytes());
    }

    @Test
    void clipNeverSplitsASurrogatePair() {
        String value = "a".repeat(9) + "😀" + "b";

        String clipped = FormationSelectionCodec.clip(value, 11);

        assertEquals("a".repeat(9) + "…", clipped);
        assertEquals("short", FormationSelectionCodec.clip("short", 11));
    }

    @Test
    void clientIntentRejectsInvalidIdsAndTrailingBytes() {
        assertThrows(IllegalArgumentException.class,
                () -> new SelectFormationPacket(1L, "BLUE", "default"));

        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        new SelectFormationPacket(1L, "academy", "default");
        encoded.writeVarLong(1L);
        encoded.writeUtf("academy", FormationSelectionCodec.MAX_ID);
        encoded.writeUtf("default", FormationSelectionCodec.MAX_ID);
        encoded.writeByte(1);
        assertThrows(IllegalArgumentException.class,
                () -> SelectFormationPacket.decode(encoded));

        FriendlyByteBuf faction = new FriendlyByteBuf(Unpooled.buffer());
        SelectFactionPacket.encode(new SelectFactionPacket(2L, "academy"), faction);
        assertEquals(new SelectFactionPacket(2L, "academy"),
                SelectFactionPacket.decode(faction));
        FriendlyByteBuf vote = new FriendlyByteBuf(Unpooled.buffer());
        CastFormationVotePacket.encode(new CastFormationVotePacket(2L, "light_infantry"),
                vote);
        assertEquals(new CastFormationVotePacket(2L, "light_infantry"),
                CastFormationVotePacket.decode(vote));
    }

    @Test
    void protocolFiveRoundTripsBallotStateDetailsAndSupportNames() {
        FormationDetailView detail = new FormationDetailView(
                List.of(new FormationDetailView.ClassQuota("突击兵", 8),
                        new FormationDetailView.ClassQuota("侦察兵", 1)),
                List.of(new FormationDetailView.Vehicle("M1296 龙骑兵", 3,
                                FormationDetailView.Vehicle.NEVER),
                        new FormationDetailView.Vehicle("悍马 M2", 2, 300)),
                List.of(new FormationDetailView.Squad("阿尔法小队", 8)), 2, 1, 15,
                List.of("M1296 龙骑兵"), FormationSupportPolicy.Mode.ALLOW_LIST,
                List.of("wok_commander_support:recon_satellite"));
        FormationSelectionView mobile = new FormationSelectionView("millennium_seminar_mobile",
                "千禧年研讨会机动部队", "先头力量",
                "wok_infantry:textures/gui/formations/millennium_seminar_mobile.png",
                "mechanized", "机械化步兵营", 0, 40, true, "", List.of("assault"),
                List.of("Alpha (8)"), List.of("M1296 龙骑兵 ×3（不可再生）"),
                List.of("兵站上限 2"), detail);
        FormationSelectionSnapshot expected = new FormationSelectionSnapshot(9L, true, "", "",
                FormationVotePhase.NOT_STARTED, false, "", "", Map.of(), List.of(
                new FactionSelectionView("academy", "学院军", "学院联军", 18, 40, true,
                        List.of(mobile), FormationVotePhase.LOCKED, "millennium_seminar_mobile"),
                new FactionSelectionView("caesar", "凯撒", "", 21, 40, true, List.of(
                        new FormationSelectionView("caesar_234_mechanized", "234", "",
                                "mechanized", "机械化步兵营", 0, 40, true, "", List.of(),
                                List.of(), List.of(), List.of())),
                        FormationVotePhase.OPEN, "")),
                List.of(new FormationSupportLabel("wok_commander_support:recon_satellite",
                        "support.wok_commander_support.recon_satellite", "侦察卫星")));
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        FormationSelectionCodec.encode(encoded, expected);

        FormationSelectionSnapshot decoded = FormationSelectionCodec.decode(encoded);

        assertEquals(expected, decoded);
        assertEquals(0, encoded.readableBytes());
        assertEquals("突击兵", decoded.factions().get(0).formations().get(0).detail()
                .classQuotas().get(0).displayName(), "public class names, not class ids");
        assertEquals(FormationVotePhase.LOCKED, decoded.factions().get(0).votePhase(),
                "unjoined viewers see every faction's ballot phase");
    }

    @Test
    void catalogPacketCarriesTheLockNotice() {
        FormationSelectionSnapshot snapshot = new FormationSelectionSnapshot(2L, false,
                "academy", "default", List.of());
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        FormationCatalogPacket.encode(new FormationCatalogPacket(snapshot, false, true), encoded);

        FormationCatalogPacket decoded = FormationCatalogPacket.decode(encoded);

        assertTrue(decoded.lockNotice());
        assertFalse(decoded.openScreen());
        assertFalse(new FormationCatalogPacket(snapshot, true).lockNotice());
    }

    @Test
    void lockedPhaseRequiresALockedFormationBothWays() {
        FormationSelectionSnapshot inconsistent = new FormationSelectionSnapshot(1L, true, "",
                "", FormationVotePhase.NOT_STARTED, false, "", "", Map.of(), List.of(
                new FactionSelectionView("academy", "学院军", "", 0, 40, true, List.of(),
                        FormationVotePhase.LOCKED, "")));
        assertThrows(IllegalArgumentException.class, () -> FormationSelectionCodec.encode(
                new FriendlyByteBuf(Unpooled.buffer()), inconsistent));

        FriendlyByteBuf forged = catalogPrefix();
        forged.writeUtf("OPEN");
        forged.writeUtf("default");
        assertThrows(IllegalArgumentException.class, () -> FormationSelectionCodec.decode(forged));
    }

    @Test
    void detailListsAndNumbersAreBoundedOnDecode() {
        FriendlyByteBuf tooMany = formationPrefix();
        tooMany.writeVarInt(FormationSelectionCodec.MAX_DETAIL_ENTRIES + 1);
        assertThrows(IllegalArgumentException.class, () -> FormationSelectionCodec.decode(tooMany));

        FriendlyByteBuf badCooldown = formationPrefix();
        badCooldown.writeVarInt(0);
        badCooldown.writeVarInt(1);
        badCooldown.writeUtf("CV90");
        badCooldown.writeVarInt(1);
        badCooldown.writeVarInt(-2);
        assertThrows(IllegalArgumentException.class,
                () -> FormationSelectionCodec.decode(badCooldown));

        FormationSelectionSnapshot tooLong = snapshotWith(new FormationDetailView(List.of(),
                List.of(new FormationDetailView.Vehicle("CV90", 1,
                        FormationSelectionCodec.MAX_COOLDOWN_SECONDS + 1)), List.of(), 0, 0,
                FormationDetailView.INHERIT_RESPAWN, List.of(), FormationSupportPolicy.Mode.NONE,
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> FormationSelectionCodec.encode(
                new FriendlyByteBuf(Unpooled.buffer()), tooLong));
    }

    @Test
    void overlongDetailNamesAreClippedNotRejected() {
        String name = "超长载具名称".repeat(20);
        FormationSelectionSnapshot snapshot = snapshotWith(new FormationDetailView(
                List.of(new FormationDetailView.ClassQuota(name, 2)),
                List.of(new FormationDetailView.Vehicle(name, 1, 60)),
                List.of(new FormationDetailView.Squad(name, 8)), 0, 0,
                FormationDetailView.INHERIT_RESPAWN, List.of(name),
                FormationSupportPolicy.Mode.NONE, List.of()));
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        FormationSelectionCodec.encode(encoded, snapshot);

        FormationDetailView decoded = FormationSelectionCodec.decode(encoded).factions().get(0)
                .formations().get(0).detail();

        assertEquals(FormationSelectionCodec.MAX_DISPLAY_NAME,
                decoded.vehicles().get(0).displayName().length());
        assertTrue(decoded.classQuotas().get(0).displayName().endsWith("…"));
    }

    /**
     * Two factions with the maximum 32 formations each, every one with a long description,
     * 8 classes, 12 vehicle groups, 5 squads and 32 supports, stay far below the 1 MiB payload
     * limit (the support names travel once, in the label table).
     */
    @Test
    void worstRealisticCatalogStaysWellBelowThePacketLimit() {
        String name = "名".repeat(12);
        List<FormationDetailView.ClassQuota> classes = java.util.stream.IntStream.range(0, 8)
                .mapToObj(index -> new FormationDetailView.ClassQuota(name, 8)).toList();
        List<FormationDetailView.Vehicle> vehicles = java.util.stream.IntStream.range(0, 12)
                .mapToObj(index -> new FormationDetailView.Vehicle(name, 3, 900)).toList();
        List<FormationDetailView.Squad> squads = java.util.stream.IntStream.range(0, 5)
                .mapToObj(index -> new FormationDetailView.Squad(name, 8)).toList();
        List<String> supports = java.util.stream.IntStream.range(0, 32)
                .mapToObj(index -> "wok_commander_support:support_number_" + index).toList();
        FormationDetailView detail = new FormationDetailView(classes, vehicles, squads, 4, 2, 30,
                List.of(name, name), FormationSupportPolicy.Mode.ALLOW_LIST, supports);
        List<String> classIds = java.util.Collections.nCopies(8, "assault_x");
        List<String> squadSummaries = java.util.Collections.nCopies(5, "Alpha (8)");
        List<String> vehicleSummaries = java.util.Collections.nCopies(12, name + " ×3（15分钟）");
        List<String> capabilities = java.util.stream.Stream.concat(
                java.util.stream.Stream.of("兵站上限 4", "队包上限/小队 2", "重生等待 30 秒"),
                supports.stream()).toList();
        List<FormationSelectionView> formations = java.util.stream.IntStream.range(0, 32)
                .mapToObj(index -> new FormationSelectionView("formation_" + index, name,
                        "述".repeat(200), "", "infantry", name, 0, 40, true, "", classIds,
                        squadSummaries, vehicleSummaries, capabilities, detail)).toList();
        List<FormationSupportLabel> labels = supports.stream()
                .map(id -> new FormationSupportLabel(id, "support." + id, name)).toList();
        FormationSelectionSnapshot snapshot = new FormationSelectionSnapshot(1L, true, "", "",
                FormationVotePhase.NOT_STARTED, false, "", "", Map.of(), List.of(
                new FactionSelectionView("academy", name, "", 18, 40, true, formations),
                new FactionSelectionView("caesar", name, "", 21, 40, true, formations)), labels);
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        FormationSelectionCodec.encode(encoded, snapshot);

        assertTrue(encoded.readableBytes() < 1_000_000, "catalog bytes: "
                + encoded.readableBytes());
        assertEquals(snapshot, FormationSelectionCodec.decode(encoded));
    }

    private static FormationSelectionSnapshot snapshotWith(FormationDetailView detail) {
        return new FormationSelectionSnapshot(1L, true, "", "", FormationVotePhase.NOT_STARTED,
                false, "", "", Map.of(), List.of(new FactionSelectionView("academy", "学院军", "",
                0, 40, true, List.of(new FormationSelectionView("default", "常规", "", "",
                "infantry", "步兵营", 0, 40, true, "", List.of(), List.of(), List.of(), List.of(),
                detail)))));
    }

    /** Hand-written catalog up to the first faction's ballot fields. */
    private static FriendlyByteBuf catalogPrefix() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarLong(1L);
        buffer.writeBoolean(true);
        buffer.writeUtf("");
        buffer.writeUtf("");
        buffer.writeUtf("NOT_STARTED");
        buffer.writeBoolean(false);
        buffer.writeUtf("");
        buffer.writeUtf("");
        buffer.writeVarInt(0);
        buffer.writeVarInt(1);
        buffer.writeUtf("academy");
        buffer.writeUtf("学院军");
        buffer.writeUtf("");
        buffer.writeVarInt(0);
        buffer.writeVarInt(40);
        buffer.writeBoolean(true);
        return buffer;
    }

    /** Hand-written catalog up to the start of one formation's detail. */
    private static FriendlyByteBuf formationPrefix() {
        FriendlyByteBuf buffer = catalogPrefix();
        buffer.writeUtf("NOT_STARTED");
        buffer.writeUtf("");
        buffer.writeVarInt(1);
        buffer.writeUtf("default");
        buffer.writeUtf("常规");
        buffer.writeUtf("");
        buffer.writeUtf("");
        buffer.writeUtf("infantry");
        buffer.writeUtf("步兵营");
        buffer.writeVarInt(0);
        buffer.writeVarInt(40);
        buffer.writeBoolean(true);
        buffer.writeUtf("");
        for (int list = 0; list < 4; list++) {
            buffer.writeVarInt(0);
        }
        return buffer;
    }

    @Test
    void catalogRejectsOversizedCountsBeforeAllocation() {
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        encoded.writeVarLong(1L);
        encoded.writeBoolean(true);
        encoded.writeUtf("");
        encoded.writeUtf("");
        encoded.writeUtf("NOT_STARTED");
        encoded.writeBoolean(false);
        encoded.writeUtf("");
        encoded.writeUtf("");
        encoded.writeVarInt(0);
        encoded.writeVarInt(FormationSelectionCodec.MAX_FACTIONS + 1);
        assertThrows(IllegalArgumentException.class,
                () -> FormationSelectionCodec.decode(encoded));
    }
}
