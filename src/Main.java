import chat.Chat;
import utils.User;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public final class Main {
    private Main() {
    }

    static void main() {
        try (Scanner reader = new Scanner(System.in)) {
            System.out.print("Enter your name: ");
            User user = new User(reader.nextLine());

            System.out.print("Listening port (blank to start without one): ");
            String portText = reader.nextLine().trim();
            if (!portText.isEmpty()) {
                user.startServer(Integer.parseInt(portText));
            }

            printHelp();
            runCommandLoop(user, reader);
        } catch (IOException exception) {
            System.err.println("Could not start chat: " + exception.getMessage());
        }
    }

    private static void runCommandLoop(User user, Scanner reader) {
        try (user) {
            while (reader.hasNextLine()) {
                String input = reader.nextLine();
                try {
                    if (handleCommand(user, input)) {
                        return;
                    }
                } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
                    System.out.println("Error: " + exception.getMessage());
                }
            }
        }
    }

    private static boolean handleCommand(User user, String input) throws IOException {
        if (!input.startsWith("/")) {
            user.sendMessage(input);
            return false;
        }

        String[] parts = input.trim().split("\\s+");
        switch (parts[0]) {
            case "/listen" -> {
                requireArguments(parts, 2, "/listen <port>");
                user.startServer(Integer.parseInt(parts[1]));
            }
            case "/connect" -> {
                requireArguments(parts, 3, "/connect <host> <port>");
                Chat chat = user.connect(parts[1], Integer.parseInt(parts[2]));
                displayHistory(chat.getCounterpartName(), user.openChat(chat.getCounterpartName()));
            }
            case "/chats" -> displayChats(user);
            case "/open" -> {
                requireArguments(parts, 2, "/open <name>");
                displayHistory(parts[1], user.openChat(parts[1]));
            }
            case "/history" -> {
                Chat chat = user.getActiveChat()
                        .orElseThrow(() -> new IllegalStateException("No chat is currently open"));
                displayHistory(chat.getCounterpartName(), chat.getChatHistory());
            }
            case "/close" -> user.closeActiveView();
            case "/disconnect" -> user.disconnectActiveChat();
            case "/help" -> printHelp();
            case "/quit" -> {
                return true;
            }
            default -> System.out.println("Unknown command. Type /help for the command list.");
        }
        return false;
    }

    private static void displayChats(User user) {
        List<Chat> chats = user.getChats();
        if (chats.isEmpty()) {
            System.out.println("No chats yet.");
            return;
        }
        for (Chat chat : chats) {
            String selected = user.getActiveChat().filter(chat::equals).isPresent() ? "*" : " ";
            String state = chat.isOpen() ? "open" : "closed";
            System.out.printf("%s %s (%s, %s, %d messages)%n", selected,
                    chat.getCounterpartName(), state, chat.getDirection().name().toLowerCase(),
                    chat.getChatHistory().size());
        }
    }

    private static void displayHistory(String name, List<String> history) {
        System.out.println("--- Chat with " + name + " ---");
        if (history.isEmpty()) {
            System.out.println("(no messages yet)");
        } else {
            history.forEach(System.out::println);
        }
    }

    private static void requireArguments(String[] parts, int expected, String usage) {
        if (parts.length != expected) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  /listen <port>         accept chats on another port");
        System.out.println("  /connect <host> <port> connect to another user");
        System.out.println("  /chats                 list all chats");
        System.out.println("  /open <name>           select a chat and show its history");
        System.out.println("  /history               show the selected chat's history");
        System.out.println("  /close                 close the view (keep receiving)");
        System.out.println("  /disconnect            disconnect but preserve history");
        System.out.println("  /quit                   close everything and exit");
        System.out.println("Any other text is sent to the selected chat.");
    }
}
