package org.example;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {

    // Băm một mật khẩu
    public static String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    // Kiểm tra mật khẩu có khớp với bản băm không
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        return BCrypt.checkpw(plainTextPassword, hashedPassword);
    }
}