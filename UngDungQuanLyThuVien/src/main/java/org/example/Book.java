package org.example;

public class Book implements LibraryItem {
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
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthors() {return authors;}
    public String getCategory() {return category;}
    public String getDescription() { return authors + " - " + category; }
    public void setDescription(String description) {}
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public String toString() {
        return title + " by " + authors + " (" + category + ") - ISBN: " + isbn + " | Quantity: " + quantity;
    }


    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }
}