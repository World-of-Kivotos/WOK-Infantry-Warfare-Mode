package com.wok.capturepoints.capture;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapturePointTest {
    private static final ResourceLocation OVERWORLD = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "overworld");

    @Test
    void normalizesCornersAndIncludesBoundaryBlocks() {
        CapturePoint point = new CapturePoint("a", "A", OVERWORLD,
                new BlockPos(10, 80, 12), new BlockPos(2, 60, 4), 0);
        assertEquals(new BlockPos(2, 60, 4), point.min());
        assertEquals(new BlockPos(10, 80, 12), point.max());
        assertTrue(point.contains(OVERWORLD, new BlockPos(2, 60, 4)));
        assertTrue(point.contains(OVERWORLD, new BlockPos(10, 80, 12)));
        assertFalse(point.contains(OVERWORLD, new BlockPos(11, 80, 12)));
    }

    @Test
    void ownerOnlyChangesAtFullControl() {
        CapturePoint point = new CapturePoint("a", "A", OVERWORLD,
                BlockPos.ZERO, BlockPos.ZERO, 0);
        point.setControl(0.99D);
        assertEquals(CaptureTeam.NEUTRAL, point.owner());
        point.setOwner(CaptureTeam.BLUE);
        assertEquals(CaptureTeam.BLUE, point.owner());
    }
}
