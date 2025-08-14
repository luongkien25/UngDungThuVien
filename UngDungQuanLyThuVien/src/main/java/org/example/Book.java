package org.example;

public class Book implements LibraryItem {
    private String title;
    private String authors;
    private String category;
    private String isbn;
    private int quantity;
    private String thumbnailLink;
    private String id;

    // ===== Cache phục vụ tìm kiếm (điền khi load) =====
    private String normTitle;
    private String normAuthors;
    private String normCategory;
    private String normIsbn;
    private java.util.List<String> titleTokens;
    private java.util.List<String> authorTokens;
    private java.util.List<String> categoryTokens;

    public Book(String title, String authors, String category, String isbn, int quantity, String thumbnailLink) {
        this.title = title;
        this.authors = authors;
        this.category = category;
        this.isbn = isbn;
        this.quantity = quantity;
        this.thumbnailLink = thumbnailLink;
    }

    // ====== getters/setters cơ bản ======
    public String getThumbnailLink() { return thumbnailLink; }
    public void setThumbnailLink(String thumbnailLink) { this.thumbnailLink = thumbnailLink; }
    public String getDescription() { return authors + " - " + category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }
    public String getCategory() { return category; }
    public void setDescription(String description) {}
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setCategory(String category) { this.category = category; }
    @Override
    public String toString() {
        String title = getTitle() != null ? getTitle() : "(No title)";
        String authors = getAuthors() != null ? getAuthors() : "(Unknown)";
        String isbnTxt = (getIsbn() == null || getIsbn().isBlank() || "N/A".equalsIgnoreCase(getIsbn()))
                ? "N/A" : getIsbn();
        String cat = (this.getCategory() != null) ? this.getCategory() : "";
        return title + " by " + authors +
                (cat.isBlank() ? "" : " (" + cat + ")") +
                " - ISBN: " + isbnTxt + " | Quantity: " + getQuantity();
    }
    @Override public String getId() { return id; }
    @Override public void setId(String id) { this.id = id; }

    // ====== equals ======
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        if (title != null ? !title.equals(book.title) : book.title != null) return false;
        if (authors != null ? !authors.equals(book.authors) : book.authors != null) return false;
        if (category != null ? !category.equals(book.category) : book.category != null) return false;
        return isbn != null ? isbn.equals(book.isbn) : book.isbn == null;
    }

    // ====== Cache getters/setters ======
    public String getNormTitle() { return normTitle; }
    public void setNormTitle(String normTitle) { this.normTitle = normTitle; }

    public String getNormAuthors() { return normAuthors; }
    public void setNormAuthors(String normAuthors) { this.normAuthors = normAuthors; }

    public String getNormCategory() { return normCategory; }
    public void setNormCategory(String normCategory) { this.normCategory = normCategory; }

    public String getNormIsbn() { return normIsbn; }
    public void setNormIsbn(String normIsbn) { this.normIsbn = normIsbn; }

    public java.util.List<String> getTitleTokens() {
        if (this.titleTokens == null) this.titleTokens = new java.util.ArrayList<>();
        return this.titleTokens;
    }
    public void setTitleTokens(java.util.List<String> titleTokens) { this.titleTokens = titleTokens; }

    public java.util.List<String> getAuthorTokens() { return authorTokens; }
    public void setAuthorTokens(java.util.List<String> authorTokens) { this.authorTokens = authorTokens; }

    public java.util.List<String> getCategoryTokens() { return categoryTokens; }
    public void setCategoryTokens(java.util.List<String> categoryTokens) { this.categoryTokens = categoryTokens; }
}