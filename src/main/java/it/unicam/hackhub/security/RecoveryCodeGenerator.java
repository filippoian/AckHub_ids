package it.unicam.hackhub.security;

@org.springframework.stereotype.Component
public class RecoveryCodeGenerator {
    public String generate() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
