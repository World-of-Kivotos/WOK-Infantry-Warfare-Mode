package com.wok.infantry.client.map;

/**
 * Client-only tactical-map meaning for a registered commander support capability.
 *
 * <p>The support definition remains server-authoritative. This presentation hint only controls
 * how the already validated radius is communicated on the map.</p>
 */
public enum TacticalSupportMapPresentation {
    UTILITY,
    INTELLIGENCE,
    OFFENSIVE
}
