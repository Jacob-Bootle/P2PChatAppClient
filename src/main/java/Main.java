import chat.Chat;
import discovery.DiscoveryManager;
import discovery.DiscoveryServerException;
import discovery.DiscoveryStruct;
import utils.User;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public final class Main {
    private final DiscoveryManager discoveryManager;

    private Main() {
        this.discoveryManager = new DiscoveryManager();
    }

    static void main() {
        new Main().run();
    }

    private void run() {
        try (Scanner reader = new Scanner(System.in); discoveryManager) {
            System.out.print("Enter your name: ");

            try (User user = new User(reader.nextLine())) {
                System.out.print(
                        "Listening port (blank to start without one): "
                );

                String portText = reader.nextLine().trim();

                if (!portText.isEmpty()) {
                    user.startServer(Integer.parseInt(portText));
                }

                printHelp();
                runCommandLoop(user, reader);
            }
        } catch (IOException
                 | IllegalArgumentException
                 | IllegalStateException
                 | CompletionException exception) {
            System.err.println(
                    "Could not run chat: " + exception.getMessage()
            );
        }
    }

    private void runCommandLoop(User user, Scanner reader) {
        while (reader.hasNextLine()) {
            String input = reader.nextLine();

            try {
                if (handleCommand(user, input)) {
                    return;
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                System.err.println("Chat interrupted; shutting down.");
                return;
            } catch (IOException
                     | IllegalArgumentException
                     | IllegalStateException
                     | CompletionException
                     | DiscoveryServerException
                     | ExecutionException
                     | TimeoutException exception) {
                System.err.println("Error: " + exception.getMessage());
            }
        }
    }

    private boolean handleCommand(User user, String input)
            throws IOException,
            DiscoveryServerException,
            ExecutionException,
            InterruptedException,
            TimeoutException {

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

                Chat chat = user.connect(
                        parts[1],
                        Integer.parseInt(parts[2])
                );

                displayHistory(
                        chat.getCounterpartName(),
                        user.openChat(chat.getCounterpartName())
                );
            }

            case "/discovery" -> {
                requireArguments(parts, 3, "/discovery <host> <port>");

                String host = parts[1];
                int port = Integer.parseInt(parts[2]);

                if (port < 1 || port > 65535) {
                    throw new IllegalArgumentException(
                            "Discovery port must be between 1 and 65535"
                    );
                }

                int listenerPort = user.getListeningPort();
                String listenerIP;

                try (DatagramSocket route = new DatagramSocket()) {
                    route.connect(InetAddress.getByName(host), port);
                    listenerIP = route.getLocalAddress().getHostAddress();
                }

                discoveryManager.joinDiscoveryServer(
                        host,
                        port,
                        user.getName(),
                        listenerIP,
                        listenerPort
                );

                System.out.println(
                        "Connected to discovery server at "
                                + host + ":" + port + "."
                );
            }

            case "/list" -> {
                requireArguments(parts, 1, "/list");

                DiscoveryStruct response = discoveryManager.getList();
                boolean foundUser = false;

                if (response.names != null) {
                    for (DiscoveryStruct.ClientInfo person : response.names) {
                        if (!user.getName().equals(person.name)) {
                            System.out.println("Name: " + person.name);
                            System.out.println("IP: " + person.ip);
                            System.out.println("Port: " + person.port);
                            if (response.names.size() > 1) {
                                System.out.println("------------------");
                            }
                            foundUser = true;
                        }
                    }
                }

                if (!foundUser) {
                    System.out.println("No other users are connected.");
                }
            }

            case "/undiscover" -> {
                requireArguments(parts, 1, "/undiscover");
                discoveryManager.close();

                System.out.println(
                        "Disconnected from the discovery server."
                );
            }

            case "/chats" -> {
                requireArguments(parts, 1, "/chats");
                displayChats(user);
            }

            case "/open" -> {
                requireArguments(parts, 2, "/open <name>");
                displayHistory(parts[1], user.openChat(parts[1]));
            }

            case "/history" -> {
                requireArguments(parts, 1, "/history");

                Chat chat = user.getActiveChat().orElseThrow(
                        () -> new IllegalStateException(
                                "No chat is currently open"
                        )
                );

                displayHistory(
                        chat.getCounterpartName(),
                        chat.getChatHistory()
                );
            }

            case "/close" -> {
                requireArguments(parts, 1, "/close");
                user.closeActiveView();
            }

            case "/disconnect" -> {
                requireArguments(parts, 1, "/disconnect");
                user.disconnectActiveChat();
            }

            case "/help" -> {
                requireArguments(parts, 1, "/help");
                printHelp();
            }

            case "/quit" -> {
                requireArguments(parts, 1, "/quit");
                return true;
            }

            default -> System.out.println(
                    "Unknown command. Type /help for the command list."
            );
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
            String selected = user.getActiveChat()
                    .filter(chat::equals)
                    .isPresent() ? "*" : " ";

            String state = chat.isOpen() ? "open" : "closed";

            System.out.printf(
                    "%s %s (%s, %s, %d messages)%n",
                    selected,
                    chat.getCounterpartName(),
                    state,
                    chat.getDirection().name().toLowerCase(),
                    chat.getChatHistory().size()
            );
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

    private static void requireArguments(
            String[] parts,
            int expected,
            String usage
    ) {
        if (parts.length != expected) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println(
                "  /listen <port>             accept chats on another port"
        );
        System.out.println(
                "  /connect <host> <port>     connect to another user"
        );
        System.out.println(
                "  /discovery <host> <port>   connect to a discovery server"
        );
        System.out.println(
                "  /undiscover                disconnect from the discovery server"
        );
        System.out.println(
                "  /list                      list the users to chat to"
        );
        System.out.println(
                "  /chats                     list all chats"
        );
        System.out.println(
                "  /open <name>               select a chat and show its history"
        );
        System.out.println(
                "  /history                   show the selected chat's history"
        );
        System.out.println(
                "  /close                     close the chat (but keep receiving)"
        );
        System.out.println(
                "  /disconnect                disconnect but preserve history"
        );
        System.out.println(
                "  /help                      show this command list"
        );
        System.out.println(
                "  /quit                      close everything and exit"
        );
        System.out.println(
                "Any other text is sent to the selected chat."
        );
    }
}