package moe.ku6.sekaimagic.music;

import lombok.Getter;
import moe.ku6.jsonwrapper.JsonWrapper;
import org.jline.jansi.Ansi;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a music sheet in the game. Contains references to metadata about the sheet and all difficulties of the sheet.
 */
public class MusicPackage {
    @Getter
    private final int id;
    @Getter
    private final String title, pronounciation;
    @Getter
    private final double fillerSeconds;
    @Getter
    private final Map<TrackDifficulty, Track> tracks = new HashMap<>();
    @Getter
    private final Map<TrackDifficulty, Integer> playLevels = new HashMap<>();

    public MusicPackage(JsonWrapper data) {
        id = data.GetInt("id");
        title = data.GetString("title", "");
        pronounciation = data.GetString("pronunciation", ""); // intentionally misspelled to match the API
        fillerSeconds = data.GetDouble("fillerSec");
    }

    public void SortTracks() {
        tracks.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(e -> tracks.put(e.getKey(), e.getValue()));
    }

    public int GetPlayDifficulty(TrackDifficulty difficulty) {
        return playLevels.getOrDefault(difficulty, -1);
    }

    public String GetInfoLine() {
        var sb = new StringBuilder();
        sb.append(Ansi.ansi().bold().fg(Ansi.Color.WHITE).bg(tracks.isEmpty() ? Ansi.Color.RED : Ansi.Color.BLUE).a(String.format("%05d", id)).reset());
        sb.append(" ");
        sb.append(Ansi.ansi().bold().fg(Ansi.Color.YELLOW).a(title).reset());
        sb.append(" ");

        if (tracks.isEmpty()) {
            sb.append(Ansi.ansi().fg(Ansi.Color.RED).bg(Ansi.Color.WHITE).a("No tracks").reset());

        } else {
            for (var difficulty : TrackDifficulty.values()) {
                var track = tracks.get(difficulty);
                if (track != null) {
                    sb.append(Ansi.ansi().bold().bgRgb(difficulty.getColor()).fg(Ansi.Color.WHITE).bold().a("%s %02d".formatted(difficulty, GetPlayDifficulty(difficulty))).reset());
                } else {
                    sb.append(Ansi.ansi().bold().bgRgb("757575").fg(Ansi.Color.WHITE).a("%s --".formatted(difficulty)).reset());
                }
                sb.append(" ");
            }
        }

        return sb.toString();
    }
}
