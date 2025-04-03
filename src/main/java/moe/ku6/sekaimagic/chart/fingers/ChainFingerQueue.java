package moe.ku6.sekaimagic.chart.fingers;

import lombok.Getter;
import moe.ku6.sekaimagic.chart.SheetLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Used to represent long and hold notes.
 */
public class ChainFingerQueue extends FingerCue {
    @Getter
    private final List<FingerCue> cues = new ArrayList<>();

    public ChainFingerQueue(SheetLocation location, double time, double pos) {
        super(location, time, pos, FingerActionType.FINGER_DOWN);
        cues.add(this);
    }

    public void Add(FingerCue cue) {
        if (cues.isEmpty()) return;
        cues.add(cue);

        cues.sort(Comparator.comparingDouble(FingerCue::getTime));
    }

    @Override
    public String Serialize() {
        var ret = new StringBuilder().append(super.Serialize());
        for (int i = 1; i < cues.size(); i++) {
            ret.append("|");
            ret.append(cues.get(i).Serialize());
        }

        return ret.toString();
    }
}
