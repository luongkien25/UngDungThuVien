package org.example;

public class UserBorrowStat extends User {
    public final int borrowCount;

    public UserBorrowStat(String userId, String name, String role, int borrowCount) {
        super(userId, name, role);
        this.borrowCount = borrowCount;
    }
}

