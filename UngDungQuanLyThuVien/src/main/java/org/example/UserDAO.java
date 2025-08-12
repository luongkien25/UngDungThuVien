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
            SELECT br.id, br.borrow_date,
                   b.isbn, b.title, b.authors, b.category, b.quantity
            FROM borrow_records br
            JOIN books b ON b.isbn = br.isbn
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
                    java.sql.Date d = rs.getDate("borrow_date");
                    list.add(new BorrowRecord(book, new java.util.Date(d.getTime())));
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

    public void borrowBook(String userId, String isbn, LocalDate borrowDate) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // khóa & kiểm tra tồn
                try (PreparedStatement check = conn.prepareStatement(
                        "SELECT quantity FROM books WHERE isbn=? FOR UPDATE")) {
                    check.setString(1, isbn);
                    try (ResultSet rs = check.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Book not found: " + isbn);
                        if (rs.getInt("quantity") <= 0) throw new SQLException("Out of stock for " + isbn);
                    }
                }
                // ghi mượn
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO borrow_records(user_id,isbn,borrow_date) VALUES(?,?,?)")) {
                    ins.setString(1, userId);
                    ins.setString(2, isbn);
                    ins.setDate(3, Date.valueOf(borrowDate));
                    ins.executeUpdate();
                }
                // trừ tồn
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE books SET quantity = quantity - 1 WHERE isbn=?")) {
                    upd.setString(1, isbn);
                    upd.executeUpdate();
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void returnBook(String userId, String isbn) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int deleted;
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM borrow_records WHERE id=(" +
                                " SELECT id FROM borrow_records WHERE user_id=? AND isbn=? " +
                                " ORDER BY borrow_date DESC, id DESC LIMIT 1)")) {
                    del.setString(1, userId);
                    del.setString(2, isbn);
                    deleted = del.executeUpdate();
                }
                if (deleted == 0) throw new SQLException("No borrow record to return.");

                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE books SET quantity = quantity + 1 WHERE isbn=?")) {
                    upd.setString(1, isbn);
                    upd.executeUpdate();
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
