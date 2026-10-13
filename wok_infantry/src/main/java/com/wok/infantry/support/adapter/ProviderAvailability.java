package com.wok.infantry.support.adapter;

import com.wok.infantry.support.SupportOptionView;

/** Fail-closed result of probing one optional provider integration. */
public record ProviderAvailability(boolean available, String reason) {
    public ProviderAvailability {
        reason = SupportOptionView.sanitizeAvailabilityReason(reason);
        if (available) {
            reason = "";
        } else if (reason.isBlank()) {
            reason = "支援组件不可用";
        }
    }

    public static ProviderAvailability present() {
        return new ProviderAvailability(true, "");
    }

    public static ProviderAvailability unavailable(String reason) {
        return new ProviderAvailability(false, reason);
    }
}
