package org.example;

import java.sql.*;
import java.util.ArrayList;

public class BookDAO {

    public ArrayList<LibraryItem> getAllBooks() throws SQLException {
        String sql = "SELECT isbn, title, authors, category, quantity FROM books";
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
                        rs.getInt("quantity")
                );
                books.add(b);
            }
        }
        return books;
    }

    public void insertBook(Book book) throws SQLException {
        String sql = "INSERT INTO books (isbn, title, authors, category, quantity) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthors());
            ps.setString(4, book.getCategory());
            ps.setInt(5, book.getQuantity());
            ps.executeUpdate();
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
}