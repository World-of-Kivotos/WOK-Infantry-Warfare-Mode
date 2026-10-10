package com.wok.infantry.client.tablet;

import com.mojang.blaze3d.platform.NativeImage;
import com.wok.infantry.WokInfantryMod;
import com.wok.infantry.client.screen.DeviceArt;
import com.wok.infantry.client.screen.TacticalLivery;
import com.wok.infantry.client.screen.TacticalShellLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Textures of scheme A's 3D device (IMPL_PLAN 2.2 {@code TabletFaceTexture}, D10, D11): the front
 * face of one layout size, size class, livery, key layout and link colour, uploaded from
 * {@code DeviceArt.rasterize} (no drop shadow, matte specks, blank keys, the link LED baked in; no
 * silkscreen, DESIGN 3.5 / anim-spec {@code d2.face3D}) with the walls traced from the same image;
 * and a white texture for the case body. A few faces are kept (a change of window size, livery or
 * link state makes a new one); the oldest is released. Everything is released on logout and on a
 * resource reload. Render thread only.
 */
final class TabletTextures {
    /** Faces kept at once (open, the snapshot's link change, a resize). */
    private static final int KEEP = 4;

    /** What one front face is painted from. */
    record FaceKey(int width, int height, TacticalShellLayout.Density density,
                   TacticalLivery.Livery livery, List<DeviceArt.BlankKey> keys, int linkColor,
                   boolean linear) {
        FaceKey {
            keys = keys == null ? List.of() : List.copyOf(keys);
        }
    }

    /** One uploaded front face and its traced walls. */
    record Face(FaceKey key, ResourceLocation location, DynamicTexture texture,
                TabletDeviceModel.Mesh mesh) {
        int textureId() {
            return texture.getId();
        }
    }

    private static final Map<FaceKey, Face> FACES = new LinkedHashMap<>(8, 0.75F, true);
    /** Faces that could not be built (not tried again until the next {@link #clear()}). */
    private static final java.util.Set<FaceKey> FAILED = new java.util.HashSet<>();
    private static ResourceLocation white;
    private static int serial;

    private TabletTextures() {
    }

    /** The face of {@code key}, painted and uploaded on first use; {@code null} when it fails. */
    static Face face(FaceKey key) {
        Face face = FACES.get(key);
        if (face != null) {
            return face;
        }
        if (FAILED.contains(key)) {
            return null;
        }
        try {
            face = build(key);
        } catch (RuntimeException exception) {
            FAILED.add(key);
            WokInfantryMod.LOGGER.warn("[TabletAnim] front face {}x{} unavailable", key.width(),
                    key.height(), exception);
            return null;
        }
        FACES.put(key, face);
        trim();
        return face;
    }

    private static Face build(FaceKey key) {
        int w = Math.max(1, key.width());
        int h = Math.max(1, key.height());
        int[] argb = DeviceArt.rasterize(w, h, key.density(), key.livery(),
                new DeviceArt.RasterOptions(false, true, key.keys(), key.linkColor()));
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setPixelRGBA(x, y, abgr(argb[y * w + x]));
            }
        }
        DynamicTexture texture = new DynamicTexture(image);
        texture.setFilter(key.linear(), false);
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(WokInfantryMod.MOD_ID,
                "dynamic/tablet_face_" + (serial++));
        Minecraft.getInstance().getTextureManager().register(location, texture);
        TabletDeviceModel.Mesh mesh = TabletDeviceModel.mesh(argb, w, h,
                TabletD2Geometry.of(w, h).d());
        return new Face(key, location, texture, mesh);
    }

    /** {@code 0xAARRGGBB} → the {@code 0xAABBGGRR} {@link NativeImage#setPixelRGBA} takes. */
    static int abgr(int argb) {
        return (argb & 0xFF00FF00) | ((argb >>> 16) & 0xFF) | ((argb & 0xFF) << 16);
    }

    private static void trim() {
        Iterator<Map.Entry<FaceKey, Face>> it = FACES.entrySet().iterator();
        while (FACES.size() > KEEP && it.hasNext()) {
            Face old = it.next().getValue();
            it.remove();
            release(old.location());
        }
    }

    /** The 16×16 white texture of the case body. */
    static ResourceLocation white() {
        if (white == null) {
            NativeImage image = new NativeImage(NativeImage.Format.RGBA, 16, 16, false);
            image.fillRect(0, 0, 16, 16, 0xFFFFFFFF);
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                    WokInfantryMod.MOD_ID, "dynamic/tablet_white");
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            white = location;
        }
        return white;
    }

    private static void release(ResourceLocation location) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && location != null) {
            minecraft.getTextureManager().release(location);
            TabletRenderTypes.forget(location);
        }
    }

    /** Releases every texture (logout, resource reload); they are rebuilt on demand. */
    static void clear() {
        for (Face face : FACES.values()) {
            release(face.location());
        }
        FACES.clear();
        FAILED.clear();
        if (white != null) {
            release(white);
            white = null;
        }
        TabletRenderTypes.clear();
    }
}
