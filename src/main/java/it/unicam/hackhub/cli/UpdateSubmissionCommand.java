package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.SubmissionController;
import it.unicam.hackhub.controller.TeamController;
import it.unicam.hackhub.model.Submission;

import java.util.Optional;

public class UpdateSubmissionCommand implements Command {
    private final SubmissionController submissionController;
    private final TeamController teamController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public UpdateSubmissionCommand(SubmissionController submissionController,
                                   TeamController teamController,
                                   SessionContext sessionContext,
                                   InputHelper inputHelper) {
        this.submissionController = submissionController;
        this.teamController = teamController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "update-submission";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isUserLoggedIn()) {
                System.out.println("Devi effettuare login USER per modificare la submission.");
                return;
            }

            Optional<Long> currentUserId = sessionContext.getCurrentUserId();
            if (currentUserId.isEmpty()) {
                System.out.println("Sessione utente non valida.");
                return;
            }

            Long teamId = teamController.getTeamIdOfUser(currentUserId.get());
            if (teamId == null) {
                System.out.println("Non hai un team.");
                return;
            }

            Optional<Submission> existingOpt = submissionController.viewMySubmission(teamId);
            if (existingOpt.isEmpty()) {
                System.out.println("Nessuna sottomissione presente. Usa 'submit' per inviare la prima sottomissione.");
                return;
            }

            Submission existing = existingOpt.get();
            System.out.println("Contenuto attuale: " + TablePrinter.truncate(existing.getContent(), 40));
            System.out.println("lastUpdatedAt attuale: " + existing.getLastUpdatedAt());
            String newContent = inputHelper.readNonBlank("Nuovo contenuto submission");

            Submission updated = submissionController.submitOrUpdate(currentUserId.get(), teamId, newContent);
            System.out.println("Submission aggiornata. lastUpdatedAt=" + updated.getLastUpdatedAt());
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore submission: " + ex.getMessage());
        }
    }
}
