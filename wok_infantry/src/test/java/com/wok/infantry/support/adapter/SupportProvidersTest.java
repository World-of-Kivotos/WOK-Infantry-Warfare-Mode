package com.wok.infantry.support.adapter;

import com.wok.infantry.support.SupportDefinition;
import com.wok.infantry.support.SupportRegistry;
import com.wok.infantry.support.SupportTargetMode;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportProvidersTest {
    @Test
    void defaultBootstrapIsCachedEmptyAndRejectsLateRegistration() {
        SupportRegistry first = SupportProviders.createDefault();
        SupportRegistry second = SupportProviders.createDefault();

        assertSame(first, second);
        assertTrue(first.isEmpty());
        assertTrue(SupportProviders.frozen());
        SupportDefinition late = new SupportDefinition(
                ResourceLocation.fromNamespaceAndPath("test", "late"),
                "support.test.late", "Late", "Late", SupportTargetMode.POINT,
                0L, 0L, 1, 1, 0.0D);
        assertThrows(IllegalStateException.class,
                () -> SupportProviders.registerDefinition(late));
    }
}
