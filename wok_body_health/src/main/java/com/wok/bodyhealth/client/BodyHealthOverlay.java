package com.wok.bodyhealth.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wok.bodyhealth.WokBodyHealthMod;
import com.wok.bodyhealth.health.BodyHealthSnapshot;
import com.wok.bodyhealth.health.BodyPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.EnumMap;
import java.util.Map;

public final class BodyHealthOverlay {
    private static final int TEXTURE_SIZE = 256;
    private static final int FIGURE_SIZE = 48;
    private static final int FIGURE_X = 45;
    // Leave a compact strip below the total for WOK步战's optional stamina HUD.
    // The body-health MOD remains fully usable when the core is not installed.
    private static final int BOTTOM_RESERVED = 25;

    private static final ResourceLocation BODY_TEXTURE = texture("body_hud.png");
    private static final Map<BodyPart, ResourceLocation> PART_TEXTURES = createPartTextures();
    private static final Map<BodyPart, ValueAnchor> VALUE_ANCHORS = createValueAnchors();

    public static final IGuiOverlay INSTANCE =
            (gui, graphics, partialTick, width, height) ->
                    render(graphics, height, partialTick);

    private static void render(GuiGraphics graphics, int screenHeight, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        BodyHealthSnapshot snapshot = ClientBodyHealthState.get();
        if (minecraft.player == null
                || minecraft.options.hideGui
                || minecraft.player.isSpectator()
                || snapshot == null) {
            return;
        }

        int figureX = FIGURE_X;
        int figureY = screenHeight - FIGURE_SIZE - BOTTOM_RESERVED;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1.0F, 1.0F, 1.0F, 0.92F);
        blit(graphics, BODY_TEXTURE, figureX, figureY);

        for (BodyPart part : BodyPart.values()) {
            drawInjuryLayer(graphics, minecraft, part, figureX, figureY, partialTick);
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        drawPartValues(graphics, minecraft, snapshot, figureX, figureY, partialTick);
        drawTotal(graphics, minecraft.font, snapshot, figureX, figureY);
    }

    private static void drawPartValues(GuiGraphics graphics, Minecraft minecraft,
                                       BodyHealthSnapshot snapshot, int figureX, int figureY,
                                       float partialTick) {
        Font font = minecraft.font;
        for (BodyPart part : BodyPart.values()) {
            int index = part.ordinal();
            int current = Math.round(snapshot.current()[index]);
            int maximum = Math.round(snapshot.maximum()[index]);
            String text = current + "/" + maximum;
            ValueAnchor anchor = VALUE_ANCHORS.get(part);
            int textX = figureX + anchor.xOffset();
            if (anchor.alignRight()) {
                textX = Math.max(2, textX - font.width(text));
            }
            int textY = figureY + anchor.yOffset();
            int color = healthTextColor(minecraft, part, partialTick);
            if (anchor.showLeader()) {
                drawLeaderArrow(
                        graphics,
                        figureX + anchor.bodyXOffset(),
                        figureY + anchor.bodyYOffset(),
                        anchor.alignRight()
                                ? figureX + anchor.xOffset() + 2
                                : textX - 3,
                        textY + 4,
                        anchor.alignRight(),
                        0xFF858A8D);
            }
            graphics.drawString(
                    font, text, textX, textY,
                    color, true);
        }
    }

    private static void drawLeaderArrow(GuiGraphics graphics,
                                        int startX, int startY,
                                        int endX, int endY,
                                        boolean pointsLeft,
                                        int color) {
        drawLine(graphics, startX, startY, endX, endY, color);
        if (pointsLeft) {
            graphics.fill(endX, endY, endX + 3, endY + 1, color);
            graphics.fill(endX + 1, endY - 1, endX + 3, endY + 2, color);
        } else {
            graphics.fill(endX - 2, endY, endX + 1, endY + 1, color);
            graphics.fill(endX - 2, endY - 1, endX, endY + 2, color);
        }
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

    private static int healthTextColor(Minecraft minecraft, BodyPart part, float partialTick) {
        float ratio = ClientBodyHealthState.ratio(part);
        if (ClientBodyHealthState.hitFlash(part) > 0.0F) {
            return 0xFFFFF1B0;
        }
        if (ratio >= 0.75F) {
            return 0xFFD4D8DA;
        }
        if (ratio >= 0.50F) {
            return 0xFFFFDC26;
        }
        if (ratio >= 0.25F) {
            return 0xFFFF7418;
        }
        if (ratio > 0.0F) {
            return 0xFFFF2420;
        }
        float pulse = 0.5F + 0.5F * Mth.sin(
                (minecraft.player.tickCount + partialTick) * 0.20F);
        int red = 112 + Math.round(pulse * 72.0F);
        return 0xFF000000 | red << 16;
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
            float pulse = 0.5F + 0.5F * Mth.sin(
                    (minecraft.player.tickCount + partialTick) * 0.20F);
            red = 0.32F + pulse * 0.20F;
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

    private static void drawTotal(GuiGraphics graphics, Font font,
                                  BodyHealthSnapshot snapshot, int figureX, int figureY) {
        float current = 0.0F;
        float maximum = 0.0F;
        for (float value : snapshot.current()) {
            current += value;
        }
        for (float value : snapshot.maximum()) {
            maximum += value;
        }

        String text = Math.round(current) + "/" + Math.round(maximum);
        int textX = figureX + (FIGURE_SIZE - font.width(text)) / 2;
        int textY = figureY + FIGURE_SIZE + 1;
        graphics.drawString(font, text, textX, textY, 0xFFD4D8DA, true);
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        graphics.blit(texture, x, y, FIGURE_SIZE, FIGURE_SIZE,
                0.0F, 0.0F, TEXTURE_SIZE, TEXTURE_SIZE,
                TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private static Map<BodyPart, ResourceLocation> createPartTextures() {
        EnumMap<BodyPart, ResourceLocation> textures = new EnumMap<>(BodyPart.class);
        for (BodyPart part : BodyPart.values()) {
            textures.put(part, texture("body_hud_" + part.key() + ".png"));
        }
        return textures;
    }

    private static Map<BodyPart, ValueAnchor> createValueAnchors() {
        EnumMap<BodyPart, ValueAnchor> anchors = new EnumMap<>(BodyPart.class);
        anchors.put(BodyPart.HEAD,
                new ValueAnchor(FIGURE_SIZE + 14, 0, false, 19, 7, true));
        anchors.put(BodyPart.RIGHT_ARM,
                new ValueAnchor(FIGURE_SIZE + 14, 10, false, 32, 17, true));
        anchors.put(BodyPart.CHEST,
                new ValueAnchor(-14, 23, true, 16, 22, true));
        anchors.put(BodyPart.ABDOMEN,
                new ValueAnchor(FIGURE_SIZE + 14, 28, false, 22, 29, true));
        anchors.put(BodyPart.RIGHT_LEG,
                new ValueAnchor(FIGURE_SIZE + 14, 40, false, 26, 40, true));
        anchors.put(BodyPart.LEFT_ARM,
                new ValueAnchor(-14, 5, true, 9, 17, true));
        anchors.put(BodyPart.LEFT_LEG,
                new ValueAnchor(-14, 40, true, 13, 40, true));
        return anchors;
    }

    private static ResourceLocation texture(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(
                WokBodyHealthMod.MOD_ID, "textures/gui/" + fileName);
    }

    private record ValueAnchor(int xOffset, int yOffset, boolean alignRight,
                               int bodyXOffset, int bodyYOffset,
                               boolean showLeader) {
    }

    private BodyHealthOverlay() {
    }
}
