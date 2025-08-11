package org.example;

import java.util.List;

public interface LibraryUser {
    String getUserId();
    void setUserId(String userId);
    String getName();
    void setName(String name);
    List<BorrowRecord> getBorrowRecord();
    void borrowBook(Book book);
    void returnBook(Book book);
    String toString();
}