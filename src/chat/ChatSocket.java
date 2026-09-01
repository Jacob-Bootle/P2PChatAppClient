package chat;

import java.io.IOException;

public interface ChatSocket {
    void sendMessage(String message) throws IOException;
    void recieveMessage();
    void closeConnection();
}
