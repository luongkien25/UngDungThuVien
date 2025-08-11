package org.example;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import org.example.Library;


public class BookPanel extends JPanel {
    private final Book book;

    public BookPanel(Book book) {
        this.book = book;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        setBackground(Color.WHITE);

        // === Ảnh sách ===
        JLabel imageLabel = new JLabel();
        imageLabel.setPreferredSize(new Dimension(100, 150));

        String imageUrl = book.getThumbnailLink();
        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("N/A")) {
            try {
                Image image = ImageIO.read(new URL(imageUrl));
                Image scaledImage = image.getScaledInstance(100, 150, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaledImage));
            } catch (IOException e) {
                System.err.println("Could not load image: " + imageUrl);
                imageLabel.setText("No Image");
            }
        } else {
            imageLabel.setText("No Image");
        }

        // === Thông tin sách ===
        JTextArea infoArea = new JTextArea(book.toString());
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setBackground(null);
        infoArea.setBorder(null);
        infoArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // === Nút "Borrow" ===
        JButton borrowButton = new JButton("Borrow");
        borrowButton.addActionListener(e -> {
            Book borrowedBook = new Book(
                    book.getTitle(),
                    book.getAuthors(),
                    book.getCategory(),
                    book.getIsbn(),
                    1,
                    book.getThumbnailLink()
            );

            Library library = Library.getInstance();
            LibraryUser currentUser = Session.getCurrentUser();
            if (currentUser != null) {
                library.borrowBook(currentUser, borrowedBook);
                JOptionPane.showMessageDialog(this, "You borrowed: " + book.getTitle());
            } else {
                JOptionPane.showMessageDialog(this, "No user logged in");
            }
        });

        // === Tổ chức layout ===
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(infoArea, BorderLayout.CENTER);
        centerPanel.add(borrowButton, BorderLayout.SOUTH);

        add(imageLabel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
    }
}
