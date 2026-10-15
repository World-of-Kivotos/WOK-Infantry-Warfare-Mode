package com.wok.infantry.formation;

/** Administrator mutations supported by the formation-scoped loadout editor. */
public enum FormationLoadoutEditAction {
    /** Captured/new entry: create a strict list when unrestricted, otherwise append it. */
    INCLUDE_CAPTURED,
    /** Replace the slot allow-list with this single entry. */
    EXCLUSIVE,
    /** Append an entry to an existing strict allow-list. */
    ADD,
    /** Remove an entry from a strict allow-list while keeping at least one choice. */
    REMOVE,
    /** Remove the slot restriction so every global entry is allowed. */
    ALLOW_ALL
}
