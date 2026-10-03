package utils;

import java.io.IOException;

public final class Protocol {
    private Protocol() {
    }

    public static String hello(String name) {
        return "hello " + name;
    }

    public static String message(String sender, String content) {
        return "from " + sender + " " + content;
    }

    public static String quit(String name) {
        return "quit " + name;
    }

    public static String readHello(String message) throws IOException {
        String[] parts = message.split(" ", 2);
        if (parts.length != 2 || !"hello".equals(parts[0]) || parts[1].isBlank()) {
            throw new IOException("Expected 'hello <name>' as the first message");
        }
        return parts[1];
    }

    public static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A name is required");
        }
        String trimmed = name.trim();
        if (trimmed.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Names cannot contain spaces");
        }
        return trimmed;
    }
}
