package moe.ku6.sekaimagic.music;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import moe.ku6.sekaimagic.exception.track.TrackException;
import moe.ku6.sekaimagic.exception.track.TrackLoadException;

import java.io.File;

@ToString
public class Track {
    @Getter
    private final int id;
    @Getter
    private final TrackDifficulty difficulty;
    @Getter
    private final File file;

    public Track(File file) throws TrackException {
        this.file = file;

        var fileName = file.getName().split("\\.")[0];
        difficulty = TrackDifficulty.FromString(fileName);
        if (difficulty == null)
            throw new TrackLoadException(this, "Invalid track difficulty: " + fileName);

        var parent = file.getParentFile();
        if (parent == null)
            throw new TrackLoadException(this, "Parent file is null");

        var parentName = parent.getName();
        var id = Integer.parseInt(parentName.split("_")[0]);
        if (id < 0)
            throw new TrackLoadException(this, "Invalid track ID: " + id);

        this.id = id;
    }
}
