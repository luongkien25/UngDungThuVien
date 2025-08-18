package org.example;

import java.util.List;

public interface LibraryUser {

    String getUserId();
    void setUserId(String userId);

    String getName();
    void setName(String name);

    List<BorrowRecord> getBorrowRecord();

    List<LibraryItem> getBorrowedItems();
    void borrowItem(LibraryItem item);
    void returnItem(LibraryItem item);

    default void borrowBook(Book book) {
        borrowItem(book);
    }
    default void returnBook(Book book) {
        returnItem(book);
    }

    String toString();

    List<LibraryItem> getBorrowedBooks();
}
