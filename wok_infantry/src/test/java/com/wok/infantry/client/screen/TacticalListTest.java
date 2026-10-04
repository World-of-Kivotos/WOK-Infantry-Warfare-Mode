package com.wok.infantry.client.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacticalListTest {
    private static final int ROW = 18;
    private static final int MORE = TacticalList.MORE_LINE_HEIGHT;

    private static int[] rows(int count) {
        int[] heights = new int[count];
        Arrays.fill(heights, ROW);
        return heights;
    }

    private static boolean[] noHeaders(int count) {
        return new boolean[count];
    }

    // ---- pure fitting ---------------------------------------------------------------------------

    @Test
    void listThatFitsShowsEveryRowAndNoMoreLine() {
        TacticalList.Window window = TacticalList.window(rows(5), noHeaders(5), 3, 90, MORE);

        assertFalse(window.overflow(), "5 × 18 = 90 fits exactly");
        assertEquals(0, window.first(), "a list that fits never scrolls");
        assertEquals(5, window.count());
        assertEquals(0, window.hiddenAbove());
        assertEquals(0, window.hiddenBelow());
    }

    @Test
    void overflowReservesTheMoreLineAndShowsOnlyWholeRows() {
        // 100px: 88px for rows after the 12px "还有 n 项" line -> 4 whole rows (72px), not 5.
        TacticalList.Window top = TacticalList.window(rows(10), noHeaders(10), 0, 100, MORE);

        assertTrue(top.overflow());
        assertEquals(0, top.first());
        assertEquals(4, top.count());
        assertEquals(0, top.hiddenAbove());
        assertEquals(6, top.hiddenBelow());
        assertTrue(4 * ROW + MORE <= 100, "rows and the more line stay inside the viewport");
        assertEquals(6, top.maxFirst(), "the last 4 rows can reach the bottom");
    }

    @Test
    void oneRowShortOfFittingStillReservesTheMoreLine() {
        TacticalList.Window window = TacticalList.window(rows(6), noHeaders(6), 0, 100, MORE);

        assertTrue(window.overflow(), "108px of rows in 100px");
        assertEquals(4, window.count());
        assertEquals(2, window.hiddenBelow());
    }

    @Test
    void scrollPositionIsClampedToBothEnds() {
        TacticalList.Window end = TacticalList.window(rows(10), noHeaders(10), 99, 100, MORE);
        assertEquals(6, end.first());
        assertEquals(4, end.count());
        assertEquals(6, end.hiddenAbove());
        assertEquals(0, end.hiddenBelow(), "at the end the line counts what is above");

        TacticalList.Window start = TacticalList.window(rows(10), noHeaders(10), -5, 100, MORE);
        assertEquals(0, start.first());

        TacticalList.Window middle = TacticalList.window(rows(10), noHeaders(10), 3, 100, MORE);
        assertEquals(3, middle.first());
        assertEquals(3, middle.hiddenAbove());
        assertEquals(3, middle.hiddenBelow());
        assertEquals(7, middle.end());
    }

    @Test
    void scrollbarOnlyStyleUsesTheWholeViewport() {
        TacticalList.Window window = TacticalList.window(rows(10), noHeaders(10), 0, 100, 0);

        assertEquals(5, window.count(), "no reserved line: 5 × 18 = 90 <= 100");
        assertEquals(5, window.maxFirst());
    }

    @Test
    void groupTitleNeverEndsTheViewWithoutItsFirstRow() {
        // [title 13][a][b][title 13][c][d][e], 70px viewport -> 58px for entries.
        int[] heights = {13, ROW, ROW, 13, ROW, ROW, ROW};
        boolean[] header = {true, false, false, true, false, false, false};

        TacticalList.Window atB = TacticalList.window(heights, header, 1, 70, MORE);
        assertEquals(1, atB.first());
        assertEquals(2, atB.count(), "a, b and the next title fit, but the title is dropped");
        assertEquals(0, atB.hiddenAbove(), "titles are not counted as items");
        assertEquals(3, atB.hiddenBelow());

        TacticalList.Window top = TacticalList.window(heights, header, 0, 70, MORE);
        assertEquals(3, top.count(), "title, a and b");
        assertEquals(3, top.hiddenBelow());
    }

    @Test
    void emptyListHasNothingToShow() {
        TacticalList.Window window = TacticalList.window(new int[0], new boolean[0], 4, 100, MORE);

        assertEquals(new TacticalList.Window(0, 0, 0, 0, false, 0), window);
    }

    @Test
    void viewportSmallerThanOneRowShowsNothingRatherThanHalfARow() {
        TacticalList.Window window = TacticalList.window(rows(3), noHeaders(3), 0, 20, MORE);

        assertTrue(window.overflow());
        assertEquals(0, window.count());
        assertEquals(3, window.hiddenBelow());
    }

    @Test
    void revealScrollsTheLeastAmount() {
        int[] heights = rows(10);
        boolean[] header = noHeaders(10);

        assertEquals(0, TacticalList.scrollToReveal(heights, header, 3, 0, 100, MORE),
                "already visible");
        assertEquals(1, TacticalList.scrollToReveal(heights, header, 4, 0, 100, MORE),
                "one below the view becomes the last visible row");
        assertEquals(6, TacticalList.scrollToReveal(heights, header, 9, 0, 100, MORE));
        assertEquals(2, TacticalList.scrollToReveal(heights, header, 2, 6, 100, MORE),
                "above the view becomes the first row");
    }

    @Test
    void revealingTheFirstRowOfAGroupBringsItsTitle() {
        int[] heights = {13, ROW, ROW, 13, ROW, ROW, ROW, ROW, ROW};
        boolean[] header = {true, false, false, true, false, false, false, false, false};

        assertEquals(6, TacticalList.window(heights, header, 99, 70, MORE).first());
        assertEquals(3, TacticalList.scrollToReveal(heights, header, 4, 6, 70, MORE),
                "scrolling up onto c also shows the title of its group");
        assertEquals(0, TacticalList.scrollToReveal(heights, header, 1, 4, 70, MORE));
    }

    @Test
    void moreLineTextCountsHiddenItems() {
        Component below = TacticalList.moreLineText(new TacticalList.Window(0, 4, 0, 6, true, 6));
        Component above = TacticalList.moreLineText(new TacticalList.Window(6, 4, 6, 0, true, 6));

        assertArrayEquals(new Object[]{6},
                translation(below, "screen.wok_infantry.list.more_below").getArgs());
        assertArrayEquals(new Object[]{6},
                translation(above, "screen.wok_infantry.list.more_above").getArgs());
    }

    // ---- widget ---------------------------------------------------------------------------------

    /** Well of 102px: 100px viewport inside the 1px well edges. */
    private static TacticalList<String> list(int count) {
        TacticalList<String> list = new TacticalList<String>(0, 0, 100, 102,
                Component.literal("test"), TacticalDraw.RowSpec::of).rowHeight(ROW);
        list.setItems(IntStream.range(0, count).mapToObj(index -> "item" + index).toList());
        return list;
    }

    @Test
    void widgetDrawsOnlyWholeRowsAboveTheMoreLine() {
        TacticalList<String> list = list(10);
        UiRect more = list.moreLineBounds();

        assertEquals(new UiRect(1, 1, 99, 101), list.viewport());
        assertEquals(4, list.currentWindow().count());
        assertEquals(new UiRect(1, 89, 96, 101), more,
                "last line of the well, left of the 2px scrollbar");
        for (int index = 0; index < 4; index++) {
            UiRect row = list.rowBounds(index);
            assertFalse(row.isEmpty());
            assertEquals(1 + index * ROW, row.top());
            assertEquals(ROW - 1, row.height(), "1px seam between rows");
            assertTrue(row.bottom() <= more.top(), "no row reaches into the more line");
        }
        assertEquals(UiRect.EMPTY, list.rowBounds(4), "a hidden row has no bounds");
    }

    @Test
    void widgetWithoutOverflowHasNoMoreLineAndFullWidthRows() {
        TacticalList<String> list = list(3);

        assertEquals(UiRect.EMPTY, list.moreLineBounds());
        assertEquals(99, list.rowBounds(0).right(), "no scrollbar column");
        assertFalse(list.mouseScrolled(10, 10, -1), "nothing to scroll: the wheel is not taken");
    }

    @Test
    void wheelScrollsOneRowAndStopsAtTheEnds() {
        TacticalList<String> list = list(10);

        assertTrue(list.mouseScrolled(10, 10, -1));
        assertEquals(1, list.firstVisible());
        assertTrue(list.mouseScrolled(10, 10, 1));
        assertEquals(0, list.firstVisible());
        assertTrue(list.mouseScrolled(10, 10, 1), "the wheel stays with the list at the top");
        assertEquals(0, list.firstVisible());
        assertFalse(list.scrollBy(-3));
        assertTrue(list.scrollBy(100));
        assertEquals(6, list.firstVisible(), "clamped to the last whole page");
        assertFalse(list.scrollBy(1));
    }

    @Test
    void keyboardMovesTheSelectionAndKeepsItVisible() {
        TacticalList<String> list = list(10);
        List<Integer> selected = new ArrayList<>();
        list.onSelect((index, item) -> selected.add(index));

        assertTrue(list.keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0));
        assertEquals(0, list.selectedIndex());
        for (int press = 0; press < 4; press++) {
            assertTrue(list.keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0));
        }
        assertEquals(4, list.selectedIndex());
        assertEquals(1, list.firstVisible(), "scrolled just enough to show row 4");
        assertTrue(list.keyPressed(GLFW.GLFW_KEY_END, 0, 0));
        assertEquals(9, list.selectedIndex());
        assertEquals(6, list.firstVisible());
        assertFalse(list.keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0),
                "↓ on the last row is left to focus navigation");
        assertTrue(list.keyPressed(GLFW.GLFW_KEY_HOME, 0, 0));
        assertEquals(0, list.firstVisible());
        assertFalse(list.keyPressed(GLFW.GLFW_KEY_UP, 0, 0),
                "↑ on the first row is left to focus navigation");
        assertTrue(list.keyPressed(GLFW.GLFW_KEY_PAGE_DOWN, 0, 0));
        assertEquals(3, list.selectedIndex(), "a page is one row less than the view");
        assertEquals(List.of(0, 1, 2, 3, 4, 9, 0, 3), selected);
    }

    @Test
    void enterActivatesTheSelectedRowEvenWhenDisabled() {
        TacticalList<String> list = new TacticalList<String>(0, 0, 100, 102,
                Component.literal("test"), item -> TacticalDraw.RowSpec.of(item)
                .withDisabledReason(Component.literal("编制锁定后开放")));
        list.setItems(List.of("a", "b"));
        List<String> activated = new ArrayList<>();
        list.onActivate((index, item) -> activated.add(item));

        assertFalse(list.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0), "nothing selected yet");
        list.setSelectedIndex(1);
        assertTrue(list.isDisabled(1));
        assertTrue(list.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0));
        assertEquals(List.of("b"), activated, "the screen decides what a disabled row does");
    }

    @Test
    void clickSelectsTheRowUnderThePointerAndTheMoreLinePagesOn() {
        TacticalList<String> list = list(10);
        List<Integer> selected = new ArrayList<>();
        list.onSelect((index, item) -> selected.add(index));

        assertEquals(2, list.itemAt(10, 1 + 2 * ROW + 5));
        assertEquals(-1, list.itemAt(10, 95), "the more line is no row");
        assertEquals(-1, list.itemAt(98, 10), "the scrollbar column is no row");
        assertTrue(list.mouseClicked(10, 1 + 2 * ROW + 5, 0));
        assertEquals(List.of(2), selected);
        assertTrue(list.mouseClicked(10, 95, 0), "click on ↓ 还有 n 项");
        assertEquals(3, list.firstVisible(), "pages by a view minus one row");
    }

    @Test
    void replacingItemsKeepsTheSelectionByKeyAndTheScrollPosition() {
        TacticalList<String> list = list(10).keyedBy(item -> item.substring(0, 5));
        list.setSelectedIndex(7);
        int first = list.firstVisible();

        list.setItems(IntStream.range(0, 10).mapToObj(index -> "item" + index + "*").toList());
        assertEquals(7, list.selectedIndex(), "same key, new record");
        assertEquals(first, list.firstVisible(), "a refresh does not jump to the top");

        list.setItems(List.of("other"));
        assertEquals(-1, list.selectedIndex());
        assertNull(list.selected());
        assertEquals(0, list.firstVisible());
    }

    @Test
    void disabledRowTooltipSaysWhyAndFullTitleWhenCut() {
        TacticalDraw.RowSpec spec = TacticalDraw.RowSpec.of("突击兵")
                .withDisabledReason(Component.literal("名额已满"));

        Component reason = TacticalList.rowTooltipText(spec, false, false);
        Object[] args = translation(reason, "screen.wok_infantry.list.unavailable").getArgs();
        assertEquals(1, args.length);
        assertEquals("名额已满", ((Component) args[0]).getString());
        assertTrue(TacticalList.rowTooltipText(spec, true, false).getString().startsWith("突击兵\n"),
                "a cut title is completed before the reason");
        assertNull(TacticalList.rowTooltipText(TacticalDraw.RowSpec.of("短"), false, false),
                "a row that fits needs no tooltip");
        assertEquals("长标题  12\n第二行", TacticalList.rowTooltipText(TacticalDraw.RowSpec.of("长标题")
                .withRight(Component.literal("12")).withSub(Component.literal("第二行")), true,
                false).getString());
    }

    @Test
    void groupedListCountsGroupSizesAndSkipsTitlesWhenHitTesting() {
        TacticalList<String> list = new TacticalList<String>(0, 0, 100, 202,
                Component.literal("test"), TacticalDraw.RowSpec::of).rowHeight(ROW)
                .headerHeight(13).groupBy(item -> Component.literal(item.substring(0, 1)));
        list.setItems(List.of("a1", "a2", "b1"));

        assertEquals(-1, list.itemAt(10, 5), "group title");
        assertEquals(0, list.itemAt(10, 1 + 13 + 2));
        assertEquals(2, list.itemAt(10, 1 + 13 + 2 * ROW + 13 + 2));
        assertEquals(1 + 13, list.rowBounds(0).top());
    }

    @Test
    void hitTestingAndClippingFollowTheLogicalCoordinatesAt2x() {
        // TacticalScreen divides GUI mouse coordinates by the factor before they reach the list.
        TacticalList<String> list = list(10);
        double guiY = (1 + 2 * ROW + 5) * 2.0D;
        assertEquals(2, list.itemAt(UiScale.toLayout(20.0D, 2), UiScale.toLayout(guiY, 2)));

        // The list clips through UiScale, which scales its logical viewport by the 2x pose.
        int[] scissor = UiScale.scissorBounds(new Matrix4f().scale(2.0F, 2.0F, 1.0F),
                list.viewport().left(), list.viewport().top(), list.viewport().right(),
                list.viewport().bottom());
        assertArrayEquals(new int[]{2, 2, 198, 202}, scissor);
    }

    private static TranslatableContents translation(Component component, String key) {
        assertTrue(component.getContents() instanceof TranslatableContents, key);
        TranslatableContents contents = (TranslatableContents) component.getContents();
        assertEquals(key, contents.getKey());
        return contents;
    }
}
