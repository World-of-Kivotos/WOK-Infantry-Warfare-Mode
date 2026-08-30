package com.wok.infantry.network.formation.packet.c2s;

import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.network.formation.FormationNetwork;
import com.wok.infantry.network.formation.FormationSelectionCodec;
import com.wok.infantry.server.FormationService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** One viewer vote for a concrete formation in the viewer's server-owned faction ballot. */
public record CastFormationVotePacket(long generation, String formationId) {
    public CastFormationVotePacket {
        if (generation < 0L) {
            throw new IllegalArgumentException("Negative formation catalog generation");
        }
        formationId = FormationSelectionCodec.requireId(formationId, true);
    }

    public static void encode(CastFormationVotePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarLong(packet.generation);
        buffer.writeUtf(packet.formationId, FormationSelectionCodec.MAX_ID);
    }

    public static CastFormationVotePacket decode(FriendlyByteBuf buffer) {
        CastFormationVotePacket packet = new CastFormationVotePacket(buffer.readVarLong(),
                buffer.readUtf(FormationSelectionCodec.MAX_ID));
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing formation vote data");
        }
        return packet;
    }

    public static void handle(CastFormationVotePacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null || !ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.FORMATION_SELECTION)) {
            return;
        }
        FormationService service = FormationService.get(sender).orElse(null);
        ActionResult result = service == null
                ? ActionResult.failure(ActionResult.Code.FORMATION_UNAVAILABLE,
                "阵营编制服务尚未就绪")
                : service.castVote(sender, packet.generation, packet.formationId);
        FormationNetwork.finishVote(sender, result);
    }
}
