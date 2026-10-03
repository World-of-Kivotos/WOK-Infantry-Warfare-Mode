package com.wok.commandersupport.artillery;

import java.util.List;
import java.util.Objects;

/** What one mission step does: rounds that land now and rounds whose whistle starts now. */
record ArtilleryStep(int index, List<ArtilleryRound> impacts, List<ArtilleryRound> whistles) {
    ArtilleryStep {
        if (index < 0) {
            throw new IllegalArgumentException("Artillery step cannot be negative");
        }
        impacts = List.copyOf(Objects.requireNonNull(impacts, "impacts"));
        whistles = List.copyOf(Objects.requireNonNull(whistles, "whistles"));
    }

    boolean isEmpty() {
        return impacts.isEmpty() && whistles.isEmpty();
    }
}
