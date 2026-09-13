package it.unicam.hackhub.web;

import it.unicam.hackhub.controller.AuthController;
import it.unicam.hackhub.controller.RegistrationResult;
import it.unicam.hackhub.controller.RecoveryResult;
import org.springframework.web.bind.annotation.RequestParam;
import it.unicam.hackhub.controller.auth.LoginOutcome;
import it.unicam.hackhub.controller.auth.LoginResult;
import it.unicam.hackhub.web.dto.LoginRequest;
import it.unicam.hackhub.web.dto.LoginResponse;
import it.unicam.hackhub.web.dto.RegisterRequest;
import it.unicam.hackhub.web.session.InMemorySessionStore;
import it.unicam.hackhub.web.session.SessionPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthWebController {
    private static final String SESSION_TOKEN_HEADER = "X-Session-Token";

    private final AuthController authController;
    private final InMemorySessionStore sessionStore;

    public AuthWebController(AuthController authController, InMemorySessionStore sessionStore) {
        this.authController = authController;
        this.sessionStore = sessionStore;
    }

    // Registra un nuovo utente partendo dai dati HTTP.
    @PostMapping("/register")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    @org.springframework.transaction.annotation.Transactional
    public RegistrationResult register(@RequestBody RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Username e password obbligatori");
        }

        String username = trimToNull(request.username());
        String password = request.password();
        if (username == null || password == null) {
            throw new IllegalArgumentException("Username e password obbligatori");
        }

        if (request.email() != null && !request.email().isBlank()
                && !request.email().trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Email non valida");
        RegistrationResult result = authController.registerUser(username, password);
        if (request.email() == null || request.email().isBlank()) return result;
        return new RegistrationResult(authController.updateProfile(result.profile().userId(), username,
                request.email(), password, null), result.recoveryCode());
    }

    @PostMapping(value = "/recover", consumes = "application/x-www-form-urlencoded")
    public RecoveryResult recoverCredentials(@RequestParam String userName, @RequestParam String recoveryCode,
                                              @RequestParam String newPassword) {
        return authController.recoverCredentials(userName, recoveryCode, newPassword);
    }

    // Autentica USER/STAFF e crea la sessione.
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Credenziali non valide");
        }

        String loginType = normalizeLoginType(request.type());
        String identifier = trimToNull(request.identifier());
        String password = request.password();
        if (identifier == null || password == null) {
            throw new IllegalArgumentException("Credenziali non valide");
        }

        LoginResult loginResult = authController.login(loginType, identifier, password);
        Long principalId = loginResult.principalId();

        if (loginResult.outcome() == LoginOutcome.USER_AUTHENTICATED && principalId != null) {
            // La sessione nasce qui: dal dominio passiamo a token HTTP.
            SessionPrincipal principal = new SessionPrincipal(SessionPrincipal.ProfileType.USER, principalId);
            String token = sessionStore.create(principal);
            return new LoginResponse(token, principal.getType().name(), principal.getId());
        }

        if (loginResult.outcome() == LoginOutcome.STAFF_AUTHENTICATED && principalId != null) {
            SessionPrincipal principal = new SessionPrincipal(SessionPrincipal.ProfileType.STAFF, principalId);
            String token = sessionStore.create(principal);
            return new LoginResponse(token, principal.getType().name(), principal.getId());
        }

        throw new IllegalArgumentException("Credenziali non valide");
    }

    // Invalida la sessione corrente.
    @PostMapping("/logout")
    public Map<String, String> logout(
            @RequestHeader(value = SESSION_TOKEN_HEADER, required = false) String token) {
        sessionStore.invalidate(token);
        return Map.of("status", "ok");
    }

    // Uniforma il tipo login per il controller applicativo.
    private String normalizeLoginType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Tipo login non valido");
        }

        String normalized = type.trim().toUpperCase(Locale.ROOT);
        if (!"USER".equals(normalized) && !"STAFF".equals(normalized)) {
            throw new IllegalArgumentException("Tipo login non valido");
        }
        return normalized;
    }

    // Converte stringhe vuote in null per semplificare i controlli.
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
