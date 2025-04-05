package moe.ku6.sekaimagic.util.timer;

import com.sun.jna.Callback;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;
import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;

public class HighPrecisionWindowsTimer implements Closeable {
    private final int handle;
    private final TimerCallback callback; // prevents garbage collection
    @Getter
    private boolean running;

    public HighPrecisionWindowsTimer(TimerCallback callback) {
        WinMM.INSTANCE.timeBeginPeriod(1);
        this.callback = callback;

        handle = WinMM.INSTANCE.timeSetEvent(1, 0, callback, null, WinMM.TIME_PERIODIC);
        if (handle == 0) {
            throw new RuntimeException("Failed to create high-resolution timer");
        }

        running = true;
    }

    @Override
    public void close() throws IOException {
        if (!running) {
            throw new IllegalStateException("Timer is not running");
        }
        WinMM.INSTANCE.timeKillEvent(handle);
        running = false;
        WinMM.INSTANCE.timeEndPeriod(1);
    }

    // Load winmm.dll and define required functions
    public interface WinMM extends StdCallLibrary {
        WinMM INSTANCE = Native.load("winmm", WinMM.class);

        int TIME_PERIODIC = 1;
        int TIME_CALLBACK_FUNCTION = 0x0000;
        int timeSetEvent(int delay, int resolution, Callback callback, Pointer user, int eventType);
        int timeKillEvent(int timerID);
        int timeBeginPeriod(int period);
        int timeEndPeriod(int period);
    }

    public interface TimerCallback extends Callback {
        void invoke(int timerID, int msg, Pointer user, int dw1, int dw2);
    }
}
