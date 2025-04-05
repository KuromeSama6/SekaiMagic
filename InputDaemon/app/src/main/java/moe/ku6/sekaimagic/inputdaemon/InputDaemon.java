package moe.ku6.sekaimagic.inputdaemon;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

public class InputDaemon extends Activity {
    @Getter
    private static InputDaemon instance;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        Log.i("InputDaemon", "InputDaemon started");

        // start the InputDaemonService
        var intent = new Intent(this, InputDaemonService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent); // Required for Android 8.0+
        } else {
            startService(intent);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("InputDaemon", "InputDaemon stopped. Stopping InputDaemonService.");

        InputDaemonService.getInstance().stopSelf();
    }

    public void Quit() {
        Log.i("InputDaemon", "Quitting InputDaemon");
        // stop the InputDaemonService
        InputDaemonService.getInstance().stopSelf();
        System.exit(1);
        return;
    }
}
