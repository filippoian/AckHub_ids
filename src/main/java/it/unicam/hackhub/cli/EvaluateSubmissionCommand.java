package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.EvaluationController;
import it.unicam.hackhub.controller.SubmissionAccessController;
import it.unicam.hackhub.model.Evaluation;

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
    private final boolean update;

    public EvaluateSubmissionCommand(SubmissionAccessController submissionAccessController,
                                     EvaluationController evaluationController,
                                     SessionContext sessionContext,
                                     InputHelper inputHelper) {
        this(submissionAccessController, evaluationController, sessionContext, inputHelper, false);
    }

    public EvaluateSubmissionCommand(SubmissionAccessController submissionAccessController,
                                     EvaluationController evaluationController,
                                     SessionContext sessionContext,
                                     InputHelper inputHelper, boolean update) {
        this.update = update;
        this.submissionAccessController = submissionAccessController;
        this.evaluationController = evaluationController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return update ? "update-evaluation" : "evaluate-submission";
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
                    submissionAccessController.listEvaluatableHackathons(currentStaffId.get());
            if (judgeHackathons.isEmpty()) {
                System.out.println("Nessun hackathon assegnato in valutazione e senza vincitore.");
                return;
            }

            long selectedHackathonId = selectHackathon(judgeHackathons);
            evaluationController.assertHackathonInReviewForJudge(currentStaffId.get(), selectedHackathonId);

            List<SubmissionAccessController.SubmissionView> submissions =
                    submissionAccessController.listSubmissionViewsForHackathon(currentStaffId.get(), selectedHackathonId);
            if (submissions.isEmpty()) {
                System.out.println("Nessuna sottomissione per questo hackathon.");
                return;
            }

            Set<Long> visibleSubmissionIds = new LinkedHashSet<>();
            List<List<String>> rows = new ArrayList<>();
            for (var submission : submissions) {
                visibleSubmissionIds.add(submission.submissionId());
                rows.add(List.of(String.valueOf(submission.submissionId()), safe(submission.teamName()),
                        submission.disciplinaryStatus(), submission.evaluated() ? "SÌ" : "NO"));
            }
            TablePrinter.print(List.of("SUBMISSION_ID", "TEAM", "STATO DISCIPLINARE", "VALUTATA"), rows);

            long submissionId = readSubmissionId(visibleSubmissionIds);
            evaluationController.assertEvaluatable(currentStaffId.get(), submissionId);
            var existing = evaluationController.viewEvaluation(currentStaffId.get(), submissionId);
            if (existing.isPresent() != update) {
                System.out.println(update ? "Valutazione assente. Usa evaluate-submission."
                        : "Sottomissione già valutata. Usa update-evaluation.");
                return;
            }

            var detail = submissionAccessController.getSubmissionDetailForHackathon(
                    currentStaffId.get(), selectedHackathonId, submissionId).orElseThrow();
            System.out.println(detail.content());
            for (var warning : detail.warnings()) {
                System.out.println("Ammonizione " + warning.reportId() + " | " + warning.reason()
                        + " | " + (warning.evaluated() ? warning.penalty() : "da valutare con evaluate-warning"));
            }
            existing.ifPresent(current -> System.out.println("Voto base: " + current.baseScore()
                    + " | Giudizio: " + current.comment()));
            int score = readScore();
            String comment = inputHelper.readNonBlank("Giudizio").trim();

            Evaluation evaluation = update
                    ? evaluationController.updateEvaluation(currentStaffId.get(), submissionId, score, comment)
                    : evaluationController.evaluateSubmission(currentStaffId.get(), submissionId, score, comment);
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

