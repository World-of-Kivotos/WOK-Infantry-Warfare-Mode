package com.wok.infantry.server;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wok.infantry.loadout.LoadoutEntry;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

final class LoadoutStackFactory {
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
        if (itemId == null || !ForgeRegistries.ITEMS.containsKey(itemId)) {
            return ValidationResult.error("物品不存在: " + entry.itemId());
        }
        if (entry.count() < 1 || entry.count() > 64) {
            return ValidationResult.error("数量必须在 1–64 之间");
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
        ItemStack stack = new ItemStack(item, entry.count());
        if (entry.snbt() != null && !entry.snbt().isBlank()) {
            stack.setTag(TagParser.parseTag(entry.snbt()));
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
