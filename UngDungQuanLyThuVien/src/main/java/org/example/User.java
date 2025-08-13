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

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<BorrowRecord> getBorrowRecord() {
        return borrowRecord;
    }

    @Override
    public List<LibraryItem> getBorrowedItems() {
        List<LibraryItem> items = new ArrayList<>();
        for (BorrowRecord record : borrowRecord) {
            items.add(record.getItem());
        }
        return items;
    }

    @Override
    public List<LibraryItem> getBorrowedBooks() {
        List<LibraryItem> books = new ArrayList<>();
        for (BorrowRecord record : borrowRecord) {
            if (record.getItem() instanceof Book) {
                books.add(record.getItem());
            }
        }
        return books;
    }

    @Override
    public void borrowItem(LibraryItem item) {
        if (item instanceof Book) {
            Book book = (Book) item;
            if (book.getQuantity() > 0) {
                borrowRecord.add(new BorrowRecord(item, new Date()));
                book.setQuantity(book.getQuantity() - 1);
            }
        } else {
            // Mượn tài liệu khác (nếu có)
            borrowRecord.add(new BorrowRecord(item, new Date()));
        }
    }

    @Override
    public void returnItem(LibraryItem item) {
        Iterator<BorrowRecord> iterator = borrowRecord.iterator();
        while (iterator.hasNext()) {
            BorrowRecord record = iterator.next();
            if (record.getItem().equals(item)) {
                iterator.remove();
                if (item instanceof Book) {
                    Book book = (Book) item;
                    book.setQuantity(book.getQuantity() + 1);
                }
                break;
            }
        }
    }

    @Override
    public String toString() {
        return userId + " - " + name;
    }
}
