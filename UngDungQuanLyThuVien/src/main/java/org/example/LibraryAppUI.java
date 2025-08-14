package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LibraryAppUI {
    private final Library library = Library.getInstance();
    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private boolean isAdminLoggedIn = false;
    private LibraryUserImpl currentUser; // Người dùng hiện tại

    private JButton btnLoginAdmin;
    private JButton btnLogoutAdmin;
    private JButton btnAddDocument;
    private JButton btnRemoveDocument;
    private JButton btnUpdateDocument;
    private JButton btnDisplayDocuments;
    private JButton btnSearchGoogleBooks;
    private JButton btnAddUser;
    private JButton btnDisplayBorrowRecords;
    private JButton btnExitAdmin;


    public LibraryAppUI() {
        frame = new JFrame("📚 Library Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 700);
        frame.setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Thêm các màn hình
        mainPanel.add(createWelcomePanel(), "welcome");
        mainPanel.add(createAdminPanel(), "admin");
        mainPanel.add(createUserPanel(), "user");

        frame.add(mainPanel);
        frame.setVisible(true);

        // Mặc định hiển thị welcome
        cardLayout.show(mainPanel, "welcome");
    }

    /** Màn hình chào */
    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(50, 40, 50, 40));
        panel.setBackground(new Color(240, 248, 255));

        JLabel title = new JLabel("Welcome to the Library System", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(new Color(33, 66, 99));

        JButton adminBtn = new JButton("Admin");
        JButton userBtn = new JButton("User");

        adminBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        userBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        Font btnFont = new Font("Segoe UI", Font.BOLD, 20);
        adminBtn.setFont(btnFont);
        userBtn.setFont(btnFont);

        // Kích thước nút
        adminBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        userBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));

        // Màu nền nút
        adminBtn.setBackground(new Color(70, 130, 180));
        adminBtn.setForeground(Color.WHITE);
        adminBtn.setFocusPainted(false);

        userBtn.setBackground(new Color(34, 139, 34));
        userBtn.setForeground(Color.WHITE);
        userBtn.setFocusPainted(false);

        // Viền bo tròn
        adminBtn.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        userBtn.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Hover effect
        adminBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                adminBtn.setBackground(new Color(60, 110, 160));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                adminBtn.setBackground(new Color(70, 130, 180));
            }
        });

        userBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                userBtn.setBackground(new Color(24, 119, 24));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                userBtn.setBackground(new Color(34, 139, 34));
            }
        });

        // Action
        adminBtn.addActionListener(e -> cardLayout.show(mainPanel, "admin"));
        userBtn.addActionListener(e -> cardLayout.show(mainPanel, "user"));

        // Thêm vào panel
        panel.add(Box.createVerticalGlue());
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 40)));
        panel.add(adminBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(userBtn);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    /** Giao diện Admin */
    private JPanel createAdminPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        panel.setBackground(new Color(240, 248, 255)); // Màu nền giống Welcome

        JLabel title = new JLabel("Admin Panel", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(new Color(33, 66, 99));
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        Font btnFont = new Font("Segoe UI", Font.BOLD, 16);

        // Tạo các nút (dùng anonymous class thay vì lambda)
        btnLoginAdmin = createAdminButton("Login", btnFont, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loginAdmin();
            }
        });
        btnLogoutAdmin = createAdminButton("Logout", btnFont, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logoutAdmin();
            }
        });
        btnAddDocument = createAdminButton("Add Document", btnFont, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addDocument();
            }
        });
        btnRemoveDocument = createAdminButton("Remove Document", btnFont, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeDocument();
            }
        });
        btnUpdateDocument = createAdminButton("Update Document", btnFont, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateDocument();
            }
        });
        btnDisplayDocuments = createAdminButton("Display Documents", btnFont, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                displayDocuments();
            }
        });
        btnSearchGoogleBooks = createAdminButton("Search Google Books", btnFont, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchGoogleBooks();
            }
        });
        btnAddUser = createAdminButton("Add User", btnFont, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addUser();
            }
        });
        btnDisplayBorrowRecords = createAdminButton("Display Borrow Records", btnFont, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                displayBorrowRecords();
            }
        });
        btnExitAdmin = createAdminButton("Exit", btnFont, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "welcome");
            }
        });

        // Thêm nút vào panel
        panel.add(btnLoginAdmin);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnLogoutAdmin);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnAddDocument);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnRemoveDocument);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnUpdateDocument);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnDisplayDocuments);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnSearchGoogleBooks);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnAddUser);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnDisplayBorrowRecords);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnExitAdmin);

        // Ban đầu chỉ hiện Login + Exit
        showAdminButtons(false);
        btnLoginAdmin.setVisible(true);
        btnExitAdmin.setVisible(true);

        return panel;
    }

    /** Hàm tạo nút Admin */
    private JButton createAdminButton(String text, Font font, ActionListener listener) {
        JButton button = new JButton(text);
        button.setFont(font);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        button.setFocusPainted(false);
        button.setBackground(new Color(200, 220, 240));
        button.setForeground(Color.BLACK);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(160, 180, 200)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        button.addActionListener(listener);
        return button;
    }

    /** Hiển thị hoặc ẩn các nút chức năng (trừ Login & Exit) */
    private void showAdminButtons(boolean show) {
        btnLogoutAdmin.setVisible(show);
        btnAddDocument.setVisible(show);
        btnRemoveDocument.setVisible(show);
        btnUpdateDocument.setVisible(show);
        btnDisplayDocuments.setVisible(show);
        btnSearchGoogleBooks.setVisible(show);
        btnAddUser.setVisible(show);
        btnDisplayBorrowRecords.setVisible(show);
    }

    /** Login Admin */
    private void loginAdmin() {
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        Object[] message = {
                "Username:", usernameField,
                "Password:", passwordField
        };

        int option = JOptionPane.showConfirmDialog(
                frame, message, "Admin Login", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            if (username.equals("admin") && password.equals("1234")) {
                isAdminLoggedIn = true;
                JOptionPane.showMessageDialog(frame, "Login successful");
                showAdminButtons(true); // Hiện các nút chức năng
                btnLoginAdmin.setVisible(false); // Ẩn nút login
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid admin credentials");
            }
        }
    }

    private void logoutAdmin() {
        isAdminLoggedIn = false;
        showAdminButtons(false); // Ẩn các nút chức năng
        btnLoginAdmin.setVisible(true); // Hiện lại nút login
        btnExitAdmin.setVisible(true);  // Đảm bảo nút Exit vẫn hiện

        JOptionPane.showMessageDialog(frame, "Admin has been logged out.");
        cardLayout.show(mainPanel, "admin"); // Quay lại admin panel
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
        library.addItem(book);
        JOptionPane.showMessageDialog(null, "Document added.");
    }

    private void removeDocument() {
        String isbn = JOptionPane.showInputDialog("Enter ISBN to remove:");
        LibraryItem item = library.findItemByIsbn(isbn);
        if (item != null) {
            library.removeItem(item);
            JOptionPane.showMessageDialog(null, "Document removed.");
        } else {
            JOptionPane.showMessageDialog(null, "Document not found.");
        }
    }

    private void updateDocument() {
        String isbn = JOptionPane.showInputDialog("Enter ISBN to update:");
        LibraryItem item = library.findItemByIsbn(isbn);
        if (item != null) {
            String newTitle = JOptionPane.showInputDialog("New title:");
            String newDesc = JOptionPane.showInputDialog("New description:");
            String qtyStr = JOptionPane.showInputDialog("New quantity:");
            try {
                int quantity = Integer.parseInt(qtyStr);
                item.setTitle(newTitle);
                item.setDescription(newDesc);
                item.setQuantity(quantity);
                JOptionPane.showMessageDialog(null, "Document updated.");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Invalid quantity.");
            }
        } else {
            JOptionPane.showMessageDialog(null, "Document not found.");
        }
    }


    private void displayDocuments() {
        List<LibraryItem> items = library.getItems();
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No documents.");
            return;
        }

        String[] columnNames = {"Title", "Authors", "ISBN", "Quantity"};
        Object[][] data = new Object[items.size()][4];

        for (int i = 0; i < items.size(); i++) {
            LibraryItem item = items.get(i);
            data[i][0] = item.getTitle();

            // Xử lý authors an toàn (do LibraryItem.getAuthors() trả về Object)
            Object authorsObj = item.getAuthors();
            String authorsStr = "";
            if (authorsObj == null) {
                authorsStr = "";
            } else if (authorsObj instanceof String) {
                authorsStr = (String) authorsObj;
            } else if (authorsObj instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) authorsObj;
                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < list.size(); k++) {
                    if (k > 0) sb.append(", ");
                    sb.append(String.valueOf(list.get(k)));
                }
                authorsStr = sb.toString();
            } else {
                authorsStr = authorsObj.toString();
            }
            data[i][1] = authorsStr;

            data[i][2] = item.getIsbn();
            data[i][3] = item.getQuantity();
        }

        JTable table = new JTable(data, columnNames);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    LibraryItem selectedItem = items.get(row);

                    JPanel bookPanel = new JPanel(new BorderLayout(10, 10));
                    bookPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                    JTextArea infoArea = new JTextArea(selectedItem.toString());
                    infoArea.setEditable(false);
                    infoArea.setLineWrap(true);
                    infoArea.setWrapStyleWord(true);
                    bookPanel.add(infoArea, BorderLayout.CENTER);

                    try {
                        java.awt.image.BufferedImage qrImage =
                                QRCodeGenerator.generateQRCodeImage(selectedItem.toString(), 120, 120);
                        JLabel qrLabel = new JLabel(new ImageIcon(qrImage));
                        bookPanel.add(qrLabel, BorderLayout.EAST);
                    } catch (Exception e) {
                        System.err.println("Failed to generate QR code: " + e.getMessage());
                    }

                    JOptionPane.showMessageDialog(frame, bookPanel, "Document Info", JOptionPane.PLAIN_MESSAGE);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(700, 400));
        JOptionPane.showMessageDialog(frame, scrollPane, "Document List", JOptionPane.PLAIN_MESSAGE);
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

            // Main panel để chứa danh sách sách (theo chiều dọc)
            JPanel resultPanel = new JPanel();
            resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));

            for (Book book : results) {
                // Chỉ tạo panel hiển thị thông tin sách, không có nút borrow
                JPanel bookPanel = new JPanel(new BorderLayout(10, 10));
                bookPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));

                // Thông tin sách bên trái
                JTextArea infoArea = new JTextArea(
                        "Title: " + book.getTitle() + "\n" +
                                "Authors: " + book.getAuthors() + "\n" +
                                "ISBN: " + book.getIsbn() + "\n" +
                                "Description: " + book.getDescription()
                );
                infoArea.setEditable(false);
                infoArea.setLineWrap(true);
                infoArea.setWrapStyleWord(true);
                bookPanel.add(new JScrollPane(infoArea), BorderLayout.CENTER);

                // Nút Add bên dưới thông tin sách
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
                            JOptionPane.showMessageDialog(null,
                                    "Book already exists. Increased quantity to " + existingBook.getQuantity() + ".");
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
                            JOptionPane.showMessageDialog(null,
                                    "Book matched by title + author. Quantity updated to " + b.getQuantity() + ".");
                            return;
                        }
                    }

                    // Thêm mới nếu không trùng
                    library.addItem(book);
                    JOptionPane.showMessageDialog(null, "Book added to library.");
                });

                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                btnPanel.add(addButton);

                bookPanel.add(btnPanel, BorderLayout.SOUTH);

                // Thêm panel sách vào danh sách hiển thị
                resultPanel.add(bookPanel);
                resultPanel.add(Box.createVerticalStrut(10)); // khoảng cách giữa các sách
            }

            // Cuộn từ trên xuống
            JScrollPane scrollPane = new JScrollPane(resultPanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);

            JOptionPane.showMessageDialog(null, scrollPane, "Search Results", JOptionPane.PLAIN_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void addUser() {
        String userId = JOptionPane.showInputDialog("Enter User ID:");
        if (userId == null || userId.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "User ID cannot be empty.");
            return;
        }

        String name = JOptionPane.showInputDialog("Enter User Name:");
        if (name == null || name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "User Name cannot be empty.");
            return;
        }

        // Kiểm tra xem user đã tồn tại chưa
        if (library.findUserById(userId) != null) {
            JOptionPane.showMessageDialog(null, "User already exists with ID: " + userId);
            return;
        }

        // Tạo user mới (mật khẩu để trống, user sẽ tạo khi sign in)
        LibraryUserImpl newUser = new LibraryUserImpl(userId, name);
        library.addUser(newUser);

        JOptionPane.showMessageDialog(null, "User added successfully:\nID: " + userId + "\nName: " + name);
    }

    private void displayBorrowRecords() {
        StringBuilder records = new StringBuilder();
        for (LibraryUser user : library.getUsers()) {
            records.append(user.getName()).append(" (ID: ").append(user.getUserId()).append(")\n");
            for (BorrowRecord record : user.getBorrowRecord()) {
                records.append("  - ").append(record.getItem().getTitle())
                        .append(" (Borrowed on: ").append(record.getBorrowDate()).append(")\n");
            }
            records.append("----------------------\n");
        }

        if (records.length() == 0) {
            JOptionPane.showMessageDialog(frame, "No borrow records found.");
        } else {
            JTextArea textArea = new JTextArea(records.toString());
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(600, 400));
            JOptionPane.showMessageDialog(frame, scrollPane, "Borrow Records", JOptionPane.PLAIN_MESSAGE);
        }
    }

    /** Màn hình User */
    // Nút cho User Panel
    private JButton btnSigninUser;
    private JButton btnLoginUser;
    private JButton btnLogoutUser;
    private JButton btnDisplayDocs;
    private JButton btnReturnDoc;
    private JButton btnUserInfo;
    private JButton btnExitUser;

    private JPanel createUserPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        panel.setBackground(new Color(240, 248, 255));

        JLabel title = new JLabel("User Panel", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(new Color(33, 66, 99));
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        Font btnFont = new Font("Segoe UI", Font.BOLD, 16);

        // Tạo các nút
        btnSigninUser = createUserButton("Sign in", btnFont, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                signinUser();
            }
        });
        btnLoginUser = createUserButton("Login", btnFont, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loginUser();
            }
        });
        btnLogoutUser = createUserButton("Logout", btnFont, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logoutUser();
            }
        });
        btnDisplayDocs = createUserButton("Display Document", btnFont, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                displayDocuments();
            }
        });
        btnReturnDoc = createUserButton("Return Document", btnFont, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                returnDocument();
            }
        });
        btnUserInfo = createUserButton("Display User Info", btnFont, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                displayUserInfo();
            }
        });
        btnExitUser = createUserButton("Exit", btnFont, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "welcome");
            }
        });

        // Thêm nút vào panel
        panel.add(btnSigninUser);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnLoginUser);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnLogoutUser);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnDisplayDocs);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnReturnDoc);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnUserInfo);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnExitUser);

        // Ban đầu chỉ hiện Sign in, Login, Exit
        showUserButtons(false);
        btnSigninUser.setVisible(true);
        btnLoginUser.setVisible(true);
        btnExitUser.setVisible(true);

        return panel;
    }

    /** Hàm tạo nút user */
    private JButton createUserButton(String text, Font font, ActionListener listener) {
        JButton button = new JButton(text);
        button.setFont(font);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        button.setFocusPainted(false);
        button.setBackground(new Color(200, 220, 240));
        button.setForeground(Color.BLACK);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(160, 180, 200)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        button.addActionListener(listener);
        return button;
    }

    /** Ẩn/hiện nút chức năng User (trừ Sign in, Login, Exit) */
    private void showUserButtons(boolean show) {
        btnLogoutUser.setVisible(show);
        btnDisplayDocs.setVisible(show);
        btnReturnDoc.setVisible(show);
        btnUserInfo.setVisible(show);
    }

    // Sign in (đặt mật khẩu lần đầu)
    private void signinUser() {
        String userId = JOptionPane.showInputDialog("Enter your User ID:");
        if (userId == null || userId.trim().isEmpty()) return;

        LibraryUser foundUser = library.findUserById(userId);
        if (foundUser == null) {
            JOptionPane.showMessageDialog(null, "User ID not found. Please contact admin to create an account first.");
            return;
        }

        LibraryUserImpl existingUser = (LibraryUserImpl) foundUser;

        // Nếu đã có password thì không cho đăng ký lại
        if (existingUser.getPassword() != null && !existingUser.getPassword().isEmpty()) {
            JOptionPane.showMessageDialog(null, "This account already has a password. Please use Login instead.");
            return;
        }

        String password = JOptionPane.showInputDialog("Set your password:");
        if (password == null || password.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Password cannot be empty.");
            return;
        }

        existingUser.setPassword(password);
        JOptionPane.showMessageDialog(null, "Password set successfully! You can now log in.");
    }


    /** Login User */
    private void loginUser() {
        String userId = JOptionPane.showInputDialog("Enter your User ID:");
        if (userId == null || userId.trim().isEmpty()) return;

        LibraryUser foundUser = library.findUserById(userId);
        if (foundUser == null) {
            JOptionPane.showMessageDialog(null, "User ID not found. Please sign in first.");
            return;
        }

        LibraryUserImpl existingUser = (LibraryUserImpl) foundUser;

        if (existingUser.getPassword() == null || existingUser.getPassword().isEmpty()) {
            JOptionPane.showMessageDialog(null, "This account does not have a password yet. Please use Sign in first.");
            return;
        }

        String password = JOptionPane.showInputDialog("Enter your password:");
        if (password == null) return;

        if (!password.equals(existingUser.getPassword())) {
            JOptionPane.showMessageDialog(null, "Incorrect password. Please try again.");
            return;
        }

        currentUser = existingUser;
        JOptionPane.showMessageDialog(null, "Welcome, " + currentUser.getName() + "!");
        showUserButtons(true); // Hiện các nút chức năng
        btnSigninUser.setVisible(false);
        btnLoginUser.setVisible(false);
    }

    /** Logout User */
    private void logoutUser() {
        currentUser = null;
        showUserButtons(false); // Ẩn các nút chức năng
        btnSigninUser.setVisible(true);
        btnLoginUser.setVisible(true);
        JOptionPane.showMessageDialog(frame, "You have been logged out.");
    }


    private void returnDocument() {
        LibraryUser user = Session.getCurrentUser();
        if (user == null) {
            JOptionPane.showMessageDialog(null, "No user logged in. Please login first.");
            return;
        }

        List<LibraryItem> borrowedItems = user.getBorrowedBooks();
        if (borrowedItems.isEmpty()) {
            JOptionPane.showMessageDialog(null, "You have not borrowed any documents.");
            return;
        }

        String[] columnNames = {"ID/ISBN", "Title", "Type", "Author(s)"};
        Object[][] data = new Object[borrowedItems.size()][4];
        for (int i = 0; i < borrowedItems.size(); i++) {
            LibraryItem item = borrowedItems.get(i);
            data[i][0] = item.getId();
            data[i][1] = item.getTitle();
            data[i][2] = item.getClass().getSimpleName();
            data[i][3] = item.getAuthors();
        }

        JTable table = new JTable(data, columnNames);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton returnOneButton = new JButton("Return Selected Document");
        JButton returnAllButton = new JButton("Return All Documents");

        returnOneButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                LibraryItem item = borrowedItems.get(selectedRow);
                user.returnItem(item);
                JOptionPane.showMessageDialog(null, "Document returned successfully.");
                ((DefaultTableModel) table.getModel()).removeRow(selectedRow);
            } else {
                JOptionPane.showMessageDialog(null, "Please select a document to return.");
            }
        });

        returnAllButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(null,
                    "Are you sure you want to return all borrowed documents?",
                    "Confirm Return All", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                for (LibraryItem item : borrowedItems) {
                    user.returnItem(item);
                }
                JOptionPane.showMessageDialog(null, "All documents returned successfully.");
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


    private void displayUserInfo() {
        LibraryUser user = Session.getCurrentUser();
        if (user == null) {
            JOptionPane.showMessageDialog(null, "No user logged in. Please login first.");
            return;
        }
        JOptionPane.showMessageDialog(null, user.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LibraryAppUI::new);
    }
}
