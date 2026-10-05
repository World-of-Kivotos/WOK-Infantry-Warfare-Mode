package com.wok.infantry.client.screen;

import com.wok.infantry.deployment.DeploymentPoint;
import com.wok.infantry.deployment.DeploymentPointKind;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * "部署点方位": the viewer's deployment points plotted by X/Z inside a well (north = −Z up, one
 * scale for both axes), with a block grid, the main base's supply radius as a dashed ring and a
 * scale bar (preview {@code pointMapPanel}). Every point first claims its icon box (a main base
 * its whole ring); labels then take the first free spot (right, left, below, above) and are left
 * out when none is free, so dense beacons never print over each other; the scale bar takes the
 * first free corner. Only points of the selected point's dimension are drawn. Fill-only.
 *
 * <p>{@link #plan} is pure (text widths come in as a function) and unit-tested; points can be
 * clicked through {@link #hit}.
 */
final class DeploymentPointMap {
    /** Grid steps in blocks; the first one at least 26px wide is used. */
    static final int[] STEPS = {10, 20, 25, 50, 100, 200, 500, 1000};
    /** Half size of a point's click box. */
    static final int HIT = 7;

    private DeploymentPointMap() {
    }

    /** One point to plot. */
    record Point(UUID id, DeploymentPointKind kind, ResourceLocation dimension, int x, int z,
                 int radius, boolean selected, String label) {
    }

    /** A plotted point: screen position, ring radius (0 = none) and its label box (or EMPTY). */
    record Placed(Point point, int x, int y, int ring, UiRect label) {
    }

    /** The plan of one frame. */
    record Plan(UiRect well, double scale, int step, List<Integer> gridX, List<Integer> gridY,
                List<Placed> points, UiRect scaleBar, int scaleWidth, String scaleText) {
        Plan {
            gridX = List.copyOf(gridX);
            gridY = List.copyOf(gridY);
            points = List.copyOf(points);
        }
    }

    static Point point(SquadBoardModel.PointRow row, String label) {
        DeploymentPoint point = row.point();
        return new Point(point.id(), point.kind(), point.dimension(), point.position().getX(),
                point.position().getZ(), point.supplyRadius(), row.selected(), label);
    }

    /** The points worth plotting: those in the selected point's (else the first one's) dimension. */
    static List<Point> sameDimension(List<Point> points) {
        if (points.isEmpty()) {
            return List.of();
        }
        ResourceLocation dimension = points.stream().filter(Point::selected).findFirst()
                .orElse(points.get(0)).dimension();
        return points.stream().filter(point -> point.dimension().equals(dimension)).toList();
    }

    static Plan plan(UiRect well, List<Point> all, ToIntFunction<String> width,
                     String scaleUnit) {
        List<Point> points = sameDimension(all);
        if (points.isEmpty() || well.isEmpty()) {
            return new Plan(well, 1.0D, STEPS[0], List.of(), List.of(), List.of(), UiRect.EMPTY,
                    0, "");
        }
        int labelWidth = 0;
        for (Point point : points) {
            labelWidth = Math.max(labelWidth, width.applyAsInt(point.label()));
        }
        labelWidth += 12;
        boolean low = well.height() < 90;
        UiRect inner = new UiRect(well.left() + 16, well.top() + (low ? 9 : 14),
                well.right() - 16 - labelWidth, well.bottom() - (low ? 15 : 20));
        if (inner.width() < 24) {
            inner = new UiRect(well.left() + 16, inner.top(), well.right() - 16, inner.bottom());
        }
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (Point point : points) {
            int radius = point.kind() == DeploymentPointKind.MAIN_BASE ? point.radius() : 0;
            minX = Math.min(minX, point.x() - radius);
            maxX = Math.max(maxX, point.x() + radius);
            minZ = Math.min(minZ, point.z() - radius);
            maxZ = Math.max(maxZ, point.z() + radius);
        }
        double scale = Math.min(Math.max(1, inner.width()) / Math.max(16.0D, maxX - minX),
                Math.max(1, inner.height()) / Math.max(16.0D, maxZ - minZ));
        double centerX = (minX + maxX) / 2.0D;
        double centerZ = (minZ + maxZ) / 2.0D;
        double originX = inner.left() + inner.width() / 2.0D;
        double originZ = inner.top() + inner.height() / 2.0D;
        int step = STEPS[STEPS.length - 1];
        for (int candidate : STEPS) {
            if (candidate * scale >= 26) {
                step = candidate;
                break;
            }
        }
        List<Integer> gridX = new ArrayList<>();
        double startX = Math.ceil((centerX + (well.left() - originX) / scale) / step) * step;
        for (double gx = startX; ; gx += step) {
            int screen = (int) Math.round(originX + (gx - centerX) * scale);
            if (screen >= well.right() - 1 || gridX.size() > 256) {
                break;
            }
            if (screen > well.left()) {
                gridX.add(screen);
            }
        }
        List<Integer> gridY = new ArrayList<>();
        double startZ = Math.ceil((centerZ + (well.top() - originZ) / scale) / step) * step;
        for (double gz = startZ; ; gz += step) {
            int screen = (int) Math.round(originZ + (gz - centerZ) * scale);
            if (screen >= well.bottom() - 1 || gridY.size() > 256) {
                break;
            }
            if (screen > well.top()) {
                gridY.add(screen);
            }
        }
        List<int[]> taken = new ArrayList<>();
        List<int[]> positions = new ArrayList<>();
        for (Point point : points) {
            int x = (int) Math.round(originX + (point.x() - centerX) * scale);
            int y = (int) Math.round(originZ + (point.z() - centerZ) * scale);
            int ring = ring(point, scale);
            int reach = Math.max(8, ring + 1);
            positions.add(new int[]{x, y, ring});
            taken.add(new int[]{x - reach, y - reach, x + reach + 1, y + reach + 1});
        }
        UiRect[] labels = new UiRect[points.size()];
        List<Integer> order = new ArrayList<>();
        for (int index = 0; index < points.size(); index++) {
            order.add(index);
        }
        // The selected point's label is placed first.
        order.sort(Comparator.comparing(index -> !points.get(index).selected()));
        for (int index : order) {
            int[] position = positions.get(index);
            int reach = Math.max(8, position[2] + 1);
            int textWidth = width.applyAsInt(points.get(index).label());
            int middle = position[0] - textWidth / 2;
            int[][] spots = {
                    {position[0] + reach + 3, position[1] - 4},
                    {position[0] - reach - 3 - textWidth, position[1] - 4},
                    {middle, position[1] + reach + 2},
                    {middle, position[1] - reach - 11}};
            labels[index] = UiRect.EMPTY;
            for (int[] spot : spots) {
                int[] box = {spot[0], spot[1], spot[0] + textWidth, spot[1] + 9};
                if (box[0] >= well.left() + 3 && box[2] <= well.right() - 4
                        && box[1] >= well.top() + 2 && box[3] <= well.bottom() - 2
                        && !hits(box, taken)) {
                    labels[index] = new UiRect(box[0], box[1], box[2], box[1] + 8);
                    taken.add(box);
                    break;
                }
            }
        }
        List<Placed> placed = new ArrayList<>();
        for (int index = 0; index < points.size(); index++) {
            int[] position = positions.get(index);
            placed.add(new Placed(points.get(index), position[0], position[1], position[2],
                    labels[index]));
        }
        int scaleWidth = (int) Math.round(step * scale);
        String scaleText = scaleUnit;
        int barWidth = scaleWidth + 5 + width.applyAsInt(scaleText);
        int[][] corners = {
                {well.left() + 8, well.bottom() - 12}, {well.right() - 8 - barWidth,
                well.bottom() - 12}, {well.left() + 8, well.top() + 4},
                {well.right() - 8 - barWidth, well.top() + 4}};
        UiRect bar = UiRect.EMPTY;
        for (int[] corner : corners) {
            int[] box = {corner[0], corner[1], corner[0] + barWidth, corner[1] + 9};
            if (!hits(box, taken) && box[0] >= well.left() + 1 && box[2] <= well.right() - 1) {
                bar = new UiRect(box[0], box[1], box[2], box[3]);
                break;
            }
        }
        return new Plan(well, scale, step, gridX, gridY, placed, bar, scaleWidth, scaleText);
    }

    /** Ring radius of a main base in pixels (0 = none: a ring under 10px sits in the frame). */
    static int ring(Point point, double scale) {
        if (point.kind() != DeploymentPointKind.MAIN_BASE) {
            return 0;
        }
        double radius = point.radius() * scale;
        return radius >= 10.0D ? (int) Math.round(radius) : 0;
    }

    private static boolean hits(int[] box, List<int[]> taken) {
        for (int[] other : taken) {
            if (box[0] < other[2] && box[2] > other[0] && box[1] < other[3]
                    && box[3] > other[1]) {
                return true;
            }
        }
        return false;
    }

    /** The point under (x, y), or {@code null}. */
    static UUID hit(Plan plan, double x, double y) {
        if (plan == null) {
            return null;
        }
        for (Placed placed : plan.points()) {
            if (Math.abs(x - placed.x()) <= HIT && Math.abs(y - placed.y()) <= HIT) {
                return placed.point().id();
            }
        }
        return null;
    }

    static TacticalIcon icon(DeploymentPointKind kind) {
        return switch (kind) {
            case MAIN_BASE -> TacticalIcon.FLAG;
            case FIELD_BEACON -> TacticalIcon.DEPLOY;
            case RALLY -> TacticalIcon.RADIO;
        };
    }

    static void render(GuiGraphics graphics, Font font, Plan plan, boolean locked) {
        UiRect well = plan.well();
        TacticalDraw.well(graphics, well);
        for (int x : plan.gridX()) {
            graphics.fill(x, well.top() + 1, x + 1, well.bottom() - 1, TacticalBoardTheme.WELL_EDGE);
        }
        for (int y : plan.gridY()) {
            graphics.fill(well.left() + 1, y, well.right() - 1, y + 1, TacticalBoardTheme.WELL_EDGE);
        }
        int ringColor = locked ? TacticalBoardTheme.FAINT
                : SquadBoardBlocks.alpha(TacticalBoardTheme.SUCCESS_B, 0xA0);
        for (Placed placed : plan.points()) {
            if (placed.ring() > 0) {
                dashedRing(graphics, placed.x(), placed.y(), placed.ring(), ringColor, well);
            }
        }
        for (Placed placed : plan.points()) {
            boolean selected = placed.point().selected();
            int color = selected ? TacticalBoardTheme.SELECT_B : locked ? TacticalBoardTheme.FAINT
                    : TacticalBoardTheme.LIGHT_MUTED;
            if (selected) {
                BattleUiTheme.outline(graphics, placed.x() - 7, placed.y() - 7, placed.x() + 8,
                        placed.y() + 8, TacticalBoardTheme.SELECT_B);
            }
            icon(placed.point().kind()).draw(graphics, placed.x() - 4, placed.y() - 4, color);
        }
        for (Placed placed : plan.points()) {
            UiRect label = placed.label();
            if (label.isEmpty()) {
                continue;
            }
            boolean selected = placed.point().selected();
            TextFit.draw(graphics, font, placed.point().label(), label.left(), label.top(),
                    label.width(), selected ? TacticalBoardTheme.SELECT_B : locked
                            ? TacticalBoardTheme.FAINT : TacticalBoardTheme.LIGHT_MUTED,
                    TextFit.Align.LEFT);
        }
        UiRect bar = plan.scaleBar();
        if (!bar.isEmpty()) {
            int left = bar.left();
            int top = bar.top();
            int width = plan.scaleWidth();
            graphics.fill(left, top + 7, left + width, top + 8, TacticalBoardTheme.LIGHT_MUTED);
            graphics.fill(left, top + 4, left + 1, top + 8, TacticalBoardTheme.LIGHT_MUTED);
            graphics.fill(left + width - 1, top + 4, left + width, top + 8,
                    TacticalBoardTheme.LIGHT_MUTED);
            TextFit.draw(graphics, font, plan.scaleText(), left + width + 5, top,
                    Math.max(0, bar.right() - (left + width + 5)), TacticalBoardTheme.LIGHT_MUTED,
                    TextFit.Align.LEFT);
        }
    }

    /** Dashed midpoint circle of 1×1 fills (two dots on, one off), kept inside {@code clip}. */
    static void dashedRing(GuiGraphics graphics, int centerX, int centerY, int radius, int color,
                           UiRect clip) {
        int x = radius;
        int y = 0;
        int error = 1 - radius;
        int count = 0;
        while (x >= y) {
            if (count++ % 3 != 2) {
                int[][] offsets = {{x, y}, {y, x}, {-y, x}, {-x, y}, {-x, -y}, {-y, -x},
                        {y, -x}, {x, -y}};
                for (int[] offset : offsets) {
                    int px = centerX + offset[0];
                    int py = centerY + offset[1];
                    if (px > clip.left() && px < clip.right() - 1 && py > clip.top()
                            && py < clip.bottom() - 1) {
                        graphics.fill(px, py, px + 1, py + 1, color);
                    }
                }
            }
            y++;
            if (error < 0) {
                error += 2 * y + 1;
            } else {
                x--;
                error += 2 * (y - x) + 1;
            }
        }
    }

    /** Label of a plotted point: its title, plus "已选" / "部署于此" on the selected one. */
    static Component label(SquadBoardModel.PointRow row, boolean locked) {
        if (!row.selected()) {
            return row.title();
        }
        return SquadBoardText.t(locked ? SquadBoardText.MAP_LABEL_HERE
                : SquadBoardText.MAP_LABEL_SELECTED, row.title());
    }
}
