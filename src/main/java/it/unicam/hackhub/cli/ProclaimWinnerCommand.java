package it.unicam.hackhub.cli;

import it.unicam.hackhub.controller.HackathonController;
import it.unicam.hackhub.model.Hackathon;

import java.util.Optional;

public class ProclaimWinnerCommand implements Command {
    private final HackathonController hackathonController;
    private final SessionContext sessionContext;
    private final InputHelper inputHelper;

    public ProclaimWinnerCommand(HackathonController hackathonController,
                                 SessionContext sessionContext,
                                 InputHelper inputHelper) {
        this.hackathonController = hackathonController;
        this.sessionContext = sessionContext;
        this.inputHelper = inputHelper;
    }

    @Override
    public String name() {
        return "proclaim-winner";
    }

    @Override
    public void execute() {
        try {
            if (!sessionContext.isStaffLoggedIn()) {
                System.out.println("Devi effettuare login STAFF per proclamare il vincitore.");
                return;
            }

            Optional<Long> currentStaffId = sessionContext.getCurrentStaffId();
            if (currentStaffId.isEmpty()) {
                System.out.println("Sessione staff non valida.");
                return;
            }

            long hackathonId = inputHelper.readLong("Hackathon id");
            Hackathon hackathon = hackathonController.getHackathonDetails(hackathonId)
                    .orElseThrow(() -> new IllegalArgumentException("Hackathon not found"));

            if (hackathon.isClosed()) {
                if (hackathon.getWinnerTeamId() == null) {
                    System.out.println(
                            "Hackathon gia CLOSED ma vincitore non impostato. Usa set-winner quando l'hackathon e in REVIEW."
                    );
                    return;
                }
                String receipt = hackathonController.payPrize(currentStaffId.get(), hackathonId);
                System.out.println(receipt);
                return;
            }

            if (!hackathon.canEvaluate()) {
                System.out.println("Proclamazione vincitore disponibile solo in REVIEW.");
                return;
            }

            long teamId = inputHelper.readLong("Team id");

            Hackathon afterWinner = hackathonController.setWinner(currentStaffId.get(), hackathonId, teamId);
            System.out.println(
                    "Vincitore impostato: hackathon "
                            + afterWinner.getHackathonId()
                            + " -> team "
                            + afterWinner.getWinnerTeamId()
            );

            String receipt = hackathonController.payPrize(currentStaffId.get(), hackathonId);
            System.out.println(receipt);
        } catch (OperationCancelledException ex) {
            System.out.println("Operazione annullata.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Errore proclamazione vincitore: " + ex.getMessage());
        }
    }
}

