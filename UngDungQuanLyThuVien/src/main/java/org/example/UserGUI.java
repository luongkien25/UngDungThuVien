package org.example;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.text.Collator;
import java.util.*;
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
        JButton btnDisplay = new JButton("📚 DisplayDocument");   // luôn có để quay lại trang đầu
        JButton btnChange  = new JButton("🔑 ChangePassword");
        JButton btnReturn  = new JButton("🔙 ReturnDocument");
        JButton btnSuggest = new JButton("✨ Gợi ý");
        JButton btnRecent  = new JButton("🕓 Vừa đọc");
        JButton btnLogout  = new JButton("🚪 Logout");
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
        btnRecent.addActionListener(e -> showRecentView());   // lấy 10 cuốn gần nhất
        btnLogout.addActionListener(e -> doLogout());

        // mở app vào thẳng Display
        showDisplayView();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    /* ============ VIEW 1: DisplayDocument (có Search + Sort) ============ */
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

        // Quick category
        final String[] quickTerms = new String[]{
                "ALL",
                "NOVEL","HISTORY","SCIENCE","MATHEMATICS","ENGINEERING","COMPUTER","PROGRAMMING","JAVA","DATABASE","ART",
                "ECONOMICS","BUSINESS","MANAGEMENT","PHILOSOPHY","PSYCHOLOGY","EDUCATION","CHILDREN","FANTASY","MYSTERY",
                "MACHINE LEARNING","DATA SCIENCE","CHEMISTRY","PHYSICS","BIOLOGY","MEDICINE","LAW","POLITICS",
                "CULTURE","MUSIC","DESIGN","ARCHITECTURE","PHOTOGRAPHY","TRAVEL","COOKING","HEALTH","SPORT",
                "POETRY","DRAMA","LITERATURE","ENVIRONMENT","ASTRONOMY","GEOGRAPHY","ANTHROPOLOGY","MYTHOLOGY","RELIGION",
                "SELF HELP","PRODUCTIVITY","STARTUPS","MARKETING","FINANCE","INVESTMENT","NETWORKING"
        };
        final JComboBox<String> categoryBox = new JComboBox<>(quickTerms);

        // Sort options (6 tuỳ chọn)
        final String[] sortOptions = new String[]{
                "None",
                "Tên (A→Z)",
                "Tên (Z→A)",
                "Borrowed (nhiều→ít)",
                "Borrowed (ít→nhiều)",
                "Quantity (nhiều→ít)",
                "Quantity (ít→nhiều)"
        };
        final JComboBox<String> sortBox = new JComboBox<>(sortOptions);

        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTop.setOpaque(false);
        rightTop.add(categoryBox);
        rightTop.add(sortBox);
        rightTop.add(searchBtn);

        topBar.add(new JLabel("Search:"), BorderLayout.WEST);
        topBar.add(searchField, BorderLayout.CENTER);
        topBar.add(rightTop, BorderLayout.EAST);

        // render/search handler
        final Runnable[] doSearchRef = new Runnable[1];
        final Runnable doSearch = () -> {
            String q = searchField.getText().trim();
            List<Book> toShow = q.isEmpty()
                    ? new ArrayList<>(allBooks)
                    : Optional.ofNullable(library.search(q)).orElse(List.of());

            // LẤY COUNT MAP (đóng băng biến để dùng trong lambda)
            Map<String, Long> tmpCountMap;
            try {
                tmpCountMap = new BookDAO().getBorrowCountsMap(); // Map<String, Long>
            } catch (SQLException ex) {
                tmpCountMap = Collections.emptyMap();
            }
            final Map<String, Long> countMap = tmpCountMap; // final để dùng trong lambda
            final Collator vi = Collator.getInstance(new Locale("vi", "VN"));
            final String sortSel = String.valueOf(sortBox.getSelectedItem());

            // Sort theo lựa chọn
            toShow.sort((b1, b2) -> {
                final String t1 = b1.getTitle() == null ? "" : b1.getTitle();
                final String t2 = b2.getTitle() == null ? "" : b2.getTitle();
                final long c1 = borrowedOf(countMap, b1);
                final long c2 = borrowedOf(countMap, b2);
                switch (sortSel) {
                    case "Tên (A→Z)": return vi.compare(t1, t2);
                    case "Tên (Z→A)": return -vi.compare(t1, t2);
                    case "Borrowed (nhiều→ít)": return Long.compare(c2, c1);
                    case "Borrowed (ít→nhiều)": return Long.compare(c1, c2);
                    case "Quantity (nhiều→ít)": return Integer.compare(b2.getQuantity(), b1.getQuantity());
                    case "Quantity (ít→nhiều)": return Integer.compare(b1.getQuantity(), b2.getQuantity());
                    case "None": default: return 0;
                }
            });

            renderBooks(listPanel, toShow,
                    () -> { if (doSearchRef[0] != null) doSearchRef[0].run(); },
                    true,  // Borrow
                    false, // Update
                    false  // Remove
            );
            scrollToTop(scrollPane);
        };
        doSearchRef[0] = doSearch;

        // render lần đầu theo Sort mặc định + rỗng query
        doSearch.run();

        // actions
        searchBtn.addActionListener(ev -> doSearch.run());
        searchField.addActionListener(ev -> doSearch.run());
        categoryBox.addActionListener(ev -> {
            String sel = String.valueOf(categoryBox.getSelectedItem());
            if ("All".equalsIgnoreCase(sel)) searchField.setText("");
            else searchField.setText(sel);
            doSearch.run();
            searchField.requestFocusInWindow();
        });
        sortBox.addActionListener(ev -> doSearch.run());

        // (tuỳ chọn) ESC để xoá nhanh ô search
        javax.swing.KeyStroke esc = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0);
        searchField.getInputMap(JComponent.WHEN_FOCUSED).put(esc, "clearSearch");
        searchField.getActionMap().put("clearSearch", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!searchField.getText().isEmpty()) searchField.setText("");
            }
        });

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

    /* ============ VIEW 3: Vừa đọc (TOP 10 gần nhất) ============ */
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
        SwingUtilities.invokeLater(LibraryAppUI::new);

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
                String qrContent;
                if (b.getTitle() != null && !b.getTitle().isBlank()) {
                    qrContent = "https://www.google.com/search?q="
                            + java.net.URLEncoder.encode(b.getTitle(), java.nio.charset.StandardCharsets.UTF_8)
                            + "+site:books.google.com";
                } else {
                    qrContent = "https://books.google.com"; // fallback nếu không có title
                }
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

    // ===== Helpers cho Sort Borrowed =====
    private static long borrowedOf(Map<String, Long> countMap, Book b) {
        if (b == null || b.getId() == null) return 0L;
        return countMap.getOrDefault(b.getId(), 0L);
    }
}
