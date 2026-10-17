package com.wok.bodyhealth.prone;

/** The six player-model cubes that make up a prone hit layout. Names are the player's own left/right. */
public enum SegmentId {
    HEAD("head"),
    TORSO("torso"),
    RIGHT_ARM("rightArm"),
    LEFT_ARM("leftArm"),
    RIGHT_LEG("rightLeg"),
    LEFT_LEG("leftLeg");

    /** Order between segments whose entry parameters differ by less than 1e-9; earlier wins. */
    public static final SegmentId[] TIE_ORDER = {TORSO, HEAD, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG};

    private static final int[] TIE_RANK = new int[values().length];

    static {
        // Ranked from a private copy so a caller writing into the public array cannot reorder ties.
        SegmentId[] order = TIE_ORDER.clone();
        for (int i = 0; i < order.length; i++) {
            TIE_RANK[order[i].ordinal()] = i;
        }
    }

    private final String jsonKey;

    SegmentId(String jsonKey) {
        this.jsonKey = jsonKey;
    }

    /** Key of this segment in the TAA segment table JSON. */
    public String jsonKey() {
        return jsonKey;
    }

    int tieRank() {
        return TIE_RANK[ordinal()];
    }
}
