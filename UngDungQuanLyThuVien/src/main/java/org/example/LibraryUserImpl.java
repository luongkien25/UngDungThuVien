package org.example;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LibraryUserImpl implements LibraryUser {
    private String userId;
    private String name;
    private List<BorrowRecord> borrowRecords = new ArrayList<>();

    // Static user đang đăng nhập
    private static LibraryUserImpl currentUser;

    public LibraryUserImpl(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public static void setCurrentUser(LibraryUserImpl user) {
        currentUser = user;
    }

    public static LibraryUserImpl getCurrentUser() {
        return currentUser;
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
        return borrowRecords;
    }

    // Lấy danh sách tất cả LibraryItem đang mượn
    @Override
    public List<LibraryItem> getBorrowedItems() {
        List<LibraryItem> items = new ArrayList<>();
        for (BorrowRecord record : borrowRecords) {
            items.add(record.getItem());
        }
        return items;
    }

    @Override
    public List<LibraryItem> getBorrowedBooks() {
        List<LibraryItem> books = new ArrayList<>();
        for (BorrowRecord record : borrowRecords) {
            if (record.getItem() instanceof Book) {
                books.add(record.getItem()); // vẫn là LibraryItem
            }
        }
        return books;
    }

    // Mượn một item (có thể là Book, Magazine, DVD,...)
    @Override
    public void borrowItem(LibraryItem item) {
        borrowRecords.add(new BorrowRecord(item, new Date()));
    }

    // Trả một item
    @Override
    public void returnItem(LibraryItem item) {
        borrowRecords.removeIf(r -> r.getItem().equals(item));
    }

    @Override
    public String toString() {
        return "User: " + name + " (ID: " + userId + ")";
    }
}
