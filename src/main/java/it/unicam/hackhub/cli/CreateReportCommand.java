package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.ViolationReportController;
import it.unicam.hackhub.model.ViolationReport;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CreateReportCommand implements Command {
    private final ViolationReportController violationReportController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public CreateReportCommand(ViolationReportController violationReportController,
                               SessionContext sessionContext,
                               InputHelper inputHelper) {
        this.violationReportController = violationReportController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "create-report";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isStaffLoggedIn()) {
                System.out.println("Devi effettuare login STAFF per creare una segnalazione.");
                return;
            }

            Optional<Long> currentStaffId = sessionContext.getCurrentStaffId();
            if (currentStaffId.isEmpty()) {
                System.out.println("Sessione staff non valida.");
                return;
            }

            List<ViolationReportController.MentorHackathonOption> mentorHackathons =
                    violationReportController.listMentorHackathons(currentStaffId.get());
            if (mentorHackathons.isEmpty()) {
                System.out.println("Comando disponibile solo per MENTOR.");
                return;
            }

            long hackathonId = chooseHackathon(mentorHackathons);
            List<ViolationReportController.ReportTeamOption> teams =
                    violationReportController.listReportableTeams(currentStaffId.get(), hackathonId);
            if (teams.isEmpty()) {
                System.out.println("Nessun team iscritto a questo hackathon.");
                return;
            }

            long teamId = chooseTeam(teams);
            String description = inputHelper.readNonBlank("Descrizione");

            ViolationReport report = violationReportController.createReport(
                    currentStaffId.get(),
                    hackathonId,
                    teamId,
                    description
            );
            System.out.println(
                    "reportId=" + report.getReportId()
                            + " | hackathonId=" + report.getHackathonId()
                            + " | teamId=" + report.getTeamId()
            );
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore creazione segnalazione: " + ex.getMessage());
        }
    }

    private long chooseHackathon(List<ViolationReportController.MentorHackathonOption> hackathons) {
        Set<Long> hackathonIds = new LinkedHashSet<>();
        List<List<String>> rows = new ArrayList<>();
        for (ViolationReportController.MentorHackathonOption hackathon : hackathons) {
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

    private long chooseTeam(List<ViolationReportController.ReportTeamOption> teams) {
        Set<Long> teamIds = new LinkedHashSet<>();
        List<List<String>> rows = new ArrayList<>();
        for (ViolationReportController.ReportTeamOption team : teams) {
            teamIds.add(team.teamId());
            rows.add(List.of(
                    String.valueOf(team.teamId()),
                    safe(team.teamName()),
                    String.valueOf(team.expelled())
            ));
        }
        TablePrinter.print(List.of("TEAM_ID", "TEAM_NAME", "EXPELLED"), rows);

        while (true) {
            long selected = inputHelper.readLong("Scegli teamId");
            if (teamIds.contains(selected)) {
                return selected;
            }
            System.out.println("Team non valido.");
        }
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}
