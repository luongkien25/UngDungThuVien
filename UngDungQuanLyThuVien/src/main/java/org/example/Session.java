package org.example;

public class Session {
    private static LibraryUser currentUser;

    public static void setCurrentUser(LibraryUser user) {
        currentUser = user;
    }

    public static LibraryUser getCurrentUser() {
        return currentUser;
    }

    public static void logout() {
        currentUser = null;
    }
}


