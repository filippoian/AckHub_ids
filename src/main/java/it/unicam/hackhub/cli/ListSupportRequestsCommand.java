package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.SupportController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ListSupportRequestsCommand implements Command {
    private final SupportController supportController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public ListSupportRequestsCommand(SupportController supportController,
                                      SessionContext sessionContext,
                                      InputHelper inputHelper) {
        this.supportController = supportController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "list-support-requests";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isStaffLoggedIn()) {
                System.out.println("Devi effettuare login STAFF per visualizzare le richieste di supporto.");
                return;
            }

            Optional<Long> currentStaffId = sessionContext.getCurrentStaffId();
            if (currentStaffId.isEmpty()) {
                System.out.println("Sessione staff non valida.");
                return;
            }

            List<SupportController.MentorHackathonView> mentorHackathons =
                    supportController.listMentorAssignedHackathons(currentStaffId.get());
            if (mentorHackathons.isEmpty()) {
                System.out.println("Non sei assegnato come mentor a nessun hackathon.");
                return;
            }

            long selectedHackathonId = chooseHackathon(mentorHackathons);
            List<SupportController.SupportRequestView> requests =
                    supportController.listSupportRequestsForMentor(currentStaffId.get(), selectedHackathonId);

            List<List<String>> rows = new ArrayList<>();
            Set<Long> requestIds = new LinkedHashSet<>();
            for (SupportController.SupportRequestView request : requests) {
                requestIds.add(request.requestId());
                rows.add(List.of(
                        String.valueOf(request.requestId()),
                        String.valueOf(request.teamId()),
                        String.valueOf(request.createdAt()),
                        TablePrinter.truncate(request.message(), 40)
                ));
            }
            TablePrinter.print(List.of("REQUEST_ID", "TEAM_ID", "CREATED_AT", "MESSAGE"), rows);

            if (requests.isEmpty()) {
                return;
            }

            if (!askShowDetail()) {
                return;
            }

            long requestId = readRequestId(requestIds);
            requests.stream()
                    .filter(request -> request.requestId() == requestId)
                    .findFirst()
                    .ifPresent(this::printDetail);
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore visualizzazione richieste supporto: " + ex.getMessage());
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

    private boolean askShowDetail() {
        while (true) {
            String answer = inputHelper.readNonBlank("Vuoi vedere il dettaglio di una richiesta? (y/n)").trim();
            if ("y".equalsIgnoreCase(answer)) {
                return true;
            }
            if ("n".equalsIgnoreCase(answer)) {
                return false;
            }
            System.out.println("Risposta non valida. Inserisci y o n.");
        }
    }

    private long readRequestId(Set<Long> requestIds) {
        while (true) {
            long requestId = inputHelper.readLong("Seleziona requestId");
            if (requestIds.contains(requestId)) {
                return requestId;
            }
            System.out.println("Request non valida.");
        }
    }

    private void printDetail(SupportController.SupportRequestView request) {
        System.out.println("requestId: " + request.requestId());
        System.out.println("teamId: " + request.teamId());
        System.out.println("createdAt: " + request.createdAt());
        System.out.println("MESSAGE:");
        System.out.println(request.message() == null ? "" : request.message());
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}

