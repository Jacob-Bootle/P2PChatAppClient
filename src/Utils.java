public class Utils {
    public static void clearTerminal() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
