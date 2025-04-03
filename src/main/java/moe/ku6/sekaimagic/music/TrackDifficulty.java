package moe.ku6.sekaimagic.music;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jline.jansi.Ansi;

@AllArgsConstructor
@Getter
public enum TrackDifficulty {
    EASY("23af00"),
    NORMAL("007eb0"),
    HARD("d7a802"),
    EXPERT("ff0049"),
    MASTER("bd00ff"),
    APPEND("ffbafa");

    private final String color;

    public static TrackDifficulty FromString(String str) {
        try {
            return valueOf(str.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
