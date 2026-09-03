package chat;

import utils.Protocol;
import utils.User;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Chat {
    public enum Direction {
        INCOMING,
        OUTGOING
    }

    private final CopyOnWriteArrayList<String> chatHistory = new CopyOnWriteArrayList<>();
    private final ChatSocket chatSocket;
    private final User owner;
    private final String myName;
    private final String counterpartName;
    private final Direction direction;
    private final AtomicBoolean open = new AtomicBoolean(true);

    public Chat(User owner, ChatSocket chatSocket, String myName, String counterpartName,
                Direction direction) {
        this.owner = owner;
        this.chatSocket = chatSocket;
        this.myName = Protocol.validateName(myName);
        this.counterpartName = Protocol.validateName(counterpartName);
        this.direction = direction;
    }

    public void sendMessage(String content) throws IOException {
        if (content == null || content.isBlank()) {
            return;
        }
        if (!isOpen()) {
            throw new IOException("The chat with " + counterpartName + " is closed");
        }

        try {
            chatSocket.sendMessage(Protocol.message(myName, content));
            record(myName + ": " + content);
        } catch (IOException exception) {
            connectionLost();
            throw exception;
        }
    }

    void receiveProtocolMessage(String message) {
        String[] parts = message.split(" ", 3);
        if (parts.length < 2) {
            return;
        }

        String command = parts[0];
        String sender = parts[1];
        if (!counterpartName.equals(sender)) {
            owner.publishStatus("Ignored a message with the wrong sender name on the chat with "
                    + counterpartName + ".");
            return;
        }

        switch (command) {
            case "from" -> {
                String content = parts.length == 3 ? parts[2] : "";
                record(sender + ": " + content);
            }
            case "quit" -> remoteQuit();
            default -> owner.publishStatus("Ignored an unknown chat command from " + sender + ".");
        }
    }

    private void remoteQuit() {
        if (open.compareAndSet(true, false)) {
            record("[system] " + counterpartName + " closed the chat.");
        }
        chatSocket.closeConnection();
    }

    void connectionLost() {
        if (open.compareAndSet(true, false)) {
            record("[system] Connection to " + counterpartName + " closed.");
        }
    }

    public void close() {
        if (!open.compareAndSet(true, false)) {
            return;
        }
        try {
            chatSocket.sendMessage(Protocol.quit(myName));
        } catch (IOException ignored) {} finally {
            chatSocket.closeConnection();
        }
        record("[system] You closed the chat with " + counterpartName + ".");
    }

    private void record(String printableMessage) {
        chatHistory.add(printableMessage);
        owner.chatUpdated(this, printableMessage);
    }

    public List<String> getChatHistory() {
        return List.copyOf(chatHistory);
    }

    public String getCounterpartName() {
        return counterpartName;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isOpen() {
        return open.get() && chatSocket.isOpen();
    }
}
