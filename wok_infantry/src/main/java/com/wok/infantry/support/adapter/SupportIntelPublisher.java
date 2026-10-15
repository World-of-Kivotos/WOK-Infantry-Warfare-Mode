package com.wok.infantry.support.adapter;

import com.wok.infantry.battle.ActionResult;
import com.wok.infantry.battle.BattleService;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bounded bridge for publishing provider-generated contacts into the friendly tactical map.
 * Contacts are ephemeral, faction-isolated and never written to battle saved data.
 *
 * <p>Failure classes: a malformed batch (count, TTL, null or out-of-scan contacts) is a provider
 * defect and trips the circuit without refund; a missing online commander, an unavailable battle
 * service or a rejected publication only ends the current mission.</p>
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
        // A respawn replaces the ServerPlayer and only removes the old instance, which never
        // reports a disconnect; the scheduler may still hand that stale entity over once the
        // requester is offline. Only the requester's current session may publish. A commander
        // waiting on the death screen is still that session (vanilla removes the dead body after
        // one second, so isRemoved alone would wrongly end the scan).
        if (context == null || !(context.owner() instanceof ServerPlayer owner)
                || !isCurrentSession(owner, owner.server.getPlayerList()
                        .getPlayer(owner.getUUID()), owner.hasDisconnected())) {
            throw SupportSpawnException.endMission("临时情报只能由在线指挥官支援任务发布");
        }
        List<SupportIntelContact> batch = validatedBatch(contacts, ttlTicks,
                context.target().startX(), context.target().startZ(),
                context.definition().radius(),
                context.level().getMinBuildHeight(), context.level().getMaxBuildHeight());

        BattleService battle = BattleService.get(owner).orElse(null);
        if (battle == null) {
            throw SupportSpawnException.endMission("战局服务不可用，无法发布临时情报");
        }
        ActionResult result = battle.publishSupportIntel(owner, context.callId(),
                context.faction(), context.level().dimension(), batch, ttlTicks);
        if (!result.success()) {
            // Battle-side rejections (full roster, missing formation, changed faction) describe
            // the current match state rather than a broken integration.
            throw SupportSpawnException.endMission(result.message());
        }
    }

    /**
     * True when {@code owner} is the player list's live instance for its UUID: offline
     * requesters ({@code listed == null}) and stale pre-respawn instances are rejected, a dead
     * commander still on the death screen is accepted.
     */
    static boolean isCurrentSession(Object owner, Object listed, boolean disconnected) {
        return owner != null && !disconnected && listed == owner;
    }

    /**
     * Validates one provider batch and returns its contacts de-duplicated by entity id in first
     * occurrence order. Every violation is a provider defect and is reported as a non-refunding
     * {@link SupportSpawnException#providerBroken provider-broken} failure.
     */
    static List<SupportIntelContact> validatedBatch(List<SupportIntelContact> contacts,
                                                    int ttlTicks,
                                                    double centerX, double centerZ,
                                                    double radius,
                                                    int minBuildHeight, int maxBuildHeight)
            throws SupportSpawnException {
        if (contacts == null || contacts.size() > MAX_CONTACTS) {
            throw broken("临时情报目标数量超出上限");
        }
        if (ttlTicks < MIN_TTL_TICKS || ttlTicks > MAX_TTL_TICKS) {
            throw broken("临时情报持续时间超出上限");
        }
        double radiusSquared = radius * radius;
        Map<UUID, SupportIntelContact> unique = new LinkedHashMap<>();
        for (SupportIntelContact contact : contacts) {
            if (contact == null) {
                throw broken("临时情报包含空目标");
            }
            double dx = contact.x() - centerX;
            double dz = contact.z() - centerZ;
            if (!(dx * dx + dz * dz <= radiusSquared)
                    || contact.y() < minBuildHeight
                    || contact.y() >= maxBuildHeight) {
                throw broken("临时情报目标超出支援扫描区域");
            }
            unique.putIfAbsent(contact.entityId(), contact);
        }
        return List.copyOf(unique.values());
    }

    private static SupportSpawnException broken(String message) {
        return SupportSpawnException.providerBroken(message, null, false);
    }
}
