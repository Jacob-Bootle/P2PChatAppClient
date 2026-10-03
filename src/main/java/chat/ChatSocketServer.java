package chat;

import utils.Protocol;
import utils.User;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ChatSocketServer implements AutoCloseable {
    private static final int HANDSHAKE_TIMEOUT_MILLIS = 10_000;

    private final ServerSocket serverSocket;
    private final User user;
    private final Set<AcceptedConnection> connections = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    public ChatSocketServer(int port, User user) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.user = user;
    }

    public void start() {
        if (closed.get()) {
            throw new IllegalStateException("This chat server is closed");
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }

        Thread acceptThread = new Thread(this::acceptLoop,
                "chat-acceptor-" + getPort());
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                Thread setupThread = new Thread(() -> setUpClient(socket),
                        "chat-handshake-" + socket.getRemoteSocketAddress());
                setupThread.setDaemon(true);
                setupThread.start();
            } catch (SocketException exception) {
                if (running.get()) {
                    user.publishStatus("The chat server stopped: " + exception.getMessage());
                }
            } catch (IOException exception) {
                user.publishStatus("Could not accept a chat connection: " + exception.getMessage());
            }
        }
    }

    private void setUpClient(Socket socket) {
        AcceptedConnection connection = null;
        try {
            socket.setSoTimeout(HANDSHAKE_TIMEOUT_MILLIS);
            connection = new AcceptedConnection(socket);
            connections.add(connection);

            String counterpartName = Protocol.readHello(connection.readHandshakeMessage());
            connection.sendMessage(Protocol.hello(user.getName()));
            socket.setSoTimeout(0);

            Chat chat = new Chat(user, connection, user.getName(), counterpartName,
                    Chat.Direction.INCOMING);
            connection.attachChat(chat);
            if (!user.registerIncomingChat(chat)) {
                connection.closeConnection();
                return;
            }
            connection.startReceiving();
        } catch (SocketTimeoutException exception) {
            user.publishStatus("An incoming connection did not identify itself in time.");
            closeQuietly(connection, socket);
        } catch (IOException | RuntimeException exception) {
            user.publishStatus("Could not create an incoming chat: " + exception.getMessage());
            closeQuietly(connection, socket);
        }
    }

    private static void closeQuietly(AcceptedConnection connection, Socket socket) {
        if (connection != null) {
            connection.closeConnection();
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        running.set(false);
        try {
            serverSocket.close();
        } catch (IOException ignored) {}

        for (AcceptedConnection connection : Set.copyOf(connections)) {
            Chat chat = connection.attachedChat();
            if (chat != null) {
                chat.connectionLost();
            }
            connection.closeConnection();
        }
        connections.clear();
    }

    private final class AcceptedConnection extends ChatSocketConnection {
        AcceptedConnection(Socket socket) throws IOException {
            super(socket);
        }

        @Override
        public void closeConnection() {
            super.closeConnection();
            connections.remove(this);
        }
    }
}
