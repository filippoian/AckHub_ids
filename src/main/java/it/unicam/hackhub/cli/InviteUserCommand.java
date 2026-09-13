package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.TeamController;
import it.unicam.hackhub.model.Invitation;

import java.util.Optional;

public class InviteUserCommand implements Command {
    private final TeamController teamController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public InviteUserCommand(TeamController teamController, SessionContext sessionContext, InputHelper inputHelper) {
        this.teamController = teamController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "invite-user";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isUserLoggedIn()) {
                System.out.println("Devi effettuare login USER per invitare utenti.");
                return;
            }

            Optional<Long> currentUserId = sessionContext.getCurrentUserId();
            if (currentUserId.isEmpty()) {
                System.out.println("Sessione utente non valida.");
                return;
            }

            Long teamId;
            teamId = teamController.getTeamIdOfUser(currentUserId.get());
            if (teamId == null) {
                System.out.println("Devi appartenere a un team per inviare inviti.");
                return;
            }

            long invitedUserId = inputHelper.readLong("Invited userId");
            Invitation invitation = teamController.inviteUser(currentUserId.get(), teamId, invitedUserId);
            System.out.println("Invito creato. invitationId=" + invitation.getInvitationId());
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException ex) {
            System.out.println("Errore invio invito: " + ex.getMessage());
        } catch (IllegalStateException ex) {
            System.out.println("Errore invio invito: " + ex.getMessage());
        }
    }
}
