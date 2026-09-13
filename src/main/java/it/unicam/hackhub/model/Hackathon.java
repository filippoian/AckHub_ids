package it.unicam.hackhub.model;

import it.unicam.hackhub.model.enums.HackathonStatus;
import it.unicam.hackhub.model.state.ClosedState;
import it.unicam.hackhub.model.state.HackathonState;
import it.unicam.hackhub.model.state.RegistrationState;
import it.unicam.hackhub.model.state.ReviewState;
import it.unicam.hackhub.model.state.RunningState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@org.hibernate.annotations.SQLRestriction("deleted = false")
@Table(name = "hackathons")
public class Hackathon {
    @jakarta.persistence.Column(columnDefinition = "boolean default false")
    private boolean deleted = false;
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean value) { this.deleted = value; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long hackathonId;
    @Column(unique = true)
    private String hackathonName;
    private String regulation;
    private LocalDateTime registrationDeadline;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime submissionDeadline;
    private String location;
    private BigDecimal prizeAmount;
    private int maxTeamSize;
    @Enumerated(EnumType.STRING)
    private HackathonStatus status;
    private Long winnerTeamId;
    private boolean prizePaid;
    private String prizeReceiptId;

    public Hackathon() {
        this.prizePaid = false;
    }

    public Hackathon(long hackathonId,
                     String hackathonName,
                     String regulation,
                     LocalDateTime registrationDeadline,
                     LocalDateTime startDate,
                     LocalDateTime endDate,
                     LocalDateTime submissionDeadline,
                     String location,
                     BigDecimal prizeAmount,
                     int maxTeamSize,
                     HackathonStatus status,
                     Long winnerTeamId) {
        this.hackathonId = hackathonId;
        this.hackathonName = hackathonName;
        this.regulation = regulation;
        this.registrationDeadline = registrationDeadline;
        this.startDate = startDate;
        this.endDate = endDate;
        this.submissionDeadline = submissionDeadline;
        this.location = location;
        this.prizeAmount = prizeAmount;
        this.maxTeamSize = maxTeamSize;
        this.status = status;
        this.winnerTeamId = winnerTeamId;
        this.prizePaid = false;
    }

    public String getPrizeReceiptId() { return prizeReceiptId; }
    public void setPrizeReceiptId(String prizeReceiptId) { this.prizeReceiptId = prizeReceiptId; }

    public static Builder builder() {
        return new Builder();
    }

    public long getHackathonId() { return hackathonId; }
    public void setHackathonId(long hackathonId) { this.hackathonId = hackathonId; }
    public String getHackathonName() { return hackathonName; }
    public void setHackathonName(String hackathonName) { this.hackathonName = hackathonName; }
    public String getRegulation() { return regulation; }
    public void setRegulation(String regulation) { this.regulation = regulation; }
    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
    public void setRegistrationDeadline(LocalDateTime registrationDeadline) { this.registrationDeadline = registrationDeadline; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public LocalDateTime getSubmissionDeadline() { return submissionDeadline; }
    public void setSubmissionDeadline(LocalDateTime submissionDeadline) { this.submissionDeadline = submissionDeadline; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public BigDecimal getPrizeAmount() { return prizeAmount; }
    public void setPrizeAmount(BigDecimal prizeAmount) { this.prizeAmount = prizeAmount; }
    public int getMaxTeamSize() { return maxTeamSize; }
    public void setMaxTeamSize(int maxTeamSize) { this.maxTeamSize = maxTeamSize; }
    public HackathonStatus getStatus() { return status; }
    public void setStatus(HackathonStatus status) { this.status = status; }
    public Long getWinnerTeamId() { return winnerTeamId; }
    public void setWinnerTeamId(Long winnerTeamId) {
        if (winnerTeamId != null && winnerTeamId <= 0) {
            throw new IllegalArgumentException("Winner team id must be positive");
        }
        this.winnerTeamId = winnerTeamId;
    }
    public boolean isPrizePaid() { return prizePaid; }
    // Marca il premio come gia' pagato.
    public void markPrizePaid() { this.prizePaid = true; }

    // Traduce lo status corrente nello stato concreto della macchina a stati.
    public HackathonState getState() {
        if (status == null) {
            throw new IllegalStateException("Hackathon status is not set");
        }
        return switch (status) {
            case REGISTRATION -> new RegistrationState();
            case RUNNING -> new RunningState();
            case REVIEW -> new ReviewState();
            case CLOSED -> new ClosedState();
        };
    }

    // Verifica se siamo nella fase in cui sono aperte le registrazioni.
    public boolean canRegister() {
        return !deleted && getState().canRegister();
    }

    // Verifica se siamo nella fase in cui sono ammessi invii.
    public boolean canSubmit() {
        return !deleted && getState().canSubmit();
    }

    // Verifica se siamo nella fase in cui i judge possono valutare.
    public boolean canEvaluate() {
        return !deleted && getState().canEvaluate();
    }

    public boolean isClosed() {
        return getState().isClosed();
    }

    public void changeStatus(HackathonStatus target) {
        // La transizione passa sempre dallo stato corrente.
        this.status = getState().nextStatus(target);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Hackathon other)) {
            return false;
        }
        return hackathonId == other.hackathonId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hackathonId);
    }

    public static final class Builder {
        private long hackathonId;
        private String hackathonName;
        private String regulation;
        private LocalDateTime registrationDeadline;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private LocalDateTime submissionDeadline;
        private String location;
        private BigDecimal prizeAmount;
        private int maxTeamSize;
        private HackathonStatus status;
        private Long winnerTeamId;

        public Builder hackathonId(long hackathonId) {
            this.hackathonId = hackathonId;
            return this;
        }

        public Builder hackathonName(String hackathonName) {
            this.hackathonName = hackathonName;
            return this;
        }

        public Builder regulation(String regulation) {
            this.regulation = regulation;
            return this;
        }

        public Builder registrationDeadline(LocalDateTime registrationDeadline) {
            this.registrationDeadline = registrationDeadline;
            return this;
        }

        public Builder startDate(LocalDateTime startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(LocalDateTime endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder submissionDeadline(LocalDateTime submissionDeadline) {
            this.submissionDeadline = submissionDeadline;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder prizeAmount(BigDecimal prizeAmount) {
            this.prizeAmount = prizeAmount;
            return this;
        }

        public Builder maxTeamSize(int maxTeamSize) {
            this.maxTeamSize = maxTeamSize;
            return this;
        }

        public Builder status(HackathonStatus status) {
            this.status = status;
            return this;
        }

        public Builder winnerTeamId(Long winnerTeamId) {
            this.winnerTeamId = winnerTeamId;
            return this;
        }

        // Controlla le regole principali prima di costruire l'entity.
        public Hackathon build() {
            if (hackathonName == null || hackathonName.isBlank()) {
                throw new IllegalArgumentException("Hackathon name is required");
            }
            if (startDate == null || endDate == null || !startDate.isBefore(endDate)) {
                throw new IllegalArgumentException("Invalid hackathon dates");
            }
            if (maxTeamSize <= 0) {
                throw new IllegalArgumentException("Max team size must be positive");
            }
            if (prizeAmount != null && prizeAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Prize amount cannot be negative");
            }
            if (status == null) {
                throw new IllegalArgumentException("Hackathon status is required");
            }
            if (location == null || location.isBlank()) {
                throw new IllegalArgumentException("Location is required");
            }
            if (registrationDeadline != null && !registrationDeadline.isBefore(startDate)) {
                throw new IllegalArgumentException("Invalid registration deadline");
            }
            if (submissionDeadline != null && startDate != null && submissionDeadline.isBefore(startDate)) {
                throw new IllegalArgumentException("Invalid submission deadline");
            }
            if (submissionDeadline != null && submissionDeadline.isAfter(endDate)) {
                throw new IllegalArgumentException("Invalid submission deadline");
            }

            // Il nome viene salvato pulito per evitare differenze solo di spazi.
            Hackathon hackathon = new Hackathon(
                    hackathonId,
                    hackathonName.trim(),
                    regulation,
                    registrationDeadline,
                    startDate,
                    endDate,
                    submissionDeadline,
                    location,
                    prizeAmount,
                    maxTeamSize,
                    status,
                    winnerTeamId
            );
            hackathon.prizePaid = false;
            return hackathon;
        }
    }
}
