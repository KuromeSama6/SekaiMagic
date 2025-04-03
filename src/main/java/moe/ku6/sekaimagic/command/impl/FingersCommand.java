package moe.ku6.sekaimagic.command.impl;

import com.beust.jcommander.Parameter;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.SekaiMagic;
import moe.ku6.sekaimagic.chart.fingers.C4FSheet;
import moe.ku6.sekaimagic.command.ICommand;
import moe.ku6.sekaimagic.data.SekaiDataManager;
import moe.ku6.sekaimagic.exception.command.CommandExecutionException;
import moe.ku6.sekaimagic.music.TrackDifficulty;
import moe.ku6.sekaimagic.chart.sus.SUSSheet;
import org.jline.jansi.Ansi;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.OpenOption;
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
        if (args.generate.size() == 2) {
            var packageId = Integer.parseInt(args.generate.getFirst());
            var difficulty = TrackDifficulty.FromString(args.generate.getLast());
            if (difficulty == null) {
                throw new CommandExecutionException("Invalid difficulty: %s".formatted(args.generate.getLast()));
            }

            var pkg = SekaiDataManager.getInstance().getPackages().get(packageId);
            if (pkg == null) {
                throw new CommandExecutionException("Package ID %d not found".formatted(packageId));
            }

            var susData = Files.readAllLines(pkg.getTracks().get(difficulty).getFile().toPath());
            var susSheet = new SUSSheet(pkg, pkg.getTracks().get(difficulty), susData);

            var savePath = new File(SekaiMagic.getInstance().getCwd() + "/fingers/%d/%s.c4f".formatted(packageId, difficulty.toString().toLowerCase()));
            log.info(susSheet.ToPrintedString());

            var c4fSheet = new C4FSheet(susSheet);
            savePath.getParentFile().mkdirs();
            if (!savePath.exists()) savePath.createNewFile();

            Files.writeString(savePath.toPath(), c4fSheet.Serialize(), StandardOpenOption.WRITE);

            log.info(Ansi.ansi().fgBrightGreen().a("Generated file saved to %s".formatted(savePath)).reset().toString());

            return;
        }

        log.info(GetUsage());
    }

    public static class Params {
        @Parameter(
                names = {"-g", "--generate"},
                description = "...-g <Package ID> <Difficulty>. Generates a finger chart from a track file. Default location for generated files are in the `fingers` directory in the current working directory.",
                arity = 2
        )
        public List<String> generate = new ArrayList<>();

        @Parameter(
                names = {"-o" , "--output"},
                description = "Specify a different output file for the generated finger chart."
        )
        public String outputFile;
    }
}
