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
        name = "evaluations",
        // Una submission mantiene una sola valutazione corrente.
        uniqueConstraints = @UniqueConstraint(columnNames = {"submissionId"})
)
public class Evaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long evaluationId;
    private long submissionId;
    private int score;
    private int baseScore;
    private int penalty;
    private String comment;
    private LocalDateTime evaluatedAt;

    public Evaluation() {
    }

    // Rappresenta il voto finale del judge su quella submission.
    public Evaluation(long evaluationId, long submissionId, int score, String comment, LocalDateTime evaluatedAt) {
        this.evaluationId = evaluationId;
        this.submissionId = submissionId;
        setScore(score);
        this.comment = comment;
        this.evaluatedAt = evaluatedAt;
    }

    public long getEvaluationId() { return evaluationId; }
    public void setEvaluationId(long evaluationId) { this.evaluationId = evaluationId; }
    public long getSubmissionId() { return submissionId; }
    public void setSubmissionId(long submissionId) { this.submissionId = submissionId; }
    public int getScore() { return score; }
    // Compatibilità con i chiamanti esistenti: il voto inserito è sempre quello base.
    public void setScore(int score) { setBaseScore(score); }
    public int getBaseScore() { return baseScore; }
    public void setBaseScore(int baseScore) {
        if (baseScore < 0 || baseScore > 30) throw new IllegalArgumentException("Voto base fuori intervallo 0–30");
        this.baseScore = baseScore;
        recalculateScore();
    }
    public int getPenalty() { return penalty; }
    public void setPenalty(int penalty) {
        if (penalty < 0) throw new IllegalArgumentException("Penalità negativa");
        this.penalty = penalty;
        recalculateScore();
    }
    private void recalculateScore() { this.score = Math.max(0, baseScore - penalty); }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Evaluation other)) {
            return false;
        }
        return evaluationId == other.evaluationId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(evaluationId);
    }
}
