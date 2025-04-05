package moe.ku6.sekaimagic.inputdaemon;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import lombok.Getter;

public class InputDaemonService extends Service {
    @Getter
    private static InputDaemonService instance;
    private UInput uInput;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        new WebsocketServer();

        Log.i("InputDaemonService", "Creating UInput");
        uInput = new UInput();
        if (!uInput.Init()) {
            Log.e("InputDaemonService", "Failed to initialize UInput. Quitting.");
            InputDaemon.getInstance().Quit();
            return;
        }

        StartInForeground();
        Log.i("InputDaemonService", "InputDaemonService started");
    }

    private void StartInForeground() {
        String channelId = "input_daemon_channel";
        String channelName = "Input Daemon Service";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_MIN
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(chan);
            }
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("SekaiMagic Input Daemon Running")
                .setContentText("daemon is running")
                .setSmallIcon(android.R.drawable.ic_menu_rotate)
                .build();

        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("InputDaemonService", "InputDaemonService stopped");

        if (WebsocketServer.getInstance() != null) {
            WebsocketServer.getInstance().Destroy();
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }
}
