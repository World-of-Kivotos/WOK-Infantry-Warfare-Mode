package com.wok.infantry.client.screen;

/** Pure responsive geometry for the tactical board. */
final class TacticalMapLayout {
    private static final int SUPPORT_PANEL_TOP_OFFSET = 94;
    private static final int RICH_MIN_HEIGHT = 620;
    private static final int SUPPORT_VERTICAL_MIN_HEIGHT = 100;
    private static final int SUPPORT_BOTTOM_CONTENT_HEIGHT = 42;

    private TacticalMapLayout() {
    }

    static Layout compute(int screenWidth, int screenHeight) {
        int width = Math.max(240, screenWidth);
        int height = Math.max(200, screenHeight);
        boolean compact = width < 420 || height < 350;
        // The rich board reserves a fixed operation card above the support module. Below this
        // height the support body collapses, so retain the standard full-height sidebar instead.
        boolean rich = width >= 700 && height >= RICH_MIN_HEIGHT;
        int outerMargin = rich ? 10 : 4;
        int mapLeft = rich ? 16 : 8;
        int mapTop = rich ? 58 : 48;
        int sidebarRight = width - outerMargin;
        int sidebarWidth = rich
                ? clamp((int) Math.round(width * 0.28D), 220, 720)
                : clamp(width / 5, 124, 164);
        int sidebarLeft = Math.max(mapLeft + 96, sidebarRight - sidebarWidth);
        int mapRight = Math.max(mapLeft + 80, sidebarLeft - (rich ? 10 : 6));
        int mapBottom = height - (compact ? 50 : rich ? 30 : 25);
        mapBottom = Math.max(mapTop + 80, mapBottom);

        Rect header = new Rect(outerMargin, outerMargin,
                width - outerMargin, mapTop - 4);
        Rect mapViewport = new Rect(mapLeft, mapTop, mapRight, mapBottom);
        Rect mapFrame = new Rect(mapLeft - 5, mapTop - 5,
                mapRight + 5, mapBottom + 5);
        Rect sidebar = new Rect(sidebarLeft, mapTop - 5,
                sidebarRight, mapBottom + 5);
        int footerTop = height - (compact ? 20 : 24);
        Rect footer = new Rect(outerMargin, footerTop,
                width - outerMargin, height - outerMargin);
        Rect compactDrawer = compact
                ? new Rect(mapLeft, mapBottom + 3, mapRight,
                        Math.max(mapBottom + 4, footerTop - 2))
                : Rect.EMPTY;

        int layerTitleY = rich ? mapTop + 99 : mapTop + 3;
        int layerButtonsTop = rich ? mapTop + 116 : mapTop + 20;
        int markerTitleY = rich ? mapTop + 176 : mapTop + 119;
        int markerButtonsTop = compact
                ? compactDrawer.top() + 2
                : rich ? mapTop + 193 : mapTop + 136;
        int detailTop = rich ? mapTop + 280 : mapTop + 32;
        int topBarY = rich ? header.top() + 22 : 24;
        return new Layout(compact, rich, header, mapFrame, mapViewport, sidebar,
                compactDrawer, footer, topBarY, layerTitleY, layerButtonsTop,
                markerTitleY, markerButtonsTop, detailTop);
    }

    static Rect iconScaleSlider(Layout layout, int controlsRight) {
        int right = layout.header().right() - 4;
        int freeWidth = right - controlsRight;
        int gap = freeWidth >= 45 ? 3 : 0;
        int desiredWidth = layout.compact() ? 42 : layout.rich() ? 190 : 120;
        int width = Math.min(desiredWidth, freeWidth - gap);
        if (width < 36) {
            return Rect.EMPTY;
        }
        int height = layout.rich() ? 22 : 20;
        return new Rect(right - width, layout.topBarY(), right,
                layout.topBarY() + height);
    }

    /** Marker/support mode selector used when both tool sets cannot stay visible together. */
    static Rect sidebarModeSwitcher(Layout layout) {
        if (layout.rich()) {
            return Rect.EMPTY;
        }
        Rect sidebar = layout.sidebar();
        return new Rect(sidebar.left() + 6, layout.markerTitleY(),
                sidebar.right() - 6, layout.markerTitleY() + 20);
    }

    /** Space occupied by marker buttons after the responsive mode selector. */
    static Rect markerToolRegion(Layout layout) {
        if (layout.compact()) {
            Rect drawer = layout.compactDrawer();
            return new Rect(drawer.left() + 2, drawer.top() + 2,
                    drawer.right() - 2, drawer.bottom() - 2);
        }
        Rect sidebar = layout.sidebar();
        int top = layout.rich() ? layout.markerButtonsTop()
                : sidebarModeSwitcher(layout).bottom() + 3;
        int bottom = layout.rich() ? layout.detailTop() - 4 : sidebar.bottom() - 6;
        return new Rect(sidebar.left() + 6, top, sidebar.right() - 6, bottom);
    }

    /** Fixed support-module region: the marked lower card on rich layouts, otherwise tool space. */
    static Rect supportRegion(Layout layout) {
        if (layout.compact()) {
            Rect drawer = layout.compactDrawer();
            return new Rect(drawer.left() + 2, drawer.top() + 2,
                    drawer.right() - 2, drawer.bottom() - 2);
        }
        Rect sidebar = layout.sidebar();
        int top = layout.rich() ? layout.detailTop() + SUPPORT_PANEL_TOP_OFFSET
                : sidebarModeSwitcher(layout).bottom() + 3;
        return new Rect(sidebar.left() + 6, top,
                sidebar.right() - 6, sidebar.bottom() - 6);
    }

    /** Selected-target card above the fixed rich-layout support module. */
    static Rect selectedTargetRegion(Layout layout) {
        if (!layout.rich()) {
            return Rect.EMPTY;
        }
        Rect sidebar = layout.sidebar();
        Rect support = supportRegion(layout);
        return new Rect(sidebar.left() + 6, layout.detailTop(),
                sidebar.right() - 6, support.top() - 4);
    }

    /** Body below the support section header; compact drawers intentionally have no header. */
    static Rect supportBody(Layout layout) {
        Rect region = supportRegion(layout);
        if (layout.compact()) {
            return region;
        }
        return new Rect(region.left(), Math.min(region.bottom(), region.top() + 17),
                region.right(), region.bottom());
    }

    /** Exact button geometry shared by rendering, hit testing and layout regression tests. */
    static Rect supportButton(Layout layout, int index, int visibleCount) {
        if (visibleCount <= 0 || visibleCount > 3 || index < 0 || index >= visibleCount) {
            return Rect.EMPTY;
        }
        Rect body = supportBody(layout);
        if (body.width() <= 0 || body.height() <= 0) {
            return Rect.EMPTY;
        }
        boolean horizontal = layout.compact()
                || body.height() < SUPPORT_VERTICAL_MIN_HEIGHT;
        if (horizontal) {
            int gap = 2;
            int left = body.left() + 2;
            int right = body.right() - 2;
            int availableWidth = Math.max(visibleCount,
                    right - left - gap * (visibleCount - 1));
            int buttonWidth = Math.max(1, availableWidth / visibleCount);
            int buttonLeft = left + index * (buttonWidth + gap);
            int buttonRight = index == visibleCount - 1 ? right : buttonLeft + buttonWidth;
            return new Rect(buttonLeft, body.top() + 2, buttonRight,
                    Math.max(body.top() + 3, body.bottom() - 2));
        }

        int gap = 3;
        int top = body.top() + 4;
        int buttonAreaBottom = body.bottom() - SUPPORT_BOTTOM_CONTENT_HEIGHT;
        int availableHeight = Math.max(visibleCount,
                buttonAreaBottom - top - gap * (visibleCount - 1));
        int buttonHeight = Math.min(24, Math.max(1, availableHeight / visibleCount));
        int buttonTop = top + index * (buttonHeight + gap);
        return new Rect(body.left() + 4, buttonTop, body.right() - 4,
                buttonTop + buttonHeight);
    }

    /** Previous/next catalog page controls; compact layouts page with the mouse wheel. */
    static Rect supportPagerButton(Layout layout, boolean next) {
        Rect body = supportBody(layout);
        if (layout.compact() || body.width() < 70
                || body.height() < SUPPORT_VERTICAL_MIN_HEIGHT) {
            return Rect.EMPTY;
        }
        int width = 20;
        int top = body.bottom() - 21;
        int left = next ? body.right() - width - 4 : body.left() + 4;
        return new Rect(left, top, left + width, body.bottom() - 3);
    }

    /** Status row kept between the support buttons and the pager row. */
    static Rect supportSummary(Layout layout, int visibleCount) {
        Rect body = supportBody(layout);
        if (visibleCount <= 0 || visibleCount > 3 || layout.compact() || body.width() <= 8
                || body.height() < SUPPORT_VERTICAL_MIN_HEIGHT) {
            return Rect.EMPTY;
        }
        int pagerTop = body.bottom() - 21;
        Rect lastButton = supportButton(layout, visibleCount - 1, visibleCount);
        int top = lastButton.bottom() + 6;
        int bottom = Math.min(top + 13, pagerTop - 4);
        return bottom <= top ? Rect.EMPTY
                : new Rect(body.left() + 4, top, body.right() - 4, bottom);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    record Layout(boolean compact, boolean rich,
                  Rect header, Rect mapFrame, Rect mapViewport, Rect sidebar,
                  Rect compactDrawer, Rect footer,
                  int topBarY, int layerTitleY, int layerButtonsTop,
                  int markerTitleY, int markerButtonsTop, int detailTop) {
    }

    record Rect(int left, int top, int right, int bottom) {
        private static final Rect EMPTY = new Rect(0, 0, 0, 0);

        int width() {
            return Math.max(0, right - left);
        }

        int height() {
            return Math.max(0, bottom - top);
        }

        boolean contains(double x, double y) {
            return x >= left && x < right && y >= top && y < bottom;
        }

        boolean intersects(Rect other) {
            return left < other.right && right > other.left
                    && top < other.bottom && bottom > other.top;
        }
    }
}
