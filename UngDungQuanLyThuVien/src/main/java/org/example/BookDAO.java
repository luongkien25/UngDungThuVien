package org.example;
import java.util.*;
import java.sql.*;

public class BookDAO {
    private final Connection conn;

    public BookDAO(Connection conn) {
        this.conn = conn;
    }

    public ArrayList<LibraryItem> getAllBooks() {
        ArrayList<LibraryItem> books = new ArrayList<>();
        String sql = "SELECT * FROM Books";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Book book = new Book(
                        rs.getString("title"),
                        rs.getString("authors"),
                        rs.getString("category"),
                        rs.getString("isbn"),
                        rs.getInt("quantity")
                );
                books.add(book);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return books;
    }

    public void insertBook(Book book) throws SQLException {
        String sql = "INSERT INTO books (isbn, title, authors, category, quantity) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, book.getIsbn());
            stmt.setString(2, book.getTitle());
            stmt.setString(3, book.getAuthors());
            stmt.setString(4, book.getCategory());
            stmt.setInt(5, book.getQuantity());
            stmt.executeUpdate();
        }
    }

    public void removeBook(String isbn) throws SQLException {
        String sql = "DELETE FROM books WHERE isbn = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, isbn);
            stmt.executeUpdate();
        }
    }
    public Book findBookByIsbn(String isbn) throws SQLException {
        String sql = "SELECT * FROM books WHERE isbn = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, isbn);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Book(
                        rs.getString("title"),
                        rs.getString("authors"),
                        rs.getString("category"),
                        rs.getString("isbn"),
                        rs.getInt("quantity")
                );
            }
        }
        return null;
    }
}

