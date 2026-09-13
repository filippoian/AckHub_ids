package it.unicam.hackhub.web.session;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class InMemorySessionStore implements it.unicam.hackhub.security.SessionInvalidator {
    private final Map<String, SessionPrincipal> sessions = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void invalidateUser(long userId) {
        sessions.entrySet().removeIf(e -> e.getValue().getType() == SessionPrincipal.ProfileType.USER
                && e.getValue().getId() == userId);
    }

    // Crea un token nuovo e lo associa al principal autenticato.
    public String create(SessionPrincipal principal) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, principal);
        return token;
    }

    // Cerca il principal della sessione se il token e' valorizzato.
    public Optional<SessionPrincipal> find(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(token));
    }

    // Rimuove il token dalla session store.
    public void invalidate(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        sessions.remove(token);
    }
}
