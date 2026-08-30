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

/** First-stage player intent: choose only a public faction, never a private battle side. */
public record SelectFactionPacket(long generation, String factionId) {
    public SelectFactionPacket {
        if (generation < 0L) {
            throw new IllegalArgumentException("Negative formation catalog generation");
        }
        factionId = FormationSelectionCodec.requireId(factionId, true);
    }

    public static void encode(SelectFactionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarLong(packet.generation);
        buffer.writeUtf(packet.factionId, FormationSelectionCodec.MAX_ID);
    }

    public static SelectFactionPacket decode(FriendlyByteBuf buffer) {
        SelectFactionPacket packet = new SelectFactionPacket(buffer.readVarLong(),
                buffer.readUtf(FormationSelectionCodec.MAX_ID));
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing faction selection data");
        }
        return packet;
    }

    public static void handle(SelectFactionPacket packet,
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
                : service.selectFaction(sender, packet.generation, packet.factionId);
        FormationNetwork.finishFactionSelection(sender, result);
    }
}
