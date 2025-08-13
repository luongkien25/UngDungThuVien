package org.example;

import java.util.*;

public class User implements LibraryUser {
    private String userId;
    private String name;
    private String password;
    private final List<BorrowRecord> borrowRecord = new ArrayList<>();

    public User(String userId, String name) { this.userId = userId; this.name = name; }
    public User(String userId, String name, String password) {
        this.userId = userId;
        this.name = name;
        this.password = password;
    }

    @Override public String getUserId() { return userId; }
    @Override public void setUserId(String userId) { this.userId = userId; }
    public String getPassword() { return password; } //
    public void setPassword(String password) { this.password = password; }
    @Override public String getName() { return name; }
    @Override public void setName(String name) { this.name = name; }
    @Override public List<BorrowRecord> getBorrowRecord(){return borrowRecord;}

    @Override
    public List<LibraryItem> getBorrowedItems() {
        List<LibraryItem> items = new ArrayList<>();
        for (BorrowRecord r : borrowRecord) {
            if (r.getBook() != null && r.getReturnDate() == null) { // chỉ bản ghi đang mượn
                items.add(r.getBook());
            }
        }
        return items;
    }

    @Override
    public List<LibraryItem> getBorrowedBooks() { return getBorrowedItems(); }

    /** MƯỢN: ghi DB trước (qua DAO), xong mới cập nhật RAM. */
    public void borrowBook(Book book, UserDAO dao) {
        if (book.getId() == null || book.getId().isBlank())
            throw new RuntimeException("Book has no DB id; cannot borrow.");
        try {
            int bookId = Integer.parseInt(book.getId());
            dao.borrowBookByBookId(this.userId, bookId);   // DB trước

            // RAM sau DB OK:
            Date borrow = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(borrow);
            cal.add(Calendar.DATE, 14);
            Date due = cal.getTime();

            this.borrowRecord.add(new BorrowRecord(0L, book, borrow, due, null)); // <-- 5 tham số
            book.setQuantity(book.getQuantity() - 1);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void returnBook(Book book, UserDAO dao) {
        if (book == null) return;
        if (book.getId() == null || book.getId().isBlank())
            throw new RuntimeException("Book has no DB id; cannot return.");

        try {
            dao.returnBookByBookId(this.userId, Integer.parseInt(book.getId()));

            for (ListIterator<BorrowRecord> it = borrowRecord.listIterator(); it.hasNext();) {
                BorrowRecord r = it.next();
                Book b = r.getBook();
                if (b != null && book.getId().equals(b.getId())) {
                    it.remove();
                    break;
                }
            }
            book.setQuantity(book.getQuantity() + 1);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override public void borrowItem(LibraryItem item) {
        if (item instanceof Book) borrowBook((Book)item, new UserDAO());
    }
    @Override public void returnItem(LibraryItem item) {
        if (item instanceof Book) returnBook((Book)item, new UserDAO());
    }

    @Override public String toString() { return userId + " - " + name; }

    private static Date addDays(Date d, int days) {
        Calendar c = Calendar.getInstance(); c.setTime(d); c.add(Calendar.DATE, days); return c.getTime();
    }
}