package com.wok.infantry.deployment;

import com.wok.infantry.battle.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BaseSupplyAreasTest {
    @Test void reversedCornersIncludeBoundariesAndRoundTrip() {
        var dimension = ResourceLocation.parse("minecraft:overworld");
        var area = new BaseSupplyArea(dimension, new BlockPos(10, 80, 20), new BlockPos(-5, 60, -10));
        assertTrue(area.contains(dimension, new BlockPos(10, 80, 20)));
        assertFalse(area.contains(dimension, new BlockPos(10, 81, 20)));
        assertFalse(area.contains(ResourceLocation.parse("minecraft:the_nether"), BlockPos.ZERO));
        BaseSupplyAreas saved = new BaseSupplyAreas();
        saved.set(Faction.BLUE, area);
        assertEquals(area, BaseSupplyAreas.load(saved.save(new CompoundTag())).area(Faction.BLUE));
        assertThrows(IllegalArgumentException.class, () -> saved.set(Faction.RED, area));
    }
}
