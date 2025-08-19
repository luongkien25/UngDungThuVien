package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.Map;
public class BookDAO {

    public ArrayList<LibraryItem> getAllBooks() throws SQLException {
        String sql = "SELECT id, isbn, title, authors, category, quantity, thumbnail_link FROM books";
        ArrayList<LibraryItem> books = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Book b = new Book(
                        rs.getString("title"),
                        rs.getString("authors"),
                        rs.getString("category"),
                        rs.getString("isbn"),
                        rs.getInt("quantity"),
                        rs.getString("thumbnail_link")
                );
                b.setId(rs.getString("id")); // luôn đọc String
                books.add(b);
            }
        }
        return books;
    }

    public Book insertBook(Book book) throws SQLException {
        String isbnParam = book.getIsbn();
        if ("N/A".equalsIgnoreCase(isbnParam) || (isbnParam != null && isbnParam.isBlank())) {
            isbnParam = null; // lưu null thay vì 'N/A'
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (isbnParam != null) {
                    String sql = """
                    INSERT INTO books (isbn, title, authors, category, quantity, thumbnail_link)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        title = VALUES(title),
                        authors = VALUES(authors),
                        category = VALUES(category),
                        quantity = quantity + VALUES(quantity),
                        thumbnail_link = VALUES(thumbnail_link)
                """;
                    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, isbnParam);
                        ps.setString(2, book.getTitle());
                        ps.setString(3, book.getAuthors());
                        ps.setString(4, book.getCategory());
                        ps.setInt(5, book.getQuantity());
                        ps.setString(6, book.getThumbnailLink());
                        ps.executeUpdate();

                        // Nếu là insert mới, lấy id tự sinh
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) {
                                book.setId(rs.getString(1));
                            }
                        }
                    }

                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT id, quantity FROM books WHERE isbn = ?")) {
                        ps.setString(1, isbnParam);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                book.setId(rs.getString("id"));
                                book.setQuantity(rs.getInt("quantity"));
                            }
                        }
                    }
                } else {
                    String selectSql = """
                    SELECT id, quantity FROM books
                     WHERE isbn IS NULL
                       AND title = ?
                       AND authors = ?
                       AND category = ?
                     LIMIT 1
                """;
                    try (PreparedStatement sel = conn.prepareStatement(selectSql)) {
                        sel.setString(1, book.getTitle());
                        sel.setString(2, book.getAuthors());
                        sel.setString(3, book.getCategory());
                        try (ResultSet rs = sel.executeQuery()) {
                            if (rs.next()) {
                                // Trùng → cộng quantity trong DB
                                long id = rs.getLong("id");
                                int currentQty = rs.getInt("quantity");
                                int newQty = currentQty + book.getQuantity();

                                try (PreparedStatement up = conn.prepareStatement(
                                        "UPDATE books SET quantity = ?, thumbnail_link = ? WHERE id = ?")) {
                                    up.setInt(1, newQty);
                                    up.setString(2, book.getThumbnailLink());
                                    up.setLong(3, id);
                                    up.executeUpdate();
                                }

                                book.setId(String.valueOf(id));
                                book.setQuantity(newQty);
                            } else {
                                // Không trùng → insert mới
                                String ins = "INSERT INTO books (isbn, title, authors, category, quantity, thumbnail_link) VALUES (NULL, ?, ?, ?, ?, ?)";
                                try (PreparedStatement ps = conn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
                                    ps.setString(1, book.getTitle());
                                    ps.setString(2, book.getAuthors());
                                    ps.setString(3, book.getCategory());
                                    ps.setInt(4, book.getQuantity());
                                    ps.setString(5, book.getThumbnailLink());
                                    ps.executeUpdate();
                                    try (ResultSet rs2 = ps.getGeneratedKeys()) {
                                        if (rs2.next()) book.setId(rs2.getString(1));
                                    }
                                }
                            }
                        }
                    }
                }

                conn.commit();
                return book;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void removeBook(String isbn) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM books WHERE isbn=?")) {
            ps.setString(1, isbn);
            ps.executeUpdate();
        }
    }

    public void updateBook(String isbn,
                           String newTitle,
                           String newAuthors,
                           String newCategory,
                           int newQuantity) throws SQLException {
        String sql = """
            
                UPDATE books
               SET title = ?, authors = ?, category = ?, quantity = ?
             WHERE isbn = ?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newTitle);
            ps.setString(2, newAuthors);
            ps.setString(3, newCategory);
            ps.setInt(4, newQuantity);
            ps.setString(5, isbn);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new SQLException("No book updated. ISBN not found: " + isbn);
        }
    }

    public java.util.List<Book> suggestForUser(String userId, int limit) throws SQLException {
        String sql = """
        WITH
        -- Mốc THÁNG hiện tại
        this_month AS (
          SELECT
            DATE_FORMAT(CURDATE(), '%Y-%m-01') AS d_start,
            DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), '%Y-%m-01') AS d_next
        ),
        -- Top 3 category user mượn trong THÁNG này
        my_cat AS (
          SELECT b.category, COUNT(*) c
          FROM borrow_records br
          JOIN books b ON b.id = br.book_id
          JOIN this_month tm
          WHERE br.user_id = ?
            AND br.borrow_date >= tm.d_start
            AND br.borrow_date <  tm.d_next
          GROUP BY b.category
          ORDER BY c DESC
          LIMIT 3
        ),
        -- Pool 1: sách theo sở thích trong THÁNG
        fav_books AS (
          SELECT b.*
          FROM books b
          WHERE b.category IN (SELECT category FROM my_cat)
        ),
        -- Pool 2: sách phổ biến toàn hệ thống trong THÁNG (hot)
        global_pop AS (
          SELECT b.*, COUNT(br.id) AS pop
          FROM books b
          LEFT JOIN borrow_records br ON br.book_id = b.id
          JOIN this_month tm
          WHERE (br.borrow_date IS NULL
                 OR (br.borrow_date >= tm.d_start AND br.borrow_date < tm.d_next))
          GROUP BY b.id
        ),
        -- Gộp 2 nguồn; src=1=fav, src=2=global
        unioned AS (
          SELECT fb.id, fb.isbn, fb.title, fb.authors, fb.category, fb.quantity, fb.thumbnail_link,
                 1 AS src, NULL AS pop
          FROM fav_books fb
          UNION ALL
          SELECT gp.id, gp.isbn, gp.title, gp.authors, gp.category, gp.quantity, gp.thumbnail_link,
                 2 AS src, gp.pop
          FROM global_pop gp
        ),
        -- Khử trùng theo id, ưu tiên src=1
        ranked AS (
          SELECT u.*,
                 ROW_NUMBER() OVER (PARTITION BY u.id ORDER BY u.src) AS rn
          FROM unioned u
        )
        SELECT r.id, r.isbn, r.title, r.authors, r.category, r.quantity, r.thumbnail_link
        FROM ranked r
        WHERE
          r.rn = 1                 -- khử trùng: giữ fav nếu trùng với global
          AND r.quantity > 0
          -- loại mọi sách user đã từng mượn (all-time); nếu muốn chỉ loại trong THÁNG, báo mình để đổi điều kiện thời gian
          AND NOT EXISTS (
                SELECT 1
                FROM borrow_records br_all
                WHERE br_all.user_id = ?
                  AND br_all.book_id = r.id
          )
        ORDER BY
          r.src,                                       -- fav trước global
          CASE WHEN r.src = 1 THEN r.title END ASC,    -- trong fav: sắp theo title (tùy bạn đổi tiêu chí)
          CASE WHEN r.src = 2 THEN r.pop   END DESC,   -- trong global: sắp theo độ hot trong THÁNG
          r.title ASC
        LIMIT ?;
    """;

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId); // my_cat (tháng)
            ps.setString(2, userId); // loại sách đã từng mượn (all-time)
            ps.setInt(3, limit);

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


    public void updateBookById(int id,
                               String newTitle,
                               String newAuthors,
                               String newCategory,
                               int newQuantity) throws SQLException {
        String sql = """
        UPDATE books
           SET title = ?, authors = ?, category = ?, quantity = ?
         WHERE id = ?
    """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newTitle);
            ps.setString(2, newAuthors);
            ps.setString(3, newCategory);
            ps.setInt(4, newQuantity);
            ps.setInt(5, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new SQLException("No book updated. ID not found: " + id);
        }
    }

    public void removeBookById(int id) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM books WHERE id = ?")) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new SQLException("No book removed. ID not found: " + id);
        }
    }

    public Map<String, Long> getBorrowCountsMap() throws SQLException {
        String sql = """
        SELECT b.id, COUNT(br.id) AS cnt
        FROM books b
        LEFT JOIN borrow_records br ON br.book_id = b.id
        GROUP BY b.id
    """;
        Map<String, Long> map = new java.util.HashMap<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("id"), rs.getLong("cnt"));
            }
        }
        return map;
    }
}
