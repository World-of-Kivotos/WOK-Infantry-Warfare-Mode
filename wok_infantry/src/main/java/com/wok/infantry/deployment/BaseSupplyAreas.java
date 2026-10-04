package com.wok.infantry.deployment;

import com.wok.infantry.battle.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.EnumMap;

public final class BaseSupplyAreas extends SavedData {
    private final EnumMap<Faction, BaseSupplyArea> areas = new EnumMap<>(Faction.class);

    public static BaseSupplyAreas get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(BaseSupplyAreas::load,
                BaseSupplyAreas::new, "wok_infantry_base_supply_areas");
    }

    static BaseSupplyAreas load(CompoundTag root) {
        BaseSupplyAreas data = new BaseSupplyAreas();
        for (Faction faction : Faction.values()) {
            if (!root.contains(faction.id(), Tag.TAG_COMPOUND)) continue;
            CompoundTag tag = root.getCompound(faction.id());
            try {
                BaseSupplyArea area = new BaseSupplyArea(ResourceLocation.tryParse(tag.getString("Dimension")),
                        BlockPos.of(tag.getLong("Min")), BlockPos.of(tag.getLong("Max")));
                data.set(faction, area);
            } catch (IllegalArgumentException ignored) { }
        }
        return data;
    }

    public BaseSupplyArea area(Faction faction) { return areas.get(faction); }

    public void set(Faction faction, BaseSupplyArea area) {
        if (faction == null) throw new IllegalArgumentException("Missing faction");
        if (area != null && areas.entrySet().stream().anyMatch(entry -> entry.getKey() != faction
                && area.overlaps(entry.getValue()))) throw new IllegalArgumentException("双方基地补给区不能重叠");
        if (area == null) areas.remove(faction); else areas.put(faction, area);
        setDirty();
    }

    @Override public CompoundTag save(CompoundTag root) {
        areas.forEach((faction, area) -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dimension", area.dimension().toString());
            tag.putLong("Min", area.min().asLong());
            tag.putLong("Max", area.max().asLong());
            root.put(faction.id(), tag);
        });
        return root;
    }
}
