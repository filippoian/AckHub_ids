package it.unicam.hackhub.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "violation_reports")
public class ViolationReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long reportId;
    private long hackathonId;
    private long teamId;
    private long mentorStaffId;
    private String description;
    private LocalDateTime createdAt;
    // Null finche' l'organizer non decide (REJECTED o TEAM_EXPELLED).
    private String decision;
    private int penalty;
    private boolean penaltyEvaluated;
    private Long managedByStaffId;
    private Long penaltyJudgeStaffId;

    public ViolationReport() {
    }

    // Crea una segnalazione aperta da mentor su un team registrato.
    public ViolationReport(long reportId, long hackathonId, long teamId, long mentorStaffId, String description, LocalDateTime createdAt, String decision) {
        if (reportId < 0) {
            throw new IllegalArgumentException("Report id non valido");
        }
        if (hackathonId <= 0) {
            throw new IllegalArgumentException("Hackathon id non valido");
        }
        if (teamId <= 0) {
            throw new IllegalArgumentException("Team id non valido");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Descrizione non valida");
        }
        this.reportId = reportId;
        this.hackathonId = hackathonId;
        this.teamId = teamId;
        this.mentorStaffId = mentorStaffId;
        this.description = description.trim();
        this.createdAt = createdAt;
        this.decision = decision;
    }

    public long getReportId() { return reportId; }
    public void setReportId(long reportId) { this.reportId = reportId; }
    public long getHackathonId() { return hackathonId; }
    public void setHackathonId(long hackathonId) { this.hackathonId = hackathonId; }
    public long getTeamId() { return teamId; }
    public void setTeamId(long teamId) { this.teamId = teamId; }
    public long getMentorStaffId() { return mentorStaffId; }
    public void setMentorStaffId(long mentorStaffId) { this.mentorStaffId = mentorStaffId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public int getPenalty() { return penalty; }
    public void setPenalty(int penalty) {
        if (penalty < 0 || penalty > 30) throw new IllegalArgumentException("Penalità fuori intervallo 0–30");
        this.penalty = penalty;
    }
    public boolean isPenaltyEvaluated() { return penaltyEvaluated; }
    public void setPenaltyEvaluated(boolean value) { this.penaltyEvaluated = value; }
    public Long getManagedByStaffId() { return managedByStaffId; }
    public void setManagedByStaffId(Long value) { this.managedByStaffId = value; }
    public Long getPenaltyJudgeStaffId() { return penaltyJudgeStaffId; }
    public void setPenaltyJudgeStaffId(Long value) { this.penaltyJudgeStaffId = value; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
}
