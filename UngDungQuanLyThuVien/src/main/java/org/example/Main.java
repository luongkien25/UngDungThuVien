package org.example;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Tăng ngân sách cache ngay khi khởi động (VD: 2 GB)
        ImageCache.getInstance().setMaxBytes(2_500L * 1024 * 1024);

        // Chỉ chạy khi JVM tắt (giữ cache suốt vòng đời app)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                ImageCache.getInstance().shutdown();
                System.out.println("Image cache closed.");
            } catch (Throwable ignore) {}
        }));

        SwingUtilities.invokeLater(LibraryAppUI::new);
    }
}

