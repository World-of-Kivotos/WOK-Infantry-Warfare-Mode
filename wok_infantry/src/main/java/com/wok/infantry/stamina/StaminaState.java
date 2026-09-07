package com.wok.infantry.stamina;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Persistent per-player arm and leg reserves. Transient cooldowns remain server-runtime state. */
public record StaminaState(float arms, float legs) {
    private static final String ROOT_KEY = "wok_infantry_stamina";
    private static final String ARMS_KEY = "arms";
    private static final String LEGS_KEY = "legs";

    public StaminaState {
        arms = StaminaMath.clamp(arms, 0.0F, StaminaRules.MAX_STAMINA);
        legs = StaminaMath.clamp(legs, 0.0F, StaminaRules.MAX_STAMINA);
    }

    public static StaminaState full() {
        return new StaminaState(StaminaRules.MAX_STAMINA, StaminaRules.MAX_STAMINA);
    }

    public static StaminaState load(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_KEY, CompoundTag.TAG_COMPOUND)) {
            return full();
        }
        CompoundTag stamina = persistent.getCompound(ROOT_KEY);
        return new StaminaState(stamina.getFloat(ARMS_KEY), stamina.getFloat(LEGS_KEY));
    }

    public void save(Player player) {
        CompoundTag stamina = new CompoundTag();
        stamina.putFloat(ARMS_KEY, arms);
        stamina.putFloat(LEGS_KEY, legs);
        player.getPersistentData().put(ROOT_KEY, stamina);
    }

    public static void copy(Player original, Player replacement) {
        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(ROOT_KEY, CompoundTag.TAG_COMPOUND)) {
            replacement.getPersistentData().put(ROOT_KEY,
                    originalData.getCompound(ROOT_KEY).copy());
        }
    }
}
