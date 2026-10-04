package com.wok.infantry.client.screen;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry of the formation vote page (preview {@code 45-formation.js renderNew}), in the
 * logical coordinates of a {@link TacticalScreen}.
 *
 * <ul>
 *   <li><b>Wide</b> ({@code w − 2·margin − 2·gap ≥ 440}, e.g. 480×270, 640×336, 960×720 at
 *   GUI 1 = 480×360): the faction strip on top (faction keys, then the step status and the join
 *   key at the right), below it the list panel (formation well, vote summary, administrator
 *   area) and the detail panel (identity, sections, action bar).</li>
 *   <li><b>Narrow</b> (320×240): two pages. The list page has the faction strip, the list panel
 *   with the well, a preview of the highlighted formation, the administrator area and the action
 *   bar "详情 | 加入/投票"; the detail page has a crumb row (list key, crumb, pager) above the
 *   detail panel.</li>
 *   <li><b>Waiting</b>: one panel with the waiting state and the retry key.</li>
 * </ul>
 * An administrator also gets the test-start key (0.4.0-beta.2): beside the vote key in the
 * administrator area, or, when there is no vote key (not joined yet, or locked), alone in a
 * compact administrator area at the same place. Panels never overlap; every rectangle stays
 * inside the board body.
 */
public record FormationScreenLayout(Mode mode, TacticalShellLayout shell, UiRect inner,
                                    UiRect strip, List<UiRect> factionKeys, UiRect status,
                                    UiRect joinKey, UiRect listPanel, UiRect well,
                                    UiRect summary, UiRect preview, UiRect admin,
                                    UiRect adminKey, UiRect actionBar, UiRect detailsKey,
                                    UiRect mainKey, UiRect detailPanel, UiRect identity,
                                    UiRect content, UiRect detailAction, UiRect crumb,
                                    UiRect crumbBack, UiRect crumbText, UiRect crumbPager,
                                    UiRect waitingPanel, UiRect testKey) {
    /** Which arrangement is used. */
    public enum Mode {
        WAITING,
        WIDE,
        NARROW_LIST,
        NARROW_DETAIL
    }

    /** Width of {@code main} that switches to the two-page narrow layout. */
    public static final int NARROW_BELOW = 440;
    /** Smallest and largest list column of the wide layout. */
    public static final int LIST_MIN = 176;
    public static final int LIST_MAX = 300;
    /** Width of the narrow "列表" back key, the "详情" key and the crumb pager. */
    public static final int BACK_KEY = 52;
    public static final int DETAILS_KEY = 56;
    public static final int PAGER = 64;
    /** Smallest summary block below the list (preview: {@code rest.h ≥ 34}). */
    public static final int SUMMARY_MIN = 34;

    /** Height of the administrator area: two text lines and a key. */
    public static int adminHeight(TacticalShellLayout.Metrics metrics) {
        return 26 + metrics.buttonHeight();
    }

    /**
     * Height of the administrator area that only holds the test-start key (0.4.0-beta.2): one
     * text line and the key. Used when the vote keys are not shown (not joined yet, or locked).
     */
    public static int adminCompactHeight(TacticalShellLayout.Metrics metrics) {
        return 14 + metrics.buttonHeight();
    }

    /** Gap between the vote key and the test-start key sharing the administrator key row. */
    public static final int ADMIN_KEY_GAP = 4;
    /** Largest share of the administrator key row the test-start key takes beside a vote key. */
    public static final int TEST_KEY_SHARE_PERCENT = 45;

    /** Emblem size of the detail identity block: 64 in the roomy class, otherwise 32. */
    public static int emblemSize(TacticalShellLayout.Metrics metrics) {
        return metrics.roomy() ? 64 : 32;
    }

    /** List row pitch: 24 in the roomy class (two text lines), otherwise the class row height. */
    public static int rowHeight(TacticalShellLayout.Metrics metrics) {
        return metrics.roomy() ? 24 : metrics.rowHeight();
    }

    /** Group title pitch of the list. */
    public static int groupHeight(TacticalShellLayout.Metrics metrics) {
        return metrics.tight() ? TacticalList.HEADER_HEIGHT_TIGHT : TacticalList.HEADER_HEIGHT;
    }

    /** Whether a logical screen uses the two-page narrow layout. */
    public static boolean narrow(int width, int height) {
        TacticalShellLayout.Metrics metrics = TacticalShellLayout.Metrics.forSize(width, height);
        return width - 2 * metrics.margin() - 2 * metrics.gap() < NARROW_BELOW;
    }

    /**
     * @param width       logical screen width
     * @param height      logical screen height
     * @param factions    number of faction keys
     * @param joined      whether the viewer joined a faction (no join key)
     * @param admin       whether the administrator area is shown
     * @param detailPage  narrow layout only: show the detail page
     * @param waiting     no catalog yet
     * @param listNeed    natural height of the well (all rows and group titles + 2)
     * @param joinWidth   preferred width of the join key (text + icon + padding), 0 for none
     */
    public static FormationScreenLayout compute(int width, int height, int factions,
                                                boolean joined, boolean admin,
                                                boolean detailPage, boolean waiting,
                                                int listNeed, int joinWidth) {
        return compute(width, height, factions, joined, admin, detailPage, waiting, listNeed,
                joinWidth, 0);
    }

    /**
     * @param testKeyWidth preferred width of the administrator test-start key (0.4.0-beta.2),
     *                     0 for none. Beside the vote keys ({@code admin}) it takes the right
     *                     part of their row (at most {@value #TEST_KEY_SHARE_PERCENT}%); without
     *                     them a compact administrator area holds it alone, full width.
     * @see #compute(int, int, int, boolean, boolean, boolean, boolean, int, int)
     */
    public static FormationScreenLayout compute(int width, int height, int factions,
                                                boolean joined, boolean admin,
                                                boolean detailPage, boolean waiting,
                                                int listNeed, int joinWidth,
                                                int testKeyWidth) {
        TacticalShellLayout shell = TacticalShellLayout.compute(width, height);
        TacticalShellLayout.Metrics m = shell.metrics();
        UiRect inner = shell.body().inset(m.gap());
        Builder b = new Builder(shell, inner);
        if (waiting) {
            b.mode = Mode.WAITING;
            b.waitingPanel = inner;
            return b.build();
        }
        if (!narrow(width, height)) {
            wide(b, m, factions, joined, admin, listNeed, joinWidth, testKeyWidth);
        } else if (detailPage) {
            narrowDetail(b, m);
        } else {
            narrowList(b, m, factions, admin, listNeed, testKeyWidth);
        }
        return b.build();
    }

    private static void wide(Builder b, TacticalShellLayout.Metrics m, int factions,
                             boolean joined, boolean admin, int listNeed, int joinWidth,
                             int testKeyWidth) {
        b.mode = Mode.WIDE;
        UiRect inner = b.inner;
        b.strip = inner.topSlice(m.buttonHeight());
        int keyGap = m.gap() + 1;
        int count = Math.max(0, factions);
        int preferred = Math.min(150, Math.max(104, b.strip.width() / 5));
        int keyRoom = count == 0 ? 0 : (b.strip.width() * 3 / 5 - keyGap * (count - 1)) / count;
        int keyWidth = Math.max(40, Math.min(preferred, keyRoom));
        int x = b.strip.left();
        for (int index = 0; index < count; index++) {
            b.factionKeys.add(new UiRect(x, b.strip.top(), x + keyWidth, b.strip.bottom()));
            x += keyWidth + keyGap;
        }
        UiRect area = new UiRect(Math.min(b.strip.right(), (count == 0 ? b.strip.left()
                : x - keyGap) + 6), b.strip.top(), b.strip.right(), b.strip.bottom());
        if (!joined && joinWidth > 0) {
            int join = Math.min(joinWidth, area.width() * 55 / 100);
            b.joinKey = area.rightSlice(join);
            b.status = new UiRect(area.left(), area.top(),
                    Math.max(area.left(), b.joinKey.left() - 8), area.bottom());
        } else {
            b.status = area;
        }
        UiRect main = new UiRect(inner.left(), b.strip.bottom() + m.gap() + 2, inner.right() - 1,
                inner.bottom() - 1);
        int listWidth = Math.max(LIST_MIN, Math.min(LIST_MAX,
                (int) Math.round(main.width() * 0.34D)));
        List<UiRect> columns = main.cols(m.gap() + 3, UiRect.Size.px(listWidth),
                UiRect.Size.STAR);
        b.listPanel = columns.get(0);
        listPanelInterior(b, m, admin, listNeed, false, testKeyWidth);
        detailPanel(b, m, columns.get(1));
    }

    private static void narrowList(Builder b, TacticalShellLayout.Metrics m, int factions,
                                   boolean admin, int listNeed, int testKeyWidth) {
        b.mode = Mode.NARROW_LIST;
        UiRect inner = b.inner;
        b.strip = inner.topSlice(m.buttonHeight());
        int count = Math.max(1, factions);
        int keyGap = m.gap() + 1;
        int keyWidth = Math.max(1, (b.strip.width() - keyGap * (count - 1)) / count);
        int x = b.strip.left();
        for (int index = 0; index < factions; index++) {
            int right = index == factions - 1 ? b.strip.right() : x + keyWidth;
            b.factionKeys.add(new UiRect(x, b.strip.top(), right, b.strip.bottom()));
            x = right + keyGap;
        }
        b.listPanel = new UiRect(inner.left(), b.strip.bottom() + m.gap() + 1, inner.right() - 1,
                inner.bottom() - 1);
        listPanelInterior(b, m, admin, listNeed, true, testKeyWidth);
    }

    private static void narrowDetail(Builder b, TacticalShellLayout.Metrics m) {
        b.mode = Mode.NARROW_DETAIL;
        UiRect inner = b.inner;
        b.crumb = inner.topSlice(m.buttonHeight());
        b.crumbBack = b.crumb.leftSlice(BACK_KEY);
        b.crumbPager = b.crumb.rightSlice(PAGER);
        b.crumbText = new UiRect(b.crumbBack.right() + 6, b.crumb.top(),
                Math.max(b.crumbBack.right() + 6, b.crumbPager.left() - 6), b.crumb.bottom());
        detailPanel(b, m, new UiRect(inner.left(), b.crumb.bottom() + m.gap() + 1,
                inner.right() - 1, inner.bottom() - 1));
    }

    private static void listPanelInterior(Builder b, TacticalShellLayout.Metrics m, boolean admin,
                                          int listNeed, boolean narrow, int testKeyWidth) {
        UiRect c = TacticalDraw.panelContent(b.listPanel, m,
                TacticalDraw.PanelStyle.titled(PANEL_TITLE));
        int bottom = c.bottom();
        if (narrow) {
            b.actionBar = new UiRect(c.left(), bottom - m.buttonHeight(), c.right(), bottom);
            List<UiRect> keys = b.actionBar.cols(4, UiRect.Size.px(DETAILS_KEY),
                    UiRect.Size.STAR);
            b.detailsKey = keys.get(0);
            b.mainKey = keys.get(1);
            bottom = b.actionBar.top() - m.gap() - 2;
        }
        if (admin) {
            b.admin = new UiRect(c.left(), bottom - adminHeight(m), c.right(), bottom);
            UiRect row = b.admin.bottomSlice(m.buttonHeight());
            if (testKeyWidth > 0) {
                int test = Math.max(0, Math.min(testKeyWidth,
                        row.width() * TEST_KEY_SHARE_PERCENT / 100));
                b.testKey = row.rightSlice(test);
                b.adminKey = new UiRect(row.left(), row.top(),
                        Math.max(row.left(), b.testKey.left() - ADMIN_KEY_GAP), row.bottom());
            } else {
                b.adminKey = row;
            }
            bottom = b.admin.top() - m.gap() - 2;
        } else if (testKeyWidth > 0) {
            b.admin = new UiRect(c.left(), bottom - adminCompactHeight(m), c.right(), bottom);
            b.testKey = b.admin.bottomSlice(m.buttonHeight());
            bottom = b.admin.top() - m.gap() - 2;
        }
        int wellBottom = Math.max(c.top(), Math.min(bottom, c.top() + Math.max(0, listNeed)));
        b.well = new UiRect(c.left(), c.top(), c.right(), wellBottom);
        UiRect rest = new UiRect(c.left(), Math.min(bottom, b.well.bottom() + m.gap() + 3),
                c.right(), bottom);
        if (narrow) {
            if (rest.height() >= emblemSize(m) + 2) {
                b.preview = rest;
            }
        } else if (rest.height() >= SUMMARY_MIN) {
            b.summary = rest;
        }
    }

    private static void detailPanel(Builder b, TacticalShellLayout.Metrics m, UiRect panel) {
        b.detailPanel = panel;
        UiRect c = TacticalDraw.panelContent(panel, m,
                TacticalDraw.PanelStyle.titled(PANEL_TITLE));
        int emblem = emblemSize(m);
        b.identity = new UiRect(c.left(), c.top(), c.right(), Math.min(c.bottom(), c.top() + emblem));
        b.detailAction = new UiRect(c.left(), Math.max(c.top(), c.bottom() - m.buttonHeight()),
                c.right(), c.bottom());
        int contentTop = Math.min(b.detailAction.top(), b.identity.bottom() + (m.tight() ? 5 : 8));
        b.content = new UiRect(c.left(), contentTop, c.right(),
                Math.max(contentTop, b.detailAction.top() - m.gap() - 4));
    }

    /** Any non-empty title: only {@link TacticalDraw.PanelStyle#hasTitle()} matters for the geometry. */
    private static final net.minecraft.network.chat.Component PANEL_TITLE =
            net.minecraft.network.chat.Component.literal("#");

    /** The y of the hairline between the detail content and its action bar. */
    public int detailSeparatorY() {
        return detailAction.top() - shell.metrics().gap() - 2;
    }

    /** Every non-empty rectangle that holds a control or panel (for overlap tests). */
    public List<UiRect> parts() {
        List<UiRect> parts = new ArrayList<>();
        for (UiRect rect : new UiRect[]{strip, listPanel, detailPanel, crumb, waitingPanel}) {
            if (!rect.isEmpty()) {
                parts.add(rect);
            }
        }
        return parts;
    }

    private static final class Builder {
        private final TacticalShellLayout shell;
        private final UiRect inner;
        private Mode mode = Mode.WIDE;
        private UiRect strip = UiRect.EMPTY;
        private final List<UiRect> factionKeys = new ArrayList<>();
        private UiRect status = UiRect.EMPTY;
        private UiRect joinKey = UiRect.EMPTY;
        private UiRect listPanel = UiRect.EMPTY;
        private UiRect well = UiRect.EMPTY;
        private UiRect summary = UiRect.EMPTY;
        private UiRect preview = UiRect.EMPTY;
        private UiRect admin = UiRect.EMPTY;
        private UiRect adminKey = UiRect.EMPTY;
        private UiRect actionBar = UiRect.EMPTY;
        private UiRect detailsKey = UiRect.EMPTY;
        private UiRect mainKey = UiRect.EMPTY;
        private UiRect detailPanel = UiRect.EMPTY;
        private UiRect identity = UiRect.EMPTY;
        private UiRect content = UiRect.EMPTY;
        private UiRect detailAction = UiRect.EMPTY;
        private UiRect crumb = UiRect.EMPTY;
        private UiRect crumbBack = UiRect.EMPTY;
        private UiRect crumbText = UiRect.EMPTY;
        private UiRect crumbPager = UiRect.EMPTY;
        private UiRect waitingPanel = UiRect.EMPTY;
        private UiRect testKey = UiRect.EMPTY;

        private Builder(TacticalShellLayout shell, UiRect inner) {
            this.shell = shell;
            this.inner = inner;
        }

        private FormationScreenLayout build() {
            return new FormationScreenLayout(mode, shell, inner, strip, List.copyOf(factionKeys),
                    status, joinKey, listPanel, well, summary, preview, admin, adminKey,
                    actionBar, detailsKey, mainKey, detailPanel, identity, content, detailAction,
                    crumb, crumbBack, crumbText, crumbPager, waitingPanel, testKey);
        }
    }
}
