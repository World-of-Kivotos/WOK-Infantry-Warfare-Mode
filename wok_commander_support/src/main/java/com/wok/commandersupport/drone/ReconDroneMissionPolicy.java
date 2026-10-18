package com.wok.commandersupport.drone;

import com.wok.infantry.battle.Faction;
import com.wok.infantry.support.adapter.SupportSpawnException;

import java.util.Objects;

/**
 * Pure step rules and mission outcomes of the drone sortie.
 *
 * <p>Step 0 launches the drone and scans at once; anything that fails before that first scan
 * reached the map returns the cooldown, following the satellite's failure policy. From step 1
 * on the call is spent: a downed drone or a lost signal only ends the mission, and unchecked
 * faults trip the circuit without a refund. Every fifth step scans (12 scans over the 60-step
 * sortie) and the last step sends the drone home.</p>
 */
final class ReconDroneMissionPolicy {
    static final int SCAN_EVERY_STEPS = 5;
    static final String SHOT_DOWN_MESSAGE = "侦察无人机已被击落";
    static final String SIGNAL_LOST_MESSAGE = "侦察无人机信号丢失";
    static final String EXECUTION_FAULT_MESSAGE = "侦察无人机执行异常";
    static final String AIRSPACE_UNLOADED_MESSAGE = "目标上空盘旋区域未加载，需有友军靠近目标";
    static final String LAUNCH_REJECTED_MESSAGE = "侦察无人机未能进入目标空域";
    static final String NO_COMMANDER_MESSAGE = "侦察无人机缺少在线指挥官";
    static final String NO_BATTLE_MESSAGE = "战局服务不可用，无法使用侦察无人机";
    static final String MISSING_CONTEXT_MESSAGE = "无人机侦察任务上下文缺失";

    /** What the provider finds when it checks on the drone at a mission step. */
    enum Status {
        ACTIVE,
        SHOT_DOWN,
        SIGNAL_LOST
    }

    private ReconDroneMissionPolicy() {
    }

    static boolean isScanStep(int stepIndex) {
        return stepIndex >= 0 && stepIndex % SCAN_EVERY_STEPS == 0;
    }

    static boolean isFinalStep(int stepIndex, int stepCount) {
        return stepIndex == stepCount - 1;
    }

    /**
     * A shoot-down wins over everything else: the wreck may already be gone by the next step.
     * Otherwise a drone that left the level or sits in a chunk that no longer ticks has lost
     * its link.
     */
    static Status status(boolean reportedShotDown, boolean present, boolean crashing,
                         boolean ticking) {
        if (reportedShotDown || present && crashing) {
            return Status.SHOT_DOWN;
        }
        if (!present || !ticking) {
            return Status.SIGNAL_LOST;
        }
        return Status.ACTIVE;
    }

    /** Ends the mission and keeps the cooldown: the drone already flew. */
    static SupportSpawnException outcome(Status status) {
        return switch (Objects.requireNonNull(status, "status")) {
            case SHOT_DOWN -> SupportSpawnException.endMission(SHOT_DOWN_MESSAGE);
            case SIGNAL_LOST -> SupportSpawnException.endMission(SIGNAL_LOST_MESSAGE);
            case ACTIVE -> throw new IllegalArgumentException("An active drone is no outcome");
        };
    }

    /** Reasons the drone never took off at step 0; all of them return the cooldown. */
    static SupportSpawnException airspaceUnloaded() {
        return SupportSpawnException.notDelivered(AIRSPACE_UNLOADED_MESSAGE);
    }

    static SupportSpawnException launchRejected() {
        return SupportSpawnException.notDelivered(LAUNCH_REJECTED_MESSAGE);
    }

    static SupportSpawnException noCommander() {
        return SupportSpawnException.endMission(NO_COMMANDER_MESSAGE);
    }

    static SupportSpawnException noBattle() {
        return SupportSpawnException.endMission(NO_BATTLE_MESSAGE);
    }

    /** Intel belongs to the accepted faction; an unassigned requester has left it as well. */
    static boolean requesterLeftMissionFaction(Faction missionFaction, Faction requesterFaction) {
        return missionFaction == null || requesterFaction != missionFaction;
    }

    /**
     * Maps a step failure onto the core outcome, like the satellite's failure policy: at step 0
     * nothing reached the map yet, so ordinary failures return the cooldown and a defective
     * integration trips the circuit with a refund. Later failures keep their own outcome and
     * unchecked faults trip the circuit without a refund.
     */
    static SupportSpawnException classify(int stepIndex, Throwable failure) {
        Objects.requireNonNull(failure, "failure");
        if (stepIndex < 0) {
            throw new IllegalArgumentException("Drone step cannot be negative: " + stepIndex);
        }
        boolean launchStep = stepIndex == 0;
        if (failure instanceof SupportSpawnException spawnFailure) {
            if (!launchStep || spawnFailure.refundCooldown()) {
                return spawnFailure;
            }
            if (spawnFailure.providerBroken()) {
                return SupportSpawnException.providerBroken(spawnFailure.getMessage(),
                        spawnFailure, true);
            }
            return SupportSpawnException.notDelivered(spawnFailure.getMessage(), spawnFailure);
        }
        return SupportSpawnException.providerBroken(EXECUTION_FAULT_MESSAGE, failure,
                launchStep);
    }
}
