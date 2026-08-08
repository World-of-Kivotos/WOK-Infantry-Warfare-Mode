package com.wok.bodyhealth.health;

import com.wok.bodyhealth.config.BodyHealthConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;

public final class BodyHealthData {
    public static final String ROOT_KEY = "wok_body_health";
    private static final String INITIALIZED_KEY = "initialized";
    private static final String LAST_PART_KEY = "last_part";
    private static final String MAXIMUM_SUFFIX = "_maximum";

    private final EnumMap<BodyPart, Float> health = new EnumMap<>(BodyPart.class);
    private BodyPart lastDamagedPart = BodyPart.CHEST;

    public static BodyHealthData load(Player player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT_KEY);
        BodyHealthData data = new BodyHealthData();
        boolean initialized = root.getBoolean(INITIALIZED_KEY);
        boolean migrated = false;

        for (BodyPart part : BodyPart.values()) {
            float maximum = BodyHealthConfig.maxHealth(part);
            float value = initialized && root.contains(part.key(), Tag.TAG_ANY_NUMERIC)
                    ? root.getFloat(part.key()) : maximum;
            if (initialized) {
                String maximumKey = part.key() + MAXIMUM_SUFFIX;
                float previousMaximum = root.contains(maximumKey, Tag.TAG_ANY_NUMERIC)
                        ? root.getFloat(maximumKey)
                        : BodyHealthConfig.defaultMaxHealth(part);
                if (previousMaximum > 0.0F
                        && Math.abs(previousMaximum - maximum) > 0.0001F) {
                    value = value / previousMaximum * maximum;
                    migrated = true;
                } else if (!root.contains(maximumKey, Tag.TAG_ANY_NUMERIC)) {
                    migrated = true;
                }
            }
            data.health.put(part, Math.max(0.0F, Math.min(value, maximum)));
        }

        if (root.contains(LAST_PART_KEY, Tag.TAG_STRING)) {
            try {
                data.lastDamagedPart = BodyPart.valueOf(root.getString(LAST_PART_KEY));
            } catch (IllegalArgumentException ignored) {
                data.lastDamagedPart = BodyPart.CHEST;
            }
        }

        if (!initialized || migrated) {
            data.save(player);
        }
        return data;
    }

    public static void copy(Player original, Player replacement) {
        CompoundTag source = original.getPersistentData().getCompound(ROOT_KEY);
        if (!source.isEmpty()) {
            replacement.getPersistentData().put(ROOT_KEY, source.copy());
        }
    }

    public static void reset(Player player) {
        player.getPersistentData().remove(ROOT_KEY);
        load(player);
    }

    public void save(Player player) {
        CompoundTag root = new CompoundTag();
        root.putBoolean(INITIALIZED_KEY, true);
        for (BodyPart part : BodyPart.values()) {
            root.putFloat(part.key(), get(part));
            root.putFloat(part.key() + MAXIMUM_SUFFIX, BodyHealthConfig.maxHealth(part));
        }
        root.putString(LAST_PART_KEY, lastDamagedPart.name());
        player.getPersistentData().put(ROOT_KEY, root);
    }

    public float get(BodyPart part) {
        return health.getOrDefault(part, BodyHealthConfig.maxHealth(part));
    }

    public void set(BodyPart part, float value) {
        health.put(part, Math.max(0.0F, Math.min(value, BodyHealthConfig.maxHealth(part))));
    }

    public boolean isDestroyed(BodyPart part) {
        return get(part) <= 0.0001F;
    }

    public boolean needsHealing() {
        for (BodyPart part : BodyPart.values()) {
            if (get(part) < BodyHealthConfig.maxHealth(part) - 0.0001F) {
                return true;
            }
        }
        return false;
    }

    public BodyPart lastDamagedPart() {
        return lastDamagedPart;
    }

    public void setLastDamagedPart(BodyPart part) {
        lastDamagedPart = part;
    }
}
