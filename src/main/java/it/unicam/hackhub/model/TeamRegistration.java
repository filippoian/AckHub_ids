package it.unicam.hackhub.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "team_registrations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"teamId"})
)
public class TeamRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long registrationId;
    private long teamId;
    private long hackathonId;
    private LocalDateTime registeredAt;
    // Quando diventa true, il team viene escluso dai flussi gara.
    private boolean expelled;

    @jakarta.persistence.ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @jakarta.persistence.CollectionTable(name = "registration_participants",
            joinColumns = @jakarta.persistence.JoinColumn(name = "registration_id"))
    @jakarta.persistence.Column(name = "user_id")
    private java.util.List<Long> participantUserIds = new java.util.ArrayList<>();

    public void freezeParticipants(java.util.List<Long> userIds) {
        if (!participantUserIds.isEmpty()) throw new IllegalStateException("Partecipanti già conservati");
        if (userIds == null || userIds.stream().anyMatch(id -> id == null || id <= 0)
                || userIds.stream().distinct().count() != userIds.size()) {
            throw new IllegalArgumentException("Partecipanti non validi");
        }
        participantUserIds = new java.util.ArrayList<>(userIds);
    }

    public java.util.List<Long> getParticipantUserIds() { return java.util.List.copyOf(participantUserIds); }

    public TeamRegistration() {
    }

    // Collega team e hackathon nel momento dell'iscrizione.
    public TeamRegistration(long registrationId, long teamId, long hackathonId, LocalDateTime registeredAt, boolean expelled) {
        this.registrationId = registrationId;
        this.teamId = teamId;
        this.hackathonId = hackathonId;
        this.registeredAt = registeredAt;
        this.expelled = expelled;
    }

    public long getRegistrationId() { return registrationId; }
    public void setRegistrationId(long registrationId) { this.registrationId = registrationId; }
    public long getTeamId() { return teamId; }
    public void setTeamId(long teamId) { this.teamId = teamId; }
    public long getHackathonId() { return hackathonId; }
    public void setHackathonId(long hackathonId) { this.hackathonId = hackathonId; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    public boolean isExpelled() { return expelled; }
    public void setExpelled(boolean expelled) { this.expelled = expelled; }
}
