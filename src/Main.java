import java.util.Scanner;

void main() {
    try (Socket s = new Socket("localhost", 54321)) {
        DataOutputStream d = new DataOutputStream(s.getOutputStream());
        Scanner reader = new Scanner(System.in);

        boolean active = true;

        while (active) {
            String message = reader.next();
            d.writeUTF(message);
            d.flush();
        }
    } catch (Exception e) {
        System.out.println("Client Error: " + e);
    }
}