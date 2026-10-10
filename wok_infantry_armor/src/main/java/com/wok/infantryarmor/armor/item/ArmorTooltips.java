package com.wok.infantryarmor.armor.item;

import com.wok.infantryarmor.armor.settings.ArmorItemSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/** 插板与头盔提示框共用的数字格式和额外属性行。 */
public final class ArmorTooltips {

    private ArmorTooltips() {
    }

    /** 逐条列出实际施加的额外属性；被忽略的条目不显示，保证与服务端一致。 */
    public static void appendExtraAttributes(List<Component> tooltip, ArmorItemSettings.ItemAttributes attributes) {
        for (ArmorItemSettings.ArmorAttributeModifier modifier : attributes.modifiers()) {
            tooltip.add(Component.translatable(
                            "tooltip.wok_infantry_armor.extra_attribute." + modifier.operation().id(),
                            amountText(modifier),
                            Component.translatable(modifier.attribute().getDescriptionId()))
                    .withStyle(modifier.amount() >= 0.0D ? ChatFormatting.BLUE : ChatFormatting.RED));
        }
    }

    public static String amountText(ArmorItemSettings.ArmorAttributeModifier modifier) {
        if (modifier.operation().multiplicative()) {
            return signedPercent(modifier.amount());
        }
        String text = decimal(modifier.amount(), 3);
        return modifier.amount() > 0.0D ? "+" + text : text;
    }

    public static String percent(double value) {
        return decimal(value * 100.0D) + "%";
    }

    public static String signedPercent(double value) {
        double percentage = value * 100.0D;
        return (percentage >= 0.0D ? "+" : "") + decimal(percentage) + "%";
    }

    public static String decimal(double value) {
        return decimal(value, 2);
    }

    public static String decimal(double value, int digits) {
        String formatted = String.format(Locale.ROOT, "%." + digits + "f", value);
        int end = formatted.length();
        while (end > 0 && formatted.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && formatted.charAt(end - 1) == '.') {
            end--;
        }
        return formatted.substring(0, end);
    }
}
