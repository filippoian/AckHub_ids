package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.SupportController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ListCallProposalsCommand implements Command {
    private final SupportController supportController;
    private final SessionContext sessionContext;

    public ListCallProposalsCommand(SupportController supportController,
                                    SessionContext sessionContext) {
        this.supportController = supportController;
        this.sessionContext = sessionContext;
    }

    @Override
    public String name() {
        return "list-call-proposals";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isUserLoggedIn()) {
                System.out.println("Devi effettuare login USER per visualizzare le proposte call.");
                return;
            }

            Optional<Long> currentUserId = sessionContext.getCurrentUserId();
            if (currentUserId.isEmpty()) {
                System.out.println("Sessione utente non valida.");
                return;
            }

            List<SupportController.CallProposalSummary> proposals =
                    supportController.listAvailableCallProposalsForCurrentUser(currentUserId.get());

            List<List<String>> rows = new ArrayList<>();
            for (SupportController.CallProposalSummary proposal : proposals) {
                rows.add(List.of(
                        String.valueOf(proposal.proposalId()),
                        String.valueOf(proposal.requestId()),
                        String.valueOf(proposal.proposedStart()),
                        String.valueOf(proposal.proposedEnd()),
                        String.valueOf(proposal.booked())
                ));
            }

            TablePrinter.print(List.of("PROPOSAL_ID", "REQUEST_ID", "START", "END", "BOOKED"), rows);
        } catch (IllegalArgumentException ex) {
            System.out.println("Errore visualizzazione proposte call: " + ex.getMessage());
        }
    }
}
