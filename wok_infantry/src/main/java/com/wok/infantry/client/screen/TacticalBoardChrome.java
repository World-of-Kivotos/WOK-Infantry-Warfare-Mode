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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

/**
 * Shared physical-tablet shell of every WOK步战 terminal (squad, map, loadout, formation, admin).
 *
 * <p>{@link #shell} draws the D2 device of the preview ({@code surfaces/17-device.js},
 * {@code deviceShell} at level 2, painted in the P3 livery of {@code 18-device-livery.js}): the
 * case floating on the dimmed world ({@link DeviceArt}), the system status bar at the top of the
 * display, the board, and the bottom bezel where the page keys sit. The status bar carries, left
 * to right, the faction stripe, "title › page" (at most 55% of the free width; the short page name
 * on the compact class), the latest receipt as a pill, the viewer's identity (shortened by
 * {@link #fitIdentity}), then signal, battery and clock at the right end. The geometry comes from
 * {@link TacticalShellLayout}, the plans from the pure {@link #planStatus} and {@link #planBezel}.
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

    /**
     * System property that fixes the status-bar clock, e.g. {@code -Dwok.ui.clock=21:30}, so
     * uiTest probes and screenshots do not change with the time of day.
     */
    public static final String CLOCK_PROPERTY = "wok.ui.clock";
    /**
     * Prefix of the probe note {@link #shell} leaves in a recorded frame ({@link #shellNote}): the
     * livery and link state the device was drawn with. uiTest only; nothing without a probe.
     */
    public static final String SHELL_NOTE = "shell ";

    private TacticalBoardChrome() {
    }

    // ---- shell spec types ---------------------------------------------------------------------

    /**
     * Link to the server data the screen shows: the link LED on the top bezel and the signal bars
     * of the status bar. Screens produce only OK and WAIT; LOST is drawn as the preview does.
     */
    public enum LinkState {
        /** Data is current: the device's own LED colour, four lit bars. */
        OK,
        /** Waiting for the first sync or a pending request: amber LED, two amber bars. */
        WAIT,
        /** Link lost: red LED, no bars and a red cross. */
        LOST;

        /** Colour of the link LED on a device painted with {@code skin}. */
        public int ledColor(DeviceSkin skin) {
            return switch (this) {
                case OK -> skin.led();
                case WAIT -> TacticalBoardTheme.ACCENT_B;
                case LOST -> TacticalBoardTheme.DANGER_B;
            };
        }

        /** Lit signal bars out of four. */
        public int signalBars() {
            return switch (this) {
                case OK -> 4;
                case WAIT -> 2;
                case LOST -> 0;
            };
        }

        /** OK when a battle snapshot is present, otherwise WAIT. */
        public static LinkState forBattleSnapshot(BattleSnapshot snapshot) {
            return snapshot == null ? WAIT : OK;
        }
    }

    /**
     * One key hint such as {@code [Esc] 关闭}: the label of a hardware bezel key, or a hint the HUD
     * prints with {@link #drawKeyHint}. The key text should be the key the player actually has
     * bound: build it from the {@link KeyMapping} with {@link #of(KeyMapping, Component)} instead
     * of writing a fixed letter into the language file.
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
    }

    /**
     * Receipt shown in the status-bar pill. {@link Kind#PENDING} ("处理中", orange) is for requests
     * that are still waiting for the server; green is only for confirmed success. A standing "next
     * step" guide is {@link Kind#GUIDE} and neutral, like the HUD's waiting and to-do plates:
     * orange stays for sections, adjustable controls and attention.
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

        /** Bright semantic colour of the receipt (pill bar and text on the status bar). */
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
     * {@code null}; unbound key hints ({@code null} entries) are dropped. The shell reads the
     * current page of {@code tabs} for the status bar's "title › page"; the strip itself is a
     * widget of the screen, placed on the bezel by {@link #placeBezel}.
     * The device draws no {@code hints}: Esc and R are hardware keys of the bezel
     * ({@link TacticalScreen#setBezelKeys}).
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
     * One text of the status bar: {@code text} is what is drawn ({@code full} fitted into
     * {@code maxWidth}), starting at {@code x} and {@code width} wide; {@code truncated} says it
     * ends in an ellipsis. {@link #NONE} when the part is left out.
     */
    public record StatusText(String full, String text, int x, int maxWidth, int width,
                             boolean truncated) {
        public static final StatusText NONE = new StatusText("", "", 0, 0, 0, false);

        public StatusText {
            full = full == null ? "" : full;
            text = text == null ? "" : text;
        }

        public boolean shown() {
            return !text.isEmpty();
        }

        /** Where the drawn text ends. */
        public int right() {
            return x + width;
        }
    }

    /**
     * Status-bar geometry (preview {@code statusBar}): text baseline {@code textY}, the faction
     * {@code stripe}, the {@code title} ("title › page"), the receipt {@code pill} and its
     * {@code feedback} text ({@link UiRect#EMPTY} / {@link StatusText#NONE} without a receipt or
     * when the pill would be narrower than {@link #PILL_MIN_WIDTH}), the {@code identity} and the
     * room it had ({@code identityRoom}, what a screen may use to pick its own identity text),
     * the signal bars at {@code signalX}, the battery at {@code batteryX} and the {@code clock}.
     */
    public record StatusPlan(UiRect bar, int textY, UiRect stripe, StatusText title, UiRect pill,
                             StatusText feedback, int identityRoom, StatusText identity,
                             int signalX, int batteryX, StatusText clock) {
        /** The 2px semantic bar at the left end of the pill, or EMPTY. */
        public UiRect pillBar() {
            return pill.isEmpty() ? UiRect.EMPTY
                    : new UiRect(pill.left(), pill.top(), Math.min(pill.right(), pill.left() + 2),
                    pill.bottom());
        }

        /** Box of the four signal bars (and the cross of a lost link). */
        public UiRect signal() {
            return new UiRect(signalX, textY, signalX + 9, textY + 7);
        }

        /** Box of the battery with its terminal. */
        public UiRect battery() {
            return new UiRect(batteryX, textY + 1, batteryX + 11, textY + 7);
        }

        /**
         * Hover box of a status-bar text: its drawn span across the whole bar height, or EMPTY
         * when it is not shown.
         */
        public UiRect box(StatusText text) {
            return text == null || !text.shown() ? UiRect.EMPTY
                    : new UiRect(text.x(), bar.top(), text.right(), bar.bottom());
        }

        /**
         * The whole text behind a shortened part of the bar at ({@code x}, {@code y}), or
         * {@code null}: the receipt anywhere on its pill, the title on its drawn span, and the
         * identity on its span when {@link #fitIdentity} dropped parts of it or cut it. The
         * screen shows it as the hover tooltip ({@link TacticalScreen}).
         */
        public String fullTextAt(double x, double y) {
            if (feedback.truncated() && pill.contains(x, y)) {
                return feedback.full();
            }
            if (title.truncated() && box(title).contains(x, y)) {
                return title.full();
            }
            if (identity.shown() && !identity.text().equals(identity.full())
                    && box(identity).contains(x, y)) {
                return identity.full();
            }
            return null;
        }
    }

    /**
     * Geometry of the bottom bezel (preview {@code pageKeys}): the whole {@code bezel} strip, the
     * {@code keyRow} the hardware keys sit in (inset from the glass edges, ending above the case
     * edge) and the row of the page LEDs above it ({@code ledTop}, {@code ledHeight}).
     */
    public record BezelPlan(UiRect bezel, UiRect keyRow, int ledTop, int ledHeight) {
    }

    /** Result of {@link #shell}: geometry plus the planned status bar and bezel. */
    public record Shell(TacticalShellLayout layout, StatusPlan status, BezelPlan bezel) {
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

    /** Narrowest receipt pill; a narrower one is not drawn at all. */
    public static final int PILL_MIN_WIDTH = 40;
    /** Narrowest identity room; below it the identity is left out. */
    static final int IDENTITY_MIN_ROOM = 30;
    /** Separator between the title and the page name. */
    static final String PAGE_SEPARATOR = " › ";
    /** Separator between the parts of an identity ("阵营 · 编制 · 小队 · 职务"). */
    static final String IDENTITY_SEPARATOR = " · ";
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    /** Plans the status bar of {@code layout} for {@code spec} with real text widths. */
    public static StatusPlan planStatus(Font font, TacticalShellLayout layout, ShellSpec spec) {
        ShellSpec safe = spec == null ? ShellSpec.of(Component.empty()) : spec;
        return planStatus(layout.status(), statusTitle(safe.title(), safe.tabs(), layout.tight()),
                safe.feedback() == null ? null : safe.feedback().text().getString(),
                safe.identity() == null ? null : safe.identity().getString(), clockText(),
                font::width);
    }

    /**
     * Pure status-bar planner, a port of the preview's {@code statusBar}. From the right: clock,
     * battery and signal; from the left: faction stripe and the title (never more than 55% of
     * the room left between them, at least 24px). The receipt pill is right-aligned in what
     * remains (text width + 9, left out below {@link #PILL_MIN_WIDTH}), and the identity, shortened
     * by {@link #fitIdentity}, fills the rest up to the pill.
     */
    public static StatusPlan planStatus(UiRect bar, String title, String feedback, String identity,
                                        String clock, ToIntFunction<String> width) {
        int textY = bar.top() + Math.max(0, (bar.height() - 9) / 2);
        // right: clock, battery, signal (= link to the server)
        int xr = bar.right() - 3;
        String clockText = clock == null ? "" : clock;
        int clockWidth = clockText.isEmpty() ? 0 : width.applyAsInt(clockText);
        xr -= clockWidth;
        StatusText clockSlot = clockText.isEmpty() ? StatusText.NONE
                : new StatusText(clockText, clockText, xr, clockWidth, clockWidth, false);
        xr -= 5;
        xr -= 11;
        int batteryX = xr;
        xr -= 4;
        xr -= 9;
        int signalX = xr;
        xr -= 7;
        // left: faction stripe, then "title › page"
        int x = bar.left() + 3;
        UiRect stripe = new UiRect(x, textY + 1, x + 2, textY + 7);
        x += 5;
        int titleMax = Math.max(24, (int) Math.floor((xr - x) * 0.55D));
        StatusText titleSlot = fitted(title, x, titleMax, width);
        x += titleSlot.width() + 8;
        // middle, right-aligned: the latest receipt
        UiRect pill = UiRect.EMPTY;
        StatusText feedbackSlot = StatusText.NONE;
        if (feedback != null && !feedback.isBlank()) {
            int pillWidth = Math.min(width.applyAsInt(feedback) + 9, xr - x);
            if (pillWidth >= PILL_MIN_WIDTH) {
                int left = xr - pillWidth;
                pill = new UiRect(left, bar.top() + 1, xr, Math.max(bar.top() + 1, bar.bottom() - 2));
                feedbackSlot = fitted(feedback, left + 5, pillWidth - 7, width);
                xr = left - 6;
            }
        }
        // then the identity, right-aligned up to the pill
        int room = xr - x;
        TextFit.Plain id = fitIdentity(identity, room, width);
        StatusText identitySlot = id.text().isEmpty() ? StatusText.NONE
                : new StatusText(identity, id.text(), xr - id.width(), Math.max(0, room), id.width(),
                id.truncated());
        return new StatusPlan(bar, textY, stripe, titleSlot, pill, feedbackSlot, room, identitySlot,
                signalX, batteryX, clockSlot);
    }

    private static StatusText fitted(String text, int x, int maxWidth,
                                     ToIntFunction<String> width) {
        String safe = text == null ? "" : text;
        if (safe.isEmpty()) {
            return new StatusText("", "", x, maxWidth, 0, false);
        }
        TextFit.Plain plain = TextFit.fitPlain(safe, maxWidth, width);
        return new StatusText(safe, plain.text(), x, maxWidth, plain.width(), plain.truncated());
    }

    /**
     * The status bar's title: {@code title › page} with the current page of {@code tabs} (its
     * short name on the compact class), or the title alone without a current page.
     */
    static String statusTitle(Component title, TacticalTabStrip tabs, boolean tight) {
        String page = null;
        if (tabs != null && tabs.current() >= 0 && tabs.current() < tabs.tabs().size()) {
            TacticalTabStrip.Tab tab = tabs.tabs().get(tabs.current());
            page = (tight ? tab.shortLabel() : tab.label()).getString();
        }
        return statusTitle(title == null ? "" : title.getString(), page);
    }

    /**
     * A terminal's status-bar title (preview {@code shellTitle}): {@code full} on layouts at least
     * {@code shortBelow} wide where "full › page" fits the bar's title room without an ellipsis
     * for every page of {@code pages} (short names on the compact class), else {@code shortTitle}
     * (the title without "WOK步战 // "), so a narrow bar keeps its room for the identity. One answer
     * for all pages: a page key never makes the title jump.
     */
    public static Component shellTitle(Font font, TacticalShellLayout layout, Component full,
                                       Component shortTitle, TacticalTabStrip pages,
                                       int shortBelow) {
        if (layout.width() < shortBelow) {
            return shortTitle;
        }
        int room = planStatus(font, layout, ShellSpec.of(Component.empty())).title().maxWidth();
        List<String> names = new ArrayList<>();
        if (pages != null) {
            for (TacticalTabStrip.Tab tab : pages.tabs()) {
                names.add((layout.tight() ? tab.shortLabel() : tab.label()).getString());
            }
        }
        if (names.isEmpty()) {
            names.add(null);
        }
        String head = full == null ? "" : full.getString();
        for (String page : names) {
            if (font.width(statusTitle(head, page)) > room) {
                return shortTitle;
            }
        }
        return full;
    }

    /** Pure part of {@link #statusTitle(Component, TacticalTabStrip, boolean)}. */
    static String statusTitle(String title, String page) {
        String head = title == null ? "" : title;
        if (page == null || page.isBlank()) {
            return head;
        }
        return head.isBlank() ? page : head + PAGE_SEPARATOR + page;
    }

    /**
     * The longest form of an identity "阵营 · 编制 · 小队 · 职务" that fits {@code maxWidth}
     * (preview {@code fitIdentity}): the whole text, then without the second part (four parts
     * or more), then the first and the second-to-last part (three or more), then the first part
     * alone; when none fits, the whole text ellipsized. Nothing below
     * {@link #IDENTITY_MIN_ROOM}. Only the ellipsized fallback counts as truncated.
     */
    public static TextFit.Plain fitIdentity(String text, int maxWidth, ToIntFunction<String> width) {
        if (text == null || text.isEmpty() || maxWidth < IDENTITY_MIN_ROOM) {
            return new TextFit.Plain("", 0, false);
        }
        String[] parts = text.split(Pattern.quote(IDENTITY_SEPARATOR), -1);
        List<String> tries = new ArrayList<>(4);
        tries.add(text);
        if (parts.length >= 4) {
            List<String> kept = new ArrayList<>(Arrays.asList(parts));
            kept.remove(1);
            tries.add(String.join(IDENTITY_SEPARATOR, kept));
        }
        if (parts.length >= 3) {
            tries.add(parts[0] + IDENTITY_SEPARATOR + parts[parts.length - 2]);
        }
        tries.add(parts[0]);
        for (String candidate : tries) {
            int candidateWidth = width.applyAsInt(candidate);
            if (candidateWidth <= maxWidth) {
                return new TextFit.Plain(candidate, candidateWidth, false);
            }
        }
        return TextFit.fitPlain(text, maxWidth, width);
    }

    /** The status-bar clock: {@link #CLOCK_PROPERTY} when set, otherwise the local time. */
    public static String clockText() {
        return clockText(System.getProperty(CLOCK_PROPERTY), LocalTime.now());
    }

    /** Pure clock rule: a non-blank {@code override} as given, otherwise {@code now} as HH:mm. */
    static String clockText(String override, LocalTime now) {
        if (override != null && !override.isBlank()) {
            return override.strip();
        }
        return (now == null ? LocalTime.MIDNIGHT : now).format(CLOCK_FORMAT);
    }

    /**
     * Pure bezel planner (preview {@code pageKeys}), the rows of {@link TacticalBezelPlan} so the
     * device and its keys share one bezel layout: the key row starts 3px (compact) or 12px in from
     * the glass edges and ends 1px (compact) or 3px above the case edge; the page LEDs sit 2 / 3px
     * below the bezel top, 1 / 2px tall, and the keys start 1 / 2px below them (a bezel too low
     * for LEDs gives the keys the LED row, {@code ledHeight} 0).
     */
    public static BezelPlan planBezel(TacticalShellLayout layout) {
        UiRect bezel = layout.bezel();
        TacticalBezelPlan keys = TacticalBezelPlan.plan(bezel, layout.density(), 0, 0);
        int inset = TacticalBezelPlan.inset(layout.density());
        int left = bezel.left() + inset;
        int right = Math.max(left, bezel.right() - inset);
        return new BezelPlan(bezel, new UiRect(left, keys.keyTop(), right, keys.keyBottom()),
                keys.ledTop(), keys.ledHeight());
    }

    /**
     * Places {@code tabs} (a {@link TacticalTabStrip.Skin#BEZEL} strip, may be {@code null}) as
     * the page keys of the bottom bezel, between the hardware Esc and R keys of {@code hints} (the
     * screen's {@link TacticalScreen#bezelHints()}); call from
     * {@link TacticalScreen#initTactical()} after adding the strip and registering the keys with
     * {@link TacticalScreen#setBezelKeys}. The layout is {@link TacticalBezelPlan}'s, Esc and R
     * shrunk to their key names when the page keys would otherwise page (the screen's own keys
     * follow the same plan, {@link TacticalScreen#setTabStrip}); the result is that plan
     * (callers may ignore it).
     */
    public static TacticalBezelPlan placeBezel(Font font, TacticalShellLayout layout,
                                               TacticalTabStrip tabs, List<KeyHint> hints) {
        TacticalBezelPlan plan = TacticalBezelPlan.plan(font, layout, hints, tabs);
        if (tabs != null) {
            tabs.placeOnBezel(plan);
        }
        return plan;
    }

    // ---- drawing ------------------------------------------------------------------------------

    /**
     * Draws the D2 device for {@code layout} painted in {@code livery} (case, status bar, board)
     * and returns the planned geometry. The world dim behind it is the screen's backdrop and the
     * glass over the page is drawn after the page and its modal; widgets (the page keys too) are
     * drawn by the screen.
     */
    public static Shell shell(GuiGraphics graphics, Font font, TacticalShellLayout layout,
                              TacticalLivery.Livery livery, ShellSpec spec) {
        return shell(graphics, font, layout, livery, spec, 1.0F);
    }

    /**
     * {@link #shell(GuiGraphics, Font, TacticalShellLayout, TacticalLivery.Livery, ShellSpec)} with
     * the case's drop shadow faded to {@code shadowAlpha} (0.5.0-beta.4: the opening animation
     * fades it in); 1 draws exactly the same device.
     */
    public static Shell shell(GuiGraphics graphics, Font font, TacticalShellLayout layout,
                              TacticalLivery.Livery livery, ShellSpec spec, float shadowAlpha) {
        ShellSpec safe = spec == null ? ShellSpec.of(Component.empty()) : spec;
        TacticalLivery.Livery paint = livery == null ? TacticalLivery.Livery.NEUTRAL : livery;
        DeviceSkin skin = paint.skin();
        StatusPlan status = planStatus(font, layout, safe);
        BezelPlan bezel = planBezel(layout);

        UiRect device = layout.device();
        int bump = layout.deviceMetrics().bump();
        UiLayoutProbe.begin(graphics, DEVICE_UI_ID, device.left() - bump, device.top() - bump,
                device.right() + bump, device.bottom() + bump, false);
        DeviceArt.drawDevice(graphics, font, layout, paint, safe.link().ledColor(skin),
                TacticalBoardTheme.FRAME_MID, shadowAlpha);
        UiLayoutProbe.end(graphics);

        drawStatus(graphics, font, status, safe, skin);

        UiRect body = layout.body();
        if (!body.isEmpty()) {
            graphics.fill(body.left(), body.top(), body.right(), body.bottom(), TacticalBoardTheme.BOARD);
            BattleUiTheme.outline(graphics, body.left() - 1, body.top() - 1, body.right() + 1,
                    body.bottom() + 1, TacticalBoardTheme.BORDER_DARK);
        }

        UiRect keys = bezel.bezel();
        UiLayoutProbe.begin(graphics, BEZEL_UI_ID, keys.left(), keys.top(), keys.right(),
                keys.bottom(), false);
        UiLayoutProbe.end(graphics);
        if (UiLayoutProbe.recording()) {
            UiLayoutProbe.note(shellNote(paint, safe.link()));
        }
        return new Shell(layout, status, bezel);
    }

    /** {@code shell livery=CAESAR link=OK}: the probe note of one drawn device (uiTest). */
    public static String shellNote(TacticalLivery.Livery livery, LinkState link) {
        return SHELL_NOTE + "livery=" + (livery == null ? TacticalLivery.Livery.NEUTRAL : livery)
                .name() + " link=" + (link == null ? LinkState.OK : link).name();
    }

    private static void drawStatus(GuiGraphics graphics, Font font, StatusPlan plan,
                                   ShellSpec spec, DeviceSkin skin) {
        UiRect bar = plan.bar();
        if (bar.isEmpty()) {
            return;
        }
        UiLayoutProbe.begin(graphics, STATUS_UI_ID, bar.left(), bar.top(), bar.right(),
                bar.bottom(), false);
        graphics.fill(bar.left(), bar.top(), bar.right(), bar.bottom(), skin.status());
        graphics.fill(bar.left(), bar.bottom() - 1, bar.right(), bar.bottom(), skin.statusLine());
        int y = plan.textY();

        StatusText clock = plan.clock();
        if (clock.shown()) {
            graphics.drawString(font, clock.text(), clock.x(), y, TacticalBoardTheme.LIGHT, false);
            UiLayoutProbe.rawText(graphics, font, clock.text(), clock.x(), y);
        }
        drawBattery(graphics, plan.batteryX(), y);
        drawSignal(graphics, plan.signalX(), y, spec.link(), skin.signalOff());

        UiRect stripe = plan.stripe();
        graphics.fill(stripe.left(), stripe.top(), stripe.right(), stripe.bottom(), skin.stripe());
        StatusText title = plan.title();
        if (title.shown()) {
            TextFit.draw(graphics, font, title.full(), title.x(), y, title.maxWidth(),
                    TacticalBoardTheme.LIGHT, TextFit.Align.LEFT);
            if (title.truncated()) {
                // Like the pill: the whole title while the mouse rests on it.
                UiLayoutProbe.tipped(graphics, title.x(), y);
            }
        }

        Feedback feedback = spec.feedback();
        UiRect pill = plan.pill();
        if (feedback != null && !pill.isEmpty()) {
            int color = feedback.color();
            graphics.fill(pill.left(), pill.top(), pill.right(), pill.bottom(), skin.pill());
            UiRect pillBar = plan.pillBar();
            graphics.fill(pillBar.left(), pillBar.top(), pillBar.right(), pillBar.bottom(), color);
            StatusText text = plan.feedback();
            TextFit.draw(graphics, font, text.full(), text.x(), y, text.maxWidth(), color,
                    TextFit.Align.LEFT);
            if (text.truncated()) {
                // The screen shows the whole receipt while the mouse rests on the pill.
                UiLayoutProbe.tipped(graphics, text.x(), y);
            }
        }

        StatusText identity = plan.identity();
        if (identity.shown()) {
            if (identity.truncated()) {
                int right = identity.right();
                TextFit.draw(graphics, font, identity.full(), right - identity.maxWidth(), y,
                        identity.maxWidth(), skin.ident(), TextFit.Align.RIGHT);
                UiLayoutProbe.tipped(graphics, identity.x(), y);
            } else {
                graphics.drawString(font, identity.text(), identity.x(), y, skin.ident(), false);
                UiLayoutProbe.rawText(graphics, font, identity.text(), identity.x(), y);
            }
        }
        UiLayoutProbe.end(graphics);
    }

    /** Battery (preview {@code battery}): a 10×6 outline, its terminal and a full charge. */
    private static void drawBattery(GuiGraphics graphics, int x, int y) {
        int edge = TacticalBoardTheme.LIGHT_MUTED;
        graphics.fill(x, y + 1, x + 10, y + 2, edge);
        graphics.fill(x, y + 6, x + 10, y + 7, edge);
        graphics.fill(x, y + 2, x + 1, y + 6, edge);
        graphics.fill(x + 9, y + 2, x + 10, y + 6, edge);
        graphics.fill(x + 10, y + 3, x + 11, y + 5, edge);
        graphics.fill(x + 2, y + 3, x + 8, y + 5, TacticalBoardTheme.SUCCESS_B);
    }

    /**
     * Signal bars (preview {@code signal}): four bars rising to the right, {@link
     * LinkState#signalBars()} of them lit (amber while waiting), and a red cross for a lost link.
     */
    private static void drawSignal(GuiGraphics graphics, int x, int y, LinkState link, int off) {
        int lit = link.signalBars();
        int color = link == LinkState.WAIT ? TacticalBoardTheme.ACCENT_B : TacticalBoardTheme.LIGHT;
        for (int index = 0; index < 4; index++) {
            graphics.fill(x + index * 2, y + 5 - index, x + index * 2 + 1, y + 7,
                    index < lit ? color : off);
        }
        if (link == LinkState.LOST) {
            int cross = TacticalBoardTheme.DANGER_B;
            int[][] pixels = {{6, 0}, {7, 1}, {8, 2}, {8, 0}, {6, 2}};
            for (int[] pixel : pixels) {
                graphics.fill(x + pixel[0], y + pixel[1], x + pixel[0] + 1, y + pixel[1] + 1, cross);
            }
        }
    }

    /**
     * Draws one key cap ({@code FRAME_MID} cap, {@code KEYCAP_EDGE} outline, light key name) and its
     * muted action label at text baseline {@code y}; returns the x after the label. The HUD uses
     * the same cap so hints look identical everywhere.
     */
    public static int drawKeyHint(GuiGraphics graphics, Font font, int x, int y, KeyHint hint) {
        int keycapWidth = font.width(hint.key()) + 4;
        UiRect cap = new UiRect(x, y - 1, x + keycapWidth, y + 8);
        int labelX = x + keycapWidth + 3;
        graphics.fill(cap.left(), cap.top(), cap.right(), cap.bottom(), TacticalBoardTheme.FRAME_MID);
        BattleUiTheme.outline(graphics, cap.left(), cap.top(), cap.right(), cap.bottom(),
                TacticalBoardTheme.KEYCAP_EDGE);
        graphics.drawString(font, hint.key(), cap.left() + 2, cap.top() + 1,
                TacticalBoardTheme.LIGHT, false);
        UiLayoutProbe.rawText(graphics, font, hint.key(), cap.left() + 2, cap.top() + 1);
        if (!hint.action().getString().isEmpty()) {
            graphics.drawString(font, hint.action(), labelX, cap.top() + 1,
                    TacticalBoardTheme.LIGHT_MUTED, false);
            UiLayoutProbe.rawText(graphics, font, hint.action(), labelX, cap.top() + 1);
        }
        return labelX + font.width(hint.action());
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
