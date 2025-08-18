package org.example;

import javax.swing.*;
import java.awt.*;

public class BookPanel extends JPanel {
    private final Book book;
    private final Runnable onRefresh;
    private JButton borrowButton;
    private JButton updateButton;
    private JButton removeButton;

    public BookPanel(Book book) {
        this(book, true);
    }

    public BookPanel(Book book, boolean showBorrow) {
        this(book, showBorrow, false, false, null);
    }

    public BookPanel(Book book, boolean showBorrow, boolean showUpdate, boolean showRemove, Runnable onRefresh) {
        this.book = book;
        this.onRefresh = onRefresh;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        setBackground(Color.WHITE);

        // === Ảnh sách (bất đồng bộ + cache) ===
        JLabel imageLabel = new JLabel();
        imageLabel.setName("cover");
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(136, 200));
        imageLabel.setMinimumSize(new Dimension(136, 200));
        imageLabel.setOpaque(true);
        imageLabel.setBackground(Color.WHITE);

        // Lấy placeholder ngay và đăng ký callback khi tải xong (đúng API: gọi qua getInstance())
        ImageIcon ph = ImageCache.getInstance().getIconAsync(
                book.getIsbn(), book.getThumbnailLink(), 200, imageLabel
        );
        imageLabel.setIcon(ph);

        // === Thông tin sách ===
        String title   = book.getTitle()   != null ? book.getTitle()   : "(No title)";
        String authors = book.getAuthors() != null ? book.getAuthors() : "(Unknown)";
        String cat     = book.getCategory()!= null ? book.getCategory(): "";
        String isbnTxt = (book.getIsbn() == null || book.getIsbn().isBlank() || "N/A".equalsIgnoreCase(book.getIsbn()))
                ? "N/A" : book.getIsbn();
        String info = title + " by " + authors +
                (cat.isBlank() ? "" : " (" + cat + ")") +
                " - ISBN: " + isbnTxt + " | Quantity: " + book.getQuantity();

        JTextArea infoArea = new JTextArea(info);
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setBackground(null);
        infoArea.setBorder(null);
        infoArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // === Khu nút chức năng ===
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionPanel.setOpaque(false);

        // Borrow (User dùng; Admin truyền showBorrow=false)
        if (showBorrow) {
            borrowButton = new JButton("Borrow");
            borrowButton.addActionListener(e -> {
                LibraryUser currentUser = Session.getCurrentUser();
                if (currentUser instanceof User u) {
                    try {
                        UserDAO dao = new UserDAO();
                        int active = dao.countActiveBorrowing(u.getUserId());
                        if (active >= 3) {
                            JOptionPane.showMessageDialog(this, "Bạn đã mượn đủ 3 cuốn, hãy trả bớt trước khi mượn tiếp.");
                            return;
                        }
                        u.borrowBook(book, dao);
                        JOptionPane.showMessageDialog(this, "You borrowed: " + book.getTitle());
                        refreshIfNeeded();
                    } catch (RuntimeException ex) {
                        JOptionPane.showMessageDialog(this, "Borrow failed: " + ex.getMessage());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "No user logged in");
                }
            });
            actionPanel.add(borrowButton);
        }

        // Update (ƯU TIÊN THEO ID, fallback ISBN)
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
                    BookDAO dao = new BookDAO();
                    boolean done = false;

                    String idStr = book.getId();
                    if (idStr != null && !idStr.isBlank()) {
                        dao.updateBookById(Integer.parseInt(idStr), newTitle, newAuthors, newCategory, newQty);
                        done = true;
                    } else {
                        String isbn = book.getIsbn();
                        if (isbn != null && !isbn.isBlank() && !"N/A".equalsIgnoreCase(isbn)) {
                            dao.updateBook(isbn, newTitle, newAuthors, newCategory, newQty);
                            done = true;
                        }
                    }
                    if (!done) throw new IllegalStateException("No id or isbn to update.");

                    // cập nhật object trong RAM
                    book.setTitle(newTitle);
                    book.setAuthors(newAuthors);
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

        // Remove (ƯU TIÊN THEO ID, fallback ISBN)
        if (showRemove) {
            removeButton = new JButton("Remove");
            removeButton.addActionListener(e -> {
                int ok = JOptionPane.showConfirmDialog(this,
                        "Remove this document?\n" + (book.getTitle() != null ? book.getTitle() : "(No title)"),
                        "Confirm", JOptionPane.YES_NO_OPTION);
                if (ok != JOptionPane.YES_OPTION) return;

                try {
                    BookDAO dao = new BookDAO();
                    boolean done = false;

                    String idStr = book.getId();
                    if (idStr != null && !idStr.isBlank()) {
                        dao.removeBookById(Integer.parseInt(idStr));
                        done = true;
                    } else {
                        String isbn = book.getIsbn();
                        if (isbn != null && !isbn.isBlank() && !"N/A".equalsIgnoreCase(isbn)) {
                            dao.removeBook(isbn);
                            done = true;
                        }
                    }
                    if (!done) throw new IllegalStateException("No id or isbn to remove.");

                    Library.getInstance().removeItem(book); // xóa RAM
                    JOptionPane.showMessageDialog(this, "Document removed.");
                    refreshIfNeeded();
                } catch (java.sql.SQLException exSql) {
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
