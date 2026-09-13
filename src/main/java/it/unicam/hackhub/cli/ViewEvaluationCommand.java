package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.EvaluationController;
import it.unicam.hackhub.controller.SubmissionAccessController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ViewEvaluationCommand implements Command {
    private final SubmissionAccessController submissionAccessController;
    private final EvaluationController evaluationController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public ViewEvaluationCommand(SubmissionAccessController submissionAccessController,
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
        return "view-evaluation";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isStaffLoggedIn()) {
                System.out.println("Devi effettuare login STAFF per visualizzare la valutazione.");
                return;
            }

            Optional<Long> currentStaffId = sessionContext.getCurrentStaffId();
            if (currentStaffId.isEmpty()) {
                System.out.println("Sessione staff non valida.");
                return;
            }

            List<SubmissionAccessController.AssignedHackathonView> assignedHackathons =
                    submissionAccessController.listAssignedHackathons(currentStaffId.get());
            if (assignedHackathons.isEmpty()) {
                System.out.println("Non sei assegnato a nessun hackathon.");
                return;
            }

            long selectedHackathonId = selectHackathon(assignedHackathons);
            List<SubmissionAccessController.SubmissionView> submissions =
                    submissionAccessController.listSubmissionViewsForHackathon(currentStaffId.get(), selectedHackathonId);
            if (submissions.isEmpty()) {
                System.out.println("Nessuna submission presente.");
                return;
            }

            Map<Long, SubmissionAccessController.SubmissionView> bySubmissionId = new LinkedHashMap<>();
            List<List<String>> rows = new ArrayList<>();
            for (SubmissionAccessController.SubmissionView submission : submissions) {
                bySubmissionId.put(submission.submissionId(), submission);
                rows.add(List.of(
                        String.valueOf(submission.submissionId()),
                        String.valueOf(submission.registrationId()),
                        String.valueOf(submission.updatedAt()),
                        TablePrinter.truncate(submission.content(), 30)
                ));
            }
            TablePrinter.print(List.of("SUBMISSION_ID", "REGISTRATION_ID", "UPDATED_AT", "CONTENT"), rows);

            long submissionId = readSubmissionId(bySubmissionId.keySet());
            Optional<EvaluationController.EvaluationView> evaluationOpt =
                    evaluationController.viewEvaluation(currentStaffId.get(), submissionId);
            if (evaluationOpt.isEmpty()) {
                System.out.println("Nessuna valutazione presente.");
                return;
            }

            EvaluationController.EvaluationView evaluation = evaluationOpt.get();
            System.out.println("evaluationId=" + evaluation.evaluationId());
            System.out.println("score=" + evaluation.score());
            System.out.println("comment=" + evaluation.comment());
            System.out.println("evaluatedAt=" + evaluation.evaluatedAt());
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException ex) {
            System.out.println("Errore visualizzazione valutazione: " + ex.getMessage());
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
            long selectedHackathonId = inputHelper.readLong("Hackathon id");
            if (hackathonIds.contains(selectedHackathonId)) {
                return selectedHackathonId;
            }
            System.out.println("Hackathon non valido.");
        }
    }

    private long readSubmissionId(Set<Long> submissionIds) {
        while (true) {
            long submissionId = inputHelper.readLong("Submission id");
            if (submissionIds.contains(submissionId)) {
                return submissionId;
            }
            System.out.println("Submission id non valido.");
        }
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}
