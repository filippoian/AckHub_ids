package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.TeamController;

public class CancelInvitationCommand implements Command {
    private final TeamController teamController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public CancelInvitationCommand(TeamController teamController, SessionContext sessionContext, InputHelper inputHelper) {
        this.teamController = teamController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    public String name() { return "cancel-invitation"; }

    public void execute() {
        try {
            long userId = sessionContext.getCurrentUserId().orElseThrow(() -> new IllegalStateException("Login utente richiesto"));
            var invitations = teamController.viewSentInvites(userId);
            invitations.forEach(i -> System.out.println("Invito " + i.getInvitationId() + " per utente " + i.getInvitedUserId() + ": " + i.getStatus()));
            if (invitations.isEmpty()) return;
            long invitationId = inputHelper.readLong("ID invito da annullare");
            teamController.cancelInvitation(userId, invitationId);
            System.out.println("Invito annullato.");
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore: " + ex.getMessage());
        }
    }
}
