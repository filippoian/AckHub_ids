package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.TeamController;

public class LeaveTeamCommand implements Command {
    private final TeamController teamController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public LeaveTeamCommand(TeamController teamController, SessionContext sessionContext, InputHelper inputHelper) {
        this.teamController = teamController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    public String name() { return "leave-team"; }

    public void execute() {
        try {
            long userId = sessionContext.getCurrentUserId().orElseThrow(() -> new IllegalStateException("Login utente richiesto"));
            String confirm = inputHelper.readNonBlank("Confermi l'abbandono del team? (si/no)");
            if (!"si".equalsIgnoreCase(confirm.trim()) && !"sì".equalsIgnoreCase(confirm.trim())) return;
            teamController.leaveTeam(userId);
            System.out.println("Hai lasciato il team.");
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore: " + ex.getMessage());
        }
    }
}
