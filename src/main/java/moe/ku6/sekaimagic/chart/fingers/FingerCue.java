package moe.ku6.sekaimagic.chart.fingers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import moe.ku6.sekaimagic.chart.SheetLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
public class FingerCue {
    @Getter
    private final SheetLocation location;
    @Getter
    private final double time;
    @Getter
    private final double pos;
    @Getter
    private final FingerActionType action;

    public boolean PositionEquals(FingerCue other) {
        return other.pos == pos && (other.location.equals(location) || other.time == time);
    }

    public String Serialize() {
        var ret = new ArrayList<>();
        ret.add(time);
        ret.add(location.measure());
        ret.add(location.pos());
        ret.add(pos);
        ret.add(action.ordinal());

        return ret.stream()
                .map(Objects::toString)
                .collect(Collectors.joining(","));
    }
}
