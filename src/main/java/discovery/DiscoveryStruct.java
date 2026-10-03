package discovery;

import java.util.List;

public class DiscoveryStruct {
    public String type;
    public String message;
    public List<ClientInfo> names;

    public static class ClientInfo {
        public String name;
        public int port;
        public String ip;
    }
}