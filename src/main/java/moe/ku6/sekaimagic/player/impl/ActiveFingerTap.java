package moe.ku6.sekaimagic.player.impl;

import moe.ku6.sekaimagic.chart.fingers.FingerCue;
import moe.ku6.sekaimagic.player.ActiveFinger;
import moe.ku6.sekaimagic.util.Vec2;

public class ActiveFingerTap extends ActiveFinger {
    public ActiveFingerTap(FingerCue cue) {
        super(cue);
    }

    @Override
    public Vec2 GetActiveWindow() {
        return new Vec2(cue.getTime(), cue.getTime() + .005); // finger held down for 10ms
    }

    @Override
    public double GetPosition(double time) {
        return cue.getPos();
    }
}
