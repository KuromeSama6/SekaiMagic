package moe.ku6.sekaimagic.player;

import com.sun.jna.Pointer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.adb.ADBManager;
import moe.ku6.sekaimagic.chart.fingers.C4FSheet;
import moe.ku6.sekaimagic.chart.fingers.ChainFingerQueue;
import moe.ku6.sekaimagic.chart.fingers.FingerActionType;
import moe.ku6.sekaimagic.chart.fingers.FingerCue;
import moe.ku6.sekaimagic.chart.sus.SUSSheet;
import moe.ku6.sekaimagic.input.GuideSoundPlayer;
import moe.ku6.sekaimagic.music.MusicPackage;
import moe.ku6.sekaimagic.music.Track;
import moe.ku6.sekaimagic.util.timer.HighPrecisionWindowsTimer;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Automatically plays a track.
 */
@Slf4j
public class TrackPlayer implements Closeable {
    private final MusicPackage pkg;
    private final Track track;
    private final SUSSheet sheet;
    private final C4FSheet fingerSheet;
    private final GuideSoundPlayer guideSoundPlayer;
    private final HighPrecisionWindowsTimer timer;

    @Getter
    private double time;
    @Getter
    private boolean playing;

    private volatile boolean stopRequested = false;
    private int leadinSoundsRemaining;
    private double leadinSoundInterval;
    private double nextLeadinSoundTime;
    private long lastTickTime;
    private final List<FingerCue> pendingCues = new ArrayList<>();

    public TrackPlayer(MusicPackage pkg, Track track, SUSSheet sheet) {
        this.pkg = pkg;
        this.track = track;
        this.sheet = sheet;

        this.fingerSheet = new C4FSheet(sheet);
        pendingCues.addAll(fingerSheet.getCues());
        log.info("C4F sheet loaded.");

        GuideSoundPlayer guideSoundPlayer;
        try {
            guideSoundPlayer = new GuideSoundPlayer();
        } catch (Exception e) {
            log.warn("Failed to initialize GuideSoundPlayer. Will be silent.");
            e.printStackTrace();
            guideSoundPlayer = null;
        }
        this.guideSoundPlayer = guideSoundPlayer;

        timer = new HighPrecisionWindowsTimer(this::OnTick);
        time = fingerSheet.getCues().stream()
                .mapToDouble(FingerCue::getTime)
                .min().orElse(0);

//        leadinSoundsRemaining = sheet.GetBeatsPerBar(0);
        leadinSoundsRemaining = 0;
        leadinSoundInterval = 1.0 / sheet.GetBPS(0);
        nextLeadinSoundTime = 0;
        lastTickTime = System.nanoTime();

        log.info("Lead in: {} sounds, interval: {} seconds", leadinSoundsRemaining, leadinSoundInterval);
    }

    private void OnTick(int i, int i1, Pointer pointer, int i2, int i3) {
        final double deltaTime = (System.nanoTime() - lastTickTime) / 1_000_000_000.0;
        lastTickTime = System.nanoTime();
        time += deltaTime;

        if (leadinSoundsRemaining > 0) {
            if (time >= nextLeadinSoundTime) {
                if (guideSoundPlayer != null) {
                    guideSoundPlayer.Play();
                }
                leadinSoundsRemaining--;
                nextLeadinSoundTime += leadinSoundInterval;
            }
        }

        double actualTrackTime = time;
        if (actualTrackTime < 0) return;

        var cues = pendingCues.stream()
                .filter(c -> c.getTime() <= actualTrackTime)
                .toList();
        pendingCues.removeAll(cues);

        boolean playSound = false;
        int fingerId = 1;

        for (var cue : cues) {
            // process cue
            if (cue instanceof ChainFingerQueue chain) {

            } else {
                var pos = cue.getPos();
                for (var device : ADBManager.getInstance().getConnectors()) {
                    if (cue.getAction() == FingerActionType.FLICK_UP) {
                        device.FlickUp(pos, fingerId);
                        ++fingerId;

                    } else {
                        device.Tap(pos);
                    }
                }
            }

            if (cue.getAction() != FingerActionType.FINGER_HOLD) {
                playSound = true;
            }
        }

        if (playSound && guideSoundPlayer != null) {
            guideSoundPlayer.Play();
        }
    }

    public void Play() {
        EnsureRunning();
        playing = true;
    }

    public void Pause() {
        EnsureRunning();
        playing = false;
    }

    @Override
    public void close() throws IOException {
        guideSoundPlayer.close();
        timer.close();
    }

    private void EnsureRunning() {
        if (!timer.isRunning())
            throw new IllegalStateException("Player thread is not alive.");
    }
}
