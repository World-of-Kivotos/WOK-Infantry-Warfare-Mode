package com.wok.infantry.client.screen;

import com.wok.infantry.battle.BattleSnapshot;
import com.wok.infantry.client.ClientBattleState;
import com.wok.infantry.client.ClientFormationState;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Shared physical-tablet shell of every WOK步战 terminal (squad, map, loadout, formation, admin).
 *
 * <p>{@link #shell} draws the v2 shell of the preview's {@code UI.shell}: dark device frame with
 * rivets, a dark header strip (status LED, title, page tabs, identity), the gray-green board and
 * a dark footer with key hints on the left and the action receipt on the right. Header layout
 * priority is title &gt; tabs &gt; identity: when space runs out the identity is hidden first,
 * then the tabs switch to their short names (and finally to a pager inside the tab strip), and
 * only then the title is shortened. The geometry comes from {@link TacticalShellLayout}.
 *
 * <p>The old {@code renderShell}/{@code renderHeader} pair stays for screens that still use the
 * {@link TacticalMapLayout} geometry until their batch migrates them.
 */
public final class TacticalBoardChrome {
    // ---- uiTest probe ids of the device shell ---------------------------------------------------

    /** Device case around the glass; reported as a non-solid region. */
    public static final String DEVICE_UI_ID = "shell.device";
    /** Status bar at the top of the display; reported as a non-solid region. */
    public static final String STATUS_UI_ID = "shell.status";
    /** Bottom bezel with the page keys; reported as a non-solid region. */
    public static final String BEZEL_UI_ID = "shell.bezel";
    /** Hardware Esc key at the left end of the bezel. */
    public static final String ESC_KEY_UI_ID = "shell.key.esc";
    /** Hardware R (refresh) key at the right end of the bezel. */
    public static final String REFRESH_KEY_UI_ID = "shell.key.refresh";

    private TacticalBoardChrome() {
    }

    // ---- shell spec types ---------------------------------------------------------------------

    /** Status LED of the header: link to the server data the screen shows. */
    public enum LinkState {
        /** Data is current (green LED, blue identity). */
        OK,
        /** Waiting for the first sync or a pending request (orange). */
        WAIT,
        /** Link lost or stale (red LED). */
        LOST;

        public int ledColor() {
            return switch (this) {
                case OK -> TacticalBoardTheme.SUCCESS_B;
                case WAIT -> TacticalBoardTheme.ACCENT_B;
                case LOST -> TacticalBoardTheme.DANGER_B;
            };
        }

        public int identityColor() {
            return this == OK ? TacticalBoardTheme.SELECT_B : TacticalBoardTheme.ACCENT_B;
        }

        /** OK when a battle snapshot is present, otherwise WAIT. */
        public static LinkState forBattleSnapshot(BattleSnapshot snapshot) {
            return snapshot == null ? WAIT : OK;
        }
    }

    /**
     * One footer key hint such as {@code [Esc] 关闭}. The key text should be the key the player
     * actually has bound: build it from the {@link KeyMapping} with {@link #of(KeyMapping, Component)}
     * instead of writing a fixed letter into the language file.
     */
    public record KeyHint(Component key, Component action) {
        public KeyHint {
            key = key == null ? Component.empty() : key;
            action = action == null ? Component.empty() : action;
        }

        public static KeyHint of(Component key, Component action) {
            return new KeyHint(key, action);
        }

        /** Fixed key text such as {@code "Esc"}, {@code "R"} or {@code "↑↓"}. */
        public static KeyHint literal(String key, Component action) {
            return new KeyHint(Component.literal(key), action);
        }

        /**
         * The key currently bound to {@code mapping} (including a Forge key modifier such as
         * Ctrl), or {@code null} when the mapping is unbound, so the hint is simply left out.
         */
        public static KeyHint of(KeyMapping mapping, Component action) {
            if (mapping == null || mapping.isUnbound()) {
                return null;
            }
            return new KeyHint(mapping.getTranslatedKeyMessage(), action);
        }

        /** {@code [Esc] 关闭}. */
        public static KeyHint close() {
            return literal("Esc", Component.translatable("screen.wok_infantry.hint.close"));
        }

        /** {@code [Esc] 返回}. */
        public static KeyHint back() {
            return literal("Esc", Component.translatable("screen.wok_infantry.hint.back"));
        }

        /** {@code [Ctrl+Tab] 切页}: the terminal tab shortcut of {@link TacticalTabStrip}. */
        public static KeyHint switchTab() {
            return literal("Ctrl+Tab", Component.translatable("screen.wok_infantry.hint.switch_tab"));
        }
    }

    /**
     * Footer receipt. {@link Kind#PENDING} ("处理中", orange) is for requests that are still
     * waiting for the server; green is only for confirmed success. A standing "next step" guide
     * is {@link Kind#GUIDE} and neutral, like the HUD's waiting and to-do plates: orange stays for
     * sections, adjustable controls and attention.
     */
    public record Feedback(Kind kind, Component text) {
        public enum Kind {
            SUCCESS,
            DANGER,
            /** Request sent, result not known yet. */
            PENDING,
            /** Attention without success or failure (cooldown, unsaved changes). */
            NOTICE,
            /** What to do next (a step guide): neutral, never a warning colour. */
            GUIDE
        }

        public Feedback {
            kind = kind == null ? Kind.NOTICE : kind;
            text = text == null ? Component.empty() : text;
        }

        public static Feedback success(Component text) {
            return new Feedback(Kind.SUCCESS, text);
        }

        public static Feedback danger(Component text) {
            return new Feedback(Kind.DANGER, text);
        }

        public static Feedback pending(Component text) {
            return new Feedback(Kind.PENDING, text);
        }

        public static Feedback notice(Component text) {
            return new Feedback(Kind.NOTICE, text);
        }

        public static Feedback guide(Component text) {
            return new Feedback(Kind.GUIDE, text);
        }

        /** Bright semantic colour of the receipt (dark footer). */
        public int color() {
            return switch (kind) {
                case SUCCESS -> TacticalBoardTheme.SUCCESS_B;
                case DANGER -> TacticalBoardTheme.DANGER_B;
                case PENDING, NOTICE -> TacticalBoardTheme.ACCENT_B;
                case GUIDE -> TacticalBoardTheme.NEUTRAL_B;
            };
        }

        public boolean isBlank() {
            return text.getString().isBlank();
        }

        /** Adapter for {@link ClientBattleState#feedback()}; {@code null} when there is none. */
        public static Feedback fromBattle(ClientBattleState.BattleFeedback feedback) {
            if (feedback == null || feedback.message().isBlank()) {
                return null;
            }
            return new Feedback(feedback.success() ? Kind.SUCCESS : Kind.DANGER,
                    Component.literal(feedback.message()));
        }

        /** The current battle receipt (5 s), or {@code null}. */
        public static Feedback fromBattle() {
            return fromBattle(ClientBattleState.feedback());
        }

        /** The current formation receipt (3 s, "处理中" stays orange), or {@code null}. */
        public static Feedback fromFormation() {
            String message = ClientFormationState.feedback();
            if (message == null || message.isBlank()) {
                return null;
            }
            Kind kind = switch (ClientFormationState.feedbackKind()) {
                case SUCCESS -> Kind.SUCCESS;
                case PENDING -> Kind.PENDING;
                case DANGER -> Kind.DANGER;
            };
            return new Feedback(kind, Component.literal(message));
        }
    }

    /**
     * What {@link #shell} draws. {@code identity}, {@code tabs} and {@code feedback} may be
     * {@code null}; unbound key hints ({@code null} entries) are dropped. A strip passed as
     * {@code tabs} is positioned by the shell every frame (and hidden only when the header has no
     * room for it); the screen still adds it as a widget.
     */
    public record ShellSpec(Component title, Component identity, LinkState link,
                            TacticalTabStrip tabs, List<KeyHint> hints, Feedback feedback) {
        public ShellSpec {
            title = title == null ? Component.empty() : title;
            link = link == null ? LinkState.OK : link;
            hints = hints == null ? List.of()
                    : hints.stream().filter(Objects::nonNull).toList();
            if (feedback != null && feedback.isBlank()) {
                feedback = null;
            }
        }

        public static ShellSpec of(Component title) {
            return new ShellSpec(title, null, LinkState.OK, null, List.of(), null);
        }

        public ShellSpec withIdentity(Component value) {
            return new ShellSpec(title, value, link, tabs, hints, feedback);
        }

        public ShellSpec withLink(LinkState value) {
            return new ShellSpec(title, identity, value, tabs, hints, feedback);
        }

        public ShellSpec withTabs(TacticalTabStrip value) {
            return new ShellSpec(title, identity, link, value, hints, feedback);
        }

        public ShellSpec withHints(KeyHint... value) {
            return new ShellSpec(title, identity, link, tabs,
                    value == null ? List.of() : Arrays.asList(value), feedback);
        }

        public ShellSpec withHints(List<KeyHint> value) {
            return new ShellSpec(title, identity, link, tabs, value, feedback);
        }

        public ShellSpec withFeedback(Feedback value) {
            return new ShellSpec(title, identity, link, tabs, hints, value);
        }
    }

    // ---- planned geometry ---------------------------------------------------------------------

    /**
     * Header geometry. {@code title}, {@code identity} are text boxes (8px tall at
     * {@code textY}); {@code tabs} is the slot of the {@link TacticalTabStrip} ({@link UiRect#EMPTY}
     * without tabs); {@code led} is the status light.
     */
    public record HeaderPlan(UiRect led, UiRect title, UiRect tabs, UiRect identity,
                             boolean identityShown, boolean shortTabs, int textY) {
    }

    /** One footer key hint: the key cap box and where the action label starts. */
    public record HintSlot(UiRect keycap, int labelX) {
    }

    /**
     * Footer geometry: {@code hints} holds the slots of the leading hints that fit (trailing
     * hints that do not fit are dropped); {@code feedback} is the receipt box or EMPTY.
     */
    public record FooterPlan(List<HintSlot> hints, UiRect feedback, int textY) {
    }

    /** Result of {@link #shell}: geometry plus the planned header and footer. */
    public record Shell(TacticalShellLayout layout, HeaderPlan header, FooterPlan footer) {
        public UiRect body() {
            return layout.body();
        }

        /** The body inset by one gap. */
        public UiRect content() {
            return layout.content();
        }

        public TacticalShellLayout.Metrics metrics() {
            return layout.metrics();
        }
    }

    private static final int HEADER_TEXT_LEFT = 11;
    private static final int HEADER_GAP = 10;
    private static final int HEADER_RIGHT_PAD = 6;

    /** Plans the header for real text widths. */
    public static HeaderPlan planHeader(Font font, TacticalShellLayout layout, Component title,
                                        TacticalTabStrip tabs, Component identity) {
        boolean tight = layout.tight();
        int full = tabs == null ? 0 : tabs.preferredWidth(font, false, tight);
        int shortWidth = tabs == null ? 0 : tabs.preferredWidth(font, true, tight);
        int identityWidth = identity == null ? 0 : font.width(identity);
        return planHeader(layout.header(), layout.metrics(), title == null ? 0 : font.width(title),
                full, shortWidth, identityWidth);
    }

    /**
     * Pure header planner. Identity takes at most 30% of the header and is hidden first, tabs
     * fall back to their short widths second, and the title is shortened (never below 24px) last.
     * The tab slot therefore never depends on the identity text.
     */
    static HeaderPlan planHeader(UiRect header, TacticalShellLayout.Metrics metrics, int titleWidth,
                                 int tabsFullWidth, int tabsShortWidth, int identityWidth) {
        int textY = header.top() + Math.max(0, (metrics.headerHeight() - 2 - 8) / 2);
        UiRect led = new UiRect(header.left() + 4, textY + 1, header.left() + 7, textY + 7);
        boolean hasTabs = tabsFullWidth > 0;
        int shortTabs = hasTabs ? Math.max(1, Math.min(tabsFullWidth,
                tabsShortWidth <= 0 ? tabsFullWidth : tabsShortWidth)) : 0;
        int available = header.width() - 14;
        int idWidth = identityWidth > 0 ? Math.min(identityWidth, header.width() * 3 / 10) : 0;
        int tabsFullNeed = hasTabs ? tabsFullWidth + HEADER_GAP : 0;
        boolean showIdentity = idWidth > 0
                && titleWidth + tabsFullNeed + idWidth + HEADER_GAP <= available;
        int identitySpace = showIdentity ? idWidth + HEADER_GAP : 0;
        boolean useShort = hasTabs && titleWidth + tabsFullNeed > available
                && shortTabs < tabsFullWidth;
        int tabsWidth = !hasTabs ? 0 : useShort ? shortTabs : tabsFullWidth;
        int titleMax = Math.max(24, available - identitySpace
                - (hasTabs ? tabsWidth + HEADER_GAP : 0));
        int titleShown = Math.max(0, Math.min(titleWidth, titleMax));
        int x = header.left() + HEADER_TEXT_LEFT;
        UiRect titleRect = new UiRect(x, textY, x + titleShown, textY + 8);
        UiRect tabsRect = UiRect.EMPTY;
        if (hasTabs) {
            int tabsLeft = x + titleShown + HEADER_GAP;
            int tabsLimit = header.right() - HEADER_RIGHT_PAD - identitySpace;
            int tabsRight = Math.max(tabsLeft, Math.min(tabsLeft + tabsWidth, tabsLimit));
            tabsRect = new UiRect(tabsLeft, header.top() + 2, tabsRight,
                    Math.max(header.top() + 2, header.bottom() - 2));
        }
        UiRect identityRect = showIdentity
                ? new UiRect(header.right() - idWidth - HEADER_RIGHT_PAD, textY,
                header.right() - HEADER_RIGHT_PAD, textY + 8)
                : UiRect.EMPTY;
        return new HeaderPlan(led, titleRect, tabsRect, identityRect, showIdentity, useShort,
                textY);
    }

    /** Plans the footer for real text widths. */
    public static FooterPlan planFooter(Font font, TacticalShellLayout layout, List<KeyHint> hints,
                                        Feedback feedback) {
        List<KeyHint> safe = hints == null ? List.of() : hints;
        int[] keys = new int[safe.size()];
        int[] labels = new int[safe.size()];
        for (int index = 0; index < safe.size(); index++) {
            keys[index] = font.width(safe.get(index).key());
            labels[index] = font.width(safe.get(index).action());
        }
        int feedbackWidth = feedback == null || feedback.isBlank() ? -1 : font.width(feedback.text());
        return planFooter(layout.footer(), layout.tight(), keys, labels, feedbackWidth);
    }

    /**
     * Pure footer planner: the receipt (if {@code feedbackTextWidth >= 0}) takes the right side,
     * at most 62% (compact) or 50% of the footer; key hints are laid out from the left and the
     * first one that does not fit ends the list.
     */
    static FooterPlan planFooter(UiRect footer, boolean tight, int[] keyWidths, int[] labelWidths,
                                 int feedbackTextWidth) {
        int textY = footer.top() + Math.max(0, (footer.height() - 8) / 2);
        int feedbackWidth = feedbackTextWidth < 0 ? 0
                : Math.min(feedbackTextWidth + 16, (int) Math.floor(footer.width()
                * (tight ? 0.62D : 0.5D)));
        UiRect feedbackRect = feedbackWidth <= 0 ? UiRect.EMPTY
                : new UiRect(footer.right() - feedbackWidth - 2, footer.top() + 1,
                footer.right() - 1, Math.max(footer.top() + 1, footer.bottom() - 1));
        int hintRight = footer.right() - (feedbackWidth > 0 ? feedbackWidth + 6 : 4);
        List<HintSlot> slots = new ArrayList<>();
        int x = footer.left() + 4;
        int count = Math.min(keyWidths.length, labelWidths.length);
        for (int index = 0; index < count; index++) {
            int keycapWidth = keyWidths[index] + 4;
            if (x + keycapWidth + 3 + labelWidths[index] > hintRight) {
                break;
            }
            slots.add(new HintSlot(new UiRect(x, textY - 1, x + keycapWidth, textY + 8),
                    x + keycapWidth + 3));
            x += keycapWidth + 3 + labelWidths[index] + (tight ? 6 : 10);
        }
        return new FooterPlan(List.copyOf(slots), feedbackRect, textY);
    }

    /**
     * Places {@code tabs} into its header slot; call from {@link TacticalScreen#initTactical()}
     * after adding the strip, so it is clickable before the first frame. {@link #shell} applies
     * the same placement every frame.
     */
    public static HeaderPlan placeTabs(Font font, TacticalShellLayout layout, Component title,
                                       TacticalTabStrip tabs) {
        HeaderPlan plan = planHeader(font, layout, title, tabs, null);
        applyTabs(tabs, plan, layout);
        return plan;
    }

    /**
     * Places {@code tabs} (a {@link TacticalTabStrip.Skin#BEZEL} strip, may be {@code null}) as
     * the page keys of the bottom bezel, between the hardware Esc and R keys of {@code hints} (the
     * screen's {@link TacticalScreen#bezelHints()}); call from
     * {@link TacticalScreen#initTactical()} after adding the strip and registering the keys with
     * {@link TacticalScreen#setBezelKeys}. The layout is {@link TacticalBezelPlan}'s; the result
     * is that plan (callers may ignore it).
     */
    public static TacticalBezelPlan placeBezel(Font font, TacticalShellLayout layout,
                                               TacticalTabStrip tabs, List<KeyHint> hints) {
        TacticalBezelPlan plan = TacticalBezelPlan.plan(font, layout, hints);
        if (tabs != null) {
            tabs.placeOnBezel(plan);
        }
        return plan;
    }

    private static void applyTabs(TacticalTabStrip tabs, HeaderPlan plan,
                                  TacticalShellLayout layout) {
        if (tabs == null) {
            return;
        }
        UiRect slot = plan.tabs();
        tabs.setCompact(layout.tight());
        tabs.setBounds(slot.left(), slot.top(), slot.width(), slot.height());
        tabs.visible = !slot.isEmpty();
    }

    // ---- drawing ------------------------------------------------------------------------------

    /**
     * Draws the full tablet shell for {@code layout} and returns the planned geometry. Widgets
     * (including the tab strip) are drawn afterwards by the screen.
     */
    public static Shell shell(GuiGraphics graphics, Font font, TacticalShellLayout layout,
                              ShellSpec spec) {
        ShellSpec safe = spec == null ? ShellSpec.of(Component.empty()) : spec;
        HeaderPlan header = planHeader(font, layout, safe.title(), safe.tabs(), safe.identity());
        applyTabs(safe.tabs(), header, layout);
        FooterPlan footer = planFooter(font, layout, safe.hints(), safe.feedback());
        int width = layout.width();
        int height = layout.height();

        // One extra pixel covers the odd window pixel a 2x layout drops (961 px -> 480 logical).
        graphics.fill(0, 0, width + 1, height + 1, TacticalBoardTheme.FRAME);
        BattleUiTheme.outline(graphics, 1, 1, width - 1, height - 1, TacticalBoardTheme.DEVICE_EDGE);
        graphics.fill(3, 3, width - 3, height - 3, TacticalBoardTheme.FRAME_MID);

        UiRect head = layout.header();
        UiLayoutProbe.begin(graphics, "shell.header", head.left(), head.top(), head.right(),
                head.bottom(), true);
        graphics.fill(head.left(), head.top(), head.right(), head.bottom(), TacticalBoardTheme.FRAME);
        graphics.fill(head.left(), head.bottom() - 1, head.right(), head.bottom(),
                TacticalBoardTheme.BORDER_DARK);
        graphics.fill(head.left(), head.bottom() - 2, head.right(), head.bottom() - 1,
                TacticalBoardTheme.SECTION);
        UiRect led = header.led();
        graphics.fill(led.left(), led.top(), led.right(), led.bottom(), safe.link().ledColor());
        UiRect titleBox = header.title();
        TextFit.draw(graphics, font, safe.title(), titleBox.left(), header.textY(),
                titleBox.width(), TacticalBoardTheme.LIGHT, TextFit.Align.LEFT);
        if (header.identityShown()) {
            UiRect identityBox = header.identity();
            TextFit.draw(graphics, font, safe.identity(), identityBox.left(), header.textY(),
                    identityBox.width(), safe.link().identityColor(), TextFit.Align.RIGHT);
        }
        UiLayoutProbe.end(graphics);

        UiRect body = layout.body();
        if (!body.isEmpty()) {
            graphics.fill(body.left(), body.top(), body.right(), body.bottom(), TacticalBoardTheme.BOARD);
            BattleUiTheme.outline(graphics, body.left() - 1, body.top() - 1, body.right() + 1,
                    body.bottom() + 1, TacticalBoardTheme.BORDER_DARK);
        }

        UiRect foot = layout.footer();
        UiLayoutProbe.begin(graphics, "shell.footer", foot.left(), foot.top(), foot.right(),
                foot.bottom(), true);
        graphics.fill(foot.left(), foot.top(), foot.right(), foot.bottom(), TacticalBoardTheme.FRAME);
        List<HintSlot> slots = footer.hints();
        for (int index = 0; index < slots.size(); index++) {
            drawKeyHint(graphics, font, slots.get(index), safe.hints().get(index));
        }
        if (safe.feedback() != null && !footer.feedback().isEmpty()) {
            drawFeedback(graphics, font, footer.feedback(), footer.textY(), safe.feedback());
        }
        UiLayoutProbe.end(graphics);

        TacticalBoardTheme.rivet(graphics, 2, 2);
        TacticalBoardTheme.rivet(graphics, width - 3, 2);
        TacticalBoardTheme.rivet(graphics, 2, height - 3);
        TacticalBoardTheme.rivet(graphics, width - 3, height - 3);
        return new Shell(layout, header, footer);
    }

    /**
     * Draws one key cap ({@code FRAME_MID} cap, {@code KEYCAP_EDGE} outline, light key name) and its
     * muted action label at text baseline {@code y}; returns the x after the label. The HUD uses
     * the same cap so hints look identical everywhere.
     */
    public static int drawKeyHint(GuiGraphics graphics, Font font, int x, int y, KeyHint hint) {
        int keycapWidth = font.width(hint.key()) + 4;
        HintSlot slot = new HintSlot(new UiRect(x, y - 1, x + keycapWidth, y + 8),
                x + keycapWidth + 3);
        drawKeyHint(graphics, font, slot, hint);
        return slot.labelX() + font.width(hint.action());
    }

    private static void drawKeyHint(GuiGraphics graphics, Font font, HintSlot slot, KeyHint hint) {
        UiRect cap = slot.keycap();
        graphics.fill(cap.left(), cap.top(), cap.right(), cap.bottom(), TacticalBoardTheme.FRAME_MID);
        BattleUiTheme.outline(graphics, cap.left(), cap.top(), cap.right(), cap.bottom(),
                TacticalBoardTheme.KEYCAP_EDGE);
        graphics.drawString(font, hint.key(), cap.left() + 2, cap.top() + 1,
                TacticalBoardTheme.LIGHT, false);
        UiLayoutProbe.rawText(graphics, font, hint.key(), cap.left() + 2, cap.top() + 1);
        if (!hint.action().getString().isEmpty()) {
            graphics.drawString(font, hint.action(), slot.labelX(), cap.top() + 1,
                    TacticalBoardTheme.LIGHT_MUTED, false);
            UiLayoutProbe.rawText(graphics, font, hint.action(), slot.labelX(), cap.top() + 1);
        }
    }

    /** Receipt box: dark plate, 2px semantic bar and ellipsized semantic text. */
    public static void drawFeedback(GuiGraphics graphics, Font font, UiRect box, int textY,
                                    Feedback feedback) {
        if (feedback == null || box.isEmpty()) {
            return;
        }
        int color = feedback.color();
        graphics.fill(box.left(), box.top(), box.right(), box.bottom(), TacticalBoardTheme.FEEDBACK_BG);
        graphics.fill(box.left(), box.top(), Math.min(box.right(), box.left() + 2), box.bottom(),
                color);
        TextFit.draw(graphics, font, feedback.text(), box.left() + 6, textY,
                Math.max(0, box.width() - 11), color, TextFit.Align.LEFT);
    }

    // ---- legacy shell (screens on the TacticalMapLayout geometry) -----------------------------

    static void renderShell(GuiGraphics graphics, int width, int height,
                            TacticalMapLayout.Layout layout) {
        // The device frame is opaque and covers the whole screen, so no world shade or
        // drop shadow is drawn underneath it.
        graphics.fill(0, 0, width, height, TacticalBoardTheme.FRAME);
        BattleUiTheme.outline(graphics, 1, 1, width - 1, height - 1,
                TacticalBoardTheme.DEVICE_EDGE);
        graphics.fill(4, 4, width - 4, height - 4, TacticalBoardTheme.FRAME_MID);

        TacticalMapLayout.Rect header = layout.header();
        TacticalMapLayout.Rect footer = layout.footer();
        graphics.fill(header.left(), header.top(), footer.right(), footer.bottom(),
                TacticalBoardTheme.BOARD);
        TacticalBoardTheme.raisedPanel(graphics, header.left(), header.top(),
                header.right(), header.bottom(), TacticalBoardTheme.FRAME);
        TacticalBoardTheme.raisedPanel(graphics, footer.left(), footer.top(),
                footer.right(), footer.bottom(), TacticalBoardTheme.BOARD_ALT);
        TacticalBoardTheme.rivet(graphics, 6, 6);
        TacticalBoardTheme.rivet(graphics, width - 7, 6);
        TacticalBoardTheme.rivet(graphics, 6, height - 7);
        TacticalBoardTheme.rivet(graphics, width - 7, height - 7);
    }

    static void renderHeader(GuiGraphics graphics, Font font,
                             TacticalMapLayout.Layout layout,
                             Component title, Component identity,
                             boolean connected) {
        TacticalMapLayout.Rect header = layout.header();
        int identityWidth = Math.min(header.width() / 2, font.width(identity));
        int titleWidth = Math.max(32, header.width() - identityWidth - 32);
        int titleY = header.top() + 4;
        // Bright variants: the header strip is the dark device frame.
        int statusColor = connected
                ? TacticalBoardTheme.SUCCESS_B : TacticalBoardTheme.ACCENT_B;
        graphics.fill(header.left() + 5, titleY + 1,
                header.left() + 8, titleY + 8, statusColor);
        TextFit.draw(graphics, font, title, header.left() + 12, titleY, titleWidth,
                TacticalBoardTheme.LIGHT, TextFit.Align.LEFT);
        TextFit.draw(graphics, font, identity, header.right() - identityWidth - 8, titleY,
                identityWidth, connected ? TacticalBoardTheme.SELECT_B
                        : TacticalBoardTheme.ACCENT_B, TextFit.Align.LEFT);
    }

    /** Faction · callsign · role of the viewer (link_connecting while no snapshot). */
    public static Component battleIdentity(BattleSnapshot snapshot) {
        if (snapshot == null) {
            return Component.translatable("screen.wok_infantry.map.link_connecting");
        }
        Component faction = snapshot.faction() == null
                ? Component.translatable("faction.wok_infantry.unassigned")
                : Component.translatable("faction.wok_infantry." + snapshot.faction().id());
        MutableComponent identity = faction.copy();
        if (snapshot.ownSquad() != null) {
            identity.append(" · ").append(SquadScreen.callsign(snapshot.ownSquad()));
        }
        if (snapshot.commander()) {
            identity.append(" · ").append(Component.translatable(
                    "screen.wok_infantry.map.authority.commander"));
        } else if (snapshot.squadLeader()) {
            identity.append(" · ").append(Component.translatable(
                    "screen.wok_infantry.map.authority.squad_leader"));
        } else {
            identity.append(" · ").append(Component.translatable(
                    "screen.wok_infantry.map.authority.member"));
        }
        return identity;
    }
}
