package com.wok.infantry.network.battle;

import com.wok.infantry.battle.BattleRules;
import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportOptionView;
import com.wok.infantry.support.SupportView;

/**
 * Wire-level limits. These are deliberately at least as strict as the battle rules so a
 * malformed packet cannot allocate unbounded collections before the service validates it.
 */
public final class BattleNetworkLimits {
    public static final int MAX_ENUM_ID_LENGTH = 32;
    public static final int MAX_CLASS_ID_LENGTH = 64;
    public static final int MAX_CLASS_DISPLAY_NAME_LENGTH =
            BattleRules.MAX_CLASS_DISPLAY_NAME_LENGTH;
    public static final int MAX_PLAYER_NAME_LENGTH = 64;
    public static final int MAX_DIMENSION_ID_LENGTH = 128;
    public static final int MAX_SUPPORT_ID_LENGTH = SupportDefinition.MAX_ID_LENGTH;
    public static final int MAX_SUPPORT_TRANSLATION_KEY_LENGTH =
            SupportDefinition.MAX_TRANSLATION_KEY_LENGTH;
    public static final int MAX_SUPPORT_FALLBACK_NAME_LENGTH =
            SupportDefinition.MAX_FALLBACK_NAME_LENGTH;
    public static final int MAX_SUPPORT_SHORT_NAME_LENGTH =
            SupportDefinition.MAX_SHORT_NAME_LENGTH;
    public static final int MAX_ACTION_MESSAGE_LENGTH = 256;
    public static final int MAX_SUPPORT_REASON_LENGTH =
            SupportOptionView.MAX_AVAILABILITY_REASON_LENGTH;
    public static final int MAX_SUPPORT_SERVICE_MESSAGE_LENGTH =
            SupportView.MAX_SERVICE_MESSAGE_LENGTH;

    public static final int MAX_SQUADS = 5;
    public static final int MAX_MEMBERS_PER_SQUAD = BattleRules.SQUAD_CAPACITY;
    public static final int MAX_ALLIED_POSITIONS = BattleRules.FACTION_CAPACITY;
    public static final int MAX_MARKERS = BattleRules.MAX_MARKERS_PER_FACTION;
    public static final int MAX_CLASS_QUOTAS = 64;
    public static final int MAX_DEPLOYMENT_POINTS = 16;
    /** Fixed wire caps keep a dynamic provider catalog from allocating unbounded collections. */
    public static final int MAX_SUPPORT_OPTIONS = 32;
    public static final int MAX_ACTIVE_SUPPORT_MISSIONS = 8;
    public static final int MAX_SUPPORT_STEPS = 64;

    public static final int MAX_FACTION_CAPACITY = 128;
    public static final int MAX_SQUAD_CAPACITY = 32;
    public static final int MAX_CLASS_QUOTA_VALUE = 1_024;
    public static final int MAX_DEPLOYMENT_SUPPLY_RADIUS = 64;
    public static final double MAX_COORDINATE = BattleRules.MAX_COORDINATE;

    private BattleNetworkLimits() {
    }
}
