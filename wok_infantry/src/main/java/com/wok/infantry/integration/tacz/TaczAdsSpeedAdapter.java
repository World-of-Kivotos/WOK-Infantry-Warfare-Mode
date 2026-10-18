package com.wok.infantry.integration.tacz;

import com.wok.infantry.WokInfantryMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

/**
 * Optional TaCZ 1.1.8+ bridge for a persistent modifier attached to one gun ItemStack.
 * All TaCZ types stay behind reflection so WOK步战核心 still starts without TaCZ.
 */
public final class TaczAdsSpeedAdapter {
    private static final String MOD_ID = "tacz";
    private static final String GUN_INTERFACE = "com.tacz.guns.api.item.IGun";
    private static final String PROPERTY_EVENT =
            "com.tacz.guns.api.event.common.AttachmentPropertyEvent";
    private static final String CACHE_PROPERTY =
            "com.tacz.guns.resource.modifier.AttachmentCacheProperty";
    private static final String GUN_PROPERTY = "com.tacz.guns.api.GunProperty";
    private static final String GUN_PROPERTIES = "com.tacz.guns.api.GunProperties";
    private static final String PROPERTY_MANAGER =
            "com.tacz.guns.resource.modifier.AttachmentPropertyManager";

    public static final String ADS_SPEED_SCALE_TAG = "wok_infantry_ads_speed_scale";
    public static final String VERTICAL_RECOIL_SCALE_TAG =
            "wok_infantry_vertical_recoil_scale";
    public static final String HORIZONTAL_RECOIL_SCALE_TAG =
            "wok_infantry_horizontal_recoil_scale";
    public static final String SPREAD_SCALE_TAG = "wok_infantry_spread_scale";
    public static final String DAMAGE_SCALE_TAG = "wok_infantry_damage_scale";
    public static final String ARMOR_IGNORE_SCALE_TAG =
            "wok_infantry_armor_ignore_scale";
    public static final String RPM_SCALE_TAG = "wok_infantry_rpm_scale";
    private static final String EDITOR_ID_TAG = "wok_infantry_weapon_editor_id";

    private static volatile boolean bridgeInitialized;
    private static volatile Bridge bridge;
    private static boolean listenerRegistered;
    private static boolean failureLogged;
    private static int lastClientFingerprint = Integer.MIN_VALUE;

    private TaczAdsSpeedAdapter() {
    }

    /** Installs the dynamic Forge listener only when TaCZ is present. */
    public static void register() {
        Bridge current = bridge();
        if (current == null || listenerRegistered) {
            return;
        }
        synchronized (TaczAdsSpeedAdapter.class) {
            if (listenerRegistered) {
                return;
            }
            registerListener(MinecraftForge.EVENT_BUS, current.propertyEventType());
            listenerRegistered = true;
            WokInfantryMod.LOGGER.info("Enabled per-weapon TaCZ tuning integration");
        }
    }

    public static boolean available() {
        return bridge() != null;
    }

    public static boolean isGun(ItemStack stack) {
        Bridge current = bridge();
        return current != null && isGun(current, stack);
    }

    /** Applies a slowdown percentage to this exact ItemStack, not to its gun definition. */
    public static boolean setSlowdown(ItemStack stack, int slowdownPercent) {
        if (!isGun(stack)) {
            return false;
        }
        float speedScale = AdsSpeedPolicy.speedScaleForSlowdown(slowdownPercent);
        if (slowdownPercent == 0) {
            writeScale(stack, ADS_SPEED_SCALE_TAG, WeaponTuning.DEFAULT_SCALE);
        } else {
            writeScale(stack, ADS_SPEED_SCALE_TAG, speedScale);
        }
        return true;
    }

    public static boolean clearSlowdown(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ADS_SPEED_SCALE_TAG)) {
            return false;
        }
        tag.remove(ADS_SPEED_SCALE_TAG);
        return true;
    }

    public static float speedScale(ItemStack stack) {
        return readScale(stack, ADS_SPEED_SCALE_TAG);
    }

    public static WeaponTuning tuning(ItemStack stack) {
        return new WeaponTuning(
                readScale(stack, ADS_SPEED_SCALE_TAG, false),
                readScale(stack, VERTICAL_RECOIL_SCALE_TAG, true),
                readScale(stack, HORIZONTAL_RECOIL_SCALE_TAG, true),
                readScale(stack, SPREAD_SCALE_TAG, false));
    }

    public static boolean applyTuning(ItemStack stack, WeaponTuning tuning) {
        if (!isGun(stack) || tuning == null || !tuning.valid()) {
            return false;
        }
        writeScale(stack, ADS_SPEED_SCALE_TAG, tuning.adsSpeedScale());
        writeScale(stack, VERTICAL_RECOIL_SCALE_TAG, tuning.verticalRecoilScale());
        writeScale(stack, HORIZONTAL_RECOIL_SCALE_TAG, tuning.horizontalRecoilScale());
        writeScale(stack, SPREAD_SCALE_TAG, tuning.spreadScale());
        return true;
    }

    public static UUID ensureEditorId(ItemStack stack) {
        if (!isGun(stack)) {
            return null;
        }
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.hasUUID(EDITOR_ID_TAG)) {
            tag.putUUID(EDITOR_ID_TAG, UUID.randomUUID());
        }
        return tag.getUUID(EDITOR_ID_TAG);
    }

    public static boolean matchesEditorId(ItemStack stack, UUID expected) {
        if (stack == null || stack.isEmpty() || expected == null) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(EDITOR_ID_TAG)
                && expected.equals(tag.getUUID(EDITOR_ID_TAG));
    }

    /** Forces TaCZ to rebuild the holder's runtime property cache immediately. */
    public static boolean refresh(LivingEntity holder, ItemStack stack) {
        Bridge current = bridge();
        if (current == null || holder == null || !isGun(current, stack)) {
            return false;
        }
        try {
            current.postChangeEvent().invoke(null, holder, stack);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logFailureOnce("TaCZ ADS cache refresh failed", exception);
            return false;
        }
    }

    /** Client-side NBT/slot change detector; called only from ClientBootstrap. */
    public static void clientTick(LivingEntity player) {
        if (player == null) {
            resetClientTracking();
            return;
        }
        ItemStack stack = player.getMainHandItem();
        int fingerprint = heldFingerprint(player, stack);
        if (fingerprint == lastClientFingerprint) {
            return;
        }
        lastClientFingerprint = fingerprint;
        if (isGun(stack)) {
            refresh(player, stack);
        }
    }

    public static void resetClientTracking() {
        lastClientFingerprint = Integer.MIN_VALUE;
    }

    private static int heldFingerprint(LivingEntity player, ItemStack stack) {
        int result = player.getId();
        // ItemStack identity changes when the selected slot or a synced stack changes. Only add
        // our stable modifier value: TaCZ mutates ammunition NBT while firing, and that must not
        // trigger a costly property-cache rebuild on every shot.
        result = 31 * result + System.identityHashCode(stack);
        WeaponTuning tuning = tuning(stack);
        result = 31 * result + Float.floatToIntBits(tuning.adsSpeedScale());
        result = 31 * result + Float.floatToIntBits(tuning.verticalRecoilScale());
        result = 31 * result + Float.floatToIntBits(tuning.horizontalRecoilScale());
        result = 31 * result + Float.floatToIntBits(tuning.spreadScale());
        result = 31 * result + Float.floatToIntBits(readScale(stack, DAMAGE_SCALE_TAG));
        result = 31 * result + Float.floatToIntBits(readScale(stack, ARMOR_IGNORE_SCALE_TAG));
        result = 31 * result + Float.floatToIntBits(readScale(stack, RPM_SCALE_TAG));
        return result;
    }

    private static <T extends Event> void registerListener(IEventBus eventBus,
                                                            Class<T> eventType) {
        eventBus.addListener(EventPriority.NORMAL, false, eventType,
                TaczAdsSpeedAdapter::onAttachmentPropertyEvent);
    }

    private static void onAttachmentPropertyEvent(Event event) {
        Bridge current = bridge;
        if (current == null) {
            return;
        }
        try {
            Object eventStack = current.getGunItem().invoke(event);
            if (!(eventStack instanceof ItemStack stack)) {
                return;
            }
            WeaponTuning tuning = tuning(stack);
            if (tuning.defaultValues()) {
                return;
            }
            Object cache = current.getCacheProperty().invoke(event);
            applyAdsSpeed(current, cache, tuning.adsSpeedScale());
            applyRecoilScale(current, cache, current.pairLeft(),
                    tuning.verticalRecoilScale());
            applyRecoilScale(current, cache, current.pairRight(),
                    tuning.horizontalRecoilScale());
            applySpreadScale(current, cache, current.inaccuracyProperty(),
                    tuning.spreadScale());
            applySpreadScale(current, cache, current.aimInaccuracyProperty(),
                    tuning.spreadScale());
            applyDamageScale(current, cache, readScale(stack, DAMAGE_SCALE_TAG));
            applyArmorIgnoreScale(current, cache,
                    readScale(stack, ARMOR_IGNORE_SCALE_TAG));
            applyRpmScale(current, cache, readScale(stack, RPM_SCALE_TAG));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logFailureOnce("TaCZ weapon property event integration failed", exception);
        }
    }

    private static void applyAdsSpeed(Bridge current, Object cache, float scale)
            throws ReflectiveOperationException {
        if (scale == WeaponTuning.DEFAULT_SCALE) {
            return;
        }
        Object value = current.getCache().invoke(cache, current.adsTimeProperty());
        if (!(value instanceof Float aimTime) || !Float.isFinite(aimTime)
                || aimTime <= 0.0F) {
            return;
        }
        current.setCache().invoke(cache, current.adsTimeProperty(),
                AdsSpeedPolicy.adjustedAimTime(aimTime, scale));
    }

    private static void applyRecoilScale(Bridge current, Object cache, Method side,
                                         float scale) throws ReflectiveOperationException {
        if (scale == WeaponTuning.DEFAULT_SCALE) {
            return;
        }
        Object pair = current.getCache().invoke(cache, current.recoilProperty());
        if (pair == null) {
            return;
        }
        Object parameterizedCache = side.invoke(pair);
        if (parameterizedCache == null) {
            return;
        }
        Field multiplier = current.parameterizedMultiplier();
        multiplier.setDouble(parameterizedCache,
                multiplier.getDouble(parameterizedCache) * scale);
    }

    private static void applySpreadScale(Bridge current, Object cache, Object property,
                                         float scale) throws ReflectiveOperationException {
        if (scale == WeaponTuning.DEFAULT_SCALE) {
            return;
        }
        Object value = current.getCache().invoke(cache, property);
        if (!(value instanceof Map<?, ?> original)) {
            return;
        }
        Map<Object, Object> adjusted = new HashMap<>(original.size());
        for (Map.Entry<?, ?> entry : original.entrySet()) {
            Object spread = entry.getValue();
            adjusted.put(entry.getKey(), spread instanceof Float amount
                    ? Math.max(0.0F, amount * scale) : spread);
        }
        current.setCache().invoke(cache, property, adjusted);
    }

    /** Scales the complete TaCZ distance-damage curve without replacing third-party gun packs. */
    private static void applyDamageScale(Bridge current, Object cache, float scale)
            throws ReflectiveOperationException {
        if (scale == WeaponTuning.DEFAULT_SCALE) {
            return;
        }
        Object value = current.getCache().invoke(cache, current.damageProperty());
        if (!(value instanceof Iterable<?> original)) {
            return;
        }
        LinkedList<Object> adjusted = new LinkedList<>();
        for (Object pair : original) {
            if (pair == null || !current.damagePairType().isInstance(pair)) {
                return;
            }
            Object distanceValue = current.getDamageDistance().invoke(pair);
            Object damageValue = current.getDamageAmount().invoke(pair);
            if (!(distanceValue instanceof Float distance)
                    || !(damageValue instanceof Float damage)) {
                return;
            }
            adjusted.add(current.damagePairConstructor().newInstance(
                    distance, Math.max(0.0F, damage * scale)));
        }
        current.setCache().invoke(cache, current.damageProperty(), adjusted);
    }

    private static void applyArmorIgnoreScale(Bridge current, Object cache, float scale)
            throws ReflectiveOperationException {
        if (scale == WeaponTuning.DEFAULT_SCALE) {
            return;
        }
        Object value = current.getCache().invoke(cache, current.armorIgnoreProperty());
        if (value instanceof Float amount) {
            current.setCache().invoke(cache, current.armorIgnoreProperty(),
                    Math.max(0.0F, Math.min(1.0F, amount * scale)));
        }
    }

    private static void applyRpmScale(Bridge current, Object cache, float scale)
            throws ReflectiveOperationException {
        if (scale == WeaponTuning.DEFAULT_SCALE) {
            return;
        }
        Object value = current.getCache().invoke(cache, current.rpmProperty());
        if (value instanceof Integer rpm) {
            current.setCache().invoke(cache, current.rpmProperty(),
                    Math.max(1, Math.round(rpm * scale)));
        }
    }

    private static float readScale(ItemStack stack, String key) {
        return readScale(stack, key, false);
    }

    private static float readScale(ItemStack stack, String key, boolean recoil) {
        if (stack == null || stack.isEmpty()) {
            return WeaponTuning.DEFAULT_SCALE;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(key)) {
            return WeaponTuning.DEFAULT_SCALE;
        }
        float scale = tag.getFloat(key);
        boolean valid = recoil ? WeaponTuning.validRecoilScale(scale)
                : WeaponTuning.validStandardScale(scale);
        return valid ? scale : WeaponTuning.DEFAULT_SCALE;
    }

    private static void writeScale(ItemStack stack, String key, float scale) {
        if (Math.abs(scale - WeaponTuning.DEFAULT_SCALE) < 0.0001F) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                tag.remove(key);
            }
            return;
        }
        stack.getOrCreateTag().putFloat(key, scale);
    }

    private static boolean isGun(Bridge current, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            return current.getGun().invoke(null, stack) != null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logFailureOnce("TaCZ gun detection failed", exception);
            return false;
        }
    }

    private static Bridge bridge() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return null;
        }
        if (!bridgeInitialized) {
            synchronized (TaczAdsSpeedAdapter.class) {
                if (!bridgeInitialized) {
                    bridge = createBridge();
                    bridgeInitialized = true;
                }
            }
        }
        return bridge;
    }

    private static Bridge createBridge() {
        try {
            Class<?> gunType = Class.forName(GUN_INTERFACE);
            Class<? extends Event> eventType = Class.forName(PROPERTY_EVENT)
                    .asSubclass(Event.class);
            Class<?> cacheType = Class.forName(CACHE_PROPERTY);
            Class<?> gunPropertyType = Class.forName(GUN_PROPERTY);
            Class<?> gunPropertiesType = Class.forName(GUN_PROPERTIES);
            Class<?> propertyManagerType = Class.forName(PROPERTY_MANAGER);
            Class<?> pairType = Class.forName(
                    "com.tacz.guns.api.modifier.ParameterizedCachePair");
            Class<?> parameterizedCacheType = Class.forName(
                    "com.tacz.guns.api.modifier.ParameterizedCache");
            Field adsTime = gunPropertiesType.getField("ADS_TIME");
            Field recoil = gunPropertiesType.getField("RECOIL");
            Field inaccuracy = gunPropertiesType.getField("INACCURACY");
            Field aimInaccuracy = gunPropertiesType.getField("AIM_INACCURACY");
            Field damage = gunPropertiesType.getField("DAMAGE");
            Field armorIgnore = gunPropertiesType.getField("ARMOR_IGNORE");
            Field rpm = gunPropertiesType.getField("ROUNDS_PER_MINUTE");
            Class<?> damagePairType = Class.forName(
                    "com.tacz.guns.resource.pojo.data.gun.ExtraDamage$DistanceDamagePair");
            Constructor<?> damagePairConstructor = damagePairType.getConstructor(
                    float.class, float.class);
            Field parameterizedMultiplier = parameterizedCacheType.getDeclaredField(
                    "multiplier");
            parameterizedMultiplier.setAccessible(true);
            return new Bridge(
                    eventType,
                    gunType.getMethod("getIGunOrNull", ItemStack.class),
                    eventType.getMethod("getGunItem"),
                    eventType.getMethod("getCacheProperty"),
                    cacheType.getMethod("getCache", gunPropertyType),
                    cacheType.getMethod("setCache", gunPropertyType, Object.class),
                    propertyManagerType.getMethod(
                            "postChangeEvent", LivingEntity.class, ItemStack.class),
                    adsTime.get(null), recoil.get(null), inaccuracy.get(null),
                    aimInaccuracy.get(null), damage.get(null), armorIgnore.get(null),
                    rpm.get(null), damagePairType, damagePairConstructor,
                    damagePairType.getMethod("getDistance"),
                    damagePairType.getMethod("getDamage"), pairType.getMethod("left"),
                    pairType.getMethod("right"), parameterizedMultiplier);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logFailureOnce("TaCZ ADS API integration is unavailable", exception);
            return null;
        }
    }

    private static synchronized void logFailureOnce(String message, Exception exception) {
        if (!failureLogged) {
            failureLogged = true;
            WokInfantryMod.LOGGER.error(message, exception);
        }
    }

    private record Bridge(Class<? extends Event> propertyEventType, Method getGun,
                          Method getGunItem, Method getCacheProperty, Method getCache,
                          Method setCache, Method postChangeEvent, Object adsTimeProperty,
                          Object recoilProperty, Object inaccuracyProperty,
                          Object aimInaccuracyProperty, Object damageProperty,
                          Object armorIgnoreProperty, Object rpmProperty,
                          Class<?> damagePairType, Constructor<?> damagePairConstructor,
                          Method getDamageDistance, Method getDamageAmount,
                          Method pairLeft, Method pairRight, Field parameterizedMultiplier) {
    }
}
