package org.example;

import java.sql.*;
import java.time.LocalDate;               // giữ nguyên import sẵn có của bạn
import java.util.ArrayList;
import java.util.List;

// ===== bổ sung cho các phương thức bạn đang dùng =====
import java.util.Date;
import java.util.Map;
import java.util.LinkedHashMap;

public class UserDAO {

    // Lấy toàn bộ user (kèm role)
    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT user_id, name, role FROM users";
        List<User> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new User(
                        rs.getString("user_id"),
                        rs.getString("name"),
                        rs.getString("role")
                ));
            }
        }
        return list;
    }

    // Trong UserDAO
    public List<UserBorrowStat> getUserStats(boolean onlyActive) throws SQLException {
        String sql = onlyActive
                ? """
          SELECT u.user_id, u.name, u.role, COALESCE(COUNT(br.id),0) AS borrow_count
           FROM users u
           LEFT JOIN borrow_records br
             ON br.user_id = u.user_id 
           WHERE u.role = "USER"
           GROUP BY u.user_id, u.name, u.role
           ORDER BY borrow_count DESC, u.name ASC
          """
                : """
           SELECT u.user_id, u.name, u.role, COALESCE(COUNT(br.id),0) AS borrow_count
           FROM users u
           LEFT JOIN borrow_records br
             ON br.user_id = u.user_id
           GROUP BY u.user_id, u.name, u.role
           ORDER BY borrow_count DESC, u.name ASC
          """;

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<UserBorrowStat> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new UserBorrowStat(
                        rs.getString("user_id"),
                        rs.getString("name"),
                        rs.getString("role"),
                        rs.getInt("borrow_count")
                ));
            }
            return list;
        }
    }

    public User getUserById(String userId) throws SQLException {
        String sql = "SELECT user_id, name, role FROM users WHERE user_id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new User(rs.getString("user_id"), rs.getString("name"), rs.getString("role"));
            }
        }
    }
    // Lấy lịch sử mượn của 1 user (cả borrow/due/return)
    public List<BorrowRecord> getBorrowRecords(String userId) throws SQLException {
        String sql = """
            SELECT br.id,
                   br.borrow_date,
                   br.due_date,
                   br.return_date,
                   b.id AS book_id,
                   b.isbn, b.title, b.authors, b.category, b.quantity, b.thumbnail_link
              FROM borrow_records br
              JOIN books b ON b.id = br.book_id
             WHERE br.user_id = ?
             ORDER BY br.borrow_date DESC, br.id DESC
        """;
        List<BorrowRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Book book = new Book(
                            rs.getString("title"),
                            rs.getString("authors"),
                            rs.getString("category"),
                            rs.getString("isbn"),
                            rs.getInt("quantity"),
                            rs.getString("thumbnail_link")
                    );
                    book.setId(String.valueOf(rs.getInt("book_id")));

                    Date borrow = new Date(rs.getTimestamp("borrow_date").getTime());

                    Timestamp dueTs = rs.getTimestamp("due_date");
                    Date due = (dueTs != null) ? new Date(dueTs.getTime()) : null;

                    Timestamp retTs = rs.getTimestamp("return_date");
                    Date ret = (retTs != null) ? new Date(retTs.getTime()) : null;

                    list.add(new BorrowRecord(
                            rs.getLong("id"),
                            book,
                            borrow,
                            due,
                            ret
                    ));
                }
            }
        }
        return list;
    }

    // Thêm user (mặc định role=USER nếu trống)
    public void insertUser(User u, String rawPassword) throws SQLException {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(rawPassword == null ? "" : rawPassword, salt);

        String sql = "INSERT INTO users(user_id, name, role, password_hash, password_salt) VALUES(?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getUserId());
            ps.setString(2, u.getName());
            ps.setString(3, (u.getRole()==null || u.getRole().isBlank()) ? "USER" : u.getRole());
            ps.setString(4, hash);
            ps.setString(5, salt);
            ps.executeUpdate();
        }
    }

    // Mượn theo book_id (MySQL: NOW() + INTERVAL 14 DAY)
    public void borrowBookByBookId(String userId, int bookId) throws SQLException {
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);

            // Khóa và kiểm tra tồn
            try (PreparedStatement lock = c.prepareStatement(
                    "SELECT quantity FROM books WHERE id=? FOR UPDATE")) {
                lock.setInt(1, bookId);
                try (ResultSet rs = lock.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Book not found");
                    if (rs.getInt(1) < 1) throw new SQLException("Out of stock");
                }
            }

            // Ghi bản ghi mượn + hạn trả; đồng thời trừ tồn
            try (PreparedStatement ins = c.prepareStatement("""
                         INSERT INTO borrow_records(user_id, book_id, due_date)
                         VALUES (?, ?, NOW() + INTERVAL 14 DAY)
                    """);
                 PreparedStatement dec = c.prepareStatement(
                         "UPDATE books SET quantity = quantity - 1 WHERE id=?")) {

                ins.setString(1, userId);
                ins.setInt(2, bookId);
                ins.executeUpdate();

                dec.setInt(1, bookId);
                dec.executeUpdate();
            }

            c.commit();
        }
    }

    // Trả theo book_id (cập nhật bản ghi chưa return gần nhất)
    public void returnBookByBookId(String userId, int bookId) throws SQLException {
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);

            int updated;
            try (PreparedStatement up = c.prepareStatement("""
                    UPDATE borrow_records
                       SET return_date = NOW()
                     WHERE user_id = ? AND book_id = ? AND return_date IS NULL
                     ORDER BY borrow_date DESC
                     LIMIT 1
                """)) {
                up.setString(1, userId);
                up.setInt(2, bookId);
                updated = up.executeUpdate();
            }

            if (updated == 0) {
                c.rollback();
                throw new SQLException("Already returned.");
            }

            try (PreparedStatement inc = c.prepareStatement(
                    "UPDATE books SET quantity = quantity + 1 WHERE id=?")) {
                inc.setInt(1, bookId);
                inc.executeUpdate();
            }

            c.commit();
        }
    }

    // Danh sách sách đang mượn (ưu tiên quá hạn)
    public List<Book> listActiveBorrowedBooks(String userId) throws SQLException {
        String sql = """
            SELECT b.id AS book_id, b.isbn, b.title, b.authors, b.category, b.quantity, b.thumbnail_link,
                   br.borrow_date, br.due_date
            FROM borrow_records br
            JOIN books b ON b.id = br.book_id
            WHERE br.user_id = ? AND br.return_date IS NULL
            ORDER BY (NOW() > br.due_date) DESC, br.due_date ASC, br.borrow_date DESC
        """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Book> list = new ArrayList<>();
                while (rs.next()) {
                    Book b = new Book(
                            rs.getString("title"),
                            rs.getString("authors"),
                            rs.getString("category"),
                            rs.getString("isbn"),
                            rs.getInt("quantity"),
                            rs.getString("thumbnail_link")
                    );
                    b.setId(String.valueOf(rs.getInt("book_id")));
                    list.add(b);
                }
                return list;
            }
        }
    }

    // Đổi mật khẩu (user tự đổi)
    public boolean changePassword(String userId, String oldRaw, String newRaw) throws SQLException {
        String select = "SELECT password_hash, password_salt FROM users WHERE user_id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(select)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                String oldHash = rs.getString("password_hash");
                String oldSalt = rs.getString("password_salt");
                if (oldHash != null && oldSalt != null) {
                    if (!PasswordUtil.verify(oldRaw == null ? "" : oldRaw, oldSalt, oldHash)) return false;
                }
            }
        }
        String newSalt = PasswordUtil.generateSalt();
        String newHash = PasswordUtil.hashPassword(newRaw == null ? "" : newRaw, newSalt);
        String update = "UPDATE users SET password_hash=?, password_salt=? WHERE user_id=?";
        try (Connection c2 = DatabaseManager.getConnection();
             PreparedStatement up = c2.prepareStatement(update)) {
            up.setString(1, newHash);
            up.setString(2, newSalt);
            up.setString(3, userId);
            return up.executeUpdate() > 0;
        }
    }
    public User authenticate(String userId, String rawPassword) throws SQLException {
        String sql = "SELECT user_id, name, role, password_hash, password_salt FROM users WHERE user_id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String hash = rs.getString("password_hash");
                String salt = rs.getString("password_salt");
                if (hash == null || salt == null) return null;
                boolean ok = PasswordUtil.verify(rawPassword == null ? "" : rawPassword, salt, hash);
                if (!ok) return null;
                return new User(rs.getString("user_id"), rs.getString("name"), rs.getString("role"));
            }
        }
    }
    /* ====== (Optional) Admin-only: set/reset password without old ====== */
    public boolean setPasswordDirect(String userId, String newRaw) throws SQLException {
        String newSalt = PasswordUtil.generateSalt();
        String newHash = PasswordUtil.hashPassword(newRaw == null ? "" : newRaw, newSalt);
        String update = "UPDATE users SET password_hash=?, password_salt=? WHERE user_id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(update)) {
            ps.setString(1, newHash);
            ps.setString(2, newSalt);
            ps.setString(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean adminSetPassword(String userId, String newRaw) throws SQLException {
        String newSalt = PasswordUtil.generateSalt();
        String newHash = PasswordUtil.hashPassword(newRaw == null ? "" : newRaw, newSalt);

        // Chặn đổi mật khẩu cho tài khoản ADMIN
        String sql = "UPDATE users " +
                "SET password_hash = ?, password_salt = ? " +
                "WHERE user_id = ? AND UPPER(role) <> 'ADMIN'";

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, newSalt);
            ps.setString(3, userId);
            return ps.executeUpdate() > 0;  // true nếu cập nhật thành công
        }
    }

    // Đếm số bản ghi đang mượn (chưa trả)
    public int countActiveBorrowing(String userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM borrow_records WHERE user_id = ? AND return_date IS NULL";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // Lấy danh sách "đã đọc gần đây" (dạng Map thô)
    public List<Map<String, Object>> getRecentlyRead(String userId, int limit) throws SQLException {
        String sql = """
            SELECT b.id, b.title, b.authors, b.category, br.return_date
            FROM borrow_records br
            JOIN books b ON b.id = br.book_id
            WHERE br.user_id = ? AND br.return_date IS NOT NULL
            ORDER BY br.return_date DESC
            LIMIT ?
        """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> out = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getInt("id"));
                    m.put("title", rs.getString("title"));
                    m.put("authors", rs.getString("authors"));
                    m.put("category", rs.getString("category"));
                    m.put("return_date", rs.getTimestamp("return_date"));
                    out.add(m);
                }
                return out;
            }
        }
    }

    // Lấy danh sách "đã đọc gần đây" (trả về Book)
    public List<Book> getRecentlyReadBooks(String userId, int limit) throws SQLException {
        String sql = """
            SELECT b.id, b.isbn, b.title, b.authors, b.category, b.quantity, b.thumbnail_link
            FROM borrow_records br
            JOIN books b ON b.id = br.book_id
            WHERE br.user_id = ? AND br.return_date IS NOT NULL
            ORDER BY br.return_date DESC
            LIMIT ?
        """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Book> out = new ArrayList<>();
                while (rs.next()) {
                    Book b = new Book(
                            rs.getString("title"),
                            rs.getString("authors"),
                            rs.getString("category"),
                            rs.getString("isbn"),
                            rs.getInt("quantity"),
                            rs.getString("thumbnail_link")
                    );
                    b.setId(String.valueOf(rs.getInt("id")));
                    out.add(b);
                }
                return out;
            }
        }
    }
}
