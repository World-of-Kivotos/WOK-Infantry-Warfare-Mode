package com.wok.capturepoints.capture;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.regex.Pattern;

public final class CapturePoint {
    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9][a-z0-9_-]{0,31}");
    private final String id;
    private String displayName;
    private ResourceLocation dimension;
    private BlockPos min;
    private BlockPos max;
    private int order;
    private int captureSecondsOverride;
    private boolean enabled;
    private double control;

    public CapturePoint(String id, String displayName, ResourceLocation dimension,
                        BlockPos first, BlockPos second, int order) {
        if (id == null || !VALID_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid capture point id: " + id);
        }
        this.id = id;
        this.displayName = normalizeDisplayName(displayName);
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        setBounds(first, second);
        this.order = order;
        this.captureSecondsOverride = -1;
        this.enabled = true;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public ResourceLocation dimension() { return dimension; }
    public BlockPos min() { return min; }
    public BlockPos max() { return max; }
    public int order() { return order; }
    public int captureSecondsOverride() { return captureSecondsOverride; }
    public boolean enabled() { return enabled; }
    public double control() { return control; }
    public CaptureTeam owner() { return CaptureMath.owner(control); }

    public void setDisplayName(String value) { displayName = normalizeDisplayName(value); }
    public void setOrder(int value) { order = value; }
    public void setCaptureSecondsOverride(int value) { captureSecondsOverride = value < 1 ? -1 : value; }
    public void setEnabled(boolean value) { enabled = value; }
    public void setControl(double value) {
        control = Double.isFinite(value) ? Math.max(-1.0D, Math.min(1.0D, value)) : 0.0D;
    }
    public void setOwner(CaptureTeam team) { setControl(Objects.requireNonNull(team).direction()); }

    public void move(ResourceLocation newDimension, BlockPos first, BlockPos second) {
        dimension = Objects.requireNonNull(newDimension);
        setBounds(first, second);
    }

    public boolean contains(ResourceLocation candidateDimension, BlockPos position) {
        return enabled && dimension.equals(candidateDimension)
                && position.getX() >= min.getX() && position.getX() <= max.getX()
                && position.getY() >= min.getY() && position.getY() <= max.getY()
                && position.getZ() >= min.getZ() && position.getZ() <= max.getZ();
    }

    public long volume() {
        return (long) (max.getX() - min.getX() + 1)
                * (max.getY() - min.getY() + 1)
                * (max.getZ() - min.getZ() + 1);
    }

    private void setBounds(BlockPos first, BlockPos second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        min = new BlockPos(Math.min(first.getX(), second.getX()),
                Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()));
        max = new BlockPos(Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ()));
    }

    private static String normalizeDisplayName(String value) {
        String name = Objects.requireNonNull(value, "displayName").trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Display name must not be empty");
        return name.length() <= 64 ? name : name.substring(0, 64);
    }
}
