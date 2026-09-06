package com.wok.infantry.ammo.transport;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parses server-configured entity-id, cargo-type and capacity mappings. */
public final class SupplyTransportRules {
    public static final int MAX_VEHICLE_CAPACITY = 64;

    private SupplyTransportRules() {
    }

    public static boolean isValidEntry(Object candidate) {
        return candidate instanceof String text && parseEntry(text) != null;
    }

    public static Map<ResourceLocation, SupplyTransportProfile> parse(
            List<? extends String> entries) {
        LinkedHashMap<ResourceLocation, SupplyTransportProfile> profiles =
                new LinkedHashMap<>();
        if (entries != null) {
            for (String entry : entries) {
                ParsedEntry parsed = parseEntry(entry);
                if (parsed != null) {
                    profiles.put(parsed.entityId(), parsed.profile());
                }
            }
        }
        return Collections.unmodifiableMap(profiles);
    }

    private static ParsedEntry parseEntry(String entry) {
        if (entry == null) {
            return null;
        }
        String[] parts = entry.split("=", -1);
        if (parts.length != 2 && parts.length != 3) {
            return null;
        }
        ResourceLocation entityId = ResourceLocation.tryParse(parts[0].trim());
        SupplyCargoType cargoType = parts.length == 2
                ? SupplyCargoType.LARGE : SupplyCargoType.parse(parts[1]);
        int capacityIndex = parts.length - 1;
        int capacity;
        try {
            capacity = Integer.parseInt(parts[capacityIndex].trim());
        } catch (NumberFormatException exception) {
            return null;
        }
        if (entityId == null || cargoType == null
                || capacity < 1 || capacity > MAX_VEHICLE_CAPACITY) {
            return null;
        }
        return new ParsedEntry(entityId,
                new SupplyTransportProfile(cargoType, capacity));
    }

    private record ParsedEntry(ResourceLocation entityId, SupplyTransportProfile profile) {
    }
}
