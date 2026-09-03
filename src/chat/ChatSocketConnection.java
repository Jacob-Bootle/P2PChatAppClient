package chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

abstract class ChatSocketConnection implements ChatSocket {
    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;
    private final Object outputLock = new Object();
    private final AtomicBoolean receiving = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile Chat chat;

    ChatSocketConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.input = new DataInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
    }

    final String readHandshakeMessage() throws IOException {
        return input.readUTF();
    }

    public final void attachChat(Chat chat) {
        if (chat == null) {
            throw new IllegalArgumentException("A connection must be attached to a chat");
        }
        if (this.chat != null) {
            throw new IllegalStateException("This connection already belongs to a chat");
        }
        this.chat = chat;
    }

    final Chat attachedChat() {
        return chat;
    }

    final Socket rawSocket() {
        return socket;
    }

    @Override
    public final void sendMessage(String message) throws IOException {
        if (!isOpen()) {
            throw new IOException("The connection is closed");
        }
        synchronized (outputLock) {
            output.writeUTF(message);
            output.flush();
        }
    }

    @Override
    public final void startReceiving() {
        if (chat == null) {
            throw new IllegalStateException("Attach a chat before receiving messages");
        }
        if (!receiving.compareAndSet(false, true)) {
            return;
        }

        Thread receiver = new Thread(this::receiveLoop,
                "chat-receiver-" + chat.getCounterpartName());
        receiver.setDaemon(true);
        receiver.start();
    }

    private void receiveLoop() {
        try {
            while (isOpen()) {
                chat.receiveProtocolMessage(input.readUTF());
            }
        } catch (IOException exception) {
            if (!closed.get()) {
                chat.connectionLost();
            }
        } finally {
            closeConnection();
        }
    }

    @Override
    public final boolean isOpen() {
        return !closed.get() && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public void closeConnection() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {}
    }
}
