package com.wok.infantry.client.hud;

import com.wok.infantry.client.ClientStaminaState;
import com.wok.infantry.client.StaminaTrend;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.TacticalIcon;
import com.wok.infantry.client.screen.UiRect;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import com.wok.infantry.stamina.StaminaSnapshot;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.util.Locale;

/**
 * The split stamina bar (arms 手, legs 腿) in the vanilla experience row above the hotbar —
 * preview {@code surfaces/16-stamina.js}, {@code stamina-a4} "战术平板凹槽", chosen 2026-10-05:
 * the core HUD's plate language (translucent {@link TacticalBoardTheme#HUD_PLATE}, 1px
 * {@link TacticalBoardTheme#HUD_EDGE} around the whole non-rectangular plate), two recessed
 * grooves with the arms tick at 50 % and the legs tick at 15 %, ears with hand / boot silhouettes,
 * a 2px status accent on each ear's outer side and, from 640 layout pixels wide, the percentages
 * (no shadow). Geometry in {@link StaminaBarLayout}, state colours in {@link StaminaBarModel}.
 *
 * <p>Same place with or without WOK步战附属-部位血量: the core no longer draws into that add-on's
 * companion strip, nor into the bottom-left vitals slot. While the bar is shown (stamina on,
 * survival or adventure, alive, F1 off, client option {@code hud.staminaBar} on) the vanilla
 * experience bar and level and the mount jump bar are cancelled ({@link #onOverlayPre}); in
 * creative or spectator mode, or with stamina off, vanilla draws them as usual. Neither overlay
 * changes Forge's {@code leftHeight} / {@code rightHeight}, so no status row and no other MOD's
 * overlay moves.
 */
public final class StaminaHudOverlay {
    /** Overlay id {@code wok_infantry:stamina}; kept so packs can hide it by id. */
    public static final String ID = "stamina";
    /** Layout-probe boxes of the plate's pieces (uiTest only). */
    public static final String BAND_BOX = "hud.stamina.band";
    public static final String EAR_LEFT_BOX = "hud.stamina.ear_left";
    public static final String EAR_RIGHT_BOX = "hud.stamina.ear_right";
    public static final String TAB_BOX = "hud.stamina.tab";
    /** Prefix of the probe note summarising a drawn bar (uiTest only). */
    public static final String PROBE_NOTE = "stamina ";
    /** Short pool names ("手" / "腿"), kept for packs and add-ons that label the pools. */
    public static final String ARMS_SHORT_KEY = "hud.wok_infantry.stamina.arms_short";
    public static final String LEGS_SHORT_KEY = "hud.wok_infantry.stamina.legs_short";

    public static final IGuiOverlay INSTANCE =
            (gui, graphics, partialTick, width, height) -> render(graphics, width, height);

    private static volatile StaminaBarModel.State pinned;

    private StaminaHudOverlay() {
    }

    /** Drawn right after the vanilla experience bar, whose row it takes. */
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.EXPERIENCE_BAR.id(), ID, INSTANCE);
    }

    /**
     * Forge bus, highest priority, cancelled events skipped: while the bar is shown, cancels the
     * vanilla experience bar (with its level) and the mount jump bar. Cancelling first means no
     * other MOD's listener starts work on these two overlays that would need their Post event.
     * An open WOK步战 terminal hides the bar but keeps the vanilla row cancelled (only F1 brings
     * vanilla back), so nothing flickers into that row under the device.
     */
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == null || !replacesVanillaOverlay(event.getOverlay().id())) {
            return;
        }
        Window window = event.getWindow();
        HudFrame frame = HudFrame.current(window.getGuiScaledWidth(),
                window.getGuiScaledHeight());
        if (frame != null && !frame.hidden() && frame.staminaShown()) {
            event.setCanceled(true);
        }
    }

    /** The vanilla overlays whose row the bar takes: the experience bar and the jump bar. */
    public static boolean replacesVanillaOverlay(ResourceLocation id) {
        return VanillaGuiOverlay.EXPERIENCE_BAR.id().equals(id)
                || VanillaGuiOverlay.JUMP_BAR.id().equals(id);
    }

    /**
     * UI acceptance only: shows {@code state} (bright blink phase) instead of the live values,
     * so a capture never depends on network timing; {@code null} returns to the live values.
     */
    public static void pinForAcceptance(StaminaBarModel.State state) {
        pinned = state;
    }

    private static void render(GuiGraphics graphics, int width, int height) {
        HudFrame frame = HudFrame.current(width, height);
        if (frame == null || frame.coreHidden() || !frame.staminaShown()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        StaminaBarModel.State fixed = pinned;
        StaminaBarModel.State state = fixed != null ? fixed
                : liveState(minecraft.player, frame.stamina());
        boolean blinkOn = fixed != null || minecraft.player == null
                || StaminaBarModel.blinkOn(minecraft.player.tickCount);
        draw(graphics, minecraft.font, frame.layout().staminaBar(), state, blinkOn);
    }

    /** This frame's state from the server snapshot, its recent trend and the player's mount. */
    static StaminaBarModel.State liveState(LocalPlayer player, StaminaSnapshot snapshot) {
        StaminaTrend.View trend = ClientStaminaState.trend();
        StaminaBarModel.Mount mount = StaminaBarModel.Mount.NONE;
        float charge = 0.0F;
        if (player != null) {
            PlayerRideableJumping jumpable = player.jumpableVehicle();
            if (jumpable != null) {
                mount = jumpable.getJumpCooldown() > 0 ? StaminaBarModel.Mount.JUMP_COOLDOWN
                        : StaminaBarModel.Mount.JUMP;
                charge = player.getJumpRidingScale();
            } else if (player.isPassenger()) {
                mount = StaminaBarModel.Mount.SEATED;
            }
        }
        return new StaminaBarModel.State(
                new StaminaBarModel.Pool(snapshot.arms(), trend.armsGhost(), trend.armsRising()),
                new StaminaBarModel.Pool(snapshot.legs(), trend.legsGhost(), trend.legsRising()),
                snapshot.sprintBlocked(), trend.unlocked(), mount, charge);
    }

    static void draw(GuiGraphics graphics, Font font, StaminaBarLayout.Layout bar,
                     StaminaBarModel.State state, boolean blinkOn) {
        StaminaBarModel.Tone arms = StaminaBarModel.arms(state, blinkOn);
        StaminaBarModel.Tone legs = StaminaBarModel.legs(state, blinkOn);
        // Plate: each piece filled once (translucent, the pieces only touch), one outline around
        // the union, the status accents on top of it (HUD plate language). The pieces are filled
        // one by one instead of through Layout.pieces(), which builds a list.
        fill(graphics, bar.band(), TacticalBoardTheme.HUD_PLATE);
        fill(graphics, bar.earLeft(), TacticalBoardTheme.HUD_PLATE);
        fill(graphics, bar.earRight(), TacticalBoardTheme.HUD_PLATE);
        fill(graphics, bar.tab(), TacticalBoardTheme.HUD_PLATE);
        for (UiRect segment : bar.outline()) {
            fill(graphics, segment, TacticalBoardTheme.HUD_EDGE);
        }
        fill(graphics, bar.armsAccent(), arms.accent());
        fill(graphics, bar.legsAccent(), legs.accent());
        groove(graphics, bar.arms(), arms, false, StaminaBarLayout.ARMS_TICK);
        groove(graphics, bar.legs(), legs, true, StaminaBarLayout.LEGS_TICK);
        fill(graphics, bar.separator(), TacticalBoardTheme.HUD_EDGE);

        UiLayoutProbe.box(graphics, BAND_BOX, bar.band().left(), bar.band().top(),
                bar.band().right(), bar.band().bottom());
        if (bar.tab() != null) {
            UiLayoutProbe.box(graphics, TAB_BOX, bar.tab().left(), bar.tab().top(),
                    bar.tab().right(), bar.tab().bottom());
        }
        icon(graphics, bar.armsIcon(), arms.icon(), arms.iconColor());
        icon(graphics, bar.legsIcon(), legs.icon(), legs.iconColor());
        HudPaint.probeBox(graphics, EAR_LEFT_BOX, bar.earLeft());
        if (bar.armsNumber() != null) {
            number(graphics, font, bar.armsNumber(), arms.percent(), arms.numberColor(), true,
                    bar.earScale());
        }
        UiLayoutProbe.end(graphics);
        if (bar.earRight() != null) {
            HudPaint.probeBox(graphics, EAR_RIGHT_BOX, bar.earRight());
            if (bar.legsNumber() != null) {
                number(graphics, font, bar.legsNumber(), legs.percent(), legs.numberColor(),
                        false, bar.earScale());
            }
            UiLayoutProbe.end(graphics);
        }
        if (UiLayoutProbe.recording()) {
            UiLayoutProbe.note(PROBE_NOTE + summary("arms", arms) + " " + summary("legs", legs)
                    + " mount=" + state.mount() + " numbers=" + bar.numbers() + " earScale="
                    + bar.earScale() + " narrow=" + bar.narrow());
        }
    }

    /** {@code legs=0%,lock,accent=FFE8695D,fill=FFE8695D,ghost=5%} for the acceptance note. */
    static String summary(String name, StaminaBarModel.Tone tone) {
        return name + "=" + tone.percent() + "," + tone.icon().id() + ",accent="
                + hex(tone.accent()) + ",fill=" + hex(tone.fill()) + ",ghost="
                + StaminaBarModel.percent(tone.pool().ghost()) + ",rising="
                + tone.pool().rising() + ",track=" + hex(tone.track());
    }

    private static String hex(int argb) {
        return String.format(Locale.ROOT, "%08X", argb);
    }

    /**
     * One groove: track, 1px inner shadow on its top row, the just-used remnant, the fill from
     * the anchored end, a light head while recovering, and the threshold tick in the rows above
     * and below.
     */
    private static void groove(GuiGraphics graphics, UiRect groove, StaminaBarModel.Tone tone,
                               boolean fromRight, float tick) {
        if (groove.isEmpty()) {
            return;
        }
        fill(graphics, groove, tone.track());
        graphics.fill(groove.left(), groove.top(), groove.right(), groove.top() + 1,
                tone.shadow());
        StaminaBarModel.Spans spans = StaminaBarModel.spans(groove.width(), tone.pool());
        span(graphics, groove, spans.fill(), spans.ghost(), tone.ghostColor(), fromRight);
        span(graphics, groove, 0, spans.fill(), tone.fill(), fromRight);
        if (spans.head()) {
            span(graphics, groove, spans.fill() - 1, spans.fill(), TacticalBoardTheme.LIGHT,
                    fromRight);
        }
        if (tone.tick()) {
            int offset = StaminaBarModel.tickOffset(groove.width(), tick);
            int x = fromRight ? groove.right() - 1 - offset : groove.left() + offset;
            graphics.fill(x, groove.top() - 1, x + 1, groove.top(), tone.tickColor());
            graphics.fill(x, groove.bottom(), x + 1, groove.bottom() + 1, tone.tickColor());
        }
    }

    /** Columns [from, to) of a groove counted from its anchored end. */
    private static void span(GuiGraphics graphics, UiRect groove, int from, int to, int color,
                             boolean fromRight) {
        if (to <= from) {
            return;
        }
        if (fromRight) {
            graphics.fill(groove.right() - to, groove.top(), groove.right() - from,
                    groove.bottom(), color);
        } else {
            graphics.fill(groove.left() + from, groove.top(), groove.left() + to,
                    groove.bottom(), color);
        }
    }

    /** A 9×9 icon scaled to fill {@code square} (9 or 18 GUI pixels). */
    private static void icon(GuiGraphics graphics, UiRect square, TacticalIcon icon, int color) {
        if (square == null || icon == null) {
            return;
        }
        int scale = Math.max(1, square.width() / TacticalIcon.SIZE);
        graphics.pose().pushPose();
        graphics.pose().translate(square.left(), square.top(), 0.0F);
        if (scale > 1) {
            graphics.pose().scale(scale, scale, 1.0F);
        }
        icon.draw(graphics, 0, 0, color);
        graphics.pose().popPose();
    }

    /**
     * Percentage in {@code slot} at the ear scale, no shadow: right-aligned (its last glyph ends
     * at the slot's right edge, the font's trailing pixel outside) or left-aligned.
     */
    private static void number(GuiGraphics graphics, Font font, UiRect slot, String text,
                               int color, boolean rightAligned, int scale) {
        int x = rightAligned ? slot.right() - (font.width(text) - 1) * scale : slot.left();
        graphics.pose().pushPose();
        graphics.pose().translate(x, slot.top(), 0.0F);
        if (scale > 1) {
            graphics.pose().scale(scale, scale, 1.0F);
        }
        TacticalHud.readout(graphics, font, text, 0, 0, color);
        graphics.pose().popPose();
    }

    private static void fill(GuiGraphics graphics, UiRect rect, int color) {
        if (rect != null && !rect.isEmpty()) {
            graphics.fill(rect.left(), rect.top(), rect.right(), rect.bottom(), color);
        }
    }
}
