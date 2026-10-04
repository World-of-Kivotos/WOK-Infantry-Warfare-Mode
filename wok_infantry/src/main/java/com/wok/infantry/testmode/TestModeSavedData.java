package com.wok.infantry.testmode;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Objects;

/**
 * Server-wide test-mode switch, saved with the world in its own file
 * ({@code data/wok_infantry_test_mode.dat} of the overworld) so it survives restarts. No other
 * save file changes; an older build simply does not read this one.
 */
public final class TestModeSavedData extends SavedData {
    public static final String FILE_ID = "wok_infantry_test_mode";
    static final int VERSION = 1;
    private static final int MAX_NAME_LENGTH = 64;

    private boolean enabled;
    private long changedAtMillis;
    private String changedBy = "";

    public TestModeSavedData() {
    }

    public static TestModeSavedData get(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getDataStorage().computeIfAbsent(TestModeSavedData::load,
                TestModeSavedData::new, FILE_ID);
    }

    public static TestModeSavedData load(CompoundTag root) {
        TestModeSavedData data = new TestModeSavedData();
        data.enabled = root.getBoolean("Enabled");
        data.changedAtMillis = Math.max(0L, root.getLong("ChangedAt"));
        data.changedBy = clip(root.getString("ChangedBy"));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        root.putInt("Version", VERSION);
        root.putBoolean("Enabled", enabled);
        root.putLong("ChangedAt", changedAtMillis);
        root.putString("ChangedBy", changedBy);
        return root;
    }

    public boolean enabled() {
        return enabled;
    }

    /** Wall-clock time of the last switch (0 when it was never switched). */
    public long changedAtMillis() {
        return changedAtMillis;
    }

    /** Name of who switched it last ("控制台" for the console), empty when never switched. */
    public String changedBy() {
        return changedBy;
    }

    /** Records a real switch; the caller has already decided that the state changes. */
    public void set(boolean enabled, long atMillis, String by) {
        this.enabled = enabled;
        this.changedAtMillis = Math.max(0L, atMillis);
        this.changedBy = clip(by);
        setDirty();
    }

    private static String clip(String name) {
        String value = name == null ? "" : name.trim();
        return value.length() <= MAX_NAME_LENGTH ? value : value.substring(0, MAX_NAME_LENGTH);
    }
}
