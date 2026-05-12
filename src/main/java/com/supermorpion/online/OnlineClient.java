package com.supermorpion.online;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Thin wrapper around Java 11's built-in WebSocket client.
 * All callbacks are fired on the WebSocket I/O thread; callers must
 * dispatch to the JavaFX thread themselves (Platform.runLater).
 */
public class OnlineClient {

    public static final String DEFAULT_SERVER = "supertictactoe-production.up.railway.app";

    private final HttpClient httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .build();
    private WebSocket ws;

    private Consumer<String> onMessage;
    private Runnable         onOpen;
    private Consumer<String> onClose;
    private Consumer<String> onError;

    public void setOnMessage(Consumer<String> h) { onMessage = h; }
    public void setOnOpen(Runnable h)            { onOpen    = h; }
    public void setOnClose(Consumer<String> h)   { onClose   = h; }
    public void setOnError(Consumer<String> h)   { onError   = h; }

    public void connect(String url) {
        if (!url.startsWith("ws://") && !url.startsWith("wss://")) {
            url = "wss://" + url;
        }
        httpClient.newWebSocketBuilder()
            .buildAsync(URI.create(url), new WebSocket.Listener() {

                private final StringBuilder buf = new StringBuilder();

                @Override
                public void onOpen(WebSocket socket) {
                    ws = socket;
                    socket.request(1);
                    if (onOpen != null) onOpen.run();
                }

                @Override
                public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
                    buf.append(data);
                    if (last) {
                        String msg = buf.toString();
                        buf.setLength(0);
                        if (onMessage != null) onMessage.accept(msg);
                    }
                    socket.request(1);
                    return CompletableFuture.completedFuture(null);
                }

                @Override
                public CompletionStage<?> onClose(WebSocket socket, int code, String reason) {
                    if (onClose != null) onClose.accept(reason);
                    return CompletableFuture.completedFuture(null);
                }

                @Override
                public void onError(WebSocket socket, Throwable error) {
                    if (onError != null) onError.accept(error.getMessage());
                }
            })
            .exceptionally(ex -> {
                if (onError != null) onError.accept(ex.getMessage());
                return null;
            });
    }

    public void send(String json) {
        if (ws != null && !ws.isOutputClosed())
            ws.sendText(json, true);
    }

    public void close() {
        if (ws != null && !ws.isOutputClosed())
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
    }

    // ── Lightweight JSON helpers (no external deps) ───────────────────────────

    public static String getString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int i = json.indexOf(search);
        if (i < 0) return null;
        i += search.length();
        int j = json.indexOf('"', i);
        return j < 0 ? null : json.substring(i, j);
    }

    public static int getInt(String json, String key) {
        String search = "\"" + key + "\":";
        int i = json.indexOf(search);
        if (i < 0) return -1;
        i += search.length();
        int j = i;
        while (j < json.length() && Character.isDigit(json.charAt(j))) j++;
        return j > i ? Integer.parseInt(json.substring(i, j)) : -1;
    }

    public static String moveJson(int br, int bc, int cr, int cc) {
        return "{\"type\":\"move\",\"boardRow\":" + br + ",\"boardCol\":" + bc
             + ",\"cellRow\":"  + cr + ",\"cellCol\":"  + cc + "}";
    }
}
