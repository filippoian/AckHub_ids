package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.TeamController;

public class InviteUsersCommand implements Command {
    private final TeamController teamController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public InviteUsersCommand(TeamController teamController, SessionContext sessionContext, InputHelper inputHelper) {
        this.teamController = teamController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    public String name() { return "invite-users"; }

    public void execute() {
        try {
            long userId = sessionContext.getCurrentUserId().orElseThrow(() -> new IllegalStateException("Login utente richiesto"));
            Long teamId = teamController.getTeamIdOfUser(userId);
            if (teamId == null) throw new IllegalStateException("Non appartieni a un team");
            var ids = java.util.Arrays.stream(inputHelper.readNonBlank("ID destinatari separati da virgola").split(","))
                    .map(String::trim).map(Long::valueOf).toList();
            var invitations = teamController.inviteUsers(userId, teamId, ids);
            invitations.forEach(i -> System.out.println("Invito " + i.getInvitationId() + " per utente " + i.getInvitedUserId()));
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore: " + ex.getMessage());
        }
    }
}
