package moe.ku6.sekaimagic.inputdaemon;

import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import lombok.Getter;
import moe.ku6.sekaimagic.inputdaemon.command.CommandManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InputDaemonService extends Service {
    @Getter
    private static InputDaemonService instance;
    private final Map<String, InputDevice> openDevices = new HashMap<>();
    @Getter
    private UInputJNI uInput;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("InputDaemonService", "onStartCommand called");
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        Log.i("InputDaemonService", "Creating UInput");
//        uInput = new UInput();
//        if (!uInput.Init()) {
//            Log.e("InputDaemonService", "Failed to initialize UInput. Quitting.");
//            InputDaemon.getInstance().Quit();
//            return;
//        }

        try {
            StartInForeground();

        } catch (Exception e) {
            Log.e("InputDaemonService", "Failed to start in foreground", e);

            InputDaemon.getInstance().runOnUiThread(() -> {
                new AlertDialog.Builder(InputDaemon.getInstance())
                        .setTitle("SekaiMagic Input Daemon Launch Error")
                        .setMessage("""
                                Failed to start input daemon. This app will now exit. This may be caused by:
                                - Missing permissions (permission to open /dev/uinput). Try running the app as root, or modify the permissions of /dev/uinput.
                                - Incompatible or not supported JNI/Native libraries.
                                - Your device does not support /dev/uinput and/or virtual input devices.
                                
                                For more assistance, see SekaiMagic's Github repository at
                                https://github.com/KuromeSama6/SekaiMagic
                                
                                Error caused by:
                                """ + e)
                        .setPositiveButton("Close App", (dialog, which) -> {
                            dialog.dismiss();
                            InputDaemon.getInstance().Quit();
                        })
                        .setCancelable(false)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setCancelable(false)
                        .show();
            });
            return;
        }
        Log.i("InputDaemonService", "InputDaemonService started");
    }

    private void StartInForeground() throws Exception {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(new NotificationChannel("inputd_status", "Input Daemon Status", NotificationManager.IMPORTANCE_MIN));
                manager.createNotificationChannel(new NotificationChannel("inputd_push", "Input Daemon Push Notification", NotificationManager.IMPORTANCE_HIGH));
            }
        }

        Notification notification = new NotificationCompat.Builder(this, "inputd_status")
                .setContentTitle("SekaiMagic Input Daemon Running")
                .setContentText("daemon is running")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(android.R.drawable.ic_menu_rotate)
                .build();

        uInput = new UInputJNI();

        new WebsocketServer();
        new CommandManager();

        startForeground(1, notification);

        var builder = new AlertDialog.Builder(InputDaemon.getInstance())
                .setTitle("SekaiMagic Input Daemon")
                .setMessage("""
                        Input Daemon is running in the background. You can leave this app, but do not exit it completely (i.e. by swiping it away in the task manager).
                        
                        When the daemon is running, you'll see an icon in the top status bar to the left. 
                        
                        To stop the daemon, press the "Stop Daemon" button below. We advise against force stopping the daemon, as this may cause issues with the app.
                        """)
                .setPositiveButton("Stop Daemon", (dialog, which) -> {
                    InputDaemon.getInstance().Quit();
                })
                .setCancelable(false);
        InputDaemon.getInstance().runOnUiThread(builder::show);
    }

    public synchronized boolean OpenDevice(String path) {
        try {
            var device = new InputDevice(path);
            openDevices.put(path, device);
            Log.i("InputDaemonService", "Opened device: " + path);
            return true;

        } catch (IOException e) {
            Log.e("InputDaemonService", "Error opening device: " + path, e);
        }

        return false;
    }

    public void Stop() {
        Log.i("InputDaemonService", "InputDaemonService stopped");

        if (WebsocketServer.getInstance() != null) {
            WebsocketServer.getInstance().Destroy();
        }

        try {
            uInput.close();
        } catch (IOException e) {
            Log.e("InputDaemonService", "Error closing uInput", e);
        }

        openDevices.values().forEach(c -> {
            try {
                c.close();
            } catch (IOException e) {
                Log.e("InputDaemonService", "Error closing device", e);
            }
        });

        openDevices.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.i("InputDaemonService", "Task removed, stopping service");
    }
}
