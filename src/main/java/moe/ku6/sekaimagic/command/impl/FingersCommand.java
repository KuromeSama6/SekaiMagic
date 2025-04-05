package moe.ku6.sekaimagic.command.impl;

import com.beust.jcommander.Parameter;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.SekaiMagic;
import moe.ku6.sekaimagic.chart.fingers.C4FSheet;
import moe.ku6.sekaimagic.command.ICommand;
import moe.ku6.sekaimagic.data.SekaiDatabase;
import moe.ku6.sekaimagic.music.Track;
import moe.ku6.sekaimagic.music.TrackDifficulty;
import moe.ku6.sekaimagic.chart.sus.SUSSheet;
import org.jline.jansi.Ansi;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FingersCommand implements ICommand<FingersCommand.Params> {
    @Override
    public String[] GetNames() {
        return new String[] {"f", "fingers"};
    }

    @Override
    public String GetManual() {
        return "Generate and manage finger charts (C4F) files. Finger charts are used to map notes to finger events on the screen in a track.";
    }

    @Override
    public void HandleInternal(Params args) throws Exception {
        List<Track> selectedTracks = new ArrayList<>();

        if (args.args.size() >= 1) {
            var packageId = Integer.parseInt(args.args.getFirst());
            var pkg = SekaiDatabase.getInstance().getPackages().get(packageId);
            if (pkg == null) {
                log.error("Not found package with ID %d".formatted(packageId));
                return;
            }

            if (args.args.size() >= 2) {
                var difficulty = TrackDifficulty.FromString(args.args.getLast());
                if (difficulty == null) {
                    log.error("Invalid difficulty: %s".formatted(args.args.getLast()));
                    return;
                }

                var track = pkg.getTracks().get(difficulty);
                if (track == null) {
                    log.error("No track found with difficulty %s".formatted(difficulty));
                    return;
                }
                selectedTracks.add(track);

            } else {
                selectedTracks.addAll(pkg.getTracks().values());
            }
        }

        if (!selectedTracks.isEmpty()) {
            boolean operationPerformed = false;

            for (var track : selectedTracks) {
                var pkg = SekaiDatabase.getInstance().getPackages().get(track.getId());
                var difficulty = track.getDifficulty();
                var packageId = pkg.getId();

                if (args.generate) {
                    var susData = Files.readAllLines(pkg.getTracks().get(difficulty).getFile().toPath());
                    var susSheet = new SUSSheet(pkg, pkg.getTracks().get(difficulty), susData);

                    var savePath = new File(SekaiMagic.getInstance().getCwd() + "/fingers/%d/%s.c4f".formatted(packageId, difficulty.toString().toLowerCase()));

                    if (args.printInfo) log.info(susSheet.ToPrintedString());

                    var c4fSheet = new C4FSheet(susSheet);
                    savePath.getParentFile().mkdirs();
                    if (!savePath.exists()) savePath.createNewFile();

                    Files.writeString(savePath.toPath(), c4fSheet.Serialize(), StandardOpenOption.WRITE);
                    log.info(Ansi.ansi().fgBrightGreen().a("Generate: [%d]%s %s: %s".formatted(pkg.getId(), pkg.getTitle(), difficulty, savePath)).reset().toString());
                    operationPerformed = true;
                    continue;
                }

            }

            if (operationPerformed) return;
        }

        log.info(GetUsage());
    }

    public static class Params {
        @Parameter(
                description = "<Package ID> [Difficulty]. Omit the difficulty to use select all difficulties."
        )
        public List<String> args = new ArrayList<>();

        @Parameter(
                names = {"-g", "--generate"},
                description = "Generates a finger chart from a track file. Default location for generated files are in the `fingers` directory in the current working directory."
        )
        public boolean generate;

        @Parameter(
                names = {"-o" , "--output"},
                description = "Specify a different output file for the generated finger chart."
        )
        public String outputFile;

        @Parameter(
                names = {"-p", "--print-info"},
                description = "Prints SUS chart information."
        )
        public boolean printInfo;
    }
}
