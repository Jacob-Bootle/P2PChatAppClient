import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Scanner;

public class User {
    private String name;
    private String port;
    private String ip;
    private final Scanner reader = new Scanner(System.in);

    public void setup() {
        /* Main setup - used to set the name of the user, port and IP */
        System.out.println("Welcome");
        System.out.println("Please enter your name:");
        this.name = reader.next();
        System.out.println("Name set to: " + this.name);
        System.out.println("Please enter port number to be used (this will be used by other users to chat to you, suggested port is 6667):");
        this.port = reader.next();
        System.out.println("Port set to: " + this.port);
        try {
            URL url = new URL("https://api.ipify.org");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            this.ip = in.readLine();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Setup complete...");
        this.mainMenu();
    }

    public void mainMenu() {
        Utils.clearTerminal();
        System.out.println("-----Main menu-----");
        System.out.println("1 - Start new chat");
        System.out.println("2 - Join existing chat");
        System.out.println("3 - View your ip and port (to share with other users)");
        System.out.println("4 - Quit application");
        String choice = reader.next();

        switch (choice) {
            case "1":
                Utils.clearTerminal();
                System.out.println("Starting new chat...");
                this.startNewChat();
                break;
            case "2":
                Utils.clearTerminal();
                System.out.println("Joining existing chat...");
                break;
            case "3":
                Utils.clearTerminal();
                System.out.println("Share this with users to chat with them:");
                System.out.println(this.ip + ":" + this.port);
                break;
            case "4":
                Utils.clearTerminal();
                System.out.println("Quitting...");
                System.exit(0);
                break;
            default:
                Utils.clearTerminal();
                System.out.println("Invalid option, ensure you only choose from the numbers 1-4");
                this.mainMenu();
        }
    }

    public void startNewChat() {

    }
}