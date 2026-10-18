package com.wok.infantry.client;

import com.wok.infantry.battle.SquadCallsign;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable client-side bridge between screens and the battle network layer.
 *
 * <p>The client UI only emits player intent through this class. The installed
 * handler must translate that intent to server-bound packets; it must never
 * mutate {@link ClientBattleState} optimistically.</p>
 */
public final class BattleClientActions {
    private static final Handler NOOP = new Handler() {
    };

    private static volatile Handler handler = NOOP;

    private BattleClientActions() {
    }

    public static void install(Handler replacement) {
        handler = Objects.requireNonNull(replacement, "replacement");
    }

    public static void reset() {
        handler = NOOP;
    }

    public static boolean available() {
        return handler != NOOP;
    }

    public static void requestSnapshot() {
        handler.requestSnapshot();
    }

    public static void createSquad(SquadCallsign callsign) {
        handler.createSquad(Objects.requireNonNull(callsign, "callsign"));
    }

    public static void joinSquad(SquadCallsign callsign) {
        handler.joinSquad(Objects.requireNonNull(callsign, "callsign"));
    }

    public static void leaveSquad() {
        handler.leaveSquad();
    }

    public static void disbandSquad() {
        handler.disbandSquad();
    }

    public static void transferLeadership(UUID playerId) {
        handler.transferLeadership(Objects.requireNonNull(playerId, "playerId"));
    }

    public static void kickMember(UUID playerId) {
        handler.kickMember(Objects.requireNonNull(playerId, "playerId"));
    }

    public static void claimCommander() {
        handler.claimCommander();
    }

    public static void resignCommander() {
        handler.resignCommander();
    }

    public static void transferCommander(UUID playerId) {
        handler.transferCommander(Objects.requireNonNull(playerId, "playerId"));
    }

    public static void selectClass(String classId) {
        handler.selectClass(Objects.requireNonNullElse(classId, "").trim());
    }

    public static void openLoadout() {
        handler.openLoadout();
    }

    public static void selectDeploymentPoint(UUID pointId) {
        handler.selectDeploymentPoint(Objects.requireNonNull(pointId, "pointId"));
    }

    public static void deploy() {
        handler.deploy();
    }

    public static void redeploy() {
        handler.redeploy();
    }

    public static void resupply() {
        handler.resupply();
    }

    public static void createMarker(MarkerDraft marker) {
        handler.createMarker(Objects.requireNonNull(marker, "marker"));
    }

    public static void removeMarker(UUID markerId) {
        handler.removeMarker(Objects.requireNonNull(markerId, "markerId"));
    }

    public static void requestSupport(SupportDraft support) {
        handler.requestSupport(Objects.requireNonNull(support, "support"));
    }

    public interface Handler {
        default void requestSnapshot() {
        }

        default void createSquad(SquadCallsign callsign) {
        }

        default void joinSquad(SquadCallsign callsign) {
        }

        default void leaveSquad() {
        }

        default void disbandSquad() {
        }

        default void transferLeadership(UUID playerId) {
        }

        default void kickMember(UUID playerId) {
        }

        default void claimCommander() {
        }

        default void resignCommander() {
        }

        default void transferCommander(UUID playerId) {
        }

        default void selectClass(String classId) {
        }

        default void openLoadout() {
        }

        default void selectDeploymentPoint(UUID pointId) {
        }

        default void deploy() {
        }

        default void redeploy() {
        }

        default void resupply() {
        }

        default void createMarker(MarkerDraft marker) {
        }

        default void removeMarker(UUID markerId) {
        }

        default void requestSupport(SupportDraft support) {
        }
    }

    public enum MarkerTool {
        INFANTRY(false),
        TANK(false),
        IFV(false),
        DEFEND(false),
        RALLY(false),
        ATTACK_DIRECTION(true);

        private final boolean directional;

        MarkerTool(boolean directional) {
            this.directional = directional;
        }

        public boolean directional() {
            return directional;
        }
    }

    public record MarkerDraft(MarkerTool type, ResourceLocation dimension,
                              double startX, double startZ,
                              double endX, double endZ) {
        public MarkerDraft {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(dimension, "dimension");
            if (!Double.isFinite(startX) || !Double.isFinite(startZ)
                    || !Double.isFinite(endX) || !Double.isFinite(endZ)) {
                throw new IllegalArgumentException("Marker coordinates must be finite");
            }
        }

        public static MarkerDraft point(MarkerTool type, ResourceLocation dimension,
                                        double x, double z) {
            return new MarkerDraft(type, dimension, x, z, x, z);
        }
    }

    public record SupportDraft(ResourceLocation supportId, ResourceLocation dimension,
                               double startX, double startZ,
                               double endX, double endZ) {
        public SupportDraft {
            Objects.requireNonNull(supportId, "supportId");
            Objects.requireNonNull(dimension, "dimension");
            if (!Double.isFinite(startX) || !Double.isFinite(startZ)
                    || !Double.isFinite(endX) || !Double.isFinite(endZ)) {
                throw new IllegalArgumentException("Support coordinates must be finite");
            }
        }

        public static SupportDraft point(ResourceLocation supportId, ResourceLocation dimension,
                                         double x, double z) {
            return new SupportDraft(supportId, dimension, x, z, x, z);
        }
    }
}
