package com.wok.infantry.network.battle.packet.c2s;

import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleService;
import com.wok.infantry.network.ServerRequestLimiter;
import com.wok.infantry.network.battle.BattleNetworkLimits;
import com.wok.infantry.server.LoadoutService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

/** The client selects only an ID; its quota is always derived from server-owned rules. */
public record AssignClassPacket(String classId) {
    public AssignClassPacket {
        classId = Objects.requireNonNullElse(classId, "").trim();
        if (classId.isEmpty() || classId.length() > BattleNetworkLimits.MAX_CLASS_ID_LENGTH) {
            throw new IllegalArgumentException("Invalid battle class id length");
        }
    }

    public static void encode(AssignClassPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.classId, BattleNetworkLimits.MAX_CLASS_ID_LENGTH);
    }

    public static AssignClassPacket decode(FriendlyByteBuf buffer) {
        AssignClassPacket decoded = new AssignClassPacket(
                buffer.readUtf(BattleNetworkLimits.MAX_CLASS_ID_LENGTH));
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing class assignment data");
        }
        return decoded;
    }

    public static void handle(AssignClassPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender == null || !ServerRequestLimiter.allow(sender,
                ServerRequestLimiter.Kind.CLASS_SELECTION)) {
            return;
        }
        BattleService service = BattleServerPacketSupport.serviceFor(sender);
        if (service == null || !BattleServerPacketSupport.ensurePlayer(service, sender)) {
            return;
        }
        LoadoutService loadoutService = LoadoutService.get(sender).orElse(null);
        if (loadoutService == null) {
            BattleServerPacketSupport.reject(sender, service,
                    ActionResult.Code.INVALID_CLASS_ID, "配装服务尚未启动");
            return;
        }
        String previousClass = service.assignedClass(sender.getUUID());
        ActionResult result = loadoutService.assignBattleClass(sender, packet.classId);
        BattleServerPacketSupport.finish(sender, service, result,
                result.success() && !previousClass.equals(service.assignedClass(sender.getUUID())));
    }
}
