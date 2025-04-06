package moe.ku6.sekaimagic.inputdaemon;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.jsonwrapper.JsonWrapper;
import moe.ku6.sekaimagic.inputdaemon.command.CommandManager;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.Arrays;

@Slf4j
public class WebsocketServer extends WebSocketServer {
    private static final String TAG = "WebsocketServer";

    @Getter
    private static WebsocketServer instance;
    @Getter
    private WebSocket upstream;
//    private final Thread websocketThread;

    public WebsocketServer() {
        super(new InetSocketAddress(7700));

        if (instance != null)
            throw new RuntimeException("WebsocketServer already initialized");

        Log.i(TAG, "Websocket starting on port 7700");
        instance = this;

        setDaemon(true);
        setReuseAddr(true);
        start();

//        websocketThread = new Thread(this);
//        websocketThread.start();
    }

    @Override
    public void onStart() {
        Log.i(TAG, "WebSocket server started on port " + getPort());
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        if (upstream != null) {
            webSocket.close();

            Log.w(TAG, "Already connected to another client, closing new connection");
            return;
        }
        upstream = webSocket;

        Log.i(TAG, "Upstream connected");

        InputDaemon.getInstance().runOnUiThread(() -> {
            var notification = new NotificationCompat.Builder(InputDaemon.getInstance(), "inputd_push")
                    .setContentTitle("Upstream connected")
                    .setContentText("Upstream has connected to this input daemon")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
//                    .setAutoCancel(true)
                    .build();

            NotificationManagerCompat.from(InputDaemon.getInstance())
                    .notify(1, notification);
        });
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        if (upstream != webSocket) return;
        Log.w(TAG, "Upstream connection closed");
        upstream = null;
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
//        Log.d(TAG, "Message received: " + s);
        var data = new JsonWrapper(s);
        var respose = CommandManager.getInstance().HandleCommand(data);

        webSocket.send(respose.toString());
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onError(WebSocket webSocket, Exception e) {
        Log.e(TAG, "WebSocket error: ", e);

        if (e instanceof BindException) {
            InputDaemon.getInstance().runOnUiThread(() -> {
                var notification = new NotificationCompat.Builder(InputDaemon.getInstance(), "inputd_push")
                        .setContentTitle("Websocket Error")
                        .setContentText("Could not bind to port 7700. Is another instance of the daemon running or is the port already in use?")
                        .setSmallIcon(android.R.drawable.stat_sys_warning)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
//                    .setAutoCancel(true)
                        .build();

                NotificationManagerCompat.from(InputDaemon.getInstance()).notify(1, notification);

                new AlertDialog.Builder(InputDaemon.getInstance())
                        .setTitle("Websocket Error")
                        .setMessage("Could not bind to port 7700. This app will now exit. Is another instance of the daemon running or is the port already in use?\n\nThis issue may also be caused by a force exit of the app, which will usually automatically resolve within one or two minutes.\n\n" + e)
                        .setCancelable(false)
                        .setPositiveButton("Quit", (dialog, which) -> {
                            dialog.dismiss();
                            InputDaemon.getInstance().Quit();
                        })
                        .create()
                        .show();
            });
        }
    }


    public void Destroy() {
        try {
            stop();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
