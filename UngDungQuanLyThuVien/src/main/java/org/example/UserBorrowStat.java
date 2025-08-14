package org.example;

public class UserBorrowStat {
    public final String userId;
    public final String name;
    public final String role;
    public final int borrowCount;

    public UserBorrowStat(String userId, String name, String role, int borrowCount) {
        this.userId = userId;
        this.name = name;
        this.role = role;
        this.borrowCount = borrowCount;
    }
}

