package com.wok.infantry.client.map;

import com.wok.infantry.battle.TacticalMarkerType;
import com.wok.infantry.client.screen.TacticalBoardTheme;
import com.wok.infantry.client.screen.TacticalTextures;
import com.wok.infantry.client.ui.probe.UiLayoutProbe;
import com.wok.infantry.deployment.DeploymentPointKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

import java.util.Locale;

/**
 * Squad-style tactical map markers (preview {@code kit/map-icons.js}): the plate's shape names
 * the category (circle = enemy intel, rounded square = order, pin = own deployment point), its
 * colour the side and the white silhouette the kind. The unconfirmed satellite contact has a
 * dashed outline.
 *
 * <p>Every marker is four blits from {@link TacticalTextures#MAP_ICONS}: the hover / selection
 * ring (only when hovered or selected), the edge layer (outline plus drop shadow) tinted
 * {@link #OUTLINE}, the plate fill tinted with the marker colour, and the white silhouette.
 * Together they reproduce the preview's {@code drawMapIcon} pixel for pixel when opaque; a faded
 * marker ({@link #EXPIRING_ALPHA}) differs only where the drop shadow lies under the outline.
 * Each call flushes pending batched fills and text and issues its own blits: it is safe inside
 * {@code drawManaged}, but it ends the batch there, so markers are not batched with fills.
 *
 * <p>Markers keep a stable physical size: one art pixel is {@link #physicalPerArt} physical
 * pixels whatever the GUI scale, the 2× tactical screen scale or the map zoom. The icon is drawn
 * in physical-pixel space and its anchor is rounded to a whole physical pixel first, so odd art
 * pixel sizes never straddle half pixels.
 */
public final class TacticalMapIcons {
    /** Edge length of one sheet cell in texels. */
    public static final int CELL = 24;
    /** Cells per sheet row. */
    public static final int COLUMNS = 5;
    /** Art pixel (0, 0), the plate's top-left, sits this many texels inside every cell. */
    public static final int ORIGIN = 3;
    /** Physical pixels per art pixel at icon scale 1.0: a 15-art-pixel plate is 30 px. */
    public static final int BASE_PHYSICAL_PER_ART = 2;
    /** Drop shadow alpha baked into the edge layers. */
    public static final int SHADOW_ALPHA = 0x50;
    public static final int OUTLINE = TacticalBoardTheme.MAP_ICON_OUTLINE;
    public static final int HOVER_RING = 0xB0FFFFFF;
    public static final int SELECTED_RING = TacticalBoardTheme.MAP_ICON_RING;
    /** Alpha of a marker about to expire (the preview's "即将过期"). */
    public static final float EXPIRING_ALPHA = 0.45F;

    // Sheet cells, in the order written by export-ui-atlas.mjs.
    static final int CELL_GLYPH_FIRST = 11;

    /** Plate shapes with their art size, anchor and sheet cells. */
    public enum Plate {
        CIRCLE(15, 15, 15, 15, 0, 1, 8),
        SQUARE(15, 15, 15, 15, 2, 3, 9),
        PIN(15, 18, 15, 36, 4, 5, 10),
        /** Circle with a dashed outline: an unconfirmed contact. Uses the circle ring. */
        CIRCLE_DASHED(15, 15, 15, 15, 6, 7, 8);

        private final int width;
        private final int height;
        private final int anchorHalfX;
        private final int anchorHalfY;
        private final int fillCell;
        private final int edgeCell;
        private final int ringCell;

        Plate(int width, int height, int anchorHalfX, int anchorHalfY, int fillCell,
              int edgeCell, int ringCell) {
            this.width = width;
            this.height = height;
            this.anchorHalfX = anchorHalfX;
            this.anchorHalfY = anchorHalfY;
            this.fillCell = fillCell;
            this.edgeCell = edgeCell;
            this.ringCell = ringCell;
        }

        /** Plate width in art pixels. */
        public int width() {
            return width;
        }

        /** Plate height in art pixels (pins include their 3-row tip). */
        public int height() {
            return height;
        }

        /** Anchor (the map point) from the plate's left edge, in half art pixels. */
        public int anchorHalfX() {
            return anchorHalfX;
        }

        /** Anchor from the plate's top edge in half art pixels: centre, or a pin's tip. */
        public int anchorHalfY() {
            return anchorHalfY;
        }

        public int fillCell() {
            return fillCell;
        }

        public int edgeCell() {
            return edgeCell;
        }

        public int ringCell() {
            return ringCell;
        }
    }

    /** The ten marker kinds, in the sheet's silhouette order. */
    public enum MapIcon {
        INFANTRY(Plate.CIRCLE, TacticalBoardTheme.MAP_ICON_HOSTILE, "marker.wok_infantry.infantry"),
        TANK(Plate.CIRCLE, TacticalBoardTheme.MAP_ICON_HOSTILE, "marker.wok_infantry.tank"),
        IFV(Plate.CIRCLE, TacticalBoardTheme.MAP_ICON_HOSTILE, "marker.wok_infantry.ifv"),
        RECON_CONTACT(Plate.CIRCLE_DASHED, TacticalBoardTheme.MAP_ICON_RECON,
                "marker.wok_infantry.recon_contact"),
        ATTACK_DIRECTION(Plate.SQUARE, TacticalBoardTheme.MAP_ICON_ATTACK,
                "marker.wok_infantry.attack_direction"),
        DEFEND(Plate.SQUARE, TacticalBoardTheme.MAP_ICON_DEFEND, "marker.wok_infantry.defend"),
        RALLY(Plate.SQUARE, TacticalBoardTheme.MAP_ICON_RALLY, "marker.wok_infantry.rally"),
        MAIN_BASE(Plate.PIN, TacticalBoardTheme.MAP_ICON_FRIENDLY,
                "marker.wok_infantry.main_base"),
        FIELD_BEACON(Plate.PIN, TacticalBoardTheme.MAP_ICON_FRIENDLY,
                "marker.wok_infantry.field_beacon"),
        RALLY_PACK(Plate.PIN, TacticalBoardTheme.MAP_ICON_FRIENDLY,
                "marker.wok_infantry.rally_pack");

        private final Plate plate;
        private final int color;
        private final String labelKey;

        MapIcon(Plate plate, int color, String labelKey) {
            this.plate = plate;
            this.color = color;
            this.labelKey = labelKey;
        }

        /** Preview name, e.g. {@code "recon_contact"}. */
        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Plate plate() {
            return plate;
        }

        /** Plate colour (opaque ARGB). */
        public int color() {
            return color;
        }

        public boolean dashed() {
            return plate == Plate.CIRCLE_DASHED;
        }

        public String labelKey() {
            return labelKey;
        }

        public Component label() {
            return Component.translatable(labelKey);
        }

        /** Sheet cell of the white silhouette. */
        public int glyphCell() {
            return CELL_GLYPH_FIRST + ordinal();
        }

        public static MapIcon of(TacticalMarkerType type) {
            if (type == null) {
                return INFANTRY;
            }
            return switch (type) {
                case RECON_CONTACT -> RECON_CONTACT;
                case INFANTRY -> INFANTRY;
                case TANK -> TANK;
                case IFV -> IFV;
                case ATTACK_DIRECTION -> ATTACK_DIRECTION;
                case DEFEND -> DEFEND;
                case RALLY -> RALLY;
            };
        }

        public static MapIcon of(DeploymentPointKind kind) {
            if (kind == null) {
                return MAIN_BASE;
            }
            return switch (kind) {
                case MAIN_BASE -> MAIN_BASE;
                case FIELD_BEACON -> FIELD_BEACON;
                case RALLY -> RALLY_PACK;
            };
        }
    }

    /** Interaction state: hover draws a translucent white ring, selection a yellow one. */
    public enum IconState {
        NORMAL,
        HOVER,
        SELECTED
    }

    /**
     * A marker placed in physical pixels: the plate's top-left corner and the size of one art
     * pixel. All coordinates are whole physical pixels.
     */
    public record Placement(Plate plate, int plateLeft, int plateTop, int artPx) {
        public int plateRight() {
            return plateLeft + plate.width() * artPx;
        }

        public int plateBottom() {
            return plateTop + plate.height() * artPx;
        }

        /** Left of the sheet cell drawn for every layer. */
        public int cellLeft() {
            return plateLeft - ORIGIN * artPx;
        }

        public int cellTop() {
            return plateTop - ORIGIN * artPx;
        }

        /** Drawn size of one sheet cell. */
        public int cellSize() {
            return CELL * artPx;
        }

        /** Hit test against the plate plus one art pixel all round, in physical pixels. */
        public boolean hit(double physicalX, double physicalY) {
            return physicalX >= plateLeft - artPx && physicalX < plateRight() + artPx
                    && physicalY >= plateTop - artPx && physicalY < plateBottom() + artPx;
        }
    }

    private TacticalMapIcons() {
    }

    /**
     * Physical pixels per art pixel for the user's icon-size factor (0.75–1.75): whole pixels,
     * {@code max(1, round(2 × factor))}. 0.75 and 1.0 both give 2, 1.25 gives 3, 1.75 gives 4.
     */
    public static int physicalPerArt(double iconScale) {
        double safe = Double.isFinite(iconScale) ? iconScale : 1.0D;
        return Math.max(1, (int) Math.round(BASE_PHYSICAL_PER_ART * safe));
    }

    /** Left texel of a sheet cell. */
    public static int cellU(int cell) {
        return cell % COLUMNS * CELL;
    }

    /** Top texel of a sheet cell. */
    public static int cellV(int cell) {
        return cell / COLUMNS * CELL;
    }

    /**
     * Places a marker whose anchor is at the given physical position. The anchor is rounded to
     * a whole physical pixel first. With an odd art pixel size the anchor's offset inside a
     * 15-wide plate (7.5 art pixels) is a half physical pixel; it is rounded down, so the plate
     * sits half a physical pixel right of (circles and squares also below) the exact centre and
     * every edge stays on a whole physical pixel.
     */
    public static Placement place(MapIcon icon, double anchorPhysicalX, double anchorPhysicalY,
                                  int artPx) {
        Plate plate = (icon == null ? MapIcon.INFANTRY : icon).plate();
        int px = Math.max(1, artPx);
        long anchorX = Math.round(anchorPhysicalX);
        long anchorY = Math.round(anchorPhysicalY);
        int left = (int) (anchorX - Math.floorDiv(plate.anchorHalfX() * px, 2));
        int top = (int) (anchorY - Math.floorDiv(plate.anchorHalfY() * px, 2));
        return new Placement(plate, left, top, px);
    }

    /**
     * Places a marker anchored at GUI coordinates (map screens hit-test with this).
     *
     * @param physicalPerGui physical pixels per GUI unit at the caller's coordinates: the window
     *                       GUI scale times any pose scale the screen applies (2 on a tactical
     *                       screen drawn at the minimum 2×)
     */
    public static Placement placeGui(MapIcon icon, double guiX, double guiY, int artPx,
                                     double physicalPerGui) {
        return place(icon, guiX * physicalPerGui, guiY * physicalPerGui, artPx);
    }

    /** Hit test in GUI coordinates; see {@link #placeGui}. */
    public static boolean hitGui(MapIcon icon, double anchorGuiX, double anchorGuiY, int artPx,
                                 double physicalPerGui, double mouseGuiX, double mouseGuiY) {
        return placeGui(icon, anchorGuiX, anchorGuiY, artPx, physicalPerGui)
                .hit(mouseGuiX * physicalPerGui, mouseGuiY * physicalPerGui);
    }

    /** Ring colour for a state, or 0 when no ring is drawn. */
    public static int ringColor(IconState state) {
        if (state == IconState.SELECTED) {
            return SELECTED_RING;
        }
        return state == IconState.HOVER ? HOVER_RING : 0;
    }

    /** Draws a fully opaque marker anchored at (x, y) in the current pose's GUI coordinates. */
    public static void draw(GuiGraphics graphics, MapIcon icon, float x, float y, int artPx,
                            IconState state) {
        draw(graphics, icon, x, y, artPx, state, 1.0F);
    }

    /**
     * Draws a marker anchored at (x, y) in the current pose's GUI coordinates. The pose may
     * translate and scale (map pan, the tactical screen's 2×) but must not rotate; the window
     * GUI scale and the pose scale together give the physical pixel grid the icon snaps to.
     *
     * @param artPx physical pixels per art pixel, normally {@link #physicalPerArt}
     * @param alpha whole-marker opacity, {@link #EXPIRING_ALPHA} for a marker about to expire
     */
    public static void draw(GuiGraphics graphics, MapIcon icon, float x, float y, int artPx,
                            IconState state, float alpha) {
        if (icon == null || !(alpha > 0.0F)) {
            return;
        }
        Matrix4f pose = graphics.pose().last().pose();
        float scaleX = pose.m00();
        float scaleY = pose.m11();
        float translateX = pose.m30();
        float translateY = pose.m31();
        if (!(scaleX > 0.0F) || !(scaleY > 0.0F)) {
            return;
        }
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        Placement placement = place(icon, physical(x, scaleX, translateX, guiScale),
                physical(y, scaleY, translateY, guiScale), artPx);
        if (UiLayoutProbe.recording()) {
            UiLayoutProbe.icon(icon.id(), state == null ? "NORMAL" : state.name(),
                    placement.plateLeft(), placement.plateTop(), placement.plateRight(),
                    placement.plateBottom(), guiScale);
        }
        graphics.pose().pushPose();
        // From here on one unit is one physical pixel at the screen origin: a point P lands at
        // scale * (-translate / scale + P / (guiScale * scale)) + translate = P / guiScale.
        graphics.pose().translate(-translateX / scaleX, -translateY / scaleY, 0.0F);
        graphics.pose().scale((float) (1.0D / (guiScale * scaleX)),
                (float) (1.0D / (guiScale * scaleY)), 1.0F);
        int left = placement.cellLeft();
        int top = placement.cellTop();
        int size = placement.cellSize();
        TacticalTextures.begin(graphics);
        int ring = ringColor(state);
        if (ring != 0) {
            layer(graphics, icon.plate().ringCell(), left, top, size, ring, alpha);
        }
        layer(graphics, icon.plate().edgeCell(), left, top, size, OUTLINE, alpha);
        layer(graphics, icon.plate().fillCell(), left, top, size, icon.color(), alpha);
        layer(graphics, icon.glyphCell(), left, top, size, 0xFFFFFFFF, alpha);
        TacticalTextures.end(graphics);
        graphics.pose().popPose();
    }

    /** Physical-pixel coordinate of a GUI coordinate under a pose axis scale and translation. */
    static double physical(float coordinate, float scale, float translation, double guiScale) {
        return ((double) scale * coordinate + translation) * guiScale;
    }

    private static void layer(GuiGraphics graphics, int cell, int left, int top, int size,
                              int argb, float alpha) {
        TacticalTextures.tint(graphics, argb, alpha);
        graphics.blit(TacticalTextures.MAP_ICONS, left, top, size, size, cellU(cell), cellV(cell),
                CELL, CELL, TacticalTextures.MAP_ICONS_SIZE, TacticalTextures.MAP_ICONS_SIZE);
    }
}
