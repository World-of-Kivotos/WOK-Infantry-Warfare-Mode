package com.wok.bodyhealth.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wok.bodyhealth.WokBodyHealthMod;
import com.wok.bodyhealth.client.BodyHealthHudLayout.Layout;
import com.wok.bodyhealth.client.BodyHealthHudLayout.Tier;
import com.wok.bodyhealth.health.BodyHealthSnapshot;
import com.wok.bodyhealth.health.BodyPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.EnumMap;
import java.util.Map;

public final class BodyHealthOverlay {
    private static final int FIGURE_WIDTH = BodyHealthHudLayout.FIGURE_WIDTH;
    private static final int FIGURE_HEIGHT = BodyHealthHudLayout.FIGURE_HEIGHT;
    // Tactical-board chip colours shared with WOK步战核心's stamina panel.
    private static final int CHIP_FILL = 0xB5121A20;
    private static final int CHIP_BORDER = 0xCC4C5E6B;
    private static final int LEADER = 0xFF858A8D;
    private static final int NEUTRAL_TEXT = 0xFFD4D8DA;
    private static final int DESTROYED_TEXT = 0xFFFF5A4E;

    private static final ResourceLocation BODY_TEXTURE = texture("body_hud.png");
    private static final Map<BodyPart, ResourceLocation> PART_TEXTURES = createPartTextures();
    private static final Map<BodyPart, ValueAnchor> VALUE_ANCHORS = createValueAnchors();

    public static final IGuiOverlay INSTANCE =
            (gui, graphics, partialTick, width, height) ->
                    render(gui.getMinecraft(), graphics, width, height, partialTick);

    /** The layout this frame, or null when the HUD is hidden; also used by companion HUDs. */
    public static Layout currentLayout(Minecraft minecraft, int screenWidth, int screenHeight) {
        BodyHealthSnapshot snapshot = ClientBodyHealthState.get();
        if (minecraft.player == null
                || minecraft.options.hideGui
                || minecraft.player.isSpectator()
                || snapshot == null
                || !drawsSurvivalElements(minecraft)) {
            return null;
        }
        return layout(minecraft.font, snapshot, screenWidth, screenHeight);
    }

    private static void render(Minecraft minecraft, GuiGraphics graphics,
                               int screenWidth, int screenHeight, float partialTick) {
        Layout layout = currentLayout(minecraft, screenWidth, screenHeight);
        BodyHealthSnapshot snapshot = ClientBodyHealthState.get();
        if (layout == null || snapshot == null) {
            return;
        }

        int figureX = layout.figureX();
        int figureY = layout.figureY();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1.0F, 1.0F, 1.0F, 0.92F);
        blit(graphics, BODY_TEXTURE, figureX, figureY);
        for (BodyPart part : BodyPart.values()) {
            drawInjuryLayer(graphics, minecraft, part, figureX, figureY, partialTick);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        float pulse = pulse(minecraft, partialTick);
        if (layout.tier() != Tier.COMPACT) {
            drawPartValues(graphics, minecraft.font, snapshot, layout, pulse);
        }
        drawTotal(graphics, minecraft.font, snapshot, layout);
    }

    private static void drawPartValues(GuiGraphics graphics, Font font,
                                       BodyHealthSnapshot snapshot, Layout layout, float pulse) {
        int gap = layout.tier().labelGap();
        for (BodyPart part : BodyPart.values()) {
            int index = part.ordinal();
            int current = Math.round(snapshot.current()[index]);
            String text = layout.tier() == Tier.FULL
                    ? current + "/" + Math.round(snapshot.maximum()[index])
                    : Integer.toString(current);
            ValueAnchor anchor = VALUE_ANCHORS.get(part);
            int textY = layout.figureY() + anchor.textY();
            int textX;
            int leaderEndX;
            if (anchor.rightSide()) {
                int chipLeft = layout.figureX() + FIGURE_WIDTH + gap;
                textX = chipLeft + 2;
                leaderEndX = chipLeft - 1;
            } else {
                int chipRight = layout.figureX() - gap;
                textX = chipRight - 1 - font.width(text);
                leaderEndX = chipRight;
            }
            int bodyX = layout.figureX() + anchor.bodyX();
            int bodyY = layout.figureY() + anchor.bodyY();
            drawLine(graphics, bodyX, bodyY, leaderEndX, textY + 3, LEADER);
            graphics.fill(bodyX - 1, bodyY - 1, bodyX + 1, bodyY + 1, LEADER);
            boolean destroyed = snapshot.current()[index] <= 0.0001F;
            drawChip(graphics, font, text, textX, textY,
                    destroyed ? DESTROYED_TEXT : healthTextColor(part),
                    destroyed ? destroyedBorder(pulse) : CHIP_BORDER);
        }
    }

    private static void drawTotal(GuiGraphics graphics, Font font,
                                  BodyHealthSnapshot snapshot, Layout layout) {
        float current = 0.0F;
        float maximum = 0.0F;
        for (int index = 0; index < snapshot.current().length; index++) {
            current += snapshot.current()[index];
            maximum += snapshot.maximum()[index];
        }

        String text = Math.round(current) + "/" + Math.round(maximum);
        int chipLeft = layout.centredChipLeft(font.width(text) + BodyHealthHudLayout.CHIP_PADDING);
        if (chipLeft < 0) {
            text = Integer.toString(Math.round(current));
            chipLeft = layout.centredChipLeft(font.width(text) + BodyHealthHudLayout.CHIP_PADDING);
            if (chipLeft < 0) {
                return;
            }
        }
        drawChip(graphics, font, text, chipLeft + 2, layout.figureY() + FIGURE_HEIGHT + 1,
                NEUTRAL_TEXT, CHIP_BORDER);
    }

    /** A dark plate behind the text replaces the default shadow for contrast on bright scenes. */
    private static void drawChip(GuiGraphics graphics, Font font, String text,
                                 int textX, int textY, int textColor, int borderColor) {
        int left = textX - 2;
        int right = textX + font.width(text) + 1;
        int top = textY - 1;
        int bottom = textY + 8;
        graphics.fill(left, top, right, bottom, CHIP_FILL);
        graphics.fill(left, top, right, top + 1, borderColor);
        graphics.fill(left, bottom - 1, right, bottom, borderColor);
        graphics.fill(left, top + 1, left + 1, bottom - 1, borderColor);
        graphics.fill(right - 1, top + 1, right, bottom - 1, borderColor);
        graphics.drawString(font, text, textX, textY, textColor, false);
    }

    private static void drawLine(GuiGraphics graphics,
                                 int startX, int startY,
                                 int endX, int endY,
                                 int color) {
        int x = startX;
        int y = startY;
        int deltaX = Math.abs(endX - startX);
        int stepX = startX < endX ? 1 : -1;
        int deltaY = -Math.abs(endY - startY);
        int stepY = startY < endY ? 1 : -1;
        int error = deltaX + deltaY;
        while (true) {
            graphics.fill(x, y, x + 1, y + 1, color);
            if (x == endX && y == endY) {
                return;
            }
            int doubledError = error * 2;
            if (doubledError >= deltaY) {
                error += deltaY;
                x += stepX;
            }
            if (doubledError <= deltaX) {
                error += deltaX;
                y += stepY;
            }
        }
    }

    private static int healthTextColor(BodyPart part) {
        float ratio = ClientBodyHealthState.ratio(part);
        if (ClientBodyHealthState.hitFlash(part) > 0.0F) {
            return 0xFFFFF1B0;
        }
        if (ratio >= 0.75F) {
            return NEUTRAL_TEXT;
        }
        if (ratio >= 0.50F) {
            return 0xFFFFDC26;
        }
        if (ratio >= 0.25F) {
            return 0xFFFF7418;
        }
        return 0xFFFF2420;
    }

    private static int destroyedBorder(float pulse) {
        int red = 122 + Math.round(pulse * 102.0F);
        int green = 30 + Math.round(pulse * 38.0F);
        int blue = 26 + Math.round(pulse * 32.0F);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static float pulse(Minecraft minecraft, float partialTick) {
        return 0.5F + 0.5F * Mth.sin((minecraft.player.tickCount + partialTick) * 0.20F);
    }

    private static void drawInjuryLayer(GuiGraphics graphics, Minecraft minecraft,
                                        BodyPart part, int x, int y, float partialTick) {
        float ratio = ClientBodyHealthState.ratio(part);
        float hitFlash = ClientBodyHealthState.hitFlash(part);
        if (ratio >= 0.75F && hitFlash <= 0.0F) {
            return;
        }

        float red;
        float green;
        float blue;
        float alpha;
        if (ratio >= 0.75F) {
            red = 1.0F;
            green = 0.34F;
            blue = 0.12F;
            alpha = 0.92F;
        } else if (ratio >= 0.50F) {
            red = 1.0F;
            green = 0.86F;
            blue = 0.05F;
            alpha = 0.96F;
        } else if (ratio >= 0.25F) {
            red = 1.0F;
            green = 0.38F;
            blue = 0.015F;
            alpha = 0.98F;
        } else if (ratio > 0.0F) {
            red = 1.0F;
            green = 0.025F;
            blue = 0.01F;
            alpha = 1.0F;
        } else {
            red = 0.32F + pulse(minecraft, partialTick) * 0.20F;
            green = 0.0F;
            blue = 0.0F;
            alpha = 1.0F;
        }

        if (hitFlash > 0.0F) {
            // A short white-hot flash makes the newly hit region readable in
            // peripheral vision, then it settles into its health-state color.
            float flashStrength = hitFlash * hitFlash;
            red = Mth.lerp(flashStrength, red, 1.0F);
            green = Mth.lerp(flashStrength, green, 1.0F);
            blue = Mth.lerp(flashStrength, blue, 0.82F);
            alpha = 1.0F;
        }

        graphics.setColor(red, green, blue, alpha);
        blit(graphics, PART_TEXTURES.get(part), x, y);
    }

    static Layout layout(Font font, BodyHealthSnapshot snapshot, int screenWidth, int screenHeight) {
        int fullWidth = 0;
        int shortWidth = 0;
        for (float value : snapshot.maximum()) {
            String maximum = Integer.toString(Math.round(value));
            fullWidth = Math.max(fullWidth, font.width(maximum + "/" + maximum));
            shortWidth = Math.max(shortWidth, font.width(maximum));
        }
        return BodyHealthHudLayout.compute(screenWidth, screenHeight, fullWidth, shortWidth);
    }

    /** Same rule as ForgeGui#shouldDrawSurvivalElements, usable outside an overlay callback. */
    private static boolean drawsSurvivalElements(Minecraft minecraft) {
        return minecraft.gameMode != null
                && minecraft.gameMode.canHurtPlayer()
                && minecraft.getCameraEntity() instanceof Player;
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        graphics.blit(texture, x, y, FIGURE_WIDTH, FIGURE_HEIGHT,
                0.0F, 0.0F, FIGURE_WIDTH, FIGURE_HEIGHT,
                FIGURE_WIDTH, FIGURE_HEIGHT);
    }

    private static Map<BodyPart, ResourceLocation> createPartTextures() {
        EnumMap<BodyPart, ResourceLocation> textures = new EnumMap<>(BodyPart.class);
        for (BodyPart part : BodyPart.values()) {
            textures.put(part, texture("body_hud_" + part.key() + ".png"));
        }
        return textures;
    }

    /**
     * Label rows and leader anchors for the 39x48 rifle-holding figure generated
     * by tools/body_health_hud_icon.ps1. It is a front view, so the player's right
     * arm and leg are on screen left and their labels sit in the left column.
     * Every anchor's 2x2 node lies inside its part's mask.
     */
    private static Map<BodyPart, ValueAnchor> createValueAnchors() {
        EnumMap<BodyPart, ValueAnchor> anchors = new EnumMap<>(BodyPart.class);
        anchors.put(BodyPart.HEAD, new ValueAnchor(false, 0, 8, 4));
        anchors.put(BodyPart.RIGHT_ARM, new ValueAnchor(false, 10, 4, 15));
        anchors.put(BodyPart.CHEST, new ValueAnchor(false, 21, 9, 23));
        anchors.put(BodyPart.RIGHT_LEG, new ValueAnchor(false, 39, 7, 40));
        anchors.put(BodyPart.LEFT_ARM, new ValueAnchor(true, 13, 25, 17));
        anchors.put(BodyPart.ABDOMEN, new ValueAnchor(true, 25, 15, 28));
        anchors.put(BodyPart.LEFT_LEG, new ValueAnchor(true, 38, 18, 40));
        return anchors;
    }

    private static ResourceLocation texture(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(
                WokBodyHealthMod.MOD_ID, "textures/gui/" + fileName);
    }

    private record ValueAnchor(boolean rightSide, int textY, int bodyX, int bodyY) {
    }

    private BodyHealthOverlay() {
    }
}
