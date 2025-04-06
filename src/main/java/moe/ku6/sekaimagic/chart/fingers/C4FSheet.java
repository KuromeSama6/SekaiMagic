package moe.ku6.sekaimagic.chart.fingers;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.chart.SheetLocation;
import moe.ku6.sekaimagic.chart.sus.RegularInstruction;
import moe.ku6.sekaimagic.chart.sus.SUSSheet;
import moe.ku6.sekaimagic.exception.sus.SUSParseException;
import moe.ku6.sekaimagic.util.Vec2;

import java.util.*;

@Slf4j
public class C4FSheet {
    @Getter
    private final List<FingerCue> cues = new ArrayList<>();
    private final SUSSheet originalSheet;

    /**
     * Creates a new Chart for Fingers (C4F) sheet from the given SUS sheet.
     * @param sheet The input sheet.
     */
    public C4FSheet(SUSSheet sheet) {
        originalSheet = sheet;

        {
            // single notes first
            for (var instruction : sheet.getRegularInstructions()) {
                switch (instruction.noteType()) {
                    case 1, 5 -> AddSingleNote(instruction);
                }
            }
        }

        // hold notes
        {
            Map<Integer, ChainFingerCue> openGroups = new HashMap<>();
            var instructions = sheet.getRegularInstructions().stream()
                    .filter(c -> c.noteType() == 3)
                    .toList();

            for (var instruction : instructions) {
                var groupId = instruction.group();
                double barSplit = instruction.data().size();

                for (int i = 0; i < instruction.data().size(); i++) {
                    var pair = instruction.data().get(i);
                    if (pair.IsEmpty()) continue;
                    var loc = new SheetLocation(instruction.measure(), 1.0 / barSplit * i);
                    var time = originalSheet.ToRealTime(loc);
                    var tapPos = (instruction.lane() - 2) + pair.width() / 2.0;

                    var action = HoldActionType.FromId(pair.data());
                    if (action == null)
                        throw new SUSParseException("Unknown hold action type: %d".formatted(pair.data()));

                    switch (action) {
                        case START -> {
                            if (openGroups.containsKey(groupId)) {
                                // groups are committed on the next start
                                var current = openGroups.get(groupId);
                                cues.add(current);
                                openGroups.remove(groupId);
                            }

                            var chain = new ChainFingerCue(loc, time, tapPos, pair.width());
                            openGroups.put(groupId, chain);
                        }
                        case SOFT_RELAY, HARD_RELAY -> {
                            var chain = openGroups.get(groupId);
                            if (chain == null)
                                throw new SUSParseException("Could not add a relay to a group that is not open (%d) (at %s)".formatted(groupId, instruction));

                            chain.Add(new FingerCue(loc, time, tapPos, pair.width(), action == HoldActionType.SOFT_RELAY ? FingerActionType.FINGER_HOLD_SOFT : FingerActionType.FINGER_HOLD), true);
                        }
                        case END -> {
                            if (!openGroups.containsKey(groupId))
                                throw new SUSParseException("Could not close a group that is not open (%d)".formatted(groupId));
                            var chain = openGroups.get(groupId);
                            chain.Add(new FingerCue(loc, time, tapPos, pair.width(), FingerActionType.FINGER_UP), true);
                        }
                    }

                }
            }

            // commit all open groups
            cues.addAll(openGroups.values());
        }

        cues.sort(Comparator.comparingDouble(FingerCue::getTime));
        for (var cue : cues) {
            if (cue instanceof ChainFingerCue chain) {
                PruneHoldNotes(chain);
            }
        }
//        GenerateHelperTouches();
    }

    private void AddSingleNote(RegularInstruction instruction) {
        if (originalSheet == null) {
            throw new IllegalStateException("Original sheet is not set.");
        }

        // Sekai uses lane 2~d; lanes 0, 1, e, f are control lanes
        if (instruction.lane() < 2 || instruction.lane() > 13) {
            return;
        }

        var data = instruction.data();
        double barSplit = data.size();
        for (int i = 0; i < data.size(); i++) {
            var pair = data.get(i);
            if (pair.data() == 0 || pair.width() == 0) continue;

            var loc = new SheetLocation(instruction.measure(), 1.0 / barSplit * i);
            var time = originalSheet.ToRealTime(loc);

            // find tap pos
            var tapPos = (instruction.lane() - 2) + pair.width() / 2.0;

            FingerCue note = new FingerCue(loc, time, tapPos, pair.width(), instruction.noteType() == 1 ? FingerActionType.TAP : FingerActionType.FLICK_UP);
            // Add note to the chart
            cues.add(note);
        }
    }

    public void GenerateHelperTouches() {
        for (var cue : cues) {
            if (cue instanceof ChainFingerCue chain) {
                chain.Add(GenerateHelperTouch(chain, 1, .2), false);
                chain.Add(GenerateHelperTouch(chain, -1, .2), false);
                chain.Sort();
            }
        }
    }

    private void PruneHoldNotes(ChainFingerCue chain) {
        log.debug("------ prune: hold: {}", chain.getLocation());
        var bpm = originalSheet.GetBPM(chain.getLocation().measure());
        var bps = originalSheet.GetBPS(chain.getLocation().measure());
        var secondsPerBeat = 1.0 / bps;

        log.debug("prune: bpm: {} bps: {}, spb: {}", bpm, bps, secondsPerBeat);
        var cues = chain.getCues();
        if (cues.size() > 1) {
            for (int i = 0; i < cues.size() - 1; i++) {
                var current = cues.get(i);
                var next = cues.get(i + 1);

                Vec2 currentRange = current.GetRange();
                Vec2 nextRange = next.GetRange();

                if (next.getTime() - current.getTime() < secondsPerBeat / 8.0 && currentRange.Overlaps(nextRange)) {
                    log.debug("prune: warn: cues too close (0: #{}@{}, 1: #{}@{})", i, current.getLocation(), i + 1, next.getLocation());
                }
            }
        }

    }

    public String Serialize() {
        var sb = new StringBuilder();
        sb.append(";C4F - For your fingers\n");
        sb.append(";This file is generated by SekaiMagic %s\n".formatted(getClass().getPackage().getImplementationVersion()));
        sb.append("#vendor SekaiMagic\n");
        sb.append("#package %d\n".formatted(originalSheet.getPkg().getId()));
        sb.append("#difficulty %s\n".formatted(originalSheet.getTrack().getDifficulty()));

        for (var note : getCues()) {
            sb.append(note.Serialize());
            sb.append("\n");
        }
        return sb.toString();
    }

    private static ChainFingerCue GenerateHelperTouch(ChainFingerCue cue, double offset, double timeOffset) {
        var ret = new ChainFingerCue(cue);
        ret.setPos(ret.getPos() + offset);
        ret.setTime(ret.getTime() + timeOffset);

        ret.getCues().forEach(c -> {
            c.setPos(c.getPos() + offset);
            c.setTime(c.getTime() + timeOffset);
        });

        return ret;
    }
}
