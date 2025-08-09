package org.example;

public abstract class Book implements LibraryItem {
    private String title;
    private String authors;
    private String category;
    private String isbn;
    private int quantity;

    public Book(String title, String authors, String category, String isbn, int quantity){
        this.title = title;
        this.authors = authors;
        this.category = category;
        this.isbn = isbn;
        this.quantity = quantity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return authors + " - " + category;
    }

    public void setDescription(String description) {}

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String toString() {
        return title + " by " + authors + " (" + category + ") - ISBN: " + isbn + " | Quantity: " + quantity;
    }
}