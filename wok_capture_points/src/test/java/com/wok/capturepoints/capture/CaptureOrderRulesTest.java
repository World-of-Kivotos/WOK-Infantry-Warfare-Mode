package com.wok.capturepoints.capture;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptureOrderRulesTest {
    @Test
    void blueAdvancesAscendingAndRedDescending() {
        ResourceLocation dimension = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
        CapturePoint a = new CapturePoint("a", "A", dimension, BlockPos.ZERO, BlockPos.ZERO, 0);
        CapturePoint b = new CapturePoint("b", "B", dimension, BlockPos.ZERO, BlockPos.ZERO, 1);
        CapturePoint c = new CapturePoint("c", "C", dimension, BlockPos.ZERO, BlockPos.ZERO, 2);
        List<CapturePoint> points = List.of(a, b, c);
        Map<String, CaptureTeam> owners = new LinkedHashMap<>();
        owners.put("a", CaptureTeam.BLUE);
        owners.put("b", CaptureTeam.NEUTRAL);
        owners.put("c", CaptureTeam.RED);

        assertTrue(CaptureOrderRules.allowed(points, owners, b, CaptureTeam.BLUE));
        assertFalse(CaptureOrderRules.allowed(points, owners, c, CaptureTeam.BLUE));
        assertTrue(CaptureOrderRules.allowed(points, owners, b, CaptureTeam.RED));
        assertFalse(CaptureOrderRules.allowed(points, owners, a, CaptureTeam.RED));
    }
}
