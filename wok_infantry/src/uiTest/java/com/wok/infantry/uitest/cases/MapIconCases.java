package com.wok.infantry.uitest.cases;

import com.wok.infantry.client.map.TacticalMapIcons;
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
 * × the whole physical pixels per art pixel of the size knob — on every GUI scale.
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
                .open(context -> new UiMapIconGalleryScreen(paper))
                .check(MapIconCases::checkSizes)
                .build();
    }

    private static void checkSizes(UiCaseContext context, UiCapture.Result capture) {
        if (!(context.screen() instanceof UiMapIconGalleryScreen screen)) {
            context.fail("the marker sheet is not open: " + context.screen());
            return;
        }
        context.require(context.uiCase().stateId().equals(screen.uiStateId()),
                "the sheet shows " + screen.uiStateId());
        List<Integer> expectedArt = new ArrayList<>();
        int base = TacticalMapIcons.physicalPerArt(1.0D);
        for (int index = 0; index < TacticalMapIcons.MapIcon.values().length + 4; index++) {
            expectedArt.add(base);
        }
        if (screen.showsSizes()) {
            for (double knob : UiMapIconGalleryScreen.KNOBS) {
                expectedArt.add(TacticalMapIcons.physicalPerArt(knob));
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
