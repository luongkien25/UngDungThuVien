/**package org.example;

import java.util.List;

public class Admin implements LibraryUser {
    private String UserID;
    private String UserName;
    private String AdminPassword;
    private String AdminEmail;
    private String AdminPhone;
    String getUserId() {
        return UserID;
    }
    void setUserId(String userId) {
        AdminID = userId;
    }

    String getName();
    void setName(String name);

    List<BorrowRecord> getBorrowRecord();

    // Tổng quát - tất cả loại tài liệu
    List<LibraryItem> getBorrowedItems();
    void borrowItem(LibraryItem item);
    void returnItem(LibraryItem item);

    // Nếu vẫn muốn giữ cho sách (optional)
    default void borrowBook(Book book) {
        borrowItem(book);
    }
    default void returnBook(Book book) {
        returnItem(book);
    }

    String toString();

    List<LibraryItem> getBorrowedBooks();
}*/
