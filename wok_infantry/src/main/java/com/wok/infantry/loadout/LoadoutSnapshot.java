package com.wok.infantry.loadout;

import com.wok.infantry.formation.FormationConfigData;

public record LoadoutSnapshot(LoadoutConfigData config,
                              PlayerLoadoutData player,
                              boolean administrator,
                              FormationConfigData formations) {
    public LoadoutSnapshot(LoadoutConfigData config, PlayerLoadoutData player,
                           boolean administrator) {
        this(config, player, administrator, new FormationConfigData());
    }

    public LoadoutSnapshot {
        formations = formations == null ? new FormationConfigData() : formations.copy();
    }

    @Override
    public FormationConfigData formations() {
        return formations.copy();
    }
}
