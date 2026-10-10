package com.wok.infantry.client.screen;

import com.wok.infantry.client.hud.TacticalHud;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Faction key of the formation vote strip. It only changes which faction is browsed; joining is a
 * separate confirmed key. The four looks keep "looked at" and "joined" apart (user report 1), in
 * the selection colour of the tablet's livery (P3: the Academy's blue, Caesar's crimson, Neutral
 * graphite before joining):
 *
 * <ul>
 *   <li>{@link FormationVoteModel.FactionLook#BROWSING} (not joined yet): selection-colour
 *   outline and left bar ({@link TacticalButtonStyle#barWidth}, 3px from 16px tall) on a light
 *   wash with an eye icon. Never solid, so it cannot be mistaken for a joined faction; this is
 *   where the page deliberately differs from the P3 preview, which draws the browsed faction
 *   solid.</li>
 *   <li>{@link FormationVoteModel.FactionLook#JOINED}: the solid "current" key of
 *   {@link TacticalButtonStyle} (selection fill, light bar, no bevel) with a check; its count
 *   sits on the dark inset in {@code ON_SELECT}.</li>
 *   <li>{@link FormationVoteModel.FactionLook#OTHER}: disabled key (gray, hatched) with a lock
 *   and the light count inset; the tooltip says why the faction cannot be changed.</li>
 *   <li>{@link FormationVoteModel.FactionLook#NORMAL}: a plain raised key.</li>
 * </ul>
 * Every look carries the population count ("18/40"). Colours are read while the key is drawn, so
 * they follow the palette of the frame.
 */
final class FormationFactionButton extends Button {
    private final FormationVoteModel.FactionLook look;
    private final Component badge;
    private final TruncationTooltip truncationTooltip = new TruncationTooltip();

    FormationFactionButton(int x, int y, int width, int height, Component label,
                           Component badge, FormationVoteModel.FactionLook look,
                           OnPress onPress) {
        super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        this.look = look;
        this.badge = badge;
        this.active = look == FormationVoteModel.FactionLook.BROWSING
                || look == FormationVoteModel.FactionLook.NORMAL;
    }

    FormationVoteModel.FactionLook look() {
        return look;
    }

    /** Fill of the browsing look: a light wash of the selection bar over the key colour. */
    static int browsingFill(boolean hovered) {
        return TacticalHud.mix(TacticalBoardTheme.CARD, TacticalBoardTheme.SELECT_BAR,
                hovered ? 0.42D : 0.3D);
    }

    /**
     * Label padding of the browsing look on a key {@code height} tall: clear of its selection
     * bar like a selected key's label ({@link TacticalButtonStyle#labelPadLeft}).
     */
    static int browsingPadLeft(int height) {
        return TacticalButtonStyle.barWidth(height) + 3;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int left = getX();
        int top = getY();
        int right = left + width;
        int bottom = top + height;
        boolean focus = TacticalButtonStyle.keyboardFocused(this);
        TextFit.Fitted fitted;
        String probeState;
        if (look == FormationVoteModel.FactionLook.BROWSING) {
            fitted = renderBrowsing(graphics, font, left, top, right, bottom,
                    TacticalButtonStyle.hovered(this), focus);
            probeState = "BROWSING";
        } else {
            TacticalButtonStyle.Look style = switch (look) {
                case JOINED -> new TacticalButtonStyle.Look(TacticalButtonStyle.State.CURRENT,
                        false);
                case OTHER -> new TacticalButtonStyle.Look(TacticalButtonStyle.State.DISABLED,
                        false);
                default -> TacticalButtonStyle.resolve(active, false, false,
                        TacticalButtonStyle.Variant.NORMAL, TacticalButtonStyle.hovered(this),
                        false);
            };
            TacticalIcon icon = look == FormationVoteModel.FactionLook.JOINED ? TacticalIcon.CHECK
                    : look == FormationVoteModel.FactionLook.OTHER ? TacticalIcon.LOCK : null;
            fitted = TacticalButtonStyle.render(graphics, font, left, top, right, bottom,
                    getMessage(), style, new TacticalButtonStyle.Options(TextFit.Align.LEFT,
                            badge, TacticalBoardTheme.MUTED, focus, icon, false));
            probeState = style.state().name();
        }
        truncationTooltip.sync(this, getMessage(), fitted.truncated());
        UiLayoutProbe.widget(graphics, this, "button", probeState, fitted.truncated(), focus);
    }

    private TextFit.Fitted renderBrowsing(GuiGraphics graphics, Font font, int left, int top,
                                          int right, int bottom, boolean hovered,
                                          boolean focus) {
        graphics.fill(left, top, right, bottom, browsingFill(hovered));
        BattleUiTheme.outline(graphics, left, top, right, bottom, TacticalBoardTheme.SELECT);
        graphics.fill(left + 1, top + 1, Math.min(right - 1,
                left + 1 + TacticalButtonStyle.barWidth(bottom - top)), bottom - 1,
                TacticalBoardTheme.SELECT);
        if (focus) {
            TacticalButtonStyle.focusRing(graphics, left, top, right, bottom);
        }
        int padLeft = browsingPadLeft(bottom - top);
        int textY = top + Math.max(0, (bottom - top - 8) / 2);
        int badgeWidth = 0;
        if (badge != null && !badge.getString().isEmpty()) {
            badgeWidth = font.width(badge) + 6;
            int badgeLeft = right - TacticalButtonStyle.LABEL_PAD_RIGHT - badgeWidth;
            graphics.fill(badgeLeft, textY - 1, badgeLeft + badgeWidth, textY + 8,
                    TacticalBoardTheme.BADGE_ON_CARD);
            graphics.drawString(font, badge, badgeLeft + 3, textY, TacticalBoardTheme.SELECT_EDGE,
                    false);
        }
        int room = TacticalButtonStyle.labelRoom(left, right, padLeft, true, badgeWidth);
        TextFit.Fitted fitted = TextFit.fit(font, getMessage(), room);
        TacticalButtonStyle.Content content = TacticalButtonStyle.content(left, top, right, bottom,
                padLeft, true, fitted.width(), badgeWidth, TextFit.Align.LEFT);
        TacticalIcon.EYE.draw(graphics, content.iconX(), content.iconY(),
                TacticalBoardTheme.SELECT);
        return TextFit.drawFitted(graphics, font, fitted, content.textX(), textY, fitted.width(),
                TacticalBoardTheme.TEXT, TextFit.Align.LEFT);
    }
}
