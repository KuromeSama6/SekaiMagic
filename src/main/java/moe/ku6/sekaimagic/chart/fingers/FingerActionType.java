package moe.ku6.sekaimagic.chart.fingers;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum FingerActionType {
    TAP,
    FLICK_UP,
    FINGER_DOWN,
    FINGER_UP,
    FINGER_HOLD,
    FINGER_HOLD_SOFT
}
