package it.unicam.hackhub.security;

public interface SessionInvalidator {
    void invalidateUser(long userId);
}
