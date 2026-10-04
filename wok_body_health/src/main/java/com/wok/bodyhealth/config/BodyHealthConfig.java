package com.wok.bodyhealth.config;

import com.wok.bodyhealth.health.BodyPart;
import net.minecraftforge.common.ForgeConfigSpec;

public final class BodyHealthConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue HEAD_MAX;
    public static final ForgeConfigSpec.IntValue CHEST_MAX;
    public static final ForgeConfigSpec.IntValue ABDOMEN_MAX;
    public static final ForgeConfigSpec.IntValue LEFT_ARM_MAX;
    public static final ForgeConfigSpec.IntValue RIGHT_ARM_MAX;
    public static final ForgeConfigSpec.IntValue LEFT_LEG_MAX;
    public static final ForgeConfigSpec.IntValue RIGHT_LEG_MAX;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_SCALE;
    public static final ForgeConfigSpec.DoubleValue HEAL_SCALE;
    public static final ForgeConfigSpec.DoubleValue DESTROYED_PART_DAMAGE_TRANSFER_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ARMOR_BODY_PART_RESISTANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SEGMENTED_PRONE_HITBOX;
    public static final ForgeConfigSpec.DoubleValue PRONE_HEAD_MARGIN;
    public static final ForgeConfigSpec.DoubleValue PRONE_TORSO_MARGIN;
    public static final ForgeConfigSpec.DoubleValue PRONE_LIMB_MARGIN;
    public static final ForgeConfigSpec.IntValue PRONE_TRANSITION_LAG_TICKS;
    public static final ForgeConfigSpec.EnumValue<VanillaCrawlModel> PRONE_VANILLA_CRAWL_MODEL;
    public static final ForgeConfigSpec.BooleanValue PRONE_SBW_LEG_SEGMENTS_ARE_LEG_SHOTS;
    public static final ForgeConfigSpec.BooleanValue PRONE_DEBUG_LOGGING;
    public static final ForgeConfigSpec.BooleanValue REPLACE_VANILLA_HEARTS;

    /** Forward offset of the vanilla one-block-gap crawl model. */
    public enum VanillaCrawlModel {
        /** TACZ_TWEAKS when TaCZ Tweaks is installed, otherwise VANILLA. */
        AUTO,
        VANILLA,
        /** TaCZ Tweaks {@code crawl.visualTweak}: the model is drawn 0.4 blocks further back. */
        TACZ_TWEAKS
    }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("part_health");
        HEAD_MAX = partMaximum(builder, "headMax", 35, "头部最大血量。");
        CHEST_MAX = partMaximum(builder, "chestMax", 85, "胸部最大血量。");
        ABDOMEN_MAX = partMaximum(builder, "abdomenMax", 70, "腹部最大血量。");
        LEFT_ARM_MAX = partMaximum(builder, "leftArmMax", 60, "左臂最大血量。");
        RIGHT_ARM_MAX = partMaximum(builder, "rightArmMax", 60, "右臂最大血量。");
        LEFT_LEG_MAX = partMaximum(builder, "leftLegMax", 65, "左腿最大血量。");
        RIGHT_LEG_MAX = partMaximum(builder, "rightLegMax", 65, "右腿最大血量。");
        builder.pop();

        builder.push("conversion");
        DAMAGE_SCALE = builder
                .comment("每 1 点 Minecraft 最终伤害转换为多少部位伤害。")
                .defineInRange("bodyDamageScale", 8.0D, 0.01D, 1_000.0D);
        HEAL_SCALE = builder
                .comment("每 1 点 Minecraft 治疗量转换为多少部位治疗量。")
                .defineInRange("bodyHealScale", 10.0D, 0.01D, 1_000.0D);
        DESTROYED_PART_DAMAGE_TRANSFER_MULTIPLIER = builder
                .comment(
                        "已损毁的非致命部位再次受到伤害时，传递给其他未损毁部位的伤害倍率。",
                        "默认 0.8 表示传递本次部位伤害的 80%；0 表示不传递，1 表示完整传递。")
                .defineInRange("destroyedPartDamageTransferMultiplier", 0.8D, 0.0D, 10.0D);
        builder.pop();

        builder.push("armor_compatibility");
        ENABLE_ARMOR_BODY_PART_RESISTANCE = builder
                .comment(
                        "是否启用独立护甲的部位抗性模式。需要同时安装 WOK步战附属-独立护甲。",
                        "开启：板甲仅防护其信息中列出的部位；未覆盖命中不减伤，也不消耗板甲耐久。",
                        "关闭：板甲维持原来的全身抗性逻辑。等离子护盾始终为全身能量屏障。")
                .define("enableArmorBodyPartResistance", true);
        builder.pop();

        builder.push("prone_hitbox");
        ENABLE_SEGMENTED_PRONE_HITBOX = builder
                .comment(
                        "趴姿分段命中总开关。需要安装 TaCZ 或卓越前线（SBW）才会生效。",
                        "开启：趴下或爬行的玩家按头、躯干、四肢六段模型判定子弹命中，部位直接取自命中的段。",
                        "关闭：与 0.1.0-beta.9 相同，仍按原来的 0.6 格移动碰撞箱判定。")
                .define("enableSegmentedProneHitbox", true);
        PRONE_HEAD_MARGIN = builder
                .comment("头段各方向外扩的格数，用来覆盖帽子层和头盔。")
                .defineInRange("headMargin", 0.0625D, 0.0D, 0.25D);
        PRONE_TORSO_MARGIN = builder
                .comment("躯干段各方向外扩的格数，用来覆盖外套层和背心。")
                .defineInRange("torsoMargin", 0.03125D, 0.0D, 0.25D);
        PRONE_LIMB_MARGIN = builder
                .comment("四肢各段外扩的格数，用来覆盖袖子层和裤子层。")
                .defineInRange("limbMargin", 0.015625D, 0.0D, 0.25D);
        PRONE_TRANSITION_LAG_TICKS = builder
                .comment(
                        "趴下、起身、转身过渡动画的进度向后推迟的 tick 数。",
                        "用于让判定对齐客户端画面；默认 0，按游戏内叠加显示校准后再调整。")
                .defineInRange("transitionLagTicks", 0, 0, 5);
        PRONE_VANILLA_CRAWL_MODEL = builder
                .comment(
                        "没有 Tacz-Animation-Additions 趴姿状态时（例如一格缝原版爬行），爬行模型的前后位置。",
                        "AUTO：装了 TaCZ Tweaks 时按 TACZ_TWEAKS，否则按 VANILLA。",
                        "VANILLA：原版位置。TACZ_TWEAKS：TaCZ Tweaks 的 crawl.visualTweak 位置（整体后移 0.4 格）。")
                .defineEnum("vanillaCrawlModel", VanillaCrawlModel.AUTO);
        PRONE_SBW_LEG_SEGMENTS_ARE_LEG_SHOTS = builder
                .comment(
                        "卓越前线（SBW）子弹打中趴姿玩家的腿段时，是否按 SBW 的腿部命中处理。",
                        "开启：沿用 SBW 的腿部伤害倍率和短暂缓慢效果。关闭：腿段只记部位，不触发这些效果。")
                .define("sbwLegSegmentsAreLegShots", true);
        PRONE_DEBUG_LOGGING = builder
                .comment("逐次把趴姿分段判定写进日志，最多每秒 40 条。仅用于排查问题。")
                .define("debugLogging", false);
        builder.pop();

        builder.push("client");
        REPLACE_VANILLA_HEARTS = builder
                .comment("启用七部位 HUD 时是否隐藏原版红心。")
                .define("replaceVanillaHearts", true);
        builder.pop();

        SPEC = builder.build();
    }

    public static float maxHealth(BodyPart part) {
        return switch (part) {
            case HEAD -> HEAD_MAX.get().floatValue();
            case CHEST -> CHEST_MAX.get().floatValue();
            case ABDOMEN -> ABDOMEN_MAX.get().floatValue();
            case LEFT_ARM -> LEFT_ARM_MAX.get().floatValue();
            case RIGHT_ARM -> RIGHT_ARM_MAX.get().floatValue();
            case LEFT_LEG -> LEFT_LEG_MAX.get().floatValue();
            case RIGHT_LEG -> RIGHT_LEG_MAX.get().floatValue();
        };
    }

    public static float defaultMaxHealth(BodyPart part) {
        return switch (part) {
            case HEAD -> 35.0F;
            case CHEST -> 85.0F;
            case ABDOMEN -> 70.0F;
            case LEFT_ARM, RIGHT_ARM -> 60.0F;
            case LEFT_LEG, RIGHT_LEG -> 65.0F;
        };
    }

    public static boolean armorBodyPartResistanceEnabled() {
        return ENABLE_ARMOR_BODY_PART_RESISTANCE.get();
    }

    private static ForgeConfigSpec.IntValue partMaximum(
            ForgeConfigSpec.Builder builder, String key, int defaultValue, String comment) {
        return builder.comment(comment).defineInRange(key, defaultValue, 1, 10_000);
    }

    private BodyHealthConfig() {
    }
}
