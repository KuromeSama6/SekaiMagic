package moe.ku6.sekaimagic.chart.fingers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import moe.ku6.sekaimagic.chart.SheetLocation;
import moe.ku6.sekaimagic.util.Vec2;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
public class FingerCue {
    @Getter
    private final SheetLocation location;
    @Getter @Setter
    private double time;
    @Getter @Setter
    private double pos;
    @Getter
    private double width;
    @Getter
    private final FingerActionType action;

    public FingerCue(FingerCue other) {
        this(other.location, other.time, other.pos, other.width, other.action);
    }

    public boolean PositionEquals(FingerCue other) {
        return other.pos == pos && (other.location.equals(location) || other.time == time);
    }

    public Vec2 GetRange() {
        return new Vec2(pos - width / 2.0, pos + width / 2.0);
    }

    public String Serialize() {
        var ret = new ArrayList<>();
        ret.add(time);
        ret.add(location.measure());
        ret.add(location.pos());
        ret.add(pos);
        ret.add(width);
        ret.add(action.ordinal());

        return ret.stream()
                .map(Objects::toString)
                .collect(Collectors.joining(","));
    }
}
