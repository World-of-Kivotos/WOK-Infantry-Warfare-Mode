package com.wok.infantry.support;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportDefinitionAndTargetTest {
    private static final ResourceLocation OVERWORLD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    @Test
    void definitionIsNamespacedBoundedAndProviderNeutral() {
        SupportDefinition point = definition("example", "point", SupportTargetMode.POINT);
        SupportDefinition directional = definition("other_mod", "directional",
                SupportTargetMode.DIRECTIONAL);

        assertEquals("example:point", point.id().toString());
        assertFalse(point.directional());
        assertTrue(directional.directional());
        assertThrows(IllegalArgumentException.class, () -> new SupportDefinition(
                point.id(), "key", "Name", "N", SupportTargetMode.POINT,
                0L, 0L, 0, 1, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> new SupportDefinition(
                point.id(), "key", "Name", "N", SupportTargetMode.POINT,
                0L, 0L, 1, 1, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new SupportDefinition(
                point.id(), "", "Name", "N", SupportTargetMode.POINT,
                0L, 0L, 1, 1, 0.0D));

        ResourceLocation maximumId = ResourceLocation.fromNamespaceAndPath(
                "test", "a".repeat(SupportDefinition.MAX_ID_LENGTH - "test:".length()));
        ResourceLocation oversizedId = ResourceLocation.fromNamespaceAndPath(
                "test", "a".repeat(SupportDefinition.MAX_ID_LENGTH - "test:".length() + 1));
        assertEquals(SupportDefinition.MAX_ID_LENGTH, maximumId.toString().length());
        assertEquals(maximumId, new SupportDefinition(maximumId, "key", "Name", "N",
                SupportTargetMode.POINT, 0L, 0L, 1, 1, 0.0D).id());
        assertThrows(IllegalArgumentException.class, () -> new SupportDefinition(
                oversizedId, "key", "Name", "N", SupportTargetMode.POINT,
                0L, 0L, 1, 1, 0.0D));
    }

    @Test
    void pointAndDirectionalHelpersPreserveFiniteIntent() {
        SupportTarget point = SupportTarget.point(OVERWORLD, 10.25D, -30.5D);
        assertEquals(0.0D, point.length());
        assertEquals(point.startX(), point.endX());
        assertEquals(point.startZ(), point.endZ());

        SupportTarget direction = SupportTarget.directional(OVERWORLD,
                0.0D, 0.0D, 3.0D, 4.0D);
        assertEquals(3.0D, direction.deltaX());
        assertEquals(4.0D, direction.deltaZ());
        assertEquals(5.0D, direction.length());
        assertTrue(SupportTarget.isValidDirection(0.0D, 0.0D, 16.0D, 0.0D));
        assertTrue(SupportTarget.isValidDirection(0.0D, 0.0D, 512.0D, 0.0D));
        assertFalse(SupportTarget.isValidDirection(0.0D, 0.0D, 15.99D, 0.0D));

        assertThrows(NullPointerException.class,
                () -> SupportTarget.point(null, 0.0D, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> SupportTarget.point(OVERWORLD, Double.NaN, 0.0D));
    }

    static SupportDefinition definition(String namespace, String path,
                                        SupportTargetMode targetMode) {
        return new SupportDefinition(ResourceLocation.fromNamespaceAndPath(namespace, path),
                "support." + namespace + "." + path, "Framework Test", "Test",
                targetMode, 2_400L, 100L, 4, 10, 18.0D);
    }
}
