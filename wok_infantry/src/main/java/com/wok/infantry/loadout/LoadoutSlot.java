package com.wok.infantry.loadout;

import java.util.Arrays;
import java.util.Optional;

public enum LoadoutSlot {
    PRIMARY("primary", "主武器", 0),
    SECONDARY("secondary", "副武器", 1),
    MELEE("melee", "近战武器", 2),
    GADGET_ONE("gadget_one", "战术道具一", 3),
    GADGET_TWO("gadget_two", "战术道具二", 4),
    THROWABLE("throwable", "投掷物", 5);

    private final String id;
    private final String displayName;
    private final int hotbarIndex;

    LoadoutSlot(String id, String displayName, int hotbarIndex) {
        this.id = id;
        this.displayName = displayName;
        this.hotbarIndex = hotbarIndex;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int hotbarIndex() {
        return hotbarIndex;
    }

    public static Optional<LoadoutSlot> byId(String id) {
        return Arrays.stream(values()).filter(slot -> slot.id.equals(id)).findFirst();
    }
}
