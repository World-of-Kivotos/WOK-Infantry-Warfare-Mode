package com.wok.infantry.client.screen;

import com.wok.infantry.battle.TacticalMarkerType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacticalMapScreenLayoutTest {
    @Test
    void responsiveBoardRegionsStayInsideTheScreenAndDoNotOverlap() {
        for (int[] size : List.of(
                new int[] {320, 240},
                new int[] {960, 720},
                new int[] {2560, 1351})) {
            TacticalMapLayout.Layout layout = TacticalMapLayout.compute(size[0], size[1]);
            assertInsideScreen(layout.header(), size[0], size[1]);
            assertInsideScreen(layout.mapFrame(), size[0], size[1]);
            assertInsideScreen(layout.mapViewport(), size[0], size[1]);
            assertInsideScreen(layout.sidebar(), size[0], size[1]);
            assertInsideScreen(layout.footer(), size[0], size[1]);
            assertFalse(layout.mapViewport().intersects(layout.sidebar()),
                    size[0] + "x" + size[1] + " map must not overlap the command board");
            assertFalse(layout.mapViewport().intersects(layout.footer()),
                    size[0] + "x" + size[1] + " map must not overlap the status rail");
            assertTrue(layout.sidebar().left() > layout.mapViewport().right(),
                    size[0] + "x" + size[1] + " command board must stay right of the map");
            TacticalMapLayout.Rect support = TacticalMapLayout.supportRegion(layout);
            assertInsideScreen(support, size[0], size[1]);
            assertFalse(support.intersects(layout.mapViewport()),
                    size[0] + "x" + size[1] + " support tools must not cover the map");
            assertFalse(support.intersects(layout.footer()),
                    size[0] + "x" + size[1] + " support tools must not cover the footer");
            TacticalMapLayout.Rect previousSupportButton = null;
            for (int index = 0; index < 3; index++) {
                TacticalMapLayout.Rect button = TacticalMapLayout.supportButton(
                        layout, index, 3);
                assertContainedBy(button, support,
                        size[0] + "x" + size[1] + " support button " + index);
                if (previousSupportButton != null) {
                    assertFalse(previousSupportButton.intersects(button),
                            size[0] + "x" + size[1] + " support buttons must not overlap");
                }
                previousSupportButton = button;
            }
            if (layout.rich()) {
                TacticalMapLayout.Rect target = TacticalMapLayout.selectedTargetRegion(layout);
                assertInsideScreen(target, size[0], size[1]);
                assertFalse(target.intersects(support),
                        size[0] + "x" + size[1]
                                + " selected target must stay above support");
            } else {
                TacticalMapLayout.Rect switcher = TacticalMapLayout.sidebarModeSwitcher(layout);
                assertInsideScreen(switcher, size[0], size[1]);
                assertFalse(switcher.intersects(support),
                        size[0] + "x" + size[1]
                                + " marker/support switch must not cover support tools");
            }
            if (layout.compact()) {
                assertInsideScreen(layout.compactDrawer(), size[0], size[1]);
                assertFalse(layout.mapViewport().intersects(layout.compactDrawer()));
                assertFalse(layout.compactDrawer().intersects(layout.footer()));
            }
        }
    }

    @Test
    void compactAndWideLayoutsReserveUsefulMapAndCommandBoardSpace() {
        TacticalMapLayout.Layout compact = TacticalMapLayout.compute(320, 240);
        assertTrue(compact.compact());
        assertFalse(compact.rich());
        assertEquals(178, compact.mapViewport().width());
        assertEquals(142, compact.mapViewport().height());
        assertTrue(compact.sidebar().width() >= 120);
        assertEquals(compact.compactDrawer().top() + 2,
                compact.markerButtonsTop());

        TacticalMapLayout.Layout standard = TacticalMapLayout.compute(960, 720);
        assertFalse(standard.compact());
        assertTrue(standard.rich());
        assertTrue(standard.mapViewport().width() >= 640);
        assertTrue(standard.sidebar().width() >= 260);
        assertTrue(standard.sidebar().width() * 100 >= 27 * 960,
                "the command board must remain visually substantial at 960px");

        TacticalMapLayout.Layout ultraWide = TacticalMapLayout.compute(2560, 1351);
        assertTrue(ultraWide.sidebar().width() * 100 >= 27 * 2560,
                "the command board must not collapse into a thin debug strip on wide screens");
        int richMarkerBottom = ultraWide.markerButtonsTop() + 3 * 24 + 2 * 4;
        assertTrue(richMarkerBottom < ultraWide.detailTop());
        assertTrue(ultraWide.detailTop() + 44 < ultraWide.sidebar().bottom());
    }

    @Test
    void supportModuleOccupiesTheMarkedRichCardAndCompactDrawer() {
        TacticalMapLayout.Layout compact = TacticalMapLayout.compute(320, 240);
        TacticalMapLayout.Rect compactSupport = TacticalMapLayout.supportRegion(compact);
        assertTrue(compact.compactDrawer().contains(compactSupport.left(), compactSupport.top()));
        assertTrue(compactSupport.bottom() <= compact.compactDrawer().bottom());
        assertEquals(0, TacticalMapLayout.supportButton(compact, 0, 0).width(),
                "an empty dynamic catalog must not reserve a fake support button");
        TacticalMapLayout.Rect single = TacticalMapLayout.supportButton(compact, 0, 1);
        assertContainedBy(single, compactSupport, "single compact support button");
        assertEquals(0, TacticalMapLayout.supportPagerButton(compact, false).width(),
                "compact catalogs page by wheel instead of fake overlapping controls");
        assertEquals(0, TacticalMapLayout.supportSummary(compact, 3).width(),
                "compact empty/support rows must stay in the single-line drawer");
        assertTrue(TacticalMapLayout.supportButton(compact, 0, 3).left()
                < TacticalMapLayout.supportButton(compact, 1, 3).left());
        assertTrue(TacticalMapLayout.supportButton(compact, 1, 3).left()
                < TacticalMapLayout.supportButton(compact, 2, 3).left());

        for (int[] size : List.of(new int[] {960, 720}, new int[] {2560, 1351})) {
            TacticalMapLayout.Layout rich = TacticalMapLayout.compute(size[0], size[1]);
            TacticalMapLayout.Rect target = TacticalMapLayout.selectedTargetRegion(rich);
            TacticalMapLayout.Rect support = TacticalMapLayout.supportRegion(rich);
            assertEquals(rich.detailTop(), target.top());
            assertEquals(rich.detailTop() + 94, support.top());
            assertEquals(support.top() - 4, target.bottom());
            assertTrue(target.height() >= 88,
                    "target card must retain room for its remove button");
            assertContainedBy(TacticalMapLayout.supportButton(rich, 0, 1), support,
                    "single rich support button");
            assertTrue(TacticalMapLayout.supportButton(rich, 0, 3).top()
                    < TacticalMapLayout.supportButton(rich, 1, 3).top());
            assertTrue(TacticalMapLayout.supportButton(rich, 1, 3).top()
                    < TacticalMapLayout.supportButton(rich, 2, 3).top());
            TacticalMapLayout.Rect previous = TacticalMapLayout.supportPagerButton(rich, false);
            TacticalMapLayout.Rect next = TacticalMapLayout.supportPagerButton(rich, true);
            assertContainedBy(previous, support, "previous support page button");
            assertContainedBy(next, support, "next support page button");
            assertFalse(previous.intersects(next));
        }
    }

    @Test
    void supportModuleFallsBackBeforeTheRichCardWouldCollapse() {
        for (int[] size : List.of(
                new int[] {700, 500},
                new int[] {720, 580},
                new int[] {720, 599},
                new int[] {700, 619})) {
            TacticalMapLayout.Layout layout = TacticalMapLayout.compute(size[0], size[1]);
            assertFalse(layout.rich(), size[0] + "x" + size[1]
                    + " must retain the taller standard support sidebar");
            assertSupportRowsDoNotOverlap(layout, size[0] + "x" + size[1]);
        }

        TacticalMapLayout.Layout firstSafeRich = TacticalMapLayout.compute(700, 620);
        assertTrue(firstSafeRich.rich());
        assertSupportRowsDoNotOverlap(firstSafeRich, "700x620");
        assertFalse(TacticalMapLayout.supportRegion(firstSafeRich)
                .intersects(firstSafeRich.footer()));
    }

    @Test
    void supportDirectionPreviewUsesTheSameInclusiveRangeAsTheServerContract() {
        assertFalse(TacticalMapScreen.isValidSupportDirectionGeometry(0, 0, 15.999, 0));
        assertTrue(TacticalMapScreen.isValidSupportDirectionGeometry(0, 0, 16, 0));
        assertTrue(TacticalMapScreen.isValidSupportDirectionGeometry(0, 0, 512, 0));
        assertFalse(TacticalMapScreen.isValidSupportDirectionGeometry(0, 0, 512.001, 0));
        assertFalse(TacticalMapScreen.isValidSupportDirectionGeometry(
                Double.NaN, 0, 16, 0));
    }

    @Test
    void changingSupportCatalogPageDisarmsTheHiddenSelection() {
        ResourceLocation selected = ResourceLocation.fromNamespaceAndPath(
                "wok_infantry", "selected_support");
        TacticalMapScreen.SupportPagingState changed = TacticalMapScreen.supportPagingState(
                0, 1, 2, selected, true);
        assertTrue(changed.changed());
        assertEquals(1, changed.page());
        assertEquals(null, changed.selectedSupport());
        assertFalse(changed.supportStartSet());

        TacticalMapScreen.SupportPagingState unchanged = TacticalMapScreen.supportPagingState(
                1, 1, 2, selected, true);
        assertFalse(unchanged.changed());
        assertEquals(selected, unchanged.selectedSupport());
        assertTrue(unchanged.supportStartSet());

        TacticalMapScreen.SupportPagingState clamped = TacticalMapScreen.supportPagingState(
                3, 3, 1, selected, true);
        assertTrue(clamped.changed());
        assertEquals(0, clamped.page());
        assertEquals(null, clamped.selectedSupport());
        assertFalse(clamped.supportStartSet());
    }

    private static void assertSupportRowsDoNotOverlap(TacticalMapLayout.Layout layout,
                                                      String context) {
        TacticalMapLayout.Rect support = TacticalMapLayout.supportRegion(layout);
        TacticalMapLayout.Rect body = TacticalMapLayout.supportBody(layout);
        assertFalse(support.intersects(layout.footer()), context + " support/footer overlap");
        assertTrue(body.height() >= 54, context
                + " support body must fit the full three-line empty state");
        TacticalMapLayout.Rect summary = TacticalMapLayout.supportSummary(layout, 3);
        TacticalMapLayout.Rect previous = TacticalMapLayout.supportPagerButton(layout, false);
        TacticalMapLayout.Rect next = TacticalMapLayout.supportPagerButton(layout, true);
        assertContainedBy(summary, body, context + " support summary");
        assertContainedBy(previous, body, context + " previous pager");
        assertContainedBy(next, body, context + " next pager");
        assertFalse(summary.intersects(previous), context + " summary/previous overlap");
        assertFalse(summary.intersects(next), context + " summary/next overlap");
        assertFalse(previous.intersects(next), context + " pager overlap");

        TacticalMapLayout.Rect priorButton = null;
        for (int index = 0; index < 3; index++) {
            TacticalMapLayout.Rect button = TacticalMapLayout.supportButton(layout, index, 3);
            assertContainedBy(button, body, context + " support button " + index);
            assertFalse(button.intersects(summary), context + " button/summary overlap");
            assertFalse(button.intersects(previous), context + " button/previous overlap");
            assertFalse(button.intersects(next), context + " button/next overlap");
            if (priorButton != null) {
                assertFalse(priorButton.intersects(button), context + " button overlap");
            }
            priorButton = button;
        }
    }

    @Test
    void iconScaleSliderUsesTheFreeRightSideOfTheTopBar() {
        TacticalMapLayout.Layout compact = TacticalMapLayout.compute(320, 240);
        TacticalMapLayout.Rect compactSlider = TacticalMapLayout.iconScaleSlider(
                compact, 270);
        assertEquals(42, compactSlider.width());
        assertEquals(20, compactSlider.height());
        assertTrue(compactSlider.left() >= 270);
        assertInsideScreen(compactSlider, 320, 240);

        TacticalMapLayout.Layout wide = TacticalMapLayout.compute(960, 720);
        TacticalMapLayout.Rect wideSlider = TacticalMapLayout.iconScaleSlider(
                wide, 394);
        assertEquals(190, wideSlider.width());
        assertEquals(22, wideSlider.height());
        assertTrue(wideSlider.left() > 394);
        assertInsideScreen(wideSlider, 960, 720);
    }

    @Test
    void layoutRectUsesTheSameHalfOpenHitboxSemanticsAsTheMap() {
        TacticalMapLayout.Rect rect = new TacticalMapLayout.Rect(10, 20, 30, 40);
        assertTrue(rect.contains(10.0D, 20.0D));
        assertTrue(rect.contains(29.999D, 39.999D));
        assertFalse(rect.contains(30.0D, 20.0D));
        assertFalse(rect.contains(10.0D, 40.0D));
    }

    @Test
    void tacticalMarkerIconsAreDistinctNativePixelPatterns() {
        Set<String> distinctPatterns = new HashSet<>();
        for (TacticalMarkerType type : List.of(
                TacticalMarkerType.INFANTRY,
                TacticalMarkerType.DEFEND,
                TacticalMarkerType.RALLY,
                TacticalMarkerType.ATTACK_DIRECTION)) {
            String[] pattern = TacticalMapScreen.markerIconPattern(type);
            assertEquals(TacticalMapScreen.MARKER_ICON_SIZE, pattern.length,
                    type + " icon height");
            for (String row : pattern) {
                assertEquals(TacticalMapScreen.MARKER_ICON_SIZE, row.length(),
                        type + " icon width");
                assertTrue(row.matches("[.WA]+"), type + " icon pixel palette");
            }
            assertTrue(distinctPatterns.add(String.join("/", pattern)),
                    type + " must have a unique silhouette");
        }
    }

    @Test
    void infantryHelmetIsLargerAndUsesPureRedLineWork() {
        assertEquals(new TacticalMapScreen.MarkerIconSize(16, 16),
                TacticalMapScreen.markerIconSize(TacticalMarkerType.INFANTRY, false));
        assertEquals(new TacticalMapScreen.MarkerIconSize(14, 14),
                TacticalMapScreen.markerIconSize(TacticalMarkerType.INFANTRY, true));
        int red = TacticalMapScreen.markerColor(TacticalMarkerType.INFANTRY);
        assertEquals(0xFFFF3030, red);
        assertEquals(red, TacticalMapScreen.markerForegroundColor(
                TacticalMarkerType.INFANTRY, red));
    }

    @Test
    void orderMarkersUseReadableMapAndToolbarSizes() {
        for (TacticalMarkerType type : List.of(
                TacticalMarkerType.DEFEND,
                TacticalMarkerType.RALLY)) {
            assertEquals(new TacticalMapScreen.MarkerIconSize(16, 16),
                    TacticalMapScreen.markerIconSize(type, false),
                    type + " map icon must match the enlarged infantry silhouette");
            assertEquals(new TacticalMapScreen.MarkerIconSize(14, 14),
                    TacticalMapScreen.markerIconSize(type, true),
                    type + " toolbar preview must remain legible");
            TacticalMapScreen.MarkerIconSize strategic =
                    TacticalMapScreen.markerPhysicalIconSizeForMap(type, 0.02D);
            assertTrue(strategic.width() >= 8,
                    type + " must remain visible at strategic zoom");
            assertTrue(strategic.height() >= 8,
                    type + " must remain visible at strategic zoom");
        }
        assertEquals(new TacticalMapScreen.MarkerIconSize(14, 14),
                TacticalMapScreen.markerIconSize(
                        TacticalMarkerType.ATTACK_DIRECTION, true),
                "attack-direction toolbar arrow must match the other order tools");
    }

    @Test
    void approvedVehicleTexturesKeepTheirDistinctTopDownProportions() throws IOException {
        assertMarkerTexture(TacticalMarkerType.TANK,
                TacticalMapScreen.TANK_MARKER_TEXTURE_WIDTH,
                TacticalMapScreen.TANK_MARKER_TEXTURE_HEIGHT);
        assertMarkerTexture(TacticalMarkerType.IFV,
                TacticalMapScreen.IFV_MARKER_TEXTURE_WIDTH,
                TacticalMapScreen.IFV_MARKER_TEXTURE_HEIGHT);

        TacticalMapScreen.MarkerIconSize tank = TacticalMapScreen.markerIconSize(
                TacticalMarkerType.TANK, false);
        TacticalMapScreen.MarkerIconSize ifv = TacticalMapScreen.markerIconSize(
                TacticalMarkerType.IFV, false);
        assertEquals(new TacticalMapScreen.MarkerIconSize(20, 30), tank);
        assertEquals(new TacticalMapScreen.MarkerIconSize(14, 31), ifv);
        assertEquals(new TacticalMapScreen.MarkerIconSize(10, 15),
                TacticalMapScreen.markerIconSize(TacticalMarkerType.TANK, true));
        assertEquals(0xFFFF3038,
                TacticalMapScreen.markerColor(TacticalMarkerType.TANK));
        assertTrue(tank.width() * ifv.height() > tank.height() * ifv.width(),
                "the Squad-style tank must be visibly squatter than the IFV");
        assertTrue(tank.height() > tank.width(),
                "the user-specified tank must retain its portrait silhouette");
        assertTrue(ifv.height() > ifv.width(),
                "the Bradley muzzle must point down on a portrait canvas");
        assertTrue(TacticalMapScreen.markerIconSize(TacticalMarkerType.TANK, true).height() <= 18,
                "the compact tank icon must fit its toolbar button without a frame");
    }

    @Test
    void vehicleMarkersUseMatchedVisibleMapFootprints() throws IOException {
        double[] tank = visibleMarkerFootprint(TacticalMarkerType.TANK);
        double[] ifv = visibleMarkerFootprint(TacticalMarkerType.IFV);

        assertEquals(tank[0], ifv[0], 0.75D,
                "tank and IFV visible widths must match after transparent margins");
        assertEquals(tank[1], ifv[1], 0.75D,
                "tank and IFV visible heights must match after transparent margins");
    }

    @Test
    void tankTextureDisablesBlurAtMapScale() throws IOException {
        String resourcePath = "/assets/wok_infantry/textures/gui/tactical_markers/"
                + "tank.png.mcmeta";
        try (InputStream stream = TacticalMapScreenLayoutTest.class
                .getResourceAsStream(resourcePath)) {
            assertNotNull(stream, "tank texture metadata");
            String metadata = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(metadata.matches("(?s).*\\\"blur\\\"\\s*:\\s*false.*"),
                    "the user-specified tank texture must use crisp nearest sampling");
        }
    }

    @Test
    void mapMarkerIconsKeepStablePhysicalSizeAndShrinkAtStrategicZoom() {
        for (TacticalMarkerType type : TacticalMarkerType.values()) {
            TacticalMapScreen.MarkerIconSize base =
                    TacticalMapScreen.markerIconSize(type, false);
            TacticalMapScreen.MarkerIconSize physicalDefault =
                    TacticalMapScreen.markerPhysicalIconSizeForMap(type, 0.45D);
            TacticalMapScreen.MarkerIconSize logicalScaleOne =
                    TacticalMapScreen.markerIconSizeForMap(type, 0.45D, 1.0D);
            TacticalMapScreen.MarkerIconSize logicalScaleThree =
                    TacticalMapScreen.markerIconSizeForMap(type, 0.45D, 3.0D);
            assertEquals(base, physicalDefault,
                    type + " default physical size must match its authored icon");
            assertEquals(physicalDefault, logicalScaleOne);
            assertTrue(Math.abs(physicalDefault.width()
                            - logicalScaleThree.width() * 3) <= 1,
                    type + " GUI scale must not inflate physical width");
            assertTrue(Math.abs(physicalDefault.height()
                            - logicalScaleThree.height() * 3) <= 1,
                    type + " GUI scale must not inflate physical height");

            TacticalMapScreen.MarkerIconSize strategic =
                    TacticalMapScreen.markerPhysicalIconSizeForMap(type, 0.02D);
            assertTrue(strategic.width() <= physicalDefault.width(),
                    type + " must not grow at strategic zoom");
            assertTrue(strategic.height() <= physicalDefault.height(),
                    type + " must not grow at strategic zoom");
            assertEquals(physicalDefault,
                    TacticalMapScreen.markerPhysicalIconSizeForMap(type, 4.0D),
                    type + " must not inflate beyond its base size when zooming in");
        }

        TacticalMapScreen.MarkerIconSize strategicTank =
                TacticalMapScreen.markerPhysicalIconSizeForMap(
                        TacticalMarkerType.TANK, 0.02D);
        assertTrue(strategicTank.width() >= 10);
        assertTrue(strategicTank.height() >= 15);
        assertEquals(strategicTank,
                TacticalMapScreen.markerPhysicalIconSizeForMap(
                        TacticalMarkerType.TANK, 0.02D, 0.75D),
                "the player scale knob must not shrink a strategic tank below its readable floor");
    }

    @Test
    void userScaleChangesIntelMarkersButLeavesOrdersAndToolbarStable() {
        for (TacticalMarkerType type : List.of(
                TacticalMarkerType.INFANTRY,
                TacticalMarkerType.TANK,
                TacticalMarkerType.IFV)) {
            TacticalMapScreen.MarkerIconSize small =
                    TacticalMapScreen.markerPhysicalIconSizeForMap(type, 0.45D, 0.75D);
            TacticalMapScreen.MarkerIconSize normal =
                    TacticalMapScreen.markerPhysicalIconSizeForMap(type, 0.45D, 1.0D);
            TacticalMapScreen.MarkerIconSize large =
                    TacticalMapScreen.markerPhysicalIconSizeForMap(type, 0.45D, 1.75D);
            assertTrue(small.width() < normal.width(), type + " must shrink");
            assertTrue(small.height() < normal.height(), type + " must shrink");
            assertTrue(large.width() > normal.width(), type + " must grow");
            assertTrue(large.height() > normal.height(), type + " must grow");

            TacticalMapScreen.MarkerIconSize logicalAtScaleThree =
                    TacticalMapScreen.markerIconSizeForMap(type, 0.45D, 3.0D, 1.75D);
            assertTrue(Math.abs(large.width()
                            - logicalAtScaleThree.width() * 3) <= 1,
                    type + " scaled physical width must survive GUI scaling");
            assertTrue(Math.abs(large.height()
                            - logicalAtScaleThree.height() * 3) <= 1,
                    type + " scaled physical height must survive GUI scaling");
        }

        for (TacticalMarkerType type : List.of(
                TacticalMarkerType.DEFEND,
                TacticalMarkerType.RALLY,
                TacticalMarkerType.ATTACK_DIRECTION)) {
            assertEquals(
                    TacticalMapScreen.markerPhysicalIconSizeForMap(type, 0.45D, 0.75D),
                    TacticalMapScreen.markerPhysicalIconSizeForMap(type, 0.45D, 1.75D),
                    type + " is an order marker and must not use the intel scale");
        }

        assertEquals(TacticalMapScreen.MIN_INTEL_MARKER_SCALE,
                TacticalMapScreen.clampIntelMarkerScale(-10.0D));
        assertEquals(TacticalMapScreen.MAX_INTEL_MARKER_SCALE,
                TacticalMapScreen.clampIntelMarkerScale(10.0D));
        assertEquals(1.0D, TacticalMapScreen.clampIntelMarkerScale(Double.NaN));
        assertEquals(new TacticalMapScreen.MarkerIconSize(14, 14),
                TacticalMapScreen.markerIconSize(TacticalMarkerType.INFANTRY, true),
                "toolbar preview must stay fixed while the map scale changes");
    }

    private static void assertMarkerTexture(TacticalMarkerType type,
                                            int expectedWidth,
                                            int expectedHeight) throws IOException {
        ResourceLocation location = TacticalMapScreen.markerIconTexture(type);
        assertNotNull(location, type + " texture location");
        String resourcePath = "/assets/" + location.getNamespace() + "/" + location.getPath();
        try (InputStream stream = TacticalMapScreenLayoutTest.class
                .getResourceAsStream(resourcePath)) {
            assertNotNull(stream, type + " texture resource");
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, type + " readable PNG");
            assertEquals(expectedWidth, image.getWidth(), type + " texture width");
            assertEquals(expectedHeight, image.getHeight(), type + " texture height");
            assertTrue(image.getColorModel().hasAlpha(), type + " transparent background");
            boolean hasTransparentPixel = false;
            boolean hasOpaquePixel = false;
            boolean hasRedLine = false;
            boolean hasNeutralFill = false;
            boolean hasDarkRedFill = false;
            boolean hasWhiteOrYellowLine = false;
            boolean hasAccentNearBottom = false;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    int alpha = argb >>> 24;
                    hasTransparentPixel |= alpha == 0;
                    hasOpaquePixel |= alpha == 255;
                    int red = argb >> 16 & 0xFF;
                    int green = argb >> 8 & 0xFF;
                    int blue = argb & 0xFF;
                    boolean redLine = alpha >= 64
                            && red >= 220 && green <= 80 && blue <= 80;
                    hasRedLine |= redLine;
                    hasNeutralFill |= alpha >= 64 && !redLine
                            && red >= 45 && red <= 140
                            && green >= 45 && green <= 140
                            && blue >= 35 && blue <= 130
                            && Math.max(red, Math.max(green, blue))
                            - Math.min(red, Math.min(green, blue)) <= 30;
                    hasDarkRedFill |= alpha >= 64 && !redLine
                            && red >= 55 && red <= 150
                            && green >= 10 && green <= 70
                            && blue >= 10 && blue <= 70;
                    hasWhiteOrYellowLine |= alpha >= 64
                            && (Math.min(red, Math.min(green, blue)) >= 150
                            || red >= 180 && green >= 130 && blue <= 100);
                    hasAccentNearBottom |= y >= image.getHeight() * 3 / 4 && redLine;
                }
            }
            assertTrue(hasTransparentPixel, type + " must not contain a baked background");
            assertTrue(hasOpaquePixel, type + " must retain fully legible line work");
            assertTrue(hasRedLine, type + " must use red tactical line work");
            assertTrue(hasNeutralFill || hasDarkRedFill,
                    type + " must retain a layered tactical vehicle body");
            assertFalse(hasWhiteOrYellowLine,
                    type + " must not retain white or yellow line work");
            assertTrue(hasAccentNearBottom,
                    type + " red cannon stroke must reach toward the bottom edge");
        }
    }

    private static double[] visibleMarkerFootprint(TacticalMarkerType type)
            throws IOException {
        ResourceLocation location = TacticalMapScreen.markerIconTexture(type);
        assertNotNull(location, type + " texture location");
        String resourcePath = "/assets/" + location.getNamespace() + "/"
                + location.getPath();
        try (InputStream stream = TacticalMapScreenLayoutTest.class
                .getResourceAsStream(resourcePath)) {
            assertNotNull(stream, type + " texture resource");
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, type + " readable PNG");
            int minX = image.getWidth();
            int minY = image.getHeight();
            int maxX = -1;
            int maxY = -1;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if ((image.getRGB(x, y) >>> 24) != 0) {
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
            assertTrue(maxX >= minX && maxY >= minY, type + " visible texture bounds");
            TacticalMapScreen.MarkerIconSize display =
                    TacticalMapScreen.markerIconSize(type, false);
            return new double[] {
                    display.width() * (maxX - minX + 1.0D) / image.getWidth(),
                    display.height() * (maxY - minY + 1.0D) / image.getHeight()
            };
        }
    }

    private static void assertInsideScreen(TacticalMapLayout.Rect rect,
                                           int width, int height) {
        assertTrue(rect.left() >= 0);
        assertTrue(rect.top() >= 0);
        assertTrue(rect.right() <= width);
        assertTrue(rect.bottom() <= height);
        assertTrue(rect.width() > 0);
        assertTrue(rect.height() > 0);
    }

    private static void assertContainedBy(TacticalMapLayout.Rect inner,
                                          TacticalMapLayout.Rect outer,
                                          String context) {
        assertTrue(inner.left() >= outer.left(), context + " left");
        assertTrue(inner.top() >= outer.top(), context + " top");
        assertTrue(inner.right() <= outer.right(), context + " right");
        assertTrue(inner.bottom() <= outer.bottom(), context + " bottom");
        assertTrue(inner.width() > 0, context + " width");
        assertTrue(inner.height() > 0, context + " height");
    }
}
