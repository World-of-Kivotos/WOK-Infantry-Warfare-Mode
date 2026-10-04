package com.wok.infantry.network.formation;

import com.wok.infantry.formation.selection.FactionSelectionView;
import com.wok.infantry.formation.selection.FormationSelectionSnapshot;
import com.wok.infantry.formation.selection.FormationSelectionView;
import com.wok.infantry.formation.vote.FormationVotePhase;
import com.wok.infantry.network.formation.packet.c2s.SelectFormationPacket;
import com.wok.infantry.network.formation.packet.c2s.SelectFactionPacket;
import com.wok.infantry.network.formation.packet.c2s.CastFormationVotePacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
