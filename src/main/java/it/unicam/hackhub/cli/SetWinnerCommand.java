package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.HackathonController;
import it.unicam.hackhub.model.Hackathon;

import java.util.Optional;

public class SetWinnerCommand implements Command {
    private final HackathonController hackathonController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public SetWinnerCommand(HackathonController hackathonController,
                            SessionContext sessionContext,
                            InputHelper inputHelper) {
        this.hackathonController = hackathonController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "set-winner";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isStaffLoggedIn()) {
                System.out.println("Devi effettuare login STAFF per impostare il vincitore.");
                return;
            }

            Optional<Long> currentStaffId = sessionContext.getCurrentStaffId();
            if (currentStaffId.isEmpty()) {
                System.out.println("Sessione staff non valida.");
                return;
            }

            long hackathonId = inputHelper.readLong("Hackathon id");
            long teamId = inputHelper.readLong("Team id");

            Hackathon hackathon = hackathonController.setWinner(currentStaffId.get(), hackathonId, teamId);
            System.out.println(
                    "Vincitore impostato: hackathon "
                            + hackathon.getHackathonId()
                            + " -> team "
                            + hackathon.getWinnerTeamId()
            );
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore impostazione vincitore: " + ex.getMessage());
        }
    }
}
