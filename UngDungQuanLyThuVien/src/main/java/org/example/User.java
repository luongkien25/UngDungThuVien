package org.example;

import java.util.*;

public class User implements LibraryUser {
    private String userId;
    private String name;
    private List<BorrowRecord> borrowRecord = new ArrayList<>();

    public User(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<BorrowRecord> getBorrowRecord() {
        return borrowRecord;
    }
    public void borrowBook(Book book) {
        if (book.getQuantity() > 0) {
            borrowRecord.add(new BorrowRecord(book, new Date()));
            book.setQuantity(book.getQuantity() - 1);
        }
    }

    public void returnBook(Book book) {
        for (BorrowRecord br : borrowRecord) {
            if (br.getBook().equals(book)) {
                borrowRecord.remove(br);
                book.setQuantity(book.getQuantity() + 1);
                break;
            }
        }
    }

    public String toString() { return userId + " - " + name; }
}


