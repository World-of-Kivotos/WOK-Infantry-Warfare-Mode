package com.wok.infantry.block;

import com.wok.infantry.battle.Faction;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

/** Visual binding state stored directly in the deployment beacon block state. */
public enum DeploymentBeaconMode implements StringRepresentable {
    UNBOUND("unbound", null),
    BLUE("blue", Faction.BLUE),
    RED("red", Faction.RED);

    private final String serializedName;
    private final Faction faction;

    DeploymentBeaconMode(String serializedName, Faction faction) {
        this.serializedName = serializedName;
        this.faction = faction;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public Optional<Faction> faction() {
        return Optional.ofNullable(faction);
    }

    public static DeploymentBeaconMode fromFaction(Faction faction) {
        return faction == Faction.BLUE ? BLUE : faction == Faction.RED ? RED : UNBOUND;
    }
}
