package chat;

import java.io.IOException;
import java.util.Vector;

public class Chat {
    Vector<String> chatHistory = new Vector<>();
    ChatSocket chatSocket;
    String myName;
    String counterpartName;

    public Chat(String ip, int port, String name) {
        try {
            this.myName = name;
            this.chatSocket = new ChatSocketClient(ip, port, this);
            this.chatSocket.recieveMessage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Chat(ChatSocket chatSocketServer, String name) {
        this.chatSocket = chatSocketServer;
        this.myName = name;
        this.chatSocket.recieveMessage();
    }

    public void receiveMessage(String message) {
        String[] parts = message.split(" ", 3);

        if (parts.length < 2) {
            return;
        }
        String command = parts[0];
        String sender = parts[1];
        String content;
        switch (command) {
            case "from":
                content = (parts.length == 3) ? parts[2] : "";
                String printableMessage = sender + ": " + content;
                System.out.println(printableMessage);
                this.chatHistory.add(printableMessage);
                break;
            case "quit":
                System.out.println(sender + " quit the chat.");
                this.chatHistory.add(sender + " quit the chat.");
                break;
            case "hello":
                content = (parts.length == 3) ? parts[2] : "";
                this.counterpartName = content;
                break;
            default:
                break;
        }
    }

    public void sendMessage(String message) throws IOException {
        String[] parts = message.split(" ", 3);

        if (parts.length > 0 && "from".equals(parts[0]) && (parts.length != 3 || "".equals(parts[2]))) {
            return;
        }

        if (parts.length == 3) {
            if ("from".equals(parts[0])) {
                String printableMessage = parts[1] + ": " + parts[2];
                this.chatHistory.add(printableMessage);
            }
        }
        this.chatSocket.sendMessage(message);
    }

    public String getMyName() {
        return this.myName;
    }
}
