package moe.ku6.sekaimagic.player;

import lombok.Getter;
import lombok.Setter;
import moe.ku6.sekaimagic.chart.fingers.ChainFingerCue;
import moe.ku6.sekaimagic.chart.fingers.FingerActionType;
import moe.ku6.sekaimagic.chart.fingers.FingerCue;
import moe.ku6.sekaimagic.player.impl.ActiveFingerChain;
import moe.ku6.sekaimagic.player.impl.ActiveFingerFlick;
import moe.ku6.sekaimagic.player.impl.ActiveFingerTap;
import moe.ku6.sekaimagic.util.Vec2;

/**
 * Represents a finger that is currently active in the game.
 */
public abstract class ActiveFinger {
    @Getter
    protected final FingerCue cue;
    @Getter @Setter
    private int fingerId;

    protected ActiveFinger(FingerCue cue) {
        this.cue = cue;
    }

    public abstract Vec2 GetActiveWindow();
    public abstract double GetPosition(double time);
    public double GetYOffset(double time) {
        return 0;
    }
    public double GetExitingPosition() {
        return -1;
    }

    public static ActiveFinger NewInstance(FingerCue cue) {
        if (cue instanceof ChainFingerCue chain) {
            return new ActiveFingerChain(chain);
        } else {
            return cue.getAction() == FingerActionType.FLICK_UP ? new ActiveFingerFlick(cue) : new ActiveFingerTap(cue);
        }
    }

}
