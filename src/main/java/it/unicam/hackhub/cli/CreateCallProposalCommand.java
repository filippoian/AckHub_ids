package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.SupportController;
import it.unicam.hackhub.model.CallProposal;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CreateCallProposalCommand implements Command {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SupportController supportController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public CreateCallProposalCommand(SupportController supportController,
                                     SessionContext sessionContext,
                                     InputHelper inputHelper) {
        this.supportController = supportController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "create-call-proposal";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isStaffLoggedIn()) {
                System.out.println("Devi effettuare login STAFF per creare una proposta call.");
                return;
            }

            Optional<Long> currentStaffId = sessionContext.getCurrentStaffId();
            if (currentStaffId.isEmpty()) {
                System.out.println("Sessione staff non valida.");
                return;
            }

            List<SupportController.MentorHackathonView> hackathons =
                    supportController.listMentorAssignedHackathons(currentStaffId.get());
            if (hackathons.isEmpty()) {
                System.out.println("Non sei assegnato come mentor a nessun hackathon.");
                return;
            }

            long selectedHackathonId = chooseHackathon(hackathons);
            List<SupportController.SupportRequestView> supportRequests =
                    supportController.listSupportRequestsForMentor(currentStaffId.get(), selectedHackathonId);

            List<List<String>> requestRows = new ArrayList<>();
            Set<Long> requestIds = new LinkedHashSet<>();
            for (SupportController.SupportRequestView request : supportRequests) {
                requestIds.add(request.requestId());
                requestRows.add(List.of(
                        String.valueOf(request.requestId()),
                        String.valueOf(request.teamId()),
                        String.valueOf(request.createdAt()),
                        TablePrinter.truncate(request.message(), 40)
                ));
            }
            TablePrinter.print(List.of("REQUEST_ID", "TEAM_ID", "CREATED_AT", "MESSAGE"), requestRows);
            if (supportRequests.isEmpty()) {
                System.out.println("Nessuna richiesta di supporto per questo hackathon.");
                return;
            }

            long supportRequestId = chooseRequestId(requestIds);
            LocalDateTime proposedStart = readDateTime("Start (yyyy-MM-dd HH:mm)");
            LocalDateTime proposedEnd = readDateTime("End (yyyy-MM-dd HH:mm)");

            CallProposal proposal = supportController.createCallProposal(
                    currentStaffId.get(),
                    supportRequestId,
                    proposedStart,
                    proposedEnd
            );
            System.out.println(
                    "Proposal creata. proposalId=" + proposal.getProposalId()
                            + " | isBooked=" + proposal.isBooked()
            );
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore creazione proposta call: " + ex.getMessage());
        }
    }

    private long chooseHackathon(List<SupportController.MentorHackathonView> hackathons) {
        if (hackathons.size() == 1) {
            return hackathons.get(0).hackathonId();
        }

        Set<Long> hackathonIds = new LinkedHashSet<>();
        List<List<String>> rows = new ArrayList<>();
        for (SupportController.MentorHackathonView hackathon : hackathons) {
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

    private long chooseRequestId(Set<Long> requestIds) {
        while (true) {
            long requestId = inputHelper.readLong("Support request id");
            if (requestIds.contains(requestId)) {
                return requestId;
            }
            System.out.println("Request non valida.");
        }
    }

    private LocalDateTime readDateTime(String prompt) {
        while (true) {
            String raw = inputHelper.readNonBlank(prompt).trim();
            try {
                return LocalDateTime.parse(raw, DATE_TIME_FORMATTER);
            } catch (DateTimeParseException ex) {
                System.out.println("Formato data/ora non valido. Usa yyyy-MM-dd HH:mm");
            }
        }
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}
