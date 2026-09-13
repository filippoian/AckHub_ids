package it.unicam.hackhub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "users")
public class User {
    @jakarta.persistence.Column(columnDefinition = "boolean default true")
    private boolean active = true;
    private String email;
    private String recoveryCodeHash;
    public boolean isActive() { return active; }
    public void setActive(boolean value) { this.active = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { this.email = value; }
    public String getRecoveryCodeHash() { return recoveryCodeHash; }
    public void setRecoveryCodeHash(String value) { this.recoveryCodeHash = value; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long userId;
    @Column(unique = true)
    private String userName;
    private String passwordHash;
    private Long teamId;

    public User() {
    }

    // teamId puo' restare null finche' l'utente non entra in un team.
    public User(long userId, String userName, String passwordHash, Long teamId) {
        if (userId < 0) {
            throw new IllegalArgumentException("User id non valido");
        }
        if (userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("Username non valido");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password non valida");
        }
        if (teamId != null && teamId <= 0) {
            throw new IllegalArgumentException("Team id non valido");
        }
        this.userId = userId;
        this.userName = userName.trim();
        this.passwordHash = passwordHash;
        this.teamId = teamId;
    }

    public User(long userId, String userName, String passwordHash) {
        this(userId, userName, passwordHash, null);
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof User other)) {
            return false;
        }
        return userId == other.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
