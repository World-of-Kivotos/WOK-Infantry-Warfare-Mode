package com.wok.trauma.compat.tacz;

import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.wok.trauma.config.TraumaConfig;
import com.wok.trauma.registry.ModEffects;
import net.minecraft.world.entity.player.Player;

import java.util.WeakHashMap;

final class TaczWeaponPenalty {
    private static final WeakHashMap<Player, State> STATES = new WeakHashMap<>();

    static void update(Player player) {
        AttachmentCacheProperty cache = IGunOperator.fromLivingEntity(player).getCacheProperty();
        if (cache == null) {
            STATES.remove(player);
            return;
        }

        State state = STATES.get(player);
        if (state == null || state.cache != cache) {
            state = State.capture(cache);
            if (state == null) {
                STATES.remove(player);
                return;
            }
            STATES.put(player, state);
        }

        if (player.hasEffect(ModEffects.TREMOR.get())) {
            state.applyOrRefreshPenalty();
        } else {
            state.restore();
        }
    }

    private static final class State {
        private final AttachmentCacheProperty cache;
        private final float baseAdsTime;
        private boolean applied;

        private State(AttachmentCacheProperty cache, float adsTime) {
            this.cache = cache;
            this.baseAdsTime = adsTime;
        }

        private static State capture(AttachmentCacheProperty cache) {
            Float adsTime = cache.getCache(GunProperties.ADS_TIME);
            if (adsTime == null || !Float.isFinite(adsTime)) {
                return null;
            }
            return new State(cache, adsTime);
        }

        private void applyOrRefreshPenalty() {
            float adsMultiplier = TraumaConfig.TREMOR_ADS_TIME_MULTIPLIER.get().floatValue();
            float penalizedAdsTime = baseAdsTime * adsMultiplier;
            if (!Float.isFinite(penalizedAdsTime)) {
                return;
            }

            Float currentAdsTime = cache.getCache(GunProperties.ADS_TIME);
            if (currentAdsTime == null || Float.compare(currentAdsTime, penalizedAdsTime) != 0) {
                cache.setCache(GunProperties.ADS_TIME, penalizedAdsTime);
            }
            applied = true;
        }

        private void restore() {
            if (!applied) {
                return;
            }
            cache.setCache(GunProperties.ADS_TIME, baseAdsTime);
            applied = false;
        }
    }

    private TaczWeaponPenalty() {
    }
}
