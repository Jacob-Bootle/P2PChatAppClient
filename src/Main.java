void main() {
    try (Socket s = new Socket("localhost", 54321)) {
        DataOutputStream d = new DataOutputStream(s.getOutputStream());
        d.writeUTF("Hello, World!");
        d.flush();
    } catch (Exception e) {
        System.out.println("Client Error: " + e);
    }
}