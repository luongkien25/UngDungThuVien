package org.example;

import java.awt.print.Book;
import java.util.List;

public interface LibraryUser {
    String getUserId();
    void setUserId(String userId);
    String getName();
    void setName(String name);
    List<BorrowRecord> getBorrowRecord();
    void borrowBook (Book var1);
    void returnBook (Book var1);
    String toString();
}
