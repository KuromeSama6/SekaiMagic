package moe.ku6.sekaimagic.command.impl;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.validators.PositiveInteger;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.SekaiMagic;
import moe.ku6.sekaimagic.chart.fingers.C4FSheet;
import moe.ku6.sekaimagic.chart.sus.SUSSheet;
import moe.ku6.sekaimagic.command.ICommand;
import moe.ku6.sekaimagic.data.SekaiDatabase;
import moe.ku6.sekaimagic.music.TrackDifficulty;
import moe.ku6.sekaimagic.player.TrackPlayer;
import org.jline.jansi.Ansi;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AutoplayCommand implements ICommand<AutoplayCommand.Params> {
    private TrackPlayer player;

    @Override
    public String[] GetNames() {
        return new String[] {"autoplay", "ap"};
    }

    @Override
    public String GetManual() {
        return "Play songs automatically.";
    }

    @Override
    public void HandleInternal(Params args) throws Exception {
        if (player != null) {
            if (args.stop) {
                player.close();
                player = null;
                log.info("Track player stopped.");
                return;
            }

            log.warn("Track player is already playing a song. Do `autoplay -s` to stop it.");
            return;
        }

        if (args.args.size() < 2) {
            log.error("Invalid arguments. Usage: {} <Package ID> <difficulty>", GetNames()[0]);
            return;
        }

        var packageId = Integer.parseInt(args.args.get(0));
        var pkg = SekaiDatabase.getInstance().getPackages().get(packageId);
        if (pkg == null) {
            log.error("Package ID {} not found.", packageId);
            return;
        }

        var difficulty = TrackDifficulty.FromString(args.args.get(1));
        if (difficulty == null) {
            log.error("Invalid difficulty: {}", args.args.get(1));
            return;
        }

        var track = pkg.getTracks().get(difficulty);
        if (track == null) {
            log.error("Track not found for package ID {} and difficulty {}", packageId, difficulty);
            return;
        }

        log.info(Ansi.ansi().a("Autoplay: ")
                .fg(Ansi.Color.WHITE).bg(Ansi.Color.BLUE).a("%s".formatted(pkg.getId())).reset()
                .fgBrightYellow().a(" %s ".formatted(pkg.getTitle())).reset()
                .fg(Ansi.Color.WHITE).bgRgb(difficulty.getColor()).bold().a("%s %d".formatted(difficulty, pkg.GetPlayDifficulty(difficulty))).reset()
                .a(" (x%s)".formatted(args.times))
                .toString());

        var sheet = new SUSSheet(pkg, track, Files.readAllLines(track.getFile().toPath()));

        log.info(Ansi.ansi().a("Track length: ").fgBrightMagenta().a("%.2f seconds".formatted(sheet.getTotalLength())).reset().toString());

        log.info("Starting autoplay. Creating player...");
        player = new TrackPlayer(pkg, track, sheet);
        if (args.skipConfirmation || SekaiMagic.getInstance().getConsole().Prompt("Begin autoplay?")) {
            player.Play();
            log.info(Ansi.ansi().fgBrightGreen().a("Autoplay started. Do `autoplay -s` to stop.").reset().toString());
        } else {
            log.warn("Autoplay cancelled.");
            player.close();
            player = null;
        }

    }

    public static class Params {
        @Parameter(
                description = "The song to play. Use <Package ID> <difficulty>."
        )
        public List<String> args = new ArrayList<>();

        @Parameter(
                names = {"-s", "--stop"},
                description = "Stop the autoplay."
        )
        public boolean stop;

        @Parameter(
                names = {"-t", "--times"},
                description = "The number of times to play the song.",
                validateWith = PositiveInteger.class
        )
        public int times = 1;

        @Parameter(
                names = {"-y", "--yes"},
                description = "Skip confirmation."
        )
        public boolean skipConfirmation;
    }
}
