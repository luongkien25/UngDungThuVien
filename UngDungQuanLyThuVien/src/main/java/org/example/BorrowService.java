package org.example;

import java.sql.*;

public class BorrowService {

    /** Mượn sách với giới hạn: tối đa 3 cuốn đang mượn (return_date IS NULL) */
    public boolean borrow(String userId, int bookId) throws SQLException {
        UserDAO udao = new UserDAO();
        if (udao.countActiveBorrowing(userId) >= 3) {
            return false; // đã đủ 3 cuốn
        }
        // kiểm tra tồn kho: còn cuốn để mượn?
        String availableSql = """
            SELECT b.quantity - COALESCE(x.active_cnt,0) AS available
            FROM books b
            LEFT JOIN (
              SELECT book_id, COUNT(*) active_cnt
              FROM borrow_records
              WHERE return_date IS NULL
              GROUP BY book_id
            ) x ON x.book_id = b.id
            WHERE b.id = ?
            """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(availableSql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt("available") <= 0) return false;
            }
        }
        // tạo bản ghi mượn
        String ins = "INSERT INTO borrow_records(user_id, book_id, borrow_date) VALUES (?, ?, NOW())";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(ins)) {
            ps.setString(1, userId);
            ps.setInt(2, bookId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Trả sách */
    public boolean giveBack(long borrowRecordId) throws SQLException {
        String upd = "UPDATE borrow_records SET return_date = NOW() WHERE id = ? AND return_date IS NULL";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(upd)) {
            ps.setLong(1, borrowRecordId);
            return ps.executeUpdate() == 1;
        }
    }
}