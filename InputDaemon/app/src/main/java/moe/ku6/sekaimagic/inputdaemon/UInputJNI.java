package moe.ku6.sekaimagic.inputdaemon;

import android.os.Build;
import android.util.Log;
import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;

public class UInputJNI implements Closeable {
    static {
        System.loadLibrary("uinput_touch");
    }

    private static native int Open();
    private static native void Close(int fd);
    private static native void Emit(int fd, int type, int code, int value);

    private final int fd;
    private boolean closed = false;

    public UInputJNI() throws IOException {
        fd = Open();
        Log.i("UInputJNI", "Opened uinput device: " + fd);

        if (fd < 0) {
            throw new IOException("Failed to open uinput device: " + fd);
        }
    }

    public void Emit(int type, int code, int value) {
        if (closed)
            throw new IllegalStateException("closed");

        Emit(fd, type, code, value);
    }

    @Override
    public void close() throws IOException {
        if (closed)
            throw new IllegalStateException("closed");

        Close(fd);
        closed = true;
    }
}
