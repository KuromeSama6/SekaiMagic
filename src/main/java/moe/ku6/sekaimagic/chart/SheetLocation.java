package moe.ku6.sekaimagic.chart;

import java.util.Objects;

public record SheetLocation(
        int measure,
        double pos
) {
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SheetLocation that = (SheetLocation)o;
        return Double.compare(pos, that.pos) == 0 && measure == that.measure;
    }

    @Override
    public int hashCode() {
        return Objects.hash(measure, pos);
    }
}
