package it.unicam.hackhub.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordHasher {
    private static final String HASH_ALGORITHM = "SHA-256";

    private PasswordHasher() {
    }

    // Converte una password in hash SHA-256.
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        return toHex(digest(plainPassword));
    }

    // Confronta password in chiaro e hash salvato.
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || plainPassword.isBlank() || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        return hashPassword(plainPassword).equals(storedHash);
    }

    private static byte[] digest(String plainPassword) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            messageDigest.update(plainPassword.getBytes(StandardCharsets.UTF_8));
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException ex) {
            // Se manca SHA-256 e' un problema di ambiente, non di input.
            throw new IllegalStateException("Missing hash algorithm: " + HASH_ALGORITHM, ex);
        }
    }

    private static String toHex(byte[] value) {
        StringBuilder builder = new StringBuilder(value.length * 2);
        for (byte b : value) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
