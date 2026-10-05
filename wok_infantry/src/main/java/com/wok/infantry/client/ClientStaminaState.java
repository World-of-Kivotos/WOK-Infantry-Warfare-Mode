package com.wok.infantry.client;

import com.wok.infantry.network.stamina.StaminaClientPacketBridge;
import com.wok.infantry.stamina.StaminaSnapshot;
import net.minecraft.Util;

/**
 * Latest server-authoritative stamina values for local HUD and TaCZ sway, plus their recent
 * history for the stamina bar ({@link StaminaTrend}).
 *
 * <p>{@link #snapshot()} and {@code snapshot().enabled()} are read reflectively by
 * WOK步战附属-部位血量's HUD acceptance; keep their signatures and meaning.
 */
public final class ClientStaminaState {
    private static final StaminaSnapshot EMPTY = new StaminaSnapshot(100.0F, 100.0F, false);
    private static final StaminaTrend TREND = new StaminaTrend();
    private static volatile StaminaSnapshot snapshot = EMPTY;

    private ClientStaminaState() {
    }

    public static void install() {
        StaminaClientPacketBridge.install(ClientStaminaState::update);
    }

    public static StaminaSnapshot snapshot() {
        return snapshot;
    }

    public static void update(StaminaSnapshot replacement) {
        snapshot = replacement;
        TREND.record(replacement, Util.getMillis());
    }

    /** Remnants, recovery and unlock flags of the latest snapshots, as of now. */
    public static StaminaTrend.View trend() {
        return TREND.view(Util.getMillis());
    }

    public static void clear() {
        snapshot = EMPTY;
        TREND.clear();
    }
}
