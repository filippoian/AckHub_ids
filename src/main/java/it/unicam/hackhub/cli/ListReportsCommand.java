package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.ViolationReportController;
import it.unicam.hackhub.model.ViolationReport;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ListReportsCommand implements Command {
    private final ViolationReportController violationReportController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public ListReportsCommand(ViolationReportController violationReportController,
                              SessionContext sessionContext,
                              InputHelper inputHelper) {
        this.violationReportController = violationReportController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "list-reports";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isStaffLoggedIn()) {
                System.out.println("Devi effettuare login STAFF per visualizzare le segnalazioni.");
                return;
            }

            Optional<Long> currentStaffId = sessionContext.getCurrentStaffId();
            if (currentStaffId.isEmpty()) {
                System.out.println("Sessione staff non valida.");
                return;
            }

            List<ViolationReportController.OrganizerHackathonOption> organizerHackathons =
                    violationReportController.listOrganizerHackathons(currentStaffId.get());
            if (organizerHackathons.isEmpty()) {
                System.out.println("Comando disponibile solo per ORGANIZER.");
                return;
            }

            long selectedHackathonId = chooseHackathon(organizerHackathons);
            boolean onlyPending = readOnlyPendingFlag();

            List<ViolationReport> reports = violationReportController.listReports(
                    currentStaffId.get(),
                    selectedHackathonId,
                    onlyPending
            );

            List<List<String>> rows = new ArrayList<>();
            for (ViolationReport report : reports) {
                rows.add(List.of(
                        String.valueOf(report.getReportId()),
                        String.valueOf(report.getTeamId()),
                        String.valueOf(report.getCreatedAt()),
                        report.getDecision() == null ? "PENDING" : report.getDecision()
                ));
            }
            TablePrinter.print(List.of("REPORT_ID", "TEAM_ID", "CREATED_AT", "DECISION"), rows);
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException ex) {
            System.out.println("Errore visualizzazione segnalazioni: " + ex.getMessage());
        }
    }

    private long chooseHackathon(List<ViolationReportController.OrganizerHackathonOption> hackathons) {
        Set<Long> hackathonIds = new LinkedHashSet<>();
        List<List<String>> rows = new ArrayList<>();
        for (ViolationReportController.OrganizerHackathonOption hackathon : hackathons) {
            hackathonIds.add(hackathon.hackathonId());
            rows.add(List.of(String.valueOf(hackathon.hackathonId()), safe(hackathon.hackathonName())));
        }
        TablePrinter.print(List.of("HACKATHON_ID", "NAME"), rows);

        while (true) {
            long selected = inputHelper.readLong("Scegli hackathonId");
            if (hackathonIds.contains(selected)) {
                return selected;
            }
            System.out.println("Hackathon non valido.");
        }
    }

    private boolean readOnlyPendingFlag() {
        while (true) {
            String answer = inputHelper.readNonBlank("Solo pendenti? (y/n)").trim();
            if ("y".equalsIgnoreCase(answer)) {
                return true;
            }
            if ("n".equalsIgnoreCase(answer)) {
                return false;
            }
            System.out.println("Valore non valido. Inserisci y oppure n.");
        }
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}
