package com.wok.infantry.client.map;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TacticalSupportMapPresentationRegistryTest {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            "wok_test", "offensive_support");

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
}
