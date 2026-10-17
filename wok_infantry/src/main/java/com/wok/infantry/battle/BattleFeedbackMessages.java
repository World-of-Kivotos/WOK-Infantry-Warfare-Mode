package com.wok.infantry.battle;

import net.minecraft.network.chat.Component;

import java.util.Objects;

/** Small wire tokens for feedback that must be translated by the receiving client. */
public final class BattleFeedbackMessages {
    private static final String MARKER_CREATED_PREFIX = "@wok_infantry:marker_created:";

    private BattleFeedbackMessages() {
    }

    public static String markerCreated(TacticalMarkerType type) {
        return MARKER_CREATED_PREFIX + Objects.requireNonNull(type, "type").id();
    }

    /** Unknown and legacy server text remains literal for protocol compatibility. */
    public static Component resolve(String wireMessage) {
        String message = Objects.requireNonNullElse(wireMessage, "");
        if (!message.startsWith(MARKER_CREATED_PREFIX)) {
            return Component.literal(message);
        }
        String markerId = message.substring(MARKER_CREATED_PREFIX.length());
        return TacticalMarkerType.byId(markerId)
                .<Component>map(type -> Component.translatable(
                        "message.wok_infantry.marker_created",
                        Component.translatable("marker.wok_infantry." + type.id())))
                .orElseGet(() -> Component.literal(message));
    }
}
