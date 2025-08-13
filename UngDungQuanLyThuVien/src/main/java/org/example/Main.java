package org.example;

public class Main {
    public static void main(String[] args) {
        String mode = (args.length > 0) ? args[0] : "admin"; // "admin" hoặc "user"
        javax.swing.SwingUtilities.invokeLater(() -> {
            if ("admin".equalsIgnoreCase(mode)) {
                new AdminGUI();
            } else {
                new UserGUI("23"); // đổi thành user_id bạn muốn test
            }
        });
    }
}