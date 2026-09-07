package com.wok.infantry.support.adapter;

import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleService;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded bridge for publishing provider-generated contacts into the friendly tactical map.
 * Contacts are ephemeral, faction-isolated and never written to battle saved data.
 */
public final class SupportIntelPublisher {
    public static final int MAX_CONTACTS = 64;
    public static final int MIN_TTL_TICKS = 20;
    public static final int MAX_TTL_TICKS = 20 * 60 * 10;

    private SupportIntelPublisher() {
    }

    public static void publish(SupportSpawnContext context,
                               List<SupportIntelContact> contacts,
                               int ttlTicks) throws SupportSpawnException {
        if (context == null || !(context.owner() instanceof ServerPlayer owner)) {
            throw new SupportSpawnException("临时情报只能由在线指挥官支援任务发布");
        }
        if (contacts == null || contacts.size() > MAX_CONTACTS) {
            throw new SupportSpawnException("临时情报目标数量超出上限");
        }
        if (ttlTicks < MIN_TTL_TICKS || ttlTicks > MAX_TTL_TICKS) {
            throw new SupportSpawnException("临时情报持续时间超出上限");
        }

        double centerX = context.target().startX();
        double centerZ = context.target().startZ();
        double radius = context.definition().radius();
        double radiusSquared = radius * radius;
        Map<java.util.UUID, SupportIntelContact> unique = new LinkedHashMap<>();
        for (SupportIntelContact contact : contacts) {
            if (contact == null) {
                throw new SupportSpawnException("临时情报包含空目标");
            }
            double dx = contact.x() - centerX;
            double dz = contact.z() - centerZ;
            if (dx * dx + dz * dz > radiusSquared
                    || contact.y() < context.level().getMinBuildHeight()
                    || contact.y() >= context.level().getMaxBuildHeight()) {
                throw new SupportSpawnException("临时情报目标超出支援扫描区域");
            }
            unique.putIfAbsent(contact.entityId(), contact);
        }

        BattleService battle = BattleService.get(owner).orElse(null);
        if (battle == null) {
            throw new SupportSpawnException("战局服务不可用，无法发布临时情报");
        }
        ActionResult result = battle.publishSupportIntel(owner, context.callId(),
                context.faction(), context.level().dimension(),
                new ArrayList<>(unique.values()), ttlTicks);
        if (!result.success()) {
            throw new SupportSpawnException(result.message());
        }
    }
}
