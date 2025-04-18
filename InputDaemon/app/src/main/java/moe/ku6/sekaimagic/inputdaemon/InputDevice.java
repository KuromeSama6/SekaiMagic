package moe.ku6.sekaimagic.inputdaemon;

import android.util.Log;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class InputDevice implements Closeable {
    private static final String TAG = "InputDevice";
    private final FileOutputStream fileOutputStream;
    private final FileChannel fileChannel;

    public InputDevice(String path) throws IOException {
        fileOutputStream = new FileOutputStream(path);
        fileChannel = fileOutputStream.getChannel();

        Log.d(TAG, "Open: " + path);
    }


    @Override
    public void close() throws IOException {
        fileChannel.close();
        fileOutputStream.close();
    }
}
