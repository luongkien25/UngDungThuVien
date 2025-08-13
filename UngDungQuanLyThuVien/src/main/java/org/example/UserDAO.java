// UserDAO.java
package org.example;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    /**
     * Xác thực người dùng.
     * @param userId ID người dùng
     * @param plainPassword Mật khẩu dạng văn bản thuần
     * @return Đối tượng User nếu thành công, null nếu thất bại.
     */
    public User authenticateUser(String userId, String plainPassword) throws SQLException {
        String sql = "SELECT user_id, name, password FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hashedPassword = rs.getString("password");
                    if (PasswordUtils.checkPassword(plainPassword, hashedPassword)) {
                        return new User(rs.getString("user_id"), rs.getString("name"), hashedPassword);
                    }
                }
            }
        }
        return null; // Xác thực thất bại
    }

    /**
     * Thay đổi mật khẩu cho người dùng đã đăng nhập.
     * @param userId ID người dùng
     * @param newPassword Mật khẩu mới
     */
    public void changePassword(String userId, String newPassword) throws SQLException {
        String newHashedPassword = PasswordUtils.hashPassword(newPassword);
        String sql = "UPDATE users SET password = ? WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHashedPassword);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Reset mật khẩu (dành cho chức năng "Quên mật khẩu").
     * @param userId ID người dùng
     * @param newPassword Mật khẩu mới
     */
    public void resetPassword(String userId, String newPassword) throws SQLException {
        // Trong một ứng dụng thực tế, bạn nên xác minh danh tính người dùng
        // trước khi cho phép reset, ví dụ qua email.
        // Ở đây, chúng ta thực hiện trực tiếp để đơn giản hóa.
        changePassword(userId, newPassword);
    }


    public void insertUser(User u, String plainPassword) throws SQLException {
        String hashedPassword = PasswordUtils.hashPassword(plainPassword);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO users(user_id, name, password) VALUES(?, ?, ?)")) {
            ps.setString(1, u.getUserId());
            ps.setString(2, u.getName());
            ps.setString(3, hashedPassword); // Lưu mật khẩu đã băm
            ps.executeUpdate();
        }
    }

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
}
