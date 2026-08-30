package com.wok.infantry.formation.vehicle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.regex.Pattern;

/** Namespaced persistent entity marker; contains no卓越前线 class references. */
public final class VehiclePersistentData {
    public static final String ROOT_TAG = "wok_infantry";
    public static final String VERSION_TAG = "VehicleVersion";
    public static final String SESSION_TAG = "Session";
    public static final String FACTION_TAG = "Faction";
    public static final String FORMATION_TAG = "Formation";
    public static final String ALLOCATION_TAG = "Allocation";
    public static final String ENTITY_TYPE_TAG = "EntityType";
    public static final String DIMENSION_TAG = "Dimension";
    public static final int VERSION = 1;
    private static final int MAX_ID_LENGTH = 256;
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z0-9_.:/-]+");

    private VehiclePersistentData() {
    }

    public static void stamp(Entity entity, VehicleOwnership ownership) {
        if (entity == null || ownership == null) {
            throw new IllegalArgumentException("Vehicle entity and ownership are required");
        }
        CompoundTag persistent = entity.getPersistentData();
        CompoundTag root = persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)
                ? persistent.getCompound(ROOT_TAG) : new CompoundTag();
        root.putInt(VERSION_TAG, VERSION);
        root.putUUID(SESSION_TAG, ownership.sessionId());
        root.putString(FACTION_TAG, ownership.factionId());
        root.putString(FORMATION_TAG, ownership.formationId());
        root.putString(ALLOCATION_TAG, ownership.allocationId());
        root.putString(ENTITY_TYPE_TAG, ownership.entityTypeId().toString());
        root.putString(DIMENSION_TAG, ownership.dimension().toString());
        persistent.put(ROOT_TAG, root);
    }

    public static Optional<VehicleOwnership> read(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag root = persistent.getCompound(ROOT_TAG);
        if (root.getInt(VERSION_TAG) != VERSION || !root.hasUUID(SESSION_TAG)
                || !validString(root, FACTION_TAG) || !validString(root, FORMATION_TAG)
                || !validString(root, ALLOCATION_TAG) || !validString(root, ENTITY_TYPE_TAG)
                || !validString(root, DIMENSION_TAG)) {
            return Optional.empty();
        }
        ResourceLocation entityType = ResourceLocation.tryParse(root.getString(ENTITY_TYPE_TAG));
        ResourceLocation dimension = ResourceLocation.tryParse(root.getString(DIMENSION_TAG));
        if (entityType == null || dimension == null
                || !SuperbWarfareVehicleGate.supportsEntity(entityType)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new VehicleOwnership(root.getUUID(SESSION_TAG),
                    root.getString(FACTION_TAG), root.getString(FORMATION_TAG),
                    root.getString(ALLOCATION_TAG), entityType, dimension));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    /** Distinguishes an unmanaged entity from a corrupted WOK vehicle marker. */
    public static boolean hasVehicleMarker(Entity entity) {
        if (entity == null) {
            return false;
        }
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return false;
        }
        CompoundTag root = persistent.getCompound(ROOT_TAG);
        return root.contains(VERSION_TAG, Tag.TAG_INT);
    }

    private static boolean validString(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_STRING)) {
            return false;
        }
        String value = tag.getString(key);
        return !value.isBlank() && value.length() <= MAX_ID_LENGTH
                && IDENTIFIER.matcher(value).matches();
    }
}
