package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.EvaluationController;
import it.unicam.hackhub.controller.ViolationReportController;
import it.unicam.hackhub.model.ViolationReport;

public class EvaluateWarningCommand implements Command {
    private final EvaluationController evaluationController;
    private final ViolationReportController violationReportController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public EvaluateWarningCommand(EvaluationController evaluationController,
                                  ViolationReportController violationReportController,
                                  SessionContext sessionContext, InputHelper inputHelper) {
        this.evaluationController = evaluationController;
        this.violationReportController = violationReportController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    public String name() { return "evaluate-warning"; }

    public void execute() {
        try {
            long staffId = sessionContext.getCurrentStaffId()
                    .orElseThrow(() -> new IllegalStateException("Login staff richiesto"));
            long hackathonId = inputHelper.readLong("Hackathon id");
            var warnings = violationReportController.listWarnings(staffId, hackathonId);
            for (ViolationReport warning : warnings) {
                System.out.println("reportId=" + warning.getReportId() + " | teamId=" + warning.getTeamId()
                        + " | penalità=" + (warning.isPenaltyEvaluated() ? warning.getPenalty() : "da valutare"));
            }
            if (warnings.isEmpty()) return;
            long reportId = inputHelper.readLong("Ammonizione da valutare");
            if (warnings.stream().noneMatch(w -> w.getReportId() == reportId)) {
                throw new IllegalArgumentException("Ammonizione non presente nell’hackathon selezionato");
            }
            long penalty = inputHelper.readLong("Penalità (0–30)");
            if (penalty < 0 || penalty > 30) throw new IllegalArgumentException("Penalità fuori intervallo");
            evaluationController.evaluateWarning(staffId, reportId, (int) penalty);
            System.out.println("Penalità salvata e valutazione eventualmente ricalcolata.");
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore: " + ex.getMessage());
        }
    }
}
