package com.wok.infantry.client.tablet;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Render types of scheme A's 3D device (IMPL_PLAN D9 / D10), as a {@link RenderType} subclass
 * only to reach the protected state shards (it is never instantiated).
 *
 * <ul>
 *   <li>{@link #face}: the front face and the sleep line. The position-colour-texture shader has no
 *   light map, no fog and no directional light, so the face is exactly the colours the GUI draws
 *   (p = 0.61 hands over to the 2D device without a colour step); it discards texels below alpha
 *   0.1, and the face image is only 0 or 255. Nearest or linear sampling for both minification and
 *   magnification (review V6: the smooth tiers sample linearly both ways), no culling.</li>
 *   <li>The case body uses the vanilla {@code entityCutoutNoCull} on a white texture with vertex
 *   colours: world light and the two entity lights, so the sides read as a solid object.</li>
 * </ul>
 */
final class TabletRenderTypes extends RenderType {
    private record Key(ResourceLocation texture, boolean linear) {
    }

    private static final Map<Key, RenderType> FACES = new HashMap<>();

    private TabletRenderTypes() {
        super("wok_infantry_tablet_unused", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 0,
                false, false, () -> {
                }, () -> {
                });
    }

    /** The full-bright, alpha-tested front face on {@code texture}. */
    static RenderType face(ResourceLocation texture, boolean linear) {
        return FACES.computeIfAbsent(new Key(texture, linear), key -> create(
                "wok_infantry_tablet_face", DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS, 256, false, true,
                CompositeState.builder()
                        .setShaderState(POSITION_COLOR_TEX_SHADER)
                        .setTextureState(new TextureStateShard(key.texture(), key.linear(), false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .createCompositeState(false)));
    }

    /** The case body (sides and back) on the white {@code texture}. */
    static RenderType body(ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }

    /** Drops the cached types of a released texture. */
    static void forget(ResourceLocation texture) {
        FACES.keySet().removeIf(key -> key.texture().equals(texture));
    }

    /** Drops every cached type. */
    static void clear() {
        FACES.clear();
    }
}
