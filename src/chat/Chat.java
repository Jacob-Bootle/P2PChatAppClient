package chat;

import java.io.IOException;
import java.util.Vector;

public class Chat {
    Vector<String> chatHistory = new Vector<>();
    String chatName = null;
    ChatSocket chatSocket;

    public Chat(String ip, int port) {
        try {
            this.chatSocket = new ChatSocketClient(ip, port, this);
            this.chatSocket.recieveMessage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Chat(ChatSocket chatSocketServer) {
        this.chatSocket = chatSocketServer;
        this.chatSocket.recieveMessage();
    }

    public void receiveMessage(String message) {
        String[] parts = message.split(" ", 3);

        if (parts.length < 2) {
            return;
        }
        String command = parts[0];
        String sender = parts[1];

        switch (command) {
            case "from":
                String content = (parts.length == 3) ? parts[2] : "";
                String printableMessage = sender + ": " + content;
                System.out.println(printableMessage);
                this.chatHistory.add(printableMessage);
                break;
            case "quit":
                System.out.println(sender + " quit the chat.");
                break;
            default:
                break;
        }
    }

    public void sendMessage(String message) throws IOException {
        this.chatSocket.sendMessage(message);
    }
}
