package moe.ku6.sekaimagic.chart.sus;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.chart.SheetLocation;
import moe.ku6.sekaimagic.exception.sus.InvalidRequestAttributeException;
import moe.ku6.sekaimagic.exception.sus.SUSParseException;
import moe.ku6.sekaimagic.chart.sus.section.TimingSection;
import moe.ku6.sekaimagic.chart.sus.section.FlowSpeedTiming;
import moe.ku6.sekaimagic.music.MusicPackage;
import moe.ku6.sekaimagic.music.Track;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.types.Commandline;
import org.jline.jansi.Ansi;

import java.util.*;

/**
 * Represents a sheet in SUS (Sliding Universal Score) format.
 */
@Slf4j
public class SUSSheet {
    @Getter
    private final Map<String, String> metadata = new HashMap<>();
    @Getter
    private int ticksPerBeat = 480;
    @Getter
    private final List<ITimingPoint> timings = new ArrayList<>();
    private final Map<Integer, BPMSection> bpmSections = new HashMap<>();
    @Getter
    private final List<RegularInstruction> regularInstructions = new ArrayList<>();
    private final Map<Integer, Double> timingPoints = new HashMap<>();
    private final List<PendingBPMSection> pendingBPMSections = new ArrayList<>();
    private final List<PendingBeatSection> pendingBeatSections = new ArrayList<>();
    @Getter
    private final MusicPackage pkg;
    @Getter
    private final Track track;
    @Getter
    private final double totalLength;

    public SUSSheet(MusicPackage pkg, Track track, List<String> data) {
        this.pkg = pkg;
        this.track = track;
        var lines = data.stream()
                .filter(c -> c.startsWith("#"))
                .map(String::trim)
                .map(c -> c.substring(1))
                .toList();

        lines.forEach(this::AddLine);

        timings.sort(Comparator.comparingInt(ITimingPoint::GetMeasure));

        FinalizeTimingSections();

        {
            // register timing points
            double time = 0;
            var points = GetTimingPoints(TimingSection.class);
            for (int i = 0; i < points.size(); i++) {
                var current = points.get(i);
                int length;

//                log.debug("{}", i);
                if (i == points.size() - 1) {
                    length = GetLastMeasure() + 1 - current.GetMeasure();

                } else {
                    var next = points.get(i + 1);
                    length = next.GetMeasure() - current.GetMeasure();
                }

                var bpm = GetBPM(current.measure());
                var beat = GetBeatsPerBar(current.measure());

                if (i == 0 && current.measure() != 0)
                    throw new SUSParseException("There must be a BPM timing point at the start of the track!");

                var bps = bpm / 60.0;
                var beats = length * beat;
                var duration = beats / bps;

//                log.debug("bpm {} beatsPerBar {} bar {} bps {} beats {} duration {}", bpm, beatsPerBar, current.measure(), bps, beats, duration);
                timingPoints.put(current.measure(), time);
                time += duration;
            }

            totalLength = time;
        }

    }

    public <T extends ITimingPoint> List<T> GetTimingPoints(Class<T> clazz) {
        return GetTimingPoints(-1, clazz);
    }
    public <T extends ITimingPoint> List<T> GetTimingPoints(int measure, Class<T> clazz) {
        return timings.stream()
                .filter(c -> clazz.isAssignableFrom(c.getClass()))
                .filter(c -> c.GetMeasure() <= measure || measure < 0)
                .map(c -> (T)c)
                .toList();
    }

    /**
     * Gets the last timing point before or at the specified measure of the specified type. Returns the default value if no timing point is found.
     * @param measure The measure to search for.
     * @param clazz The class of the timing point to search for.
     * @param def The default value to return if no timing point is found.
     * @return The last timing point before or at the specified measure of the specified type, or the default value if no timing point is found.
     * @param <T> The type of the timing point.
     */
    public <T extends ITimingPoint> T GetTimingPoint(int measure, Class<T> clazz, T def) {
        return GetTimingPoints(measure, clazz).stream()
                .min(Comparator.comparingInt(c -> -c.GetMeasure()))
                .orElse(def);
    }

    private void AddLine(String line) {
        // metadata lines
        if (!line.contains(":")) {
            var args = Commandline.translateCommandline(line);
            if (args.length < 2) return;

            if (args[0].equalsIgnoreCase("request")) {
                var reqeustArgs = Commandline.translateCommandline(args[1]);
                AddRequest(reqeustArgs[0], reqeustArgs.length >= 2 ? reqeustArgs[1] : "");
                return;
            }

            metadata.put(args[0].toLowerCase(), args[1]);
            return;
        }

        // non-metadata lines
        // 1. Check measure number
        var lineArgs = line.split(":");
        var header = lineArgs[0].trim();
        var data = line.substring(header.length() + 1).trim();

        var measureString = header.substring(0, 3);
        if (!StringUtils.isNumeric(measureString)) {
            AddSpecialInstruction(header, data);
            return;
        }

        // parse to regular instruction
        var measure = Integer.parseInt(measureString);
        var type = Integer.parseInt(header.substring(3, 4), 36);
        if (type == 0) {
            var cmd = Integer.parseInt(header.substring(4, 5), 36);
            AddControlInstruction(measure, cmd, data);
            return;
        }

        var pos = Integer.parseInt(header.substring(4, 5), 36);
        var channel = header.length() > 5 ? Integer.parseInt(line.substring(5, 6), 36) : 0;
        List<SUSDataPair> dataPairs = new ArrayList<>();

        if (data.length() % 2 != 0) {
            throw new SUSParseException("Data section must be even length");
        }

        for (int i = 0; i < data.length(); i += 2) {
            var pair = new SUSDataPair(Integer.parseInt(data.substring(i, i + 1), 36), Integer.parseInt(data.substring(i + 1, i+ 2), 36));
            dataPairs.add(pair);
        }

        AddRegularInstruction(measure, type, pos, channel, dataPairs, data);
    }

    private void AddRequest(String key, String value) {
        switch (key.toLowerCase()) {
            case "ticks_per_beat" -> {
                try {
                    ticksPerBeat = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new InvalidRequestAttributeException(key, value, "expected an integer");
                }
            }

            default -> throw new InvalidRequestAttributeException(key, value, "unknown request attribute");
        }
    }

    private void AddRegularInstruction(int measure, int type, int pos, int channel, List<SUSDataPair> data, String rawData) {
        // add notes
        var instruction = new RegularInstruction(measure, type, pos, channel, data, rawData);
        regularInstructions.add(instruction);
    }

    private void AddControlInstruction(int measure, int cmd, String data) {
        switch (cmd) {
            // Beat timing
            case 2 -> pendingBeatSections.add(new PendingBeatSection(measure, Integer.parseInt(data)));

            // BPM designation
            case 8 -> {
                int sectionId = Integer.parseInt(data);
                pendingBPMSections.add(new PendingBPMSection(measure, sectionId));
            }
            default -> throw new SUSParseException("Unknown control instruction %s".formatted(cmd));
        }
    }

    private void AddSpecialInstruction(String header, String data) {
        var instruction = header.substring(0, 3).toLowerCase();
        switch (instruction) {
            case "bpm" -> {
                int sectionId = Integer.parseInt(header.substring(3, 5));
                int bpm = Integer.parseInt(data);

                bpmSections.put(sectionId, new BPMSection(sectionId, bpm));
            }
            case "til" -> {
                var rawStr = data.replaceAll("^\"+|\"+$", "");
                if (rawStr.isEmpty()) return;

                var sections = rawStr.split(",");
                for (var str : sections) {
                    var section = str.trim();
                    var measure = Integer.parseInt(section.split("'")[0]);
                    var tick = Integer.parseInt(section.split("'")[1].split(":")[0]);
                    var speed = Double.parseDouble(section.split("'")[1].split(":")[1]);

                    timings.add(new FlowSpeedTiming(measure, tick, speed));
                }

            }
//            default -> throw new SUSParseException("Unknown special instruction: %s".formatted(instruction));
        }

    }

    private void FinalizeTimingSections() {
        if (pendingBPMSections.isEmpty())
            throw new SUSParseException("No BPM section found! Required at least one BPM section.");
        if (pendingBeatSections.isEmpty())
            pendingBeatSections.add(new PendingBeatSection(0, 4)); // default 4/4 beatsPerBar

        pendingBPMSections.sort(Comparator.comparingInt(PendingBPMSection::measure));
        pendingBeatSections.sort(Comparator.comparingInt(PendingBeatSection::measure));

        var bpm = pendingBPMSections.getFirst();
        var beat = pendingBeatSections.getFirst();
        var last = Math.max(pendingBPMSections.getLast().measure(), pendingBeatSections.getLast().measure());

        for (int i = 0; i <= last; i++) {
            final var finalI = i;
            var newBpm = pendingBPMSections.stream()
                    .filter(c -> c.measure() == finalI)
                    .findFirst()
                    .orElse(null);
            var newBeat = pendingBeatSections.stream()
                    .filter(c -> c.measure() == finalI)
                    .findFirst()
                    .orElse(null);

            if (newBeat == null && newBpm == null) {
                continue;
            }

            bpm = newBpm == null ? bpm : newBpm;
            beat = newBeat == null ? beat : newBeat;
            timings.add(new TimingSection(i, bpmSections.get(bpm.sectionId()).bpm(), beat.beat()));
        }

    }

    public int GetBPM() {
        return GetBPM(0);
    }

    public int GetBPM(int measure) {
        var ret = GetTimingPoint(measure, TimingSection.class, null);
        if (ret == null) {
            throw new SUSParseException("No BPM section found.");
        }
        return ret.bpm();
    }

    public int GetBeatsPerBar(int measure) {
        var ret = GetTimingPoint(measure, TimingSection.class, null);
        if (ret == null) {
            throw new SUSParseException("No timing section found.");
        }
        return ret.beatsPerBar();
    }

    public double GetBPS(int measure) {
        return GetBPM(measure) / 60.0;
    }

    public double GetFlowSpeed() {
        var ret = GetTimingPoint(0, FlowSpeedTiming.class, null);
        if (ret == null) return 1;
        return ret.speed();
    }

    public int GetLastMeasure() {
        return regularInstructions.stream()
                .mapToInt(RegularInstruction::measure)
                .max()
                .orElseThrow();
    }

    public double ToRealTime(SheetLocation location) {
        var timingSection = GetTimingPoint(location.measure(), TimingSection.class, null);
        if (timingSection == null)
            throw new SUSParseException("No BPM section found.");

//        log.debug("to realtime {}", location);

        var start = timingPoints.get(timingSection.GetMeasure());
        var bps = GetBPS(timingSection.GetMeasure());
        var beats = timingSection.beatsPerBar();
        var barLength = beats / bps;

        var ret = start + barLength * (location.measure() - timingSection.measure()) + location.pos() * barLength;
//        log.debug("start {} bps {} beats {} barLength {} ret {}", start, bps, beats, barLength, ret);

        return ret;
    }

    public SheetLocation ToSheetLocation(double time) {
        var timingSection = GetTimingPoint(0, TimingSection.class, null);
        if (timingSection == null)
            throw new SUSParseException("No BPM section found.");

        var start = timingPoints.get(timingSection.GetMeasure());
        var bps = GetBPS(timingSection.GetMeasure());
        var beats = timingSection.beatsPerBar();
        var barLength = beats / bps;

        var measure = (int) ((time - start) / barLength) + timingSection.measure();
        var pos = ((time - start) % barLength) / barLength;

        return new SheetLocation(measure, pos);
    }

    public String ToPrintedString() {
        var sb = new StringBuilder();
        sb.append(Ansi.ansi().fgBrightMagenta().a("SUS Metadata:").reset().newline().toString());
        metadata.forEach((key, value) -> {
            var keyStr = Ansi.ansi().fg(Ansi.Color.YELLOW).a(key).reset().toString();
            var valueStr = Ansi.ansi().fg(Ansi.Color.GREEN).a(value).reset().toString();
            sb.append(("- %s: %s\n").formatted(keyStr, valueStr));
        });
        sb.append(Ansi.ansi().a("Ticks per beatsPerBar: ").fgBrightMagenta().a(ticksPerBeat).reset().newline());
        sb.append(Ansi.ansi().a("Starting BPM: ").fgBrightMagenta().a(GetBPM()).reset().newline());
        sb.append(Ansi.ansi().a("Starting Flow Speed: ").fgBrightMagenta().a(GetFlowSpeed()).reset().newline());
        sb.append(Ansi.ansi().a("Instructions: ").fgBrightMagenta().a(regularInstructions.size()).reset().newline());
        sb.append(Ansi.ansi().fgBrightMagenta().a("Timing Sections:".formatted(timings.size())).reset().newline());

        {
            // timing sections
            for (var timing : GetTimingPoints(TimingSection.class)) {
                sb.append(Ansi.ansi().fgBrightCyan().a("| ")
                        .bg(Ansi.Color.BLUE).fg(Ansi.Color.WHITE).a("%03d".formatted(timing.GetMeasure())).reset()
                        .a(" %.3f".formatted(timingPoints.get(timing.GetMeasure()))).reset()
                        .fg(Ansi.Color.YELLOW).a(" BPM %d".formatted(GetBPM(timing.GetMeasure()))).reset()
                );
                sb.append("    ");
            }
        }

        return sb.toString();
    }
}
