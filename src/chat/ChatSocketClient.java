package chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ChatSocketClient implements ChatSocket {
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private Chat chat;

    public ChatSocketClient(String ip, int port, Chat chat) throws IOException {
        this.socket = new Socket(ip, port);
        this.dataInputStream = new DataInputStream(this.socket.getInputStream());
        this.dataOutputStream = new DataOutputStream(this.socket.getOutputStream());
        this.chat = chat;
        this.sendMessage("hello " + this.chat.myName + " " +this.chat.myName);
    }

    @Override
    public void sendMessage(String message) throws IOException {
        this.dataOutputStream.writeUTF(message);
        this.dataOutputStream.flush();
    }

    @Override
    public void recieveMessage() {
        new Thread(() -> {
            try {
                while (socket.isConnected() && !socket.isClosed()) {
                    String incomingMessage = dataInputStream.readUTF();
                    this.chat.receiveMessage(incomingMessage);
                }
            } catch (IOException e) {
                System.out.println("Connection closed by the server.");
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void closeConnection() {
        try {

            if (dataInputStream != null) dataInputStream.close();
            if (dataOutputStream != null) dataOutputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
