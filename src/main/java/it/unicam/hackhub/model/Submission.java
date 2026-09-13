package it.unicam.hackhub.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "submissions",
        // Una sola submission per ogni registrazione team.
        uniqueConstraints = @UniqueConstraint(columnNames = {"registrationId"})
)
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long submissionId;
    private long registrationId;
    private String content;
    private LocalDateTime submittedAt;
    private LocalDateTime lastUpdatedAt;


    public Submission() {
    }

    // submittedAt e lastUpdatedAt partono uguali al primo invio.
    public Submission(long submissionId, long registrationId, String content, LocalDateTime submittedAt, LocalDateTime lastUpdatedAt) {
        this.submissionId = submissionId;
        this.registrationId = registrationId;
        this.content = content;
        this.submittedAt = submittedAt;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public long getSubmissionId() { return submissionId; }
    public void setSubmissionId(long submissionId) { this.submissionId = submissionId; }
    public long getRegistrationId() { return registrationId; }
    public void setRegistrationId(long registrationId) { this.registrationId = registrationId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Submission other)) {
            return false;
        }
        return submissionId == other.submissionId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(submissionId);
    }
}




/* Appunti di progettazione conservati:
submission.calcolaFinale()

    if sottomissione.getAmmonizione() == True:
        ammonizione.evalutaion()

    while
*/
