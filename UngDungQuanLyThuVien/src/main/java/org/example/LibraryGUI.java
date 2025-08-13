package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.table.DefaultTableModel;
import java.awt.image.BufferedImage;

import com.google.zxing.WriterException;


public class LibraryGUI {
    private final Library library = Library.getInstance();

    public LibraryGUI() {

        library.loadBooksFromDatabase();
        library.loadUsersFromDatabase();

        JFrame frame = new JFrame("📚 Library Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);

        // Font và màu mặc định
        Font font = new Font("Segoe UI Emoji", Font.PLAIN, 16);

        UIManager.put("Button.font", font);
        UIManager.put("Label.font", font);
        UIManager.put("OptionPane.messageFont", font);
        UIManager.put("OptionPane.buttonFont", font);
        UIManager.put("TextArea.font", font);
        UIManager.put("TextField.font", font);
        UIManager.put("Table.font", font);
        UIManager.put("TableHeader.font", font);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(245, 245, 245));  // Màu nền dịu
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Padding

        JLabel titleLabel = new JLabel(" Welcome to the Library System", JLabel.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(33, 66, 99));
        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));


        String[] buttonLabels = {
                "🔐 Login",
                "🔓 Logout",
                "➕📄 Add Document",
                "❌📄 Remove Document",
                "✏️📄 Update Document",
                "📚 Display Documents",
                "➕👤 Add User",
                "🔙📖 Return Document",
                "👤ℹ️ Display User Info",
                "🔍📘 Search Google Books",
                "📝 Display Borrow Records",
                "🚪 Exit"
        };

        for (int i = 0; i < buttonLabels.length; i++) {
            JButton button = new JButton("[" + i + "] " + buttonLabels[i]);
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            button.setFocusPainted(false);
            button.setBackground(new Color(200, 220, 240));
            button.setForeground(Color.BLACK);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(160, 180, 200)),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));

            int index = i;
            button.addActionListener(e -> handleMenuOption(index));
            panel.add(button);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        frame.add(scrollPane);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void handleMenuOption(int option) {
        switch (option) {
            case 0 -> loginUser();
            case 1 -> logoutUser();
            case 2 -> addDocument();
            case 3 -> removeDocument();
            case 4 -> updateDocument();
            case 5 -> displayDocuments();
            case 6 -> addUser();
            case 7 -> returnDocument();
            case 8 -> displayUserInfo();
            case 9 -> searchGoogleBooks();
            case 10 -> displayBorrowRecords();
            case 11 -> System.exit(0);
            default -> JOptionPane.showMessageDialog(null, "Invalid option");
        }
    }

    // ===== Thêm chức năng Login/Logout (dùng Session) =====
    private void loginUser() {
        List<LibraryUser> users = library.getUsers();
        if (users.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No users found. Please add a user first.");
            return;
        }

        String[] userOptions = users.stream()
                .map(u -> u.getUserId() + " - " + u.getName())
                .toArray(String[]::new);

        String selected = (String) JOptionPane.showInputDialog(
                null,
                "Select a user to login:",
                "Login",
                JOptionPane.PLAIN_MESSAGE,
                null,
                userOptions,
                userOptions[0]
        );

        if (selected != null) {
            String selectedId = selected.split(" - ")[0];
            LibraryUser user = library.findUserById(selectedId);
            if (user != null) {
                Session.setCurrentUser(user); // LƯU VÀO SESSION
                JOptionPane.showMessageDialog(null, "Logged in as " + user.getName());
            } else {
                JOptionPane.showMessageDialog(null, "Selected user not found.");
            }
        }
    }

    private void logoutUser() {
        LibraryUser user = Session.getCurrentUser();
        if (user != null) {
            Session.logout();
            JOptionPane.showMessageDialog(null, "User " + user.getName() + " logged out.");
        } else {
            JOptionPane.showMessageDialog(null, "No user is currently logged in.");
        }
    }

    private void addDocument() {
        String title = JOptionPane.showInputDialog("Enter title:");
        if (title == null) return;

        String authors = JOptionPane.showInputDialog("Enter authors:");
        if (authors == null) return;

        String category = JOptionPane.showInputDialog("Enter category:");
        if (category == null) return;

        String isbn = JOptionPane.showInputDialog("Enter ISBN:");
        if (isbn == null) return;

        String quantityStr = JOptionPane.showInputDialog("Enter quantity:");
        if (quantityStr == null) return;

        String thumbnailLink = JOptionPane.showInputDialog("Enter thumbnail link:");
        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid quantity.");
            return;
        }

        Book book = new Book(title, authors, category, isbn, quantity, thumbnailLink);

        try {
            new BookDAO().insertBook(book); // ghi DB
            library.addItem(book);          // cập nhật bộ nhớ
            JOptionPane.showMessageDialog(null, "Document added.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "DB error: " + e.getMessage());
        }
    }

        private void removeDocument() {
            String isbn = JOptionPane.showInputDialog("Enter ISBN to remove:");
            if (isbn == null) return;

            var item = library.findItemByIsbn(isbn);
            if (item == null) {
                JOptionPane.showMessageDialog(null, "Document not found.");
                return;
            }

            try {
                new BookDAO().removeBook(isbn);
                library.removeItem(item);
                JOptionPane.showMessageDialog(null, "Document removed.");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "This book is currently borrowed by another user.");
            }
        }

    private void updateDocument() {
        String isbn = JOptionPane.showInputDialog("Enter ISBN to update:");
        if (isbn == null) return;

        var item = library.findItemByIsbn(isbn);
        if (item == null) {
            JOptionPane.showMessageDialog(null, "Document not found.");
            return;
        }

        String newTitle = JOptionPane.showInputDialog("New title:", item.getTitle());
        if (newTitle == null) return;
        String newAuthors = JOptionPane.showInputDialog("New authors:", item.getDescription());
        if (newAuthors == null) return;
        String newCategory = JOptionPane.showInputDialog("New category:",
                (item instanceof Book b) ? b.getCategory() : "");
        if (newCategory == null) return;

        int quantity;
        try {
            quantity = Integer.parseInt(
                    JOptionPane.showInputDialog("New quantity:", item.getQuantity()).trim()
            );
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Invalid quantity.");
            return;
        }

        try {
            new BookDAO().updateBook(isbn, newTitle, newAuthors, newCategory, quantity);

            item.setTitle(newTitle);
            item.setDescription(newAuthors);
            item.setQuantity(quantity);
            if (item instanceof Book b) b.setCategory(newCategory);

            JOptionPane.showMessageDialog(null, "Document updated.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "DB error: " + e.getMessage());
        }
    }

    private void displayDocuments() {
        List<LibraryItem> items = library.getItems();
        if (items == null || items.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No documents.");
            return;
        }

        // Snapshot các Book để dùng khi query rỗng
        final List<Book> allBooks = new ArrayList<>();
        for (LibraryItem li : items) {
            if (li instanceof Book b) allBooks.add(b);
        }

        // Panel danh sách
        final JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        final JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setPreferredSize(new Dimension(900, 650));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Top bar: label + field + (Search | Clear)
        final JPanel topBar = new JPanel(new BorderLayout(8, 8));
        topBar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        topBar.setBackground(Color.WHITE);

        final JTextField searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Search by title, author, ISBN…");
        searchField.setToolTipText("Type your query, then press Search or Enter.");

        final JButton searchBtn = new JButton("Search");
        searchBtn.setFocusable(false);

        final JButton clearBtn = new JButton("Clear");
        clearBtn.setFocusable(false);

        final JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTop.setOpaque(false);
        rightTop.add(searchBtn);
        rightTop.add(clearBtn);

        topBar.add(new JLabel("Search:"), BorderLayout.WEST);
        topBar.add(searchField, BorderLayout.CENTER);
        topBar.add(rightTop, BorderLayout.EAST);

        // Dialog
        final JDialog dialog = new JDialog((Frame) null, "Library Books", true);
        final JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(topBar, BorderLayout.NORTH);
        root.add(scrollPane, BorderLayout.CENTER);
        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(null);

        // Render lần đầu: hiển thị tất cả
        renderBooks(listPanel, allBooks);
        listPanel.revalidate();
        listPanel.repaint();
        SwingUtilities.invokeLater(() -> {
            scrollPane.getViewport().setViewPosition(new Point(0, 0));
            scrollPane.getVerticalScrollBar().setValue(0);
        });

        // ---- Chỉ tìm khi bấm Search / Enter ----
        final Runnable doSearch = () -> {
            String q = searchField.getText().trim();
            List<Book> toShow = q.isEmpty()
                    ? allBooks
                    : Optional.ofNullable(library.search(q)).orElse(Collections.emptyList());
            renderBooks(listPanel, toShow);
            SwingUtilities.invokeLater(() -> {
                scrollPane.getViewport().setViewPosition(new Point(0, 0));
                scrollPane.getVerticalScrollBar().setValue(0);
            });
        };

        // Bấm nút Search
        searchBtn.addActionListener(ev -> doSearch.run());

        // Nhấn Enter trong ô search
        searchField.addActionListener(ev -> doSearch.run());

        // Esc: chỉ clear text (không tự tìm)
        KeyStroke esc = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0);
        searchField.getInputMap(JComponent.WHEN_FOCUSED).put(esc, "clearSearch");
        searchField.getActionMap().put("clearSearch", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!searchField.getText().isEmpty()) {
                    searchField.setText("");
                }
            }
        });

        // Nút Clear: xoá và hiển thị lại tất cả
        clearBtn.addActionListener(ev -> {
            if (!searchField.getText().isEmpty()) searchField.setText("");
            renderBooks(listPanel, allBooks);
            SwingUtilities.invokeLater(() -> {
                scrollPane.getViewport().setViewPosition(new Point(0, 0));
                scrollPane.getVerticalScrollBar().setValue(0);
            });
        });

        // Focus vào ô search khi mở
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowOpened(java.awt.event.WindowEvent e) {
                SwingUtilities.invokeLater(searchField::requestFocusInWindow);
            }
        });

        dialog.setVisible(true);
    }


    // === CHỈ GIỮ MỘT BẢN renderBooks NÀY TRONG CLASS ===
    private void renderBooks(JPanel listPanel, List<Book> books) {
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
            // BookPanel có nút Borrow
            BookPanel bookPanel = new BookPanel(b, true);

            JPanel card = new JPanel(new BorderLayout(12, 12));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 220, 220)),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            card.add(bookPanel, BorderLayout.CENTER);

            // QR code bên phải
            try {
                String qrContent = (b.getId() != null && !b.getId().isBlank()) ? ("BOOK_ID:" + b.getId())
                        : (b.getIsbn() != null && !b.getIsbn().isBlank() && !"N/A".equalsIgnoreCase(b.getIsbn())) ? ("ISBN:" + b.getIsbn())
                        : b.toString();

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

    private void addUser() {
        String userId = JOptionPane.showInputDialog("Enter user ID:");
        String name = JOptionPane.showInputDialog("Enter name:");

        if (userId == null || userId.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "User ID and name cannot be empty.");
            return;
        }

        User user = new User(userId, name);
        try {
            new UserDAO().insertUser(user); // ghi DB
            library.addUser(user);          // cập nhật bộ nhớ
            JOptionPane.showMessageDialog(null, "User added.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "DB error: " + e.getMessage());
        }
    }


    private void returnDocument() {
        LibraryUser lu = Session.getCurrentUser();
        if (lu == null) {
            JOptionPane.showMessageDialog(null, "No user logged in. Please login first.");
            return;
        }

        // Model & Table
        String[] columns = {"ID", "Title", "Type", "Author(s)"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);

        // Nguồn dữ liệu hiển thị
        final List<LibraryItem> borrowedItems = new ArrayList<>();

        // Helper: nạp lại danh sách ĐANG MƯỢN từ DB và render vào bảng
        Runnable reloadBorrowedTable = () -> {
            borrowedItems.clear();
            model.setRowCount(0);
            try {
                UserDAO dao = new UserDAO();
                // lấy tất cả record của user rồi lọc active (hoặc thay bằng dao.getActiveBorrowRecords(...))
                for (BorrowRecord br : dao.getBorrowRecords(lu.getUserId())) {
                    if (br.getReturnDate() == null && br.getBook() != null) {
                        LibraryItem it = br.getBook();
                        borrowedItems.add(it);
                        model.addRow(new Object[]{ it.getId(), it.getTitle(),
                                it.getClass().getSimpleName(), it.getAuthors() });
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Error loading borrowed documents: " + ex.getMessage());
            }
        };

        // Lần đầu mở
        reloadBorrowedTable.run();
        if (borrowedItems.isEmpty()) {
            JOptionPane.showMessageDialog(null, "You have no active borrowed documents.");
            return;
        }

        // Nút trả 1
        JButton btnReturnOne = new JButton("Return Selected Document");
        btnReturnOne.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(null, "Please select a document to return.");
                return;
            }
            LibraryItem item = borrowedItems.get(row);
            try {
                if (!(item instanceof Book b)) {
                    throw new IllegalStateException("Unsupported item type: " + item.getClass().getSimpleName());
                }
                String idStr = b.getId();
                if (idStr == null || idStr.isBlank()) {
                    JOptionPane.showMessageDialog(null, "Book has no DB id. Please reload.");
                    return;
                }

                new UserDAO().returnBookByBookId(lu.getUserId(), Integer.parseInt(idStr)); // DB
                lu.returnItem(item); // RAM (xóa theo id trong implement của bạn)

                reloadBorrowedTable.run(); // UI cập nhật ngay
                JOptionPane.showMessageDialog(null, "Document returned successfully.");

                if (borrowedItems.isEmpty()) {
                    // không còn gì để trả, có thể tắt dialog bằng cách đóng JOptionPane cha nếu bạn muốn
                }
            } catch (Exception ex) {
                String msg = String.valueOf(ex.getMessage());
                if (msg.contains("Already returned") || msg.contains("No active borrow")) {
                    // Dữ liệu đã được trả ở nơi khác → dọn UI
                    reloadBorrowedTable.run();
                    JOptionPane.showMessageDialog(null, "This item was already returned. List refreshed.");
                } else {
                    JOptionPane.showMessageDialog(null, "DB error: " + msg);
                }
            }
        });

        // Nút trả tất cả
        JButton btnReturnAll = new JButton("Return All Documents");
        btnReturnAll.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    null, "Return ALL borrowed documents?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            int success = 0, failed = 0;

            // snapshot để tránh ConcurrentModification
            List<LibraryItem> snapshot = new ArrayList<>(borrowedItems);
            for (LibraryItem item : snapshot) {
                try {
                    if (!(item instanceof Book b)) throw new IllegalStateException("Unsupported item type.");
                    String idStr = b.getId();
                    if (idStr == null || idStr.isBlank()) throw new IllegalStateException("Book has no DB id.");

                    new UserDAO().returnBookByBookId(lu.getUserId(), Integer.parseInt(idStr)); // DB
                    lu.returnItem(item); // RAM
                    success++;
                } catch (Exception ex2) {
                    failed++;
                }
            }

            reloadBorrowedTable.run(); // làm sạch bảng ngay
            String msg = (failed == 0) ? "All documents returned." : "Returned: " + success + ", Failed: " + failed;
            JOptionPane.showMessageDialog(null, msg);
        });

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(btnReturnOne);
        buttonPanel.add(btnReturnAll);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(null, panel, "Borrowed Documents", JOptionPane.PLAIN_MESSAGE);
    }

    private void displayUserInfo() {
        LibraryUser user = Session.getCurrentUser();
        if (user == null) {
            JOptionPane.showMessageDialog(null, "No user logged in. Please login first.");
            return;
        }
        JOptionPane.showMessageDialog(null, user.toString());
    }

    private void searchGoogleBooks() {
        String keyword = JOptionPane.showInputDialog("Enter keyword to search Google Books:");
        if (keyword == null || keyword.trim().isEmpty()) return;

        try {
            List<Book> results = GoogleBooksAPI.searchBooks(keyword.trim());

            if (results.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No results found.");
                return;
            }

            // Panel chứa danh sách BookPanel
            JPanel resultPanel = new JPanel();
            resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
            resultPanel.setAutoscrolls(false);

            Library library = Library.getInstance();
            BookDAO dao = new BookDAO();

            for (Book book : results) {
                BookPanel panel = new BookPanel(book, false); // ẩn Borrow

                // Nút Add
                JButton addButton = new JButton("Add");
                addButton.setFocusable(false); // tránh auto-scroll do focus
                addButton.addActionListener(e -> {
                    String quantityStr = JOptionPane.showInputDialog("Enter quantity to add:", "1");
                    if (quantityStr == null || quantityStr.trim().isEmpty()) return;

                    int quantity;
                    try {
                        quantity = Integer.parseInt(quantityStr.trim());
                        if (quantity <= 0) throw new NumberFormatException();
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null, "Invalid quantity.");
                        return;
                    }

                    // set số lượng muốn cộng/thêm
                    book.setQuantity(quantity);

                    try {
                        // UPSERT trong DB: trùng -> DB tự cộng; không trùng -> insert
                        dao.insertBook(book); // sẽ set id + quantity hiện tại trong DB cho book

                        // tìm theo ISBN (nếu có), không có thì theo id
                        Book matched = null;
                        if (book.getIsbn() != null && !"N/A".equalsIgnoreCase(book.getIsbn()) && !book.getIsbn().isBlank()) {
                            LibraryItem li = library.findItemByIsbn(book.getIsbn());
                            if (li instanceof Book) matched = (Book) li;
                        }
                        if (matched == null && book.getId() != null) {
                            for (LibraryItem li : library.getItems()) {
                                if (li instanceof Book b && book.getId().equals(b.getId())) {
                                    matched = b; break;
                                }
                            }
                        }

                        if (matched != null) {
                            // đã có trong RAM -> đặt quantity theo DB (đã +)
                            matched.setQuantity(book.getQuantity());
                            JOptionPane.showMessageDialog(null,
                                    "Quantity increased in database. New quantity: " + matched.getQuantity());
                        } else {
                            // chưa có trong RAM -> thêm vào RAM (book đang mang id + quantity từ DB)
                            library.addItem(book);
                            JOptionPane.showMessageDialog(null,
                                    "Book added to library. (ID: " + book.getId() + ", Qty: " + book.getQuantity() + ")");
                        }
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(null, "DB error: " + ex.getMessage());
                    }
                });

                panel.add(addButton, BorderLayout.SOUTH);
                resultPanel.add(panel);
                resultPanel.add(Box.createVerticalStrut(10)); // khoảng cách
            }

            // ScrollPane + Dialog (để kiểm soát scrollbar ở đầu)
            JScrollPane scrollPane = new JScrollPane(resultPanel);
            scrollPane.setPreferredSize(new Dimension(600, 600));

            JDialog dialog = new JDialog((Frame) null, "Search Results", true);
            dialog.getContentPane().add(scrollPane);
            dialog.pack();
            dialog.setLocationRelativeTo(null);

            // Kéo scrollbar về đầu ngay khi dialog mở
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowOpened(java.awt.event.WindowEvent e) {
                    SwingUtilities.invokeLater(() -> {
                        scrollPane.getViewport().setViewPosition(new Point(0, 0));
                        scrollPane.getVerticalScrollBar().setValue(0);
                    });
                }
            });

            dialog.setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void displayBorrowRecords() {
        List<LibraryUser> users = library.getUsers();
        StringBuilder builder = new StringBuilder();

        System.out.println("DEBUG: Number of users = " + users.size());

        StringBuilder overdueBuilder = new StringBuilder();
        StringBuilder normalBuilder  = new StringBuilder();
        Date now = new Date();

        for (LibraryUser user : users) {
            List<BorrowRecord> brs = user.getBorrowRecord();

            for (BorrowRecord br : brs) {
                LibraryItem item = br.getBook();
                if (item instanceof Book) {
                    Book book = (Book) item;
                    Date borrowDate = br.getBorrowDate();

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(borrowDate);
                    calendar.add(Calendar.DAY_OF_MONTH, 14);
                    Date returnDate = calendar.getTime();

                    boolean overdue = returnDate.before(now);

                    StringBuilder target = overdue ? overdueBuilder : normalBuilder;
                    target.append("User: ").append(user.getName()).append("\n");
                    target.append("Book: ").append(book.toString()).append("\n");
                    target.append("Borrowed on: ").append(borrowDate.toString()).append("\n");
                    target.append("Return by: ").append(returnDate.toString()).append("\n");
                    if (overdue) target.append("⚠ Overdue\n");
                    target.append("-----------------------------\n");
                }
            }
        }


        String message = (overdueBuilder.length() > 0 || normalBuilder.length() > 0)
                ? overdueBuilder.append(normalBuilder).toString()
                : "No documents.";

        JTextArea textArea = new JTextArea(message);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Font hỗ trợ Unicode
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400)); // Kích thước tùy chỉnh
        scrollPane.getVerticalScrollBar().setValue(0);
        JOptionPane.showMessageDialog(null, scrollPane, "Borrow Records", JOptionPane.INFORMATION_MESSAGE);
    }

}
