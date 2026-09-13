package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.SubmissionAccessController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ListSubmissionsCommand implements Command {
    private final SubmissionAccessController submissionAccessController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public ListSubmissionsCommand(SubmissionAccessController submissionAccessController,
                                  SessionContext sessionContext,
                                  InputHelper inputHelper) {
        this.submissionAccessController = submissionAccessController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "list-submissions";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isStaffLoggedIn()) {
                System.out.println("Devi effettuare login STAFF per visualizzare le submission.");
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

            List<List<String>> rows = new ArrayList<>();
            Set<Long> visibleSubmissionIds = new LinkedHashSet<>();
            for (SubmissionAccessController.SubmissionView submission : submissions) {
                visibleSubmissionIds.add(submission.submissionId());
                rows.add(List.of(
                        String.valueOf(submission.submissionId()),
                        String.valueOf(submission.registrationId()),
                        String.valueOf(submission.updatedAt()),
                        TablePrinter.truncate(submission.content(), 30)
                ));
            }
            TablePrinter.print(List.of("SUBMISSION_ID", "REGISTRATION_ID", "UPDATED_AT", "CONTENT"), rows);
            if (submissions.isEmpty()) {
                System.out.println("Nessuna submission.");
                return;
            }

            if (!askShowDetail()) {
                return;
            }

            long submissionId = readSubmissionId(visibleSubmissionIds);
            submissionAccessController.getSubmissionDetailForHackathon(
                            currentStaffId.get(),
                            selectedHackathonId,
                            submissionId
                    )
                    .ifPresentOrElse(
                            this::printDetail,
                            () -> System.out.println("Submission non trovata.")
                    );
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException ex) {
            System.out.println("Errore visualizzazione submission: " + ex.getMessage());
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
                    safe(hackathon.location()),
                    safe(hackathon.rolesLabel())
            ));
        }

        System.out.println("I tuoi hackathon:");
        TablePrinter.print(List.of("HACKATHON_ID", "NAME", "STATUS", "LOCATION", "ROLES"), rows);

        while (true) {
            long selectedHackathonId = inputHelper.readLong("Scegli hackathonId");
            if (hackathonIds.contains(selectedHackathonId)) {
                return selectedHackathonId;
            }
            System.out.println("Hackathon non valido.");
        }
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }

    private boolean askShowDetail() {
        while (true) {
            String answer = inputHelper.readNonBlank("Vuoi vedere il dettaglio di una submission? (y/n)").trim();
            if ("y".equalsIgnoreCase(answer)) {
                return true;
            }
            if ("n".equalsIgnoreCase(answer)) {
                return false;
            }
            System.out.println("Risposta non valida. Inserisci y o n.");
        }
    }

    private long readSubmissionId(Set<Long> visibleSubmissionIds) {
        while (true) {
            long submissionId = inputHelper.readLong("Seleziona submissionId");
            if (visibleSubmissionIds.contains(submissionId)) {
                return submissionId;
            }
            System.out.println("Submission non valida.");
        }
    }

    private void printDetail(SubmissionAccessController.SubmissionDetailView detail) {
        System.out.println("submissionId: " + detail.submissionId());
        System.out.println("registrationId: " + detail.registrationId());
        System.out.println("updatedAt: " + detail.updatedAt());
        System.out.println("CONTENT:");
        System.out.println(detail.content() == null ? "" : detail.content());
    }
}
