package it.unicam.hackhub.web;

import it.unicam.hackhub.web.session.ForbiddenException;
import it.unicam.hackhub.web.session.UnauthenticatedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Sessione assente o token non valido.
    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthenticatedException(UnauthenticatedException ex) {
        String message = ex.getMessage() == null ? "Non autenticato" : ex.getMessage();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", message));
    }

    // Sessione valida ma permessi insufficienti.
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbiddenException(ForbiddenException ex) {
        String message = ex.getMessage() == null ? "Non autorizzato" : ex.getMessage();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", message));
    }

    // Input non valido passato dal client.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        String message = ex.getMessage() == null ? "Richiesta non valida" : ex.getMessage();
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    // Regola di dominio violata nello stato attuale.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException ex) {
        String message = ex.getMessage() == null ? "Operazione non valida" : ex.getMessage();
        return ResponseEntity.status(409).body(Map.of("error", message));
    }

    // Vincolo di persistenza violato, tipicamente su campi univoci.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Conflitto: dato già esistente"));
    }

    // JSON malformato o non coerente con il payload atteso.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "JSON non valido"));
    }
}
