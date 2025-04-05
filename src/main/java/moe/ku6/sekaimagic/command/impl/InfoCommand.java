package moe.ku6.sekaimagic.command.impl;

import com.beust.jcommander.Parameter;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.command.ICommand;
import moe.ku6.sekaimagic.data.SekaiDatabase;
import moe.ku6.sekaimagic.music.TrackDifficulty;
import org.jline.jansi.Ansi;

@Slf4j
public class InfoCommand implements ICommand<InfoCommand.Params> {
    @Override
    public String[] GetNames() {
        return new String[]{"i", "info", "是什么歌" };
    }

    @Override
    public String GetManual() {
        return "See info about a package (song).";
    }

    @Override
    public void HandleInternal(Params args) throws Exception {
        var pkg = SekaiDatabase.getInstance().getPackages().get(Integer.parseInt(args.id));
        if (pkg == null) {
            log.error("No package found with ID {}", args.id);
            return;
        }

        log.info(pkg.GetInfoLine());
        log.info(Ansi.ansi().a("Pronounciation: ").fgBrightMagenta().a(pkg.getPronounciation()).reset().toString());
        log.info(Ansi.ansi().a("Total Valid Tracks: ").fgBrightMagenta().a(pkg.getTracks().size()).reset().toString());
        log.info(Ansi.ansi().fgBrightMagenta().a("Track Info:").reset().toString());
        for (var difficulty : TrackDifficulty.values()) {
            if (!pkg.getTracks().containsKey(difficulty)) {
                continue;
            }

            var track = pkg.getTracks().get(difficulty);
            log.info(Ansi.ansi().fgRgb(difficulty.getColor()).a("[%s %d]".formatted(difficulty, pkg.GetPlayDifficulty(difficulty))).reset().a(" %s".formatted(track.getFile())).toString());
        }

        if (pkg.getTracks().isEmpty()) {
            log.info(Ansi.ansi().fgYellow().a("* This package has no tracks. Check if you have the corresponding game files imported.").reset().toString());
        } else {
            log.info(Ansi.ansi().fgGreen().a("* Do `fingers -g %d expert` to generate fingers chart for the EXPERT difficulty.".formatted(pkg.getId())).reset().toString());
            log.info(Ansi.ansi().fgGreen().a("* Do `autoplay %d expert -t 15` to auto play the EXPERT difficulty of this track 15 times (useful for hitting the leaderboards).".formatted(pkg.getId())).reset().toString());
            log.info(Ansi.ansi().fgGreen().a("* Do `man fingers` to learn more to learn more about finger charts and the C4F (chart for fingers) format.").reset().toString());
            log.info("(info {})", pkg.getId());
        }
    }

    public static class Params {
        @Parameter(
                description = "The numeric ID of the package to get information about",
                required = true
        )
        public String id;
    }
}
