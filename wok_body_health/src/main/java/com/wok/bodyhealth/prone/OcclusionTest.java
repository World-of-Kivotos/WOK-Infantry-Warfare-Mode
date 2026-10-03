package com.wok.bodyhealth.prone;

import net.minecraft.world.phys.Vec3;

/** Whether a block lies between a segment hit point and the body core. */
@FunctionalInterface
public interface OcclusionTest {
    /** Open space everywhere; for tests and tools without a level. */
    OcclusionTest NONE = (from, to) -> false;

    boolean blocked(Vec3 from, Vec3 to);
}
