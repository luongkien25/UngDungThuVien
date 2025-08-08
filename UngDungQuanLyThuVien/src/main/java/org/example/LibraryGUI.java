package org.example;


import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.time.LocalDate;
import java.util.Date;
public class LibraryGUI {
    private final Library library = Library.getInstance();

    public LibraryGUI() {
        JFrame frame = new JFrame("Library System Menu");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 600);
        frame.setLayout(new GridLayout(0, 1, 10, 10));

        JLabel titleLabel = new JLabel("Welcome to My Application!", JLabel.CENTER);
        frame.add(titleLabel);

        String[] buttonLabels = {
                "Add Document",
                "Remove Document",
                "Update Document",
                "Find Document",
                "Display Documents",
                "Add User",
                "Borrow Document",
                "Return Document",
                "Display User Info",
                "Search Google Books",
                "Display Borrow Records",
                "Exit"
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
        String authors = JOptionPane.showInputDialog("Enter authors:");
        String category = JOptionPane.showInputDialog("Enter category:");
        String isbn = JOptionPane.showInputDialog("Enter ISBN:");
        int quantity = Integer.parseInt(JOptionPane.showInputDialog("Enter quantity:"));

        Book book = new Book(title, authors, category, isbn, quantity);
        library.addItem(book);
        JOptionPane.showMessageDialog(null, "Document added.");
    }

    private void removeDocument() {
        String isbn = JOptionPane.showInputDialog("Enter ISBN to remove:");
        var item = library.findItemByIsbn(isbn);
        if (item != null) {
            library.removeItem(item);
            JOptionPane.showMessageDialog(null, "Document removed.");
        } else {
            JOptionPane.showMessageDialog(null, "Document not found.");
        }
    }

    private void updateDocument() {
        String isbn = JOptionPane.showInputDialog("Enter ISBN to update:");
        var item = library.findItemByIsbn(isbn);
        if (item != null) {
            item.setTitle(JOptionPane.showInputDialog("New title:"));
            item.setDescription(JOptionPane.showInputDialog("New description:"));
            int quantity = Integer.parseInt(JOptionPane.showInputDialog("New quantity:"));
            item.setQuantity(quantity);
            JOptionPane.showMessageDialog(null, "Document updated.");
        } else {
            JOptionPane.showMessageDialog(null, "Document not found.");
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

        if (userId == null || userId.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "User ID and name cannot be empty.");
            return;
        }

        User user = new User(userId.trim(), name.trim());
        library.addUser(user);
        JOptionPane.showMessageDialog(null, "User added.");
    }

    private void borrowDocument() {
        // Chọn user từ danh sách
        List<LibraryUser> users = library.getUsers();
        if (users.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No users found.");
            return;
        }

        String[] userOptions = users.stream().map(LibraryUser::getUserId).toArray(String[]::new);
        String userId = (String) JOptionPane.showInputDialog(null, "Select user:",
                "Borrow Document", JOptionPane.PLAIN_MESSAGE, null, userOptions, userOptions[0]);

        if (userId == null) return; // user bấm Cancel

        LibraryUser user = library.findUserById(userId);

        // Chọn sách từ danh sách hiện có
        List<LibraryItem> items = library.getItems();
        List<Book> availableBooks = items.stream()
                .filter(i -> i instanceof Book && i.getQuantity() > 0)
                .map(i -> (Book) i)
                .toList();

        if (availableBooks.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No available books to borrow.");
            return;
        }

        String[] bookOptions = availableBooks.stream().map(Book::toString).toArray(String[]::new);
        String selectedBookStr = (String) JOptionPane.showInputDialog(null, "Select book to borrow:",
                "Borrow Document", JOptionPane.PLAIN_MESSAGE, null, bookOptions, bookOptions[0]);

        if (selectedBookStr == null) return; // user bấm Cancel

        // Tìm đúng Book từ toString()
        Book selectedBook = availableBooks.stream()
                .filter(b -> b.toString().equals(selectedBookStr))
                .findFirst().orElse(null);

        if (user != null && selectedBook != null) {
            user.borrowBook(selectedBook);
            JOptionPane.showMessageDialog(null, "Book borrowed.\nRemaining quantity: " + selectedBook.getQuantity());
        } else {
            JOptionPane.showMessageDialog(null, "User or book not found.");
        }
    }


    private void returnDocument() {
        String userId = JOptionPane.showInputDialog("User ID:");
        String isbn = JOptionPane.showInputDialog("Enter ISBN to return:");
        var user = library.findUserById(userId);
        var item = library.findItemByIsbn(isbn);
        if (user != null && item instanceof Book) {
            user.returnBook((Book) item);
            JOptionPane.showMessageDialog(null, "Book returned.");
        } else {
            JOptionPane.showMessageDialog(null, "User or book not found.");
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
                    null,
                    "Select a book to add:",
                    "Google Books Results",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (selection == null) return;

            // Tìm Book từ lựa chọn
            Book selected = results.stream()
                    .filter(b -> b.toString().equals(selection))
                    .findFirst()
                    .orElse(null);

            if (selected == null) {
                JOptionPane.showMessageDialog(null, "Book not found.");
                return;
            }

            // Nhập số lượng tùy chọn
            String quantityStr = JOptionPane.showInputDialog("Enter quantity to add:", "1");
            if (quantityStr == null || quantityStr.trim().isEmpty()) return;

            int quantity;
            try {
                quantity = Integer.parseInt(quantityStr.trim());
                if (quantity <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Invalid quantity.");
                return;
            }

            selected.setQuantity(quantity);

            // Gộp sách nếu ISBN có
            if (!selected.getIsbn().equals("N/A")) {
                LibraryItem existing = library.findItemByIsbn(selected.getIsbn());
                if (existing instanceof Book existingBook) {
                    existingBook.setQuantity(existingBook.getQuantity() + quantity);
                    JOptionPane.showMessageDialog(null, "Book already exists. Increased quantity to " + existingBook.getQuantity() + ".");
                    return;
                }
            }

            // Nếu ISBN = N/A → tìm trùng theo title + author
            for (LibraryItem item : library.getItems()) {
                if (item instanceof Book b &&
                        b.getIsbn().equals("N/A") &&
                        b.getTitle().equalsIgnoreCase(selected.getTitle()) &&
                        b.getDescription().equalsIgnoreCase(selected.getDescription())) {
                    b.setQuantity(b.getQuantity() + quantity);
                    JOptionPane.showMessageDialog(null, "Book matched by title + author. Quantity updated to " + b.getQuantity() + ".");
                    return;
                }
            }

            // Nếu không trùng, thêm mới
            library.addItem(selected);
            JOptionPane.showMessageDialog(null, "Book added to library.");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void displayBorrowRecords() {
        List<LibraryUser> users = library.getUsers();
        StringBuilder builder = new StringBuilder();

        System.out.println("DEBUG: Number of users = " + users.size());

        for (int i = 0; i < users.size(); i++) {
            LibraryUser user = users.get(i);
            List<BorrowRecord> brs = user.getBorrowRecord();

            System.out.println("DEBUG: User '" + user.getName() + "' has " + brs.size() + " borrow records");

            for (int j = 0; j < brs.size(); j++) {
                BorrowRecord br = brs.get(j);
                Book book = br.getBook();
                Date d = br.getBorrowDate();

                builder.append("User: ").append(user.getName()).append("\n");
                builder.append("Book: ").append(book.toString()).append("\n");
                builder.append("Borrowed on: ").append(d.toString()).append("\n");
                builder.append("-----------------------------\n");
            }
        }

        String message = builder.length() > 0 ? builder.toString() : "No documents.";
        JOptionPane.showMessageDialog(null, message);
    }


}
