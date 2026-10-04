package com.wok.infantry.client.map;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TacticalSupportMapPresentationRegistryTest {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            "wok_test", "offensive_support");
    private static final ResourceLocation GUIDED_ID = ResourceLocation.fromNamespaceAndPath(
            "wok_test", "guided_support");

    @Test
    void defaultsToUtilityAndCanRegisterAndRemoveClientPresentation() {
        TacticalSupportMapPresentationRegistry.unregister(ID);
        assertEquals(TacticalSupportMapPresentation.UTILITY,
                TacticalSupportMapPresentationRegistry.presentation(ID));

        TacticalSupportMapPresentationRegistry.register(ID,
                TacticalSupportMapPresentation.OFFENSIVE);
        assertEquals(TacticalSupportMapPresentation.OFFENSIVE,
                TacticalSupportMapPresentationRegistry.presentation(ID));

        TacticalSupportMapPresentationRegistry.unregister(ID);
        assertEquals(TacticalSupportMapPresentation.UTILITY,
                TacticalSupportMapPresentationRegistry.presentation(ID));
    }

    @Test
    void guidanceRadiusIsOptionalAndIndependentOfThePresentationHint() {
        TacticalSupportMapPresentationRegistry.unregister(GUIDED_ID);
        assertEquals(OptionalDouble.empty(),
                TacticalSupportMapPresentationRegistry.guidanceRadius(GUIDED_ID),
                "supports without an inner zone must keep the previous map drawing");

        TacticalSupportMapPresentationRegistry.register(GUIDED_ID,
                TacticalSupportMapPresentation.OFFENSIVE);
        TacticalSupportMapPresentationRegistry.registerGuidanceRadius(GUIDED_ID, 64.0D);
        assertEquals(OptionalDouble.of(64.0D),
                TacticalSupportMapPresentationRegistry.guidanceRadius(GUIDED_ID));

        TacticalSupportMapPresentationRegistry.registerGuidanceRadius(GUIDED_ID, 48.0D);
        assertEquals(OptionalDouble.of(48.0D),
                TacticalSupportMapPresentationRegistry.guidanceRadius(GUIDED_ID),
                "re-registering replaces the previous inner zone");

        TacticalSupportMapPresentationRegistry.unregisterGuidanceRadius(GUIDED_ID);
        assertEquals(OptionalDouble.empty(),
                TacticalSupportMapPresentationRegistry.guidanceRadius(GUIDED_ID));
        assertEquals(TacticalSupportMapPresentation.OFFENSIVE,
                TacticalSupportMapPresentationRegistry.presentation(GUIDED_ID),
                "removing the inner zone must not reset the presentation hint");

        TacticalSupportMapPresentationRegistry.registerGuidanceRadius(GUIDED_ID, 64.0D);
        TacticalSupportMapPresentationRegistry.unregister(GUIDED_ID);
        assertEquals(OptionalDouble.empty(),
                TacticalSupportMapPresentationRegistry.guidanceRadius(GUIDED_ID),
                "unregister removes every client hint of the support");
        assertEquals(TacticalSupportMapPresentation.UTILITY,
                TacticalSupportMapPresentationRegistry.presentation(GUIDED_ID));
    }

    @Test
    void guidanceRadiusRejectsUnusableValues() {
        for (double invalid : new double[] {0.0D, -1.0D, Double.NaN,
                Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> TacticalSupportMapPresentationRegistry.registerGuidanceRadius(
                            GUIDED_ID, invalid));
        }
        assertThrows(NullPointerException.class,
                () -> TacticalSupportMapPresentationRegistry.registerGuidanceRadius(
                        null, 64.0D));
        assertThrows(NullPointerException.class,
                () -> TacticalSupportMapPresentationRegistry.guidanceRadius(null));
    }
}
