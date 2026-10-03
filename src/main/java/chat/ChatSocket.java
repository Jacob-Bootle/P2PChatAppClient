package chat;

import java.io.IOException;

public interface ChatSocket extends AutoCloseable {
    void sendMessage(String message) throws IOException;

    void startReceiving();

    boolean isOpen();

    void closeConnection();

    @Override
    default void close() {
        closeConnection();
    }
}
