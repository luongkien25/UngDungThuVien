package org.example;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        ImageCache.getInstance().setMaxBytes(2_500L * 1024 * 1024);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                ImageCache.getInstance().shutdown();
                System.out.println("Image cache closed.");
            } catch (Throwable ignore) {}
        }));

        SwingUtilities.invokeLater(LibraryAppUI::new);
    }
}

