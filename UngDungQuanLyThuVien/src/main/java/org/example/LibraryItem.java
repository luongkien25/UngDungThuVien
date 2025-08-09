package org.example;

public interface LibraryItem {
    String getTitle();
    void setTitle(String title);
    String getDescription();
    void setDescription(String description);
    String getIsbn();
    void setIsbn(String isbn);
    int getQuantity();
    void setQuantity(int quantity);
    String toString();
}
