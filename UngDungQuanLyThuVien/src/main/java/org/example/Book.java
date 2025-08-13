package org.example;

public class Book implements LibraryItem, Authorable {
    private String title;
    private String authors;
    private String category;
    private String isbn;
    private int quantity;
    private String thumbnailLink;
    private String id;


    public Book(String title, String authors, String category, String isbn, int quantity, String thumbnailLink) {
        this.title = title;
        this.authors = authors;
        this.category = category;
        this.isbn = isbn;
        this.quantity = quantity;
        this.thumbnailLink = thumbnailLink;
    }

    public String getThumbnailLink() { return thumbnailLink; }
    public void setThumbnailLink(String thumbnailLink) { this.thumbnailLink = thumbnailLink; }

    @Override
    public String getTitle() { return title; }
    @Override
    public void setTitle(String title) { this.title = title; }

    @Override
    public String getDescription() { return authors + " - " + category; }
    @Override
    public void setDescription(String description) {}

    @Override
    public String getIsbn() { return isbn; }
    @Override
    public void setIsbn(String isbn) { this.isbn = isbn; }

    @Override
    public int getQuantity() { return quantity; }
    @Override
    public void setQuantity(int quantity) { this.quantity = quantity; }

    @Override
    public String toString() {
        return title + " by " + authors + " (" + category + ") - ISBN: " + isbn + " | Quantity: " + quantity;
    }

    @Override
    public String getAuthors() { return authors; }

    @Override
    public void setAuthors(String authors) { // bổ sung
        this.authors = authors;
    }

    public String getCategory() { return category; }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }
}

