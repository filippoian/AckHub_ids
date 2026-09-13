package it.unicam.hackhub.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "call_proposals")
public class CallProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long proposalId;
    private long requestId;
    private long mentorStaffId;
    private LocalDateTime proposedStart;
    private LocalDateTime proposedEnd;
    private boolean booked;

    public CallProposal() {
    }

    public CallProposal(long proposalId, long requestId, long mentorStaffId, LocalDateTime proposedStart, LocalDateTime proposedEnd, boolean booked) {
        this.proposalId = proposalId;
        this.requestId = requestId;
        this.mentorStaffId = mentorStaffId;
        this.proposedStart = proposedStart;
        this.proposedEnd = proposedEnd;
        this.booked = booked;
    }

    public long getProposalId() { return proposalId; }
    public void setProposalId(long proposalId) { this.proposalId = proposalId; }
    public long getRequestId() { return requestId; }
    public void setRequestId(long requestId) { this.requestId = requestId; }
    public long getMentorStaffId() { return mentorStaffId; }
    public void setMentorStaffId(long mentorStaffId) { this.mentorStaffId = mentorStaffId; }
    public LocalDateTime getProposedStart() { return proposedStart; }
    public void setProposedStart(LocalDateTime proposedStart) { this.proposedStart = proposedStart; }
    public LocalDateTime getProposedEnd() { return proposedEnd; }
    public void setProposedEnd(LocalDateTime proposedEnd) { this.proposedEnd = proposedEnd; }
    public boolean isBooked() { return booked; }
    public void setBooked(boolean booked) { this.booked = booked; }
}
