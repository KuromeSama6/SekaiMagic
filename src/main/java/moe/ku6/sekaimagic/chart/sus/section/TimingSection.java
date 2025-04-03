package moe.ku6.sekaimagic.chart.sus.section;

import moe.ku6.sekaimagic.chart.sus.ITimingPoint;

public record TimingSection(
        int measure,
        int bpm,
        int beatsPerBar
) implements ITimingPoint {
    @Override
    public int GetMeasure() {
        return measure;
    }
}
