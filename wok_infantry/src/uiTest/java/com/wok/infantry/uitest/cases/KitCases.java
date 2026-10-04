package com.wok.infantry.uitest.cases;

import com.wok.infantry.client.map.TacticalMapIcons;
import com.wok.infantry.client.screen.TacticalConfirmDialog;
import com.wok.infantry.client.screen.TacticalList;
import com.wok.infantry.client.ui.probe.UiLayoutFrame;
import com.wok.infantry.uitest.UiCapture;
import com.wok.infantry.uitest.UiCase;
import com.wok.infantry.uitest.UiCaseContext;
import com.wok.infantry.uitest.UiInputDriver;
import com.wok.infantry.uitest.UiStep;
import com.wok.infantry.uitest.UiTier;
import com.wok.infantry.uitest.gallery.UiKitGalleryScreen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The component gallery ({@link UiKitGalleryScreen}, preview surface {@code kit}): the first
 * cases of every run, so a broken shared component shows up here before any screen uses it. All
 * gallery states are migrated surfaces: a layout violation on a required tier fails the run.
 */
public final class KitCases {
    /** The seven key states of the UI rules. */
    private static final Set<String> KEY_STATES = Set.of("NORMAL", "HOVER", "SELECTED",
            "DISABLED", "DANGER", "SUCCESS", "CONTROL");

    private KitCases() {
    }

    public static List<UiCase> cases() {
        return List.of(defaultState(), confirmState(),
                page(UiKitGalleryScreen.Page.INPUTS, KitCases::checkInputs),
                page(UiKitGalleryScreen.Page.CARDS, (context, capture) -> { }),
                page(UiKitGalleryScreen.Page.HUD, KitCases::checkHud),
                page(UiKitGalleryScreen.Page.ICONS, KitCases::checkIcons));
    }

    private static UiCase.Builder base(String state) {
        return UiCase.builder(UiKitGalleryScreen.SURFACE_ID, state)
                .group("kit")
                .tiers(UiTier.ALL)
                .migrated(true)
                .check(KitCases::checkSurface);
    }

    private static UiCase defaultState() {
        return base("default")
                .open(context -> new UiKitGalleryScreen(UiKitGalleryScreen.Page.CONTROLS))
                // As the preview, show the full-text tooltip of the shortened key where it fits.
                .steps(UiStep.when(context -> !context.tight(),
                        UiStep.hover(UiKitGalleryScreen.LONG_KEY_UI_ID)))
                .check(KitCases::checkControls)
                .build();
    }

    private static UiCase confirmState() {
        return base("confirm")
                .open(context -> new UiKitGalleryScreen(UiKitGalleryScreen.Page.CONTROLS))
                .steps(UiStep.click(UiKitGalleryScreen.DANGER_KEY_UI_ID),
                        UiStep.until("the danger confirmation", context ->
                                gallery(context).hasModal()))
                .check(KitCases::checkConfirm)
                .build();
    }

    private static UiCase page(UiKitGalleryScreen.Page page, UiCase.Check check) {
        return base(page.stateId())
                .open(context -> new UiKitGalleryScreen(page))
                .check(check)
                .build();
    }

    // ---- checks ---------------------------------------------------------------------------------

    private static UiKitGalleryScreen gallery(UiCaseContext context) {
        if (!(context.screen() instanceof UiKitGalleryScreen gallery)) {
            context.fail("Gallery is not open: " + context.screen());
            throw new IllegalStateException();
        }
        return gallery;
    }

    /** Every tier: surface and state ids, and the minimum 2x at GUI 1. */
    private static void checkSurface(UiCaseContext context, UiCapture.Result capture) {
        UiKitGalleryScreen gallery = gallery(context);
        context.require(UiKitGalleryScreen.SURFACE_ID.equals(gallery.uiSurfaceId()),
                "surface id " + gallery.uiSurfaceId());
        context.require(context.uiCase().stateId().equals(gallery.uiStateId()),
                "gallery shows state " + gallery.uiStateId() + " instead of "
                        + context.uiCase().stateId());
        if (context.tier() == UiTier.T960) {
            context.require(capture.baseScale() == 2 && capture.layoutWidth() == 480
                            && capture.layoutHeight() == 360,
                    "960x720 at GUI 1 must lay out as 480x360 at 2x, got "
                            + capture.layoutWidth() + "x" + capture.layoutHeight() + " x"
                            + capture.baseScale());
        }
        UiLayoutFrame.Control current = capture.frame().control(
                UiKitGalleryScreen.PAGES_UI_ID + "/" + gallery.page().tabId());
        context.require(current != null && "CURRENT".equals(current.state()),
                "the page tab of " + gallery.page() + " is not drawn as current");
    }

    private static void checkControls(UiCaseContext context, UiCapture.Result capture) {
        UiLayoutFrame frame = capture.frame();
        Set<String> states = new HashSet<>();
        boolean focus = false;
        for (UiLayoutFrame.Control control : frame.controls()) {
            states.add(control.state());
            focus |= control.focusRing();
        }
        Set<String> missing = new HashSet<>(KEY_STATES);
        missing.removeAll(states);
        context.require(missing.isEmpty(), "key states not drawn: " + missing);
        context.require(focus, "no key drawn with the keyboard focus ring");
        UiLayoutFrame.Control longKey = frame.control(UiKitGalleryScreen.LONG_KEY_UI_ID);
        context.require(longKey != null && longKey.truncated(),
                "the long key must be ellipsized on every tier");
        context.require(longKey.tooltip().replaceAll("\\s+", "")
                        .contains(longKey.label().replaceAll("\\s+", "")),
                "the ellipsized key must offer its full label as a tooltip");
        UiLayoutFrame.Control disabledTab = frame.control("kit.tabs/throwable");
        context.require(disabledTab != null && "DISABLED".equals(disabledTab.state())
                        && !disabledTab.disabledReason().isBlank(),
                "the disabled board tab must carry its reason");
        context.require(frame.control("kit.tabs/secondary") != null
                        && "CURRENT".equals(frame.control("kit.tabs/secondary").state()),
                "the board tab strip must draw its current tab");
        if (!context.tight()) {
            context.require(frame.box("tooltip") != null,
                    "hovering the long key must show its tooltip");
        }
        context.observe("kitKeyStates[" + context.tier().id() + "]=" + sorted(states));
    }

    private static void checkConfirm(UiCaseContext context, UiCapture.Result capture) {
        UiKitGalleryScreen gallery = gallery(context);
        TacticalConfirmDialog dialog = gallery.dialog();
        context.require(dialog != null && gallery.hasModal(), "the confirmation did not open");
        context.require(dialog.danger(), "the disband confirmation must be dangerous");
        context.require(!dialog.confirmFocused(),
                "a dangerous confirmation must start with the focus on cancel");
        context.require(capture.frame().box("modal.confirm") != null,
                "the confirmation card was not drawn");
        context.require(capture.frame().control(TacticalConfirmDialog.CONFIRM_UI_ID) != null,
                "the confirm key was not drawn");
        // Enter never confirms a dangerous action, not even with the focus on its key.
        UiInputDriver.key(context.minecraft(), GLFW.GLFW_KEY_TAB, 0);
        context.require(dialog.confirmFocused(), "Tab did not move the focus to confirm");
        UiInputDriver.key(context.minecraft(), GLFW.GLFW_KEY_ENTER, 0);
        context.require(gallery.hasModal() && !gallery.confirmed(),
                "Enter confirmed a dangerous action");
        UiInputDriver.key(context.minecraft(), GLFW.GLFW_KEY_ESCAPE, 0);
        context.require(!gallery.hasModal() && !gallery.confirmed(),
                "Esc did not cancel the confirmation");
        context.observe("kitDangerConfirmEnterIgnored[" + context.tier().id() + "]=true");
    }

    private static void checkInputs(UiCaseContext context, UiCapture.Result capture) {
        UiLayoutFrame frame = capture.frame();
        for (String id : List.of("kit.slider.rounds", "kit.slider.scale", "kit.slider.disabled",
                "kit.stepper.min", "kit.text.placeholder", "kit.text.typed", "kit.text.error")) {
            context.require(frame.control(id) != null, "missing control " + id);
        }
        UiLayoutFrame.Control error = frame.control("kit.text.error");
        context.require("ERROR".equals(error.state()) && !error.tooltip().isBlank(),
                "the error field must show its reason");
        TacticalList<?> list = null;
        for (GuiEventListener child : gallery(context).children()) {
            if (child instanceof TacticalList<?> candidate) {
                list = candidate;
            }
        }
        context.require(list != null && list.currentWindow().overflow(),
                "the candidate list must overflow to show the more line");
        context.require(frame.controls().stream().anyMatch(control ->
                        "list-row".equals(control.kind()) && !control.active()
                                && !control.disabledReason().isBlank()),
                "a disabled list row with its reason must be visible");
    }

    private static void checkHud(UiCaseContext context, UiCapture.Result capture) {
        long plates = capture.frame().boxes().stream()
                .filter(box -> "hud.plate".equals(box.id())).count();
        context.require(plates >= 3, "HUD plates drawn: " + plates);
    }

    private static void checkIcons(UiCaseContext context, UiCapture.Result capture) {
        List<UiLayoutFrame.Icon> icons = capture.frame().icons();
        int expected = TacticalMapIcons.MapIcon.values().length * 4 * 2;
        context.require(icons.size() == expected,
                "map icons drawn: " + icons.size() + " of " + expected);
        int artPx = TacticalMapIcons.physicalPerArt(1.0D);
        for (UiLayoutFrame.Icon icon : icons) {
            TacticalMapIcons.MapIcon marker = TacticalMapIcons.MapIcon.valueOf(
                    icon.id().toUpperCase(Locale.ROOT));
            context.require(icon.physicalWidth() == marker.plate().width() * artPx
                            && icon.physicalHeight() == marker.plate().height() * artPx,
                    "map icon " + icon.id() + " is " + icon.physicalWidth() + "x"
                            + icon.physicalHeight() + " physical pixels, expected "
                            + marker.plate().width() * artPx + "x"
                            + marker.plate().height() * artPx);
        }
        context.observe("kitMapIconPhysicalSize[" + context.tier().id() + "]=" + 15 * artPx);
    }

    private static String sorted(Set<String> states) {
        return String.join(",", states.stream().sorted().toList());
    }
}
