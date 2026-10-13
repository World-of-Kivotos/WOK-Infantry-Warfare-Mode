package com.wok.infantry.ammo.transport;

import net.minecraft.resources.ResourceLocation;

/** Physical supply item carried by one configured vehicle profile. */
public enum SupplyCargoType {
    LARGE("large",
            ResourceLocation.fromNamespaceAndPath(
                    "dragonrise_reforge", "ammo_supply_station"),
            "label.wok_infantry.supply_transport.large"),
    MEDIUM("medium",
            ResourceLocation.fromNamespaceAndPath(
                    "wok_infantry", "medium_ammo_supply_crate"),
            "label.wok_infantry.supply_transport.medium");

    private final String id;
    private final ResourceLocation itemId;
    private final String translationKey;

    SupplyCargoType(String id, ResourceLocation itemId, String translationKey) {
        this.id = id;
        this.itemId = itemId;
        this.translationKey = translationKey;
    }

    public String id() {
        return id;
    }

    public ResourceLocation itemId() {
        return itemId;
    }

    public String translationKey() {
        return translationKey;
    }

    public static SupplyCargoType parse(String value) {
        if (value != null) {
            for (SupplyCargoType type : values()) {
                if (type.id.equalsIgnoreCase(value.trim())) {
                    return type;
                }
            }
        }
        return null;
    }
}
