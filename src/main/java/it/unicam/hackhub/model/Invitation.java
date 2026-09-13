package it.unicam.hackhub.model;

import it.unicam.hackhub.model.enums.InvitationStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "invitations")
public class Invitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long invitationId;
    private long teamId;
    private long invitedUserId;
    @Enumerated(EnumType.STRING)
    // PENDING/ACCEPTED/DECLINED guida i flussi di gestione invito.
    private InvitationStatus status;

    public Invitation() {
    }

    // Crea l'invito inizialmente in stato pendente.
    public Invitation(long invitationId, long teamId, long invitedUserId, InvitationStatus status) {
        this.invitationId = invitationId;
        this.teamId = teamId;
        this.invitedUserId = invitedUserId;
        this.status = status;
    }

    public long getInvitationId() { return invitationId; }
    public void setInvitationId(long invitationId) { this.invitationId = invitationId; }
    public long getTeamId() { return teamId; }
    public void setTeamId(long teamId) { this.teamId = teamId; }
    public long getInvitedUserId() { return invitedUserId; }
    public void setInvitedUserId(long invitedUserId) { this.invitedUserId = invitedUserId; }
    public InvitationStatus getStatus() { return status; }
    public void setStatus(InvitationStatus status) { this.status = status; }
}
