package com.wok.commandersupport.drone;

import java.util.List;
import java.util.Objects;

/**
 * Geometry and texture layout of the recon drone: a generic medium-altitude long-endurance
 * airframe with a slender fuselage, a satcom hump, an electro-optical ball under the nose, a
 * long straight shoulder wing, an inverted-V tail with a ventral fin and a pusher propeller.
 *
 * <p>This table is plain Java on purpose: the client model is built from it, the texture was
 * painted from it, and the tests check the texture against it without loading client classes.
 * Model space follows the vanilla entity convention: one unit is 1/16 block before
 * {@link #RENDER_SCALE}, {@code -Z} is the nose, {@code +Y} points down and {@code +X} is the
 * port (left) wing. Every box uses the vanilla box UV layout ({@code 2(d+w) x (d+h)} texels at
 * {@code (u, v)}).</p>
 */
public final class ReconDroneAirframe {
    public static final int TEXTURE_WIDTH = 256;
    public static final int TEXTURE_HEIGHT = 64;
    /** One model unit is 1/8 block: the 96-unit wing spans 12 blocks. */
    public static final float RENDER_SCALE = 2.0F;
    /** Height of the fuselage axis above the entity position: the middle of the hit box. */
    public static final float MODEL_CENTER_HEIGHT = 0.6F;

    public static final String AIRFRAME = "airframe";
    public static final String STABILIZER_PORT = "stabilizer_port";
    public static final String STABILIZER_STARBOARD = "stabilizer_starboard";
    public static final String PROPELLER = "propeller";
    public static final String SENSOR = "sensor";
    public static final String NAV_PORT = "nav_port";
    public static final String NAV_STARBOARD = "nav_starboard";
    public static final String STROBE = "strobe";
    public static final String BEACON = "beacon";

    /** Parts lit by the world; the remaining parts are the lights. */
    public static final List<String> LIT_PARTS = List.of(AIRFRAME, STABILIZER_PORT,
            STABILIZER_STARBOARD, PROPELLER, SENSOR);
    public static final List<String> LIGHT_PARTS = List.of(NAV_PORT, NAV_STARBOARD, STROBE,
            BEACON);

    /** Inverted-V tail: each surface droops 40 degrees below the horizontal. */
    public static final float TAIL_ANHEDRAL_RADIANS = (float) Math.toRadians(40.0D);
    /** Pivot of the propeller on the thrust axis, behind the tail cone. */
    public static final float PROPELLER_PIVOT_Y = -0.5F;
    public static final float PROPELLER_PIVOT_Z = 25.5F;
    /** Centre of the electro-optical ball under the forward fuselage. */
    public static final float SENSOR_PIVOT_Y = 6.0F;
    public static final float SENSOR_PIVOT_Z = -15.0F;

    /** Painting hint for each box; the model itself ignores it. */
    public enum Material {
        FUSELAGE,
        NOSE,
        DOME,
        SENSOR,
        MOUNT,
        WING,
        STABILIZER,
        FIN,
        ANTENNA,
        INTAKE,
        TAILCONE,
        SPINNER,
        PROPELLER,
        LIGHT_RED,
        LIGHT_GREEN,
        LIGHT_WHITE
    }

    /** One cuboid relative to its part pivot, with its box-UV origin. */
    public record Box(Material material, int u, int v, float x, float y, float z,
                      int width, int height, int depth, boolean mirror) {
        public Box {
            Objects.requireNonNull(material, "material");
            if (width < 1 || height < 1 || depth < 1) {
                throw new IllegalArgumentException("Airframe boxes need positive sizes");
            }
        }

        public int footprintWidth() {
            return 2 * (depth + width);
        }

        public int footprintHeight() {
            return depth + height;
        }
    }

    /** One model part: pivot, static rotation (radians) and its boxes. */
    public record Part(String name, float pivotX, float pivotY, float pivotZ,
                       float xRot, float yRot, float zRot, List<Box> boxes) {
        public Part {
            Objects.requireNonNull(name, "name");
            boxes = List.copyOf(boxes);
        }
    }

    private static final List<Part> PARTS = List.of(
            new Part(AIRFRAME, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, List.of(
                    // Centre fuselage, nose, upswept tail boom and the tail cone.
                    box(Material.FUSELAGE, 0, 0, -3.0F, -3.0F, -18.0F, 6, 6, 26),
                    box(Material.NOSE, 64, 0, -2.0F, -2.0F, -24.0F, 4, 4, 6),
                    box(Material.FUSELAGE, 84, 0, -2.0F, -2.5F, 8.0F, 4, 4, 14),
                    box(Material.TAILCONE, 168, 0, -1.0F, -1.5F, 22.0F, 2, 2, 3),
                    // Satcom hump, sensor mount, blade antenna, engine air scoop, ventral fin.
                    box(Material.DOME, 120, 0, -2.0F, -5.0F, -16.0F, 4, 2, 8),
                    box(Material.MOUNT, 160, 0, -1.0F, 3.0F, -16.0F, 2, 1, 2),
                    box(Material.ANTENNA, 188, 0, -0.5F, -6.0F, -6.0F, 1, 3, 2),
                    box(Material.INTAKE, 194, 0, -1.0F, -3.5F, 9.0F, 2, 1, 4),
                    box(Material.FIN, 206, 0, -0.5F, 1.5F, 18.0F, 1, 4, 4),
                    // Shoulder wing: centre section plus two outer panels sharing one UV.
                    box(Material.WING, 0, 32, -16.0F, -3.5F, -2.0F, 32, 1, 8),
                    box(Material.WING, 80, 32, 16.0F, -3.5F, -1.0F, 32, 1, 6),
                    mirrored(Material.WING, 80, 32, -48.0F, -3.5F, -1.0F, 32, 1, 6))),
            new Part(STABILIZER_PORT, 2.0F, -1.5F, 18.0F, 0.0F, 0.0F,
                    TAIL_ANHEDRAL_RADIANS, List.of(
                    box(Material.STABILIZER, 156, 32, 0.0F, -0.5F, -3.0F, 16, 1, 6))),
            new Part(STABILIZER_STARBOARD, -2.0F, -1.5F, 18.0F, 0.0F, 0.0F,
                    -TAIL_ANHEDRAL_RADIANS, List.of(
                    mirrored(Material.STABILIZER, 156, 32, -16.0F, -0.5F, -3.0F, 16, 1, 6))),
            new Part(PROPELLER, 0.0F, PROPELLER_PIVOT_Y, PROPELLER_PIVOT_Z, 0.0F, 0.0F, 0.0F,
                    List.of(
                    box(Material.SPINNER, 178, 0, -1.5F, -1.5F, -0.5F, 3, 3, 2),
                    box(Material.PROPELLER, 120, 12, -9.0F, -1.0F, 0.0F, 18, 2, 1))),
            new Part(SENSOR, 0.0F, SENSOR_PIVOT_Y, SENSOR_PIVOT_Z, 0.0F, 0.0F, 0.0F, List.of(
                    box(Material.SENSOR, 144, 0, -2.0F, -2.0F, -2.0F, 4, 4, 4))),
            // Port red and starboard green at the wing tips, white strobe on the tail cone,
            // red anti-collision beacon under the fuselage.
            new Part(NAV_PORT, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, List.of(
                    box(Material.LIGHT_RED, 216, 0, 48.0F, -3.5F, 1.0F, 1, 1, 1))),
            new Part(NAV_STARBOARD, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, List.of(
                    box(Material.LIGHT_GREEN, 220, 0, -49.0F, -3.5F, 1.0F, 1, 1, 1))),
            new Part(STROBE, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, List.of(
                    box(Material.LIGHT_WHITE, 224, 0, -0.5F, -2.5F, 22.0F, 1, 1, 1))),
            new Part(BEACON, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, List.of(
                    box(Material.LIGHT_RED, 228, 0, -0.5F, 3.0F, 2.0F, 1, 1, 1))));

    private ReconDroneAirframe() {
    }

    public static List<Part> parts() {
        return PARTS;
    }

    public static Part part(String name) {
        for (Part part : PARTS) {
            if (part.name().equals(name)) {
                return part;
            }
        }
        throw new IllegalArgumentException("Unknown airframe part " + name);
    }

    /** Distance between the outer faces of the two wing tips, in blocks. */
    public static double wingspanBlocks() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Box box : part(AIRFRAME).boxes()) {
            if (box.material() == Material.WING) {
                min = Math.min(min, box.x());
                max = Math.max(max, box.x() + box.width());
            }
        }
        return (max - min) * RENDER_SCALE / 16.0D;
    }

    /** Nose to the rear of the propeller spinner, in blocks. */
    public static double lengthBlocks() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Part part : PARTS) {
            for (Box box : part.boxes()) {
                min = Math.min(min, part.pivotZ() + box.z());
                max = Math.max(max, part.pivotZ() + box.z() + box.depth());
            }
        }
        return (max - min) * RENDER_SCALE / 16.0D;
    }

    /** White tail strobe: two short flashes every 1.2 seconds. */
    public static boolean strobeOn(float ageInTicks) {
        float phase = positiveModulo(ageInTicks, 24.0F);
        return phase < 1.5F || (phase >= 4.0F && phase < 5.5F);
    }

    /** Red belly beacon: one longer flash, out of phase with the strobe. */
    public static boolean beaconOn(float ageInTicks) {
        float phase = positiveModulo(ageInTicks + 12.0F, 24.0F);
        return phase < 3.0F;
    }

    private static float positiveModulo(float value, float modulus) {
        float result = value % modulus;
        return result < 0.0F ? result + modulus : result;
    }

    private static Box box(Material material, int u, int v, float x, float y, float z,
                           int width, int height, int depth) {
        return new Box(material, u, v, x, y, z, width, height, depth, false);
    }

    private static Box mirrored(Material material, int u, int v, float x, float y, float z,
                                int width, int height, int depth) {
        return new Box(material, u, v, x, y, z, width, height, depth, true);
    }
}
