package moe.ku6.sekaimagic.inputdaemon;

import android.util.Log;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Arrays;

@Slf4j
public class WebsocketServer extends WebSocketServer {
    private static final String TAG = "WebsocketServer";

    @Getter
    private static WebsocketServer instance;
    @Getter
    private WebSocket upstream;

    public WebsocketServer() {
        super(new InetSocketAddress(7700));

        if (instance != null)
            throw new RuntimeException("WebsocketServer already initialized");

        Log.i(TAG, "Websocket starting on port 7700");
        instance = this;
        start();
    }

    @Override
    public void onStart() {
        Log.i(TAG, "WebSocket server started on port " + getPort());
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        if (upstream != null) {
            webSocket.close();
            Log.w(TAG, "Already connected to another client, closing new connection");
            return;
        }
        upstream = webSocket;

        Log.i(TAG, "Upstream connected");
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        if (upstream != webSocket) return;
        Log.w(TAG, "Upstream connection closed");
        upstream = null;
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        Log.d(TAG, "Message received: " + s);
        var commands = s.split(";");

        for (var cmd : commands) {
            try {
                var args = cmd.split(" ");
                if (args.length == 0) return;
                HandleCommand(args);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        Log.e(TAG, "WebSocket error: ", e);
    }

    private void HandleCommand(String[] args) throws Exception {
        Log.d(TAG, "Handle command: " + Arrays.toString(args));
        switch (args[0].toLowerCase()) {
            case "tap" -> {

            }

            default -> {
                Log.e(TAG, "Unknown command: " + args[0]);
            }
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
