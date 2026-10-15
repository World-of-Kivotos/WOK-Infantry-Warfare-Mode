package com.wok.infantry.mixin;

import com.wok.infantry.client.screen.TacticalTextField;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets {@link TacticalTextField} keep vanilla {@link EditBox} behaviour while following the
 * WOK步战 UI rules: no black vanilla box and border (the field draws its own well), no text
 * shadow on the value, the placeholder, the suggestion or the "_" cursor, and the mid-text cursor
 * bar in the palette's text-on-well colour ({@link TacticalTextField#cursorColor()}) instead of
 * vanilla's fixed light gray, which vanishes in the pale Neutral well. Every other
 * {@link EditBox} takes the original calls unchanged.
 *
 * <p>Names: like the other core mixins this runs without a refmap ({@code remap = false}). The
 * method list carries the official and the SRG name of {@code renderWidget}; the call targets
 * name only the owner and the descriptor, because 1.20.1 production keeps Mojang class names and
 * only the method names differ ({@code drawString}/{@code m_2804xx_}). Within
 * {@code renderWidget} each descriptor belongs to exactly one {@link GuiGraphics} method, which
 * {@code EditBoxShadowMixinTest} checks against the Minecraft build this core compiles against.
 *
 * <p>The redirects are cosmetic, so they use {@code require = 0}: should a later build move these
 * calls, the game keeps running and the tactical field simply looks like a vanilla box.
 * Shadowless text returns the same end x as vanilla's shadowed call ({@code Font} adds 1 for a
 * shadow), so the cursor and the text behind it stay where vanilla puts them.
 */
@Mixin(value = EditBox.class, remap = false)
public abstract class EditBoxShadowMixin {
    @Redirect(method = {"renderWidget", "m_87963_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;(IIIII)V"),
            require = 0, remap = false)
    private void wokInfantry$skipVanillaFrame(GuiGraphics graphics, int left, int top, int right,
                                              int bottom, int color) {
        if (!((Object) this instanceof TacticalTextField)) {
            graphics.fill(left, top, right, bottom, color);
        }
    }

    /** The mid-text cursor bar (vanilla: {@code fill(RenderType.guiOverlay(), …, 0xFFD0D0D0)}). */
    @Redirect(method = {"renderWidget", "m_87963_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;"
                    + "(Lnet/minecraft/client/renderer/RenderType;IIIII)V"),
            require = 0, remap = false)
    private void wokInfantry$recolorCursor(GuiGraphics graphics, RenderType type, int left,
                                           int top, int right, int bottom, int color) {
        graphics.fill(type, left, top, right, bottom,
                (Object) this instanceof TacticalTextField field ? field.cursorColor() : color);
    }

    @Redirect(method = {"renderWidget", "m_87963_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;"
                    + "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"),
            require = 0, remap = false)
    private int wokInfantry$drawSequence(GuiGraphics graphics, Font font,
                                         FormattedCharSequence text, int x, int y, int color) {
        if (!((Object) this instanceof TacticalTextField)) {
            return graphics.drawString(font, text, x, y, color);
        }
        return graphics.drawString(font, text, x, y, color, false) + 1;
    }

    @Redirect(method = {"renderWidget", "m_87963_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;"
                    + "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I"),
            require = 0, remap = false)
    private int wokInfantry$drawComponent(GuiGraphics graphics, Font font, Component text, int x,
                                          int y, int color) {
        if (!((Object) this instanceof TacticalTextField)) {
            return graphics.drawString(font, text, x, y, color);
        }
        return graphics.drawString(font, text, x, y, color, false) + 1;
    }

    @Redirect(method = {"renderWidget", "m_87963_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;"
                    + "(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"),
            require = 0, remap = false)
    private int wokInfantry$drawString(GuiGraphics graphics, Font font, String text, int x, int y,
                                       int color) {
        if (!((Object) this instanceof TacticalTextField)) {
            return graphics.drawString(font, text, x, y, color);
        }
        return text == null ? 0 : graphics.drawString(font, text, x, y, color, false) + 1;
    }
}
