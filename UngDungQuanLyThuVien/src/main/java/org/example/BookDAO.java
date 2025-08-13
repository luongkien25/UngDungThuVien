package org.example;

import java.sql.*;
import java.util.ArrayList;

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
                    // UPSERT theo ISBN (UNIQUE)
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

                    // Dù insert hay update, lấy id + quantity hiện tại để đồng bộ RAM
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
                    // KHÔNG CÓ ISBN: thử khớp theo title + authors + category
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
    public java.util.List<java.util.Map<String,Object>> getBooksWithBorrowCount() throws SQLException {
        String sql = """
        SELECT b.id, b.isbn, b.title, b.authors, b.category, b.quantity,
               COUNT(br.id) AS borrow_count
        FROM books b
        LEFT JOIN borrow_records br ON br.book_id = b.id
        GROUP BY b.id
        ORDER BY borrow_count DESC, b.title
    """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            java.util.List<java.util.Map<String,Object>> out = new java.util.ArrayList<>();
            while (rs.next()) {
                java.util.Map<String,Object> row = new java.util.LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("isbn", rs.getString("isbn"));
                row.put("title", rs.getString("title"));
                row.put("authors", rs.getString("authors"));
                row.put("category", rs.getString("category"));
                row.put("quantity", rs.getInt("quantity"));
                row.put("borrow_count", rs.getLong("borrow_count"));
                out.add(row);
            }
            return out;
        }
    }

    public java.util.List<java.util.Map<String,Object>> getTopByCategoriesForUser(String userId) throws SQLException {
        String sql = """
        WITH my_cat AS (
          SELECT b.category, COUNT(*) c
          FROM borrow_records br
          JOIN books b ON b.id = br.book_id
          WHERE br.user_id = ?
          GROUP BY b.category
          ORDER BY c DESC
          LIMIT 3
        )
        SELECT b.*
        FROM books b
        JOIN my_cat mc ON mc.category = b.category
        LEFT JOIN borrow_records br_me
               ON br_me.user_id = ? AND br_me.book_id = b.id
        WHERE br_me.id IS NULL
        ORDER BY b.title
        LIMIT 20
    """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<java.util.Map<String,Object>> out = new java.util.ArrayList<>();
                java.sql.ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                while (rs.next()) {
                    java.util.Map<String,Object> row = new java.util.LinkedHashMap<>();
                    for (int i = 1; i <= n; i++) row.put(md.getColumnLabel(i), rs.getObject(i));
                    out.add(row);
                }
                return out;
            }
        }
    }
    public java.util.List<Book> suggestForUser(String userId, int limit) throws SQLException {
        String sql = """
        WITH my_cat AS (
          SELECT b.category, COUNT(*) c
          FROM borrow_records br
          JOIN books b ON b.id = br.book_id
          WHERE br.user_id = ?
          GROUP BY b.category
          ORDER BY c DESC
          LIMIT 3
        )
        SELECT b.id, b.isbn, b.title, b.authors, b.category, b.quantity, b.thumbnail_link
        FROM books b
        JOIN my_cat mc ON mc.category = b.category
        LEFT JOIN borrow_records br_me
               ON br_me.user_id = ? AND br_me.book_id = b.id
        WHERE br_me.id IS NULL
        ORDER BY b.title
        LIMIT ?
    """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
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

    public java.util.Map<Integer, Long> getBorrowCountsMap() throws SQLException {
        String sql = """
        SELECT b.id, COUNT(br.id) AS cnt
        FROM books b
        LEFT JOIN borrow_records br ON br.book_id = b.id
        GROUP BY b.id
    """;
        java.util.Map<Integer, Long> map = new java.util.HashMap<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getInt("id"), rs.getLong("cnt"));
            }
        }
        return map;
    }
}
