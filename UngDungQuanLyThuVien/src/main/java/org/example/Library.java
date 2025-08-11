package org.example;

import java.util.*;

public class Library {
    private static Library instance;
    private ArrayList<LibraryItem> items = new ArrayList<>();
    private ArrayList<LibraryUser> users = new ArrayList<>();

    private Library() {};

    public List<LibraryItem> getItems() {
        return items;
    }

    public List<LibraryUser> getUsers() {
        return users;
    }

    public static Library getInstance() {
        if (instance == null) {
            instance = new Library();
        }
        return instance;
    }

    public void addItem(LibraryItem item) { items.add(item); }
    public void removeItem(LibraryItem item) { items.remove(item); }

    public LibraryItem findItemByTitle(String title) {
        for (LibraryItem item : items) {
            if (item.getTitle().equalsIgnoreCase(title)) return item;
        }
        return null;
    }

    public LibraryItem findItemByIsbn(String isbn) {
        for (LibraryItem item : items) {
            if (item.getIsbn().equals(isbn)) return item;
        }
        return null;
    }

    public void addUser(LibraryUser user) { users.add(user); }
    public void removeUser(LibraryUser user) { users.remove(user); }

    public LibraryUser findUserById(String userId) {
        for (LibraryUser user : users) {
            if (user.getUserId().equals(userId)) return user;
        }
        return null;
    }
    public void borrowBook(LibraryUser user, Book book) {
        if (user != null && book != null && book.getQuantity() > 0) {
            user.borrowBook(book);
            book.setQuantity(book.getQuantity() - 1); // trừ số lượng
        } else {
            System.out.println("Không thể mượn sách: " + (book == null ? "book null" : "hết sách"));
        }
    }


    public LibraryItem findItemById(String id) {
        return null;
    }
}