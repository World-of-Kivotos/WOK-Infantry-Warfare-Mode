package com.wok.infantry.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.integration.tacz.TaczLoadoutAdapter;
import com.wok.infantry.loadout.LoadoutEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Resolves the native TaCZ HUD silhouette for a configured loadout entry. */
final class LoadoutEntryPreview {
    static final int TACZ_HUD_WIDTH = 39;
    static final int TACZ_HUD_HEIGHT = 13;

    private static final String TACZ_MOD_ID = "tacz";
    private static final String TACZ_GUN_ITEM_ID = "tacz:modern_kinetic_gun";
    private static final String TACZ_API_CLASS = "com.tacz.guns.api.TimelessAPI";
    private static final int MAX_CACHE_ENTRIES = 256;
    private static final Map<PreviewKey, Preview> CACHE = new LinkedHashMap<>(32, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<PreviewKey, Preview> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };
    private static boolean reflectionFailureLogged;

    private LoadoutEntryPreview() {
    }

    static Preview resolve(LoadoutEntry entry) {
        if (entry == null) {
            return Preview.EMPTY;
        }
        PreviewKey key = new PreviewKey(entry.itemId(), entry.count(), entry.snbt());
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(key, LoadoutEntryPreview::createPreview);
        }
    }

    static boolean render(GuiGraphics graphics, Preview preview,
                          int x, int y, int maxWidth, int maxHeight) {
        if (graphics == null || preview == null || maxWidth <= 0 || maxHeight <= 0) {
            return false;
        }
        if (preview.hudTexture() != null) {
            float scale = Math.min(maxWidth / (float) TACZ_HUD_WIDTH,
                    maxHeight / (float) TACZ_HUD_HEIGHT);
            int drawWidth = Math.max(1, Math.round(TACZ_HUD_WIDTH * scale));
            int drawHeight = Math.max(1, Math.round(TACZ_HUD_HEIGHT * scale));
            int drawX = x + (maxWidth - drawWidth) / 2;
            int drawY = y + (maxHeight - drawHeight) / 2;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.blit(preview.hudTexture(), drawX, drawY, drawWidth, drawHeight,
                    0.0F, 0.0F, TACZ_HUD_WIDTH, TACZ_HUD_HEIGHT,
                    TACZ_HUD_WIDTH, TACZ_HUD_HEIGHT);
            RenderSystem.disableBlend();
            return true;
        }
        if (!preview.stack().isEmpty()) {
            float scale = Math.min(1.0F,
                    Math.min(maxWidth / 16.0F, maxHeight / 16.0F));
            int drawSize = Math.max(1, Math.round(16.0F * scale));
            int drawX = x + (maxWidth - drawSize) / 2;
            int drawY = y + (maxHeight - drawSize) / 2;
            graphics.pose().pushPose();
            graphics.pose().translate(drawX, drawY, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.renderItem(preview.stack(), 0, 0);
            graphics.pose().popPose();
            return true;
        }
        return false;
    }

    private static Preview createPreview(PreviewKey key) {
        CompoundTag configuredTag;
        try {
            configuredTag = key.snbt().isBlank() ? null : TagParser.parseTag(key.snbt());
        } catch (CommandSyntaxException exception) {
            return Preview.EMPTY;
        }

        ItemStack stack;
        if (TACZ_GUN_ITEM_ID.equals(key.itemId())) {
            LoadoutEntry entry = new LoadoutEntry("preview", "preview", key.itemId(),
                    key.count(), key.snbt());
            stack = TaczLoadoutAdapter.createGun(entry, configuredTag)
                    .orElse(ItemStack.EMPTY);
        } else {
            ResourceLocation itemId = ResourceLocation.tryParse(key.itemId());
            Item item = itemId == null ? null : ForgeRegistries.ITEMS.getValue(itemId);
            stack = item == null ? ItemStack.EMPTY : new ItemStack(item, key.count());
            if (!stack.isEmpty() && configuredTag != null) {
                stack.setTag(configuredTag.copy());
            }
        }

        ResourceLocation hudTexture = resolveTaczHudTexture(stack);
        return new Preview(stack, hudTexture);
    }

    private static ResourceLocation resolveTaczHudTexture(ItemStack stack) {
        if (stack.isEmpty() || !ModList.get().isLoaded(TACZ_MOD_ID)) {
            return null;
        }
        try {
            Class<?> apiType = Class.forName(TACZ_API_CLASS);
            Method getGunDisplay = apiType.getMethod("getGunDisplay", ItemStack.class);
            Object result = getGunDisplay.invoke(null, stack);
            if (!(result instanceof Optional<?> optional) || optional.isEmpty()) {
                return null;
            }
            Object display = optional.get();
            Object texture = display.getClass().getMethod("getHUDTexture").invoke(display);
            return texture instanceof ResourceLocation location ? location : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logFailureOnce(exception);
            return null;
        }
    }

    private static synchronized void logFailureOnce(Exception exception) {
        if (!reflectionFailureLogged) {
            reflectionFailureLogged = true;
            WokInfantryMod.LOGGER.warn(
                    "TaCZ HUD silhouette integration failed; loadout previews will use item icons",
                    exception);
        }
    }

    record Preview(ItemStack stack, ResourceLocation hudTexture) {
        private static final Preview EMPTY = new Preview(ItemStack.EMPTY, null);

        Preview {
            stack = stack == null ? ItemStack.EMPTY : stack;
        }

        boolean available() {
            return hudTexture != null || !stack.isEmpty();
        }
    }

    private record PreviewKey(String itemId, int count, String snbt) {
        private PreviewKey {
            itemId = itemId == null ? "" : itemId;
            count = Math.max(1, count);
            snbt = snbt == null ? "" : snbt;
        }
    }
}
