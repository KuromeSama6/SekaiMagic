package moe.ku6.sekaimagic.chart.fingers;

import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.util.Vec2;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;

import java.awt.*;
import java.awt.geom.Path2D;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class FingerPathDrawer implements Closeable {
    private final C4FSheet sheet;
    private final int COLUMN_WIDTH, MEASURE_HEIGHT, MEASURES_PER_COLUMN, LANE_PADDING, TOTAL_COLUMNS;
    private final int WIDTH, HEIGHT;
    private final SVGGraphics2D graphics;

    public FingerPathDrawer(C4FSheet sheet) {
        this(sheet, 30 * 12, 500, 4, 50);
    }

    public FingerPathDrawer(C4FSheet sheet, int laneWidth, int measureHeight, int measuresPerColumn, int lanePadding) {
        if (sheet.getOriginalSheet() == null)
            throw new IllegalArgumentException("sheet.getOriginalSheet() == null");

        this.sheet = sheet;
        COLUMN_WIDTH = laneWidth;
        MEASURE_HEIGHT = measureHeight;
        MEASURES_PER_COLUMN = measuresPerColumn;
        LANE_PADDING = lanePadding;

        var sus = sheet.getOriginalSheet();
        var measures = sus.GetLastMeasure() + 1;
        TOTAL_COLUMNS = (int) Math.ceil((double) measures / MEASURES_PER_COLUMN);

        WIDTH = TOTAL_COLUMNS * laneWidth + (TOTAL_COLUMNS - 1) * lanePadding;
        HEIGHT = measureHeight * measuresPerColumn;

        graphics = new SVGGraphics2D(WIDTH, HEIGHT + 40);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setBackground(Color.BLACK);
    }

    public void Draw() {
        var sus = sheet.getOriginalSheet();
        var track = sus.getTrack();
        var pkg = sus.getPkg();
        
        // Draw background
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, WIDTH, HEIGHT + 40);

        {
            int measure = 0;
            for (int i = 0; i < TOTAL_COLUMNS; i++) {
                // draw columns
                graphics.setColor(Color.WHITE);
                graphics.setStroke(new BasicStroke(1));
                int x = i * (COLUMN_WIDTH + LANE_PADDING);
                graphics.drawRect(x, 0, COLUMN_WIDTH, HEIGHT);

                // draw measure lines and write measure numbers
                for (int j = 0; j < MEASURES_PER_COLUMN; j++) {
                    int y = HEIGHT - j * MEASURE_HEIGHT;
                    graphics.setColor(Color.GRAY);
                    graphics.drawLine(x, y, x + COLUMN_WIDTH, y);

                    graphics.setColor(Color.YELLOW);
                    graphics.setFont(new Font("Arial", Font.PLAIN, 16));
                    graphics.drawString("%d".formatted(measure), x + 5, y - 5);
                    ++measure;
                }

                // draw lanes
                for (int j = 0; j < 12; j++) {
                    int lineX = x + j * COLUMN_WIDTH / 12;
                    graphics.setColor(Color.DARK_GRAY);
                    graphics.drawLine(lineX, 0, lineX, HEIGHT);
                }
            }
        }

        // draw notes
        {
            for (var cue : sheet.getCues().stream().sorted(Comparator.comparingInt(c -> c.getAction() == FingerActionType.FLICK_UP ? 0 : 1)).toList()) {
                // taps / flicks
                if (cue instanceof ChainFingerCue chain) {
                    var subCues = chain.getCues();
                    for (int i = 0; i < subCues.size(); i++) {
                        graphics.setStroke(new BasicStroke(2));
                        var subCue = subCues.get(i);
                        var drawPos = GetDrawPosition(subCue);
                        int x = (int)drawPos.getX();
                        int y = (int)drawPos.getY();
                        var width = cue.getWidth();
                        var lineSize = width * (COLUMN_WIDTH / 12.0) / 2.0;

                        if (i == 0) {
                            graphics.setStroke(new BasicStroke(1));
                            graphics.setColor(Color.GREEN);
                            DrawCrossBottomHalf(x, HEIGHT - y, 5);
                            graphics.drawLine((int)(x - lineSize), HEIGHT - y, (int)(x + lineSize), HEIGHT - y);

                        } else if (i == subCues.size() - 1) {
                            graphics.setStroke(new BasicStroke(1));
                            graphics.setColor(Color.GREEN);
                            DrawCrossTopHalf(x, HEIGHT - y, 5);
                            graphics.drawLine((int)(x - lineSize), HEIGHT - y, (int)(x + lineSize), HEIGHT - y);
                            continue;

                        } else {
                            graphics.setColor(Color.GREEN);
                            if (subCue.getAction() == FingerActionType.FINGER_HOLD_SOFT) {
                                graphics.fillOval(x - 4, HEIGHT - y - 4, 8, 8);
                            } else {
                                graphics.fillRect(x - 4, HEIGHT - y - 4, 8, 8);
                            }
                        }

                        var nextCue = subCues.get(i + 1);
                        var nextPos = GetDrawPosition(nextCue);
                        int columnDiff = nextCue.getLocation().measure() / MEASURES_PER_COLUMN - subCue.getLocation().measure() / MEASURES_PER_COLUMN;

                        graphics.setStroke(new BasicStroke(1));

                        {
                            graphics.setColor(Color.GREEN);
                            if (columnDiff != 0) {
                                graphics.drawLine(x, HEIGHT - y, (int)nextPos.getX() - columnDiff * (COLUMN_WIDTH + LANE_PADDING), 0);
                                graphics.drawLine((int)nextPos.getX(), HEIGHT, (int)nextPos.getX(), Math.max(0, HEIGHT - (int)nextPos.getY()));
                            } else {
                                graphics.drawLine(x, HEIGHT - y, (int)nextPos.getX(), Math.max(0, HEIGHT - (int)nextPos.getY()));
                            }
                        }

                    }

                    // draw curve
                    {
                        List<Vec2> points = new ArrayList<>();
                        for (int i = 0; i < subCues.size(); i++) {
                            var subCue = subCues.get(i);
                            if (points.isEmpty()) {
                                points.add(GetDrawPosition(subCue));
                                continue;
                            }

                            if (subCue.getAction() == FingerActionType.FINGER_HOLD_SOFT) {
                                points.add(GetDrawPosition(subCue));

                            } else {
                                if (points.size() <= 1) {
                                    // not enough points to draw a curve
                                    points.clear();
                                    continue;
                                } else {
                                    points.add(GetDrawPosition(subCue));
                                    var toDraw = points.stream()
                                            .map(c -> new Vec2(c.x, HEIGHT - c.y))
                                            .toList();
                                    Path2D path = new Path2D.Double();
                                    DrawFittedCurve(path, toDraw);
                                    graphics.setColor(Color.CYAN);
                                    graphics.draw(path);
                                    points.clear();
                                }
                            }

                        }
                    }

                } else {
                    graphics.setColor(cue.getAction() == FingerActionType.FLICK_UP ? Color.PINK : Color.WHITE);

                    var pos = GetDrawPosition(cue);
                    int x = (int)pos.getX();
                    int y = (int)pos.getY();

                    if (cue.getAction() == FingerActionType.FLICK_UP) {
                        DrawTriangle(x, HEIGHT - y, 7);
                    } else {
                        DrawCross(x, HEIGHT - y, 5);
                    }

                    graphics.setStroke(new BasicStroke(1));
                    var width = cue.getWidth();
                    var lineSize = width * (COLUMN_WIDTH / 12.0) / 2.0;
                    graphics.drawLine((int)(x - lineSize), HEIGHT - y, (int)(x + lineSize), HEIGHT - y);
                }
            }
        }

        // draw package name
        graphics.setColor(Color.decode("#" + track.getDifficulty().getColor()));
        graphics.setFont(new Font("Arial", Font.PLAIN, 16));
        graphics.drawString("[%03d]%s %s%d".formatted(
                pkg.getId(),
                pkg.getTitle(),
                track.getDifficulty(),
                pkg.getPlayLevels().get(track.getDifficulty())
        ), 20, HEIGHT + 20);
    }

    private void DrawFittedCurve(Path2D path, List<Vec2> points) {
        for (int i = 0; i < points.size() - 1; i++) {
            Vec2 p0 = i > 0 ? points.get(i - 1) : points.get(i);
            Vec2 p1 = points.get(i);
            Vec2 p2 = points.get(i + 1);
            Vec2 p3 = (i + 2 < points.size()) ? points.get(i + 2) : p2;

            // Catmull-Rom to Cubic Bezier conversion
            double c1x = p1.x + (p2.x - p0.x) / 6.0;
            double c1y = p1.y + (p2.y - p0.y) / 6.0;

            double c2x = p2.x - (p3.x - p1.x) / 6.0;
            double c2y = p2.y - (p3.y - p1.y) / 6.0;

            if (i == 0) {
                path.moveTo(p1.x, p1.y);
            }

            path.curveTo(c1x, c1y, c2x, c2y, p2.x, p2.y);
        }
    }

    private Vec2 GetDrawPosition(FingerCue cue) {
        var loc = cue.getLocation();
        var pos = cue.getPos();
        var measure = loc.measure();

        int column = (measure / MEASURES_PER_COLUMN);
        int columnX = column * (COLUMN_WIDTH + LANE_PADDING);
        int x = columnX + (int)((pos / 12.0) * COLUMN_WIDTH);
        int y = loc.measure() % MEASURES_PER_COLUMN * MEASURE_HEIGHT + (int)(loc.pos() * MEASURE_HEIGHT);

        return new Vec2(x, y);
    }

    private void DrawCross(int x, int y, int size) {
        graphics.drawLine(x - size, y - size, x + size, y + size);
        graphics.drawLine(x - size, y + size, x + size, y - size);
    }

    private void DrawCrossBottomHalf(int x, int y, int size) {
        graphics.drawLine(x, y, x + size, y + size);
        graphics.drawLine(x, y, x - size, y + size);
    }

    private void DrawCrossTopHalf(int x, int y, int size) {
        graphics.drawLine(x, y, x + size, y - size);
        graphics.drawLine(x, y, x - size, y - size);
    }

    private void DrawTriangle(int x, int y, int size) {
        int[] xPoints = {x, x - size, x + size};
        int[] yPoints = {y - size, y + size, y + size};
        graphics.fillPolygon(xPoints, yPoints, 3);
    }

    public void Export(File file) throws IOException {
        SVGUtils.writeToSVG(file, graphics.getSVGElement());
    }

    @Override
    public void close() throws IOException {
        graphics.dispose();
    }
}
