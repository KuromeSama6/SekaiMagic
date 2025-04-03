package moe.ku6.sekaimagic.exception.track;

import moe.ku6.sekaimagic.music.Track;

public class TrackLoadException extends TrackException{
    public TrackLoadException(Track track, String message) {
        super(message);
    }
}
