package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.TeamController;
import it.unicam.hackhub.model.Team;

import java.util.Optional;

public class CreateTeamCommand implements Command {
    private final TeamController teamController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public CreateTeamCommand(TeamController teamController, SessionContext sessionContext, InputHelper inputHelper) {
        this.teamController = teamController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "create-team";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isUserLoggedIn()) {
                System.out.println("Devi effettuare login USER per creare un team.");
                return;
            }

            Optional<Long> currentUserId = sessionContext.getCurrentUserId();
            if (currentUserId.isEmpty()) {
                System.out.println("Sessione utente non valida.");
                return;
            }

            String teamName = inputHelper.readNonBlank("Team name").trim();
            Team createdTeam = teamController.createTeam(currentUserId.get(), teamName);
            System.out.println("Team creato. teamId=" + createdTeam.getTeamId());
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore creazione team: " + ex.getMessage());
        }
    }
}
