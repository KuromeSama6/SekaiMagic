package moe.ku6.sekaimagic.input;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.jsonwrapper.JsonWrapper;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class DaemonWebsocketClient extends WebSocketListener implements Closeable {
    private final OkHttpClient client;
    private final WebSocket webSocket;

    public DaemonWebsocketClient(int port) throws URISyntaxException {
        client = new OkHttpClient();

        var request = new Request.Builder()
                .url("ws://127.0.0.1:%d".formatted(port))
                .build();

        webSocket = client.newWebSocket(request, this);
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        super.onOpen(webSocket, response);
        log.info("[inputd] WebSocket connected");
    }

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        super.onClosed(webSocket, code, reason);
        log.warn("[inputd] WebSocket closed");
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        super.onFailure(webSocket, t, response);
        log.error("[inputd] WebSocket error: {}", t.getMessage());
        t.printStackTrace();
    }

    @Override
    public void close() throws IOException {
        webSocket.close(1000, "Client closed");
    }

    public void SendCommand(String command, JsonWrapper data) {
        var json = new JsonWrapper();
        json.Set("command", command);
        json.Set("data", data);
        webSocket.send(json.toString());
    }

    public void SendEvents(List<Integer> events) {
        var data = new JsonWrapper()
                .Set("events", events);
        SendCommand("sendevent", data);
    }
}
