package discovery;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;

public class DiscoveryManager implements WebSocket.Listener, AutoCloseable {

    private volatile WebSocket socket;
    private volatile CompletableFuture<String> pendingResponse;
    private final StringBuilder messageBuffer = new StringBuilder();
    private final Gson g = new Gson();


    public synchronized void joinDiscoveryServer(
            String ip,
            int port,
            String name,
            String listenerIP,
            int listenerPort
    ) throws ExecutionException, InterruptedException, TimeoutException, DiscoveryServerException {

        if (socket != null) {
            throw new IllegalStateException(
                    "This DiscoveryManager has already connected"
            );
        }

        messageBuffer.setLength(0);
        try {
            socket = HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(
                            URI.create("ws://" + ip + ":" + port),
                            this
                    )
                    .join();

            String joinMessage = "{\"type\":\"join\",\"name\":\"" + name + "\", \"ip\": \"" + listenerIP + "\", \"port\":" + listenerPort + "}";

            String response = sendMessage(joinMessage);

            DiscoveryStruct parsed_response = g.fromJson(response, DiscoveryStruct.class);

            if (!"joined".equals(parsed_response.type)) {
                if ("error".equals(parsed_response.type)) {
                    throw new DiscoveryServerException(parsed_response.message);
                }
                throw new IllegalStateException(
                        "Unexpected join response: " + response
                );
            }
        } catch (ExecutionException | InterruptedException | TimeoutException | RuntimeException | DiscoveryServerException exception) {
            if (socket != null) {
                socket.abort();
                socket = null;
            }
            throw exception;
        }
    }

    public synchronized DiscoveryStruct getList() throws ExecutionException, InterruptedException, TimeoutException, DiscoveryServerException {
        String listMessage = "{\"type\":\"list\"}";
        String response = sendMessage(listMessage);
        DiscoveryStruct parsed_response = g.fromJson(response, DiscoveryStruct.class);
        if (!"list".equals(parsed_response.type)) {
            if ("error".equals(parsed_response.type)) {
                throw new DiscoveryServerException(parsed_response.message);
            }
            throw new IllegalStateException(
                    "Unexpected list response: " + response
            );
        }
        return parsed_response;
    }

    public synchronized String sendMessage(String message)
            throws ExecutionException, InterruptedException, TimeoutException {

        if (socket == null
                || socket.isInputClosed()
                || socket.isOutputClosed()) {
            throw new IllegalStateException(
                    "Not connected to the discovery server"
            );
        }

        CompletableFuture<String> response = new CompletableFuture<>();
        pendingResponse = response;

        try {
            socket.sendText(message, true)
                    .whenComplete((_, error) -> {
                        if (error != null) {
                            response.completeExceptionally(error);
                        }
                    });

            return response.get(2, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            socket.abort();
            Thread.currentThread().interrupt();
            throw e;

        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            socket.abort();
            throw e;

        } finally {
            pendingResponse = null;
        }
    }

    @Override
    public CompletionStage<?> onText(
            WebSocket ws,
            CharSequence data,
            boolean last
    ) {
        if (ws != socket) {
            ws.request(1);
            return null;
        }
        messageBuffer.append(data);

        if (last) {
            String message = messageBuffer.toString();
            messageBuffer.setLength(0);

            CompletableFuture<String> response = pendingResponse;

            if (response != null) {
                response.complete(message);
            }
        }

        ws.request(1);
        return null;
    }

    @Override
    public void onError(WebSocket ws, Throwable error) {
        if (ws != socket) {
            return;
        }
        CompletableFuture<String> response = pendingResponse;

        if (response != null) {
            response.completeExceptionally(error);
        }
    }

    @Override
    public CompletionStage<?> onClose(
            WebSocket ws,
            int statusCode,
            String reason
    ) {
        onError(ws, new IllegalStateException(
                "Discovery server disconnected: " + reason
        ));

        return null;
    }

    @Override
    public synchronized void close() {
        if (socket == null) {
            return;
        }
        try {
            if (!socket.isOutputClosed()) {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "Closed").join();
            }
        } finally {
            socket = null;
        }
    }
}
