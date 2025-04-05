package moe.ku6.sekaimagic.input;

import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.SekaiMagic;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

@Slf4j
public class GuideSoundPlayer implements Closeable {
    private final URL audioUrl;

    public GuideSoundPlayer() throws UnsupportedAudioFileException, IOException {
        audioUrl = SekaiMagic.getInstance().getClass().getResource("/static/audio/guide.wav");
        if (audioUrl == null)
            throw new UnsupportedOperationException("Guide sound file not found");
    }

    public void Play() {
        try (var audioInputStream = AudioSystem.getAudioInputStream(audioUrl)){
            var clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();

            clip.addLineListener(event -> {
                if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                    clip.close();
                }
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
//        audioInputStream.close();
    }
}
