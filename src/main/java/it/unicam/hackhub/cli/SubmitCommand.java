package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.SubmissionController;
import it.unicam.hackhub.controller.TeamController;
import it.unicam.hackhub.model.Submission;

import java.util.Optional;

public class SubmitCommand implements Command {
    private final SubmissionController submissionController;
    private final TeamController teamController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public SubmitCommand(SubmissionController submissionController,
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
        return "submit";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isUserLoggedIn()) {
                System.out.println("Devi effettuare login USER per inviare una submission.");
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
                System.out.println("Devi appartenere a un team per inviare una submission.");
                return;
            }

            if (submissionController.viewMySubmission(teamId).isPresent()) {
                System.out.println("Hai già inviato una sottomissione. Usa 'update-submission' per modificarla.");
                return;
            }

            String content = inputHelper.readNonBlank("Submission content");
            Submission saved = submissionController.submitOrUpdate(currentUserId.get(), teamId, content);
            System.out.println("Submission salvata. submissionId=" + saved.getSubmissionId());
            System.out.println("submittedAt=" + saved.getSubmittedAt());
            System.out.println("lastUpdatedAt=" + saved.getLastUpdatedAt());
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore submission: " + ex.getMessage());
        }
    }
}
