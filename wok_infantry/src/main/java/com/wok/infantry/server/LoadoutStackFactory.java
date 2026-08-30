package com.wok.infantry.server;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wok.infantry.loadout.LoadoutEntry;
import com.wok.infantry.integration.tacz.TaczLoadoutAdapter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

final class LoadoutStackFactory {
    private static final String LARGE_STATION_DEPLOYER =
            "dragonrise_reforge:ammo_supply_station";
    private LoadoutStackFactory() {
    }

    static ValidationResult validate(LoadoutEntry entry) {
        if (entry == null || !validIdentifier(entry.id())) {
            return ValidationResult.error("装备 ID 只能包含小写字母、数字、_、-、.、/，长度 1–64");
        }
        if (entry.displayName() == null || entry.displayName().isBlank()
                || entry.displayName().length() > 80) {
            return ValidationResult.error("显示名称不能为空且不能超过 80 个字符");
        }
        ResourceLocation itemId = ResourceLocation.tryParse(entry.itemId());
        if (LARGE_STATION_DEPLOYER.equals(entry.itemId())) {
            return ValidationResult.error("大型弹药补给站只能由载具运输，不能加入步兵配装");
        }
        if (itemId == null || !ForgeRegistries.ITEMS.containsKey(itemId)) {
            return ValidationResult.error("物品不存在: " + entry.itemId());
        }
        if (entry.count() < 1 || entry.count() > 64) {
            return ValidationResult.error("数量必须在 1–64 之间");
        }
        if (!entry.hasValidAmmoReserveLimit()) {
            return ValidationResult.error("该枪械弹药携带/补给上限必须为 1–4096 发");
        }
        if (entry.snbt() != null && entry.snbt().length() > 32_767) {
            return ValidationResult.error("SNBT 太长");
        }
        if (entry.snbt() != null && !entry.snbt().isBlank()) {
            try {
                TagParser.parseTag(entry.snbt());
            } catch (CommandSyntaxException exception) {
                return ValidationResult.error("SNBT 格式错误: " + exception.getRawMessage().getString());
            }
        }
        return ValidationResult.ok();
    }

    static ItemStack create(LoadoutEntry entry) throws CommandSyntaxException {
        ResourceLocation itemId = ResourceLocation.tryParse(entry.itemId());
        Item item = itemId == null ? null : ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        CompoundTag configuredTag = null;
        if (entry.snbt() != null && !entry.snbt().isBlank()) {
            configuredTag = TagParser.parseTag(entry.snbt());
        }
        java.util.Optional<ItemStack> taczGun = TaczLoadoutAdapter.createGun(entry,
                configuredTag);
        if (taczGun.isPresent()) {
            return taczGun.get();
        }
        ItemStack stack = new ItemStack(item, entry.count());
        if (configuredTag != null) {
            stack.setTag(configuredTag);
        }
        return stack;
    }

    private static boolean validIdentifier(String value) {
        return value != null && value.matches("[a-z0-9_./-]{1,64}");
    }

    record ValidationResult(boolean valid, String message) {
        static ValidationResult ok() {
            return new ValidationResult(true, "");
        }

        static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }
}
