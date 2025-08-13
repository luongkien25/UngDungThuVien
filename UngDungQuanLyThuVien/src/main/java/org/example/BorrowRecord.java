package org.example;

import java.util.Date;

public class BorrowRecord {
    private long id;
    private Book book;
    private Date borrowDate;
    private Date dueDate;     // mới
    private Date returnDate;  // có thể null

    public BorrowRecord(long id, Book book, Date borrowDate, Date dueDate, Date returnDate) {
        this.id = id;
        this.book = book;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }

    public Date getBorrowDate() { return borrowDate; }
    public void setBorrowDate(Date borrowDate) { this.borrowDate = borrowDate; }

    public Date getReturnDate() { return returnDate; }
    public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }
}