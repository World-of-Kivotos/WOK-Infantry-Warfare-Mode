package com.wok.capturepoints.capture;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CaptureSavedData extends SavedData {
    private static final String DATA_NAME = "wok_capture_points";
    private final Map<String, CapturePoint> points = new LinkedHashMap<>();
    private int durationOverride = -1;
    private Boolean sequentialOverride;

    public static CaptureSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                CaptureSavedData::load, CaptureSavedData::new, DATA_NAME);
    }

    public List<CapturePoint> points() {
        return points.values().stream()
                .sorted(Comparator.comparingInt(CapturePoint::order)
                        .thenComparing(CapturePoint::id)).toList();
    }

    public CapturePoint point(String id) {
        return points.get(id);
    }

    public void put(CapturePoint point) {
        points.put(point.id(), point);
        setDirty();
    }

    public CapturePoint remove(String id) {
        CapturePoint removed = points.remove(id);
        if (removed != null) setDirty();
        return removed;
    }

    public int size() {
        return points.size();
    }

    public int nextOrder() {
        return points.values().stream().mapToInt(CapturePoint::order).max().orElse(-1) + 1;
    }

    public int durationOverride() {
        return durationOverride;
    }

    public void setDurationOverride(int seconds) {
        durationOverride = seconds < 1 ? -1 : seconds;
        setDirty();
    }

    public Boolean sequentialOverride() {
        return sequentialOverride;
    }

    public void setSequentialOverride(Boolean value) {
        sequentialOverride = value;
        setDirty();
    }

    public void changed() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("DurationOverride", durationOverride);
        if (sequentialOverride != null) {
            tag.putBoolean("HasSequentialOverride", true);
            tag.putBoolean("SequentialOverride", sequentialOverride);
        }
        ListTag list = new ListTag();
        for (CapturePoint point : points()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", point.id());
            entry.putString("DisplayName", point.displayName());
            entry.putString("Dimension", point.dimension().toString());
            entry.putLong("Min", point.min().asLong());
            entry.putLong("Max", point.max().asLong());
            entry.putInt("Order", point.order());
            entry.putInt("CaptureSeconds", point.captureSecondsOverride());
            entry.putBoolean("Enabled", point.enabled());
            entry.putDouble("Control", point.control());
            list.add(entry);
        }
        tag.put("Points", list);
        return tag;
    }

    private static CaptureSavedData load(CompoundTag tag) {
        CaptureSavedData data = new CaptureSavedData();
        data.durationOverride = tag.contains("DurationOverride", Tag.TAG_INT)
                ? tag.getInt("DurationOverride") : -1;
        if (tag.getBoolean("HasSequentialOverride")) {
            data.sequentialOverride = tag.getBoolean("SequentialOverride");
        }
        ListTag list = tag.getList("Points", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            String id = entry.getString("Id");
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("Dimension"));
            if (id.isBlank() || dimension == null || data.points.containsKey(id)) continue;
            BlockPos min = BlockPos.of(entry.getLong("Min"));
            BlockPos max = BlockPos.of(entry.getLong("Max"));
            CapturePoint point;
            try {
                point = new CapturePoint(id,
                        entry.getString("DisplayName").isBlank() ? id : entry.getString("DisplayName"),
                        dimension, min, max, entry.getInt("Order"));
            } catch (IllegalArgumentException exception) {
                continue;
            }
            point.setCaptureSecondsOverride(entry.getInt("CaptureSeconds"));
            point.setEnabled(!entry.contains("Enabled") || entry.getBoolean("Enabled"));
            point.setControl(entry.getDouble("Control"));
            data.points.put(id, point);
        }
        return data;
    }
}
