package org.example;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import java.sql.SQLException;

public class LibraryAppUI {
    private final Library library = Library.getInstance();
    private JFrame frame;

    public LibraryAppUI() {
        // Nạp dữ liệu để 2 GUI dùng luôn (khi chạy độc lập cũng ok)
        library.loadBooksFromDatabase();
        library.loadUsersFromDatabase();

        frame = new JFrame("📚 Library Launcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 350);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(50, 40, 50, 40));
        panel.setBackground(new java.awt.Color(240, 248, 255)); // cùng màu với trường hợp 1

        JLabel title = new JLabel("Welcome to the Library System", SwingConstants.CENTER);
        title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 25));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(new java.awt.Color(33, 66, 99));

        JButton btnAdmin = new JButton("Admin");
        JButton btnUser  = new JButton("User");

        btnAdmin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnUser.setAlignmentX(Component.CENTER_ALIGNMENT);

        java.awt.Font btnFont = new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 20);
        btnAdmin.setFont(btnFont);
        btnUser.setFont(btnFont);

// Kích thước nút
        btnAdmin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        btnUser.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));

// Màu nền + style giống trường hợp 1
        btnAdmin.setBackground(new java.awt.Color(70, 130, 180));
        btnAdmin.setForeground(java.awt.Color.WHITE);
        btnAdmin.setFocusPainted(false);
        btnAdmin.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        btnUser.setBackground(new java.awt.Color(34, 139, 34));
        btnUser.setForeground(java.awt.Color.WHITE);
        btnUser.setFocusPainted(false);
        btnUser.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

// Hover effect đồng nhất
        btnAdmin.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnAdmin.setBackground(new java.awt.Color(60, 110, 160));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnAdmin.setBackground(new java.awt.Color(70, 130, 180));
            }
        });
        btnUser.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnUser.setBackground(new java.awt.Color(24, 119, 24));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnUser.setBackground(new java.awt.Color(34, 139, 34));
            }
        });

// Gắn action như cũ
        btnAdmin.addActionListener(e -> loginAdminAndLaunch());
        btnUser.addActionListener(e -> loginUserAndLaunch());

// Lắp vào panel
        panel.add(Box.createVerticalGlue());
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 40)));
        panel.add(btnAdmin);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(btnUser);
        panel.add(Box.createVerticalGlue());

// Đặt vào frame
        frame.setContentPane(panel);
        frame.setVisible(true);
    }

    private void loginAdminAndLaunch() {
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        Object[] msg = { "Admin userId:", userField, "Password:", passField };
        int ok = JOptionPane.showConfirmDialog(frame, msg, "Admin Login", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        String uid = userField.getText().trim();
        String pw  = new String(passField.getPassword());

        try {
            User u = new UserDAO().authenticate(uid, pw);
            if (u == null || !u.isAdmin()) {
                JOptionPane.showMessageDialog(frame, "Invalid admin credentials or not an admin.");
                return;
            }
            Session.setCurrentUser(u);
            SwingUtilities.invokeLater(() -> { new AdminGUI(); frame.dispose(); });
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "DB error: " + e.getMessage());
        }
    }

    private void loginUserAndLaunch() {
        Object[] options = {"Login", "Sign up", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
                frame, "Login or Sign up?", "User",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) return;

        if (choice == JOptionPane.YES_OPTION) {
            // Login
            JTextField idF = new JTextField();
            JPasswordField pwF = new JPasswordField();
            Object[] msg = { "User ID:", idF, "Password:", pwF };
            if (JOptionPane.showConfirmDialog(frame, msg, "User Login", JOptionPane.OK_CANCEL_OPTION)
                    != JOptionPane.OK_OPTION) return;

            String uid = idF.getText().trim();
            String pw  = new String(pwF.getPassword());

            try {
                User u = new UserDAO().authenticate(uid, pw);
                if (u == null) { JOptionPane.showMessageDialog(frame, "Wrong credentials."); return; }
                Session.setCurrentUser(u);

                if (u.isAdmin()) {
                    // Nếu user là admin, chuyển thẳng AdminGUI
                    SwingUtilities.invokeLater(() -> { new AdminGUI(); frame.dispose(); });
                } else {
                    SwingUtilities.invokeLater(() -> { new UserGUI(u.getUserId()); frame.dispose(); });
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(frame, "DB error: " + e.getMessage());
            }
        } else if (choice == JOptionPane.NO_OPTION) {
            // Sign up
            JTextField idF = new JTextField();
            JTextField nameF = new JTextField();
            JPasswordField pwF = new JPasswordField();
            JPasswordField pw2F = new JPasswordField();

            Object[] msg = {
                    "New User ID:", idF,
                    "Name:", nameF,
                    "Password:", pwF,
                    "Confirm Password:", pw2F
            };
            if (JOptionPane.showConfirmDialog(frame, msg, "Sign up", JOptionPane.OK_CANCEL_OPTION)
                    != JOptionPane.OK_OPTION) return;

            String uid = idF.getText().trim();
            String name = nameF.getText().trim();
            String pw1 = new String(pwF.getPassword());
            String pw2 = new String(pw2F.getPassword());

            if (uid.isEmpty() || name.isEmpty() || pw1.isEmpty() || !pw1.equals(pw2)) {
                JOptionPane.showMessageDialog(frame, "Invalid input or password mismatch.");
                return;
            }

            try {
                if (new UserDAO().getUserById(uid) != null) {
                    JOptionPane.showMessageDialog(frame, "User already exists: " + uid);
                    return;
                }
                User u = new User(uid, name, "USER");
                new UserDAO().insertUser(u, pw1); // hash + salt
                library.addUser(u); // RAM
                Session.setCurrentUser(u);
                SwingUtilities.invokeLater(() -> { new UserGUI(u.getUserId()); frame.dispose(); });
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(frame, "DB error: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LibraryAppUI::new);
    }
}