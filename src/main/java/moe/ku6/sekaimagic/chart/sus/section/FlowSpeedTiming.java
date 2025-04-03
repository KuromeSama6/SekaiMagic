package moe.ku6.sekaimagic.chart.sus.section;

import moe.ku6.sekaimagic.chart.sus.ITimingPoint;

public record FlowSpeedTiming(
        int measure,
        int tick,
        double speed
) implements ITimingPoint {
    @Override
    public int GetMeasure() {
        return measure;
    }
}
