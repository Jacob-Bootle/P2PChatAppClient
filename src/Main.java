import chat.Chat;
import chat.ChatSocketServer;

void main() throws IOException {
    //User user = new User();
    //user.setup();
    System.out.println("Enter mode: ");
    Scanner reader = new Scanner(System.in);
    String option = reader.nextLine();
    Chat chat;
    System.out.println("Enter your name:");
    String name = reader.nextLine();


    if ("server".equals(option)) {
        System.out.println("Starting server...");
        ChatSocketServer chatSocketServer = new ChatSocketServer(6667);
        chat = new Chat(chatSocketServer, name);
        chatSocketServer.setChat(chat);
        System.out.println("Created chat.");
    } else {
        System.out.println("Enter IP:");
        String ip = reader.nextLine();
        System.out.println("Enter port:");
        int port = reader.nextInt();
        chat = new Chat(ip, port, name);
        System.out.println("Created chat.");
    }

    String myName = chat.getMyName();
    while (true) {
        String message = reader.nextLine();
        System.out.print("\033[1A\033[2K");
        String full_message = "from " + myName + " " + message;
        chat.sendMessage(full_message);
    }
}