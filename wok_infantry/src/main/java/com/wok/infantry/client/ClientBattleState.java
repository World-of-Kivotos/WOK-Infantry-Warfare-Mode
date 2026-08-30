package com.wok.infantry.client;

import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.battle.MemberPosition;
import com.wok.infantry.battle.MemberView;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.SquadView;
import com.wok.infantry.battle.TacticalMarker;
import com.wok.infantry.deployment.DeploymentView;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Client cache of the latest server-filtered battle view. */
public final class ClientBattleState {
    private static final AtomicLong GENERATION = new AtomicLong();
    private static volatile BattleSnapshot snapshot;
    private static volatile long snapshotReceivedNanos;
    private static volatile BattleFeedback feedback;
    private static volatile long feedbackReceivedNanos;

    private ClientBattleState() {
    }

    public static BattleSnapshot snapshot() {
        return snapshot;
    }

    public static long generation() {
        return GENERATION.get();
    }

    public static void update(BattleSnapshot value) {
        Objects.requireNonNull(value, "value");
        BattleSnapshot current = snapshot;
        // SimpleChannel preserves packet order. The revision is a viewer-visible content
        // fingerprint rather than a global monotonic counter, so numeric ordering is meaningless.
        boolean structureChanged = current == null
                || !current.viewerId().equals(value.viewerId())
                || current.revision() != value.revision()
                || !current.classQuotas().equals(value.classQuotas())
                || current.support().structuralRevision()
                != value.support().structuralRevision()
                || deploymentStructureChanged(current.deployment(), value.deployment());
        snapshot = value;
        snapshotReceivedNanos = System.nanoTime();
        if (structureChanged) {
            GENERATION.incrementAndGet();
        }
    }

    public static void clear() {
        snapshot = null;
        snapshotReceivedNanos = 0L;
        feedback = null;
        feedbackReceivedNanos = 0L;
        GENERATION.incrementAndGet();
    }

    public static void showFeedback(boolean success, String message) {
        feedback = new BattleFeedback(success, Objects.requireNonNullElse(message, ""));
        feedbackReceivedNanos = System.nanoTime();
    }

    public static BattleFeedback feedback() {
        BattleFeedback value = feedback;
        if (value == null || System.nanoTime() - feedbackReceivedNanos > 5_000_000_000L) {
            return null;
        }
        return value;
    }

    public static List<SquadView> squads() {
        BattleSnapshot value = snapshot;
        return value == null ? List.of() : value.squads();
    }

    public static SquadView squad(SquadCallsign callsign) {
        if (callsign == null) {
            return null;
        }
        return squads().stream()
                .filter(candidate -> candidate.callsign() == callsign)
                .findFirst().orElse(null);
    }

    public static SquadView ownSquad() {
        BattleSnapshot value = snapshot;
        return value == null ? null : squad(value.ownSquad());
    }

    public static MemberView member(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        for (SquadView squad : squads()) {
            for (MemberView member : squad.members()) {
                if (member.playerId().equals(playerId)) {
                    return member;
                }
            }
        }
        return null;
    }

    public static MemberView viewer() {
        BattleSnapshot value = snapshot;
        return value == null ? null : member(value.viewerId());
    }

    public static boolean isSameSquad(UUID playerId) {
        BattleSnapshot value = snapshot;
        if (value == null || value.ownSquad() == null || playerId == null) {
            return false;
        }
        SquadView own = ownSquad();
        return own != null && own.members().stream()
                .anyMatch(member -> member.playerId().equals(playerId));
    }

    public static MemberPosition position(UUID playerId) {
        BattleSnapshot value = snapshot;
        if (value == null || playerId == null) {
            return null;
        }
        return value.alliedPositions().stream()
                .filter(position -> position.playerId().equals(playerId))
                .findFirst().orElse(null);
    }

    public static List<TacticalMarker> activeMarkers() {
        BattleSnapshot value = snapshot;
        if (value == null) {
            return List.of();
        }
        long now = estimatedServerTimeMillis(value);
        return value.markers().stream().filter(marker -> !marker.expiredAt(now)).toList();
    }

    /** Advances the last authoritative server timestamp with a local monotonic clock. */
    public static long estimatedServerTimeMillis() {
        BattleSnapshot value = snapshot;
        return value == null ? 0L : estimatedServerTimeMillis(value);
    }

    /** Advances the support service game tick between one-second authoritative snapshots. */
    public static long estimatedSupportGameTick() {
        BattleSnapshot value = snapshot;
        if (value == null) {
            return 0L;
        }
        return estimateSupportGameTick(value.support().serverGameTick(),
                snapshotReceivedNanos, System.nanoTime());
    }

    /** Package-visible pure clock calculation keeps rollover and saturation behavior testable. */
    static long estimateSupportGameTick(long authoritativeTick, long receivedNanos,
                                        long currentNanos) {
        long elapsedNanos = currentNanos - receivedNanos;
        long elapsedTicks = elapsedNanos <= 0L ? 0L : elapsedNanos / 50_000_000L;
        long safeAuthoritativeTick = Math.max(0L, authoritativeTick);
        return elapsedTicks > Long.MAX_VALUE - safeAuthoritativeTick
                ? Long.MAX_VALUE : safeAuthoritativeTick + elapsedTicks;
    }

    private static long estimatedServerTimeMillis(BattleSnapshot value) {
        long elapsedMillis = Math.max(0L,
                (System.nanoTime() - snapshotReceivedNanos) / 1_000_000L);
        long authoritativeNow = Math.max(0L, value.serverTimeMillis());
        return elapsedMillis > Long.MAX_VALUE - authoritativeNow
                ? Long.MAX_VALUE : authoritativeNow + elapsedMillis;
    }

    /** Ignore ticking countdown fields; render reads those directly from the newest snapshot. */
    private static boolean deploymentStructureChanged(DeploymentView current,
                                                      DeploymentView updated) {
        return current.revision() != updated.revision()
                || current.phase() != updated.phase()
                || !Objects.equals(current.selectedPointId(), updated.selectedPointId())
                || current.canChangeClass() != updated.canChangeClass()
                || current.canChangeSquad() != updated.canChangeSquad()
                || current.canDeploy() != updated.canDeploy()
                || current.canResupply() != updated.canResupply()
                || !current.points().equals(updated.points());
    }

    public record BattleFeedback(boolean success, String message) {
    }
}
