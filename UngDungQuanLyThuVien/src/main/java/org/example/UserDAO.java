// UserDAO.java
package org.example;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT user_id, name FROM users";
        List<User> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new User(rs.getString("user_id"), rs.getString("name")));
            }
        }
        return list;
    }

    public List<BorrowRecord> getBorrowRecords(String userId) throws SQLException {
        String sql = """
    SELECT br.id,
           br.borrow_date,
           br.due_date,        -- thêm
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

                    java.sql.Timestamp dueTs = rs.getTimestamp("due_date");
                    Date due = (dueTs != null) ? new Date(dueTs.getTime()) : null;

                    java.sql.Timestamp retTs = rs.getTimestamp("return_date");
                    Date ret = (retTs != null) ? new Date(retTs.getTime()) : null;

                    list.add(new BorrowRecord(rs.getLong("id"), book, borrow, due, ret)); // <-- đủ 5 tham số
                }
            }
        }
        return list;
    }

    public void insertUser(User u) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO users(user_id, name) VALUES(?,?)")) {
            ps.setString(1, u.getUserId());
            ps.setString(2, u.getName());
            ps.executeUpdate();
        }
    }

    public void borrowBookByBookId(String userId, int bookId) throws SQLException {
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);

            // khóa tồn
            try (PreparedStatement lock = c.prepareStatement(
                    "SELECT quantity FROM books WHERE id=? FOR UPDATE")) {
                lock.setInt(1, bookId);
                try (ResultSet rs = lock.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Book not found");
                    if (rs.getInt(1) < 1) throw new SQLException("Out of stock");
                }
            }

            // ghi bản ghi mượn + hạn trả (nếu DB không hỗ trợ DEFAULT expr, luôn set ở đây)
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

    public void returnBookByBookId(String userId, int bookId) throws SQLException {
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);

            // chỉ cập nhật bản ghi đang mở
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
            if (updated == 0) { c.rollback(); throw new SQLException("Already returned."); }

            try (PreparedStatement inc = c.prepareStatement(
                    "UPDATE books SET quantity = quantity + 1 WHERE id=?")) {
                inc.setInt(1, bookId);
                inc.executeUpdate();
            }
            c.commit();
        }
    }

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

    public boolean changePassword(String userId, String oldPlain, String newPlain) throws SQLException {
        String sel = "SELECT password_hash FROM users WHERE user_id = ?";
        String upd = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sel)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                String current = rs.getString(1);
                if (!PasswordUtil.matches(oldPlain, current)) return false;
            }
            try (PreparedStatement up = c.prepareStatement(upd)) {
                up.setString(1, PasswordUtil.hash(newPlain));
                up.setString(2, userId);
                return up.executeUpdate() == 1;
            }
        }
    }

    public boolean adminSetPassword(String userId, String newPlain) throws SQLException {
        String upd = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement up = c.prepareStatement(upd)) {
            up.setString(1, PasswordUtil.hash(newPlain));
            up.setString(2, userId);
            return up.executeUpdate() == 1;
        }
    }

    public int countActiveBorrowing(String userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM borrow_records WHERE user_id = ? AND return_date IS NULL";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    public java.util.List<java.util.Map<String,Object>> getRecentlyRead(String userId, int limit) throws SQLException {
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
                java.util.List<java.util.Map<String,Object>> out = new java.util.ArrayList<>();
                while (rs.next()) {
                    java.util.Map<String,Object> m = new java.util.LinkedHashMap<>();
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
    public java.util.List<Book> getRecentlyReadBooks(String userId, int limit) throws SQLException {
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
                java.util.List<Book> out = new java.util.ArrayList<>();
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
