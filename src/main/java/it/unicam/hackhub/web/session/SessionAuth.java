package it.unicam.hackhub.web.session;

public final class SessionAuth {
    private SessionAuth() {
    }

    // Richiede una sessione USER valida e restituisce il suo id.
    public static long requireUserId(InMemorySessionStore store, String token) {
        SessionPrincipal principal = requirePrincipal(store, token);
        if (principal.getType() == SessionPrincipal.ProfileType.USER) {
            return principal.getId();
        }
        throw new ForbiddenException("Non autorizzato");
    }

    // Richiede una sessione STAFF valida e restituisce il suo id.
    public static long requireStaffId(InMemorySessionStore store, String token) {
        SessionPrincipal principal = requirePrincipal(store, token);
        if (principal.getType() == SessionPrincipal.ProfileType.STAFF) {
            return principal.getId();
        }
        throw new ForbiddenException("Non autorizzato");
    }

    private static SessionPrincipal requirePrincipal(InMemorySessionStore store, String token) {
        // Token mancante o scaduto: non c'e' una sessione valida.
        return store.find(token)
                .orElseThrow(() -> new UnauthenticatedException("Non autenticato"));
    }
}
