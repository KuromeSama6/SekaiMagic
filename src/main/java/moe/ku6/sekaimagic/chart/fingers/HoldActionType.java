package moe.ku6.sekaimagic.chart.fingers;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum HoldActionType {
    /**
     * Start of the hold action.
     */
    START(1),
    /**
     * End of the hold action.
     */
    END(2),
    /**
     * Specifies a hard relay point. Hard relay points are connected to with straight lines.
     */
    HARD_RELAY(3),
    /**
     * Bezier curve control. Unused.
     */
    CURVE_CONTROL(4),
    /**
     * Specifies a soft relay point. Soft relay points are connected to with Bézier curves.
     */
    SOFT_RELAY(5);

    private final int id;
    public static HoldActionType FromId(int id) {
        return Arrays.stream(values())
                .filter(holdActionType -> holdActionType.getId() == id)
                .findFirst()
                .orElse(null);
    }
}
