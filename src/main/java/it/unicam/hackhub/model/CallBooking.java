package it.unicam.hackhub.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "call_bookings")
public class CallBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long callId;
    private long proposalId;
    private String meetingLink;
    private LocalDateTime createdAt;

    public CallBooking() {
    }

    public CallBooking(long callId, long proposalId, String meetingLink, LocalDateTime createdAt) {
        this.callId = callId;
        this.proposalId = proposalId;
        this.meetingLink = meetingLink;
        this.createdAt = createdAt;
    }

    public long getCallId() { return callId; }
    public void setCallId(long callId) { this.callId = callId; }
    public long getProposalId() { return proposalId; }
    public void setProposalId(long proposalId) { this.proposalId = proposalId; }
    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
