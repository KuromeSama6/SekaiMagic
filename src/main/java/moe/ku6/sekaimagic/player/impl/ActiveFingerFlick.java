package moe.ku6.sekaimagic.player.impl;

import moe.ku6.sekaimagic.chart.fingers.FingerCue;
import moe.ku6.sekaimagic.player.ActiveFinger;
import moe.ku6.sekaimagic.util.MathUtil;
import moe.ku6.sekaimagic.util.Vec2;
import org.apache.tools.ant.taskdefs.Get;

public class ActiveFingerFlick extends ActiveFinger {
    public ActiveFingerFlick(FingerCue cue) {
        super(cue);
    }

    @Override
    public Vec2 GetActiveWindow() {
        return new Vec2(cue.getTime(), cue.getTime() + .025); // flick 50 ms
    }

    @Override
    public double GetPosition(double time) {
        return cue.getPos();
    }

    @Override
    public double GetYOffset(double time) {
        var percentage = MathUtil.InverseLerp(GetActiveWindow().getX(), GetActiveWindow().getY(), time);
        return percentage * 100; // 100 px over 50 ms
    }
}
