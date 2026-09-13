package it.unicam.hackhub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "teams")
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long teamId;
    @Column(unique = true)
    private String teamName;

    public Team() {
    }

    // Valida subito id e nome per evitare team inconsistenti.
    public Team(long teamId, String teamName) {
        if (teamId < 0) {
            throw new IllegalArgumentException("Team id non valido");
        }
        if (teamName == null || teamName.isBlank()) {
            throw new IllegalArgumentException("Team name non valido");
        }
        this.teamId = teamId;
        this.teamName = teamName.trim();
    }

    public long getTeamId() {
        return teamId;
    }

    public void setTeamId(long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Team other)) {
            return false;
        }
        return teamId == other.teamId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamId);
    }
}
