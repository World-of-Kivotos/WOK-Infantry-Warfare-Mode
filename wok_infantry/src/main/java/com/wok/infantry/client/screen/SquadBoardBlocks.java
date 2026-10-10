package com.wok.infantry.client.screen;

import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Text blocks shared by the battle terminal's squad, class and deployment pages (preview
 * {@code 20-squad.js}): orphan-free paragraphs, titled bullet lists that fit a height ("更换规则",
 * "须知", "怎么投票") and the match flow of the formation vote. Pure fitting is separated from
 * drawing so the layouts can be planned in {@code init()} and drawn every frame.
 */
final class SquadBoardBlocks {
    /** Text line pitch. */
    static final int LINE = TacticalDraw.LINE_HEIGHT;

    private SquadBoardBlocks() {
    }

    // ---- paragraphs ---------------------------------------------------------------------------------

    /** Lines of the first variant that wraps cleanly into {@code maxLines} (0 = any). */
    static List<String> lines(Font font, List<Component> variants, int width, int maxLines) {
        return TextFit.wrapBest(font, variants, Math.max(1, width), maxLines).lines();
    }

    /**
     * Draws the orphan-free wrap of {@code variants} at 10px pitch; returns the y under the last
     * line. Nothing is drawn when not even one line fits ({@code maxLines < 1}).
     */
    static int paragraph(GuiGraphics graphics, Font font, List<Component> variants, int x, int y,
                         int width, int color, int maxLines) {
        if (maxLines < 1 || variants.isEmpty() || width <= 0) {
            return y;
        }
        for (String line : lines(font, variants, width, maxLines)) {
            TextFit.draw(graphics, font, line, x, y, width, color, TextFit.Align.LEFT);
            y += LINE;
        }
        return y;
    }

    /** Draws the first variant that fits (the last one ellipsized); returns the drawn width. */
    static int fitted(GuiGraphics graphics, Font font, List<Component> variants, int x, int y,
                      int width, int color, TextFit.Align align) {
        if (variants.isEmpty() || width <= 0) {
            return 0;
        }
        return TextFit.draw(graphics, font, FormationDetailPanel.pick(font, variants, width), x, y,
                width, color, align).width();
    }

    // ---- bullet rules ---------------------------------------------------------------------------------

    /** One rule: its variants, longest first. */
    record Rule(List<Component> variants) {
        Rule {
            variants = List.copyOf(variants);
        }

        static Rule of(List<Component> variants) {
            return new Rule(variants);
        }
    }

    /** A fitted rules block: the lines of each rule, the pitch and the total height. */
    record RulesFit(Component title, List<List<String>> lines, int pitch, int height) {
        RulesFit {
            lines = List.copyOf(lines);
        }
    }

    /**
     * Fits a titled bullet list into {@code height}: whole sentences that wrap cleanly, then
     * one-line short forms, then (unless {@code all}) the first rules that fit. {@code null} when
     * nothing fits.
     */
    static RulesFit fitRules(Font font, TacticalShellLayout.Metrics metrics, int width,
                             Component title, List<Rule> rules, int height, boolean all) {
        if (width <= 8 || height <= 0) {
            return null;
        }
        int pitch = metrics.roomy() ? 12 : LINE;
        List<List<String>> lines = null;
        for (boolean shortForms : new boolean[]{false, true}) {
            lines = ruleLines(font, width - 8, rules, shortForms);
            int total = metrics.sectionHeight() + 3;
            for (List<String> rule : lines) {
                total += rule.size() * pitch;
            }
            if (total <= height) {
                return new RulesFit(title, lines, pitch, total);
            }
        }
        if (all) {
            return null;
        }
        int total = metrics.sectionHeight() + 3;
        List<List<String>> kept = new ArrayList<>();
        for (List<String> rule : lines) {
            if (total + rule.size() * pitch > height) {
                break;
            }
            kept.add(rule);
            total += rule.size() * pitch;
        }
        return kept.isEmpty() ? null : new RulesFit(title, kept, pitch, total);
    }

    private static List<List<String>> ruleLines(Font font, int width, List<Rule> rules,
                                                boolean shortForms) {
        List<List<String>> result = new ArrayList<>(rules.size());
        for (Rule rule : rules) {
            if (shortForms) {
                Component one = null;
                for (Component variant : rule.variants()) {
                    if (font.width(variant) <= width) {
                        one = variant;
                        break;
                    }
                }
                if (one != null) {
                    result.add(List.of(one.getString()));
                    continue;
                }
            }
            result.add(lines(font, rule.variants(), width, 0));
        }
        return result;
    }

    /** Draws a fitted rules block in {@code bounds} (section strip, then the bullets). */
    static void rules(GuiGraphics graphics, Font font, UiRect bounds,
                      TacticalShellLayout.Metrics metrics, RulesFit fit) {
        if (fit == null) {
            return;
        }
        subRegion(graphics, "squad.rules", bounds);
        TacticalDraw.section(graphics, font, bounds.topSlice(metrics.sectionHeight()),
                fit.title());
        int y = bounds.top() + metrics.sectionHeight() + 3;
        for (List<String> rule : fit.lines()) {
            graphics.fill(bounds.left() + 2, y + 3, bounds.left() + 4, y + 5,
                    TacticalBoardTheme.MUTED);
            for (String line : rule) {
                TextFit.draw(graphics, font, line, bounds.left() + 8, y, bounds.width() - 8,
                        TacticalBoardTheme.MUTED, TextFit.Align.LEFT);
                y += fit.pitch();
            }
        }
        UiLayoutProbe.end(graphics);
    }

    // ---- match flow -------------------------------------------------------------------------------

    /** State of one flow step: done (check), current (clock) or later (dot). */
    enum StepState {
        DONE, CURRENT, LATER
    }

    /** One step of the match flow: name and value variants. */
    record Step(Component name, List<Component> value, StepState state) {
        Step {
            value = List.copyOf(value);
        }
    }

    /** Room the flow takes: two-line steps when tall (roomy), else one line per step. */
    record FlowFit(int pitch, int height) {
        boolean twoLines() {
            return pitch >= 20;
        }
    }

    static FlowFit fitFlow(TacticalShellLayout.Metrics metrics, int steps, int height) {
        int one = metrics.roomy() ? 13 : 11;
        int[] pitches = metrics.roomy() ? new int[]{24, one} : new int[]{one};
        for (int pitch : pitches) {
            int total = metrics.sectionHeight() + 3 + steps * pitch;
            if (total <= height) {
                return new FlowFit(pitch, total);
            }
        }
        return null;
    }

    static void flow(GuiGraphics graphics, Font font, UiRect bounds,
                     TacticalShellLayout.Metrics metrics, List<Step> steps, FlowFit fit) {
        if (fit == null) {
            return;
        }
        subRegion(graphics, "squad.flow", bounds);
        Component meta = FormationDetailPanel.pick(font,
                SquadBoardText.all(SquadBoardText.FLOW_META), bounds.width() * 2 / 5);
        if (font.width(meta) > bounds.width() * 2 / 5) {
            meta = Component.empty();
        }
        TacticalDraw.section(graphics, font, bounds.topSlice(metrics.sectionHeight()),
                SquadBoardText.t(SquadBoardText.FLOW_TITLE), meta, TacticalBoardTheme.LIGHT_MUTED,
                TacticalBoardTheme.SECTION);
        int y = bounds.top() + metrics.sectionHeight() + 3;
        int keyWidth = 0;
        for (Step step : steps) {
            keyWidth = Math.max(keyWidth, font.width(step.name()));
        }
        keyWidth += 8;
        for (int index = 0; index < steps.size(); index++) {
            Step step = steps.get(index);
            int iconColor = switch (step.state()) {
                case DONE -> TacticalBoardTheme.SUCCESS;
                case CURRENT -> TacticalBoardTheme.TEXT;
                case LATER -> TacticalBoardTheme.FAINT;
            };
            TacticalIcon icon = switch (step.state()) {
                case DONE -> TacticalIcon.CHECK;
                case CURRENT -> TacticalIcon.CLOCK;
                case LATER -> TacticalIcon.DOT;
            };
            icon.draw(graphics, bounds.left(), y - 1, iconColor);
            int nameColor = step.state() == StepState.LATER ? TacticalBoardTheme.MUTED
                    : TacticalBoardTheme.TEXT;
            int valueColor = step.state() == StepState.CURRENT ? TacticalBoardTheme.TEXT
                    : TacticalBoardTheme.MUTED;
            int x = bounds.left() + 13;
            if (fit.twoLines()) {
                if (index < steps.size() - 1) {
                    graphics.fill(bounds.left() + 4, y + 10, bounds.left() + 5,
                            y + fit.pitch() - 2, TacticalBoardTheme.EDGE);
                }
                TextFit.draw(graphics, font, step.name(), x, y, bounds.right() - x, nameColor,
                        TextFit.Align.LEFT);
                fitted(graphics, font, step.value(), x, y + 11, bounds.right() - x, valueColor,
                        TextFit.Align.LEFT);
            } else {
                TextFit.draw(graphics, font, step.name(), x, y, Math.min(keyWidth,
                        bounds.right() - x), nameColor, TextFit.Align.LEFT);
                fitted(graphics, font, step.value(), x + keyWidth, y,
                        bounds.right() - x - keyWidth, valueColor, TextFit.Align.LEFT);
            }
            y += fit.pitch();
        }
        UiLayoutProbe.end(graphics);
    }

    // ---- semantic colours ---------------------------------------------------------------------------

    /**
     * Text colour of a value the pages may plan before drawing (the key-value rows of "我的状态"
     * and the vote panel). A plan keeps the ink, never an {@code int}: the theme colours are the
     * viewer's livery only while {@link TacticalScreen} draws, so a colour read in {@code init()}
     * would stay the A palette on a faction-painted board.
     */
    enum Ink {
        TEXT,
        MUTED,
        SUCCESS;

        /** This ink in the palette active now; call while drawing. */
        int color() {
            return switch (this) {
                case TEXT -> TacticalBoardTheme.TEXT;
                case MUTED -> TacticalBoardTheme.MUTED;
                case SUCCESS -> TacticalBoardTheme.SUCCESS;
            };
        }
    }

    // ---- small parts --------------------------------------------------------------------------------

    /** Capacity pips: one 5×3 cell per slot, filled when occupied; returns the x after them. */
    static int pips(GuiGraphics graphics, int x, int y, int used, int capacity, int color,
                    int emptyColor) {
        for (int index = 0; index < capacity; index++) {
            int px = x + index * 6;
            if (index < used) {
                graphics.fill(px, y, px + 5, y + 3, color);
            } else {
                BattleUiTheme.outline(graphics, px, y, px + 5, y + 3, emptyColor);
            }
        }
        return x + capacity * 6;
    }

    /** Panel height for {@code contentHeight} (preview {@code panelH}). */
    static int panelHeight(TacticalShellLayout.Metrics metrics, int contentHeight, boolean titled) {
        return contentHeight + (titled ? metrics.sectionHeight() + 1 : 1) + 2 * metrics.pad();
    }

    /**
     * Opens a solid probe region for a page panel (no-op outside the uiTest probe): texts drawn
     * until {@link #endRegion} must stay inside it and sibling panels must not overlap.
     */
    static void region(GuiGraphics graphics, String id, UiRect rect) {
        UiLayoutProbe.begin(graphics, id, rect.left(), rect.top(), rect.right(), rect.bottom(),
                true);
    }

    /**
     * Opens a non-solid probe region inside a panel (a well, a list, a block): its texts must stay
     * inside it, but it may share space with the panel's own box.
     */
    static void subRegion(GuiGraphics graphics, String id, UiRect rect) {
        UiLayoutProbe.begin(graphics, id, rect.left(), rect.top(), rect.right(), rect.bottom(),
                false);
    }

    static void endRegion(GuiGraphics graphics) {
        UiLayoutProbe.end(graphics);
    }

    /** {@code color} with its alpha replaced. */
    static int alpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    /** Health colour of the board (light panels): green, orange, red. */
    static int boardHealthColor(float ratio) {
        return ratio > 0.55F ? TacticalBoardTheme.SUCCESS : ratio > 0.25F
                ? TacticalBoardTheme.ACCENT : TacticalBoardTheme.DANGER;
    }
}
