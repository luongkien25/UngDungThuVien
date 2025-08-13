package org.example;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.image.BufferedImage;
import javax.swing.table.DefaultTableModel;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;

public class AdminGUI extends JFrame {
    private final Library library = Library.getInstance();
    private final JPanel centerRoot = new JPanel(new BorderLayout()); // nơi thay view

    public AdminGUI() {
        super("AdminGUI");
        library.loadBooksFromDatabase();
        library.loadUsersFromDatabase();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 780);
        setLayout(new BorderLayout());
        add(centerRoot, BorderLayout.CENTER);

        // ===== Bottom bar =====
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnDisplay = new JButton("📚 DisplayDocument");
        JButton btnAddDoc  = new JButton("➕📄 Add Document");
        JButton btnAddUser = new JButton("➕👤 Add User");
        JButton btnBR      = new JButton("📝 DisplayBorrowRecords");
        JButton btnSearchG = new JButton("🔍📘 Search Google Books");
        JButton btnUInfo   = new JButton("👤ℹ️ Display User Info");
        JButton btnLogout  = new JButton("🚪 Logout");
        bottom.add(btnDisplay);
        bottom.add(btnAddDoc);
        bottom.add(btnAddUser);
        bottom.add(btnBR);
        bottom.add(btnSearchG);
        bottom.add(btnUInfo);
        bottom.add(btnLogout);
        add(bottom, BorderLayout.SOUTH);

        // Hành vi nút
        btnDisplay.addActionListener(e -> showDisplayView());
        btnAddDoc.addActionListener(e -> addDocument());
        btnAddUser.addActionListener(e -> addUser());
        btnBR.addActionListener(e -> displayBorrowRecords());
        btnSearchG.addActionListener(e -> searchGoogleBooks());
        btnUInfo.addActionListener(e -> displayUserInfo());
        btnLogout.addActionListener(e -> doLogout());

        // Vào thẳng Display
        showDisplayView();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    /* ==================== DISPLAY VIEW ==================== */

    private void showDisplayView() {
        centerRoot.removeAll();

        // UI: top search + scrollable list
        final JPanel listPanel  = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        final JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setPreferredSize(new Dimension(1000, 640));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        final JPanel topBar = new JPanel(new BorderLayout(8, 8));
        topBar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        topBar.setBackground(Color.WHITE);

        final JTextField searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Search by title, author, ISBN…");
        searchField.setToolTipText("Type your query, then press Search or Enter.");

        final JButton searchBtn = new JButton("Search");
        final JButton clearBtn  = new JButton("Clear");
        final JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTop.setOpaque(false);
        rightTop.add(searchBtn);
        rightTop.add(clearBtn);

        topBar.add(new JLabel("Search:"), BorderLayout.WEST);
        topBar.add(searchField, BorderLayout.CENTER);
        topBar.add(rightTop, BorderLayout.EAST);

        // callback refresh cho BookPanel
        final Runnable[] doSearchRef = new Runnable[1];

        // hàm render luôn dùng dữ liệu MỚI từ RAM + count MỚI từ DB
        final Runnable doSearch = () -> {
            String q = searchField.getText().trim();
            java.util.List<Book> toShow = q.isEmpty()
                    ? getAllBooksFromRAM()
                    : java.util.Optional.ofNullable(library.search(q)).orElse(java.util.Collections.emptyList());
            java.util.Map<Integer, Long> countMap = freshCountMap();
            renderBooksAdmin(listPanel, toShow, countMap,
                    () -> { if (doSearchRef[0] != null) doSearchRef[0].run(); },
                    false,  // Borrow off (Admin)
                    true,   // Update on
                    true    // Remove on
            );
            scrollToTop(scrollPane);
        };
        doSearchRef[0] = doSearch;

        // render lần đầu
        doSearch.run();

        // bind nút/enter/clear
        searchBtn.addActionListener(ev -> doSearch.run());
        searchField.addActionListener(ev -> doSearch.run());
        javax.swing.KeyStroke esc = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0);
        searchField.getInputMap(JComponent.WHEN_FOCUSED).put(esc, "clearSearch");
        searchField.getActionMap().put("clearSearch", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!searchField.getText().isEmpty()) searchField.setText("");
            }
        });
        clearBtn.addActionListener(ev -> { if (!searchField.getText().isEmpty()) searchField.setText(""); doSearch.run(); });

        centerRoot.add(topBar, BorderLayout.NORTH);
        centerRoot.add(scrollPane, BorderLayout.CENTER);
        centerRoot.revalidate();
        centerRoot.repaint();
    }

    /** render card giống LibraryGUI + badge Borrowed:N; truyền chính instance Book trong RAM */
    private void renderBooksAdmin(JPanel listPanel, List<Book> books,
                                  Map<Integer, Long> countMap,
                                  Runnable onRefresh,
                                  boolean showBorrow, boolean showUpdate, boolean showRemove) {
        listPanel.removeAll();

        if (books == null || books.isEmpty()) {
            JLabel empty = new JLabel("No results.");
            empty.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            listPanel.add(empty);
            listPanel.revalidate();
            listPanel.repaint();
            return;
        }

        for (Book b : books) {
            BookPanel bookPanel = new BookPanel(b, showBorrow, showUpdate, showRemove, onRefresh);

            JPanel card = new JPanel(new BorderLayout(12, 12));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 220, 220)),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));

            // Badge "Borrowed: N"
            long cnt = 0;
            try {
                if (b.getId() != null && !b.getId().isBlank()) {
                    cnt = countMap.getOrDefault(Integer.parseInt(b.getId()), 0L);
                }
            } catch (Exception ignore) {}
            JLabel badge = new JLabel("Borrowed: " + cnt);
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.add(badge, BorderLayout.EAST);
            card.add(header, BorderLayout.NORTH);

            card.add(bookPanel, BorderLayout.CENTER);

            // QR giống các view khác (KHÔNG dùng b.toString() để tránh NPE)
            try {
                String qrContent = (b.getId() != null && !b.getId().isBlank()) ? ("BOOK_ID:" + b.getId())
                        : (b.getIsbn() != null && !b.getIsbn().isBlank() && !"N/A".equalsIgnoreCase(b.getIsbn())) ? ("ISBN:" + b.getIsbn())
                        : (b.getTitle() != null ? b.getTitle() : "BOOK");

                BufferedImage qr = QRCodeGenerator.generateQRCodeImage(qrContent, 120, 120);
                JLabel qrLabel = new JLabel(new ImageIcon(qr));
                qrLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
                card.add(qrLabel, BorderLayout.EAST);
            } catch (Exception ex) {
                System.err.println("Failed to generate QR code: " + ex.getMessage());
            }

            listPanel.add(card);
            listPanel.add(Box.createVerticalStrut(10));
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private void scrollToTop(JScrollPane sp) {
        SwingUtilities.invokeLater(() -> {
            sp.getViewport().setViewPosition(new Point(0, 0));
            sp.getVerticalScrollBar().setValue(0);
        });
    }

    /* ================== HÀNH VI NÚT KHÁC (giữ y như bản bạn gửi) ================== */

    private void addDocument() {
        String title = JOptionPane.showInputDialog(this, "Enter title:");
        if (title == null) return;

        String authors = JOptionPane.showInputDialog(this, "Enter authors:");
        if (authors == null) return;

        String category = JOptionPane.showInputDialog(this, "Enter category:");
        if (category == null) return;

        String isbn = JOptionPane.showInputDialog(this, "Enter ISBN:");
        if (isbn == null) return;

        String quantityStr = JOptionPane.showInputDialog(this, "Enter quantity:");
        if (quantityStr == null) return;

        String thumbnailLink = JOptionPane.showInputDialog(this, "Enter thumbnail link:");
        int quantity;
        try { quantity = Integer.parseInt(quantityStr); }
        catch (NumberFormatException e) { JOptionPane.showMessageDialog(this, "Invalid quantity."); return; }

        Book book = new Book(title, authors, category, isbn, quantity, thumbnailLink);

        try {
            new BookDAO().insertBook(book); // DB
            library.addItem(book);          // RAM (đúng instance)
            JOptionPane.showMessageDialog(this, "Document added.");
            showDisplayView();              // refresh
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "DB error: " + e.getMessage());
        }
    }

    private void addUser() {
        String userId = JOptionPane.showInputDialog(this, "Enter user ID:");
        String name = JOptionPane.showInputDialog(this, "Enter name:");
        if (userId == null || userId.isBlank() || name == null || name.isBlank()) {
            JOptionPane.showMessageDialog(this, "User ID and name cannot be empty."); return;
        }
        User user = new User(userId, name);
        try {
            new UserDAO().insertUser(user);
            library.addUser(user);
            JOptionPane.showMessageDialog(this, "User added.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "DB error: " + e.getMessage());
        }
    }

    private void searchGoogleBooks() {
        String keyword = JOptionPane.showInputDialog(this, "Enter keyword to search Google Books:");
        if (keyword == null || keyword.trim().isEmpty()) return;

        try {
            List<Book> results = GoogleBooksAPI.searchBooks(keyword.trim());
            if (results.isEmpty()) { JOptionPane.showMessageDialog(this, "No results found."); return; }

            JPanel resultPanel = new JPanel();
            resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
            resultPanel.setAutoscrolls(false);

            Library library = Library.getInstance();
            BookDAO dao = new BookDAO();

            for (Book book : results) {
                BookPanel panel = new BookPanel(book, false); // ẩn Borrow
                JButton addButton = new JButton("Add");
                addButton.setFocusable(false);
                addButton.addActionListener(e -> {
                    String quantityStr = JOptionPane.showInputDialog(this, "Enter quantity to add:", "1");
                    if (quantityStr == null || quantityStr.trim().isEmpty()) return;

                    int quantity;
                    try { quantity = Integer.parseInt(quantityStr.trim()); if (quantity <= 0) throw new NumberFormatException(); }
                    catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Invalid quantity."); return; }

                    book.setQuantity(quantity);

                    try {
                        dao.insertBook(book);
                        Book matched = null;
                        if (book.getIsbn() != null && !"N/A".equalsIgnoreCase(book.getIsbn()) && !book.getIsbn().isBlank()) {
                            LibraryItem li = library.findItemByIsbn(book.getIsbn());
                            if (li instanceof Book) matched = (Book) li;
                        }
                        if (matched == null && book.getId() != null) {
                            for (LibraryItem li : library.getItems()) {
                                if (li instanceof Book b && book.getId().equals(b.getId())) { matched = b; break; }
                            }
                        }
                        if (matched != null) {
                            matched.setQuantity(book.getQuantity());
                            JOptionPane.showMessageDialog(this, "Quantity increased in database. New quantity: " + matched.getQuantity());
                        } else {
                            library.addItem(book);
                            JOptionPane.showMessageDialog(this, "Book added to library. (ID: " + book.getId() + ", Qty: " + book.getQuantity() + ")");
                        }
                        showDisplayView(); // refresh
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
                    }
                });

                panel.add(addButton, BorderLayout.SOUTH);
                resultPanel.add(panel);
                resultPanel.add(Box.createVerticalStrut(10));
            }

            JScrollPane scrollPane = new JScrollPane(resultPanel);
            scrollPane.setPreferredSize(new Dimension(700, 640));

            JDialog dialog = new JDialog(this, "Search Results", true);
            dialog.getContentPane().add(scrollPane);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayBorrowRecords() {
        List<LibraryUser> users = library.getUsers();
        StringBuilder overdueBuilder = new StringBuilder();
        StringBuilder normalBuilder  = new StringBuilder();
        java.util.Date now = new java.util.Date();

        for (LibraryUser user : users) {
            List<BorrowRecord> brs = user.getBorrowRecord();
            for (BorrowRecord br : brs) {
                LibraryItem item = br.getBook();
                if (item instanceof Book) {
                    Book book = (Book) item;
                    java.util.Date borrowDate = br.getBorrowDate();

                    java.util.Calendar calendar = java.util.Calendar.getInstance();
                    calendar.setTime(borrowDate);
                    calendar.add(java.util.Calendar.DAY_OF_MONTH, 14);
                    java.util.Date returnDate = calendar.getTime();

                    boolean overdue = returnDate.before(now);
                    StringBuilder target = overdue ? overdueBuilder : normalBuilder;
                    target.append("User: ").append(user.getName()).append("\n");
                    target.append("Book: ").append(book.getTitle()).append("\n");
                    target.append("Borrowed on: ").append(borrowDate).append("\n");
                    target.append("Return by: ").append(returnDate).append("\n");
                    if (overdue) target.append("⚠ Overdue\n");
                    target.append("-----------------------------\n");
                }
            }
        }

        String message = (overdueBuilder.length() > 0 || normalBuilder.length() > 0)
                ? overdueBuilder.append(normalBuilder).toString()
                : "No documents.";

        JTextArea textArea = new JTextArea(message);
        textArea.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(700, 500));
        JOptionPane.showMessageDialog(this, scrollPane, "Borrow Records", JOptionPane.INFORMATION_MESSAGE);
    }

    private void displayUserInfo() {
        LibraryUser user = Session.getCurrentUser();
        if (user == null) {
            JOptionPane.showMessageDialog(this, "No user logged in. (Main sẽ login sẵn để test)");
            return;
        }
        JOptionPane.showMessageDialog(this, user.toString());
    }

    private void doLogout() {
        if (Session.getCurrentUser() == null) {
            JOptionPane.showMessageDialog(this, "No user logged in.");
            return;
        }
        String name = Session.getCurrentUser().getName();
        Session.logout();
        JOptionPane.showMessageDialog(this, "Logged out: " + name);
        showDisplayView();
    }
    private java.util.List<Book> getAllBooksFromRAM() {
        java.util.List<Book> list = new java.util.ArrayList<>();
        for (LibraryItem li : library.getItems()) if (li instanceof Book b) list.add(b);
        return list;
    }

    private java.util.Map<Integer, Long> freshCountMap() {
        try { return new BookDAO().getBorrowCountsMap(); }
        catch (java.sql.SQLException e) { return java.util.Collections.emptyMap(); }
    }
}
