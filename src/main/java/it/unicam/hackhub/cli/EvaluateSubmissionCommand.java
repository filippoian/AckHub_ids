package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.EvaluationController;
import it.unicam.hackhub.controller.SubmissionAccessController;
import it.unicam.hackhub.model.Evaluation;
import it.unicam.hackhub.model.Submission;
import it.unicam.hackhub.model.enums.StaffRole;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class EvaluateSubmissionCommand implements Command {
    private final SubmissionAccessController submissionAccessController;
    private final EvaluationController evaluationController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public EvaluateSubmissionCommand(SubmissionAccessController submissionAccessController,
                                     EvaluationController evaluationController,
                                     SessionContext sessionContext,
                                     InputHelper inputHelper) {
        this.submissionAccessController = submissionAccessController;
        this.evaluationController = evaluationController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "evaluate-submission";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isStaffLoggedIn()) {
                System.out.println("Devi effettuare login STAFF per valutare una submission.");
                return;
            }

            Optional<Long> currentStaffId = sessionContext.getCurrentStaffId();
            if (currentStaffId.isEmpty()) {
                System.out.println("Sessione staff non valida.");
                return;
            }

            List<SubmissionAccessController.AssignedHackathonView> judgeHackathons =
                    submissionAccessController.listAssignedHackathons(currentStaffId.get()).stream()
                            .filter(hackathon -> hackathon.roles() != null && hackathon.roles().contains(StaffRole.JUDGE))
                            .toList();
            if (judgeHackathons.isEmpty()) {
                System.out.println("Non sei assegnato come JUDGE a nessun hackathon.");
                return;
            }

            long selectedHackathonId = selectHackathon(judgeHackathons);
            evaluationController.assertHackathonInReviewForJudge(currentStaffId.get(), selectedHackathonId);

            List<Submission> submissions =
                    submissionAccessController.listSubmissionsForHackathon(currentStaffId.get(), selectedHackathonId);
            if (submissions.isEmpty()) {
                System.out.println("Nessuna submission per questo hackathon.");
                return;
            }

            Set<Long> visibleSubmissionIds = new LinkedHashSet<>();
            List<List<String>> rows = new ArrayList<>();
            for (Submission submission : submissions) {
                visibleSubmissionIds.add(submission.getSubmissionId());
                rows.add(List.of(
                        String.valueOf(submission.getSubmissionId()),
                        String.valueOf(submission.getRegistrationId()),
                        String.valueOf(submission.getLastUpdatedAt()),
                        TablePrinter.truncate(submission.getContent(), 30)
                ));
            }
            TablePrinter.print(List.of("SUBMISSION_ID", "REGISTRATION_ID", "UPDATED_AT", "CONTENT"), rows);

            long submissionId = readSubmissionId(visibleSubmissionIds);
            evaluationController.assertEvaluatable(currentStaffId.get(), submissionId);

            int score = readScore();
            String comment = inputHelper.readLine("Commento");

            Evaluation evaluation = evaluationController.evaluateSubmission(
                    currentStaffId.get(),
                    submissionId,
                    score,
                    comment
            );
            System.out.println(
                    "evaluationId=" + evaluation.getEvaluationId()
                            + " | submissionId=" + evaluation.getSubmissionId()
                            + " | voto base=" + evaluation.getBaseScore()
                            + " | penalità=" + evaluation.getPenalty()
                            + " | score=" + evaluation.getScore()
                            + " | evaluatedAt=" + evaluation.getEvaluatedAt()
            );
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore valutazione submission: " + ex.getMessage());
        }
    }

    private long selectHackathon(List<SubmissionAccessController.AssignedHackathonView> hackathons) {
        if (hackathons.size() == 1) {
            return hackathons.get(0).hackathonId();
        }

        Set<Long> hackathonIds = new LinkedHashSet<>();
        List<List<String>> rows = new ArrayList<>();
        for (SubmissionAccessController.AssignedHackathonView hackathon : hackathons) {
            hackathonIds.add(hackathon.hackathonId());
            rows.add(List.of(
                    String.valueOf(hackathon.hackathonId()),
                    safe(hackathon.name()),
                    safe(hackathon.status()),
                    safe(hackathon.location())
            ));
        }
        TablePrinter.print(List.of("HACKATHON_ID", "NAME", "STATUS", "LOCATION"), rows);

        while (true) {
            long hackathonId = inputHelper.readLong("Scegli hackathonId");
            if (hackathonIds.contains(hackathonId)) {
                return hackathonId;
            }
            System.out.println("Hackathon non valido.");
        }
    }

    private long readSubmissionId(Set<Long> submissionIds) {
        while (true) {
            long submissionId = inputHelper.readLong("Seleziona submissionId");
            if (submissionIds.contains(submissionId)) {
                return submissionId;
            }
            System.out.println("Submission non valida.");
        }
    }

    private int readScore() {
        while (true) {
            long rawScore = inputHelper.readLong("Score");
            if (rawScore < 0 || rawScore > 30) {
                System.out.println("Score non valido. Inserisci un valore tra 0 e 30.");
                continue;
            }
            return (int) rawScore;
        }
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}

