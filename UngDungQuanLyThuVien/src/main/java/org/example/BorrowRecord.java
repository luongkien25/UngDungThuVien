package org.example;

import java.util.Date;

public class BorrowRecord {
    private LibraryItem item;
    private Date borrowDate;

    public BorrowRecord(LibraryItem item, Date borrowDate) {
        this.item = item;
        this.borrowDate = borrowDate;
    }

    public LibraryItem getItem() {
        return item;
    }

    public Date getBorrowDate() {
        return borrowDate;
    }
}
