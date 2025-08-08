package org.example;

import java.util.Date;

public class BorrowRecord {
    private Book book;
    private Date borrowDate;

    public BorrowRecord(Book book, Date borrowDate) {
        this.book = book;
        this.borrowDate = borrowDate;
    }

    public Book getBook() { return book; }
    public Date getBorrowDate() { return borrowDate; }

}