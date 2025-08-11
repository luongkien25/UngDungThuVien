package org.example;

import java.awt.print.Book;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class User implements LibraryUser {
    private String userId;
    private String name;
    private List<BorrowRecord> borrowRecord = new ArrayList();
    public User(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }
    public String getUserId() {return this.userId;}
    public void setUserId(String userId) {this.userId = userId;}
    public String getName() {return this.name;}
    public void setName(String name) {this.name = name;}
    public List<BorrowRecord> getBorrowRecord() {return this.borrowRecord;}

    public void borrowBook (Book book){
    if (book.getQuantity() > 0){
        this.borrowRecord.add(new BorrowRecord(book, new Date()));
        book.setQuantity(book.getQuantity() - 1);
    }

    }
    public void returnBook (Book book){
        for (BorrowRecord br : this.borrowRecord){
            if (br.getBook().equals(book)){
                this.borrowRecord.remove(br);
                book.setQuantity(book.getQuantity() + 1);
                break;
            }
    }

    }

    @Override
    public String toString() { return this.userId + " - " + this.name;}
}
