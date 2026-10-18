package com.wok.downed.state;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;

/**
 * Decides what a casualty may still fire while downed.
 *
 * <p>A downed player keeps a sidearm and nothing else. The item is only ever read, never moved
 * or granted: a casualty who is not holding a listed pistol simply cannot shoot. That keeps the
 * core module's deployment provenance rules untouched, because a swapped-in pistol would have to
 * be issued and reclaimed and would read as an unauthorised stack to the active-inventory check.
 *
 * <p>Classification is by exact item id from an allow list, so adding a new pistol is a config
 * edit rather than a code change. The item's NBT is deliberately ignored, so nothing stored
 * inside a gun's data can widen the allowance.
 */
public final class DownedWeaponPolicy {
    /**
     * Pistols registered by the gun packs this project ships with (TaCZ default pack plus the
     * Superb Warfare handgun). Used as the config default and as the unit-test fixture.
     */
    public static final List<String> DEFAULT_SIDEARMS = List.of(
            "tacz:glock_17",
            "tacz:b93r",
            "tacz:cz75",
            "tacz:deagle",
            "tacz:deagle_golden",
            "tacz:hk_mk23",
            "tacz:m1911",
            "tacz:m9a4",
            "tacz:p320",
            "tacz:rhino357",
            "tacz:taurus500",
            "tacz:taurus943",
            "tacz:timeless50",
            "tacz:uzi",
            "superbwarfare:pistol");

    private DownedWeaponPolicy() {
    }

    /** True when {@code stack} is a sidearm a downed player is allowed to fire. */
    public static boolean isAllowedSidearm(ItemStack stack, List<? extends String> allowedIds) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && isAllowedSidearmId(id.toString(), allowedIds);
    }

    /**
     * Checks a raw item id such as {@code tacz:glock_17} against the allow list. Comparison is
     * case-insensitive and tolerant of surrounding whitespace so a hand-edited config works.
     */
    public static boolean isAllowedSidearmId(String itemId, Iterable<? extends String> allowedIds) {
        if (itemId == null || allowedIds == null) {
            return false;
        }
        String normalised = itemId.trim().toLowerCase(Locale.ROOT);
        if (normalised.isEmpty()) {
            return false;
        }
        for (String allowed : allowedIds) {
            if (allowed != null && allowed.trim().toLowerCase(Locale.ROOT).equals(normalised)) {
                return true;
            }
        }
        return false;
    }
}
