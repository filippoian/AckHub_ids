package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.ViolationReportController;
import it.unicam.hackhub.model.ViolationReport;

import java.util.Optional;

public class ManageReportCommand implements Command {
    private final ViolationReportController violationReportController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public ManageReportCommand(ViolationReportController violationReportController,
                               SessionContext sessionContext,
                               InputHelper inputHelper) {
        this.violationReportController = violationReportController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "manage-report";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isStaffLoggedIn()) {
                System.out.println("Devi effettuare login STAFF per gestire le segnalazioni.");
                return;
            }

            Optional<Long> currentStaffId = sessionContext.getCurrentStaffId();
            if (currentStaffId.isEmpty()) {
                System.out.println("Sessione staff non valida.");
                return;
            }

            long reportId = inputHelper.readLong("Report id");
            String action = inputHelper.readNonBlank("Azione (REJECT/WARN/EXPEL)").trim();

            ViolationReport report = violationReportController.manageReport(
                    currentStaffId.get(),
                    reportId,
                    action
            );
            System.out.println("Segnalazione gestita. decision=" + report.getDecision());
            if ("TEAM_EXPELLED".equals(report.getDecision())) {
                System.out.println("Team espulso.");
            }
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore gestione segnalazione: " + ex.getMessage());
        }
    }
}
