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

/** Client intent containing IDs only; side, capacity, loadouts, entities and positions stay server-owned. */
public record SelectFormationPacket(long generation, String factionId, String formationId) {
    public SelectFormationPacket {
        if (generation < 0L) {
            throw new IllegalArgumentException("Negative formation catalog generation");
        }
        factionId = FormationSelectionCodec.requireId(factionId, true);
        formationId = FormationSelectionCodec.requireId(formationId, true);
    }

    public static void encode(SelectFormationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarLong(packet.generation);
        buffer.writeUtf(packet.factionId, FormationSelectionCodec.MAX_ID);
        buffer.writeUtf(packet.formationId, FormationSelectionCodec.MAX_ID);
    }

    public static SelectFormationPacket decode(FriendlyByteBuf buffer) {
        SelectFormationPacket packet = new SelectFormationPacket(buffer.readVarLong(),
                buffer.readUtf(FormationSelectionCodec.MAX_ID),
                buffer.readUtf(FormationSelectionCodec.MAX_ID));
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing formation selection data");
        }
        return packet;
    }

    public static void handle(SelectFormationPacket packet,
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
                : service.select(sender, packet.generation, packet.factionId, packet.formationId);
        FormationNetwork.finishSelection(sender, result);
    }
}
