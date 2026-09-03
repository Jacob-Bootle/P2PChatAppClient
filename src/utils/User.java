package utils;

import chat.Chat;
import chat.ChatSocketClient;
import chat.ChatSocketServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class User implements AutoCloseable {
    private final String name;
    private final ConcurrentMap<String, Chat> chats = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<ChatSocketServer> servers = new CopyOnWriteArrayList<>();
    private volatile String activeChatName;
    private final Consumer<String> notificationSink = System.out::println;

    public User(String name) {
        this.name = Protocol.validateName(name);
    }

    public String getName() {
        return name;
    }

    public void startServer(int port) throws IOException {
        ChatSocketServer server = new ChatSocketServer(port, this);
        servers.add(server);
        server.start();
        publishStatus("Listening for chats on port " + server.getPort() + ".");
    }

    public Chat connect(String ip, int port) throws IOException {
        ChatSocketClient connection = new ChatSocketClient(ip, port, name);
        Chat chat = new Chat(this, connection, name, connection.getCounterpartName(),
                Chat.Direction.OUTGOING);
        connection.attachChat(chat);

        if (registerChat(chat)) {
            connection.closeConnection();
            throw new IOException("A chat with " + chat.getCounterpartName() + " already exists");
        }
        connection.startReceiving();
        publishStatus("Connected to " + chat.getCounterpartName() + ".");
        return chat;
    }

    public boolean registerIncomingChat(Chat chat) {
        if (registerChat(chat)) {
            publishStatus("Rejected a duplicate chat from " + chat.getCounterpartName() + ".");
            return false;
        }
        publishStatus("New incoming chat from " + chat.getCounterpartName() + ".");
        return true;
    }

    private boolean registerChat(Chat chat) {
        return chats.putIfAbsent(chat.getCounterpartName(), chat) != null;
    }

    public List<String> openChat(String counterpartName) {
        Chat chat = requireChat(counterpartName);
        activeChatName = chat.getCounterpartName();
        return chat.getChatHistory();
    }

    public Optional<Chat> getActiveChat() {
        String selected = activeChatName;
        return selected == null ? Optional.empty() : Optional.ofNullable(chats.get(selected));
    }

    public List<Chat> getChats() {
        ArrayList<Chat> result = new ArrayList<>(chats.values());
        result.sort(Comparator.comparing(Chat::getCounterpartName));
        return List.copyOf(result);
    }

    public void sendMessage(String content) throws IOException {
        Chat chat = getActiveChat()
                .orElseThrow(() -> new IllegalStateException("Open a chat before sending a message"));
        chat.sendMessage(content);
    }

    public void closeChat(String counterpartName) {
        Chat chat = requireChat(counterpartName);
        chat.close();
        if (counterpartName.equals(activeChatName)) {
            activeChatName = null;
        }
    }

    public void closeActiveView() {
        activeChatName = null;
    }

    public void disconnectActiveChat() {
        Chat chat = getActiveChat()
                .orElseThrow(() -> new IllegalStateException("No chat is currently open"));
        closeChat(chat.getCounterpartName());
    }

    public void chatUpdated(Chat chat, String printableMessage) {
        if (chat.getCounterpartName().equals(activeChatName)) {
            publishStatus(printableMessage);
        } else {
            publishStatus("[Chat with " + chat.getCounterpartName() + "] " + printableMessage);
        }
    }

    public void publishStatus(String status) {
        try {
            notificationSink.accept(status);
        } catch (RuntimeException ignored) {}
    }

    private Chat requireChat(String counterpartName) {
        Chat chat = chats.get(counterpartName);
        if (chat == null) {
            throw new IllegalArgumentException("No chat exists with " + counterpartName);
        }
        return chat;
    }

    @Override
    public void close() {
        for (Chat chat : chats.values()) {
            chat.close();
        }
        for (ChatSocketServer server : servers) {
            server.close();
        }
        servers.clear();
        activeChatName = null;
    }
}
