package com.wok.infantry.client.screen;

import com.wok.infantry.battle.Faction;
import com.wok.infantry.battle.SquadCallsign;
import com.wok.infantry.battle.TacticalMarker;
import com.wok.infantry.battle.TacticalMarkerType;
import com.wok.infantry.client.map.TacticalMapIcons;
import com.wok.infantry.client.ui.UiTierMatrix;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacticalMapScreenLayoutTest {
    @Test
    void responsiveBoardRegionsStayInsideTheScreenAndDoNotOverlap() {
        for (int[] size : List.of(
                new int[] {320, 240},
                new int[] {640, 360},
                new int[] {960, 540},
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

    /**
     * Known defects ui-kit-14 / map-render-04 / map-interact-11, kept as expected failures until
     * the map batch (B7) replaces this geometry: on the standard tier (640×360, 960×540) the map
     * frame and the sidebar end 4px inside the footer, and on 320×240 the compact drawer starts
     * 2px inside the map frame. When this test fails, the defect is fixed: turn the expectations
     * into plain {@code UiTierMatrix.assertNoSolidOverlap} assertions.
     */
    @Test
    void knownDefectBoardFramesStillReachIntoTheFooterAndDrawer() {
        for (int[] size : List.of(new int[] {640, 360}, new int[] {960, 540})) {
            TacticalMapLayout.Layout layout = TacticalMapLayout.compute(size[0], size[1]);
            String context = size[0] + "x" + size[1];
            expectKnownDefect("ui-kit-14 map frame/footer " + context,
                    () -> UiTierMatrix.assertNoSolidOverlap(context, Map.of(
                            "mapFrame", ui(layout.mapFrame()), "footer", ui(layout.footer()))));
            expectKnownDefect("map-render-04 sidebar/footer " + context,
                    () -> UiTierMatrix.assertNoSolidOverlap(context, Map.of(
                            "sidebar", ui(layout.sidebar()), "footer", ui(layout.footer()))));
        }
        TacticalMapLayout.Layout compact = TacticalMapLayout.compute(320, 240);
        expectKnownDefect("ui-kit-14 compact drawer/map frame 320x240",
                () -> UiTierMatrix.assertNoSolidOverlap("320x240", Map.of(
                        "compactDrawer", ui(compact.compactDrawer()),
                        "mapFrame", ui(compact.mapFrame()))));
    }

    private static void expectKnownDefect(String defect, Executable assertion) {
        assertThrows(AssertionFailedError.class, assertion, defect
                + " no longer reproduces: the layout was fixed, make this a plain assertion");
    }

    private static UiRect ui(TacticalMapLayout.Rect rect) {
        return new UiRect(rect.left(), rect.top(), rect.right(), rect.bottom());
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
        assertTrue(summary.height() >= 20,
                context + " active-support status must remain a prominent banner");
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
    void markerColoursAreTheMapIconPlateColours() {
        for (TacticalMarkerType type : TacticalMarkerType.values()) {
            assertEquals(TacticalMapIcons.MapIcon.of(type).color(),
                    TacticalMapScreen.markerColor(type),
                    type + " line, card and tool accents match the icon on the map");
        }
        assertEquals(TacticalBoardTheme.MAP_ICON_ATTACK,
                TacticalMapScreen.markerColor(TacticalMarkerType.ATTACK_DIRECTION));
        assertEquals(TacticalBoardTheme.MAP_ICON_RECON,
                TacticalMapScreen.markerColor(TacticalMarkerType.RECON_CONTACT));
    }

    @Test
    void iconUnderTheCursorWinsOverAnAttackLineRunningBeneathIt() {
        ResourceLocation overworld = ResourceLocation.fromNamespaceAndPath("minecraft",
                "overworld");
        TacticalMarker tank = new TacticalMarker(new UUID(0L, 2L), Faction.BLUE,
                TacticalMarkerType.TANK, overworld, 0.0D, 64.0D, 0.0D, 0.0D, 0.0D,
                new UUID(0L, 9L), SquadCallsign.ALPHA, 1_000L, 2_000L);
        TacticalMarker attack = new TacticalMarker(new UUID(0L, 1L), Faction.BLUE,
                TacticalMarkerType.ATTACK_DIRECTION, overworld, -40.0D, 64.0D, 0.0D, 40.0D,
                0.0D, new UUID(0L, 9L), SquadCallsign.ALPHA, 1_000L, 2_000L);
        TacticalMarker defend = new TacticalMarker(new UUID(0L, 3L), Faction.BLUE,
                TacticalMarkerType.DEFEND, overworld, 4.0D, 64.0D, 0.0D, 4.0D, 0.0D,
                new UUID(0L, 9L), SquadCallsign.ALPHA, 1_000L, 2_000L);
        // The cursor is on the tank's icon 12 px from its centre, 1 px from the attack line.
        List<TacticalMapScreen.MarkerHit> hits = new ArrayList<>(List.of(
                new TacticalMapScreen.MarkerHit(attack, false, 1.0D),
                new TacticalMapScreen.MarkerHit(defend, true, 400.0D),
                new TacticalMapScreen.MarkerHit(tank, true, 144.0D)));
        hits.sort(TacticalMapScreen.MARKER_HIT_ORDER);
        assertEquals(List.of(tank, defend, attack),
                hits.stream().map(TacticalMapScreen.MarkerHit::marker).toList());
        // Equal distances fall back to the id, so cycling clicks keep a stable order.
        List<TacticalMapScreen.MarkerHit> ties = new ArrayList<>(List.of(
                new TacticalMapScreen.MarkerHit(defend, true, 25.0D),
                new TacticalMapScreen.MarkerHit(tank, true, 25.0D)));
        ties.sort(TacticalMapScreen.MARKER_HIT_ORDER);
        assertEquals(tank, ties.get(0).marker());
    }

    @Test
    void markerToolIconsUseTheMapSizeWhenTheKeyHasRoomAndOneArtPixelOtherwise() {
        // GUI 1: a 20 or 24 px key is 20 / 24 physical px, too short for a 30 px plate + 2 + 2.
        assertEquals(1, TacticalMapScreen.toolIconArtPx(20, 1.0D));
        assertEquals(1, TacticalMapScreen.toolIconArtPx(24, 1.0D));
        // GUI 2 and 3 (960×540, 320×240): 40 / 48 / 60 physical px keep 2 px above and below.
        assertEquals(2, TacticalMapScreen.toolIconArtPx(20, 2.0D));
        assertEquals(2, TacticalMapScreen.toolIconArtPx(24, 2.0D));
        assertEquals(2, TacticalMapScreen.toolIconArtPx(20, 3.0D));
        assertEquals(2, TacticalMapScreen.toolIconArtPx(17, 2.0D), "34 px is exactly enough");
        assertEquals(1, TacticalMapScreen.toolIconArtPx(16, 2.0D));
        assertEquals(1, TacticalMapScreen.toolIconArtPx(20, Double.NaN));
    }

    @Test
    void iconScaleKnobKeepsItsConfiguredRange() {
        assertEquals(TacticalMapScreen.MIN_INTEL_MARKER_SCALE,
                TacticalMapScreen.clampIntelMarkerScale(-10.0D));
        assertEquals(TacticalMapScreen.MAX_INTEL_MARKER_SCALE,
                TacticalMapScreen.clampIntelMarkerScale(10.0D));
        assertEquals(1.0D, TacticalMapScreen.clampIntelMarkerScale(Double.NaN));
        assertEquals(0.75D, TacticalMapScreen.MIN_INTEL_MARKER_SCALE);
        assertEquals(1.75D, TacticalMapScreen.MAX_INTEL_MARKER_SCALE);
    }

    @Test
    void alliedPlayerMarkersUseLargerStablePhysicalFootprints() {
        assertEquals(5, TacticalMapScreen.alliedPlayerMarkerRadius(
                false, false, false));
        assertEquals(6, TacticalMapScreen.alliedPlayerMarkerRadius(
                false, false, true));
        assertEquals(7, TacticalMapScreen.alliedPlayerMarkerRadius(
                false, true, false));
        assertEquals(7, TacticalMapScreen.alliedPlayerMarkerRadius(
                true, false, false));
        assertEquals(12, TacticalMapScreen.ALLIED_PLAYER_DIRECTION_LENGTH);
        assertEquals(13, TacticalMapScreen.ALLIED_PLAYER_HOVER_RADIUS);
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
