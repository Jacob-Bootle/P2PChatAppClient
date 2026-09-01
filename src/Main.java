import chat.Chat;
import chat.ChatSocketServer;

void main() throws IOException {
    //User user = new User();
    //user.setup();
    System.out.println("Enter mode: ");
    Scanner reader = new Scanner(System.in);
    String option = reader.nextLine();
    Chat chat;

    if (Objects.equals(option, "server")) {
        System.out.println("Starting server...");
        ChatSocketServer chatSocketServer = new ChatSocketServer(6667);
        chat = new Chat(chatSocketServer);
        chatSocketServer.setChat(chat);
        System.out.println("Created chat.");
    } else {
        System.out.println("Enter IP:");
        String ip = reader.nextLine();
        System.out.println("Enter port:");
        int port = reader.nextInt();
        chat = new Chat(ip, port);
        System.out.println("Created chat.");
    }

    while (true) {
        String message = reader.nextLine();
        String full_message = "from Jacob " + message;
        System.out.println("Jacob: " + message);
        chat.sendMessage(full_message);
    }
}