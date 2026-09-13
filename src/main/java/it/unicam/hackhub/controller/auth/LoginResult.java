package it.unicam.hackhub.controller.auth;

public record LoginResult(LoginOutcome outcome, Long principalId, String message) {

    public static LoginResult of(LoginOutcome outcome, Long principalId, String message) {
        return new LoginResult(outcome, principalId, message);
    }

    public static LoginResult userAuthenticated(long userId) {
        return of(LoginOutcome.USER_AUTHENTICATED, userId, "User authenticated");
    }

    public static LoginResult staffAuthenticated(long staffId) {
        return of(LoginOutcome.STAFF_AUTHENTICATED, staffId, "Staff authenticated");
    }

    public static LoginResult userNotFound() {
        return of(LoginOutcome.USER_NOT_FOUND, null, "User not found");
    }

    public static LoginResult staffNotFound() {
        return of(LoginOutcome.STAFF_NOT_FOUND, null, "Staff not found");
    }

    public static LoginResult invalidPassword() {
        return of(LoginOutcome.INVALID_PASSWORD, null, "Invalid password");
    }

    public static LoginResult invalidInput() {
        return of(LoginOutcome.INVALID_INPUT, null, "Invalid input");
    }

    public static LoginResult invalidLoginType() {
        return of(LoginOutcome.INVALID_LOGIN_TYPE, null, "Invalid login type");
    }
}
