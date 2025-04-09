package moe.ku6.sekaimagic.player.impl;

import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.chart.fingers.ChainFingerCue;
import moe.ku6.sekaimagic.chart.fingers.FingerActionType;
import moe.ku6.sekaimagic.chart.fingers.FingerCue;
import moe.ku6.sekaimagic.player.ActiveFinger;
import moe.ku6.sekaimagic.util.MathUtil;
import moe.ku6.sekaimagic.util.Vec2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class ActiveFingerChain extends ActiveFinger {
    private final ChainFingerCue chain;

    public ActiveFingerChain(ChainFingerCue cue) {
        super(cue);
        this.chain = cue;
    }

    @Override
    public Vec2 GetActiveWindow() {
        return new Vec2(chain.getCues().getFirst().getTime(), chain.getCues().getLast().getTime() + 0.015);
    }

    @Override
    public double GetPosition(double time) {
        if (chain.getCues().isEmpty()) {
            return -1;
        }
        var first = chain.getCues().getFirst();
        if (chain.getCues().size() == 1) return first.getPos();

        /*
        A chain is divided into multiple segments. Each segment starts from the most recent non-soft cue that is before the current time, ends with the next hard cue, and contains all soft cues in between.
         */

        List<FingerCue> candidates = new ArrayList<>();

        // find the start of the segment
        var start = chain.getCues().stream()
                .filter(c -> c.getTime() <= time)
                .max(Comparator.comparingDouble(FingerCue::getTime))
                .orElse(null);
        if (start == null) {
            log.warn("No segment start found for chain at time {}", time);
            return first.getPos();
        }

        candidates.add(start);

        int index = chain.getCues().indexOf(start);
        if (index == chain.getCues().size() - 1) {
            // if the start is the last cue, return its position
            return start.getPos();
        }

        for (int i = index + 1; i < chain.getCues().size(); i++) {
            var cue = chain.getCues().get(i);
            candidates.add(cue);
            if (cue.getAction() != FingerActionType.FINGER_HOLD_SOFT) {
                // if the cue is not a soft cue, it is the end of the segment
                break;
            }
        }

        candidates.sort(Comparator.comparingDouble(FingerCue::getTime));
        var points = candidates.stream()
                .map(c -> new Vec2(c.getPos(), c.getTime()))
                .toList();

        return EvaluateCatmullRom(points, MathUtil.InverseLerp(candidates.getFirst().getTime(), candidates.getLast().getTime(), time)).x;
    }

    @Override
    public double GetExitingPosition() {
        return chain.getCues().getLast().getPos();
    }

    public static double EvaluateBezier(List<Double> points, double t) {
        if (points == null || points.isEmpty()) {
            return -1;
        }
        if (points.size() == 1) {
            return points.getFirst();
        }
        if (points.size() == 2) {
            return MathUtil.Lerp(points.get(0), points.get(1), t);
        }

        // Recursive De Casteljau’s algorithm
        return EvaluateBezierStep(points, t);
    }

    private static double EvaluateBezierStep(List<Double> points, double t) {
        if (points.size() == 1) {
            return points.getFirst();
        }

        // Interpolate between successive points
        List<Double> nextLevel = new java.util.ArrayList<>();
        for (int i = 0; i < points.size() - 1; i++) {
            double a = points.get(i);
            double b = points.get(i + 1);
            nextLevel.add(MathUtil.Lerp(a, b, t));
        }

        return EvaluateBezierStep(nextLevel, t);
    }

    public static Vec2 EvaluateCatmullRom(List<Vec2> points, double t) {
        if (points.size() == 1) {
            return points.getFirst();
        }

        int n = points.size();
        if (n < 2) throw new IllegalArgumentException("At least 2 points required");

        // How many Bezier segments we'll have
        int segments = n - 1;

        // Clamp t to [0, 1]
        t = Math.max(0, Math.min(1, t));

        // Map t to segment index and local t
        double scaledT = t * segments;
        int seg = (int)Math.min(Math.floor(scaledT), segments - 1);
        double localT = scaledT - seg;

        // Get 4 points for Catmull-Rom
        Vec2 p0 = (seg > 0) ? points.get(seg - 1) : points.get(seg);
        Vec2 p1 = points.get(seg);
        Vec2 p2 = points.get(seg + 1);
        Vec2 p3 = (seg + 2 < n) ? points.get(seg + 2) : p2;

        // Convert to Bezier control points
        Vec2 c0 = p1;
        Vec2 c1 = new Vec2(
                p1.x + (p2.x - p0.x) / 6.0,
                p1.y + (p2.y - p0.y) / 6.0
        );
        Vec2 c2 = new Vec2(
                p2.x - (p3.x - p1.x) / 6.0,
                p2.y - (p3.y - p1.y) / 6.0
        );
        Vec2 c3 = p2;

        // De Casteljau's algorithm for cubic Bezier
        Vec2 a = Vec2.Lerp(c0, c1, localT);
        Vec2 b = Vec2.Lerp(c1, c2, localT);
        Vec2 c = Vec2.Lerp(c2, c3, localT);

        Vec2 d = Vec2.Lerp(a, b, localT);
        Vec2 e = Vec2.Lerp(b, c, localT);

        return Vec2.Lerp(d, e, localT);
    }
}
