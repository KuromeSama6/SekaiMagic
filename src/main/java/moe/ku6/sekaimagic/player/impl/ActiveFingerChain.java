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
            if (cue.getAction() != FingerActionType.FINGER_HOLD_SOFT || true) {
                // if the cue is not a soft cue, it is the end of the segment
                break;
            }
        }

        candidates.sort(Comparator.comparingDouble(FingerCue::getTime));
        var points = candidates.stream()
                .mapToDouble(FingerCue::getPos)
                .boxed()
                .toList();

        return EvaluateBezier(points, MathUtil.InverseLerp(candidates.getFirst().getTime(), candidates.getLast().getTime(), time));
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
}
