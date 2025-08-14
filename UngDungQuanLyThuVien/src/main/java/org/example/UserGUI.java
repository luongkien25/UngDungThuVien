package org.example;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserGUI extends JFrame {
    private final Library library = Library.getInstance();
    private final String userId; // user mặc định dùng để mở màn
    private final JPanel centerRoot = new JPanel(new BorderLayout()); // khu trung tâm thay view

    public UserGUI(String userId) {
        super("UserGUI - " + userId);
        this.userId = userId;

        // nạp dữ liệu + login sẵn user này
        library.loadBooksFromDatabase();
        library.loadUsersFromDatabase();
        Session.setCurrentUser(library.findUserById(userId));

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 750);
        setLayout(new BorderLayout());
        add(centerRoot, BorderLayout.CENTER);

        // ==== thanh nút dưới cùng ====
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnDisplay = new JButton("📚 DisplayDocument");   // mới: luôn có để quay lại trang đầu
        JButton btnChange  = new JButton("🔑 ChangePassword");
        JButton btnReturn  = new JButton("🔙 ReturnDocument");
        JButton btnSuggest = new JButton("✨ Gợi ý");
        JButton btnRecent  = new JButton("🕓 Vừa đọc");
        JButton btnLogout  = new JButton("🚪 Logout");            // mới: logout tạm thời
        bottom.add(btnDisplay);
        bottom.add(btnChange);
        bottom.add(btnReturn);
        bottom.add(btnSuggest);
        bottom.add(btnRecent);
        bottom.add(btnLogout);
        add(bottom, BorderLayout.SOUTH);

        // hành vi nút
        btnDisplay.addActionListener(e -> showDisplayView());
        btnChange.addActionListener(e -> changePassword());
        btnReturn.addActionListener(e -> new LibraryGUI_Shared(library).returnDocument());
        btnSuggest.addActionListener(e -> showSuggestView());
        btnRecent.addActionListener(e -> showRecentView());   // sẽ lấy 10 cuốn gần nhất
        btnLogout.addActionListener(e -> doLogout());

        // mở app vào thẳng Display
        showDisplayView();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    /* ============ VIEW 1: DisplayDocument (có Search) ============ */
    private void showDisplayView() {
        centerRoot.removeAll();

        // lấy toàn bộ sách trong RAM
        final List<Book> allBooks = new ArrayList<>();
        for (LibraryItem li : library.getItems()) if (li instanceof Book b) allBooks.add(b);

        // list + scroll
        final JPanel listPanel = makeListPanel();
        final JScrollPane scrollPane = makeScroll(listPanel);

        // thanh search
        final JPanel topBar = new JPanel(new BorderLayout(8, 8));
        topBar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        topBar.setBackground(Color.WHITE);
        final JTextField searchField = new JTextField();
        final JButton searchBtn = new JButton("Search");
        final JButton clearBtn  = new JButton("Clear");
        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTop.setOpaque(false); rightTop.add(searchBtn); rightTop.add(clearBtn);
        topBar.add(new JLabel("Search:"), BorderLayout.WEST);
        topBar.add(searchField, BorderLayout.CENTER);
        topBar.add(rightTop, BorderLayout.EAST);

        // render lần đầu
        final Runnable[] doSearchRef = new Runnable[1];
        renderBooks(listPanel, allBooks,
                () -> { if (doSearchRef[0] != null) doSearchRef[0].run(); },
                true,  // Borrow
                false, // Update
                false  // Remove
        );
        scrollToTop(scrollPane);

        // search action
        final Runnable doSearch = () -> {
            String q = searchField.getText().trim();
            List<Book> toShow = q.isEmpty()
                    ? allBooks
                    : Optional.ofNullable(library.search(q)).orElse(List.of());
            renderBooks(listPanel, toShow,
                    () -> { if (doSearchRef[0] != null) doSearchRef[0].run(); },
                    true, false, false);
            scrollToTop(scrollPane);
        };
        doSearchRef[0] = doSearch;
        searchBtn.addActionListener(ev -> doSearch.run());
        searchField.addActionListener(ev -> doSearch.run());
        clearBtn.addActionListener(ev -> { searchField.setText(""); doSearch.run(); });

        // gắn vào root
        centerRoot.add(topBar, BorderLayout.NORTH);
        centerRoot.add(scrollPane, BorderLayout.CENTER);
        centerRoot.revalidate();
        centerRoot.repaint();
    }

    /* ============ VIEW 2: Gợi ý (cuộn BookPanel, giống Display) ============ */
    private void showSuggestView() {
        centerRoot.removeAll();

        List<Book> books;
        try {
            String uid = currentUserIdOrDefault();
            books = new BookDAO().suggestForUser(uid, 50);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
            return;
        }

        final JPanel listPanel = makeListPanel();
        final JScrollPane scroll = makeScroll(listPanel);

        renderBooks(listPanel, books, null, true, false, false);

        centerRoot.add(scroll, BorderLayout.CENTER);
        centerRoot.revalidate();
        centerRoot.repaint();
        scrollToTop(scroll);
    }

    /* ============ VIEW 3: Vừa đọc (cuộn BookPanel, TOP 10 gần nhất) ============ */
    private void showRecentView() {
        centerRoot.removeAll();

        List<Book> books;
        try {
            String uid = currentUserIdOrDefault();
            books = new UserDAO().getRecentlyReadBooks(uid, 10); // GIỚI HẠN 10
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
            return;
        }

        final JPanel listPanel = makeListPanel();
        final JScrollPane scroll = makeScroll(listPanel);

        renderBooks(listPanel, books, null, true, false, false);

        centerRoot.add(scroll, BorderLayout.CENTER);
        centerRoot.revalidate();
        centerRoot.repaint();
        scrollToTop(scroll);
    }

    /* =================== ACTIONS =================== */

    private void changePassword() {
        if (Session.getCurrentUser() == null) {
            JOptionPane.showMessageDialog(this, "No user logged in.");
            return;
        }
        JPasswordField oldF = new JPasswordField();
        JPasswordField newF = new JPasswordField();
        Object[] msg = {"Old password:", oldF, "New password:", newF};
        int ret = JOptionPane.showConfirmDialog(this, msg, "Change password", JOptionPane.OK_CANCEL_OPTION);
        if (ret == JOptionPane.OK_OPTION) {
            try {
                boolean ok = new UserDAO().changePassword(
                        Session.getCurrentUser().getUserId(),
                        new String(oldF.getPassword()),
                        new String(newF.getPassword()));
                JOptionPane.showMessageDialog(this, ok ? "OK" : "Sai mật khẩu cũ hoặc user không tồn tại");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage());
            }
        }
    }

    private void doLogout() {
        var u = Session.getCurrentUser();
        if (u == null) {
            JOptionPane.showMessageDialog(this, "No user logged in.");
            return;
        }
        Session.logout();
        JOptionPane.showMessageDialog(this, "Logged out: " + u.getName());

        // Mở lại màn hình login trên EDT
        SwingUtilities.invokeLater(() -> new LibraryAppUI());

        // Đóng cửa sổ hiện tại
        dispose();
    }


    /* =================== RENDER HELPERS =================== */

    private JPanel makeListPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        return p;
    }

    private JScrollPane makeScroll(JPanel listPanel) {
        JScrollPane s = new JScrollPane(listPanel);
        s.setPreferredSize(new Dimension(900, 650));
        s.getVerticalScrollBar().setUnitIncrement(16);
        return s;
    }

    private void scrollToTop(JScrollPane sp) {
        SwingUtilities.invokeLater(() -> {
            sp.getViewport().setViewPosition(new Point(0, 0));
            sp.getVerticalScrollBar().setValue(0);
        });
    }

    private void renderBooks(JPanel listPanel, List<Book> books,
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
            card.add(bookPanel, BorderLayout.CENTER);

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

    private String currentUserIdOrDefault() {
        return (Session.getCurrentUser() != null)
                ? Session.getCurrentUser().getUserId()
                : this.userId;
    }
}
