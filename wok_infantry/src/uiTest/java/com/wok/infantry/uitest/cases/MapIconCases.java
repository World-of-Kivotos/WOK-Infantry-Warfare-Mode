package com.wok.infantry.uitest.cases;

import com.wok.infantry.client.map.TacticalMapIcons;
import com.wok.infantry.client.screen.TacticalBoardChrome;
import com.wok.infantry.client.screen.TacticalLivery;
import com.wok.infantry.client.ui.probe.UiLayoutFrame;
import com.wok.infantry.uitest.UiCapture;
import com.wok.infantry.uitest.UiCase;
import com.wok.infantry.uitest.UiCaseContext;
import com.wok.infantry.uitest.UiTier;
import com.wok.infantry.uitest.gallery.UiMapIconGalleryScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Map marker sheet (preview surface {@code 36-map-icons}) on dark terrain and on the paper map
 * ({@link UiMapIconGalleryScreen}). Migrated: layout violations fail required tiers. The probe's
 * icon records must show every marker at its fixed physical size — 15×15 art pixels (pins 15×18)
 * × the whole physical pixels per art pixel of the size knob — on every GUI scale, the same size
 * the tactical map draws ({@link TacticalMapIcons#mapArtPx}, size scheme B).
 *
 * <p>0.5.0-beta.3: the sheet sits on the D2 device in the Academy livery (pinned; the map's own
 * colours do not follow the livery in this round), its two backgrounds are the bezel's page keys
 * and the R key is disabled with its reason.
 */
public final class MapIconCases {
    private MapIconCases() {
    }

    public static List<UiCase> cases() {
        return List.of(sheet(false), sheet(true));
    }

    private static UiCase sheet(boolean paper) {
        return UiCase.builder(UiMapIconGalleryScreen.SURFACE_ID, paper ? "paper" : "dark")
                .group("kit")
                .tiers(UiTier.ALL)
                .migrated(true)
                .pinLivery(TacticalLivery.Livery.ACADEMY)
                .open(context -> new UiMapIconGalleryScreen(paper))
                .check(MapIconCases::checkSizes)
                .check(MapIconCases::checkDisabledRefresh)
                .build();
    }

    /** The sheet's R key is the disabled hardware key: hatched, with its reason. */
    private static void checkDisabledRefresh(UiCaseContext context, UiCapture.Result capture) {
        UiLayoutFrame.Control refresh = capture.frame().control(
                TacticalBoardChrome.REFRESH_KEY_UI_ID);
        context.require(refresh != null && "DISABLED".equals(refresh.state())
                        && !refresh.active() && !refresh.disabledReason().isBlank(),
                "the sheet's R key must be disabled with its reason: " + refresh);
    }

    private static void checkSizes(UiCaseContext context, UiCapture.Result capture) {
        if (!(context.screen() instanceof UiMapIconGalleryScreen screen)) {
            context.fail("the marker sheet is not open: " + context.screen());
            return;
        }
        context.require(context.uiCase().stateId().equals(screen.uiStateId()),
                "the sheet shows " + screen.uiStateId());
        List<Integer> expectedArt = new ArrayList<>();
        int base = TacticalMapIcons.mapArtPx(1.0D, capture.guiScale());
        for (int index = 0; index < TacticalMapIcons.MapIcon.values().length + 4; index++) {
            expectedArt.add(base);
        }
        if (screen.showsSizes()) {
            for (double knob : UiMapIconGalleryScreen.KNOBS) {
                expectedArt.add(TacticalMapIcons.mapArtPx(knob, capture.guiScale()));
            }
        }
        List<UiLayoutFrame.Icon> icons = capture.frame().icons();
        context.require(icons.size() == expectedArt.size(),
                "markers drawn: " + icons.size() + " of " + expectedArt.size());
        for (int index = 0; index < icons.size(); index++) {
            UiLayoutFrame.Icon icon = icons.get(index);
            TacticalMapIcons.MapIcon marker = TacticalMapIcons.MapIcon.valueOf(
                    icon.id().toUpperCase(Locale.ROOT));
            int art = expectedArt.get(index);
            context.require(icon.physicalWidth() == marker.plate().width() * art
                            && icon.physicalHeight() == marker.plate().height() * art,
                    "marker " + index + " (" + icon.id() + ") is " + icon.physicalWidth() + "x"
                            + icon.physicalHeight() + " physical pixels, expected "
                            + marker.plate().width() * art + "x" + marker.plate().height() * art);
            context.require(icon.rect().within(capture.frame().screen(), 0.01F),
                    "marker " + icon.id() + " leaves the screen");
        }
        context.observe("mapIconSizes[" + context.uiCase().stateId() + "@" + context.tier().id()
                + "]=" + icons.size() + " markers, " + 15 * base + "px plate at 1.0x");
    }
}
