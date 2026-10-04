package com.wok.infantry.uitest.cases;

import com.wok.infantry.client.screen.AdminLoadoutScreen;
import com.wok.infantry.uitest.UiCapture;
import com.wok.infantry.uitest.UiCase;
import com.wok.infantry.uitest.UiCaseContext;
import com.wok.infantry.uitest.UiFind;
import com.wok.infantry.uitest.UiInputDriver;
import com.wok.infantry.uitest.UiStep;
import com.wok.infantry.uitest.UiTier;
import com.wok.infantry.uitest.fixtures.LoadoutFixtures;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;

import java.util.List;

/**
 * Administrator loadout terminal (preview surface {@code 60-admin}).
 *
 * <p>Holds the three legacy captures of the B0 baseline, in their original order: the slot list at
 * 320×240 ({@code wok_ui_12}), the class settings opened from it ({@code wok_ui_14}) and the list at
 * 960×720 ({@code wok_ui_13}). Controls are looked up by uiId first ({@code admin.slot.add},
 * {@code admin.slot.edit}, {@code admin.class_settings}) and then by today's literal labels. The
 * administrator batch (B10) replaces them with its new states.
 */
public final class AdminCases {
    public static final String ADD_SLOT_UI_ID = "admin.slot.add";
    public static final String EDIT_SLOT_UI_ID = "admin.slot.edit";
    public static final String CLASS_SETTINGS_UI_ID = "admin.class_settings";

    private AdminCases() {
    }

    public static List<UiCase> cases() {
        return List.of(
                list("legacy_list", UiTier.T320, "wok_ui_12_admin_loadout_320x240.png", "compact"),
                classSettings(),
                list("legacy_list_large", UiTier.T960, "wok_ui_13_admin_loadout_960x720.png",
                        "large"),
                noClassRule());
    }

    /**
     * admin-01 regression (B1): on a formation without any class rule "职业管理" is disabled and
     * says why; clicking it, or activating it anyway, neither crashes nor leaves the list page.
     * The terminal is not migrated, so only the semantic checks fail the run.
     */
    private static UiCase noClassRule() {
        return UiCase.builder("admin", "noclass")
                .tiers(UiTier.T320, UiTier.T960, UiTier.T640)
                .open(context -> new AdminLoadoutScreen(LoadoutFixtures.adminWithoutClassRules()))
                .steps(UiStep.waitTicks(2), context -> {
                    AbstractWidget manage = UiFind.widget(context.screen(), CLASS_SETTINGS_UI_ID,
                            "职业管理").orElse(null);
                    context.require(manage != null,
                            "the administrator list page has no profession-management key");
                    context.require(!manage.active,
                            "profession management is enabled without a class rule");
                    context.require(manage.getTooltip() != null,
                            "the disabled profession-management key does not say why");
                    // A real click on the disabled key, then the press itself (keyboard or a
                    // stale state could still reach it): neither may open the settings page.
                    UiInputDriver.clickWidget(context.minecraft(), manage);
                    if (manage instanceof Button button) {
                        button.onPress();
                    }
                    context.caseState().put("manageLabel", manage.getMessage().getString());
                    return true;
                }, UiStep.waitTicks(4))
                .check((context, capture) -> {
                    context.require(context.screen() instanceof AdminLoadoutScreen,
                            "profession management without a rule left the terminal: "
                                    + context.screen());
                    boolean listPage = UiFind.widget(context.screen(), ADD_SLOT_UI_ID, "+槽位")
                            .isPresent();
                    context.require(listPage,
                            "profession management without a rule left the list page");
                    AbstractWidget manage = UiFind.widget(context.screen(), CLASS_SETTINGS_UI_ID,
                            "职业管理").orElse(null);
                    context.require(manage != null && !manage.active,
                            "profession management is no longer shown disabled");
                    context.observe("adminNoClassRuleDisabled[" + context.tier().id()
                            + "]=true");
                })
                .build();
    }

    private static UiCase list(String state, UiTier tier, String file, String size) {
        return UiCase.builder("admin", state)
                .group(UiCase.LEGACY_GROUP)
                .tiers(tier)
                .file(tier, file)
                .open(context -> new AdminLoadoutScreen(LoadoutFixtures.adminVisual()))
                .check((context, capture) -> {
                    boolean addSlot = UiFind.widget(context.screen(), ADD_SLOT_UI_ID, "+槽位")
                            .isPresent();
                    boolean editSlot = UiFind.widget(context.screen(), EDIT_SLOT_UI_ID, "设置")
                            .isPresent();
                    context.require(addSlot && editSlot,
                            "Administrator loadout omitted dynamic slot controls");
                    observeSize(context, capture, size + "AdminLoadoutLogicalSize");
                    context.observe(size + "AdminDynamicSlotControls=true");
                })
                .build();
    }

    private static UiCase classSettings() {
        return UiCase.builder("admin", "legacy_classes")
                .group(UiCase.LEGACY_GROUP)
                .tiers(UiTier.T320)
                .file(UiTier.T320, "wok_ui_14_admin_class_settings_320x240.png")
                .open(context -> new AdminLoadoutScreen(LoadoutFixtures.adminVisual()))
                .steps(UiStep.waitTicks(2), context -> {
                    AbstractWidget manage = UiFind.widget(context.screen(), CLASS_SETTINGS_UI_ID,
                            "职业管理").orElse(null);
                    context.require(manage != null,
                            "Compact administrator screen has no profession-management button");
                    UiInputDriver.clickWidget(context.minecraft(), manage);
                    return true;
                })
                .check((context, capture) -> {
                    context.require(context.screen() instanceof AdminLoadoutScreen,
                            "Profession management left the administrator screen");
                    context.observe("compactAdminProfessionSettings=true");
                })
                .build();
    }

    private static void observeSize(UiCaseContext context, UiCapture.Result capture, String key) {
        context.observe(key + "=" + capture.guiWidth() + "x" + capture.guiHeight());
    }
}
