package com.wok.infantry.loadout;

public record LoadoutSnapshot(LoadoutConfigData config,
                              PlayerLoadoutData player,
                              boolean administrator) {
}
