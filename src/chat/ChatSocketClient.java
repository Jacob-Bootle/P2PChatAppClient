package chat;

import utils.Protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class ChatSocketClient extends ChatSocketConnection {
    private static final int CONNECT_TIMEOUT_MILLISECONDS = 10_000;
    private static final int HANDSHAKE_TIMEOUT_MILLISECONDS = 10_000;
    private final String counterpartName;

    public ChatSocketClient(String ip, int port, String localName) throws IOException {
        super(connect(ip, port));

        String checkedName = Protocol.validateName(localName);
        try {
            rawSocket().setSoTimeout(HANDSHAKE_TIMEOUT_MILLISECONDS);
            sendMessage(Protocol.hello(checkedName));
            this.counterpartName = Protocol.validateName(
                    Protocol.readHello(readHandshakeMessage()));
            rawSocket().setSoTimeout(0);
        } catch (IOException | RuntimeException exception) {
            closeConnection();
            throw exception;
        }
    }

    public String getCounterpartName() {
        return counterpartName;
    }

    private static Socket connect(String ip, int port) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(ip, port), CONNECT_TIMEOUT_MILLISECONDS);
        return socket;
    }
}
