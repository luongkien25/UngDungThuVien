package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Calendar;
import java.util.List;
import java.util.Date;
import java.util.List;

import javax.swing.table.DefaultTableModel;
import java.awt.image.BufferedImage;

import com.google.zxing.WriterException;


public class LibraryGUI {
    private final Library library = Library.getInstance();

    public LibraryGUI() {
        // Nạp dữ liệu từ DB vào bộ nhớ (DAO tự mở/đóng connection)
        library.loadBooksFromDatabase();
        library.loadUsersFromDatabase();

        JFrame frame = new JFrame("📚 Library Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 700);

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
                "🔍📄 Find Document",
                "📚 Display Documents",
                "➕👤 Add User",
                "📖➡️👤 Borrow Document",
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
            case 5 -> findDocument();
            case 6 -> displayDocuments();
            case 7 -> addUser();
            case 8 -> borrowDocument();
            case 9 -> returnDocument();
            case 10 -> displayUserInfo();
            case 11 -> searchGoogleBooks();
            case 12 -> displayBorrowRecords();
            case 13 -> System.exit(0);
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

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid quantity.");
            return;
        }

        Book book = new Book(title, authors, category, isbn, quantity, ""); // imageUrl không cần thiết ở đây

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
                new BookDAO().removeBook(isbn); // xóa DB
                library.removeItem(item);       // xóa bộ nhớ
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

    private void findDocument() {
        String title = JOptionPane.showInputDialog("Enter title to find:");
        var item = library.findItemByTitle(title);
        JOptionPane.showMessageDialog(null, item != null ? item.toString() : "Not found.");
    }

    private void displayDocuments() {
        List<LibraryItem> items = library.getItems();
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No documents.");
            return;
        }

        for (LibraryItem item : items) {
            JPanel bookPanel = new JPanel(new BorderLayout(10, 10));
            bookPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Thông tin sách bên trái
            JTextArea infoArea = new JTextArea(item.toString());
            infoArea.setEditable(false);
            infoArea.setLineWrap(true);
            infoArea.setWrapStyleWord(true);
            bookPanel.add(infoArea, BorderLayout.CENTER);

            // QR code bên phải
            try {
                BufferedImage qrImage = QRCodeGenerator.generateQRCodeImage(item.toString(), 120, 120);
                JLabel qrLabel = new JLabel(new ImageIcon(qrImage));
                bookPanel.add(qrLabel, BorderLayout.EAST);
            } catch (Exception e) {
                System.err.println("Failed to generate QR code: " + e.getMessage());
            }

            JOptionPane.showMessageDialog(null, bookPanel, "Document Info", JOptionPane.PLAIN_MESSAGE);
        }
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
        // 1) Kiểm tra đăng nhập
        LibraryUser lu = Session.getCurrentUser();
        if (lu == null) {
            JOptionPane.showMessageDialog(null, "No user logged in. Please login first.");
            return;
        }
        // Ép kiểu nếu bạn dùng class User riêng (để lấy name, record,...)
        User user = (lu instanceof User u) ? u : null;

        // 2) Lấy danh sách đang mượn
        List<LibraryItem> borrowedItems = lu.getBorrowedBooks();
        if (borrowedItems == null || borrowedItems.isEmpty()) {
            JOptionPane.showMessageDialog(null, "You have not borrowed any documents.");
            return;
        }

        // 3) Chuẩn bị bảng
        String[] columns = {"ID/ISBN", "Title", "Type", "Author(s)"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (LibraryItem it : borrowedItems) {
            model.addRow(new Object[]{
                    it.getId() != null ? it.getId() : it.getIsbn(), // ưu tiên id, fallback isbn
                    it.getTitle(),
                    it.getClass().getSimpleName(),
                    it.getAuthors()
            });
        }

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);

        JButton btnReturnOne = new JButton("Return Selected Document");
        JButton btnReturnAll = new JButton("Return All Documents");

        // 4) Hành vi nút "Return Selected"
        btnReturnOne.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(null, "Please select a document to return.");
                return;
            }
            LibraryItem item = borrowedItems.get(row);

            try {
                // Cập nhật DB
                if (item instanceof Book b) {
                    new UserDAO().returnBook(lu.getUserId(), b.getIsbn());
                } else {
                    // nếu có loại khác, xử lý ở đây (ví dụ theo item.getId())
                    throw new IllegalStateException("Unsupported item type: " + item.getClass().getSimpleName());
                }

                // Cập nhật bộ nhớ
                lu.returnItem(item);                  // remove khỏi danh sách của user, +1 qty nếu bạn làm ở đây
                LibraryItem libItem = library.findItemByIsbn(item.getIsbn());
                if (libItem != null) libItem.setQuantity(libItem.getQuantity() + 1); // phòng khi returnItem không +1

                // Cập nhật UI
                model.removeRow(row);
                borrowedItems.remove(row);

                JOptionPane.showMessageDialog(null, "Document returned successfully.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "DB error: " + ex.getMessage());
            }
        });

        // 5) Hành vi nút "Return All"
        btnReturnAll.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    null,
                    "Are you sure you want to return all borrowed documents?",
                    "Confirm Return All",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) return;

            int success = 0, failed = 0;

            // duyệt từ cuối về đầu để remove row an toàn
            for (int i = borrowedItems.size() - 1; i >= 0; i--) {
                LibraryItem item = borrowedItems.get(i);
                try {
                    if (item instanceof Book b) {
                        new UserDAO().returnBook(lu.getUserId(), b.getIsbn());
                    } else {
                        throw new IllegalStateException("Unsupported item type: " + item.getClass().getSimpleName());
                    }
                    lu.returnItem(item);
                    LibraryItem libItem = library.findItemByIsbn(item.getIsbn());
                    if (libItem != null) libItem.setQuantity(libItem.getQuantity() + 1);

                    model.removeRow(i);
                    borrowedItems.remove(i);
                    success++;
                } catch (Exception ex2) {
                    failed++;
                }
            }

            String msg = "All documents returned successfully.";
            if (failed > 0) msg = "Returned: " + success + ", Failed: " + failed;
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


    private void returnDocument() {
        LibraryUser user = Session.getCurrentUser();
        if (user == null) {
            JOptionPane.showMessageDialog(null, "No user logged in. Please login first.");
            return;
        }

        List<LibraryItem> borrowedItems = new ArrayList<>(user.getBorrowedBooks());
        if (borrowedItems.isEmpty()) {
            JOptionPane.showMessageDialog(null, "You have not borrowed any documents.");
            return;
        }

        // 2) Build bảng UI
        String[] columnNames = {"ID/ISBN", "Title", "Type", "Author(s)"};
        Object[][] data = new Object[borrowedItems.size()][4];
        for (int i = 0; i < borrowedItems.size(); i++) {
            LibraryItem it = borrowedItems.get(i);
            data[i][0] = tryGetIsbnOrId(it);
            data[i][1] = it.getTitle();
            data[i][2] = it.getClass().getSimpleName();
            data[i][3] = it.getAuthors();
        }

        JTable table = new JTable(new javax.swing.table.DefaultTableModel(data, columnNames) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        });
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton returnOneButton = new JButton("Return Selected Document");
        JButton returnAllButton = new JButton("Return All Documents");

        // 3) Trả 1 tài liệu: DB trước, bộ nhớ & UI sau
        returnOneButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(null, "Please select a document to return.");
                return;
            }

            LibraryItem item = borrowedItems.get(row);
            String isbn = tryGetIsbn(item);
            if (isbn == null) {
                JOptionPane.showMessageDialog(null, "Only books with ISBN can be returned via DB.");
                return;
            }

            try {
                new UserDAO().returnBook(user.getId(), isbn);    // cập nhật DB
                if (item instanceof Book b) user.returnBook(b);   // cập nhật bộ nhớ (nếu có API riêng)
                else user.returnItem(item);                       // hoặc API chung
                ((DefaultTableModel) table.getModel()).removeRow(row);
                borrowedItems.remove(row);
                JOptionPane.showMessageDialog(null, "Document returned successfully.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "DB error: " + ex.getMessage());
            }
        });

        // 4) Trả tất cả: lặp qua, cố gắng trả từng cuốn, tổng hợp lỗi
        returnAllButton.addActionListener(e -> {
            if (borrowedItems.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No borrowed documents.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(
                    null,
                    "Are you sure you want to return all borrowed documents?",
                    "Confirm Return All",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) return;

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            List<String> failed = new ArrayList<>();

            // Duyệt bản sao để không vướng khi remove
            for (int i = 0; i < borrowedItems.size(); i++) {
                LibraryItem item = borrowedItems.get(i);
                String isbn = tryGetIsbn(item);
                if (isbn == null) {
                    failed.add(item.getTitle() + " (no ISBN)");
                    continue;
                }
                try {
                    new UserDAO().returnBook(user.getId(), isbn);  // DB
                    if (item instanceof Book b) user.returnBook(b);
                    else user.returnItem(item);                     // bộ nhớ

                    // xóa hàng tương ứng trên bảng (hàng 0 vì luôn xóa đầu)
                    model.removeRow(0);
                } catch (SQLException ex) {
                    failed.add(item.getTitle() + " (" + ex.getMessage() + ")");
                }
            }
            borrowedItems.clear();

            if (failed.isEmpty()) {
                JOptionPane.showMessageDialog(null, "All documents returned successfully.");
            } else {
                JOptionPane.showMessageDialog(null,
                        "Some items failed to return:\n- " + String.join("\n- ", failed));
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(returnOneButton);
        buttonPanel.add(returnAllButton);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(null, panel, "Borrowed Documents", JOptionPane.PLAIN_MESSAGE);
    }

    /** Ưu tiên lấy ISBN nếu là Book, fallback sang getId để hiển thị bảng. */
    private String tryGetIsbnOrId(LibraryItem item) {
        String isbn = tryGetIsbn(item);
        return (isbn != null) ? isbn : item.getId();
    }

    /** Lấy ISBN chỉ khi là Book, nếu không trả null để biết không thể gọi DAO. */
    private String tryGetIsbn(LibraryItem item) {
        if (item instanceof Book) {
            try {
                return ((Book) item).getIsbn();
            } catch (Exception ignored) {}
        }
        return null;
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

            for (Book book : results) {
                BookPanel panel = new BookPanel(book);

                // Bắt sự kiện click Add (thêm sách vào thư viện)
                JButton addButton = new JButton("Add");
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

                    book.setQuantity(quantity);

                    // Kiểm tra ISBN
                    if (!book.getIsbn().equals("N/A")) {
                        LibraryItem existing = library.findItemByIsbn(book.getIsbn());
                        if (existing instanceof Book existingBook) {
                            existingBook.setQuantity(existingBook.getQuantity() + quantity);
                            JOptionPane.showMessageDialog(null, "Book already exists. Increased quantity to " + existingBook.getQuantity() + ".");
                            return;
                        }
                    }

                    // Kiểm tra trùng theo title + description
                    for (LibraryItem item : library.getItems()) {
                        if (item instanceof Book b &&
                                b.getIsbn().equals("N/A") &&
                                b.getTitle().equalsIgnoreCase(book.getTitle()) &&
                                b.getDescription().equalsIgnoreCase(book.getDescription())) {
                            b.setQuantity(b.getQuantity() + quantity);
                            JOptionPane.showMessageDialog(null, "Book matched by title + author. Quantity updated to " + b.getQuantity() + ".");
                            return;
                        }
                    }

                    // Thêm mới nếu không trùng
                    library.addItem(book);
                    JOptionPane.showMessageDialog(null, "Book added to library.");
                });

                // Thêm nút vào panel
                panel.add(addButton, BorderLayout.SOUTH);
                resultPanel.add(panel);
                resultPanel.add(Box.createVerticalStrut(10)); // khoảng cách giữa các book
            }

            JScrollPane scrollPane = new JScrollPane(resultPanel);
            scrollPane.setPreferredSize(new Dimension(600, 600));
            JOptionPane.showMessageDialog(null, scrollPane, "Search Results", JOptionPane.PLAIN_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayBorrowRecords() {
        List<LibraryUser> users = library.getUsers();
        StringBuilder builder = new StringBuilder();

        System.out.println("DEBUG: Number of users = " + users.size());

        for (LibraryUser user : users) {
            List<BorrowRecord> brs = user.getBorrowRecord();

            for (BorrowRecord br : brs) {
                LibraryItem item = br.getItem();

                if (item instanceof Book) {
                    Book book = (Book) item;
                    Date borrowDate = br.getBorrowDate();

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(borrowDate);
                    calendar.add(Calendar.DAY_OF_MONTH, 14);
                    Date returnDate = calendar.getTime();
                    Date now = new Date();

                    builder.append("User: ").append(user.getName()).append("\n");
                    builder.append("Book: ").append(book.toString()).append("\n");
                    builder.append("Borrowed on: ").append(borrowDate.toString()).append("\n");
                    builder.append("Returned on: ").append(returnDate.toString()).append("\n");

                    if (returnDate.before(now)) {
                        builder.append("Overdue").append("\n");
                    }

                    builder.append("-----------------------------\n");
                }
            }
        }

        String message = builder.length() > 0 ? builder.toString() : "No documents.";

        JTextArea textArea = new JTextArea(message);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Font hỗ trợ Unicode
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400)); // Kích thước tùy chỉnh

        JOptionPane.showMessageDialog(null, scrollPane, "Borrow Records", JOptionPane.INFORMATION_MESSAGE);
    }

}
