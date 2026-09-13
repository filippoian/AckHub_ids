package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.SubmissionController;
import it.unicam.hackhub.controller.TeamController;
import it.unicam.hackhub.model.Submission;

import java.util.Optional;

public class MySubmissionCommand implements Command {
    private final SubmissionController submissionController;
    private final TeamController teamController;
    private final SessionContext sessionContext;

    public MySubmissionCommand(SubmissionController submissionController,
                               TeamController teamController,
                               SessionContext sessionContext) {
        this.submissionController = submissionController;
        this.teamController = teamController;
        this.sessionContext = sessionContext;
    }

    @Override
    public String name() {
        return "my-submission";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isUserLoggedIn()) {
                System.out.println("Devi effettuare login USER per visualizzare la submission.");
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
                System.out.println("Nessuna submission: non appartieni a un team.");
                return;
            }

            Optional<Submission> submissionOpt = submissionController.viewMySubmission(teamId);
            if (submissionOpt.isEmpty()) {
                System.out.println("Nessuna submission.");
                return;
            }

            Submission submission = submissionOpt.get();
            System.out.println("submissionId=" + submission.getSubmissionId());
            System.out.println("registrationId=" + submission.getRegistrationId());
            System.out.println("content=" + submission.getContent());
            System.out.println("submittedAt=" + submission.getSubmittedAt());
            System.out.println("lastUpdatedAt=" + submission.getLastUpdatedAt());
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException ex) {
            System.out.println("Errore recupero team utente: " + ex.getMessage());
        }
    }
}
