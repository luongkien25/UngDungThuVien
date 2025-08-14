package org.example;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public final class PasswordUtil {
    private static final SecureRandom RNG = new SecureRandom();

    private PasswordUtil() {}

    public static String generateSalt() {
        byte[] b = new byte[16];
        RNG.nextBytes(b);
        return Base64.getEncoder().encodeToString(b);
    }

    public static String hashPassword(String raw, String base64Salt) {
        if (raw == null) raw = "";
        byte[] salt = Base64.getDecoder().decode(base64Salt);
        byte[] pw = raw.getBytes(StandardCharsets.UTF_8);
        byte[] input = new byte[salt.length + pw.length];
        System.arraycopy(salt, 0, input, 0, salt.length);
        System.arraycopy(pw, 0, input, salt.length, pw.length);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(input);
            return Base64.getEncoder().encodeToString(dig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verify(String raw, String base64Salt, String base64Hash) {
        if (base64Salt == null || base64Hash == null) return false;
        String h = hashPassword(raw, base64Salt);
        return MessageDigest.isEqual(
                Base64.getDecoder().decode(h),
                Base64.getDecoder().decode(base64Hash)
        );
    }
}
