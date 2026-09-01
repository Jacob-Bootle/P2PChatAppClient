package chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatSocketServer implements ChatSocket {
    private ServerSocket serverSocket;
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private Chat chat;

    public ChatSocketServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.socket = serverSocket.accept();
        this.dataInputStream = new DataInputStream(this.socket.getInputStream());
        this.dataOutputStream = new DataOutputStream(this.socket.getOutputStream());
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }
    public void sendMessage(String message) throws IOException {
        this.dataOutputStream.writeUTF(message);
        this.dataOutputStream.flush();
    }

    public void recieveMessage() {
        new Thread(() -> {
            try {
                while (socket != null && socket.isConnected() && !socket.isClosed()) {
                    String incomingMessage = dataInputStream.readUTF();
                    this.chat.receiveMessage(incomingMessage);
                }
            } catch (IOException e) {
                System.out.println("Connection closed by the client.");
                e.printStackTrace();
            }
        }).start();
    }

    public void closeConnection() {
        try {
            if (dataInputStream != null) dataInputStream.close();
            if (dataOutputStream != null) dataOutputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}