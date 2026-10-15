package com.wok.infantry.support;

import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Immutable client DTO; it never exposes another faction's cooldowns or missions. */
public record SupportView(
        List<SupportOptionView> options,
        List<SupportMissionView> activeMissions,
        long serverGameTick,
        long structuralRevision,
        boolean serviceAvailable,
        String serviceMessage
) {
    public static final int MAX_SERVICE_MESSAGE_LENGTH = 160;

    public SupportView {
        options = List.copyOf(options == null ? List.of() : options);
        activeMissions = List.copyOf(activeMissions == null ? List.of() : activeMissions);
        if (serverGameTick < 0L || structuralRevision < 0L) {
            throw new IllegalArgumentException("Support view tick and revision cannot be negative");
        }
        serviceMessage = sanitizeServiceMessage(serviceMessage);
        if (serviceAvailable && !serviceMessage.isBlank() && !options.isEmpty()) {
            serviceMessage = "";
        }
        Set<ResourceLocation> optionIds = new HashSet<>();
        for (SupportOptionView option : options) {
            if (option == null || !optionIds.add(option.id())) {
                throw new IllegalArgumentException("Support options must be non-null and unique");
            }
        }
        Set<java.util.UUID> missionIds = new HashSet<>();
        Set<ResourceLocation> missionSupportIds = new HashSet<>();
        for (SupportMissionView mission : activeMissions) {
            if (mission == null || !optionIds.contains(mission.supportId())
                    || !missionIds.add(mission.callId())
                    || !missionSupportIds.add(mission.supportId())) {
                throw new IllegalArgumentException(
                        "Support missions must be unique and reference an option");
            }
        }
    }

    public static SupportView unavailable() {
        return unavailable(0L, 0L, "支援服务不可用");
    }

    public static SupportView unavailable(long serverGameTick, long structuralRevision,
                                          String reason) {
        String safeReason = reason == null || reason.isBlank() ? "支援服务不可用" : reason;
        return new SupportView(List.of(), List.of(), Math.max(0L, serverGameTick),
                Math.max(0L, structuralRevision), false, safeReason);
    }

    public static SupportView empty(long serverGameTick, long structuralRevision) {
        return new SupportView(List.of(), List.of(), serverGameTick, structuralRevision,
                true, "尚未注册支援能力");
    }

    public static String sanitizeServiceMessage(String message) {
        return SupportOptionView.sanitizeAvailabilityReason(message);
    }
}
