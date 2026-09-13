package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.TeamController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ViewInvitesCommand implements Command {
    private final TeamController teamController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public ViewInvitesCommand(TeamController teamController,
                              SessionContext sessionContext,
                              InputHelper inputHelper) {
        this.teamController = teamController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "view-invites";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isUserLoggedIn()) {
                System.out.println("Devi effettuare login USER per visualizzare gli inviti.");
                return;
            }

            Optional<Long> currentUserId = sessionContext.getCurrentUserId();
            if (currentUserId.isEmpty()) {
                System.out.println("Sessione utente non valida.");
                return;
            }

            List<TeamController.InvitationView> invitations = teamController.viewInvitesForUser(currentUserId.get());
            Set<Long> invitationIds = new LinkedHashSet<>();
            List<List<String>> rows = new ArrayList<>();
            for (TeamController.InvitationView invitation : invitations) {
                invitationIds.add(invitation.invitationId());
                rows.add(List.of(
                        String.valueOf(invitation.invitationId()),
                        String.valueOf(invitation.teamId()),
                        safe(invitation.teamName()),
                        String.valueOf(invitation.members()),
                        safe(invitation.hackathon()),
                        safe(invitation.status())
                ));
            }

            TablePrinter.print(
                    List.of("INVITATION_ID", "TEAM_ID", "TEAM_NAME", "MEMBERS", "HACKATHON", "STATUS"),
                    rows
            );

            if (invitations.isEmpty()) {
                return;
            }

            if (!askYesNo("Vuoi gestire un invito? (y/n)")) {
                return;
            }

            long selectedInvitationId = readInvitationId(invitationIds);
            String action = readAction();

            boolean success;
            if ("ACCEPT".equals(action)) {
                success = teamController.acceptInvitation(selectedInvitationId, currentUserId.get());
                System.out.println(success ? "Invito accettato." : "Impossibile accettare l'invito.");
            } else {
                success = teamController.declineInvitation(selectedInvitationId, currentUserId.get());
                System.out.println(success ? "Invito rifiutato." : "Impossibile rifiutare l'invito.");
            }
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore visualizzazione inviti: " + ex.getMessage());
        }
    }

    private boolean askYesNo(String prompt) {
        while (true) {
            String answer = inputHelper.readNonBlank(prompt).trim();
            if ("y".equalsIgnoreCase(answer)) {
                return true;
            }
            if ("n".equalsIgnoreCase(answer)) {
                return false;
            }
            System.out.println("Valore non valido. Inserisci y oppure n.");
        }
    }

    private long readInvitationId(Set<Long> invitationIds) {
        while (true) {
            long invitationId = inputHelper.readLong("Invitation id");
            if (invitationIds.contains(invitationId)) {
                return invitationId;
            }
            System.out.println("Invitation id non valido.");
        }
    }

    private String readAction() {
        while (true) {
            String action = inputHelper.readNonBlank("Action (ACCEPT/DECLINE)").trim();
            if ("ACCEPT".equalsIgnoreCase(action)) {
                return "ACCEPT";
            }
            if ("DECLINE".equalsIgnoreCase(action)) {
                return "DECLINE";
            }
            System.out.println("Azione non valida. Usa ACCEPT o DECLINE.");
        }
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}
