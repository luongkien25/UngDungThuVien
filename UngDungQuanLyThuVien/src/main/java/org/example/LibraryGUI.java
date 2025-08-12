package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class LibraryGUI {
    private final Library library = Library.getInstance();

    public LibraryGUI() {
        // Nạp dữ liệu từ DB vào bộ nhớ (DAO tự mở/đóng connection)
        library.loadBooksFromDatabase();
        library.loadUsersFromDatabase();

        JFrame frame = new JFrame("Library System Menu");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 600);
        frame.setLayout(new GridLayout(0, 1, 10, 10));

        JLabel titleLabel = new JLabel("Welcome to My Application!", JLabel.CENTER);
        frame.add(titleLabel);

        String[] buttonLabels = {
                "Add Document", "Remove Document", "Update Document", "Find Document", "Display Documents",
                "Add User", "Borrow Document", "Return Document", "Display User Info",
                "Search Google Books", "Display Borrow Records", "Exit"
        };

        for (int i = 0; i < buttonLabels.length; i++) {
            JButton button = new JButton("[" + i + "] " + buttonLabels[i]);
            int index = i;
            button.addActionListener(e -> handleMenuOption(index));
            frame.add(button);
        }

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void handleMenuOption(int option) {
        switch (option) {
            case 0 -> addDocument();
            case 1 -> removeDocument();
            case 2 -> updateDocument();
            case 3 -> findDocument();
            case 4 -> displayDocuments();
            case 5 -> addUser();
            case 6 -> borrowDocument();
            case 7 -> returnDocument();
            case 8 -> displayUserInfo();
            case 9 -> searchGoogleBooks();
            case 10 -> displayBorrowRecords();
            case 11 -> System.exit(0);
            default -> JOptionPane.showMessageDialog(null, "Invalid option");
        }
    }

    private void addDocument() {
        String title = JOptionPane.showInputDialog("Enter title:");
        if (title == null) return;
        String authors = JOptionPane.showInputDialog("Enter authors:");
        if (authors == null) return;
        String category = JOptionPane.showInputDialog("Enter category:");
        if (category == null) return;
        String isbn = JOptionPane.showInputDialog("Enter ISBN:");
        if (isbn == null) return;
        String quantityStr = JOptionPane.showInputDialog("Enter quantity:");
        if (quantityStr == null) return;

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid quantity.");
            return;
        }

        Book book = new Book(title, authors, category, isbn, quantity);

        try {
            new BookDAO().insertBook(book); // ghi DB
            library.addItem(book);          // cập nhật bộ nhớ
            JOptionPane.showMessageDialog(null, "Document added.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "DB error: " + e.getMessage());
        }
    }

        private void removeDocument() {
            String isbn = JOptionPane.showInputDialog("Enter ISBN to remove:");
            if (isbn == null) return;

            var item = library.findItemByIsbn(isbn);
            if (item == null) {
                JOptionPane.showMessageDialog(null, "Document not found.");
                return;
            }

            try {
                new BookDAO().removeBook(isbn); // xóa DB
                library.removeItem(item);       // xóa bộ nhớ
                JOptionPane.showMessageDialog(null, "Document removed.");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "This book is currently borrowed by another user.");
            }
        }

    private void updateDocument() {
        String isbn = JOptionPane.showInputDialog("Enter ISBN to update:");
        if (isbn == null) return;

        var item = library.findItemByIsbn(isbn);
        if (item == null) {
            JOptionPane.showMessageDialog(null, "Document not found.");
            return;
        }

        String newTitle = JOptionPane.showInputDialog("New title:", item.getTitle());
        if (newTitle == null) return;
        String newAuthors = JOptionPane.showInputDialog("New authors:", item.getDescription());
        if (newAuthors == null) return;
        String newCategory = JOptionPane.showInputDialog("New category:",
                (item instanceof Book b) ? b.getCategory() : "");
        if (newCategory == null) return;

        int quantity;
        try {
            quantity = Integer.parseInt(
                    JOptionPane.showInputDialog("New quantity:", item.getQuantity()).trim()
            );
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Invalid quantity.");
            return;
        }

        try {
            new BookDAO().updateBook(isbn, newTitle, newAuthors, newCategory, quantity);

            item.setTitle(newTitle);
            item.setDescription(newAuthors);
            item.setQuantity(quantity);
            if (item instanceof Book b) b.setCategory(newCategory);

            JOptionPane.showMessageDialog(null, "Document updated.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "DB error: " + e.getMessage());
        }
    }

    private void findDocument() {
        String title = JOptionPane.showInputDialog("Enter title to find:");
        var item = library.findItemByTitle(title);
        JOptionPane.showMessageDialog(null, item != null ? item.toString() : "Not found.");
    }

    private void displayDocuments() {
        StringBuilder sb = new StringBuilder();
        for (var item : library.getItems()) {
            sb.append(item.toString()).append("\n");
        }
        JOptionPane.showMessageDialog(null, !sb.isEmpty() ? sb.toString() : "No documents.");
    }

    private void addUser() {
        String userId = JOptionPane.showInputDialog("Enter user ID:");
        String name = JOptionPane.showInputDialog("Enter name:");
        if (userId == null || name == null) return;

        userId = userId.trim();
        name = name.trim();
        if (userId.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(null, "User ID and name cannot be empty.");
            return;
        }

        User user = new User(userId, name);
        try {
            new UserDAO().insertUser(user); // ghi DB
            library.addUser(user);          // cập nhật bộ nhớ
            JOptionPane.showMessageDialog(null, "User added.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "DB error: " + e.getMessage());
        }
    }

    private void borrowDocument() {
        List<LibraryUser> users = library.getUsers();
        if (users.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No users found.");
            return;
        }

        String[] userOptions = users.stream().map(LibraryUser::getUserId).toArray(String[]::new);
        String userId = (String) JOptionPane.showInputDialog(
                null, "Select user:", "Borrow Document",
                JOptionPane.PLAIN_MESSAGE, null, userOptions, userOptions[0]
        );
        if (userId == null) return;

        LibraryUser user = library.findUserById(userId);

        List<Book> availableBooks = library.getItems().stream()
                .filter(i -> i instanceof Book && i.getQuantity() > 0)
                .map(i -> (Book) i)
                .toList();

        if (availableBooks.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No available books to borrow.");
            return;
        }

        String[] bookOptions = availableBooks.stream().map(Book::toString).toArray(String[]::new);
        String selectedBookStr = (String) JOptionPane.showInputDialog(
                null, "Select book to borrow:", "Borrow Document",
                JOptionPane.PLAIN_MESSAGE, null, bookOptions, bookOptions[0]
        );
        if (selectedBookStr == null) return;

        Book selectedBook = availableBooks.stream()
                .filter(b -> b.toString().equals(selectedBookStr))
                .findFirst().orElse(null);

        if (user == null || selectedBook == null) {
            JOptionPane.showMessageDialog(null, "User or book not found.");
            return;
        }

        try {
            new UserDAO().borrowBook(user.getUserId(), selectedBook.getIsbn(), LocalDate.now()); // DB
            user.borrowBook(selectedBook); // bộ nhớ
            JOptionPane.showMessageDialog(null, "Book borrowed.\nRemaining quantity: " + selectedBook.getQuantity());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "DB error: " + e.getMessage());
        }
    }

    private void returnDocument() {
        String userId = JOptionPane.showInputDialog("User ID:");
        if (userId == null) return;
        String isbn = JOptionPane.showInputDialog("Enter ISBN to return:");
        if (isbn == null) return;

        var user = library.findUserById(userId);
        var item = library.findItemByIsbn(isbn);
        if (user == null || !(item instanceof Book book)) {
            JOptionPane.showMessageDialog(null, "User or book not found.");
            return;
        }

        try {
            new UserDAO().returnBook(userId, isbn); // DB
            user.returnBook(book);                  // bộ nhớ
            JOptionPane.showMessageDialog(null, "Book returned.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "DB error: " + e.getMessage());
        }
    }

    private void displayUserInfo() {
        String userId = JOptionPane.showInputDialog("Enter user ID:");
        var user = library.findUserById(userId);
        JOptionPane.showMessageDialog(null, user != null ? user.toString() : "User not found.");
    }

    private void searchGoogleBooks() {
        String keyword = JOptionPane.showInputDialog("Enter keyword to search Google Books:");
        if (keyword == null || keyword.trim().isEmpty()) return;

        try {
            List<Book> results = GoogleBooksAPI.searchBooks(keyword.trim());
            if (results.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No results found.");
                return;
            }

            String[] options = results.stream().map(Book::toString).toArray(String[]::new);
            String selection = (String) JOptionPane.showInputDialog(
                    null, "Select a book to add:", "Google Books Results",
                    JOptionPane.PLAIN_MESSAGE, null, options, options[0]
            );
            if (selection == null) return;

            Book selected = results.stream()
                    .filter(b -> b.toString().equals(selection))
                    .findFirst().orElse(null);
            if (selected == null) {
                JOptionPane.showMessageDialog(null, "Book not found.");
                return;
            }

            String quantityStr = JOptionPane.showInputDialog("Enter quantity to add:", "1");
            if (quantityStr == null) return;
            int quantity = Integer.parseInt(quantityStr.trim());
            if (quantity <= 0) throw new NumberFormatException("quantity <= 0");

            selected.setQuantity(quantity);

            // Gộp theo ISBN
            if (!"N/A".equals(selected.getIsbn())) {
                var existing = library.findItemByIsbn(selected.getIsbn());
                if (existing instanceof Book existingBook) {
                    existingBook.setQuantity(existingBook.getQuantity() + quantity);
                    JOptionPane.showMessageDialog(null,
                            "Book already exists. Increased quantity to " + existingBook.getQuantity() + ".");
                    return;
                }
            }

            // Gộp theo title + author khi ISBN = N/A
            for (var it : library.getItems()) {
                if (it instanceof Book b &&
                        "N/A".equals(b.getIsbn()) &&
                        b.getTitle().equalsIgnoreCase(selected.getTitle()) &&
                        b.getDescription().equalsIgnoreCase(selected.getDescription())) {
                    b.setQuantity(b.getQuantity() + quantity);
                    JOptionPane.showMessageDialog(null,
                            "Book matched by title + author. Quantity updated to " + b.getQuantity() + ".");
                    return;
                }
            }

            // Thêm mới
            new BookDAO().insertBook(selected); // DB
            library.addItem(selected);          // bộ nhớ
            JOptionPane.showMessageDialog(null, "Book added to library.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Invalid quantity.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "DB error: " + ex.getMessage());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
        }
    }

    private void displayBorrowRecords() {
        List<LibraryUser> users = library.getUsers();
        StringBuilder builder = new StringBuilder();

        for (LibraryUser user : users) {
            List<BorrowRecord> brs = user.getBorrowRecord();
            for (BorrowRecord br : brs) {
                Book book = br.getBook();
                Date borrowDate = br.getBorrowDate();
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(borrowDate);
                calendar.add(Calendar.DAY_OF_MONTH, 14);
                Date returnDate = calendar.getTime();
                Date now = new Date();

                builder.append("User: ").append(user.getName()).append("\n");
                builder.append("Book: ").append(book.toString()).append("\n");
                builder.append("Borrowed on: ").append(borrowDate).append("\n");
                builder.append("Returned on: ").append(returnDate).append("\n");
                if (returnDate.before(now)) builder.append("Overdue").append("\n");
                builder.append("-----------------------------\n");
            }
        }

        JOptionPane.showMessageDialog(null, builder.length() > 0 ? builder.toString() : "No documents.");
    }
}