package org.example;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;

public class BookPanel extends JPanel {
    private final Book book;
    private final Runnable onRefresh; // callback để render lại danh sách sau Update/Remove
    private JButton borrowButton;
    private JButton updateButton;
    private JButton removeButton;

    public BookPanel(Book book) {
        this(book, true); // giữ tương thích cũ
    }

    public BookPanel(Book book, boolean showBorrow) {
        this(book, showBorrow, false, false, null);
    }

    // Constructor đầy đủ: chọn hiển thị Borrow/Update/Remove + callback refresh
    public BookPanel(Book book, boolean showBorrow, boolean showUpdate, boolean showRemove, Runnable onRefresh) {
        this.book = book;
        this.onRefresh = onRefresh;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        setBackground(Color.WHITE);

        // === Ảnh sách ===
        JLabel imageLabel = new JLabel();
        imageLabel.setPreferredSize(new Dimension(100, 150));

        String imageUrl = book.getThumbnailLink();
        if (imageUrl != null && !imageUrl.isEmpty() && !"N/A".equals(imageUrl)) {
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

        // === Khu nút chức năng ===
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionPanel.setOpaque(false);

        // Borrow
        if (showBorrow) {
            borrowButton = new JButton("Borrow");
            borrowButton.addActionListener(e -> {
                LibraryUser currentUser = Session.getCurrentUser();
                if (currentUser instanceof User u) {
                    try {
                        u.borrowBook(book, new UserDAO()); // DB trước, RAM sau
                        JOptionPane.showMessageDialog(this, "You borrowed: " + book.getTitle());
                        refreshIfNeeded();
                    } catch (RuntimeException ex) {
                        JOptionPane.showMessageDialog(this, "Borrow failed: " + ex.getMessage());
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "No user logged in");
                }
            });
            actionPanel.add(borrowButton);
        }

        // Update
        if (showUpdate) {
            updateButton = new JButton("Update");
            updateButton.addActionListener(e -> {
                String newTitle = JOptionPane.showInputDialog(this, "New title:", book.getTitle());
                if (newTitle == null) return;

                String newAuthors = JOptionPane.showInputDialog(this, "New authors:", book.getAuthors());
                if (newAuthors == null) return;

                String newCategory = JOptionPane.showInputDialog(this, "New category:", book.getCategory());
                if (newCategory == null) return;

                String qtyStr = JOptionPane.showInputDialog(this, "New quantity:", book.getQuantity());
                if (qtyStr == null) return;

                int newQty;
                try {
                    newQty = Integer.parseInt(qtyStr.trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid quantity.");
                    return;
                }

                try {
                    new BookDAO().updateBook(book.getIsbn(), newTitle, newAuthors, newCategory, newQty);
                    // cập nhật object trong RAM
                    book.setTitle(newTitle);
                    book.setDescription(newAuthors);
                    book.setQuantity(newQty);
                    book.setCategory(newCategory);
                    JOptionPane.showMessageDialog(this, "Document updated.");
                    refreshIfNeeded();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
                }
            });
            actionPanel.add(updateButton);
        }

        // Remove
        if (showRemove) {
            removeButton = new JButton("Remove");
            removeButton.addActionListener(e -> {
                int ok = JOptionPane.showConfirmDialog(this,
                        "Remove this document?\n" + book.getTitle(),
                        "Confirm", JOptionPane.YES_NO_OPTION);
                if (ok != JOptionPane.YES_OPTION) return;

                try {
                    new BookDAO().removeBook(book.getIsbn());        // xóa DB
                    Library.getInstance().removeItem(book);          // xóa RAM
                    JOptionPane.showMessageDialog(this, "Document removed.");
                    refreshIfNeeded();
                } catch (java.sql.SQLException exSql) {
                    // case: đang có người mượn
                    JOptionPane.showMessageDialog(this, "This book is currently borrowed by another user.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            });
            actionPanel.add(removeButton);
        }

        // Center panel = info + action buttons
        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(infoArea, BorderLayout.CENTER);
        centerPanel.add(actionPanel, BorderLayout.SOUTH);

        add(imageLabel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void refreshIfNeeded() {
        if (onRefresh != null) {
            SwingUtilities.invokeLater(onRefresh);
        }
    }

    // tiện nếu muốn điều khiển bằng code
    public void setBorrowVisible(boolean visible) {
        if (borrowButton != null) borrowButton.setVisible(visible);
    }
    public void setUpdateVisible(boolean visible) {
        if (updateButton != null) updateButton.setVisible(visible);
    }
    public void setRemoveVisible(boolean visible) {
        if (removeButton != null) removeButton.setVisible(visible);
    }
}
