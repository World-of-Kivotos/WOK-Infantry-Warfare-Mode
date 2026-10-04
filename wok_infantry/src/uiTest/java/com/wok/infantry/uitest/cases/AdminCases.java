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
                        "large"));
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
