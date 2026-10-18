package com.wok.infantry.block.entity;

import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.registry.InfantryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/** Persistent hit points and ownership mirror for a squad rally radio. */
public final class RallyRadioBlockEntity extends BlockEntity {
    private UUID deploymentPointId;
    private Faction faction;
    private String formationId = "";
    private SquadCallsign squad;
    private int maxHealth;
    private int health;

    public RallyRadioBlockEntity(BlockPos pos, BlockState state) {
        super(InfantryBlockEntities.RALLY_RADIO.get(), pos, state);
    }

    public void initialize(UUID pointId, Faction ownerFaction, String ownerFormation,
                           SquadCallsign ownerSquad, int configuredMaxHealth) {
        deploymentPointId = pointId;
        faction = ownerFaction;
        formationId = ownerFormation == null ? "" : ownerFormation;
        squad = ownerSquad;
        maxHealth = Math.max(1, configuredMaxHealth);
        health = maxHealth;
        setChanged();
    }

    public UUID deploymentPointId() { return deploymentPointId; }
    public Faction faction() { return faction; }
    public String formationId() { return formationId; }
    public SquadCallsign squad() { return squad; }
    public int maxHealth() { return maxHealth; }
    public int health() { return health; }
    public boolean initialized() {
        return deploymentPointId != null && faction != null && squad != null && maxHealth > 0;
    }

    /** Returns true exactly when this damage transition depletes the radio. */
    public boolean damage(int amount) {
        if (!initialized() || amount <= 0 || health <= 0) {
            return false;
        }
        health = Math.max(0, health - amount);
        setChanged();
        return health == 0;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        deploymentPointId = tag.hasUUID("PointId") ? tag.getUUID("PointId") : null;
        faction = Faction.byId(tag.getString("Faction")).orElse(null);
        formationId = tag.getString("Formation");
        squad = SquadCallsign.byId(tag.getString("Squad")).orElse(null);
        maxHealth = tag.contains("MaxHealth", Tag.TAG_ANY_NUMERIC)
                ? Math.max(0, tag.getInt("MaxHealth")) : 0;
        health = tag.contains("Health", Tag.TAG_ANY_NUMERIC)
                ? Math.max(0, Math.min(maxHealth, tag.getInt("Health"))) : maxHealth;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (deploymentPointId != null) tag.putUUID("PointId", deploymentPointId);
        if (faction != null) tag.putString("Faction", faction.id());
        if (!formationId.isBlank()) tag.putString("Formation", formationId);
        if (squad != null) tag.putString("Squad", squad.id());
        tag.putInt("MaxHealth", maxHealth);
        tag.putInt("Health", health);
    }
}
