package moe.ku6.sekaimagic.player;

import com.sun.jna.Pointer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.SekaiMagic;
import moe.ku6.sekaimagic.input.InputDaemon;
import moe.ku6.sekaimagic.input.InputManager;
import moe.ku6.sekaimagic.chart.fingers.C4FSheet;
import moe.ku6.sekaimagic.chart.fingers.ChainFingerCue;
import moe.ku6.sekaimagic.chart.fingers.FingerActionType;
import moe.ku6.sekaimagic.chart.fingers.FingerCue;
import moe.ku6.sekaimagic.chart.sus.SUSSheet;
import moe.ku6.sekaimagic.input.GuideSoundPlayer;
import moe.ku6.sekaimagic.input.TouchEventBuilder;
import moe.ku6.sekaimagic.music.MusicPackage;
import moe.ku6.sekaimagic.music.Track;
import moe.ku6.sekaimagic.util.Vec2;
import moe.ku6.sekaimagic.util.timer.HighPrecisionWindowsTimer;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

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
    private final List<ActiveFinger> pendingFingers = new ArrayList<>();
    private final List<ActiveFinger> activeFingers = new ArrayList<>();

    public TrackPlayer(MusicPackage pkg, Track track, SUSSheet sheet) {
        this.pkg = pkg;
        this.track = track;
        this.sheet = sheet;

        this.fingerSheet = new C4FSheet(sheet);

        fingerSheet.getCues().forEach(c -> {
            var finger = ActiveFinger.NewInstance(c);
            if (finger != null) pendingFingers.add(finger);
        });

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

        boolean playSound = false;
        Map<Integer, TouchEventBuilder> touchEvents = new HashMap<>();

        // activate pending fingers
        for (var finger : new ArrayList<>(pendingFingers)) {
            if (finger.GetActiveWindow().getX() <= actualTrackTime) {
                finger.setFingerId(GetFreeFingerId());
                activeFingers.add(finger);
                pendingFingers.remove(finger);

                playSound = true;

                log.debug("begin: {} time {} loc {} range {}",
                        finger.getFingerId(),
                        finger.getCue().getTime(),
                        finger.getCue().getLocation(),
                        finger.GetActiveWindow()
                );
                touchEvents.put(finger.getFingerId(),
                        new TouchEventBuilder(finger.getFingerId())
                                .FingerDown()
                );
            }
        }

        // check for active fingers
        var daemon = InputManager.getInstance().getDaemons().getFirst();
        for (var finger : new ArrayList<>(activeFingers)) {
            if (!touchEvents.containsKey(finger.getFingerId())) {
                touchEvents.put(finger.getFingerId(), new TouchEventBuilder(finger.getFingerId()));
            }
            var event = touchEvents.get(finger.getFingerId());
//            log.debug("tick: {} endAt {} time {}", finger.getFingerId(), finger.GetActiveWindow().getY(), actualTrackTime);

            var lanePos = finger.GetPosition(actualTrackTime);
            if (lanePos >= 0) {
                var actualPos = ToScreenTapPosition(daemon, lanePos);
                event.Position(new Vec2(actualPos.getY() + finger.GetYOffset(actualTrackTime), actualPos.getX()));
            }

            if (finger.GetActiveWindow().getY() <= actualTrackTime) {
                activeFingers.remove(finger);
                event.FingerUp();
                log.debug("end: {} time {} now {}", finger.getFingerId(), finger.getCue().getTime(), actualTrackTime);

            }
        }

        for (var event : touchEvents.values()) {
            event.Sync();
            daemon.getWebsocketClient().SendEvents(event.Build());
        }
        touchEvents.clear();

        if (playSound && guideSoundPlayer != null) {
//            guideSoundPlayer.Play();
        }
    }

    public int GetFreeFingerId() {
        int ret = 1;
        activeFingers.sort(Comparator.comparingInt(ActiveFinger::getFingerId));
        for (var finger : activeFingers) {
            if (finger.getFingerId() != ret) {
                return ret;
            }
            ret++;
        }

        return ret;
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

    private static Vec2 ToScreenTapPosition(InputDaemon daemon, double x) {
        var config = SekaiMagic.getInstance().getConfig();
        var range = new Vec2(config.GetList("autoplay.randomization.tapOffsetX", Double.class));
        var offset = range.Random();
        var laneWidth = daemon.getScreenResolution().getX() / 12.0;
        var yRange = new Vec2(config.GetList("autoplay.randomization.tapOffsetY", Double.class));
        var pos = (x + offset) * laneWidth;

        return new Vec2(pos + Math.round(Math.random()), daemon.getScreenResolution().getY() * 0.05 + yRange.Random());
    }
}
