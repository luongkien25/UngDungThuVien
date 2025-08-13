package org.example;

public interface LibraryItem {
    String getId();
    void setId(String id);
    String getTitle();
    void setTitle(String title);
    String getDescription();
    void setDescription(String description);
    String getIsbn();
    void setIsbn(String isbn);
    int getQuantity();
    void setQuantity(int quantity);
    String toString();

    Object getAuthors();
}