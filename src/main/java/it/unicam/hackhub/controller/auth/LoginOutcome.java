package it.unicam.hackhub.controller.auth;

public enum LoginOutcome {
    USER_AUTHENTICATED,
    STAFF_AUTHENTICATED,
    USER_NOT_FOUND,
    STAFF_NOT_FOUND,
    INVALID_PASSWORD,
    INVALID_INPUT,
    INVALID_LOGIN_TYPE
}
