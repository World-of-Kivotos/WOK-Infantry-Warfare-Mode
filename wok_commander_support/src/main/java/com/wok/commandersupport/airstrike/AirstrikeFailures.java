package com.wok.commandersupport.airstrike;

import com.wok.infantry.support.adapter.SupportSpawnException;

/**
 * Shared mission outcomes for the CBC-based airstrikes.
 *
 * <p>Nothing released: the cooldown is returned and the provider stays usable. Ordnance lost
 * after release: only this mission ends. CBC reflection faults disable the provider until restart;
 * the cooldown is returned only when they happen before the shell entered the world.</p>
 */
final class AirstrikeFailures {
    static final String CBC_BROKEN_MESSAGE = "CBC 炮弹接口不可用";

    private AirstrikeFailures() {
    }

    /** The target fell outside the world border before release; nothing was dropped. */
    static SupportSpawnException borderMoved() {
        return SupportSpawnException.notDelivered("目标点已超出世界边界");
    }

    /** The target fell outside the world border after release; the shell must be removed first. */
    static SupportSpawnException borderMovedAfterRelease() {
        return SupportSpawnException.endMission("目标点已超出世界边界，弹体已撤回");
    }

    /** The level refused the freshly built shell (event cancelled or duplicate UUID). */
    static SupportSpawnException spawnRejected() {
        return SupportSpawnException.notDelivered("弹体未能进入世界");
    }

    /**
     * No candidate release point outside the target area is loaded, so nothing was dropped.
     * The text tells the commander what would make the next call work.
     */
    static SupportSpawnException releaseUnloaded() {
        return SupportSpawnException.notDelivered("释放航线区块未加载，需有友军靠近目标外围");
    }

    /** The released shell is gone before impact; the release already used the call, so no refund. */
    static SupportSpawnException shellLost(String name) {
        return SupportSpawnException.endMission(name + " 已在命中前消失");
    }

    /** CBC could not build or orient a shell; nothing entered the world. */
    static SupportSpawnException brokenBeforeRelease(Throwable cause) {
        return SupportSpawnException.providerBroken(CBC_BROKEN_MESSAGE, cause, true);
    }

    /** CBC rejected a call on a shell that was already in the world. */
    static SupportSpawnException brokenAfterRelease(Throwable cause) {
        return SupportSpawnException.providerBroken(CBC_BROKEN_MESSAGE, cause, false);
    }
}
